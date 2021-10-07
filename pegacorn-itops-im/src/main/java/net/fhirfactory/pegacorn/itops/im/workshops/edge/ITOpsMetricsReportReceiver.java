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
import net.fhirfactory.pegacorn.components.capabilities.base.CapabilityUtilisationRequest;
import net.fhirfactory.pegacorn.components.capabilities.base.CapabilityUtilisationResponse;
import net.fhirfactory.pegacorn.itops.im.workshops.cache.ITOpsSystemWideMetricsDM;
import net.fhirfactory.pegacorn.itops.im.workshops.edge.common.ITOpsReceiverBase;
import net.fhirfactory.pegacorn.petasos.itops.valuesets.ITOpsCapabilityNamesEnum;
import net.fhirfactory.pegacorn.petasos.model.itops.metrics.ProcessingPlantNodeMetrics;
import net.fhirfactory.pegacorn.petasos.model.itops.metrics.WorkUnitProcessorNodeMetrics;
import net.fhirfactory.pegacorn.petasos.model.itops.metrics.common.NodeMetricsBase;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

@ApplicationScoped
public class ITOpsMetricsReportReceiver extends ITOpsReceiverBase {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsMetricsReportReceiver.class);

    @Inject
    private ITOpsSystemWideMetricsDM metricsDM;

    @Override
    protected void registerCapabilities(){
        getProcessingPlant().registerCapabilityFulfillmentService(ITOpsCapabilityNamesEnum.IT_OPS_METRICS_REPORT_COLLATOR.getCapabilityName(), this);
    }

    @Override
    public CapabilityUtilisationResponse executeTask(CapabilityUtilisationRequest request) {
        getLogger().info(".executeTask(): Entry, request->{}", request);
        NodeMetricsBase metricsSet = extractMetricsSet(request);
        if(metricsSet != null){
            metricsDM.addComponentMetricSet(metricsSet.getComponentID(), metricsSet);
        }
        CapabilityUtilisationResponse response = null;
        if(metricsSet == null){
            response = generateBadResponse(request.getRequestID());

        } else {
            response = new CapabilityUtilisationResponse();
            response.setDateCompleted(Instant.now());
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

    protected NodeMetricsBase extractMetricsSet(CapabilityUtilisationRequest request){
        getLogger().info(".extractMetricsSet(): Entry, request->{}", request);
        NodeMetricsBase metricsSet = null;
        try {
            JSONObject metricsJSON = new JSONObject(request.getRequestContent());
            String metricsType = metricsJSON.getString("metricsType");
            switch(metricsType){
                case ProcessingPlantNodeMetrics.PROCESSING_PLANT_METRICS_TYPE:
                    metricsSet = getJsonMapper().readValue(request.getRequestContent(), ProcessingPlantNodeMetrics.class);
                    break;
                case WorkUnitProcessorNodeMetrics.WORK_UNIT_PROCESSOR_METRICS_TYPE:
                    metricsSet = getJsonMapper().readValue(request.getRequestContent(), WorkUnitProcessorNodeMetrics.class);
                    break;
                default:
                    metricsSet = getJsonMapper().readValue(request.getRequestContent(), NodeMetricsBase.class);
            }
        } catch (JsonProcessingException e) {
            getLogger().error(".extractMetricsSet(): Unable to JSON Decode String, {}", e);
        }
        getLogger().info(".extractMetricsSet(): Exit, metricSet->{}", metricsSet);
        return(metricsSet);
    }

}
