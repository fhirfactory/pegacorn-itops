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
package net.fhirfactory.pegacorn.itops.im.workshops.interact.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.fhirfactory.pegacorn.components.capabilities.base.CapabilityUtilisationRequest;
import net.fhirfactory.pegacorn.itops.im.workshops.cache.ITOpsSystemWidePubSubMapDM;
import net.fhirfactory.pegacorn.petasos.model.itops.metrics.ITOpsMetricsSet;
import net.fhirfactory.pegacorn.petasos.model.itops.subscriptions.ProcessingPlantSubscriptionSummary;
import net.fhirfactory.pegacorn.petasos.model.itops.subscriptions.WorkUnitProcessorSubscriptionSummary;
import org.apache.camel.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ITOpsPubSubReportHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsPubSubReportHandler.class);

    private ObjectMapper jsonMapper;

    @Inject
    private ITOpsSystemWidePubSubMapDM pubSubMapDM;

    public ITOpsPubSubReportHandler(){
        jsonMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        jsonMapper.registerModule(module);
    }


    public ProcessingPlantSubscriptionSummary retrieveProcessingPlantPubSubReport(@Header("componentId") String componentId){
        LOG.debug(".retrieveProcessingPlantPubSubReport(): Entry, componentId->{}", componentId);
        ProcessingPlantSubscriptionSummary processingPlantPubSubReport = pubSubMapDM.getProcessingPlantPubSubReport(componentId);
        LOG.debug(".retrieveProcessingPlantPubSubReport(): Exit");
        return(processingPlantPubSubReport);
    }

    public WorkUnitProcessorSubscriptionSummary retrieveWorkUnitProcessorPubSubReport(@Header("componentId")String componentId){
        LOG.debug(".retrieveWorkUnitProcessorPubSubReport(): Entry, componentId->{}", componentId);
        WorkUnitProcessorSubscriptionSummary wupPubSubReport = pubSubMapDM.getWorkUnitProcessorPubSubReport(componentId);
        LOG.debug(".retrieveWorkUnitProcessorPubSubReport(): Exit");
        return(wupPubSubReport);
    }
}
