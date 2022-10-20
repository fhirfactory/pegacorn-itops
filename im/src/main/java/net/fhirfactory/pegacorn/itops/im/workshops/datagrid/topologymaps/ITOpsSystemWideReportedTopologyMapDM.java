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
package net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps;

import net.fhirfactory.pegacorn.core.model.componentid.ComponentIdType;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ITOpsSystemWideReportedTopologyMapDM {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsSystemWideReportedTopologyMapDM.class);

    // Map<componentId, componentSummary>
    private Map<String, ProcessingPlantSummary> reportedProcessingPlantMap;

    // Map<componentId, componentSummary>
    private Map<String, SoftwareComponentSummary> reportedSoftwareComponentMap;

    // Map<subsystemParticipantName, MatrixSpace> replicaMap;

    // Map<subsystemParticipantName, Set<subsystemComponentId>>
    private Map<String, Set<ComponentIdType>> discoveredComponentMap;

    private ConcurrentHashMap<String, Instant> sourceUpdateInstantMap;

    private Instant currentStateUpdateInstant;
    private Object graphLock;

    //
    // Constructor(s)
    //

    public ITOpsSystemWideReportedTopologyMapDM() {
        LOG.debug(".ITOpsCollatedNodesDM(): Constructor initialisation");
        this.currentStateUpdateInstant = Instant.now();
        this.reportedSoftwareComponentMap = new ConcurrentHashMap<>();
        this.sourceUpdateInstantMap = new ConcurrentHashMap<>();
        this.reportedProcessingPlantMap = new ConcurrentHashMap<>();
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

    public static Logger getLogger() {
        return LOG;
    }

    protected Map<String, SoftwareComponentSummary> getReportedSoftwareComponentMap() {
        return reportedSoftwareComponentMap;
    }

    protected Object getGraphLock() {
        return graphLock;
    }

    //
    // Business Methods
    //

    public void addProcessingPlant(String forwardingAgentId, ProcessingPlantSummary processingPlant){
        getLogger().debug(".addProcessingPlant(): Entry, processingPlant->{}", processingPlant.getComponentID());
        if(reportedProcessingPlantMap.containsKey(processingPlant.getComponentID().getId())){
            removeProcessingPlant(processingPlant.getComponentID().getId());
        }
        addProcessingPlant(processingPlant);

        if(sourceUpdateInstantMap.containsKey(processingPlant.getComponentID().getId())){
            sourceUpdateInstantMap.remove(processingPlant.getComponentID().getId());
        }
        currentStateUpdateInstant = Instant.now();
        getLogger().debug(".addProcessingPlant(): Exit");
    }

    //
    //

    public void addProcessingPlant(ProcessingPlantSummary processingPlant){
        removeProcessingPlant(processingPlant);
        synchronized (graphLock) {
            reportedProcessingPlantMap.put(processingPlant.getComponentID().getId(), processingPlant);
            reportedSoftwareComponentMap.put(processingPlant.getComponentID().getId(), processingPlant);
            for(WorkshopSummary currentWorkshop: processingPlant.getWorkshops().values()){
                reportedSoftwareComponentMap.put(currentWorkshop.getComponentID().getId(), currentWorkshop);
                for(WorkUnitProcessorSummary currentWUP: currentWorkshop.getWorkUnitProcessors().values()){
                    reportedSoftwareComponentMap.put(currentWUP.getComponentID().getId(), currentWorkshop);
                    for(EndpointSummary currentEndpoint: currentWUP.getEndpoints().values()){
                        reportedSoftwareComponentMap.put(currentEndpoint.getComponentID().getId(), currentEndpoint);
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
            ProcessingPlantSummary entry = reportedProcessingPlantMap.get(componentID);
            if(entry != null){
                reportedProcessingPlantMap.remove(componentID);
                reportedSoftwareComponentMap.remove(componentID);
                for(WorkshopSummary currentWorkshop: entry.getWorkshops().values()){
                    reportedSoftwareComponentMap.remove(currentWorkshop.getComponentID());
                    for(WorkUnitProcessorSummary currentWUP: currentWorkshop.getWorkUnitProcessors().values()){
                        reportedSoftwareComponentMap.remove(currentWUP.getComponentID());
                        for(EndpointSummary currentEndpoint: currentWUP.getEndpoints().values()){
                            reportedSoftwareComponentMap.remove(currentEndpoint.getComponentID());
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
            reportedSoftwareComponentMap.clear();
            for(ProcessingPlantSummary currentProcessingPlant: reportedProcessingPlantMap.values()){
                reportedSoftwareComponentMap.put(currentProcessingPlant.getComponentID().getId(), currentProcessingPlant);
                for(WorkshopSummary currentWorkshop: currentProcessingPlant.getWorkshops().values()){
                    reportedSoftwareComponentMap.put(currentWorkshop.getComponentID().getId(), currentWorkshop);
                    for(WorkUnitProcessorSummary currentWUP: currentWorkshop.getWorkUnitProcessors().values()){
                        reportedSoftwareComponentMap.put(currentWUP.getComponentID().getId(), currentWUP);
                        for(EndpointSummary currentEndpoint: currentWUP.getEndpoints().values()){
                            reportedSoftwareComponentMap.put(currentEndpoint.getComponentID().getId(), currentEndpoint);
                        }
                    }
                }
            }
        }
    }

    public SoftwareComponentSummary getNode(String componentID){
        if(reportedSoftwareComponentMap.containsKey(componentID)){
            return(reportedSoftwareComponentMap.get(componentID));
        } else {
            return(null);
        }
    }

    public List<ProcessingPlantSummary> getProcessingPlants(){
        List<ProcessingPlantSummary> plantList = new ArrayList<>();
        synchronized(graphLock){
            plantList.addAll(reportedProcessingPlantMap.values());
        }
        return(plantList);
    }

    public void printMap(){
        synchronized (graphLock){
            reportedSoftwareComponentMap.clear();
            for(ProcessingPlantSummary currentProcessingPlant: reportedProcessingPlantMap.values()){
                LOG.trace(".printMap(): ProcessingPlant->{}/{}", currentProcessingPlant.getComponentID().getDisplayName(), currentProcessingPlant.getParticipantId().getDisplayName());
                for(WorkshopSummary currentWorkshop: currentProcessingPlant.getWorkshops().values()){
                    LOG.trace(".printMap(): Workshop->{}/{}", currentWorkshop.getComponentID().getDisplayName(), currentWorkshop.getParticipantId().getDisplayName());
                    for(WorkUnitProcessorSummary currentWUP: currentWorkshop.getWorkUnitProcessors().values()){
                        LOG.trace(".printMap(): WorkUnitProcessor->{}/{}", currentWUP.getComponentID().getDisplayName(), currentWUP.getParticipantId().getDisplayName());
                        for(EndpointSummary currentEndpoint: currentWUP.getEndpoints().values()){
                            LOG.trace(".printMap(): Endpoint->{}/{}", currentEndpoint.getComponentID().getDisplayName(), currentEndpoint.getParticipantId().getDisplayName());
                        }
                    }
                }
            }
        }
    }
}
