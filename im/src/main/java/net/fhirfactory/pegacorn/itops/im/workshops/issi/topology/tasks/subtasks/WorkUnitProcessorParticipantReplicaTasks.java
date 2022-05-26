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
import net.fhirfactory.pegacorn.core.model.internal.resources.summaries.WorkUnitProcessorSummary;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.common.BaseParticipantReplicaServices;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class WorkUnitProcessorParticipantReplicaTasks extends BaseParticipantReplicaServices {
    private static final Logger LOG = LoggerFactory.getLogger(WorkUnitProcessorParticipantReplicaTasks.class);

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

    public MatrixRoom createWorkUnitProcessorSpaceIfNotThere(String workshopId, MatrixRoom wupMatrixRoom, WorkUnitProcessorSummary wupSummary) {
        getLogger().debug(".createWorkUnitProcessorSpace(): Entry, workshopId->{}, wupMatrixRoom->{}, wup->{}", workshopId, wupMatrixRoom, wupSummary);

        try {
            String wupParticipantName = wupSummary.getParticipantName();
            String wupParticipantDisplayName = wupSummary.getParticipantDisplayName();
            String wupAlias = getRoomIdentityFactory().buildWorkUnitProcessorSpacePseudoAlias(wupParticipantName);
            MatrixRoom wupRoom = null;
            String wupRoomId= null;

            boolean foundSubsystemEventsRoom = false;
            boolean foundSubsystemMetricsRoom = false;
            boolean foundSubsystemSubscriptionsRoom = false;
            boolean foundSubsystemTasksRoom = false;

            if (wupMatrixRoom != null) {
                getLogger().trace(".createWorkUnitProcessorSpace(): Room Found, no action required");
                getLogger().trace(".createWorkUnitProcessorSpace(): Checking to see if all the OAM and Sub-Component Rooms exist: Start");
                wupRoom = wupMatrixRoom;
                if (!wupMatrixRoom.getContainedRooms().isEmpty()) {
                    for (MatrixRoom currentRoom : wupMatrixRoom.getContainedRooms()) {
                        if (StringUtils.isNotEmpty(currentRoom.getCanonicalAlias())) {
                            if (currentRoom.getCanonicalAlias().startsWith("#" + getRoomIdentityFactory().buildOAMRoomPseudoAlias(wupParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_CONSOLE))) {
                                foundSubsystemEventsRoom = true;
                            }
                            if (currentRoom.getCanonicalAlias().startsWith("#" + getRoomIdentityFactory().buildOAMRoomPseudoAlias(wupParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_METRICS))) {
                                foundSubsystemMetricsRoom = true;
                            }
                            if (currentRoom.getCanonicalAlias().startsWith("#" + getRoomIdentityFactory().buildOAMRoomPseudoAlias(wupParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_SUBSCRIPTIONS))) {
                                foundSubsystemSubscriptionsRoom = true;
                            }
                            if (currentRoom.getCanonicalAlias().startsWith("#" + getRoomIdentityFactory().buildOAMRoomPseudoAlias(wupParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_TASKS))) {
                                foundSubsystemTasksRoom = true;
                            }
                        }
                    }
                }
                wupRoomId = wupRoom.getRoomID();
                getLogger().trace(".createWorkUnitProcessorSpace(): Checking to see if all the OAM and Sub-Component Rooms exist: Finish");
            }
            //
            // If wupRoom is not provided, see if it is in the cache
            if(wupRoom == null) {
                MatrixRoom existingRoom = getRoomCache().getRoomFromPseudoAlias(wupAlias);
                if (existingRoom != null) {
                    getLogger().trace(".createSubSpaceIfNotThere(): Room already exists ->{}", wupAlias);
                    wupRoom = existingRoom;
                    wupRoomId = wupRoom.getRoomID();
                    getLogger().debug(".createEndpointSpaceIfRequired(): Adding Room/Space as Child: Parent.RoomId->{}, Child.ParticipantName->{}, Child.RoomAlias->{}, Child.RoomId->{}",  workshopId, wupParticipantName, wupRoom.getCanonicalAlias(), wupRoom.getRoomID());
                    getMatrixSpaceAPI().addChildToSpace(workshopId, wupRoom.getRoomID(), getMatrixAccessToken().getHomeServer());
                    getLogger().debug(".createWorkUnitProcessorSpace(): [Add Space(s) For WUP As Required] Creating Space for WUP ->{}", wupAlias);
                }
            }
            //
            // If wupRoom is not provided and is not in the cache, see if it is in the Synapse Server already
            if(wupRoom == null) {
                List<SynapseRoom> rooms = getSynapseRoomAPI().getRooms(wupAlias);
                if (!rooms.isEmpty()) {
                    wupRoom = new MatrixRoom(rooms.get(0));
                    wupRoomId = wupRoom.getRoomID();
                    getRoomCache().addRoom(wupRoom);
                    getLogger().debug(".createEndpointSpaceIfRequired(): Adding Room/Space as Child: Parent.RoomId->{}, Child.ParticipantName->{}, Child.RoomAlias->{}, Child.RoomId->{}",  workshopId, wupParticipantName, wupRoom.getCanonicalAlias(), wupRoom.getRoomID());
                    getMatrixSpaceAPI().addChildToSpace(workshopId, wupRoomId, getMatrixAccessToken().getHomeServer());
                }
            }
            //
            // If wupRoom is not provided, is not in the cache and is not in the Synapse Server already, create it!
            if(wupRoom == null){
                getLogger().trace(".createSubSpaceIfNotThere(): First double-checking the room isn't already create ->{}", wupAlias);
                 String wupTopic = "WorkUnitProcessor, " + wupSummary.getComponentID().getId();
                MRoomCreation mRoomCreation = getMatrixBridgeFactories().newSpaceInSpaceCreationRequest(wupParticipantDisplayName, wupAlias, wupTopic, workshopId, MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);
                MatrixRoom createdRoom = getMatrixSpaceAPI().createSpace(getMatrixAccessToken().getUserId(), mRoomCreation);
                if (createdRoom != null) {
                    getLogger().debug(".createWorkUnitProcessorSpace(): [Add Space(s) For WUP As Required] Created Space ->{}", createdRoom);
                    wupRoom = createdRoom;
                    getRoomCache().addRoom(createdRoom);
                }
                if (wupRoom != null) {
                    wupRoomId = wupRoom.getRoomID();
                    getLogger().debug(".createEndpointSpaceIfRequired(): Adding Room/Space as Child: Parent.RoomId->{}, Child.ParticipantName->{}, Child.RoomAlias->{}, Child.RoomId->{}",  workshopId, wupParticipantName, wupRoom.getCanonicalAlias(), wupRoom.getRoomID());
                    getMatrixSpaceAPI().addChildToSpace(workshopId, wupRoomId, getMatrixAccessToken().getHomeServer());
                }
            }
            //
            // If wupRoom is not null, check to see if its associated OAM Rooms exist and, if not, create them.
            if (wupRoom != null) {
                getLogger().debug(".createWorkUnitProcessorSpace(): [Add Rooms If Required] Start...");
                if (!foundSubsystemEventsRoom) {
                    installAnOAMRoom(wupParticipantName, wupParticipantDisplayName, wupRoomId, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_CONSOLE);
                }
                if (!foundSubsystemMetricsRoom) {
                    installAnOAMRoom(wupParticipantName, wupParticipantDisplayName, wupRoomId, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_METRICS);
                }
                if (!foundSubsystemSubscriptionsRoom) {
                    installAnOAMRoom(wupParticipantName, wupParticipantDisplayName, wupRoomId, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_SUBSCRIPTIONS);
                }
                if (!foundSubsystemTasksRoom) {
                    installAnOAMRoom(wupParticipantName, wupParticipantDisplayName, wupRoomId, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_TASKS);
                }
                getLogger().debug(".createWorkUnitProcessorSpace(): [Add Rooms If Required] Finish...");
            }
            //
            // If the WUP Room is still null, something is wrong!
            if(wupRoom == null){
                getLogger().warn(".createWorkUnitProcessorSpace(): Could not find or create WUP Space/Room-Set!!!!!");
            }
            getLogger().debug(".createWorkUnitProcessorSpace(): Exit, wupRoomId->{}", wupRoomId);
            return (wupRoom);
        } catch(Exception ex){
            getLogger().error(".createWorkUnitProcessorSpace(): Error Creating WUP Space/Room, message->{}, stacktrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return(null);
        }
    }


}
