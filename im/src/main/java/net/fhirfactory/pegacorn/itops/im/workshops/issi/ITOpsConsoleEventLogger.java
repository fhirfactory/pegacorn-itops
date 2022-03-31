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
package net.fhirfactory.pegacorn.itops.im.workshops.issi;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixInstantMessageMethods;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixRoomMethods;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.common.MAPIResponse;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.valuesets.PetasosComponentITOpsNotificationTypeEnum;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownRoomAndSpaceMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.ParticipantRoomIdentityFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.notifications.ParticipantNotificationEventFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class ITOpsConsoleEventLogger {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsConsoleEventLogger.class);

    private DateTimeFormatter timeFormatter;

    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    @Inject
    private ITOpsKnownRoomAndSpaceMapDM roomCache;

    @Inject
    private ParticipantNotificationEventFactory notificationEventFactory;

    @Inject
    private MatrixRoomMethods matrixRoomAPI;

    @Inject
    private MatrixInstantMessageMethods matrixInstantMessageAPI;

    @Inject
    private MatrixAccessToken matrixAccessToken;

    @Inject
    private ProcessingPlantInterface processingPlant;

    //
    // Constructor(s)
    //

    public ITOpsConsoleEventLogger(){
        this.timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected ParticipantRoomIdentityFactory getRoomIdentityFactory(){
        return(roomIdentityFactory);
    }

    protected ITOpsKnownRoomAndSpaceMapDM getRoomCache(){
        return(this.roomCache);
    }

    protected ParticipantNotificationEventFactory getNotificationEventFactory(){
        return(this.notificationEventFactory);
    }

    protected MatrixRoomMethods getMatrixRoomAPI(){
        return(this.matrixRoomAPI);
    }

    protected MatrixInstantMessageMethods getMatrixInstantMessageAPI(){
        return(this.matrixInstantMessageAPI);
    }

    protected MatrixAccessToken getMatrixAccessToken(){
        return(this.matrixAccessToken);
    }

    protected DateTimeFormatter getTimeFormatter(){
        return(this.timeFormatter);
    }

    protected ProcessingPlantInterface getProcessingPlant(){
        return(this.processingPlant);
    }

    //
    // Business Methods
    //

    public void logConsoleEvent(String event){
        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("Event( " + getTimeFormatter().format(Instant.now()) + ")\n");
        messageBuilder.append(event);

        String message = messageBuilder.toString();

        PetasosComponentITOpsNotification notification = new PetasosComponentITOpsNotification();
        notification.setContent(message);
        notification.setParticipantName(processingPlant.getSubsystemParticipantName());
        notification.setNotificationType(PetasosComponentITOpsNotificationTypeEnum.SUCCESS_NOTIFICATION_TYPE);
        notification.setComponentId(processingPlant.getMeAsASoftwareComponent().getComponentID());

        logConsoleEvent(notification);
    }

    protected void logConsoleEvent(PetasosComponentITOpsNotification notification){
        getLogger().debug(".sendConnectivityReport(): Entry, notification->{}", notification);
        try {
            String roomAlias = getRoomIdentityFactory().buildProcessingPlantRoomPseudoAlias(notification.getParticipantName(), OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_CONSOLE);

            getLogger().debug(".forwardWUPNotification(): roomAlias for Events->{}", roomAlias);

            String roomIdFromAlias =  getRoomCache().getRoomIdFromPseudoAlias(roomAlias);

            getLogger().debug(".forwardWUPNotification(): roomId for Events->{}", roomIdFromAlias);

            if(roomIdFromAlias != null) {
                getLogger().debug(".sendConnectivityReport(): [Building MessageEvent] Start");
                MRoomTextMessageEvent notificationEvent = notificationEventFactory.newNotificationEvent(roomIdFromAlias, notification);
                getLogger().debug(".sendConnectivityReport(): [Building MessageEvent] Finish");
                try {
                    getLogger().debug(".sendConnectivityReport(): [Sending MessageEvent] Start");
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), notificationEvent);
                    getLogger().debug(".sendConnectivityReport(): [Sending MessageEvent] mapiResponse->{}", mapiResponse);
                    getLogger().debug(".sendConnectivityReport(): [Sending MessageEvent] Finish");
                } catch(Exception ex){
                    getLogger().warn(".sendConnectivityReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                    return;
                }
            } else {
                getLogger().warn(".sendConnectivityReport(): No room to forward work unit processor notifications into (ProcessingPlant->{}), ITOps Room Pseudo Alias ->{}", notification.getParticipantName(), roomAlias);
                // TODO either re-queue or send to DeadLetter
                return;
            }
        } catch (Exception ex) {
            getLogger().warn(".sendConnectivityReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return;
        }
    }
}
