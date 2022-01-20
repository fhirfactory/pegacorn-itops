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
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.EndpointSummary;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.WorkUnitProcessorSummary;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.matrixbridge.common.ParticipantRoomIdentityFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.matrixbridge.topology.common.BaseParticipantReplicaServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class EndpointParticipantReplicaServices extends BaseParticipantReplicaServices {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointParticipantReplicaServices.class);

    @Inject
    private ParticipantRoomIdentityFactory identityFactory;

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

    public String createEndpointSpaceIfRequired(String parentParticipantName, String parentSpaceId, List<SynapseRoom> roomList, EndpointSummary endpointSummary) {
        getLogger().info(".createEndpointSpaceIfRequired(): Entry, parentSpaceId->{}, endpoint->{}", parentSpaceId, endpointSummary);
        String endpointParticipantDisplayName = endpointSummary.getParticipantDisplayName();
        String endpointParticipantName = endpointSummary.getParticipantName();
        String endpointParticipantAlias = identityFactory.buildEndpointRoomAlias(endpointParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT);
        String endpointSpaceId = null;
        SynapseRoom foundRoom = getMatrixBridgeCache().scanForExistingRoomWithAlias(roomList, endpointParticipantAlias);
        if (foundRoom != null) {
            getLogger().info(".createEndpointSpaceIfRequired(): Room Exists, no action required");
            endpointSpaceId = foundRoom.getRoomID();
        } else {
            getLogger().info(".createEndpointSpaceIfRequired(): Creating Space for WUP ->{}", endpointParticipantAlias);
            String endpointTopic = "Endpoint, " + endpointSummary.getComponentID().getId() + ", " + parentParticipantName;
            MRoomCreation mRoomCreation = getMatrixBridgeFactories().newSpaceInSpaceCreationRequest(endpointParticipantName, endpointParticipantAlias, endpointTopic, parentSpaceId, MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);
            SynapseRoom createdRoom = getMatrixSpaceAPI().createSpace(getSynapseAccessToken().getUserName(), mRoomCreation);
            endpointSpaceId = createdRoom.getRoomID();
            getLogger().info(".createEndpointSpaceIfRequired(): Created Space ->{}", createdRoom);
            getMatrixBridgeCache().addRoomFromMatrix(createdRoom);
            roomList.add(createdRoom);
            waitALittleBit();
        }
        //
        // TODO, Should check if it is already a child
        getMatrixSpaceAPI().addChildToSpace(parentSpaceId, endpointSpaceId);
        waitALittleBit();
        getLogger().info(".createEndpointSpaceIfRequired(): [Add Rooms If Required] Start...");
        installAnOAMRoom(endpointParticipantName, endpointParticipantDisplayName, endpointSpaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_METRICS,roomList);
        installAnOAMRoom(endpointParticipantName, endpointParticipantDisplayName, endpointSpaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_EVENTS, roomList);
        getLogger().info(".createProcessingPlantSpace(): [Add Rooms If Required] Finish...");

        getLogger().info(".createEndpointSpaceIfRequired(): Exit, endpointSpaceId->{}", endpointSpaceId);
        return(endpointSpaceId);
    }


}
