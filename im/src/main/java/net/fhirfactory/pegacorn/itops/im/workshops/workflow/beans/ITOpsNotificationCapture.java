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
package net.fhirfactory.pegacorn.itops.im.workshops.workflow.beans;

import net.fhirfactory.pegacorn.core.model.petasos.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.model.petasos.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWProcessingOutcomeEnum;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ITOpsNotificationCapture extends ITOpsNotificationToCommunicateMessage{
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsNotificationToCommunicateEmailMessage.class);


    //
    // Constructor(s)
    //

    public ITOpsNotificationCapture(){
        super();
    }

    //
    // Implemented Abstract Methods
    //

    @Override
    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Methods
    //


    public UoW captureNotification(PetasosComponentITOpsNotification notification, Exchange camelExchange){
        getLogger().info(".captureNotification(): Entry, notification->{}", notification);

        String errorCondition = null;
        String notificationAsString = null;
        try{
            notificationAsString = getJSONMapper().writeValueAsString(notification);
        } catch(Exception ex){
            errorCondition = ExceptionUtils.getMessage(ex);
            getLogger().warn(".transformNotificationIntoCommunicateEmail(): Cannot convert notification to String, message->{}", errorCondition);
        }

        UoWPayload ingresPayload = new UoWPayload();
        if(StringUtils.isNotEmpty(notificationAsString)){
            ingresPayload.setPayload(notificationAsString);
        } else {
            ingresPayload.setPayload("PetasosComponentITOpsNotification (cannot convert to JSON String)");
        }
        DataParcelManifest manifest = new DataParcelManifest();
        DataParcelTypeDescriptor descriptor = new DataParcelTypeDescriptor();
        descriptor.setDataParcelDefiner("FHIRFactory");
        descriptor.setDataParcelCategory("Petasos");
        descriptor.setDataParcelSubCategory("Operations, Administration and Maintenance");
        descriptor.setDataParcelResource("PetasosComponentITOpsNotification");
        manifest.setContentDescriptor(descriptor);
        manifest.setSourceProcessingPlantParticipantName(getProcessingPlant().getSubsystemParticipantName());
        ingresPayload.setPayloadManifest(manifest);
        UoW uow = new UoW(ingresPayload);

        if((StringUtils.isEmpty(notificationAsString))){
            uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            uow.setFailureDescription(".transformNotificationIntoCommunicateEmail(): Cannot convert notification to String, message->" + errorCondition);
        }

        getLogger().info(".captureNotification(): notification->{}", notification);
        camelExchange.setProperty(getLocalItopsNotificationMessagePropertyName(), notification);

        getLogger().info(".captureNotification(): Exit, uow->{}", uow);
        return(uow);
    }
}
