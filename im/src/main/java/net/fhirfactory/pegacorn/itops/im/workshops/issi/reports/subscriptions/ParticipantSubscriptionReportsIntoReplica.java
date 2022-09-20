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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.reports.subscriptions;

import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.common.MAPIResponse;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.dricats.model.petasos.oam.subscriptions.reporting.PetasosProcessingPlantSubscriptionSummary;
import net.fhirfactory.dricats.model.petasos.oam.subscriptions.reporting.PetasosPublisherSubscriptionSummary;
import net.fhirfactory.dricats.model.petasos.oam.subscriptions.reporting.PetasosSubscriberSubscriptionSummary;
import net.fhirfactory.dricats.model.petasos.oam.subscriptions.reporting.PetasosWorkUnitProcessorSubscriptionSummary;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsSystemWideSubscriptionMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.common.OAMRoomMessageInjectorBase;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.reports.subscriptions.ParticipantSubscriptionReportEventFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@ApplicationScoped
public class ParticipantSubscriptionReportsIntoReplica extends OAMRoomMessageInjectorBase {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantSubscriptionReportsIntoReplica.class);

    private boolean initialised;

    private boolean stillRunning;

    private Long CONTENT_FORWARDER_STARTUP_DELAY = 120000L;
    private Long CONTENT_FORWARDER_REFRESH_PERIOD = 15000L;

    @Inject
    private ParticipantSubscriptionReportEventFactory subscriptionReportEventFactory;

    @Inject
    private ITOpsSystemWideSubscriptionMapDM subscriptionMapDM;

    //
    // Constructor(s)
    //

    public ParticipantSubscriptionReportsIntoReplica() {
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

        scheduleSubscriptionReportForwarderDaemon();

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

    private void scheduleSubscriptionReportForwarderDaemon() {
        getLogger().debug(".scheduleSubscriptionReportForwarderDaemon(): Entry");
        TimerTask subscriptionReportForwarderDaemonTask = new TimerTask() {
            public void run() {
                getLogger().debug(".subscriptionReportForwarderDaemonTask(): Entry");
                if (!isStillRunning()) {
                    subscriptionReportForwarder();
                }
                getLogger().debug(".subscriptionReportForwarderDaemonTask(): Exit");
            }
        };
        Timer timer = new Timer("SubscriptionReportForwarderDaemonTimer");
        timer.schedule(subscriptionReportForwarderDaemonTask, CONTENT_FORWARDER_STARTUP_DELAY, CONTENT_FORWARDER_REFRESH_PERIOD);
        getLogger().debug(".scheduleSubscriptionReportForwarderDaemon(): Exit");
    }

    //
    // Content Forwarder
    //

    private void subscriptionReportForwarder() {
        getLogger().debug(".subscriptionReportForwarder(): Entry");
        stillRunning = true;
        if(subscriptionMapDM.isUpdated()) {
        	getLogger().trace(".subscriptionReportForwarder(): is updating subscriptions");
	        List<PetasosProcessingPlantSubscriptionSummary> processingPlantSubscriptionSummaries = subscriptionMapDM.getProcessingPlantSubscriptionSummaries();
	        for (PetasosProcessingPlantSubscriptionSummary currentReport: processingPlantSubscriptionSummaries) {
	            getLogger().trace(".subscriptionReportForwarder(): Entry");
	            forwardProcessingPlantSubscriptionReport(currentReport);
	        }
	        List<PetasosWorkUnitProcessorSubscriptionSummary> wupSubscriptionSummaries = subscriptionMapDM.getWorkUnitProcessorSubscriptionSummaries();
	        for (PetasosWorkUnitProcessorSubscriptionSummary currentReport: wupSubscriptionSummaries) {
	            getLogger().trace(".subscriptionReportForwarder(): Entry");
	            forwardWorkUnitProcessorSubscriptionReport(currentReport);
	        }
	        subscriptionMapDM.setUpdated(false);
        }
        stillRunning = false;
        getLogger().debug(".notificationForwarder(): Exit");
    }

    //
    // Per Metric/Reporting Type Helpers
    //

    private void forwardProcessingPlantSubscriptionReport(PetasosProcessingPlantSubscriptionSummary subscriptionSummary) {
        getLogger().debug(".forwardProcessingPlantSubscriptionReport(): Entry, subscriptionSummary->{}", subscriptionSummary);

        try {

            String roomAlias = getRoomIdentityFactory().buildProcessingPlantRoomPseudoAlias(subscriptionSummary.getParticipantName(), OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_SUBSCRIPTIONS);

            getLogger().trace(".forwardProcessingPlantSubscriptionReport(): roomAlias for Events->{}", roomAlias);

            String roomIdFromAlias = getRoomIdFromPseudoAlias(roomAlias);

            getLogger().trace(".forwardProcessingPlantSubscriptionReport(): roomId for Events->{}", roomIdFromAlias);

            if (roomIdFromAlias != null) {

                Collection<PetasosSubscriberSubscriptionSummary> asASubscriberValues = subscriptionSummary.getAsSubscriber().values();
                MRoomTextMessageEvent subscriberSummaryEvent = subscriptionReportEventFactory.newAsASubscriberSubscriptionReportEvent(roomIdFromAlias, asASubscriberValues);
                getLogger().trace(".forwardProcessingPlantSubscriptionReport(): subscriberSummaryEvent->{}", subscriberSummaryEvent);
                if (subscriberSummaryEvent != null) {
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), subscriberSummaryEvent);
                }

                Collection<PetasosPublisherSubscriptionSummary> asAPublisherValues = subscriptionSummary.getAsPublisher().values();
                MRoomTextMessageEvent publisherSummaryEvent = subscriptionReportEventFactory.newAsAPublisherSubscriptionReportEvent(roomIdFromAlias, asAPublisherValues);
                getLogger().trace(".forwardProcessingPlantSubscriptionReport(): publisherSummaryEvent->{}", publisherSummaryEvent);
                if (publisherSummaryEvent != null) {
                    try {
                        MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), publisherSummaryEvent);
                    } catch (Exception ex) {
                        getLogger().warn(".forwardProcessingPlantSubscriptionReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                    }
                }

            } else {
                getLogger().warn(".forwardWUPTaskReport(): No room to forward work unit processor task reports into (WorkUnitProcessor->{})!", subscriptionSummary.getParticipantName());
                // TODO either re-queue or send to DeadLetter
            }
        }
        catch (Exception ex) {
            getLogger().warn(".forwardProcessingPlantSubscriptionReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
    }

    private void forwardWorkUnitProcessorSubscriptionReport(PetasosWorkUnitProcessorSubscriptionSummary subscriptionSummary) {
        getLogger().debug(".forwardWorkUnitProcessorSubscriptionReport(): Entry, subscriptionSummary->{}", subscriptionSummary);

        try{

            String roomAlias = getRoomIdentityFactory().buildWUPRoomPseudoAlias(subscriptionSummary.getParticipantName(),OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_SUBSCRIPTIONS);

            getLogger().trace(".forwardWorkUnitProcessorSubscriptionReport(): roomAlias for Events->{}", roomAlias);

            String roomIdFromAlias = getRoomIdFromPseudoAlias(roomAlias);

            getLogger().trace(".forwardWorkUnitProcessorSubscriptionReport(): roomId for Events->{}", roomIdFromAlias);

            if (roomIdFromAlias != null) {

                MRoomTextMessageEvent subscriberSummaryEvent = subscriptionReportEventFactory.newWUPSubscriberSubscriptionReportEvent(roomIdFromAlias, subscriptionSummary);
                getLogger().debug(".forwardWorkUnitProcessorSubscriptionReport(): roomIdFromAlias->{}, subscriberSummaryEvent->{}", roomIdFromAlias, subscriberSummaryEvent);
                if (subscriberSummaryEvent != null) {
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), subscriberSummaryEvent);
                }
            } else {
                getLogger().warn(".forwardWorkUnitProcessorSubscriptionReport(): No room to forward work unit processor task reports into (WorkUnitProcessor->{})!", subscriptionSummary.getParticipantName());
                // TODO either re-queue or send to DeadLetter
            }
        } catch (Exception ex) {
            getLogger().warn(".forwardWorkUnitProcessorSubscriptionReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
    }

}
