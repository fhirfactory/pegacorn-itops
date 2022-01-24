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
package net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.topology;

import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomCreation;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomPresetEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomVisibilityEnum;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.WorkshopSummary;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.topology.common.BaseParticipantReplicaServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class WorkshopParticipantReplicaServices extends BaseParticipantReplicaServices {
    private static final Logger LOG = LoggerFactory.getLogger(WorkshopParticipantReplicaServices.class);

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

    public String createWorkUnitProcessorSpace(String processingPlantSpaceId, List<SynapseRoom> roomList, WorkshopSummary workshopSummary) {
        getLogger().debug(".createWorkUnitProcessorSpace(): Entry, processingPlantSpaceId->{}, roomList,  workshop->{}", processingPlantSpaceId,  workshopSummary.getTopologyNodeFDN());

        String workshopName = workshopSummary.getParticipantName();
        String workshopId = null;
        String workshopAlias = OAMRoomTypeEnum.OAM_ROOM_TYPE_WORKSHOP.getAliasPrefix() + workshopName.toLowerCase(Locale.ROOT).replace(".", "-");
        SynapseRoom workshopRoom = getMatrixBridgeCache().scanForExistingRoomWithAlias(roomList, workshopAlias);
        if(workshopRoom != null){
            workshopId = workshopRoom.getRoomID();
        } else {
            getLogger().trace(".createWorkUnitProcessorSpace(): Creating Space for ->{}", workshopAlias);
            String workshopTopic = "Workshop";
            MRoomCreation mRoomCreation = getMatrixBridgeFactories().newSpaceInSpaceCreationRequest(workshopName, workshopAlias, workshopTopic, processingPlantSpaceId, MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);
            SynapseRoom createdRoom = getMatrixSpaceAPI().createSpace(getSynapseAccessToken().getUserName(), mRoomCreation);
            workshopId = createdRoom.getRoomID();
            getLogger().trace(".createWorkUnitProcessorSpace(): Created Space ->{}", createdRoom);
            getMatrixBridgeCache().addRoomFromMatrix(createdRoom);
            roomList.add(createdRoom);
            waitALittleBit();
        }
        //
        // Should Check to See if it already a child
        getMatrixSpaceAPI().addChildToSpace(processingPlantSpaceId, workshopId);
        getLogger().debug(".createWorkUnitProcessorSpace(): Exit, workshopId->{}", workshopId);
        return(workshopId);
    }

}
