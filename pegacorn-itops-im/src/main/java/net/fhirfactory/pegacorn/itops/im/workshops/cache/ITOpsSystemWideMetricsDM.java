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

import net.fhirfactory.pegacorn.petasos.itops.caches.common.ITOpsLocalDMRefreshBase;
import net.fhirfactory.pegacorn.petasos.model.itops.metrics.ITOpsMetricsSet;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ITOpsSystemWideMetricsDM extends ITOpsLocalDMRefreshBase {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsSystemWideMetricsDM.class);
    private ConcurrentHashMap<String, ITOpsMetricsSet> currentStateComponentMetricSetMap;
    private ConcurrentHashMap<String, ITOpsMetricsSet> previousStateComponentMetricSetMap;
    private ConcurrentHashMap<String, ITOpsMetricsSet> displayedComponentMetricSetMap;

    public ITOpsSystemWideMetricsDM(){
        this.currentStateComponentMetricSetMap = new ConcurrentHashMap<>();
        this.previousStateComponentMetricSetMap = new ConcurrentHashMap<>();
        this.displayedComponentMetricSetMap = new ConcurrentHashMap<>();
    }

    //
    // Getters (and Setters)
    //

    public ConcurrentHashMap<String, ITOpsMetricsSet> getCurrentStateComponentMetricSetMap() {
        return currentStateComponentMetricSetMap;
    }

    public void setCurrentStateComponentMetricSetMap(ConcurrentHashMap<String, ITOpsMetricsSet> currentStateComponentMetricSetMap) {
        this.currentStateComponentMetricSetMap = currentStateComponentMetricSetMap;
    }

    public ConcurrentHashMap<String, ITOpsMetricsSet> getPreviousStateComponentMetricSetMap() {
        return previousStateComponentMetricSetMap;
    }

    public void setPreviousStateComponentMetricSetMap(ConcurrentHashMap<String, ITOpsMetricsSet> previousStateComponentMetricSetMap) {
        this.previousStateComponentMetricSetMap = previousStateComponentMetricSetMap;
    }

    public ConcurrentHashMap<String, ITOpsMetricsSet> getDisplayedComponentMetricSetMap() {
        return displayedComponentMetricSetMap;
    }

    public void setDisplayedComponentMetricSetMap(ConcurrentHashMap<String, ITOpsMetricsSet> displayedComponentMetricSetMap) {
        this.displayedComponentMetricSetMap = displayedComponentMetricSetMap;
    }

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Functions
    //

    public void addComponentMetricSet(String componentID, ITOpsMetricsSet metricsSet){
        getLogger().debug(".addComponentMetricSet(): Entry, componentID->{}, metricSet->{}", componentID, metricsSet);
        if(StringUtils.isEmpty(componentID) || metricsSet == null){
            getLogger().debug(".addComponentMetricSet(): Exit, either componentID or metricSet is empty");
            return;
        }
        if(getCurrentStateComponentMetricSetMap().containsKey(componentID)){
            if(getPreviousStateComponentMetricSetMap().containsKey(componentID)){
                getPreviousStateComponentMetricSetMap().remove(componentID);
            }
            getPreviousStateComponentMetricSetMap().put(componentID, getCurrentStateComponentMetricSetMap().get(componentID));
            getCurrentStateComponentMetricSetMap().remove(componentID);
        }
        getCurrentStateComponentMetricSetMap().put(componentID, metricsSet);
        refreshCurrentStateUpdateInstant();
        getLogger().debug(".addComponentMetricsSet():Exit");
    }

    public ITOpsMetricsSet getComponentMetricSetForDisplay(String componentID){
        getLogger().debug(".getComponentMetricSetForPublishing(): Entry, componentID->{}", componentID);
        if(getCurrentStateComponentMetricSetMap().containsKey(componentID)){
            return(new ITOpsMetricsSet());
        }
        ITOpsMetricsSet currentMetricsSet = getCurrentStateComponentMetricSetMap().get(componentID);
        if(getDisplayedComponentMetricSetMap().containsKey(componentID)){
            getDisplayedComponentMetricSetMap().remove(componentID);
        }
        ITOpsMetricsSet publishedMetricsSet = SerializationUtils.clone(currentMetricsSet);
        getDisplayedComponentMetricSetMap().put(componentID, publishedMetricsSet);
        getLogger().debug(".getComponentMetricSetForPublishing(): Exit, publishedMetricSet->{}", publishedMetricsSet);
        return(publishedMetricsSet);
    }

    public ITOpsMetricsSet getComponentMetricsSet(String componentID){
        getLogger().debug(".getComponentMetricsSet(): Entry, componentID->{}", componentID);
        if(StringUtils.isEmpty(componentID)){
            return(null);
        }
        ITOpsMetricsSet currentState = getCurrentStateComponentMetricSetMap().get(componentID);
        getLogger().debug(".getComponentMetricsSet(): Exit, currentState->{}", currentState);
        return(currentState);
    }
}
