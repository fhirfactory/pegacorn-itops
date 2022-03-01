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

import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.reporting.PetasosProcessingPlantSubscriptionSummary;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.reporting.PetasosWorkUnitProcessorSubscriptionSummary;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ITOpsSystemWideSubscriptionMapDM {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsSystemWideSubscriptionMapDM.class);

    // ConcurrentHashMap<componentID, ProcessingPlantSubscriptionSummary>
    private ConcurrentHashMap<String, PetasosProcessingPlantSubscriptionSummary> processingPlantSubscriptionSummarySet;
    // ConcurrentHashMap<componentID, WorkUnitProcessorSubscriptionSummary>
    private ConcurrentHashMap<String, PetasosWorkUnitProcessorSubscriptionSummary> workUnitProcessorSubscriptionSummarySet;
    private Object publisherSubscriptionMapLock;
    private boolean updated;

    public ITOpsSystemWideSubscriptionMapDM(){
        this.processingPlantSubscriptionSummarySet = new ConcurrentHashMap<>();
        this.workUnitProcessorSubscriptionSummarySet = new ConcurrentHashMap<>();
        this.publisherSubscriptionMapLock = new Object();
    }

    //
    // Publisher Subscription Traceability
    //

    public void addProcessingPlantSubscriptionSummary(PetasosProcessingPlantSubscriptionSummary summary){
        LOG.debug(".addProcessingPlantSubscriptionSummary(): Entry, summary->{}", summary);
        synchronized (publisherSubscriptionMapLock) {
			if (processingPlantSubscriptionSummarySet.containsKey(summary.getComponentID().getId())) {
				LOG.debug(".addProcessingPlantSubscriptionSummary(): Summary is NOT unique, summary->{}", summary);
				processingPlantSubscriptionSummarySet.remove(summary.getComponentID().getId());
			} else {
            	 LOG.debug(".addProcessingPlantSubscriptionSummary(): Summary is unique, summary->{}", summary);
            	updated = true;
            }
            processingPlantSubscriptionSummarySet.put(summary.getComponentID().getId(), summary);
        }
        LOG.debug(".addProcessingPlantSubscriptionSummary(): Exit");
    }

    public void addWorkUnitProcessorSubscriptionSummary(PetasosWorkUnitProcessorSubscriptionSummary summary){
        LOG.debug(".addWorkUnitProcessorSubscriptionSummary(): Entry, summary->{}", summary);
        synchronized (publisherSubscriptionMapLock) {
            if (workUnitProcessorSubscriptionSummarySet.containsKey(summary.getComponentID().getId())) {
           	 	LOG.debug(".addWorkUnitProcessorSubscriptionSummary(): Summary is NOT unique, summary->{}", summary);
                workUnitProcessorSubscriptionSummarySet.remove(summary.getComponentID().getId());
            } else {
           	 	LOG.debug(".addWorkUnitProcessorSubscriptionSummary(): Summary is unique, summary->{}", summary);
            	updated = true;
            }
            workUnitProcessorSubscriptionSummarySet.put(summary.getComponentID().getId(), summary);
        }
        LOG.debug(".addWorkUnitProcessorSubscriptionSummary(): Exit" );
    }

    public PetasosProcessingPlantSubscriptionSummary getProcessingPlantPubSubReport(String componentID){
        LOG.debug(".getProcessingPlantPubSubReport(): Entry, componentID->{}", componentID);
        if(StringUtils.isEmpty(componentID)){
            LOG.debug(".getProcessingPlantPubSubReport(): Exit, componentID is empty");
            return(null);
        }
        PetasosProcessingPlantSubscriptionSummary summary = null;
        synchronized (publisherSubscriptionMapLock) {
            if (processingPlantSubscriptionSummarySet.containsKey(componentID)) {
                summary = processingPlantSubscriptionSummarySet.get(componentID);
            } else {
                LOG.debug(".getProcessingPlantPubSubReport(): Cannot find processing plant with given componentID");
            }
        }
        LOG.debug(".getProcessingPlantPubSubReport(): Exit, summary->{}", summary);
        return(summary);
    }

    public PetasosWorkUnitProcessorSubscriptionSummary getWorkUnitProcessorPubSubReport(String componentID){
        if(StringUtils.isEmpty(componentID)){
            return(null);
        }
        PetasosWorkUnitProcessorSubscriptionSummary summary = null;
        synchronized (publisherSubscriptionMapLock) {
            if (workUnitProcessorSubscriptionSummarySet.containsKey(componentID)) {
                summary = workUnitProcessorSubscriptionSummarySet.get(componentID);
            }
        }
        LOG.debug(".getWorkUnitProcessorPubSubReport(): Exit, summary->{}", summary);
        return(summary);
    }

    public List<PetasosProcessingPlantSubscriptionSummary> getProcessingPlantSubscriptionSummaries(){
        List<PetasosProcessingPlantSubscriptionSummary> subscriptionReportList = new ArrayList<>();
        synchronized (publisherSubscriptionMapLock) {
            subscriptionReportList.addAll(processingPlantSubscriptionSummarySet.values());
        }
        return(subscriptionReportList);
    }

    public List<PetasosWorkUnitProcessorSubscriptionSummary> getWorkUnitProcessorSubscriptionSummaries(){
        List<PetasosWorkUnitProcessorSubscriptionSummary> subscriptionReportList = new ArrayList<>();
        synchronized(publisherSubscriptionMapLock){
            subscriptionReportList.addAll(workUnitProcessorSubscriptionSummarySet.values());
        }
        return(subscriptionReportList);
    }

	public boolean isUpdated() {
		return updated;
	}

	public void setUpdated(boolean updated) {
		this.updated = updated;
	}
}
