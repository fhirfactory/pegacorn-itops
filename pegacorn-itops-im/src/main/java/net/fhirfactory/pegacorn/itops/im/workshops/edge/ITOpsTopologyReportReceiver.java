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
import net.fhirfactory.pegacorn.core.interfaces.topology.PetasosTopologyHandlerInterface;
import net.fhirfactory.pegacorn.core.model.capabilities.base.CapabilityUtilisationRequest;
import net.fhirfactory.pegacorn.core.model.capabilities.base.CapabilityUtilisationResponse;
import net.fhirfactory.pegacorn.core.model.capabilities.valuesets.CapabilityProviderTitlesEnum;
import net.fhirfactory.pegacorn.core.model.petasos.oam.topology.PetasosMonitoredTopologyGraph;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.edge.petasos.PetasosEndpointIdentifier;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.ProcessingPlantSummary;
import net.fhirfactory.pegacorn.itops.im.workshops.cache.ITOpsSystemWideTopologyMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.edge.common.ITOpsReceiverBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

@ApplicationScoped
public class ITOpsTopologyReportReceiver extends ITOpsReceiverBase implements PetasosTopologyHandlerInterface {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsTopologyReportReceiver.class);

    @Inject
    private ITOpsSystemWideTopologyMapDM topologyMapDM;

    @Override
    protected void registerCapabilities(){
        getProcessingPlant().registerCapabilityFulfillmentService(CapabilityProviderTitlesEnum.CAPABILITY_INFORMATION_MANAGEMENT_IT_OPS.getToken(), this);
    }

    @Override
    public CapabilityUtilisationResponse executeTask(CapabilityUtilisationRequest request) {
        getLogger().debug(".executeTask(): Entry, request->{}", request);
        PetasosMonitoredTopologyGraph topologyGraph = extractTopologyGraph(request);

        CapabilityUtilisationResponse response = null;
        if(topologyGraph == null){
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

    protected PetasosMonitoredTopologyGraph extractTopologyGraph(CapabilityUtilisationRequest request){
        getLogger().debug(".extractMetricsSet(): Entry, request->{}", request);
        PetasosMonitoredTopologyGraph topologyGraph = null;
        try {
            topologyGraph = getJsonMapper().readValue(request.getRequestStringContent(),PetasosMonitoredTopologyGraph.class);
        } catch (JsonProcessingException e) {
            getLogger().error(".extractMetricsSet(): Unable to JSON Decode String, {}", e);
        }
        getLogger().debug(".extractMetricsSet(): Exit, metricSet->{}", topologyGraph);
        return(topologyGraph);
    }

    @Override
    public Instant mergeRemoteTopologyGraph(PetasosMonitoredTopologyGraph topologyGraph, PetasosEndpointIdentifier requesterEndpointIdentifier) {
        if(topologyGraph != null) {
            for (ProcessingPlantSummary currentProcessingPlant : topologyGraph.getProcessingPlants().values()) {
                topologyMapDM.addProcessingPlant(currentProcessingPlant);
            }
        }
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
        return ("TopologyNotificationServiceTimer");
    }

}
