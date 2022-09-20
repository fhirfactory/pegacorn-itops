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

import com.fasterxml.jackson.core.JsonProcessingException;
import net.fhirfactory.dricats.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.interfaces.observations.metrics.PetasosMetricsBrokerInterface;
import net.fhirfactory.dricats.interfaces.observations.metrics.PetasosMetricsHandlerInterface;
import net.fhirfactory.dricats.model.capabilities.base.CapabilityUtilisationRequest;
import net.fhirfactory.dricats.model.capabilities.base.CapabilityUtilisationResponse;
import net.fhirfactory.dricats.model.capabilities.valuesets.WorkUnitProcessorCapabilityEnum;
import net.fhirfactory.pegacorn.core.model.componentid.ComponentIdType;
import net.fhirfactory.dricats.model.petasos.dataparcel.DataParcelManifest;
import net.fhirfactory.dricats.model.petasos.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.dricats.model.petasos.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.dricats.model.petasos.dataparcel.valuesets.DataParcelNormalisationStatusEnum;
import net.fhirfactory.dricats.model.petasos.dataparcel.valuesets.DataParcelValidationStatusEnum;
import net.fhirfactory.dricats.model.petasos.dataparcel.valuesets.PolicyEnforcementPointApprovalStatusEnum;
import net.fhirfactory.dricats.model.petasos.oam.metrics.reporting.PetasosComponentMetric;
import net.fhirfactory.dricats.model.petasos.oam.metrics.reporting.PetasosComponentMetricSet;
import net.fhirfactory.dricats.model.petasos.task.PetasosActionableTask;
import net.fhirfactory.dricats.model.petasos.task.datatypes.work.datatypes.ParcelOfWorkType;
import net.fhirfactory.dricats.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.edge.jgroups.JGroupsIntegrationPointSummary;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsSystemWideMetricsDM;
import net.fhirfactory.pegacorn.itops.im.workshops.internalipc.petasos.common.ITOpsReceiverBase;
import net.fhirfactory.pegacorn.petasos.tasking.factories.PetasosActionableTaskFactory;
import net.fhirfactory.pegacorn.petasos.ipc.endpoints.common.ProcessingPlantJGroupsIntegrationPointSet;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class ITOpsMetricsReportReceiver extends ITOpsReceiverBase implements PetasosMetricsHandlerInterface, PetasosMetricsBrokerInterface {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsMetricsReportReceiver.class);

    @Inject
    private ITOpsSystemWideMetricsDM metricsDM;

    @Inject
    private PetasosActionableTaskFactory actionableTaskFactory;

    @Produce
    ProducerTemplate template;

    @Inject
    private JGroupsClusterConnectionSet integrationPointSet;


    @Override
    protected void registerCapabilities(){
        getProcessingPlant().registerCapabilityFulfillmentService(WorkUnitProcessorCapabilityEnum.CAPABILITY_INFORMATION_MANAGEMENT_IT_OPS.getToken(), this);
    }

    @Override
    public CapabilityUtilisationResponse executeTask(CapabilityUtilisationRequest request) {
        getLogger().debug(".executeTask(): Entry, request->{}", request);
        PetasosComponentMetricSet metricsSet = extractMetricsSet(request);

        CapabilityUtilisationResponse response = null;
        if(metricsSet == null){
            response = generateBadResponse(request.getRequestID());

        } else {
            response = new CapabilityUtilisationResponse();
            response.setInstantCompleted(Instant.now());
            response.setSuccessful(true);
            response.setAssociatedRequestID(request.getRequestID());
            response.setResponseContent("OK");
        }
        getLogger().debug(".executeTask(): Exit, response->{}", response);
        return(response);
    }

    @Override
    protected Logger getLogger(){
        return(LOG);
    }

    protected PetasosComponentMetricSet extractMetricsSet(CapabilityUtilisationRequest request){
        getLogger().debug(".extractMetricsSet(): Entry, request->{}", request);
        PetasosComponentMetricSet metricsSet = null;
        getLogger().debug(".extractMetricsSet(): Exit, metricSet->{}", metricsSet);
        return(metricsSet);
    }

    @Override
    public Instant replicateMetricToServerHandler(PetasosComponentMetric metric, JGroupsChannelConnectorSummary integrationPoint) {
        PetasosComponentMetricSet componentMetricsSet = metricsDM.getComponentMetricsSet(metric.getMetricSource().getId());
        componentMetricsSet.addMetric(metric);
        return(Instant.now());
    }

    @Override
    public Instant replicateMetricSetToServerHandler(PetasosComponentMetricSet metricSet, JGroupsChannelConnectorSummary integrationPoint) {
        getLogger().debug(".replicateMetricSetToServerHandler(): Entry, metricSet->{}, integrationPoint->{}", metricSet, integrationPoint);
        if(metricSet != null){
            metricsDM.addComponentMetricSet(integrationPoint.getComponentId().getId(), metricSet);
        }
        getLogger().debug(".replicateMetricSetToServerHandler(): Exit");
        return(Instant.now());
    }

    @Override
    public Instant replicateMetricToServer(String collectorSubsystemName, PetasosComponentMetric metric) {
        getLogger().debug(".replicateMetricSetToServerHandler(): Entry, collectorSubsystemName->{}, metric->{}", collectorSubsystemName, metric);
        if(metric == null){
            return(Instant.now());
        }
        ComponentIdType MyComponentId = getProcessingPlant().getMeAsASoftwareComponent().getComponentID();
        PetasosComponentMetricSet componentMetricsSet = metricsDM.getComponentMetricsSet(metric.getMetricSource().getId());
        componentMetricsSet.addMetric(metric);
        getLogger().debug(".replicateMetricSetToServerHandler(): Exit");
        return (Instant.now());
    }

    @Override
    public Instant replicateMetricSetToServer(String collectorServiceName, PetasosComponentMetricSet metricSet) {
        getLogger().debug(".replicateMetricSetToServerHandler(): Entry, collectorSubsystemName->{}, metric->{}", collectorServiceName, metricSet);
        if(metricSet == null){
            return(Instant.now());
        }
        ComponentIdType MyComponentId = getProcessingPlant().getMeAsASoftwareComponent().getComponentID();
        PetasosComponentMetricSet componentMetricsSet = metricsDM.getComponentMetricsSet(metricSet.getMetricSourceComponentId().getId());
        for(PetasosComponentMetric currentMetric: metricSet.getMetrics().values()) {
            componentMetricsSet.addMetric(currentMetric);
        }
        getLogger().debug(".replicateMetricSetToServerHandler(): Exit");
        return (Instant.now());
    }

    //
    // Update Notification Service
    //

    @Override
    protected void cacheMonitorProcess() {
        getLogger().debug(".cacheMonitorProcess(): Entry");
        List<PetasosComponentMetricSet> updatedMetricSets = metricsDM.getUpdatedMetricSets();
        for(PetasosComponentMetricSet currentMetricSet: updatedMetricSets){
            TaskWorkItem taskWorkItem = new TaskWorkItem();
            String workItemPayload = null;
            try {
                workItemPayload= getJsonMapper().writeValueAsString(currentMetricSet);
            } catch (JsonProcessingException e) {
                getLogger().warn("cacheMonitorProcess(): Could not convert metric to JSON, error->{}", ExceptionUtils.getStackTrace(e));
            }
            if(workItemPayload != null){
                UoWPayload ingresPayload = new UoWPayload();
                ingresPayload.setPayload(workItemPayload);

                DataParcelManifest manifest = new DataParcelManifest();
                DataParcelTypeDescriptor descriptor = new DataParcelTypeDescriptor();
                descriptor.setDataParcelDefiner("FHIRFactory");
                descriptor.setDataParcelCategory("OAM");
                descriptor.setDataParcelSubCategory("Reporting");
                descriptor.setDataParcelResource("PetasosComponentMetricSet");
                manifest.setContentDescriptor(descriptor);
                manifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_FALSE);
                manifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
                manifest.setDataParcelFlowDirection(DataParcelDirectionEnum.INFORMATION_FLOW_INBOUND_DATA_PARCEL);
                manifest.setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_NEGATIVE);
                ingresPayload.setPayloadManifest(manifest);

                taskWorkItem.setIngresContent(ingresPayload);

                PetasosActionableTask actionableTask = actionableTaskFactory.newMessageBasedActionableTask(taskWorkItem);

                template.sendBody(PetasosPropertyConstants.TASK_DISTRIBUTION_QUEUE, ExchangePattern.InOnly, actionableTask);
            }
        }
        touchLastUpdateInstant();
        getLogger().debug(".cacheMonitorProcess(): Exit");
    }

    @Override
    protected String cacheMonitorProcessTimerName() {
        return ("MetricsNotificationServiceTimer");
    }
}
