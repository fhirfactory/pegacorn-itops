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

import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelNormalisationStatusEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelValidationStatusEnum;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.pegacorn.internals.communicate.entities.message.CommunicateSMSMessage;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static net.fhirfactory.pegacorn.internals.communicate.entities.message.valuesets.CommunicateSMSStatusEnum.CREATED;

@ApplicationScoped
public class ITOpsNotificationToCommunicateSMSMessage extends ITOpsNotificationToCommunicateMessage{
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsNotificationToCommunicateSMSMessage.class);

    private List<String> targetPhoneNumbers = null;

    private static final String ITOPS_SMS_TARGET_PHONE_NUMBER = "ERROR_EVENT_SMS_TARGET_PHONE_NUMBER";

    //
    // Constructor(s)
    //

    public ITOpsNotificationToCommunicateSMSMessage(){
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

    private Iterator<String> getTargetPhoneNumbers() {
        if (targetPhoneNumbers == null){
            targetPhoneNumbers = new ArrayList<>();
            String targetPhoneNumbersStr = getProcessingPlant().getMeAsASoftwareComponent().getOtherConfigurationParameter(ITOPS_SMS_TARGET_PHONE_NUMBER);
            if (targetPhoneNumbersStr != null) {
                for (String phoneNumber : targetPhoneNumbersStr.split(",")) {
                    if (!StringUtils.isEmpty(phoneNumber)) {
                        targetPhoneNumbers.add(phoneNumber);
                    }
                }
            }
        }
        return(this.targetPhoneNumbers.iterator());
    }

    public UoW transformNotificationIntoCommunicateSMS(UoW uow, Exchange camelExchange) {
        getLogger().info(".transformNotificationIntoCommunicateSMS(): Entry, uow->{}", uow);

        Iterator<String> targetPhones = getTargetPhoneNumbers();

        PetasosComponentITOpsNotification notification = camelExchange.getProperty(getLocalItopsNotificationMessagePropertyName(), PetasosComponentITOpsNotification.class);

        if (uow == null) {
            uow = new UoW();
            uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            uow.setFailureDescription("No UoW Maintained in camelExchange");
        }

        if (!uow.getProcessingOutcome().equals(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED)) {
            if (!targetPhones.hasNext()) {
                uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                StringBuilder errorMessageBuilder = new StringBuilder();
                errorMessageBuilder.append("Error -> SMS Target is Undefined \n");
                uow.setFailureDescription(errorMessageBuilder.toString());
            } else {
                DataParcelManifest egressPayloadManifest = new DataParcelManifest();
                DataParcelTypeDescriptor smsMessageDescriptor = getMessageTopicFactory().createSMSTypeDescriptor();
                egressPayloadManifest.setContentDescriptor(smsMessageDescriptor);
                egressPayloadManifest.setSourceProcessingPlantParticipantName(getProcessingPlant().getSubsystemParticipantName());
                egressPayloadManifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
                egressPayloadManifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_TRUE);
                egressPayloadManifest.setDataParcelFlowDirection(DataParcelDirectionEnum.INFORMATION_FLOW_WORKFLOW_OUTPUT);
                egressPayloadManifest.setInterSubsystemDistributable(true);
                
                String message;
                if(StringUtils.isNotEmpty(notification.getContentHeading())) {
                    message =notification.getContentHeading();
                } else {
                    message = "Error ("+notification.getParticipantName()+")";
                }
                String description = "CommunicateSMSMessage: From(" + getProcessingPlant().getSubsystemParticipantName() + "), on behalf of (" + notification.getParticipantName() + ")";
                
                do {
                    CommunicateSMSMessage smsMessage = new CommunicateSMSMessage();
                    smsMessage.setPhoneNumber(targetPhones.next());
                    smsMessage.setStatus(CREATED);
                    smsMessage.setMessage(message);
                    smsMessage.setSimplifiedID("CommunicateSMSMessage:" + UUID.randomUUID().toString());  // use a new ID for each SMS
                    smsMessage.setDescription(description);
                    smsMessage.setDisplayName(getProcessingPlant().getSubsystemParticipantName() + "(" + smsMessage.getSimplifiedID() + ")");
                    
                    String egressPayloadString;
                    try {
                        egressPayloadString = getJSONMapper().writeValueAsString(smsMessage);
                    } catch (Exception ex) {
                        uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                        uow.setFailureDescription("Could not convert CommunicateSMSMessage to JSON String, error->" + ExceptionUtils.getMessage(ex));
                        getLogger().warn(".transformNotificationIntoCommunicateSMS(): Could not convert CommunicateEmailMessage to JSON String, error->{}" + ExceptionUtils.getMessage(ex));
                        break;
                    }
                    
                    UoWPayload egressPayload = new UoWPayload(egressPayloadManifest, egressPayloadString);
                    egressPayload.setPayloadManifest(egressPayloadManifest);
                    
                    uow.getEgressContent().addPayloadElement(egressPayload);
                } while (targetPhones.hasNext());
                
                if (!targetPhones.hasNext()) {
                    uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
                }
            }
        }
        getLogger().info(".transformNotificationIntoCommunicateSMS(): Exit, uow->{}", uow);
        return(uow);
    }
}
