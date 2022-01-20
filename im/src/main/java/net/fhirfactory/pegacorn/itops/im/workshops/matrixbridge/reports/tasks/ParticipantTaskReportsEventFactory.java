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
package net.fhirfactory.pegacorn.itops.im.workshops.matrixbridge.reports.tasks;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MRoomMessageTypeEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MTextContentType;
import net.fhirfactory.pegacorn.communicate.matrixbridge.workshops.matrixbridge.common.RoomServerTransactionIDProvider;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class ParticipantTaskReportsEventFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantTaskReportsEventFactory.class);

    @Inject
    private RoomServerTransactionIDProvider transactionIdProvider;

    @Inject
    private MatrixAccessToken accessToken;

    //
    // Business Methods
    //

    public MRoomTextMessageEvent newTaskReportEvent(String roomId, PetasosComponentITOpsNotification taskReportNotification){
        getLogger().debug(".newTaskReportEvent(): Entry, taskReportNotification->{}", taskReportNotification);
        if(taskReportNotification == null){
            getLogger().debug(".newTaskReportEvent(): Exit, taskReportNotification is null, returning empty list");
            return(null);
        }

        MRoomTextMessageEvent taskReportMessage = new MRoomTextMessageEvent();
        taskReportMessage.setRoomIdentifier(roomId);
        taskReportMessage.setEventIdentifier(transactionIdProvider.getNextAvailableID());
        taskReportMessage.setSender(accessToken.getMatrixUserId());
        taskReportMessage.setEventType("m.room.message");

        MTextContentType textContent = new MTextContentType();
        textContent.setBody(taskReportNotification.getContent());
        textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());
        textContent.setFormattedBody(taskReportNotification.getFormattedContent());
        textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());
        textContent.setFormat("org.matrix.custom.html");

        taskReportMessage.setContent(textContent);

        getLogger().debug(".newTaskReportEvent(): Exit, metricsEventList->{}", taskReportMessage);
        return(taskReportMessage);
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }


}
