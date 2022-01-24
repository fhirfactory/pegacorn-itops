/*
 * Copyright (c) 2021 Mark A. Hunter (ACT Health)
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
package net.fhirfactory.pegacorn.itops.im.workshops.internalipc.petasos;

import net.fhirfactory.pegacorn.core.interfaces.oam.subscriptions.PetasosSubscriptionReportHandlerInterface;
import net.fhirfactory.pegacorn.core.model.capabilities.base.CapabilityUtilisationRequest;
import net.fhirfactory.pegacorn.core.model.capabilities.base.CapabilityUtilisationResponse;
import net.fhirfactory.pegacorn.core.model.capabilities.valuesets.WorkUnitProcessorCapabilityEnum;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.reporting.PetasosProcessingPlantSubscriptionSummary;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.reporting.PetasosPublisherSubscriptionSummary;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.reporting.PetasosSubscriptionSummaryReport;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.reporting.PetasosWorkUnitProcessorSubscriptionSummary;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.edge.jgroups.JGroupsIntegrationPointSummary;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsSystemWideSubscriptionMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.internalipc.petasos.common.ITOpsReceiverBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

@ApplicationScoped
public class ITOpsPubSubReportReceiver extends ITOpsReceiverBase implements PetasosSubscriptionReportHandlerInterface{
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsPubSubReportReceiver.class);

    @Inject
    private ITOpsSystemWideSubscriptionMapDM subscriptionMapDM;

    @Override
    protected void registerCapabilities(){
        getProcessingPlant().registerCapabilityFulfillmentService(WorkUnitProcessorCapabilityEnum.CAPABILITY_INFORMATION_MANAGEMENT_IT_OPS.getToken(), this);
    }

    @Override
    public CapabilityUtilisationResponse executeTask(CapabilityUtilisationRequest request) {
        getLogger().info(".executeTask(): Entry, request->{}", request);
        if(request.getRequiredCapabilityName().contentEquals(WorkUnitProcessorCapabilityEnum.CAPABILITY_INFORMATION_MANAGEMENT_IT_OPS.getToken())) {
            PetasosPublisherSubscriptionSummary pubsubReport = extractPubSubReport(request);
            if (pubsubReport != null) {
                CapabilityUtilisationResponse response = new CapabilityUtilisationResponse();
                response.setInstantCompleted(Instant.now());
                response.setSuccessful(true);
                response.setAssociatedRequestID(request.getRequestID());
                response.setResponseContent("OK");
                getLogger().debug(".executeTask(): Exit, reponse->{}", response);
                return (response);
            }
        }
        CapabilityUtilisationResponse response = generateBadResponse(request.getRequestID());
        getLogger().debug(".executeTask(): Exit, reponse->{}", response);
        return (response);
    }

    @Override
    protected Logger getLogger(){
        return(LOG);
    }

    protected PetasosPublisherSubscriptionSummary extractPubSubReport(CapabilityUtilisationRequest request){
        getLogger().debug(".extractPubSubReport(): Entry, request->{}", request);
        PetasosPublisherSubscriptionSummary report = null;
        getLogger().debug(".extractPubSubReport(): Exit, report->{}", report);
        return(report);
    }

    /*
    private void updateLocalCache(ITOpsPubSubReport report){
        getLogger().debug(".updateLocalCache(): Entry");
        for(ProcessingPlantSubscriptionSummary currentSummary: report.getProcessingPlantSubscriptionSummarySet().values()){
            getLogger().trace(".updateLocalCache(): Updating->{}", currentSummary.getComponentID());
            pubsubMapDM.addProcessingPlantSubscriptionSummary(currentSummary);
        }
        for(WorkUnitProcessorSubscriptionSummary currentSummary: report.getWupSubscriptionSummarySet().values()){
            getLogger().trace(".updateLocalCache(): Updating->{}", currentSummary.getComponentID());
            pubsubMapDM.addWorkUnitProcessorSubscriptionSummary(currentSummary);
        }
        getLogger().debug(".updateLocalCache(): Exit");
    }

     */

    @Override
    public Instant replicateSubscriptionSummaryReportHandler(PetasosSubscriptionSummaryReport summaryReport, JGroupsIntegrationPointSummary integrationPoint) {
        getLogger().info(".replicateSubscriptionSummaryReportHandler(): Entry");
        if(summaryReport != null){
            for(PetasosWorkUnitProcessorSubscriptionSummary wupSummary: summaryReport.getWupSubscriptionSummarySet().values()){
                subscriptionMapDM.addWorkUnitProcessorSubscriptionSummary(wupSummary);
            }
            for(PetasosProcessingPlantSubscriptionSummary processingPlantSummary: summaryReport.getProcessingPlantSubscriptionSummarySet().values()){
                subscriptionMapDM.addProcessingPlantSubscriptionSummary(processingPlantSummary);
            }
        }
        getLogger().info(".replicateSubscriptionSummaryReportHandler(): Exit");
        return(Instant.now());
    }

    //
    // Update Notification Service
    //

    @Override
    protected void cacheMonitorProcess() {

    }

    @Override
    protected String cacheMonitorProcessTimerName() {
        return ("SubscriptionNotificationServiceTimer");
    }
}
