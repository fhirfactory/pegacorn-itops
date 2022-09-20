/*
 * Copyright (c) 2022 Mark A. Hunter
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
package net.fhirfactory.pegacorn.itops.im.workshops.workflow;

import net.fhirfactory.dricats.interfaces.topology.WorkshopInterface;
import net.fhirfactory.dricats.model.petasos.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.itops.im.common.ITOpsIMNames;
import net.fhirfactory.pegacorn.itops.im.workshops.workflow.beans.ITOpsNotificationCapture;
import net.fhirfactory.pegacorn.itops.im.workshops.workflow.beans.ITOpsNotificationToCommunicateEmailMessage;
import net.fhirfactory.pegacorn.itops.im.workshops.workflow.beans.ITOpsNotificationToCommunicateSMSMessage;
import net.fhirfactory.pegacorn.petasos.wup.helper.IngresActivityBeginRegistration;
import net.fhirfactory.dricats.petasos.participant.workshops.WorkflowWorkshop;
import net.fhirfactory.dricats.petasos.participant.wup.messagebased.StimuliTriggeredWorkflowWUP;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class StatusMessageGenerationWUP extends StimuliTriggeredWorkflowWUP {
    private static final Logger LOG = LoggerFactory.getLogger(StatusMessageGenerationWUP.class);

    @Inject
    private WorkflowWorkshop workshop;

    @Inject
    private ITOpsIMNames itopsNames;

    @Inject
    private IngresActivityBeginRegistration ingresActivityBeginRegistration;

    @Inject
    private ITOpsNotificationCapture notificationCaptureBean;

    @Inject
    private ITOpsNotificationToCommunicateEmailMessage emailMessageGeneratorBean;

    @Inject
    private ITOpsNotificationToCommunicateSMSMessage smsMessageGeneratorBean;

    @Inject
    private IngresActivityBeginRegistration activityBeginRegistration;

    @Override
    protected List<DataParcelManifest> specifySubscriptionTopics() {
        return (new ArrayList<>());
    }

    @Override
    protected List<DataParcelManifest> declarePublishedTopics() {
        return (new ArrayList<>());
    }

    @Override
    protected String specifyWUPInstanceName() {
        return (getClass().getSimpleName());
    }

    @Override
    protected String specifyWUPInstanceVersion() {
        return ("1.0.0");
    }

    @Override
    protected WorkshopInterface specifyWorkshop() {
        return (workshop);
    }

    @Override
    protected Logger specifyLogger() {
        return (LOG);
    }

    @Override
    public void configure() throws Exception {
        getLogger().info("{}({})->{}", getMeAsAPetasosParticipant().getParticipantName(), getClass().getSimpleName(), ingresFeed());
        getLogger().info("{}({})->{}", getMeAsAPetasosParticipant().getParticipantName(), getClass().getSimpleName(), egressFeed());

        fromIncludingPetasosServices(ingresFeed())
                .to(itopsNames.getITOpsNotificationToCommunicateMessageIngresFeed());

        fromIncludingPetasosServices(itopsNames.getITOpsNotificationToCommunicateMessageIngresFeed())
                .bean(notificationCaptureBean, "captureNotification(*, Exchange)")
                .bean(activityBeginRegistration, "registerActivityStart(*,  Exchange)")
                .bean(emailMessageGeneratorBean, "transformNotificationIntoCommunicateEmail(*, Exchange)")
                .bean(smsMessageGeneratorBean,"transformNotificationIntoCommunicateSMS(*, Exchange)")
                .to(egressFeed());
    }

    //
    // Route Helper Functions
    //

    protected RouteDefinition fromIncludingPetasosServices(String uri) {
        NodeDetailInjector nodeDetailInjector = new NodeDetailInjector();
        AuditAgentInjector auditAgentInjector = new AuditAgentInjector();
        TaskReportAgentInjector taskReportAgentInjector = new TaskReportAgentInjector();
        RouteDefinition route = fromWithStandardExceptionHandling(uri);
        route
                .process(nodeDetailInjector)
                .process(auditAgentInjector)
                .process(taskReportAgentInjector)
        ;
        return route;
    }
}
