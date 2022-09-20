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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.metrics;

import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.common.MAPIResponse;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.communicate.synapse.credentials.SynapseAdminAccessToken;
import net.fhirfactory.dricats.model.petasos.oam.metrics.reporting.PetasosComponentMetricSet;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.common.OAMRoomMessageInjectorBase;
import net.fhirfactory.pegacorn.itops.im.workshops.oam.ITOpsIMMetricsProcessor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@ApplicationScoped
public class ParticipantMetricsReportingIntoReplica extends OAMRoomMessageInjectorBase {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantMetricsReportingIntoReplica.class);

    private boolean initialised;

    private Long CONTENT_FORWARDER_STARTUP_DELAY = 180000L;
    private Long CONTENT_FORWARDER_REFRESH_PERIOD = 30000L;

    @Inject
    private ITOpsIMMetricsProcessor localMetricsProcessor;

    //
    // Constructor(s)
    //

    public ParticipantMetricsReportingIntoReplica(){
        super();
        this.initialised = false;
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

        scheduleReportsAndMetricsForwarderDaemon();

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
        return(getSynapseAccessToken());
    }

    //
    // Scheduler
    //

    private void scheduleReportsAndMetricsForwarderDaemon() {
        getLogger().debug(".scheduleReportsAndMetricsForwarderDaemon(): Entry");
        TimerTask reportsAndMetricsForwardDaemon = new TimerTask() {
            public void run() {
                getLogger().debug(".reportsAndMetricsForwardDaemon(): Entry");
                reportsAndMetricsForwarder();
                getLogger().debug(".reportsAndMetricsForwardDaemon(): Exit");
            }
        };
        Timer timer = new Timer("ReportsAndMetricsForwarderTimer");
        timer.schedule(reportsAndMetricsForwardDaemon, CONTENT_FORWARDER_STARTUP_DELAY, CONTENT_FORWARDER_REFRESH_PERIOD);
        getLogger().debug(".scheduleReportsAndMetricsForwarderDaemon(): Exit");
    }

    //
    // Content Forwarder
    //

    private void reportsAndMetricsForwarder(){
        getLogger().debug(".reportsAndMetricsForwarder(): Entry");

        //
        // Process Local Metrics 1st
        localMetricsProcessor.captureLocalMetrics();
        localMetricsProcessor.forwardLocalMetricsToServer();

        //
        // Now Process All Metrics
        List<PetasosComponentMetricSet> metricSets = getSystemWideMetricsCache().getUpdatedMetricSets();
        for(PetasosComponentMetricSet currentMetricSet: metricSets){
            if(getLogger().isDebugEnabled()) {
                getLogger().debug(".reportsAndMetricsForwarder(): Processing Metrics From -->{}", currentMetricSet.getSourceParticipantName());
            }
            switch (currentMetricSet.getComponentType()) {
                case PETASOS_MONITORED_COMPONENT_SUBSYSTEM:
                    break;
                case PETASOS_MONITORED_COMPONENT_SERVICE:
                    break;
                case PETASOS_MONITORED_COMPONENT_PROCESSING_PLANT:
                    getLogger().trace(".reportsAndMetricsForwarder(): Processing ProcessorPlant Metrics");
                    forwardProcessingPlantMetrics(currentMetricSet);
                    break;
                case PETASOS_MONITORED_COMPONENT_WORKSHOP:
                    break;
                case PETASOS_MONITORED_COMPONENT_WORK_UNIT_PROCESSOR:
                    getLogger().trace(".reportsAndMetricsForwarder(): Processing WorkUnitProcessor Metrics");
                    forwardWUPMetrics(currentMetricSet);
                    break;
                case PETASOS_MONITORED_COMPONENT_WORK_UNIT_PROCESSOR_COMPONENT:
                    break;
                case PETASOS_MONITORED_COMPONENT_ENDPOINT:
                    getLogger().trace(".reportsAndMetricsForwarder(): Processing Endpoint Metrics");
                    forwardEndpointMetrics(currentMetricSet);
                    break;
            }
        }
    }

    //
    // Per Metric/Reporting Type Helpers
    //

    private void forwardWUPMetrics(PetasosComponentMetricSet wupMetricSet){
        getLogger().debug(".forwardWUPMetrics(): Entry, wupMetricSet->{}", wupMetricSet);

        try{

            String roomAlias = getRoomIdentityFactory().buildWUPRoomPseudoAlias(
                    wupMetricSet.getSourceParticipantName(),
                    OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_METRICS);

            getLogger().trace(".forwardWUPMetrics(): roomAlias for Metric->{}", roomAlias);

            String roomIdFromAlias = getRoomIdFromPseudoAlias(roomAlias);

            getLogger().trace(".forwardWUPMetrics(): roomId for Metric->{}", roomIdFromAlias);

            if(roomIdFromAlias != null) {

                List<MRoomTextMessageEvent> metricsEventSet = getMetricsReportEventFactory().createWorkUnitProcessorMetricsEvent(roomIdFromAlias, wupMetricSet);

                for (MRoomTextMessageEvent currentEvent : metricsEventSet) {
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), currentEvent);
                    getLogger().debug(".forwardWUPMetrics(): Metrics Forwarded, mapiResponse->{}", mapiResponse);
                }
            } else {
                getLogger().warn(".forwardWUPMetrics(): No room to forward work unit processor metrics into (WorkUnitProcessor->{}!", wupMetricSet.getMetricSourceComponentId());
                // TODO either re-queue or send to DeadLetter
            }
        } catch (Exception ex) {
            getLogger().warn(".forwardWUPMetrics(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
        getLogger().debug(".forwardWUPMetrics(): Exit");
    }

    private void forwardProcessingPlantMetrics(PetasosComponentMetricSet metricSet){
        getLogger().debug(".forwardProcessingPlantMetrics(): Entry, metricSet->{}", metricSet);

        try{
            String roomAlias = getRoomIdentityFactory().buildProcessingPlantRoomPseudoAlias(metricSet.getSourceParticipantName(), OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_METRICS);

            getLogger().trace(".forwardProcessingPlantMetrics(): roomAlias for Metric->{}", roomAlias);

            String roomIdFromAlias = getRoomIdFromPseudoAlias(roomAlias);

            getLogger().trace(".forwardProcessingPlantMetrics(): roomId for Metric->{}", roomIdFromAlias);

            if(roomIdFromAlias != null) {

                List<MRoomTextMessageEvent> metricsEventSet = getMetricsReportEventFactory().createProcessingPlantMetricsEvent(roomIdFromAlias, metricSet);

                for (MRoomTextMessageEvent currentEvent : metricsEventSet) {
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), currentEvent);
                    getLogger().debug(".forwardProcessingPlantMetrics(): Metrics Forwarded, mapiResponse->{}", mapiResponse);
                }
            } else {
                getLogger().warn(".forwardProcessingPlantMetrics(): No room to forward processing plant metrics into (ProcessingPlant->{}!", metricSet.getMetricSourceComponentId());
                // TODO either re-queue or send to DeadLetter
            }
        } catch (Exception ex) {
                getLogger().warn(".forwardProcessingPlantMetrics(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
        getLogger().debug(".forwardProcessingPlantMetrics(): Exit");
    }

    private void forwardEndpointMetrics(PetasosComponentMetricSet metricSet){
        getLogger().debug(".forwardEndpointMetrics(): Entry, metricSet->{}", metricSet);

        try {

            String roomAlias = getRoomIdentityFactory().buildEndpointRoomPseudoAlias(metricSet.getSourceParticipantName(), OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_METRICS);

            getLogger().trace(".forwardEndpointMetrics(): roomAlias for Metric->{}", roomAlias);

            String roomIdFromAlias = getRoomIdFromPseudoAlias(roomAlias);

            getLogger().trace(".forwardEndpointMetrics(): roomId for Metric->{}", roomIdFromAlias);

            if (roomIdFromAlias != null) {

                List<MRoomTextMessageEvent> metricsEventSet = getMetricsReportEventFactory().createEndpointMetricsEvent(roomIdFromAlias, metricSet);

                for (MRoomTextMessageEvent currentEvent : metricsEventSet) {
                    getLogger().debug(".forwardEndpointMetrics(): Forward Metrics, currentEvent->{}", currentEvent);
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), currentEvent);
                    getLogger().trace(".forwardEndpointMetrics(): Metrics Forwarded, mapiResponse->{}", mapiResponse);
                }
            } else {
                getLogger().warn(".forwardEndpointMetrics(): No room to forward processing plant metrics into (Endpoint->{}!", metricSet.getMetricSourceComponentId());
                // TODO either re-queue or send to DeadLetter
            }
        } catch (Exception ex) {
            getLogger().warn(".forwardEndpointMetrics(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
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
}
