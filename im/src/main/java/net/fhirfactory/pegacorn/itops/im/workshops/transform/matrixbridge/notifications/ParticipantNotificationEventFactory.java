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
package net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.notifications;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MRoomMessageTypeEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MTextContentType;
import net.fhirfactory.pegacorn.communicate.matrixbridge.workshops.matrixbridge.common.RoomServerTransactionIDProvider;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.ITOpsNotification;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class ParticipantNotificationEventFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantNotificationEventFactory.class);

    @Inject
    private RoomServerTransactionIDProvider transactionIdProvider;

    @Inject
    private MatrixAccessToken accessToken;

    //
    // Business Methods
    //

    public MRoomTextMessageEvent newNotificationEvent(String roomId, ITOpsNotification notification){
        getLogger().debug(".newNotificationEvent(): Entry, notification->{}", notification);
        if(notification == null){
            getLogger().debug(".newNotificationEvent(): Exit, notification is null, returning empty list");
            return(null);
        }

        MRoomTextMessageEvent notificationEvent = new MRoomTextMessageEvent();
        notificationEvent.setRoomIdentifier(roomId);
        notificationEvent.setEventIdentifier(transactionIdProvider.getNextAvailableID());
        notificationEvent.setSender(accessToken.getUserId());
        notificationEvent.setEventType("m.room.message");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));

        MTextContentType textContent = new MTextContentType();
        if(StringUtils.isNoneEmpty(notification.getFormattedContent())){
            textContent.setBody(notification.getContent());
            textContent.setFormattedBody(notification.getFormattedContent());
            textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());
            textContent.setFormat("org.matrix.custom.html");
        } else {
            textContent.setBody(notification.getContent());
            textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());
        }

        notificationEvent.setContent(textContent);

        getLogger().debug(".newNotificationEvent(): Exit, metricsEventList->{}", notificationEvent);
        return(notificationEvent);
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }


}
