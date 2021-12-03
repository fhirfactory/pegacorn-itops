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

import net.fhirfactory.pegacorn.core.model.petasos.oam.metrics.PetasosComponentMetricSet;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ITOpsSystemWideMetricsDM {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsSystemWideMetricsDM.class);
    private ConcurrentHashMap<String, PetasosComponentMetricSet> currentStateComponentMetricSetMap;
    private ConcurrentHashMap<String, PetasosComponentMetricSet> previousStateComponentMetricSetMap;
    private ConcurrentHashMap<String, PetasosComponentMetricSet> displayedComponentMetricSetMap;
    private ConcurrentHashMap<String, String> endpointRouteToSourceMap;
    private ConcurrentHashMap<String, Instant> sourceUpdateInstantMap;

    public ITOpsSystemWideMetricsDM(){
        this.currentStateComponentMetricSetMap = new ConcurrentHashMap<>();
        this.previousStateComponentMetricSetMap = new ConcurrentHashMap<>();
        this.displayedComponentMetricSetMap = new ConcurrentHashMap<>();
        this.endpointRouteToSourceMap = new ConcurrentHashMap<>();
        this.sourceUpdateInstantMap = new ConcurrentHashMap<>();
    }

    //
    // Getters (and Setters)
    //

    public ConcurrentHashMap<String, PetasosComponentMetricSet> getCurrentStateComponentMetricSetMap() {
        return currentStateComponentMetricSetMap;
    }

    public void setCurrentStateComponentMetricSetMap(ConcurrentHashMap<String, PetasosComponentMetricSet> currentStateComponentMetricSetMap) {
        this.currentStateComponentMetricSetMap = currentStateComponentMetricSetMap;
    }

    public ConcurrentHashMap<String, PetasosComponentMetricSet> getPreviousStateComponentMetricSetMap() {
        return previousStateComponentMetricSetMap;
    }

    public void setPreviousStateComponentMetricSetMap(ConcurrentHashMap<String, PetasosComponentMetricSet> previousStateComponentMetricSetMap) {
        this.previousStateComponentMetricSetMap = previousStateComponentMetricSetMap;
    }

    public ConcurrentHashMap<String, PetasosComponentMetricSet> getDisplayedComponentMetricSetMap() {
        return displayedComponentMetricSetMap;
    }

    public void setDisplayedComponentMetricSetMap(ConcurrentHashMap<String, PetasosComponentMetricSet> displayedComponentMetricSetMap) {
        this.displayedComponentMetricSetMap = displayedComponentMetricSetMap;
    }

    public ConcurrentHashMap<String, String> getEndpointRouteToSourceMap() {
        return endpointRouteToSourceMap;
    }

    public void setEndpointRouteToSourceMap(ConcurrentHashMap<String, String> endpointRouteToSourceMap) {
        this.endpointRouteToSourceMap = endpointRouteToSourceMap;
    }

    public ConcurrentHashMap<String, Instant> getSourceUpdateInstantMap() {
        return sourceUpdateInstantMap;
    }

    public void setSourceUpdateInstantMap(ConcurrentHashMap<String, Instant> sourceUpdateInstantMap) {
        this.sourceUpdateInstantMap = sourceUpdateInstantMap;
    }

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Functions
    //

    public void addComponentMetricSet(String routingEndpointId, PetasosComponentMetricSet metricsSet){
        getLogger().debug(".addComponentMetricSet(): Entry, componentID->{}, metricSet->{}", routingEndpointId, metricsSet);
        if(StringUtils.isEmpty(routingEndpointId) || metricsSet == null){
            getLogger().debug(".addComponentMetricSet(): Exit, either componentID or metricSet is empty");
            return;
        }
        if(getCurrentStateComponentMetricSetMap().containsKey(metricsSet.getMetricSourceComponentId())){
            if(getPreviousStateComponentMetricSetMap().containsKey(metricsSet.getMetricSourceComponentId())){
                getPreviousStateComponentMetricSetMap().remove(metricsSet.getMetricSourceComponentId());
            }
            getPreviousStateComponentMetricSetMap().put(metricsSet.getMetricSourceComponentId(), getCurrentStateComponentMetricSetMap().get(metricsSet.getMetricSourceComponentId()));
            getCurrentStateComponentMetricSetMap().remove(metricsSet.getMetricSourceComponentId());
        }
        getCurrentStateComponentMetricSetMap().put(metricsSet.getMetricSourceComponentId(), metricsSet);
        if(!this.endpointRouteToSourceMap.containsKey(metricsSet.getMetricSourceComponentId())){
            this.endpointRouteToSourceMap.remove(metricsSet.getMetricSourceComponentId());
        }
        this.endpointRouteToSourceMap.put(metricsSet.getMetricSourceComponentId(), routingEndpointId);
        if(!this.sourceUpdateInstantMap.containsKey(metricsSet.getMetricSourceComponentId())){
            this.sourceUpdateInstantMap.remove(metricsSet.getMetricSourceComponentId());
        }
        this.sourceUpdateInstantMap.put(metricsSet.getMetricSourceComponentId(), Instant.now());
        getLogger().debug(".addComponentMetricsSet():Exit");
    }

    public PetasosComponentMetricSet getComponentMetricSetForDisplay(String metricSourceComponentId){
        getLogger().debug(".getComponentMetricSetForPublishing(): Entry, componentID->{}", metricSourceComponentId);
        if(getCurrentStateComponentMetricSetMap().containsKey(metricSourceComponentId)){
            return(new PetasosComponentMetricSet());
        }
        PetasosComponentMetricSet currentMetricsSet = getCurrentStateComponentMetricSetMap().get(metricSourceComponentId);
        if(getDisplayedComponentMetricSetMap().containsKey(metricSourceComponentId)){
            getDisplayedComponentMetricSetMap().remove(metricSourceComponentId);
        }
        PetasosComponentMetricSet publishedMetricsSet = SerializationUtils.clone(currentMetricsSet);
        getDisplayedComponentMetricSetMap().put(metricSourceComponentId, publishedMetricsSet);
        getLogger().debug(".getComponentMetricSetForPublishing(): Exit, publishedMetricSet->{}", publishedMetricsSet);
        return(publishedMetricsSet);
    }

    public PetasosComponentMetricSet getComponentMetricsSet(String metricSourceComponentId){
        getLogger().debug(".getComponentMetricsSet(): Entry, metricSourceComponentId->{}", metricSourceComponentId);
        if(StringUtils.isEmpty(metricSourceComponentId)){
            return(null);
        }
        PetasosComponentMetricSet currentState = getCurrentStateComponentMetricSetMap().get(metricSourceComponentId);
        getLogger().debug(".getComponentMetricsSet(): Exit, currentState->{}", currentState);
        return(currentState);
    }
}
