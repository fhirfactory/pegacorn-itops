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
import net.fhirfactory.pegacorn.itops.im.datatypes.ProcessingPlantSpaceDetail;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.common.BaseParticipantReplicaServices;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessingPlantParticipantReplicaFactory extends BaseParticipantReplicaServices {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessingPlantParticipantReplicaFactory.class);

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

    public ProcessingPlantSpaceDetail createProcessingPlantSpaceIfNotThere(String processingPlantParticipantName, MatrixRoom participantRoom){
        getLogger().debug(".createProcessingPlantSpace(): Entry, processingPlantParticipantName->{}", processingPlantParticipantName);

        try {
            String participantRoomAlias = getRoomIdentityFactory().buildProcessingPlantCanonicalAlias(processingPlantParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM);
            String spaceId = null;
            MatrixRoom participantComponentRoom = null;
            boolean foundSubsystemEventsRoom = false;
            boolean foundSubsystemMetricsRoom = false;
            boolean foundSubsystemSubscriptionsRoom = false;
            boolean foundSubsystemTasksRoom = false;
            boolean foundSubsystemComponentsRoom = false;

            if (participantRoom != null) {
                getLogger().trace(".createProcessingPlantSpace(): Checking to see if all the OAM and Sub-Component Rooms exist: Start");
                spaceId = participantRoom.getRoomID();
                if (participantRoom.getContainedRooms().isEmpty()) {
                    getLogger().trace(".createProcessingPlantSpace(): {} Space has no sub-rooms", processingPlantParticipantName);
                    foundSubsystemEventsRoom = false;
                    foundSubsystemMetricsRoom = false;
                    foundSubsystemSubscriptionsRoom = false;
                    foundSubsystemTasksRoom = false;
                    foundSubsystemComponentsRoom = false;
                } else {
                    for (MatrixRoom currentRoom : participantRoom.getContainedRooms()) {
                        if (StringUtils.isNotEmpty(currentRoom.getCanonicalAlias())) {
                            if (currentRoom.getCanonicalAlias().startsWith("#" + getRoomIdentityFactory().buildOAMRoomAlias(processingPlantParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_CONSOLE))) {
                                foundSubsystemEventsRoom = true;
                                getLogger().trace(".createProcessingPlantSpace(): {} Events Room Exists", processingPlantParticipantName);
                            }
                            if (currentRoom.getCanonicalAlias().startsWith("#" + getRoomIdentityFactory().buildOAMRoomAlias(processingPlantParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_METRICS))) {
                                foundSubsystemMetricsRoom = true;
                                getLogger().trace(".createProcessingPlantSpace(): {} Metrics Room Exists", processingPlantParticipantName);
                            }
                            if (currentRoom.getCanonicalAlias().startsWith("#" + getRoomIdentityFactory().buildOAMRoomAlias(processingPlantParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_SUBSCRIPTIONS))) {
                                foundSubsystemSubscriptionsRoom = true;
                                getLogger().trace(".createProcessingPlantSpace(): {} Subscription Room Exists", processingPlantParticipantName);
                            }
                            if (currentRoom.getCanonicalAlias().startsWith("#" + getRoomIdentityFactory().buildOAMRoomAlias(processingPlantParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_TASKS))) {
                                foundSubsystemTasksRoom = true;
                                getLogger().trace(".createProcessingPlantSpace(): {} Task Reporting Room Exists", processingPlantParticipantName);
                            }
                            if (currentRoom.getCanonicalAlias().startsWith("#" + getRoomIdentityFactory().buildOAMRoomAlias(processingPlantParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_COMPONENTS))) {
                                foundSubsystemComponentsRoom = true;
                                participantComponentRoom = currentRoom;
                                getLogger().trace(".createProcessingPlantSpace(): {} Component Space Exists", processingPlantParticipantName);
                            }
                        }
                    }
                }
                getLogger().trace(".createProcessingPlantSpace(): Checking to see if all the OAM and Sub-Component Rooms exist: Finish");
            } else {
                MatrixRoom existingRoom = getRoomCache().getRoomFromPseudoAlias(participantRoomAlias);
                if (existingRoom != null) {
                    getLogger().trace(".createProcessingPlantSpace(): [Add Space(s) As Required] Room actually exists for ->{}", processingPlantParticipantName);
                    participantRoom = existingRoom;
                } else {
                    getLogger().trace(".createProcessingPlantSpace(): [Add Space(s) As Required] Creating room for ->{}", processingPlantParticipantName);
                    MRoomCreation mRoomCreation = getMatrixBridgeFactories().newSpaceCreationRequest(processingPlantParticipantName, participantRoomAlias, "", MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);
                    participantRoom = getMatrixSpaceAPI().createSpace(getMatrixAccessToken().getUserId(), mRoomCreation);
                }
                getLogger().trace(".createProcessingPlantSpace(): [Add Space(s) As Required] Created Room ->{}", participantRoom);
                if (participantRoom != null) {
                    getRoomCache().addRoom(participantRoom);
                    spaceId = participantRoom.getRoomID();
                }
            }

            getLogger().trace(".createProcessingPlantSpace(): [Add Rooms If Required] Start...");
            if (!foundSubsystemComponentsRoom) {
                getLogger().trace(".createProcessingPlantSpace(): Creating {} Component Space", processingPlantParticipantName);
                participantComponentRoom = addProcessingPlantComponentSpace(processingPlantParticipantName, spaceId);
            }
            if (!foundSubsystemEventsRoom) {
                getLogger().trace(".createProcessingPlantSpace(): Creating {} Events/Console Room", processingPlantParticipantName);
                installAnOAMRoom(processingPlantParticipantName, processingPlantParticipantName, spaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_CONSOLE);
            }
            if (!foundSubsystemMetricsRoom) {
                getLogger().trace(".createProcessingPlantSpace(): Creating {} Metrics Room", processingPlantParticipantName);
                installAnOAMRoom(processingPlantParticipantName, processingPlantParticipantName, spaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_METRICS);
            }
            if (!foundSubsystemSubscriptionsRoom) {
                getLogger().trace(".createProcessingPlantSpace(): Creating {} Subscriptions Room", processingPlantParticipantName);
                installAnOAMRoom(processingPlantParticipantName, processingPlantParticipantName, spaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_SUBSCRIPTIONS);
            }
            if (!foundSubsystemTasksRoom) {
                getLogger().trace(".createProcessingPlantSpace(): Creating {} Task Reporting Room", processingPlantParticipantName);
                installAnOAMRoom(processingPlantParticipantName, processingPlantParticipantName, spaceId, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_TASKS);
            }
            getLogger().trace(".createProcessingPlantSpace(): [Add Rooms If Required] Finish...");

            ProcessingPlantSpaceDetail processingPlantSpace = new ProcessingPlantSpaceDetail();
            processingPlantSpace.setProcessingPlantSpace(participantRoom);
            processingPlantSpace.setProcessingPlantComponentSpace(participantComponentRoom);

            getLogger().debug(".createProcessingPlantSpace(): Exit, processingPlantSpace->{}", processingPlantSpace);
            return (processingPlantSpace);
        } catch (Exception ex){
            getLogger().error(".createWorkUnitProcessorSpace(): Error Creating WUP Space/Room, message->{}, stacktrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return(null);
        }
    }

    protected MatrixRoom addProcessingPlantComponentSpace(String processingPlantParticipantName, String processingPlantSpaceId){
        getLogger().debug(".addProcessingPlantComponentSpace(): Entry, processingPlantParticipantName->{}, processingPlantSpaceId->{}", processingPlantParticipantName, processingPlantSpaceId);
        String participantComponentRoomAlias = getRoomIdentityFactory().buildOAMRoomAlias(processingPlantParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_COMPONENTS);

        getLogger().trace(".addProcessingPlantComponentSpace(): [Add Space(s) As Required] Double Checking if Room is not Already created ->{}", participantComponentRoomAlias);
        MatrixRoom participantComponentRoom = getRoomCache().getRoomFromPseudoAlias(participantComponentRoomAlias);
        if(participantComponentRoom != null) {
            getLogger().trace(".addProcessingPlantComponentSpace(): [Add Space(s) As Required] Room actually exists for ->{}", processingPlantParticipantName);
        } else {
            getLogger().trace(".addProcessingPlantComponentSpace(): [Add Space(s) As Required] Creating Space for ->{}", participantComponentRoomAlias);
            MRoomCreation mComponentRoomCreation = getMatrixBridgeFactories().newSpaceInSpaceCreationRequest(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_COMPONENTS.getDisplayName(), participantComponentRoomAlias, "Processing Plant Components", processingPlantSpaceId, MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);
            participantComponentRoom = getMatrixSpaceAPI().createSpace(getMatrixAccessToken().getUserId(), mComponentRoomCreation);
            getLogger().trace(".addProcessingPlantComponentSpace(): [Add Space(s) As Required] Created Room ->{}", participantComponentRoomAlias);
            getRoomCache().addRoom(participantComponentRoom);
        }
        getMatrixSpaceAPI().addChildToSpace(processingPlantSpaceId, participantComponentRoom.getRoomID());

        getLogger().debug(".addProcessingPlantComponentSpace(): Exit, participantComponentRoom->{}", participantComponentRoom);
        return(participantComponentRoom);
    }
}
