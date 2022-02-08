/*
 * Copyright (c) 2021 Mark A. Hunter
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
package net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.reports.subscriptions;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MRoomMessageTypeEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MTextContentType;
import net.fhirfactory.pegacorn.communicate.matrixbridge.workshops.matrixbridge.common.RoomServerTransactionIDProvider;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.reporting.PetasosPublisherSubscriptionSummary;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.reporting.PetasosSubscriberSubscriptionSummary;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.reporting.PetasosWorkUnitProcessorSubscriptionSummary;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.work.datatypes.TaskWorkItemSubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

@ApplicationScoped
public class ParticipantSubscriptionReportEventFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantSubscriptionReportEventFactory.class);

    private DateTimeFormatter timeFormatter;

    @Inject
    private RoomServerTransactionIDProvider transactionIdProvider;

    @Inject
    private MatrixAccessToken accessToken;

    //
    // Constructor(s)
    //

    public ParticipantSubscriptionReportEventFactory(){
        this.timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS").withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));
    }

    //
    // Getters (and Setters)
    //

    protected DateTimeFormatter getTimeFormatter(){
        return(this.timeFormatter);
    }

    //
    // Business Methods
    //

    public MRoomTextMessageEvent newAsASubscriberSubscriptionReportEvent(String roomId, Collection<PetasosSubscriberSubscriptionSummary> subscriptionSummary){
        getLogger().debug(".newAsASubscriberSubscriptionReportEvent(): Entry, subscriptionSummary->{}", subscriptionSummary);
        if(subscriptionSummary == null){
            getLogger().debug(".newAsASubscriberSubscriptionReportEvent(): Exit, subscriptionSummary is null, returning -null-");
            return(null);
        }

        if(subscriptionSummary.isEmpty()){
            getLogger().debug(".newAsASubscriberSubscriptionReportEvent(): Exit, subscriptionSummary is empty, returning -null-");
            return(null);
        }

        MRoomTextMessageEvent subscriptionReportNotificationEvent = new MRoomTextMessageEvent();
        subscriptionReportNotificationEvent.setRoomIdentifier(roomId);
        subscriptionReportNotificationEvent.setEventIdentifier(transactionIdProvider.getNextAvailableID());
        subscriptionReportNotificationEvent.setSender(accessToken.getUserId());
        subscriptionReportNotificationEvent.setEventType("m.room.message");

        MTextContentType textContent = new MTextContentType();

        StringBuilder descriptionBuilder = new StringBuilder();
        for(PetasosSubscriberSubscriptionSummary currentSummary: subscriptionSummary){
            descriptionBuilder.append("Publisher --> "+ currentSummary.getPublisherParticipantName()+ "\n");
            descriptionBuilder.append("Subscriber --> "+ currentSummary.getParticipantName() + "\n");
            for(TaskWorkItemSubscriptionType currentTopic: currentSummary.getSubscribedTaskWorkItems()) {
                descriptionBuilder.append("*** ContentDescriptor --> " + currentTopic.getContentDescriptor().toDotString() + "\n");
                if(currentTopic.hasContainerDescriptor()) {
                    descriptionBuilder.append("*** ContainerDescriptor --> " + currentTopic.getContentDescriptor().toDotString() + "\n");
                }
                if(currentTopic.hasNormalisationStatus()) {
                    descriptionBuilder.append("*** Normalisation --> " + currentTopic.getNormalisationStatus().getDisplayName() + "\n");
                }
                if(currentTopic.hasValidationStatus()) {
                    descriptionBuilder.append("*** Validation --> " + currentTopic.getValidationStatus().getDisplayName() + "\n");
                }
                if(currentTopic.hasEnforcementPointApprovalStatus()) {
                    descriptionBuilder.append("*** PEP Approval --> " + currentTopic.getEnforcementPointApprovalStatus().getDisplayName() + "\n");
                }
                if(currentTopic.hasDataParcelFlowDirection()){
                    descriptionBuilder.append("*** Flow --> " + currentTopic.getDataParcelFlowDirection().getDisplayName() + "\n");
                }
                if(currentTopic.hasExternalSourceSystem()) {
                    descriptionBuilder.append("*** Source System --> " + currentTopic.getExternalSourceSystem()+ "\n");
                }
                if(currentTopic.hasExternalTargetSystem()) {
                    descriptionBuilder.append("*** Target System --> " + currentTopic.getExternalTargetSystem()+ "\n");
                }
            }
        }

        textContent.setBody(descriptionBuilder.toString());
        textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());

        subscriptionReportNotificationEvent.setContent(textContent);

        getLogger().debug(".newAsASubscriberSubscriptionReportEvent(): Exit, subscriptionReportNotificationEvent->{}", subscriptionReportNotificationEvent);
        return(subscriptionReportNotificationEvent);
    }


    public MRoomTextMessageEvent newAsAPublisherSubscriptionReportEvent(String roomId, Collection<PetasosPublisherSubscriptionSummary> publisherSummary){
        getLogger().debug(".newAsAPublisherSubscriptionReportEvent(): Entry, publisherSummary->{}", publisherSummary);
        if(publisherSummary == null){
            getLogger().debug(".newAsAPublisherSubscriptionReportEvent(): Exit, publisherSummary is null, returning -null-");
            return(null);
        }

        if(publisherSummary.isEmpty()){
            getLogger().debug(".newAsAPublisherSubscriptionReportEvent(): Exit, publisherSummary is empty, returning -null-");
            return(null);
        }

        MRoomTextMessageEvent publisherReportEvent = new MRoomTextMessageEvent();
        publisherReportEvent.setRoomIdentifier(roomId);
        publisherReportEvent.setEventIdentifier(transactionIdProvider.getNextAvailableID());
        publisherReportEvent.setSender(accessToken.getUserId());
        publisherReportEvent.setEventType("m.room.message");

        MTextContentType textContent = new MTextContentType();

        StringBuilder descriptionBuilder = new StringBuilder();
        for(PetasosPublisherSubscriptionSummary currentSummary: publisherSummary){
            getLogger().info(".newAsAPublisherSubscriptionReportEvent(): Processing currentSummary->{}", currentSummary);
            descriptionBuilder.append("Publisher --> "+ currentSummary.getParticipantName()+ "\n");
            descriptionBuilder.append("Subscriber --> "+ currentSummary.getSubscriberParticipantName() + "\n");
            for(TaskWorkItemSubscriptionType currentTopic: currentSummary.getSubscribedTaskWorkItems()) {
                descriptionBuilder.append("*** ContentDescriptor --> " + currentTopic.getContentDescriptor().toDotString() + "\n");
                if(currentTopic.hasContainerDescriptor()) {
                    descriptionBuilder.append("*** ContainerDescriptor --> " + currentTopic.getContentDescriptor().toDotString() + "\n");
                }
                if(currentTopic.hasNormalisationStatus()) {
                    descriptionBuilder.append("*** Normalisation --> " + currentTopic.getNormalisationStatus().getDisplayName() + "\n");
                }
                if(currentTopic.hasValidationStatus()) {
                    descriptionBuilder.append("*** Validation --> " + currentTopic.getValidationStatus().getDisplayName() + "\n");
                }
                if(currentTopic.hasEnforcementPointApprovalStatus()) {
                    descriptionBuilder.append("*** PEP Approval --> " + currentTopic.getEnforcementPointApprovalStatus().getDisplayName() + "\n");
                }
                if(currentTopic.hasDataParcelFlowDirection()){
                    descriptionBuilder.append("*** Flow --> " + currentTopic.getDataParcelFlowDirection().getDisplayName() + "\n");
                }
                if(currentTopic.hasExternalSourceSystem()) {
                    descriptionBuilder.append("*** Source System --> " + currentTopic.getExternalSourceSystem()+ "\n");
                }
                if(currentTopic.hasExternalTargetSystem()) {
                    descriptionBuilder.append("*** Target System --> " + currentTopic.getExternalTargetSystem()+ "\n");
                }
            }
        }

        textContent.setBody(descriptionBuilder.toString());
        textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());

        publisherReportEvent.setContent(textContent);

        getLogger().debug(".newAsAPublisherSubscriptionReportEvent(): Exit, publisherReportEvent->{}", publisherReportEvent);
        return(publisherReportEvent);
    }

    public MRoomTextMessageEvent newWUPSubscriberSubscriptionReportEvent(String roomId, PetasosWorkUnitProcessorSubscriptionSummary subscriptionSummary){
        getLogger().info(".newAsASubscriberSubscriptionReportEvent(): Entry, publisherSummary->{}", subscriptionSummary);
        if(subscriptionSummary == null){
            getLogger().debug(".newAsASubscriberSubscriptionReportEvent(): Exit, publisherSummary is null, returning -null-");
            return(null);
        }

        MRoomTextMessageEvent subscriptionReportNotificationEvent = new MRoomTextMessageEvent();
        subscriptionReportNotificationEvent.setRoomIdentifier(roomId);
        subscriptionReportNotificationEvent.setEventIdentifier(transactionIdProvider.getNextAvailableID());
        subscriptionReportNotificationEvent.setSender(accessToken.getUserId());
        subscriptionReportNotificationEvent.setEventType("m.room.message");

        MTextContentType textContent = new MTextContentType();

        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append("Subscriber --> "+ subscriptionSummary.getParticipantName()+ "\n");
        descriptionBuilder.append("ComponentId --> "+ subscriptionSummary.getComponentID() + "\n");
        for(TaskWorkItemSubscriptionType currentTopic: subscriptionSummary.getSubscribedTaskWorkItems()) {
            descriptionBuilder.append("*** ContentDescriptor --> " + currentTopic.getContentDescriptor().toDotString() + "\n");
            if(currentTopic.hasContainerDescriptor()) {
                descriptionBuilder.append("*** ContainerDescriptor --> " + currentTopic.getContentDescriptor().toDotString() + "\n");
            }
            if(currentTopic.hasNormalisationStatus()) {
                descriptionBuilder.append("*** Normalisation --> " + currentTopic.getNormalisationStatus().getDisplayName() + "\n");
            }
            if(currentTopic.hasValidationStatus()) {
                descriptionBuilder.append("*** Validation --> " + currentTopic.getValidationStatus().getDisplayName() + "\n");
            }
            if(currentTopic.hasEnforcementPointApprovalStatus()) {
                descriptionBuilder.append("*** PEP Approval --> " + currentTopic.getEnforcementPointApprovalStatus().getDisplayName() + "\n");
            }
            if(currentTopic.hasDataParcelFlowDirection()){
                descriptionBuilder.append("*** Flow --> " + currentTopic.getDataParcelFlowDirection().getDisplayName() + "\n");
            }
            if(currentTopic.hasExternalSourceSystem()) {
                descriptionBuilder.append("*** Source System --> " + currentTopic.getExternalSourceSystem()+ "\n");
            }
            if(currentTopic.hasExternalTargetSystem()) {
                descriptionBuilder.append("*** Target System --> " + currentTopic.getExternalTargetSystem()+ "\n");
            }
        }

        textContent.setBody(descriptionBuilder.toString());
        textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());

        subscriptionReportNotificationEvent.setContent(textContent);

        getLogger().info(".newAsASubscriberSubscriptionReportEvent(): Exit, subscriptionReportNotificationEvent->{}", subscriptionReportNotificationEvent);
        return(subscriptionReportNotificationEvent);
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }


}
