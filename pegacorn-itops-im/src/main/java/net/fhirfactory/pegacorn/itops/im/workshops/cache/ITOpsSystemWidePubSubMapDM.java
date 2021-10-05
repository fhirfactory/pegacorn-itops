/*
 * Copyright (c) 2020 MAHun
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
import net.fhirfactory.pegacorn.petasos.model.itops.subscriptions.ProcessingPlantSubscriptionSummary;
import net.fhirfactory.pegacorn.petasos.model.itops.subscriptions.WorkUnitProcessorSubscriptionSummary;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ITOpsSystemWidePubSubMapDM extends ITOpsLocalDMRefreshBase {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsSystemWidePubSubMapDM.class);

    // ConcurrentHashMap<componentID, ProcessingPlantSubscriptionSummary>
    private ConcurrentHashMap<String, ProcessingPlantSubscriptionSummary> processingPlantSubscriptionSummarySet;
    // ConcurrentHashMap<componentID, WorkUnitProcessorSubscriptionSummary>
    private ConcurrentHashMap<String, WorkUnitProcessorSubscriptionSummary> workUnitProcessorSubscriptionSummarySet;
    private Object publisherSubscriptionMapLock;

    public ITOpsSystemWidePubSubMapDM(){
        this.processingPlantSubscriptionSummarySet = new ConcurrentHashMap<>();
        this.workUnitProcessorSubscriptionSummarySet = new ConcurrentHashMap<>();
        this.publisherSubscriptionMapLock = new Object();

    }

    //
    // Publisher Subscription Traceability
    //

    public void addProcessingPlantSubscriptionSummary(ProcessingPlantSubscriptionSummary summary){
        LOG.debug(".addProcessingPlantSubscriptionSummary(): Entry, summary->{}", summary);
        if(processingPlantSubscriptionSummarySet.containsKey(summary.getComponentID())){
            processingPlantSubscriptionSummarySet.remove(summary.getComponentID());
        }
        processingPlantSubscriptionSummarySet.put(summary.getComponentID(), summary);
        refreshCurrentStateUpdateInstant();
        LOG.debug(".addProcessingPlantSubscriptionSummary(): Exit");
    }

    public void addWorkUnitProcessorSubscriptionSummary(WorkUnitProcessorSubscriptionSummary summary){
        LOG.debug(".addWorkUnitProcessorSubscriptionSummary(): Entry, summary->{}", summary);
        if(workUnitProcessorSubscriptionSummarySet.containsKey(summary.getSubscriber())){
            workUnitProcessorSubscriptionSummarySet.remove(summary.getSubscriber());
        }
        workUnitProcessorSubscriptionSummarySet.put(summary.getSubscriber(), summary);
        refreshCurrentStateUpdateInstant();
        LOG.debug(".addWorkUnitProcessorSubscriptionSummary(): Exit" );
    }

    public ProcessingPlantSubscriptionSummary getProcessingPlantPubSubReport(String componentID){
        LOG.debug(".getProcessingPlantPubSubReport(): Entry, componentID->{}", componentID);
        if(StringUtils.isEmpty(componentID)){
            LOG.debug(".getProcessingPlantPubSubReport(): Exit, componentID is empty");
            return(null);
        }
        if(processingPlantSubscriptionSummarySet.containsKey(componentID)){
            ProcessingPlantSubscriptionSummary summary = processingPlantSubscriptionSummarySet.get(componentID);
            return(summary);
        } else {
            LOG.debug(".getProcessingPlantPubSubReport(): Exit, cannot find processing plant with given componentID");
            return(null);
        }
    }

    public WorkUnitProcessorSubscriptionSummary getWorkUnitProcessorPubSubReport(String componentID){
        if(StringUtils.isEmpty(componentID)){
            return(null);
        }
        if(workUnitProcessorSubscriptionSummarySet.containsKey(componentID)){
            WorkUnitProcessorSubscriptionSummary summary = workUnitProcessorSubscriptionSummarySet.get(componentID);
            return(summary);
        } else {
            return(null);
        }
    }

}
