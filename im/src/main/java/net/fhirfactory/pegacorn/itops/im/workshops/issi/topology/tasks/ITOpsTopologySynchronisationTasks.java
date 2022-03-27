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
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseRoomMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.core.model.petasos.endpoint.valuesets.PetasosEndpointTopologyTypeEnum;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.EndpointSummary;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.ProcessingPlantSummary;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.WorkUnitProcessorSummary;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.WorkshopSummary;
import net.fhirfactory.pegacorn.itops.im.datatypes.ProcessingPlantSpaceDetail;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownRoomAndSpaceMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.tasks.subtasks.EndpointParticipantReplicaTasks;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.tasks.subtasks.ProcessingPlantParticipantReplicaTasks;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.tasks.subtasks.WorkUnitProcessorParticipantReplicaTasks;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.tasks.subtasks.WorkshopParticipantReplicaTasks;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.ParticipantRoomIdentityFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class ITOpsTopologySynchronisationTasks {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsTopologySynchronisationTasks.class);

    @Inject
    private SynapseRoomMethods synapseRoomAPI;

    @Inject
    private ITOpsKnownRoomAndSpaceMapDM roomCache;

    @Inject
    private MatrixRoomMethods matrixRoomAPI;

    @Inject
    private MatrixSpaceMethods matrixSpaceAPI;

    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    @Inject
    private ProcessingPlantParticipantReplicaTasks processingPlantReplicaServices;

    @Inject
    private WorkshopParticipantReplicaTasks workshopReplicaServices;

    @Inject
    private WorkUnitProcessorParticipantReplicaTasks wupReplicaServices;

    @Inject
    private EndpointParticipantReplicaTasks endpointReplicaServices;

    @Inject
    private MatrixAccessToken matrixAccessToken;

    //
    // Constructor(s)
    //

    //
    // Post Constructor
    //

    //
    // Business Methods
    //

    public void synchroniseMatrixIntoLocalCache(){
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] Start...");
        try {
            List<SynapseRoom> roomList = synapseRoomAPI.getRooms("*");
            getLogger().trace(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] RoomList->{}", roomList);
            // remove if not available
            Set<MatrixRoom> knownRooms = roomCache.getFullRoomSet();
            for (MatrixRoom currentKnownRoom : knownRooms) {
                boolean found = false;
                for (SynapseRoom currentRoom : roomList) {
                    if (currentRoom.getRoomID().contentEquals(currentKnownRoom.getRoomID())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    roomCache.deleteRoom(currentKnownRoom.getRoomID());
                }
            }
            // add if absent
            for (SynapseRoom currentRoom : roomList) {
                getLogger().trace(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] Processing Room ->{}", currentRoom);
                MatrixRoom matrixRoom = new MatrixRoom(currentRoom);
                roomCache.addRoom(matrixRoom);
            }
        } catch (Exception ex) {
            getLogger().warn(".topologyReplicationSynchronisationDaemon(): Failure to synchronise room list, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] Finish...");
    }

    /**
     *
     * @param processingPlant
     * @param processingPlantMatrixRoom
     */
    public void createParticipantSpacesAndRoomsIfNotThere(ProcessingPlantSummary processingPlant, MatrixRoom processingPlantMatrixRoom){
        getLogger().debug(".createParticipantSpacesAndRoomsIfNotThere(): Entry, processingPlant->{}, processingPlantMatrixRoom->{}", processingPlant, processingPlantMatrixRoom);

        ProcessingPlantSpaceDetail processingPlantSpace = processingPlantReplicaServices.createProcessingPlantSpaceIfNotThere(processingPlant.getParticipantName(), processingPlantMatrixRoom);
        if(processingPlantSpace != null) {
            getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processingPlantSpace->{}", processingPlantSpace);
            for (WorkshopSummary currentWorkshop : processingPlant.getWorkshops().values()) {
                getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing workshop: currentWorkshop->{}", currentWorkshop);
                String workshopPseudoAlias = roomIdentityFactory.buildWorkshopSpacePseudoAlias(currentWorkshop.getParticipantName());
                MatrixRoom currentWorkshopSpace = resolveMatrixRoomFromParticipantName(processingPlantSpace.getProcessingPlantComponentSpace().getContainedRooms(), workshopPseudoAlias);
                String workshopId = null;
                if(currentWorkshopSpace == null) {
                    getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing workshop: creating currentWorkshopSpace->{}", currentWorkshopSpace);
                    currentWorkshopSpace = workshopReplicaServices.createSubSpaceIfNotThere(processingPlantSpace.getProcessingPlantComponentSpace().getRoomID(), currentWorkshopSpace, currentWorkshop);
                    if(currentWorkshopSpace != null){
                        workshopId = currentWorkshopSpace.getRoomID();
                    }
                } else {
                    getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing workshop: matrix space already exists for currentWorkshopSpace->{}", currentWorkshopSpace);
                    workshopId = currentWorkshopSpace.getRoomID();
                }
                if(StringUtils.isNotEmpty(workshopId)) {
                    getLogger().debug(".createParticipantSpacesAndRoomsIfNotThere(): processing workshop: workshopId->{}", workshopId);
                    for (WorkUnitProcessorSummary currentWUPSummary : currentWorkshop.getWorkUnitProcessors().values()) {
                        getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing wup: currentWUPSummary->{}", currentWUPSummary);
                        MatrixRoom currentWUPSpace = null;
                        String wupSpacePseudoId = roomIdentityFactory.buildWorkUnitProcessorSpacePseudoAlias(currentWUPSummary.getParticipantName());
                        currentWUPSpace = resolveMatrixRoomFromParticipantName(currentWorkshopSpace.getContainedRooms(), wupSpacePseudoId);
                        String wupSpaceId = null;
                        String wupSpaceAliasId = null;
                        if(currentWUPSpace == null) {
                            getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing wup: currentWUPSpace->{}", currentWUPSpace);
                            currentWUPSpace = wupReplicaServices.createWorkUnitProcessorSpaceIfNotThere(workshopId, currentWUPSpace, currentWUPSummary);
                        }
                        if(currentWUPSpace != null){
                            wupSpaceId = currentWUPSpace.getRoomID();
                            wupSpaceAliasId = currentWUPSpace.getCanonicalAlias();
                        }
                        getLogger().debug(".createParticipantSpacesAndRoomsIfNotThere(): processing wup: wupSpaceAliasId->{}", wupSpaceAliasId);
                        if(StringUtils.isNotEmpty(wupSpaceId)) {
                            getLogger().debug(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: wupSpaceAliasId->{}", wupSpaceAliasId);
                            for (EndpointSummary currentEndpointSummary : currentWUPSummary.getEndpoints().values()) {
                                getLogger().info(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: currentEndpointSummary->{}", currentEndpointSummary);
                                String endpointSpacePseudoId = roomIdentityFactory.buildEndpointSpacePseudoAlias(currentEndpointSummary.getParticipantName());
                                MatrixRoom currentEndpointSpace = resolveMatrixRoomFromParticipantName(currentWUPSpace.getContainedRooms(), endpointSpacePseudoId);
                                getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: currentEndpointSpace->{}", currentEndpointSpace);
                                if(currentEndpointSpace == null){
                                    currentEndpointSpace = endpointReplicaServices.createEndpointSpaceIfRequired(currentWUPSummary.getParticipantName(), wupSpaceId, currentEndpointSpace, currentEndpointSummary);
                                }
                                String endPointId = null;
                                String endpointAliasId = null;
                                if(currentEndpointSpace != null){
                                    endPointId = currentEndpointSpace.getRoomID();
                                    endpointAliasId = currentEndpointSpace.getCanonicalAlias();
                                }
                                getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: endpointAliasId->{}", endpointAliasId);
                                if(StringUtils.isNotEmpty(endPointId)) {
                                    getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: endpointAliasId->{}", endpointAliasId);
                                    boolean isMLLPClient = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.MLLP_CLIENT);
                                    boolean isMLLPServer = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.MLLP_SERVER);
                                    boolean isHTTPClient = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.HTTP_API_CLIENT);
                                    boolean isHTTPServer = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.HTTP_API_SERVER);
                                    boolean isFileShareSink = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.FILE_SHARE_SINK);
                                    boolean isFileShareSource = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.FILE_SHARE_SOURCE);
                                    if (isHTTPClient || isHTTPServer || isMLLPClient || isMLLPServer || isFileShareSink || isFileShareSource) {
                                        matrixSpaceAPI.addChildToSpace(processingPlantSpace.getProcessingPlantSpace().getRoomID(), endPointId, matrixAccessToken.getHomeServer());
                                    }
                                } else {
                                    getLogger().info(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: Could not resolve any endpoints for ->{}", currentEndpointSummary.getParticipantName());
                                }
                            }
                        } else {
                            getLogger().error(".createParticipantSpacesAndRoomsIfNotThere(): Cannot create WUP Room for {}",currentWUPSummary.getParticipantName());
                        }
                    }
                } else {
                    getLogger().error(".createParticipantSpacesAndRoomsIfNotThere(): Cannot create Workshop Room for {}",currentWorkshop.getParticipantName());
                }
            }
        } else {
            getLogger().error(".createParticipantSpacesAndRoomsIfNotThere(): Cannot create Participant Room for {}",processingPlant.getParticipantName());
        }
        getLogger().debug(".createParticipantSpacesAndRoomsIfNotThere(): Exit");
    }

    protected MatrixRoom resolveMatrixRoomFromParticipantName(List<MatrixRoom> matrixRoomList, String pseudoAlias){
        getLogger().debug(".resolveMatrixRoomFromParticipantName(): Entry, pseudoAlias->{}, matrixRoomList->{}", pseudoAlias, matrixRoomList);
        if(matrixRoomList == null){
            getLogger().debug(".resolveMatrixRoomFromParticipantName(): Exit, matrixRoomList is null");
            return(null);
        }
        if(matrixRoomList.isEmpty()){
            getLogger().debug(".resolveMatrixRoomFromParticipantName(): Exit, matrixRoomList is empty");
            return(null);
        }
        if(StringUtils.isEmpty(pseudoAlias)){
            getLogger().debug(".resolveMatrixRoomFromParticipantName(): Exit, pseudoAlias is empty");
            return(null);
        }
        for(MatrixRoom currentRoom: matrixRoomList){
            if(StringUtils.isNotEmpty(currentRoom.getCanonicalAlias())) {
                if (currentRoom.getCanonicalAlias().contains(pseudoAlias)) {
                    getLogger().debug(".resolveMatrixRoomFromParticipantName(): Exit, room->{}", currentRoom);
                    return (currentRoom);
                }
            }
        }
        getLogger().debug(".resolveMatrixRoomFromParticipantName(): Exit, Room is not found");
        return(null);
    }

    public MatrixRoom getSpaceRoomSetForSubsystemParticipant(String subsystemParticipantName){
        getLogger().debug(".getSpaceRoomSetForSubsystemParticipant(): Entry, subsystemParticipantName->{}", subsystemParticipantName);

        String participantRoomAlias = roomIdentityFactory.buildProcessingPlantSpacePseudoAlias(subsystemParticipantName);

        MatrixRoom subsystemRoom = roomCache.getRoomFromPseudoAlias(participantRoomAlias);

        if(subsystemRoom == null){
            getLogger().debug(".getSpaceRoomSetForSubsystemParticipant(): Exit, No Room/Space Found (alias not matched), returning null");
            return(null);
        }

        String spaceId = subsystemRoom.getRoomID();
        getLogger().trace(".getSpaceRoomSetForSubsystemParticipant(): Getting hierarchy for spaceId->{}", spaceId);
        List<MatrixRoom> containedRooms = matrixSpaceAPI.getContainedRooms(subsystemRoom, 8);
        getLogger().trace(".getSpaceRoomSetForSubsystemParticipant(): containedRooms->{}", containedRooms);

        getLogger().trace(".getSpaceRoomSetForSubsystemParticipant(): Populating Hierarchy Model");
        addChildren(subsystemRoom, containedRooms);
        getLogger().trace(".getSpaceRoomSetForSubsystemParticipant(): Populating Hierarchy Model... Done");

        getLogger().debug(".getSpaceRoomSetForSubsystemParticipant(): Exit, subsystemRoom->{}", subsystemRoom);
        return(subsystemRoom);
    }

    public void addChildren(MatrixRoom parent, List<MatrixRoom> roomList){
        getLogger().debug(".addChildren(): Entry, parent->{},parent.getContainedRooms().size()->{}, List.size()->{}", parent, parent.getContainedRooms().size(), roomList.size());
        if(parent == null){
            return;
        }
        if(roomList.isEmpty()){
            return;
        }
        getLogger().trace(".addChildren(): Parent is not-null, and roomList is not empty... continuing");
        List<String> containedRoomIds = parent.getContainedRoomIds();
        for(String currentKnownChildRoomId: containedRoomIds){
            getLogger().trace(".addChildren(): Looking for Child-Room->{}", currentKnownChildRoomId);
            for(MatrixRoom currentTestRoom: roomList){
                String currentTestRoomID = currentTestRoom.getRoomID();
                if(currentTestRoomID.equals(parent.getRoomID())) {
                    getLogger().trace(".addChildren(): Room is parent, don't test!");
                } else{
                    getLogger().trace(".addChildren(): Comparing against Room in roomList with roomId->{}", currentTestRoomID);
                    if (currentKnownChildRoomId.contentEquals(currentTestRoomID)) {
                        getLogger().trace(".addChildren(): Found child, adding ->{}", currentTestRoomID);
                        parent.getContainedRooms().add(currentTestRoom);
                        addChildren(currentTestRoom, roomList);
                        break;
                    }
                }
            }
        }
        getLogger().debug(".addChildren(): Exit, parent->{}", parent);
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }

}
