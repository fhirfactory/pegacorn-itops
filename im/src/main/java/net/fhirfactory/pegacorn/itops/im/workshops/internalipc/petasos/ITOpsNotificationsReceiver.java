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

import net.fhirfactory.pegacorn.core.interfaces.oam.notifications.PetasosITOpsNotificationAgentInterface;
import net.fhirfactory.pegacorn.core.interfaces.oam.notifications.PetasosITOpsNotificationBrokerInterface;
import net.fhirfactory.pegacorn.core.interfaces.oam.notifications.PetasosITOpsNotificationHandlerInterface;
import net.fhirfactory.pegacorn.core.model.capabilities.base.CapabilityUtilisationRequest;
import net.fhirfactory.pegacorn.core.model.capabilities.base.CapabilityUtilisationResponse;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsNotificationsDM;
import net.fhirfactory.pegacorn.itops.im.workshops.internalipc.petasos.common.ITOpsReceiverBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ITOpsNotificationsReceiver extends ITOpsReceiverBase implements PetasosITOpsNotificationBrokerInterface, PetasosITOpsNotificationHandlerInterface, PetasosITOpsNotificationAgentInterface {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsNotificationsReceiver.class);

    @Inject
    private ITOpsNotificationsDM notificationsDM;

    //
    // Constructor(s)
    //

    //
    // Business Methods
    //

    @Override
    public void sendNotification(PetasosComponentITOpsNotification notification) {
        getLogger().info(".processNotification(): Entry, notification->{}", notification);
        //
        // We don't want to send notifications for the ITOps framework itself, it would create a cyclical event
        //
        // notificationsDM.addNotification(notification);
        getLogger().info(".processNotification(): Exit");
    }

    @Override
    public void processNotification(PetasosComponentITOpsNotification notification) {
        getLogger().info(".processNotification(): Entry, notification->{}", notification);
        notificationsDM.addNotification(notification);
        getLogger().info(".processNotification(): Exit");
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
