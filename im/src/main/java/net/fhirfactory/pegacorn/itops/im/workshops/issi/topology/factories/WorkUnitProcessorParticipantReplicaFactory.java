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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories;

import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixRoom;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomCreation;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomPresetEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomVisibilityEnum;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.WorkUnitProcessorSummary;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.common.BaseParticipantReplicaServices;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Locale;

@ApplicationScoped
public class WorkUnitProcessorParticipantReplicaFactory extends BaseParticipantReplicaServices {
    private static final Logger LOG = LoggerFactory.getLogger(WorkUnitProcessorParticipantReplicaFactory.class);

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

    public String createWorkUnitProcessorSpaceIfNotThere(String workshopId, MatrixRoom wupMatrixRoom, WorkUnitProcessorSummary wupSummary) {
        getLogger().debug(".createWorkUnitProcessorSpace(): Entry, workshopId->{}, wupMatrixRoom->{}, wup->{}", workshopId, wupMatrixRoom, wupSummary);

        String wupParticipantName = wupSummary.getParticipantName();
        String wupParticipantDisplayName = wupSummary.getParticipantDisplayName();
        String wupAlias = OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP.getAliasPrefix() + wupParticipantName.toLowerCase(Locale.ROOT).replace(".", "-");
        String wupRoomId = null;

        boolean foundSubsystemEventsRoom = false;
        boolean foundSubsystemMetricsRoom = false;
        boolean foundSubsystemSubscriptionsRoom = false;
        boolean foundSubsystemTasksRoom = false;

        if (wupMatrixRoom != null) {
            getLogger().trace(".createWorkUnitProcessorSpace(): Room Found, no action required");
            getLogger().trace(".createWorkUnitProcessorSpace(): Checking to see if all the OAM and Sub-Component Rooms exist: Start");
            wupRoomId = wupMatrixRoom.getRoomID();
            if(!wupMatrixRoom.getContainedRooms().isEmpty()){
                for(MatrixRoom currentRoom: wupMatrixRoom.getContainedRooms()){
                    if(StringUtils.isNotEmpty(currentRoom.getCanonicalAlias())) {
                        if (currentRoom.getCanonicalAlias().startsWith("#"+getRoomIdentityFactory().buildOAMRoomAlias(wupParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_CONSOLE))) {
                            foundSubsystemEventsRoom = true;
                        }
                        if (currentRoom.getCanonicalAlias().startsWith("#"+getRoomIdentityFactory().buildOAMRoomAlias(wupParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_METRICS))) {
                            foundSubsystemMetricsRoom = true;
                        }
                        if (currentRoom.getCanonicalAlias().startsWith("#"+getRoomIdentityFactory().buildOAMRoomAlias(wupParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_SUBSCRIPTIONS))) {
                            foundSubsystemSubscriptionsRoom = true;
                        }
                        if (currentRoom.getCanonicalAlias().startsWith("#"+getRoomIdentityFactory().buildOAMRoomAlias(wupParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_TASKS))) {
                            foundSubsystemTasksRoom = true;
                        }
                    }
                }
            }
            getLogger().trace(".createWorkUnitProcessorSpace(): Checking to see if all the OAM and Sub-Component Rooms exist: Finish");
        } else {
            getLogger().debug(".createWorkUnitProcessorSpace(): [Add Space(s) For WUP As Required] Creating Space for WUP ->{}", wupAlias);
            String wupTopic = "WorkUnitProcessor, " + wupSummary.getComponentID().getId();
            MRoomCreation mRoomCreation = getMatrixBridgeFactories().newSpaceInSpaceCreationRequest(wupParticipantDisplayName, wupAlias, wupTopic, workshopId, MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);

            SynapseRoom createdRoom = null;
            createdRoom = getMatrixSpaceAPI().createSpace(getMatrixAccessToken().getUserId(), mRoomCreation);
            if(createdRoom == null) {
                createdRoom = getExistingRoom(wupAlias);
            }

            if(createdRoom != null) {
                wupRoomId = createdRoom.getRoomID();
                getLogger().debug(".createWorkUnitProcessorSpace(): [Add Space(s) For WUP As Required] Created Space ->{}", createdRoom);
                MatrixRoom matrixRoom = new MatrixRoom(createdRoom);
                getRoomCache().addRoom(matrixRoom);
                getMatrixSpaceAPI().addChildToSpace(workshopId, wupRoomId);
            }
        }

        if(StringUtils.isNotEmpty(wupRoomId)) {
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

        getLogger().debug(".createWorkUnitProcessorSpace(): Exit, wupRoomId->{}", wupRoomId);
        return(wupRoomId);
    }


}
