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
package net.fhirfactory.pegacorn.itops.im.workshops.transform.factories;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MRoomMessageTypeEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MTextContentType;
import net.fhirfactory.pegacorn.communicate.matrixbridge.workshops.matrixbridge.common.RoomServerTransactionIDProvider;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.reporting.PetasosSubscriberSubscriptionSummary;
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
            getLogger().debug(".newAsASubscriberSubscriptionReportEvent(): Exit, subscriptionSummary is null, returning empty list");
            return(null);
        }

        MRoomTextMessageEvent subscriptionReportNotificationEvent = new MRoomTextMessageEvent();
        subscriptionReportNotificationEvent.setRoomIdentifier(roomId);
        subscriptionReportNotificationEvent.setEventIdentifier(transactionIdProvider.getNextAvailableID());
        subscriptionReportNotificationEvent.setSender(accessToken.getMatrixUserId());
        subscriptionReportNotificationEvent.setEventType("m.room.message");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));

        MTextContentType textContent = new MTextContentType();
        textContent.setBody(subscriptionSummary.toString());
        textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());

        subscriptionReportNotificationEvent.setContent(textContent);

        getLogger().debug(".newAsASubscriberSubscriptionReportEvent(): Exit, subscriptionReportNotificationEvent->{}", subscriptionReportNotificationEvent);
        return(subscriptionReportNotificationEvent);
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }


}
