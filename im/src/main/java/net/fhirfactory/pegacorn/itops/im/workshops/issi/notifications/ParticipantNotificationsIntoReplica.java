/*
 * Copyright (c) 2021 Mark A. Hunter
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.fhirfactory.pegacorn.itops.im.workshops.issi.notifications;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixInstantMessageMethods;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.common.MAPIResponse;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.communicate.synapse.credentials.SynapseAdminAccessToken;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseRoomMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseAdminProxyInterface;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.valuesets.PetasosComponentITOpsNotificationTypeEnum;
import net.fhirfactory.pegacorn.itops.im.common.ITOpsIMNames;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsNotificationsDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsSystemWideMetricsDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsSystemWideReportedTopologyMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownRoomAndSpaceMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.ParticipantRoomIdentityFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.notifications.ParticipantNotificationEventFactory;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ParticipantNotificationsIntoReplica extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantNotificationsIntoReplica.class);

    private boolean initialised;

    private boolean stillRunning;

    private Long CONTENT_FORWARDER_STARTUP_DELAY = 120000L;
    private Long CONTENT_FORWARDER_REFRESH_PERIOD = 15000L;

    List<SynapseRoom> roomList;
    Instant lastRoomListUpdate;
    ConcurrentHashMap<String, String> roomIdMap;

    @Inject
    private ITOpsIMNames itOpsIMNames;

    @Inject
    private SynapseAdminAccessToken synapseAccessToken;

    @Produce
    private ProducerTemplate camelRouteInjector;

    @Inject
    private SynapseAdminProxyInterface synapseAdminProxy;

    @Inject
    private MatrixInstantMessageMethods matrixInstantMessageAPI;

    @Inject
    private SynapseRoomMethods synapseRoomAPI;

    @Inject
    private MatrixAccessToken matrixAccessToken;

    @Inject
    private ITOpsSystemWideReportedTopologyMapDM systemWideTopologyMap;

    @Inject
    private ITOpsSystemWideMetricsDM systemWideMetrics;

    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    @Inject
    private ParticipantNotificationEventFactory notificationEventFactory;

    @Inject
    private ITOpsNotificationsDM notificationsDM;

    @Inject
    private ITOpsKnownRoomAndSpaceMapDM matrixBridgeCache;

    //
    // Constructor(s)
    //

    public ParticipantNotificationsIntoReplica(){
        super();
        this.initialised = false;
        this.stillRunning = false;
        this.roomList = new ArrayList<>();
        roomIdMap = new ConcurrentHashMap<>();
        this.lastRoomListUpdate = Instant.EPOCH;
    }

    //
    // Post Construct
    //

    @PostConstruct
    public void initialise(){
        getLogger().debug(".initialise(): Entry");
        if(initialised){
            getLogger().debug(".initialise(): Exit, already initialised, nothing to do");
            return;
        }
        getLogger().info(".initialise(): Initialisation Start...");

        scheduleNotificationsFowarderDaemon();

        this.initialised = true;

        getLogger().info(".initialise(): Initialisation Finish...");
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected SynapseAdminAccessToken getSynapseAccessToken(){
        return(synapseAccessToken);
    }

    protected boolean isStillRunning(){
        return(this.stillRunning);
    }

    //
    // Scheduler
    //

    private void scheduleNotificationsFowarderDaemon() {
        getLogger().debug(".scheduleNotificationsForwarderDaemon(): Entry");
        TimerTask notificationForwarderDaemonTask = new TimerTask() {
            public void run() {
                getLogger().debug(".notificationForwarderDaemonTask(): Entry");
                if(!isStillRunning()) {
                    notificationForwarder();
                }
                getLogger().debug(".notificationForwarderDaemonTask(): Exit");
            }
        };
        Timer timer = new Timer("NotificationsForwarderDaemonTimer");
        timer.schedule(notificationForwarderDaemonTask, CONTENT_FORWARDER_STARTUP_DELAY, CONTENT_FORWARDER_REFRESH_PERIOD);
        getLogger().debug(".scheduleNotificationsForwarderDaemon(): Exit");
    }

    //
    // Content Forwarder
    //

    private void notificationForwarder(){
        getLogger().debug(".notificationForwarder(): Entry");
        stillRunning = true;
        List<PetasosComponentITOpsNotification> failedToSend = new ArrayList<>();
        while(notificationsDM.hasMoreNotifications()) {
            getLogger().trace(".notificationForwarder(): Entry");
            PetasosComponentITOpsNotification nextNotification = notificationsDM.getNextNotification();
            boolean successfullySent = false;
            switch (nextNotification.getComponentType()) {
                case PETASOS_MONITORED_COMPONENT_SUBSYSTEM:
                    getLogger().trace(".notificationForwarder(): Processing ProcessorPlant Metrics");
                    successfullySent = forwardProcessingPlantNotification(nextNotification);
                    break;
                case PETASOS_MONITORED_COMPONENT_SERVICE:
                    break;
                case PETASOS_MONITORED_COMPONENT_PROCESSING_PLANT:
                    getLogger().trace(".notificationForwarder(): Processing ProcessorPlant Metrics");
                    successfullySent = forwardProcessingPlantNotification(nextNotification);
                    break;
                case PETASOS_MONITORED_COMPONENT_WORKSHOP:
                    break;
                case PETASOS_MONITORED_COMPONENT_WORK_UNIT_PROCESSOR:
                    getLogger().trace(".notificationForwarder(): Processing WorkUnitProcessor Metrics");
                    successfullySent = forwardWUPNotification(nextNotification);
                    if(nextNotification.getNotificationType().equals(PetasosComponentITOpsNotificationTypeEnum.FAILURE_NOTIFICATION_TYPE)){
                        camelRouteInjector.sendBody(itOpsIMNames.getITOpsNotificationToCommunicateMessageIngresFeed(), ExchangePattern.InOnly, nextNotification);
                    }
                    break;
                case PETASOS_MONITORED_COMPONENT_WORK_UNIT_PROCESSOR_COMPONENT:
                    break;
                case PETASOS_MONITORED_COMPONENT_ENDPOINT:
                    getLogger().debug(".notificationForwarder(): Processing Endpoint Metrics");
                    successfullySent = forwardEndpointNotification(nextNotification);
                    if(nextNotification.getNotificationType().equals(PetasosComponentITOpsNotificationTypeEnum.FAILURE_NOTIFICATION_TYPE)){
                        getLogger().info(".notificationForwarder(): Is Failure, generating Email/SMS Message");
                        camelRouteInjector.sendBody(itOpsIMNames.getITOpsNotificationToCommunicateMessageIngresFeed(), ExchangePattern.InOnly, nextNotification);
                    }
                    break;
            }
            if (!successfullySent) {
                failedToSend.add(nextNotification);
            }
        }
        for(PetasosComponentITOpsNotification currentNotification: failedToSend){
            notificationsDM.addNotification(currentNotification);
        }
        stillRunning = false;
        getLogger().debug(".notificationForwarder(): Exit");
    }

    //
    // Per Metric/Reporting Type Helpers
    //

    private boolean forwardWUPNotification(PetasosComponentITOpsNotification notification){
        getLogger().debug(".forwardWUPNotification(): Entry, notification->{}",notification);
        try {
            String roomAlias = roomIdentityFactory.buildWUPRoomCanonicalAlias(
                    notification.getParticipantName(),
                    OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_CONSOLE);

            getLogger().trace(".forwardWUPNotification(): roomAlias for Events->{}", roomAlias);

            String roomIdFromAlias = getRoomId(roomAlias);

            getLogger().trace(".forwardWUPNotification(): roomId for Events->{}", roomIdFromAlias);

            if(roomIdFromAlias != null) {

                MRoomTextMessageEvent notificationEvent = notificationEventFactory.newNotificationEvent(roomIdFromAlias, notification);

                try {
                    MAPIResponse mapiResponse = matrixInstantMessageAPI.postTextMessage(roomIdFromAlias, matrixAccessToken.getUserId(), notificationEvent);
                    return(true);
                } catch(Exception ex){
                    getLogger().warn(".forwardWUPNotification(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                    return(false);
                }
            } else {
                getLogger().warn(".forwardWUPNotification(): No room to forward work unit processor notifications into (WorkUnitProcessor->{})!", notification.getParticipantName());
                // TODO either re-queue or send to DeadLetter
                return(false);
            }
        } catch (Exception ex) {
            getLogger().warn(".forwardWUPNotification(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return(false);
        }
    }

    private boolean forwardProcessingPlantNotification(PetasosComponentITOpsNotification notification){
        getLogger().debug(".forwardProcessingPlantNotification(): Entry, notification->{}", notification);

        String roomAlias = roomIdentityFactory.buildProcessingPlantCanonicalAlias(notification.getParticipantName(), OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_CONSOLE);

        getLogger().trace(".forwardProcessingPlantNotification(): roomAlias for Events->{}", roomAlias);

        String roomIdFromAlias = getRoomId(roomAlias);

        getLogger().trace(".forwardProcessingPlantNotification(): roomId for Events->{}", roomIdFromAlias);

        if(roomIdFromAlias != null){
            MRoomTextMessageEvent notificationEvent = notificationEventFactory.newNotificationEvent(roomIdFromAlias, notification);

            try{
                MAPIResponse mapiResponse = matrixInstantMessageAPI.postTextMessage(roomIdFromAlias, matrixAccessToken.getUserId(), notificationEvent);
                return(true);
            } catch(Exception ex){
                getLogger().warn(".forwardProcessingPlantNotification(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                return(false);
            }
        } else {
            getLogger().warn(".forwardProcessingPlantNotification(): No room to forward processing plant notifications into (ProcessingPlant->{}!", notification.getParticipantName());
            // TODO either re-queue or send to DeadLetter
            return(false);
        }
    }

    private boolean forwardEndpointNotification(PetasosComponentITOpsNotification notification){
        getLogger().debug(".forwardEndpointNotification(): Entry, notification->{}", notification);

        String roomAlias = roomIdentityFactory.buildEndpointRoomAlias(notification.getParticipantName(), OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_CONSOLE);

        getLogger().debug(".forwardEndpointNotification(): roomAlias for Events->{}", roomAlias);

        String roomIdFromAlias = getRoomId(roomAlias);

        getLogger().debug(".forwardEndpointNotification(): roomId for Events->{}", roomIdFromAlias);

        if(roomIdFromAlias != null){
            MRoomTextMessageEvent notificationEvent = notificationEventFactory.newNotificationEvent(roomIdFromAlias, notification);
            try{
                MAPIResponse mapiResponse = matrixInstantMessageAPI.postTextMessage(roomIdFromAlias, matrixAccessToken.getUserId(), notificationEvent);
                getLogger().debug(".forwardEndpointNotification(): notification sent!");
                return(true);
            } catch(Exception ex){
                getLogger().warn(".forwardEndpointNotification(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                return(false);
            }
        } else {
            getLogger().warn(".forwardEndpointNotification(): No room to forward endpoint notifications into (EndpointRoom->{}!", notification.getParticipantName());
            return(false);
        }
    }

    //
    // Mechanism to ensure Startup
    //

    @Override
    public void configure() throws Exception {
        String processingPlantName = getClass().getSimpleName();

        from("timer://"+processingPlantName+"?delay=1000&repeatCount=1")
                .routeId("ProcessingPlant::"+processingPlantName)
                .log(LoggingLevel.DEBUG, "Starting....");
    }

    //
    // Helpers
    //

    protected void waitALittleBit(){
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            getLogger().debug(".waitALittleBit():...{}, {}", ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
        }
    }

    public String getRoomId(String alias){
        if(roomIdMap.containsKey(alias)){
            return(roomIdMap.get(alias));
        }
        List<SynapseRoom> currentStateRoomList = getCurrentStateRoomList();
        for(SynapseRoom currentRoom: currentStateRoomList){
            if(StringUtils.isNotEmpty(currentRoom.getCanonicalAlias())){
                String aliasFromRoom = currentRoom.getCanonicalAlias();
                if(aliasFromRoom.contains(alias)){
                    roomIdMap.put(alias, currentRoom.getRoomID());
                    return(currentRoom.getRoomID());
                }
            }
        }
        return(null);
    }

    public List<SynapseRoom> getCurrentStateRoomList(){
        getLogger().debug(".getCurrentStateRoomList(): [Synchronise Room List] Start...");
        Long listAge = Instant.now().getEpochSecond() - this.lastRoomListUpdate.getEpochSecond();
        if(listAge > 10) {
            List<SynapseRoom> newRoomList = synapseRoomAPI.getRooms("*");
            getLogger().trace(".getCurrentStateRoomList(): [Synchronise Room List] RoomList->{}", newRoomList);
            this.roomList.clear();
            this.roomList.addAll(newRoomList);
            this.lastRoomListUpdate = Instant.now();
            getLogger().debug(".getCurrentStateRoomList(): [Synchronise Room List] Finish...");
        }
        return(roomList);
    }
}
