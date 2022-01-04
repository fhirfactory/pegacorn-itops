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
package net.fhirfactory.pegacorn.itops.im.workshops.cache;

import net.fhirfactory.pegacorn.core.model.petasos.oam.topology.PetasosMonitoredTopologyGraph;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ITOpsSystemWideTopologyMapDM {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsSystemWideTopologyMapDM.class);

    private PetasosMonitoredTopologyGraph topologyGraph;
    private Map<String, SoftwareComponentSummary> nodeMap;
    private ConcurrentHashMap<String, Instant> sourceUpdateInstantMap;
    private Instant currentStateUpdateInstant;
    private Object graphLock;

    public ITOpsSystemWideTopologyMapDM() {
        LOG.debug(".ITOpsCollatedNodesDM(): Constructor initialisation");
        this.topologyGraph = new PetasosMonitoredTopologyGraph();
        this.currentStateUpdateInstant = Instant.now();
        this.nodeMap = new ConcurrentHashMap<>();
        this.sourceUpdateInstantMap = new ConcurrentHashMap<>();
        this.graphLock = new Object();
    }

    public PetasosMonitoredTopologyGraph getTopologyGraph() {
        return topologyGraph;
    }

    public void setTopologyGraph(PetasosMonitoredTopologyGraph topologyGraph) {
        this.topologyGraph = topologyGraph;
        this.currentStateUpdateInstant = Instant.now();
    }

    public Instant getCurrentStateUpdateInstant() {
        return currentStateUpdateInstant;
    }

    public void setCurrentStateUpdateInstant(Instant currentStateUpdateInstant) {
        this.currentStateUpdateInstant = currentStateUpdateInstant;
    }

    public void addProcessingPlant(ProcessingPlantSummary processingPlant){
        synchronized (graphLock) {
            topologyGraph.addProcessingPlant(processingPlant);
            if(nodeMap.containsKey(processingPlant.getComponentID().getId())){
                nodeMap.remove(processingPlant.getComponentID().getId());
            }
            nodeMap.put(processingPlant.getComponentID().getId(), processingPlant);
            if(sourceUpdateInstantMap.containsKey(processingPlant.getComponentID().getId())){
                sourceUpdateInstantMap.remove(processingPlant.getComponentID().getId());
            }
            sourceUpdateInstantMap.put(processingPlant.getComponentID().getId(),Instant.now());
        }
        currentStateUpdateInstant = Instant.now();
    }

    public void removeProcessingPlant(ProcessingPlantSummary processingPlant){
        removeProcessingPlant(processingPlant.getComponentID().getId());
    }

    public void removeProcessingPlant(String componentID){
        synchronized (graphLock) {
            topologyGraph.removeProcessingPlant(componentID);
        }
    }

    public ConcurrentHashMap<String, Instant> getSourceUpdateInstantMap() {
        return sourceUpdateInstantMap;
    }

    public void setSourceUpdateInstantMap(ConcurrentHashMap<String, Instant> sourceUpdateInstantMap) {
        this.sourceUpdateInstantMap = sourceUpdateInstantMap;
    }

    public void refreshNodeMap(){
        synchronized (graphLock){
            nodeMap.clear();
            for(ProcessingPlantSummary currentProcessingPlant: topologyGraph.getProcessingPlants().values()){
                nodeMap.put(currentProcessingPlant.getComponentID().getId(), currentProcessingPlant);
                for(WorkshopSummary currentWorkshop: currentProcessingPlant.getWorkshops().values()){
                    nodeMap.put(currentWorkshop.getComponentID().getId(), currentWorkshop);
                    for(WorkUnitProcessorSummary currentWUP: currentWorkshop.getWorkUnitProcessors().values()){
                        nodeMap.put(currentWUP.getComponentID().getId(), currentWUP);
                        for(EndpointSummary currentEndpoint: currentWUP.getEndpoints().values()){
                            nodeMap.put(currentEndpoint.getComponentID().getId(), currentEndpoint);
                        }
                    }
                }
            }
        }
    }

    public SoftwareComponentSummary getNode(String componentID){
        if(nodeMap.containsKey(componentID)){
            return(nodeMap.get(componentID));
        } else {
            return(null);
        }
    }

    public List<ProcessingPlantSummary> getProcessingPlants(){
        List<ProcessingPlantSummary> plantList = new ArrayList<>();
        synchronized(graphLock){
            plantList.addAll(topologyGraph.getProcessingPlants().values());
        }
        return(plantList);
    }
}
