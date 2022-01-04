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

import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.PetasosProcessingPlantSubscriptionSummary;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.PetasosSubscriptionSummaryReport;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.PetasosWorkUnitProcessorSubscriptionSummary;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ITOpsSystemWideSubscriptionMapDM {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsSystemWideSubscriptionMapDM.class);

    // ConcurrentHashMap<componentID, ProcessingPlantSubscriptionSummary>
    private ConcurrentHashMap<String, PetasosProcessingPlantSubscriptionSummary> processingPlantSubscriptionSummarySet;
    // ConcurrentHashMap<componentID, WorkUnitProcessorSubscriptionSummary>
    private ConcurrentHashMap<String, PetasosWorkUnitProcessorSubscriptionSummary> workUnitProcessorSubscriptionSummarySet;
    private Object publisherSubscriptionMapLock;

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
        if(processingPlantSubscriptionSummarySet.containsKey(summary.getComponentID())){
            processingPlantSubscriptionSummarySet.remove(summary.getComponentID());
        }
        processingPlantSubscriptionSummarySet.put(summary.getComponentID().getId(), summary);
        LOG.debug(".addProcessingPlantSubscriptionSummary(): Exit");
    }

    public void addWorkUnitProcessorSubscriptionSummary(PetasosWorkUnitProcessorSubscriptionSummary summary){
        LOG.debug(".addWorkUnitProcessorSubscriptionSummary(): Entry, summary->{}", summary);
        if(workUnitProcessorSubscriptionSummarySet.containsKey(summary.getSubscriber())){
            workUnitProcessorSubscriptionSummarySet.remove(summary.getSubscriber());
        }
        workUnitProcessorSubscriptionSummarySet.put(summary.getSubscriber().getId(), summary);
        LOG.debug(".addWorkUnitProcessorSubscriptionSummary(): Exit" );
    }

    public PetasosProcessingPlantSubscriptionSummary getProcessingPlantPubSubReport(String componentID){
        LOG.debug(".getProcessingPlantPubSubReport(): Entry, componentID->{}", componentID);
        if(StringUtils.isEmpty(componentID)){
            LOG.debug(".getProcessingPlantPubSubReport(): Exit, componentID is empty");
            return(null);
        }
        if(processingPlantSubscriptionSummarySet.containsKey(componentID)){
            PetasosProcessingPlantSubscriptionSummary summary = processingPlantSubscriptionSummarySet.get(componentID);
            return(summary);
        } else {
            LOG.debug(".getProcessingPlantPubSubReport(): Exit, cannot find processing plant with given componentID");
            return(null);
        }
    }

    public PetasosWorkUnitProcessorSubscriptionSummary getWorkUnitProcessorPubSubReport(String componentID){
        if(StringUtils.isEmpty(componentID)){
            return(null);
        }
        if(workUnitProcessorSubscriptionSummarySet.containsKey(componentID)){
            PetasosWorkUnitProcessorSubscriptionSummary summary = workUnitProcessorSubscriptionSummarySet.get(componentID);
            return(summary);
        } else {
            return(null);
        }
    }
}
