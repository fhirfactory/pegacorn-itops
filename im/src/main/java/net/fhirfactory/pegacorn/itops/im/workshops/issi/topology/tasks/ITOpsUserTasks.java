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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.tasks;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixRoomMethods;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixSpaceMethods;
import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixRoom;
import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixUser;
import net.fhirfactory.pegacorn.communicate.synapse.credentials.SynapseAdminAccessToken;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseRoomMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseUser;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownRoomAndSpaceMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownUserMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.common.ITOpsRoomHelpers;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class ITOpsUserTasks {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsUserTasks.class);

    @Inject
    private ITOpsKnownUserMapDM userCache;

    @Inject
    private ITOpsKnownRoomAndSpaceMapDM roomCache;

    @Inject
    private ITOpsRoomHelpers itopsRoomHelpers;

    @Inject
    private MatrixAccessToken matrixAccessToken;

    @Inject
    private SynapseAdminAccessToken synapseAccessToken;

    @Inject
    private MatrixRoomMethods matrixRoomAPI;

    @Inject
    private MatrixSpaceMethods matrixSpaceAPI;

    @Inject
    private SynapseRoomMethods synapseRoomAPI;

    //
    // Constructor(s)
    //

    //
    // Post Constructor
    //

    //
    // Business Methods
    //

    //
    // See if there are new Users

    public void addUsersToAllRooms(Set<MatrixUser> addedUserSet) {
        getLogger().debug(".addNewUsersToAllRooms(): Entry");

        getLogger().info(".addNewUsersToAllRooms(): [Auto Join New Users to the Older Rooms] Start...");
        try {
            for (MatrixRoom currentRoom : roomCache.getFullRoomSet()) {
                String currentRoomAlias = currentRoom.getCanonicalAlias();
                if (StringUtils.isNotEmpty(currentRoomAlias)) {
                    if (itopsRoomHelpers.isAnITOpsRoom(currentRoomAlias)) {
                        if (allShouldJoin(currentRoomAlias)) {
                            getLogger().info(".addNewUsersToAllRooms(): [Auto Join New Users to the Older Rooms] Processing Room/Space->{}", currentRoomAlias);
                            String roomId = currentRoom.getRoomID();
                            List<String> roomMembers = synapseRoomAPI.getRoomMembers(roomId);
                            for (SynapseUser currentUser : addedUserSet) {
                                getLogger().info(".addNewUsersToAllRooms(): RoomId->{}, roomMembers->{}", roomId, roomMembers);
                                if(roomMembers.contains(currentUser.getName())) {
                                    getLogger().trace(".addNewUsersToAllRooms(): [Auto Join Users to Added Rooms] User Already Member, not Adding->{}", currentUser.getName());
                                } else {
                                    if (currentUser.getName().contentEquals(matrixAccessToken.getUserId()) || currentUser.getName().contentEquals(synapseAccessToken.getUserId())) {
                                        getLogger().info(".addNewUsersToAllRooms(): [Auto Join New Users to the Older Rooms] Not Adding User->{}", currentUser.getName());
                                    } else {
                                        getLogger().info(".addNewUsersToAllRooms(): [Auto Join New Users to the Older Rooms] Processing User->{}", currentUser.getName());
                                        synapseRoomAPI.addRoomMember(roomId, currentUser.getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            getLogger().warn(".addNewUsersToAllRooms(): Failure to add New Users to Spaces/Rooms, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
        getLogger().info(".addNewUsersToAllRooms(): [Auto New Users to the Older Rooms] Finish...");

        getLogger().debug(".addNewUsersToAllRooms(): Exit");
    }

    public void addAllUsersToRoomSet(Set<MatrixRoom> addedRoomSet){
        getLogger().debug(".addAllUsersToRoomSet(): Entry");
        try {
            Set<MatrixUser> knownUsers = userCache.getKnownUsers();
            for (SynapseRoom currentRoom : addedRoomSet) {
                String currentRoomAlias = currentRoom.getCanonicalAlias();
                if (StringUtils.isNotEmpty(currentRoomAlias)) {
                    if (itopsRoomHelpers.isAnITOpsRoom(currentRoomAlias)) {
                        if (allShouldJoin(currentRoomAlias)) {
                            getLogger().debug(".addAllUsersToRoomSet(): [Auto Join Users to Added Rooms] Processing Space->{}", currentRoomAlias);
                            String roomId = currentRoom.getRoomID();
                            List<String> roomMembers = synapseRoomAPI.getRoomMembers(roomId);
                            for (MatrixUser currentUser : knownUsers) {
                                getLogger().info(".addAllUsersToRoomSet(): RoomId->{}, roomMembers->{}", roomId, roomMembers);
                                if(roomMembers.contains(currentUser.getName())) {
                                    getLogger().trace(".addAllUsersToRoomSet(): [Auto Join Users to Added Rooms] User Already Member, not Adding->{}", currentUser.getName());
                                } else {
                                    if (currentUser.getName().contentEquals(matrixAccessToken.getUserId()) || currentUser.getName().contentEquals(synapseAccessToken.getUserId())) {
                                        getLogger().trace(".addAllUsersToRoomSet(): [Auto Join Users to Added Rooms] Not Adding User->{}", currentUser.getName());
                                    } else {
                                        getLogger().debug(".addAllUsersToRoomSet(): [Auto Join Users to Added Rooms] Processing User->{}", currentUser.getName());
                                        synapseRoomAPI.addRoomMember(roomId, currentUser.getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            getLogger().warn(".addAllUsersToRoomSet(): Failure to Add Users to New Spaces/Rooms, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
    }

    public void joinAllUsersToAllRooms(){
        getLogger().debug(".joinAllUsersToAllRooms(): [Auto Join All Users to All Rooms] Start...");

        getLogger().debug(".joinAllUsersToAllRooms(): [Auto Join All Users to All Rooms] Starting... more than USER_SYNCHRONISATION_OVERRIDE_PERIOD since last full update");
        try {
            Set<MatrixRoom> fullRoomSet = roomCache.getFullRoomSet();
            Set<MatrixUser> fullUserSet = userCache.getKnownUsers();
            for (MatrixRoom currentRoom : fullRoomSet) {
                String currentRoomAlias = currentRoom.getCanonicalAlias();
                if (StringUtils.isNotEmpty(currentRoomAlias)) {
                    if (itopsRoomHelpers.isAnITOpsRoom(currentRoomAlias)) {
                        if (allShouldJoin(currentRoomAlias)) {
                            getLogger().info(".joinAllUsersToAllRooms(): [Auto Join All Users to All Rooms] Processing Room/Space->{}", currentRoomAlias);
                            String roomId = currentRoom.getRoomID();
                            List<String> roomMembers = synapseRoomAPI.getRoomMembers(roomId);
                            for (MatrixUser currentUser : fullUserSet) {
                                getLogger().info(".joinAllUsersToAllRooms(): RoomId->{}, roomMembers->{}", roomId, roomMembers);
                                if(roomMembers.contains(currentUser.getName())) {
                                    getLogger().trace(".joinAllUsersToAllRooms(): [Auto Join Users to Added Rooms] User Already Member, not Adding->{}", currentUser.getName());
                                } else {
                                    if (currentUser.getName().contentEquals(matrixAccessToken.getUserId()) || currentUser.getName().contentEquals(synapseAccessToken.getUserId())) {
                                        getLogger().info(".joinAllUsersToAllRooms(): [Auto Join All Users to All Rooms] Not Adding User->{}", currentUser.getName());
                                    } else {
                                        getLogger().info(".joinAllUsersToAllRooms(): [Auto Join All Users to All Rooms] Processing User->{}", currentUser.getName());
                                        synapseRoomAPI.addRoomMember(roomId, currentUser.getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            getLogger().warn(".joinAllUsersToAllRooms(): Failure to add All Users to Spaces/Rooms, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }

        getLogger().debug(".joinAllUsersToAllRooms(): [Auto Join All Users to All Rooms] Finish...");
    }


    //
    // Room Membership Requirement Check
    //

    public boolean allShouldJoin(String roomAlias){
        //
        // Do this check first
        boolean isSubsystemComponentRom = roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_COMPONENTS.getAliasPrefix());
        if(isSubsystemComponentRom) {
            return (false);
        }
        boolean isSubsystemRoom = roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM.getAliasPrefix());
        boolean isSubsystemSubscriptionRoom = roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_SUBSCRIPTIONS.getAliasPrefix());
        boolean isSubsystemTaskRoom = roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_TASKS.getAliasPrefix());
        boolean isSubsystemConsoleRoom = roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_CONSOLE.getAliasPrefix());
        boolean isSubsystemMetricsRoom = roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_METRICS.getAliasPrefix());
        if( isSubsystemRoom || isSubsystemSubscriptionRoom || isSubsystemTaskRoom || isSubsystemConsoleRoom || isSubsystemMetricsRoom){
            return(true);
        }
        boolean isEndpointRoom = roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT.getAliasPrefix());
        boolean isEndpointTaskRoom = roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_TASKS.getAliasPrefix());
        boolean isEndpointConsoleRoom = roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_CONSOLE.getAliasPrefix());
        boolean isEndpointMetricsRoom = roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_METRICS.getAliasPrefix());
        if(isEndpointRoom || isEndpointTaskRoom || isEndpointConsoleRoom || isEndpointMetricsRoom){
            boolean isMLLP = roomAlias.contains("mllp");
            boolean isHTTP = roomAlias.contains("http");
            boolean isFILE = roomAlias.contains("file");
            if(isMLLP || isHTTP || isFILE){
                return(true);
            }
        }
        return(false);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

}
