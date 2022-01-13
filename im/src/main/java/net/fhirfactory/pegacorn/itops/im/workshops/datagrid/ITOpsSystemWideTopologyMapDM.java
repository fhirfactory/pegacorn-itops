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
package net.fhirfactory.pegacorn.itops.im.workshops.datagrid;

import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.*;
import org.apache.camel.Endpoint;
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

    // Map<componentId, componentSummary>
    private Map<String, ProcessingPlantSummary> processingPlantMap;

    // Map<componentId, componentSummary>
    private Map<String, SoftwareComponentSummary> nodeMap;

    private ConcurrentHashMap<String, Instant> sourceUpdateInstantMap;

    private Instant currentStateUpdateInstant;
    private Object graphLock;

    //
    // Constructor(s)
    //

    public ITOpsSystemWideTopologyMapDM() {
        LOG.debug(".ITOpsCollatedNodesDM(): Constructor initialisation");
        this.currentStateUpdateInstant = Instant.now();
        this.nodeMap = new ConcurrentHashMap<>();
        this.sourceUpdateInstantMap = new ConcurrentHashMap<>();
        this.processingPlantMap = new ConcurrentHashMap<>();
        this.graphLock = new Object();
    }

    //
    // Getters (and Setters)
    //

    public Instant getCurrentStateUpdateInstant() {
        return currentStateUpdateInstant;
    }

    public void setCurrentStateUpdateInstant(Instant currentStateUpdateInstant) {
        this.currentStateUpdateInstant = currentStateUpdateInstant;
    }

    public static Logger getLOG() {
        return LOG;
    }

    protected Map<String, SoftwareComponentSummary> getNodeMap() {
        return nodeMap;
    }

    protected Object getGraphLock() {
        return graphLock;
    }

    //
    // Business Methods
    //

    public void addProcessingPlant(String forwardingAgentId, ProcessingPlantSummary processingPlant){
        if(processingPlantMap.containsKey(processingPlant.getComponentID().getId())){
            removeProcessingPlant(processingPlant.getComponentID().getId());
        }
        addProcessingPlant(processingPlant);

        if(sourceUpdateInstantMap.containsKey(processingPlant.getComponentID().getId())){
            sourceUpdateInstantMap.remove(processingPlant.getComponentID().getId());
        }
        currentStateUpdateInstant = Instant.now();
    }

    //
    //

    public void addProcessingPlant(ProcessingPlantSummary processingPlant){
        removeProcessingPlant(processingPlant);
        synchronized (graphLock) {
            processingPlantMap.put(processingPlant.getComponentID().getId(), processingPlant);
            nodeMap.put(processingPlant.getComponentID().getId(), processingPlant);
            for(WorkshopSummary currentWorkshop: processingPlant.getWorkshops().values()){
                nodeMap.put(currentWorkshop.getComponentID().getId(), currentWorkshop);
                for(WorkUnitProcessorSummary currentWUP: currentWorkshop.getWorkUnitProcessors().values()){
                    nodeMap.put(currentWUP.getComponentID().getId(), currentWorkshop);
                    for(EndpointSummary currentEndpoint: currentWUP.getEndpoints().values()){
                        nodeMap.put(currentEndpoint.getComponentID().getId(), currentEndpoint);
                    }
                }
            }
        }
    }

    public void removeProcessingPlant(ProcessingPlantSummary processingPlant){
        removeProcessingPlant(processingPlant.getComponentID().getId());
    }

    public void removeProcessingPlant(String componentID){
        synchronized (graphLock) {
            ProcessingPlantSummary entry = processingPlantMap.get(componentID);
            if(entry != null){
                processingPlantMap.remove(componentID);
                nodeMap.remove(componentID);
                for(WorkshopSummary currentWorkshop: entry.getWorkshops().values()){
                    nodeMap.remove(currentWorkshop.getComponentID());
                    for(WorkUnitProcessorSummary currentWUP: currentWorkshop.getWorkUnitProcessors().values()){
                        nodeMap.remove(currentWUP.getComponentID());
                        for(EndpointSummary currentEndpoint: currentWUP.getEndpoints().values()){
                            nodeMap.remove(currentEndpoint.getComponentID());
                        }
                    }
                }
            }
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
            for(ProcessingPlantSummary currentProcessingPlant: processingPlantMap.values()){
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
            plantList.addAll(processingPlantMap.values());
        }
        return(plantList);
    }

    public void printMap(){
        synchronized (graphLock){
            nodeMap.clear();
            for(ProcessingPlantSummary currentProcessingPlant: processingPlantMap.values()){
                LOG.info(".printMap(): ProcessingPlant->{}/{}", currentProcessingPlant.getComponentID(), currentProcessingPlant.getTopologyNodeFDN().getLeafRDN());
                for(WorkshopSummary currentWorkshop: currentProcessingPlant.getWorkshops().values()){
                    LOG.info(".printMap(): Workshop->{}/{}", currentWorkshop.getComponentID(), currentWorkshop.getTopologyNodeFDN().getLeafRDN());
                    for(WorkUnitProcessorSummary currentWUP: currentWorkshop.getWorkUnitProcessors().values()){
                        LOG.info(".printMap(): WorkUnitProcessor->{}/{}", currentWUP.getComponentID(), currentWUP.getTopologyNodeFDN().getLeafRDN());
                        for(EndpointSummary currentEndpoint: currentWUP.getEndpoints().values()){
                            LOG.info(".printMap(): Endpoint->{}/{}", currentEndpoint.getComponentID(), currentEndpoint.getTopologyNodeFDN().getLeafRDN());
                        }
                    }
                }
            }
        }
    }
}
