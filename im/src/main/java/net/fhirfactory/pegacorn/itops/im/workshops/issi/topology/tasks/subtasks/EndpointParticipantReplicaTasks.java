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
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.EndpointSummary;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.ParticipantRoomIdentityFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.common.BaseParticipantReplicaServices;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class EndpointParticipantReplicaTasks extends BaseParticipantReplicaServices {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointParticipantReplicaTasks.class);

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

    public MatrixRoom createEndpointSpaceIfRequired(String parentParticipantName, String parentSpaceId, MatrixRoom endpointSpace, EndpointSummary endpointSummary) {
        getLogger().debug(".createEndpointSpaceIfRequired(): Entry, parentSpaceId->{}, endpoint->{}", parentSpaceId, endpointSummary);
        try {
            String endpointParticipantDisplayName = endpointSummary.getParticipantDisplayName();
            String endpointParticipantName = endpointSummary.getParticipantName();
            String endpointParticipantAlias = identityFactory.buildEndpointSpacePseudoAlias(endpointParticipantName);
            String endpointSpaceId = null;
            MatrixRoom endpointRoom = null;

            boolean foundSubsystemEventsRoom = false;
            boolean foundSubsystemMetricsRoom = false;
            boolean foundSubsystemTasksRoom = false;

            if (endpointSpace != null) {
                getLogger().trace(".createEndpointSpaceIfRequired(): Room Exists, no action required");
                getLogger().trace(".createEndpointSpaceIfRequired(): Checking to see if all the OAM and Sub-Component Rooms exist: Start");
                if(!endpointSpace.getContainedRooms().isEmpty()){
                    for(MatrixRoom currentRoom: endpointSpace.getContainedRooms()){
                        if(StringUtils.isNotEmpty(currentRoom.getCanonicalAlias())) {
                            if (currentRoom.getCanonicalAlias().startsWith("#"+getRoomIdentityFactory().buildEndpointRoomPseudoAlias(endpointParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_CONSOLE))) {
                                getLogger().trace(".createEndpointSpaceIfRequired(): {} Events Room Exists", endpointParticipantName);
                                foundSubsystemEventsRoom = true;
                            }
                            if (currentRoom.getCanonicalAlias().startsWith("#"+getRoomIdentityFactory().buildEndpointRoomPseudoAlias(endpointParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_METRICS))) {
                                getLogger().trace(".createEndpointSpaceIfRequired(): {} Metrics Room Exists", endpointParticipantName);
                                foundSubsystemMetricsRoom = true;
                            }
                            if (currentRoom.getCanonicalAlias().startsWith("#"+getRoomIdentityFactory().buildEndpointRoomPseudoAlias(endpointParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_TASKS))) {
                                getLogger().trace(".createEndpointSpaceIfRequired(): {} Task/Activity-Reports Room Exists", endpointParticipantName);
                                foundSubsystemTasksRoom = true;
                            }
                        }
                    }
                }
                endpointRoom = endpointSpace;
                endpointSpaceId = endpointSpace.getRoomID();
                getLogger().trace(".createEndpointSpaceIfRequired(): Checking to see if all the OAM and Sub-Component Rooms exist: Finish");
            } else {
                MatrixRoom roomFromPseudoAlias = getRoomCache().getRoomFromPseudoAlias(endpointParticipantAlias);
                if (roomFromPseudoAlias != null) {
                    getLogger().trace(".createEndpointSpaceIfRequired(): Room already exists ->{}", endpointParticipantAlias);
                    endpointRoom = roomFromPseudoAlias;
                }

                if (endpointRoom == null) {
                    List<SynapseRoom> roomList = getSynapseRoomAPI().getRooms(endpointParticipantAlias);
                    if (!roomList.isEmpty()) {
                        getLogger().trace(".createEndpointSpaceIfRequired(): Room already exists in Synapse server->{}", endpointParticipantAlias);
                        endpointRoom = new MatrixRoom(roomList.get(0));
                        getRoomCache().addRoom(endpointRoom);
                    }
                }

                if (endpointRoom == null) {
                    getLogger().trace(".createEndpointSpaceIfRequired(): Creating Space for Endpoint ->{}", endpointParticipantAlias);
                    String endpointTopic = "Endpoint, " + endpointSummary.getComponentID().getId() + ", " + parentParticipantName;
                    MRoomCreation mRoomCreation = getMatrixBridgeFactories().newSpaceInSpaceCreationRequest(endpointParticipantDisplayName, endpointParticipantAlias, endpointTopic, parentSpaceId, MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);
                    endpointRoom = getMatrixSpaceAPI().createSpace(getMatrixAccessToken().getUserId(), mRoomCreation);
                    if (endpointRoom != null) {
                        getLogger().info(".createEndpointSpaceIfRequired(): Created Space ->{}", endpointRoom);
                        getRoomCache().addRoom(endpointRoom);
                    }
                }

                if(endpointRoom != null) {
                    endpointSpaceId = endpointRoom.getRoomID();
                    if(StringUtils.isEmpty(endpointSpaceId)){
                        getLogger().warn(".createEndpointSpaceIfRequired(): Logic Conflict for endpointSpaceId");
                    }
                    if(StringUtils.isEmpty(parentParticipantName)){
                        getLogger().warn(".createEndpointSpaceIfRequired(): Logic Conflict for parentParticipantName");
                    }
                    if(StringUtils.isEmpty(parentSpaceId)){
                        getLogger().warn(".createEndpointSpaceIfRequired(): Logic Conflict for parentSpaceId");
                    }
                    if(StringUtils.isEmpty(endpointRoom.getCanonicalAlias())){
                        getLogger().warn(".createEndpointSpaceIfRequired(): Logic Conflict for endpointRoom.getCanonicalAlias()");
                    }
                    if(StringUtils.isEmpty(endpointRoom.getCanonicalAlias())){
                        getLogger().warn(".createEndpointSpaceIfRequired(): Logic Conflict for endpointParticipantName");
                    }
                    getLogger().info(".createEndpointSpaceIfRequired(): Adding Room/Space as Child: Parent.ParticipantName={}, Parent.RoomId->{}, Child.ParticipantName->{}, Child.RoomAlias->{}, Child.RoomId->{}", parentParticipantName, parentSpaceId, endpointParticipantName, endpointRoom.getCanonicalAlias(), endpointSpaceId);
                    getMatrixSpaceAPI().addChildToSpace(parentSpaceId, endpointSpaceId, getMatrixAccessToken().getHomeServer());
                }
            }

            if(StringUtils.isNotEmpty(endpointSpaceId)) {
                getLogger().trace(".createEndpointSpaceIfRequired(): [Add Rooms If Required] Start...");
                if (!foundSubsystemEventsRoom) {
                    getLogger().trace(".createEndpointSpaceIfRequired(): Creating {} Console/Events Room", endpointParticipantName);
                    installAnOAMRoom(endpointParticipantName, endpointParticipantDisplayName, endpointSpaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_CONSOLE);
                }
                if (!foundSubsystemMetricsRoom) {
                    getLogger().trace(".createEndpointSpaceIfRequired(): Creating {} Metrics Room", endpointParticipantName);
                    installAnOAMRoom(endpointParticipantName, endpointParticipantDisplayName, endpointSpaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_METRICS);
                }
                if (!foundSubsystemTasksRoom) {
                    getLogger().trace(".createEndpointSpaceIfRequired(): Creating {} Task/Activity-Reports Room", endpointParticipantName);
                    installAnOAMRoom(endpointParticipantName, endpointParticipantDisplayName, endpointSpaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_TASKS);
                }
                getLogger().trace(".createEndpointSpaceIfRequired(): [Add Rooms If Required] Finish...");
            }
            getLogger().debug(".createEndpointSpaceIfRequired(): Exit, endpointSpaceId->{}", endpointSpaceId);
            return (endpointRoom);
        } catch (Exception ex){
            getLogger().error(".createEndpointSpaceIfRequired(): Error Creating Endpoint Space/Room, message->{}, stacktrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return(null);
        }
    }


}
