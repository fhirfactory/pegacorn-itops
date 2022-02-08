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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.reports.tasks;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixInstantMessageMethods;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.common.MAPIResponse;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseRoomMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsTaskReportsDM;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.ParticipantRoomIdentityFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.reports.tasks.ParticipantTaskReportsEventFactory;
import org.apache.camel.LoggingLevel;
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
public class ParticipantTaskReportsIntoReplica extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantTaskReportsIntoReplica.class);

    private boolean initialised;

    private boolean stillRunning;

    private Long CONTENT_FORWARDER_STARTUP_DELAY = 120000L;
    private Long CONTENT_FORWARDER_REFRESH_PERIOD = 15000L;

    List<SynapseRoom> roomList;
    Instant lastRoomListUpdate;
    ConcurrentHashMap<String, String> roomIdMap;

    @Inject
    private MatrixInstantMessageMethods matrixInstantMessageAPI;

    @Inject
    private SynapseRoomMethods synapseRoomAPI;

    @Inject
    private MatrixAccessToken matrixAccessToken;

    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    @Inject
    private ParticipantTaskReportsEventFactory taskReportEventFactory;

    @Inject
    private ITOpsTaskReportsDM taskReportsDM;

    //
    // Constructor(s)
    //

    public ParticipantTaskReportsIntoReplica() {
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
    public void initialise() {
        getLogger().debug(".initialise(): Entry");
        if (initialised) {
            getLogger().debug(".initialise(): Exit, already initialised, nothing to do");
            return;
        }
        getLogger().info(".initialise(): Initialisation Start...");

        scheduleTaskReportFowarderDaemon();

        this.initialised = true;

        getLogger().info(".initialise(): Initialisation Finish...");
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger() {
        return (LOG);
    }

    protected boolean isStillRunning() {
        return (this.stillRunning);
    }

    //
    // Scheduler
    //

    private void scheduleTaskReportFowarderDaemon() {
        getLogger().debug(".scheduleTaskReportFowarderDaemon(): Entry");
        TimerTask taskReportForwarderDaemonTask = new TimerTask() {
            public void run() {
                getLogger().debug(".taskReportForwarderDaemonTask(): Entry");
                if (!isStillRunning()) {
                    taskReportForward();
                }
                getLogger().debug(".taskReportForwarderDaemonTask(): Exit");
            }
        };
        Timer timer = new Timer("TaskReportForwarderDaemonTimer");
        timer.schedule(taskReportForwarderDaemonTask, CONTENT_FORWARDER_STARTUP_DELAY, CONTENT_FORWARDER_REFRESH_PERIOD);
        getLogger().debug(".scheduleTaskReportFowarderDaemon(): Exit");
    }

    //
    // Content Forwarder
    //

    private void taskReportForward() {
        getLogger().debug(".taskReportForward(): Entry");
        stillRunning = true;
        while (taskReportsDM.hasMoreTaskReports()) {
            getLogger().trace(".taskReportForward(): Entry");
            PetasosComponentITOpsNotification nextNotification = taskReportsDM.getNextTaskReport();
            switch (nextNotification.getComponentType()) {
                case PETASOS_MONITORED_COMPONENT_SUBSYSTEM:
                    getLogger().trace(".notificationForwarder(): Processing ProcessorPlant Metrics");
                    forwardProcessingPlantTaskReport(nextNotification);
                    break;
                case PETASOS_MONITORED_COMPONENT_SERVICE:
                    break;
                case PETASOS_MONITORED_COMPONENT_PROCESSING_PLANT:
                    getLogger().trace(".taskReportForward(): Processing ProcessorPlant Metrics");
                    forwardProcessingPlantTaskReport(nextNotification);
                    break;
                case PETASOS_MONITORED_COMPONENT_WORKSHOP:
                    break;
                case PETASOS_MONITORED_COMPONENT_WORK_UNIT_PROCESSOR:
                    getLogger().trace(".taskReportForward(): Processing WorkUnitProcessor Metrics");
                    forwardWUPTaskReport(nextNotification);
                    break;
                case PETASOS_MONITORED_COMPONENT_WORK_UNIT_PROCESSOR_COMPONENT:
                    break;
                case PETASOS_MONITORED_COMPONENT_ENDPOINT:
                    break;
            }
            waitALittleBit();
        }
        stillRunning = false;
        getLogger().debug(".notificationForwarder(): Exit");
    }

    //
    // Per Metric/Reporting Type Helpers
    //

    private void forwardWUPTaskReport(PetasosComponentITOpsNotification notification) {
        getLogger().info(".forwardWUPTaskReport(): Entry, notification->{}", notification);

        String roomAlias = roomIdentityFactory.buildWUPRoomCanonicalAlias(
                notification.getParticipantName(),
                OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_TASKS);

        getLogger().trace(".forwardWUPTaskReport(): roomAlias for Events->{}", roomAlias);

        String roomIdFromAlias = getRoomId(roomAlias);

        getLogger().trace(".forwardWUPTaskReport(): roomId for Events->{}", roomIdFromAlias);

        if (roomIdFromAlias != null) {

            MRoomTextMessageEvent notificationEvent = taskReportEventFactory.newTaskReportEvent(roomIdFromAlias, notification);

            try {
                MAPIResponse mapiResponse = matrixInstantMessageAPI.postTextMessage(roomIdFromAlias, matrixAccessToken.getUserId(), notificationEvent);
            } catch (Exception ex) {
                getLogger().warn(".forwardWUPTaskReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            }

        } else {
            getLogger().warn(".forwardWUPTaskReport(): No room to forward work unit processor task reports into (WorkUnitProcessor->{})!", notification.getParticipantName());
            // TODO either re-queue or send to DeadLetter
        }
    }

    private void forwardProcessingPlantTaskReport(PetasosComponentITOpsNotification notification) {
        getLogger().debug(".forwardProcessingPlantTaskReport(): Entry, notification->{}", notification);

        String roomAlias = roomIdentityFactory.buildProcessingPlantCanonicalAlias(notification.getParticipantName(), OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_TASKS);

        getLogger().trace(".forwardProcessingPlantTaskReport(): roomAlias for Events->{}", roomAlias);

        String roomIdFromAlias = getRoomId(roomAlias);

        getLogger().trace(".forwardProcessingPlantTaskReport(): roomId for Events->{}", roomIdFromAlias);

        if (roomIdFromAlias != null) {
            MRoomTextMessageEvent notificationEvent = taskReportEventFactory.newTaskReportEvent(roomIdFromAlias, notification);

            try {
                MAPIResponse mapiResponse = matrixInstantMessageAPI.postTextMessage(roomIdFromAlias, matrixAccessToken.getUserId(), notificationEvent);
            } catch (Exception ex) {
                getLogger().warn(".forwardProcessingPlantTaskReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            }
        } else {
            getLogger().warn(".forwardProcessingPlantTaskReport(): No room to forward processing plant notifications into (ProcessingPlant->{}!", notification.getParticipantName());
            // TODO either re-queue or send to DeadLetter
        }
    }

    //
    // Mechanism to ensure Startup
    //

    @Override
    public void configure() throws Exception {
        String processingPlantName = getClass().getSimpleName();

        from("timer://" + processingPlantName + "?delay=1000&repeatCount=1")
                .routeId("ProcessingPlant::" + processingPlantName)
                .log(LoggingLevel.DEBUG, "Starting....");
    }

    //
    // Helpers
    //

    protected void waitALittleBit() {
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            getLogger().info(".waitALittleBit():...{}, {}", ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
        }
    }

    public String getRoomId(String alias) {
        if (roomIdMap.containsKey(alias)) {
            return (roomIdMap.get(alias));
        }
        List<SynapseRoom> currentStateRoomList = getCurrentStateRoomList();
        for (SynapseRoom currentRoom : currentStateRoomList) {
            if (StringUtils.isNotEmpty(currentRoom.getCanonicalAlias())) {
                String aliasFromRoom = currentRoom.getCanonicalAlias();
                if (aliasFromRoom.contains(alias)) {
                    roomIdMap.put(alias, currentRoom.getRoomID());
                    return (currentRoom.getRoomID());
                }
            }
        }
        return (null);
    }

    public List<SynapseRoom> getCurrentStateRoomList() {
        getLogger().debug(".getCurrentStateRoomList(): [Synchronise Room List] Start...");
        Long listAge = Instant.now().getEpochSecond() - this.lastRoomListUpdate.getEpochSecond();
        if (listAge > 10) {
            List<SynapseRoom> newRoomList = synapseRoomAPI.getRooms("*");
            getLogger().trace(".getCurrentStateRoomList(): [Synchronise Room List] RoomList->{}", newRoomList);
            this.roomList.clear();
            this.roomList.addAll(newRoomList);
            this.lastRoomListUpdate = Instant.now();
            getLogger().debug(".getCurrentStateRoomList(): [Synchronise Room List] Finish...");
        }
        return (roomList);
    }
}
