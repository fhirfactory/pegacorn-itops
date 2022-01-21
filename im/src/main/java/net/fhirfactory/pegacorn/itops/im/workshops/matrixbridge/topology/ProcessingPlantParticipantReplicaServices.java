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
package net.fhirfactory.pegacorn.itops.im.workshops.matrixbridge.topology;

import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomCreation;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomPresetEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomVisibilityEnum;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.matrixbridge.topology.common.BaseParticipantReplicaServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ProcessingPlantParticipantReplicaServices extends BaseParticipantReplicaServices {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessingPlantParticipantReplicaServices.class);

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

    public String createProcessingPlantSpace(String processingPlantParticipantName, List<SynapseRoom> roomList){
        getLogger().debug(".createProcessingPlantSpace(): Entry, processingPlantParticipantName->{}", processingPlantParticipantName);

        String participantRoomAlias = getRoomIdentityFactory().buildProcessingPlantCanonicalAlias(processingPlantParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM);
        SynapseRoom participantRoom = getMatrixBridgeCache().scanForExistingRoomWithAlias(roomList, participantRoomAlias);
        if(participantRoom != null){
            getLogger().trace(".createProcessingPlantSpace(): Room Exists, nothing to do");
        } else {
            getLogger().trace(".createProcessingPlantSpace(): [Add Space(s) As Required] Creating room for ->{}", processingPlantParticipantName);
            MRoomCreation mRoomCreation = getMatrixBridgeFactories().newSpaceCreationRequest(processingPlantParticipantName, participantRoomAlias, "", MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);
            participantRoom = getMatrixSpaceAPI().createSpace(getSynapseAccessToken().getUserName(), mRoomCreation);
            getLogger().trace(".createProcessingPlantSpace(): [Add Space(s) As Required] Created Room ->{}", participantRoom);
            roomList.add(participantRoom);
            getMatrixBridgeCache().addRoomFromMatrix(participantRoom);
        }
        String spaceId = participantRoom.getRoomID();
        getLogger().trace(".createProcessingPlantSpace(): [Add Rooms If Required] Start...");
        installAnOAMRoom(processingPlantParticipantName, processingPlantParticipantName, spaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_EVENTS,roomList);
        installAnOAMRoom(processingPlantParticipantName, processingPlantParticipantName, spaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_METRICS, roomList);
        installAnOAMRoom(processingPlantParticipantName, processingPlantParticipantName, spaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_SUBSCRIPTIONS, roomList);
        installAnOAMRoom(processingPlantParticipantName, processingPlantParticipantName, spaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_TASKS, roomList);
        getLogger().trace(".createProcessingPlantSpace(): [Add Rooms If Required] Finish...");

        getLogger().debug(".createProcessingPlantSpace(): Exit, spaceId->{}", spaceId);
        return(spaceId);
    }
}
