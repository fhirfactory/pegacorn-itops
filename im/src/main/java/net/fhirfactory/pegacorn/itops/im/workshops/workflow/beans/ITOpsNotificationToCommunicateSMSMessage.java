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

import java.util.UUID;

import static net.fhirfactory.pegacorn.internals.communicate.entities.message.valuesets.CommunicateSMSStatusEnum.CREATED;

@ApplicationScoped
public class ITOpsNotificationToCommunicateSMSMessage extends ITOpsNotificationToCommunicateMessage{
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsNotificationToCommunicateSMSMessage.class);

    private String targetPhoneNumber;
    private boolean resolvedTargetSMSPhoneNumber;

    private static final String ITOPS_EMAIL_TARGET_PHONE_NUMBER = "ERROR_EVENT_SMS_TARGET_PHONE_NUMBER";

    private static final String UNDEFINED_PHONE_NUMBER= "Undefined Phone Number";

    //
    // Constructor(s)
    //

    public ITOpsNotificationToCommunicateSMSMessage(){
        super();
        resolvedTargetSMSPhoneNumber = false;
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

    private String getTargetPhoneNumber(){
        if(!this.resolvedTargetSMSPhoneNumber){
            targetPhoneNumber = getProcessingPlant().getMeAsASoftwareComponent().getOtherConfigurationParameter(ITOPS_EMAIL_TARGET_PHONE_NUMBER);
            if (StringUtils.isEmpty(targetPhoneNumber)) {
                getLogger().error(".getSourceEmailAddress(): Cannot Resolve Source Address for ITOps Email Notifications");
                this.targetPhoneNumber = UNDEFINED_PHONE_NUMBER;
            } else {
                this.resolvedTargetSMSPhoneNumber = true;
            }
        }
        return(this.targetPhoneNumber);
    }

    public UoW transformNotificationIntoCommunicateSMS(UoW uow, Exchange camelExchange) {
        getLogger().info(".transformNotificationIntoCommunicateSMS(): Entry, uow->{}", uow);

        String targetPhone = getTargetPhoneNumber();

        PetasosComponentITOpsNotification notification = camelExchange.getProperty(getLocalItopsNotificationMessagePropertyName(), PetasosComponentITOpsNotification.class);

        if (uow == null) {
            uow = new UoW();
            uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            uow.setFailureDescription("No UoW Maintained in camelExchange");
        }

        if (uow.getProcessingOutcome().equals(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS)) {
            if (targetPhone.contentEquals(UNDEFINED_PHONE_NUMBER)) {
                uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                StringBuilder errorMessageBuilder = new StringBuilder();
                errorMessageBuilder.append("Error -> SMS Target is Undefined \n");
                uow.setFailureDescription(errorMessageBuilder.toString());
            } else {
                CommunicateSMSMessage smsMessage = new CommunicateSMSMessage();
                smsMessage.setPhoneNumber(getTargetPhoneNumber());
                smsMessage.setStatus(CREATED);
                if(StringUtils.isNotEmpty(notification.getContentHeading())) {
                    smsMessage.setMessage(notification.getContentHeading());
                } else {
                    smsMessage.setMessage("Error ("+notification.getParticipantName()+")");
                }
                smsMessage.setSimplifiedID("CommunicateSMSMessage:" + UUID.randomUUID().toString());
                smsMessage.setDescription("CommunicateSMSMessage: From(" + getProcessingPlant().getSubsystemParticipantName() + "), on behalf of (" + notification.getParticipantName() + ")");
                smsMessage.setDisplayName(getProcessingPlant().getSubsystemParticipantName() + "(" + smsMessage.getSimplifiedID() + ")");

                UoWPayload egressPayload = new UoWPayload();
                try {
                    String egressPayloadString = getJSONMapper().writeValueAsString(smsMessage);
                    egressPayload.setPayload(egressPayloadString);
                    DataParcelManifest egressPayloadManifest = new DataParcelManifest();
                    DataParcelTypeDescriptor smsMessageDescriptor = getMessageTopicFactory().createSMSTypeDescriptor();
                    egressPayloadManifest.setContentDescriptor(smsMessageDescriptor);
                    egressPayloadManifest.setSourceProcessingPlantParticipantName(getProcessingPlant().getSubsystemParticipantName());
                    egressPayloadManifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
                    egressPayloadManifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_TRUE);
                    egressPayloadManifest.setDataParcelFlowDirection(DataParcelDirectionEnum.INFORMATION_FLOW_WORKFLOW_OUTPUT);
                    egressPayloadManifest.setInterSubsystemDistributable(true);
                    egressPayload.setPayloadManifest(egressPayloadManifest);
                    uow.getEgressContent().addPayloadElement(egressPayload);
                    uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
                } catch (Exception ex) {
                    uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                    uow.setFailureDescription("Could not convert CommunicateSMSMessage to JSON String, error->" + ExceptionUtils.getMessage(ex));
                    getLogger().warn(".transformNotificationIntoCommunicateSMS(): Could not convert CommunicateEmailMessage to JSON String, error->{}" + ExceptionUtils.getMessage(ex));
                }
            }
        }
        getLogger().info(".transformNotificationIntoCommunicateSMS(): Exit, uow->{}", uow);
        return(uow);
    }
}
