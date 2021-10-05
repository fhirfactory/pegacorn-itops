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

import net.fhirfactory.pegacorn.petasos.model.itops.topology.*;
import net.fhirfactory.pegacorn.petasos.model.itops.topology.common.ITOpsMonitoredNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ITOpsSystemWideTopologyDM {

    private static final Logger LOG = LoggerFactory.getLogger(ITOpsSystemWideTopologyDM.class);

    private ITOpsTopologyGraph topologyGraph;
    private Map<String, ITOpsMonitoredNode> nodeMap;
    private Instant currentStateUpdateInstant;
    private Object graphLock;

    public ITOpsSystemWideTopologyDM() {
        LOG.debug(".ITOpsCollatedNodesDM(): Constructor initialisation");
        this.topologyGraph = new ITOpsTopologyGraph();
        this.currentStateUpdateInstant = Instant.now();
        this.nodeMap = new ConcurrentHashMap<>();
        this.graphLock = new Object();
    }

    public ITOpsTopologyGraph getTopologyGraph() {
        return topologyGraph;
    }

    public void setTopologyGraph(ITOpsTopologyGraph topologyGraph) {
        this.topologyGraph = topologyGraph;
        this.currentStateUpdateInstant = Instant.now();
    }

    public Instant getCurrentStateUpdateInstant() {
        return currentStateUpdateInstant;
    }

    public void setCurrentStateUpdateInstant(Instant currentStateUpdateInstant) {
        this.currentStateUpdateInstant = currentStateUpdateInstant;
    }

    public void addProcessingPlant(ITOpsMonitoredProcessingPlant processingPlant){
        synchronized (graphLock) {
            topologyGraph.addProcessingPlant(processingPlant);
        }
        currentStateUpdateInstant = Instant.now();
    }

    public void removeProcessingPlant(ITOpsMonitoredProcessingPlant processingPlant){
        removeProcessingPlant(processingPlant.getComponentID());
    }

    public void removeProcessingPlant(String componentID){
        synchronized (graphLock) {
            topologyGraph.removeProcessingPlant(componentID);
        }
    }

    public void refreshNodeMap(){
        synchronized (graphLock){
            nodeMap.clear();
            for(ITOpsMonitoredProcessingPlant currentProcessingPlant: topologyGraph.getProcessingPlants().values()){
                nodeMap.put(currentProcessingPlant.getComponentID(), currentProcessingPlant);
                for(ITOpsMonitoredWorkshop currentWorkshop: currentProcessingPlant.getWorkshops().values()){
                    nodeMap.put(currentWorkshop.getComponentID(), currentWorkshop);
                    for(ITOpsMonitoredWUP currentWUP: currentWorkshop.getWorkUnitProcessors().values()){
                        nodeMap.put(currentWUP.getComponentID(), currentWUP);
                        for(ITOpsMonitoredEndpoint currentEndpoint: currentWUP.getEndpoints().values()){
                            nodeMap.put(currentEndpoint.getComponentID(), currentEndpoint);
                        }
                    }
                }
            }
        }
    }

    public ITOpsMonitoredNode getNode(String componentID){
        if(nodeMap.containsKey(componentID)){
            return(nodeMap.get(componentID));
        } else {
            return(null);
        }
    }

    public List<ITOpsMonitoredProcessingPlant> getProcessingPlants(){
        List<ITOpsMonitoredProcessingPlant> plantList = new ArrayList<>();
        synchronized(graphLock){
            plantList.addAll(topologyGraph.getProcessingPlants().values());
        }
        return(plantList);
    }
}
