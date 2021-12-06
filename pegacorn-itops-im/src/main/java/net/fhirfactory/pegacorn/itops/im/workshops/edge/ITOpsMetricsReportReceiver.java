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
package net.fhirfactory.pegacorn.itops.im.workshops.edge;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.interfaces.oam.metrics.PetasosMetricsHandlerInterface;
import net.fhirfactory.pegacorn.core.model.capabilities.base.CapabilityUtilisationRequest;
import net.fhirfactory.pegacorn.core.model.capabilities.base.CapabilityUtilisationResponse;
import net.fhirfactory.pegacorn.core.model.capabilities.valuesets.CapabilityProviderTitlesEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelNormalisationStatusEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelValidationStatusEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.PolicyEnforcementPointApprovalStatusEnum;
import net.fhirfactory.pegacorn.core.model.petasos.oam.metrics.PetasosComponentMetric;
import net.fhirfactory.pegacorn.core.model.petasos.oam.metrics.PetasosComponentMetricSet;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosActionableTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.work.datatypes.TaskWorkItemType;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.edge.petasos.PetasosEndpointIdentifier;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.task.factories.TaskWorkItemFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.cache.ITOpsSystemWideMetricsDM;
import net.fhirfactory.pegacorn.itops.im.workshops.edge.common.ITOpsReceiverBase;
import net.fhirfactory.pegacorn.petasos.core.tasks.factories.PetasosActionableTaskFactory;
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
public class ITOpsMetricsReportReceiver extends ITOpsReceiverBase implements PetasosMetricsHandlerInterface {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsMetricsReportReceiver.class);

    @Inject
    private ITOpsSystemWideMetricsDM metricsDM;

    @Inject
    private PetasosActionableTaskFactory actionableTaskFactory;

    @Produce
    private ProducerTemplate template;


    @Override
    protected void registerCapabilities(){
        getProcessingPlant().registerCapabilityFulfillmentService(CapabilityProviderTitlesEnum.CAPABILITY_INFORMATION_MANAGEMENT_IT_OPS.getToken(), this);
    }

    @Override
    public CapabilityUtilisationResponse executeTask(CapabilityUtilisationRequest request) {
        getLogger().info(".executeTask(): Entry, request->{}", request);
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
        getLogger().info(".executeTask(): Exit, response->{}", response);
        return(response);
    }

    @Override
    protected Logger getLogger(){
        return(LOG);
    }

    protected PetasosComponentMetricSet extractMetricsSet(CapabilityUtilisationRequest request){
        getLogger().info(".extractMetricsSet(): Entry, request->{}", request);
        PetasosComponentMetricSet metricsSet = null;
        getLogger().info(".extractMetricsSet(): Exit, metricSet->{}", metricsSet);
        return(metricsSet);
    }

    @Override
    public Instant captureMetric(PetasosComponentMetric metric, PetasosEndpointIdentifier endpointIdentifier) {
        return null;
    }

    @Override
    public Instant captureMetrics(PetasosComponentMetricSet metricSet, PetasosEndpointIdentifier endpointIdentifier) {
        if(metricSet != null){
            metricsDM.addComponentMetricSet(endpointIdentifier.getEndpointComponentID().getId(), metricSet);
        }
        return(Instant.now());
    }

    //
    // Update Notification Service
    //

    @Override
    protected void cacheMonitorProcess() {
        List<PetasosComponentMetricSet> updatedMetricSets = metricsDM.getUpdatedMetricSets();
        for(PetasosComponentMetricSet currentMetricSet: updatedMetricSets){
            TaskWorkItemType taskWorkItem = new TaskWorkItemType();
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
    }

    @Override
    protected String cacheMonitorProcessTimerName() {
        return ("MetricsNotificationServiceTimer");
    }
}
