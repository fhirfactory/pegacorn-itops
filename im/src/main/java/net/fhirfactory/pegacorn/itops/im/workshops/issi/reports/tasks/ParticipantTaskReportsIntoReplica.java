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

import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.common.MAPIResponse;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsTaskReportsDM;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.common.OAMRoomMessageInjectorBase;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.reports.tasks.ParticipantTaskReportsEventFactory;
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
public class ParticipantTaskReportsIntoReplica extends OAMRoomMessageInjectorBase {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantTaskReportsIntoReplica.class);

    private boolean initialised;

    private boolean stillRunning;

    private Long CONTENT_FORWARDER_STARTUP_DELAY = 120000L;
    private Long CONTENT_FORWARDER_REFRESH_PERIOD = 15000L;

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
        List<PetasosComponentITOpsNotification> failedToSendList = new ArrayList<>();
        while (taskReportsDM.hasMoreTaskReports()) {
            getLogger().trace(".taskReportForward(): Entry");
            PetasosComponentITOpsNotification nextNotification = taskReportsDM.getNextTaskReport();
            boolean successfullySent = false;
            switch (nextNotification.getComponentType()) {
                case PETASOS_MONITORED_COMPONENT_SUBSYSTEM:
                    getLogger().trace(".notificationForwarder(): Processing ProcessorPlant Metrics");
                    successfullySent = forwardProcessingPlantTaskReport(nextNotification);
                    break;
                case PETASOS_MONITORED_COMPONENT_SERVICE:
                    break;
                case PETASOS_MONITORED_COMPONENT_PROCESSING_PLANT:
                    getLogger().trace(".taskReportForward(): Processing ProcessorPlant Metrics");
                    successfullySent = forwardProcessingPlantTaskReport(nextNotification);
                    break;
                case PETASOS_MONITORED_COMPONENT_WORKSHOP:
                    break;
                case PETASOS_MONITORED_COMPONENT_WORK_UNIT_PROCESSOR:
                    getLogger().trace(".taskReportForward(): Processing WorkUnitProcessor Metrics");
                    successfullySent = forwardWUPTaskReport(nextNotification);
                    break;
                case PETASOS_MONITORED_COMPONENT_WORK_UNIT_PROCESSOR_COMPONENT:
                    break;
                case PETASOS_MONITORED_COMPONENT_ENDPOINT:
                    getLogger().trace(".taskReportForward(): Processing Endpoint Metrics");
                    successfullySent = forwardEndpointTaskReport(nextNotification);
                    break;
            }
            if(!successfullySent){
                failedToSendList.add(nextNotification);
            }
        }
        for(PetasosComponentITOpsNotification currentNotification: failedToSendList){
            taskReportsDM.addTaskReport(currentNotification);
        }
        stillRunning = false;
        getLogger().debug(".notificationForwarder(): Exit");
    }

    //
    // Per Metric/Reporting Type Helpers
    //

    private boolean forwardEndpointTaskReport(PetasosComponentITOpsNotification notification) {
        getLogger().debug(".forwardEndpointTaskReport(): Entry");
        getLogger().trace(".forwardEndpointTaskReport(): notification->{}", notification);

        try {

            String roomAlias = getRoomIdentityFactory().buildEndpointRoomPseudoAlias(
                    notification.getParticipantName(),
                    OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_TASKS);

            getLogger().trace(".forwardEndpointTaskReport(): roomAlias for Events->{}", roomAlias);

            String roomIdFromAlias = getRoomIdFromPseudoAlias(roomAlias);

            getLogger().trace(".forwardEndpointTaskReport(): roomId for Events->{}", roomIdFromAlias);

            if (roomIdFromAlias != null) {
                MRoomTextMessageEvent notificationEvent = taskReportEventFactory.newTaskReportEvent(roomIdFromAlias, notification);
                try {
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), notificationEvent);
                    return(true);
                } catch(Exception ex){
                    getLogger().warn(".forwardEndpointTaskReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                    return(false);
                }
            } else {
                getLogger().warn(".forwardEndpointTaskReport(): No room to forward endpoint task reports into (Endpoint->{})!", notification.getParticipantName());
                return(false);
            }
        } catch (Exception ex) {
            getLogger().warn(".forwardEndpointTaskReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return(false);
        }
    }

    private boolean forwardWUPTaskReport(PetasosComponentITOpsNotification notification) {
        getLogger().debug(".forwardEndpointTaskReport(): notification->{}", notification);

        try {

            String roomAlias = getRoomIdentityFactory().buildWUPRoomPseudoAlias(
                    notification.getParticipantName(),
                    OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_TASKS);

            getLogger().trace(".forwardWUPTaskReport(): roomAlias for Events->{}", roomAlias);

            String roomIdFromAlias =  getRoomIdFromPseudoAlias(roomAlias);

            getLogger().trace(".forwardWUPTaskReport(): roomId for Events->{}", roomIdFromAlias);

            if (roomIdFromAlias != null) {
                MRoomTextMessageEvent notificationEvent = taskReportEventFactory.newTaskReportEvent(roomIdFromAlias, notification);
                try{
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), notificationEvent);
                    return(true);
                } catch(Exception ex){
                    getLogger().warn(".forwardWUPTaskReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                    return(false);
                }
            } else {
                getLogger().warn(".forwardWUPTaskReport(): No room to forward work unit processor task reports into (WorkUnitProcessor->{})!", notification.getParticipantName());
                return(false);
            }
        } catch (Exception ex) {
            getLogger().warn(".forwardWUPTaskReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return(false);
        }
    }

    private boolean forwardProcessingPlantTaskReport(PetasosComponentITOpsNotification notification) {
        getLogger().debug(".forwardProcessingPlantTaskReport(): Entry, notification->{}", notification);

        try {
            String roomAlias = getRoomIdentityFactory().buildProcessingPlantRoomPseudoAlias(notification.getParticipantName(), OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_TASKS);

            getLogger().trace(".forwardProcessingPlantTaskReport(): roomAlias for Events->{}", roomAlias);

            String roomIdFromAlias =  getRoomIdFromPseudoAlias(roomAlias);

            getLogger().trace(".forwardProcessingPlantTaskReport(): roomId for Events->{}", roomIdFromAlias);

            if (roomIdFromAlias != null) {
                MRoomTextMessageEvent notificationEvent = taskReportEventFactory.newTaskReportEvent(roomIdFromAlias, notification);
                try {
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), notificationEvent);
                    return(true);
                } catch(Exception ex){
                    getLogger().warn(".forwardProcessingPlantTaskReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                    return(false);
                }
            } else {
                getLogger().warn(".forwardProcessingPlantTaskReport(): No room to forward processing plant notifications into (ProcessingPlant->{}!", notification.getParticipantName());
                return(false);
            }
        } catch (Exception ex) {
            getLogger().warn(".forwardProcessingPlantTaskReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return(false);
        }
    }
}
