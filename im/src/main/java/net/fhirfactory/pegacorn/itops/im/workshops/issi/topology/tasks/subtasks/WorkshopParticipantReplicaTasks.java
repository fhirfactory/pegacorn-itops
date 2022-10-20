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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.tasks.subtasks;

import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixRoom;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomCreation;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomPresetEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomVisibilityEnum;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.WorkshopSummary;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.common.BaseParticipantReplicaServices;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class WorkshopParticipantReplicaTasks extends BaseParticipantReplicaServices {
    private static final Logger LOG = LoggerFactory.getLogger(WorkshopParticipantReplicaTasks.class);

    //
    // Constructor(s)
    //

    //
    // Getters (and Setters)
    //

    @Override
    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Methods
    //

    public MatrixRoom createSubSpaceIfNotThere(String processingPlantSpaceId, MatrixRoom existingKnownSpace, WorkshopSummary workshopSummary) {
        getLogger().debug(".createSubSpaceIfNotThere(): Entry, processingPlantSpaceId->{}, roomList,  workshop->{}", processingPlantSpaceId,  workshopSummary.getComponentID().getName());
        try {
            String participantName = workshopSummary.getParticipantId().getName();
            String participantDisplayName = workshopSummary.getParticipantId().getDisplayName();
            String participantPseudoAlias = getRoomIdentityFactory().buildWorkshopSpacePseudoAlias(participantName);
            MatrixRoom newSpace = null;
            if (existingKnownSpace != null) {
                newSpace = existingKnownSpace;
            }
            if(newSpace == null){
                newSpace = getRoomCache().getRoomFromPseudoAlias(participantPseudoAlias);
            }
            if(newSpace == null){
                List<SynapseRoom> rooms = getSynapseRoomAPI().getRooms(participantPseudoAlias);
                if (!rooms.isEmpty()) {
                    newSpace = new MatrixRoom(rooms.get(0));
                    getRoomCache().addRoom(newSpace);
                    getLogger().debug(".createEndpointSpaceIfRequired(): Adding Room/Space as Child: Parent.RoomId->{}, Child.ParticipantName->{}, Child.RoomId->{}",  processingPlantSpaceId, participantName, newSpace.getRoomID());
                    getMatrixSpaceAPI().addChildToSpace(processingPlantSpaceId, newSpace.getRoomID(), getMatrixAccessToken().getHomeServer());
                }
            }
            if(newSpace == null){
                getLogger().trace(".createSubSpaceIfNotThere(): Creating Space for ->{}", participantPseudoAlias);
                String workshopTopic = "Workshop, " + participantName;
                MRoomCreation mRoomCreation = getMatrixBridgeFactories().newSpaceInSpaceCreationRequest(participantDisplayName, participantPseudoAlias, workshopTopic, processingPlantSpaceId, MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);
                MatrixRoom createdRoom = getMatrixSpaceAPI().createSpace(getMatrixAccessToken().getUserId(), mRoomCreation);
                if (createdRoom != null) {
                    newSpace = createdRoom;
                    getRoomCache().addRoom(createdRoom);
                }
                if (newSpace != null) {
                    getLogger().trace(".createSubSpaceIfNotThere(): Space ->{}", newSpace);
                    getLogger().debug(".createEndpointSpaceIfRequired(): Adding Room/Space as Child: Parent.RoomId->{}, Child.ParticipantName->{}, Child.RoomId->{}",  processingPlantSpaceId, participantName, newSpace.getRoomID());
                    getMatrixSpaceAPI().addChildToSpace(processingPlantSpaceId, newSpace.getRoomID(), getMatrixAccessToken().getHomeServer());
                }
            }

            getLogger().debug(".createSubSpaceIfNotThere(): Exit, workshopId->{}", newSpace.getRoomID());
            return (newSpace);
        } catch (Exception ex){
            getLogger().error(".createSubSpaceIfNotThere(): Error Creating WUP Space/Room, message->{}, stacktrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return(null);
        }
    }

}
