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

import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.common.MAPIResponse;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.valuesets.PetasosComponentITOpsNotificationTypeEnum;
import net.fhirfactory.pegacorn.itops.im.common.ITOpsIMNames;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsNotificationsDM;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.common.OAMRoomMessageInjectorBase;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.notifications.ParticipantNotificationEventFactory;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@ApplicationScoped
public class ParticipantNotificationsIntoReplica extends OAMRoomMessageInjectorBase {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantNotificationsIntoReplica.class);

    private boolean initialised;

    private boolean stillRunning;

    private Long CONTENT_FORWARDER_STARTUP_DELAY = 120000L;
    private Long CONTENT_FORWARDER_REFRESH_PERIOD = 15000L;

    @Inject
    private ITOpsIMNames itOpsIMNames;

    @Inject
    private ParticipantNotificationEventFactory notificationEventFactory;

    @Inject
    private ITOpsNotificationsDM notificationsDM;

    @Inject
    private ProducerTemplate camelRouteInjector;

    //
    // Constructor(s)
    //

    public ParticipantNotificationsIntoReplica(){
        super();
        this.initialised = false;
        this.stillRunning = false;
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
                        getLogger().debug(".notificationForwarder(): Is Failure, generating Email/SMS Message");
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
            String roomAlias = getRoomIdentityFactory().buildWUPRoomPseudoAlias(
                    notification.getParticipantName(),
                    OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_CONSOLE);

            getLogger().trace(".forwardWUPNotification(): roomAlias for Events->{}", roomAlias);

            String roomIdFromAlias =  getRoomIdFromPseudoAlias(roomAlias);

            getLogger().trace(".forwardWUPNotification(): roomId for Events->{}", roomIdFromAlias);

            if(roomIdFromAlias != null) {

                MRoomTextMessageEvent notificationEvent = notificationEventFactory.newNotificationEvent(roomIdFromAlias, notification);

                try {
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), notificationEvent);
                    return(true);
                } catch(Exception ex){
                    getLogger().warn(".forwardWUPNotification(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                    return(false);
                }
            } else {
                getLogger().warn(".forwardWUPNotification(): No room to forward work unit processor notifications into (WorkUnitProcessor->{}), ITOps Room Pseudo Alias ->{}", notification.getParticipantName(), roomAlias);
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

        try {
            String roomAlias = getRoomIdentityFactory().buildProcessingPlantRoomPseudoAlias(notification.getParticipantName(), OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_CONSOLE);

            getLogger().trace(".forwardProcessingPlantNotification(): roomAlias for Events->{}", roomAlias);

            String roomIdFromAlias =  getRoomIdFromPseudoAlias(roomAlias);

            getLogger().trace(".forwardProcessingPlantNotification(): roomId for Events->{}", roomIdFromAlias);

            if (roomIdFromAlias != null) {
                MRoomTextMessageEvent notificationEvent = notificationEventFactory.newNotificationEvent(roomIdFromAlias, notification);

                try {
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), notificationEvent);
                    return (true);
                } catch (Exception ex) {
                    getLogger().warn(".forwardProcessingPlantNotification(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                    return (false);
                }
            } else {
                getLogger().warn(".forwardProcessingPlantNotification(): No room to forward processing plant notifications into (ProcessingPlant->{}), ITOps Room Pseudo Alias->{}", notification.getParticipantName(), roomAlias);
                // TODO either re-queue or send to DeadLetter
                return (false);
            }
        } catch(Exception ex){
            getLogger().warn(".forwardProcessingPlantNotification(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return(false);
        }
    }

    private boolean forwardEndpointNotification(PetasosComponentITOpsNotification notification){
        getLogger().debug(".forwardEndpointNotification(): Entry, notification->{}", notification);

        String roomAlias = null;
        try {

            roomAlias = getRoomIdentityFactory().buildEndpointRoomPseudoAlias(notification.getParticipantName(), OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_CONSOLE);

            getLogger().debug(".forwardEndpointNotification(): roomAlias for Events->{}", roomAlias);

            String roomIdFromAlias = getRoomIdFromPseudoAlias(roomAlias);

            getLogger().debug(".forwardEndpointNotification(): roomId for Events->{}", roomIdFromAlias);

            if (roomIdFromAlias != null) {
                MRoomTextMessageEvent notificationEvent = notificationEventFactory.newNotificationEvent(roomIdFromAlias, notification);
                try {
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), notificationEvent);
                    getLogger().debug(".forwardEndpointNotification(): notification sent!");
                    return (true);
                } catch (Exception ex) {
                    getLogger().warn(".forwardEndpointNotification(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                    return (false);
                }
            } else {
                getLogger().warn(".forwardEndpointNotification(): No room to forward endpoint notifications into (EndpointRoom->{}), ITOps Room Pseudo Alias->{}", notification.getParticipantName(), roomAlias);
                return (false);
            }
        } catch(Exception ex){
            getLogger().warn(".forwardEndpointNotification(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return(false);
        }
    }

}
