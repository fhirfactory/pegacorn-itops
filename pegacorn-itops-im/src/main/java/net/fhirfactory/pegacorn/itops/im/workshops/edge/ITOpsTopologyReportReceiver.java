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
import net.fhirfactory.pegacorn.itops.im.workshops.cache.ITOpsSystemWideTopologyDM;
import net.fhirfactory.pegacorn.itops.im.workshops.edge.common.ITOpsReceiverBase;
import net.fhirfactory.pegacorn.petasos.itops.valuesets.ITOpsCapabilityNamesEnum;
import net.fhirfactory.pegacorn.petasos.model.itops.topology.ITOpsMonitoredProcessingPlant;
import net.fhirfactory.pegacorn.petasos.model.itops.topology.ITOpsTopologyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

@ApplicationScoped
public class ITOpsTopologyReportReceiver extends ITOpsReceiverBase {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsTopologyReportReceiver.class);

    @Inject
    private ITOpsSystemWideTopologyDM topologyMapDM;

    @Override
    protected void registerCapabilities(){
        getProcessingPlant().registerCapabilityFulfillmentService(ITOpsCapabilityNamesEnum.IT_OPS_TOPOLOGY_REPORT_COLLATOR.getCapabilityName(), this);
    }

    @Override
    public CapabilityUtilisationResponse executeTask(CapabilityUtilisationRequest request) {
        getLogger().info(".executeTask(): Entry, request->{}", request);
        ITOpsTopologyGraph topologyGraph = extractTopologyGraph(request);
        if(topologyGraph != null){
            for(ITOpsMonitoredProcessingPlant currentProcessingPlant: topologyGraph.getProcessingPlants().values()) {
                topologyMapDM.addProcessingPlant(currentProcessingPlant);
            }
            topologyMapDM.refreshNodeMap();
        }
        CapabilityUtilisationResponse response = null;
        if(topologyGraph == null){
            response = generateBadResponse(request.getRequestID());

        } else {
            response = new CapabilityUtilisationResponse();
            response.setDateCompleted(Instant.now());
            response.setSuccessful(true);
            response.setAssociatedRequestID(request.getRequestID());
            response.setResponseContent("OK");
        }
        getLogger().info(".executeTask(): Exit, reponse->{}", response);
        return(response);
    }

    @Override
    protected Logger getLogger(){
        return(LOG);
    }

    protected ITOpsTopologyGraph extractTopologyGraph(CapabilityUtilisationRequest request){
        getLogger().debug(".extractMetricsSet(): Entry, request->{}", request);
        ITOpsTopologyGraph topologyGraph = null;
        try {
            topologyGraph = getJsonMapper().readValue(request.getRequestContent(),ITOpsTopologyGraph.class);
        } catch (JsonProcessingException e) {
            getLogger().error(".extractMetricsSet(): Unable to JSON Decode String, {}", e);
        }
        getLogger().debug(".extractMetricsSet(): Exit, metricSet->{}", topologyGraph);
        return(topologyGraph);
    }

}
