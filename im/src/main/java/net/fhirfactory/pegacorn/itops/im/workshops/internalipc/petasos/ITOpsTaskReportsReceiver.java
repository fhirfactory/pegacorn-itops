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
package net.fhirfactory.pegacorn.itops.im.workshops.internalipc.petasos;

import net.fhirfactory.pegacorn.core.interfaces.oam.tasks.PetasosITOpsTaskReportingAgentInterface;
import net.fhirfactory.pegacorn.core.interfaces.oam.tasks.PetasosITOpsTaskReportingBrokerInterface;
import net.fhirfactory.pegacorn.core.interfaces.oam.tasks.PetasosITOpsTaskReportingHandlerInterface;
import net.fhirfactory.pegacorn.core.model.capabilities.use.CapabilityUtilisationRequest;
import net.fhirfactory.pegacorn.core.model.capabilities.use.CapabilityUtilisationResponse;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsTaskReportsDM;
import net.fhirfactory.pegacorn.itops.im.workshops.internalipc.petasos.common.ITOpsReceiverBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ITOpsTaskReportsReceiver extends ITOpsReceiverBase implements PetasosITOpsTaskReportingBrokerInterface, PetasosITOpsTaskReportingHandlerInterface, PetasosITOpsTaskReportingAgentInterface {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsTaskReportsReceiver.class);

    @Inject
    private ITOpsTaskReportsDM taskReportsDM;

    //
    // Constructor(s)
    //

    //
    // Business Methods
    //

    public void sendTaskReport(PetasosComponentITOpsNotification notification) {
        getLogger().debug(".sendTaskReport(): Entry, notification->{}", notification);
        //
        // We don't want to send taskReports for the ITOps framework itself, it would create a cyclical event
        //
        //  taskReportsDM.addTaskReport(notification);
        getLogger().debug(".sendTaskReport(): Exit");
    }

    public void processTaskReport(PetasosComponentITOpsNotification notification) {
        getLogger().debug(".processTaskReport(): Entry, notification->{}", notification);
        taskReportsDM.addTaskReport(notification);
        getLogger().debug(".processTaskReport(): Exit");
    }

    @Override
    public CapabilityUtilisationResponse executeTask(CapabilityUtilisationRequest request) {
        return null;
    }

    @Override
    protected void registerCapabilities() {

    }

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    @Override
    protected void cacheMonitorProcess() {

    }

    @Override
    protected String cacheMonitorProcessTimerName() {
        return ("ITOpsNotificationsHandlerDaemonTimer");
    }
}
