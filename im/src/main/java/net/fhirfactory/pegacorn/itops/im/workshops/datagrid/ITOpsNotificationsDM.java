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

import net.fhirfactory.pegacorn.core.interfaces.oam.notifications.PetasosITOpsNotificationBrokerInterface;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;

import javax.enterprise.context.ApplicationScoped;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ApplicationScoped
public class ITOpsNotificationsDM {

    Queue<PetasosComponentITOpsNotification> notificationQueue;

    //
    // Constructor(s)
    //

    public ITOpsNotificationsDM(){
        this.notificationQueue = new ConcurrentLinkedQueue<>();
    }

    //
    // Getters and Setters
    //

    public Queue<PetasosComponentITOpsNotification> getNotificationQueue() {
        return notificationQueue;
    }

    //
    // Helpers
    //

    public void addNotification(PetasosComponentITOpsNotification notification){
        if(notification == null){
            return;
        }
        getNotificationQueue().add(notification);
    }

    public PetasosComponentITOpsNotification getNextNotification(){
        if(getNotificationQueue().isEmpty()){
            return(null);
        }
        PetasosComponentITOpsNotification nextNotification = getNotificationQueue().poll();
        return(nextNotification);
    }

    public boolean hasMoreNotifications(){
        boolean hasMore = (getNotificationQueue().isEmpty() != true);
        return(hasMore);
    }
}
