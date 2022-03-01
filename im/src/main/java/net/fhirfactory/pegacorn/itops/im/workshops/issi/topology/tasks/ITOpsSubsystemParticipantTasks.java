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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.tasks;

import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixRoomMethods;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixSpaceMethods;
import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixRoom;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseRoomMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.core.model.petasos.endpoint.valuesets.PetasosEndpointTopologyTypeEnum;
import net.fhirfactory.pegacorn.core.model.petasos.participant.PetasosParticipantFulfillmentStatusEnum;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.*;
import net.fhirfactory.pegacorn.itops.im.datatypes.ProcessingPlantSpaceDetail;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownParticipantMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsSystemWideReportedTopologyMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownRoomAndSpaceMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.EndpointParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.ProcessingPlantParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.WorkUnitProcessorParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.WorkshopParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.ParticipantRoomIdentityFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.topology.ParticipantTopologyIntoReplicaFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class ITOpsSubsystemParticipantTasks {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsSubsystemParticipantTasks.class);

    @Inject
    private ITOpsSystemWideReportedTopologyMapDM systemWideTopologyMap;

    @Inject
    private ITOpsKnownRoomAndSpaceMapDM roomCache;

    @Inject
    private ITOpsKnownParticipantMapDM participantCache;

    @Inject
    private ParticipantTopologyIntoReplicaFactory matrixBridgeFactories;

    @Inject
    private ProcessingPlantParticipantReplicaFactory processingPlantReplicaServices;

    @Inject
    private WorkUnitProcessorParticipantReplicaFactory wupReplicaServices;

    @Inject
    private WorkshopParticipantReplicaFactory workshopReplicaServices;

    @Inject
    private EndpointParticipantReplicaFactory endpointReplicaServices;

    @Inject
    private SynapseRoomMethods synapseRoomAPI;

    @Inject
    private MatrixRoomMethods matrixRoomAPI;

    @Inject
    private MatrixSpaceMethods matrixSpaceAPI;

    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    //
    // Business Methods
    //

    public void updateParticipantListUsingReportedMetrics(){
        getLogger().debug(".injectSubsystemParticipantRoomSet(): [Synchronise Participant List] Start...");
        List<ProcessingPlantSummary> processingPlants = getSystemWideTopologyMap().getProcessingPlants();
        if (processingPlants.isEmpty()) {
            // do nothing
        } else {
            getLogger().debug(".injectSubsystemParticipantRoomSet(): [Synchronise Participant List] Adding Processing Plants...");
            for (ProcessingPlantSummary currentProcessingPlant : processingPlants) {
                getLogger().trace(".injectSubsystemParticipantRoomSet(): [Synchronise Participant List] Processing ->{}", currentProcessingPlant);
                //
                // Check to see if entry exists... we don't automatically delete, merely update
                PetasosParticipantSummary participantSummary = getParticipantCache().getParticipant(currentProcessingPlant.getParticipantName());
                if (participantSummary == null) {
                    getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Participant Does Not Exit, Adding");
                    PetasosParticipantSummary newParticipantSummary = getMatrixBridgeFactories().newPetasosParticipantSummary(currentProcessingPlant);
                    getParticipantCache().addParticipant(newParticipantSummary);
                    participantSummary = newParticipantSummary;
                } else {
                    getLogger().debug(".injectSubsystemParticipantRoomSet(): [Synchronise Participant List] Participant Exits, Updating");
                    if (!participantSummary.getFulfillmentState().getFulfillerComponents().contains(currentProcessingPlant.getComponentID())) {
                        Integer currentFulfillerCount = participantSummary.getFulfillmentState().getNumberOfActualFulfillers();
                        currentFulfillerCount += 1;
                        participantSummary.getFulfillmentState().setNumberOfActualFulfillers(currentFulfillerCount);
                    }
                    participantSummary.setLastSynchronisationInstant(currentProcessingPlant.getLastSynchronisationInstant());
                    participantSummary.setLastActivityInstant(currentProcessingPlant.getLastActivityInstant());
                }
                int expectedFulfillerCount = participantSummary.getFulfillmentState().getNumberOfFulfillersExpected();
                int actualFulfillerCount = participantSummary.getFulfillmentState().getNumberOfActualFulfillers();
                getLogger().debug(".injectSubsystemParticipantRoomSet(): [Synchronise Participant List] expectedFulfillerCount->{}, actualFilfillmentCount->{}", expectedFulfillerCount, actualFulfillerCount);
                if (expectedFulfillerCount > actualFulfillerCount) {
                    getLogger().debug(".injectSubsystemParticipantRoomSet(): [Synchronise Participant List] Participant Partially Fulfilled");
                    participantSummary.getFulfillmentState().setFulfillmentStatus(PetasosParticipantFulfillmentStatusEnum.PETASOS_PARTICIPANT_PARTIALLY_FULFILLED);
                }
                if (expectedFulfillerCount == actualFulfillerCount) {
                    getLogger().debug(".injectSubsystemParticipantRoomSet(): [Synchronise Participant List] Participant Fully Fulfilled");
                    participantSummary.getFulfillmentState().setFulfillmentStatus(PetasosParticipantFulfillmentStatusEnum.PETASOS_PARTICIPANT_FULLY_FULFILLED);
                }
            }
        }
        getLogger().debug(".injectSubsystemParticipantRoomSet(): [Synchronise Participant List] Finish...");
    }

    public void createParticipantSpacesAndRoomsIfNotThere(ProcessingPlantSummary processingPlant, MatrixRoom processingPlantMatrixRoom){
        getLogger().debug(".createParticipantSpacesAndRoomsIfNotThere(): Entry, processingPlant->{}, processingPlantMatrixRoom->{}", processingPlant, processingPlantMatrixRoom);

        ProcessingPlantSpaceDetail processingPlantSpace = getProcessingPlantReplicaServices().createProcessingPlantSpaceIfNotThere(processingPlant.getParticipantName(), processingPlantMatrixRoom);
        if(processingPlantSpace != null) {
            getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processingPlantSpace->{}", processingPlantSpace);
            for (WorkshopSummary currentWorkshop : processingPlant.getWorkshops().values()) {
                getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing workshop: currentWorkshop->{}", currentWorkshop);
                MatrixRoom currentWorkshopSpace = resolveMatrixRoomFromParticipantName(processingPlantSpace.getProcessingPlantComponentSpace().getContainedRooms(), currentWorkshop.getParticipantName());
                String workshopId = null;
                if(currentWorkshopSpace == null) {
                    getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing workshop: creating currentWorkshopSpace->{}", currentWorkshopSpace);
                    currentWorkshopSpace = getWorkshopReplicaServices().createSubSpaceIfNotThere(processingPlantSpace.getProcessingPlantComponentSpace().getRoomID(), currentWorkshopSpace, currentWorkshop);
                    if(currentWorkshopSpace != null){
                        workshopId = currentWorkshopSpace.getRoomID();
                    }
                } else {
                    getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing workshop: matrix space already exists for currentWorkshopSpace->{}", currentWorkshopSpace);
                    workshopId = currentWorkshopSpace.getRoomID();
                }
                if(StringUtils.isNotEmpty(workshopId)) {
                    getLogger().info(".createParticipantSpacesAndRoomsIfNotThere(): processing workshop: workshopId->{}", workshopId);
                    for (WorkUnitProcessorSummary currentWUPSummary : currentWorkshop.getWorkUnitProcessors().values()) {
                        getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing wup: currentWUPSummary->{}", currentWUPSummary);
                        MatrixRoom currentWUPSpace = null;
                        currentWUPSpace = resolveMatrixRoomFromParticipantName(currentWorkshopSpace.getContainedRooms(), currentWUPSummary.getParticipantName());
                        String wupSpaceId = null;
                        String wupSpaceAliasId = null;
                        if(currentWUPSpace == null) {
                            getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing wup: currentWUPSpace->{}", currentWUPSpace);
                            currentWUPSpace = getWupReplicaServices().createWorkUnitProcessorSpaceIfNotThere(workshopId, currentWUPSpace, currentWUPSummary);
                        }
                        if(currentWUPSpace != null){
                            wupSpaceId = currentWUPSpace.getRoomID();
                            wupSpaceAliasId = currentWUPSpace.getCanonicalAlias();
                        }
                        getLogger().info(".createParticipantSpacesAndRoomsIfNotThere(): processing wup: wupSpaceAliasId->{}", wupSpaceAliasId);
                        if(StringUtils.isNotEmpty(wupSpaceId)) {
                            getLogger().info(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: wupSpaceAliasId->{}", wupSpaceAliasId);
                            for (EndpointSummary currentEndpointSummary : currentWUPSummary.getEndpoints().values()) {
                                getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: currentEndpointSummary->{}", currentEndpointSummary);
                                MatrixRoom currentEndpointSpace = resolveMatrixRoomFromParticipantName(currentWUPSpace.getContainedRooms(), currentEndpointSummary.getParticipantName());
                                getLogger().trace(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: currentEndpointSpace->{}", currentEndpointSpace);
                                if(currentEndpointSpace == null){
                                    currentEndpointSpace = getEndpointReplicaServices().createEndpointSpaceIfRequired(currentEndpointSummary.getParticipantName(), wupSpaceId, currentEndpointSpace, currentEndpointSummary);
                                }
                                String endPointId = null;
                                String endpointAliasId = null;
                                if(currentEndpointSpace != null){
                                    endPointId = currentEndpointSpace.getRoomID();
                                    endpointAliasId = currentEndpointSpace.getCanonicalAlias();
                                }
                                getLogger().info(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: endpointAliasId->{}", endpointAliasId);
                                if(StringUtils.isNotEmpty(endPointId)) {
                                    getLogger().info(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: endpointAliasId->{}", endpointAliasId);
                                    boolean isMLLPClient = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.MLLP_CLIENT);
                                    boolean isMLLPServer = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.MLLP_SERVER);
                                    boolean isHTTPClient = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.HTTP_API_CLIENT);
                                    boolean isHTTPServer = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.HTTP_API_SERVER);
                                    if (isHTTPClient || isHTTPServer || isMLLPClient || isMLLPServer) {
                                        getMatrixSpaceAPI().addChildToSpace(processingPlantSpace.getProcessingPlantSpace().getRoomID(), endPointId);
                                    }
                                } else {
                                    getLogger().warn(".createParticipantSpacesAndRoomsIfNotThere(): processing endpoints for wup: Could not resolve any endpoints for ->{}", currentEndpointSummary.getParticipantName());
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

    protected MatrixRoom resolveMatrixRoomFromParticipantName(List<MatrixRoom> matrixRoomList, String participantName){
        getLogger().debug(".resolveMatrixRoomFromParticipantName(): Entry, participantName->{}, matrixRoomList->{}", participantName, matrixRoomList);
        if(matrixRoomList == null){
            getLogger().debug(".resolveMatrixRoomFromParticipantName(): Exit, matrixRoomList is null");
            return(null);
        }
        if(matrixRoomList.isEmpty()){
            getLogger().debug(".resolveMatrixRoomFromParticipantName(): Exit, matrixRoomList is empty");
            return(null);
        }
        if(StringUtils.isEmpty(participantName)){
            getLogger().debug(".resolveMatrixRoomFromParticipantName(): Exit, participantName is empty");
            return(null);
        }
        String convertedParticipantName = participantName.toLowerCase(Locale.ROOT).replace(".", "-");
        for(MatrixRoom currentRoom: matrixRoomList){
            if(StringUtils.isNotEmpty(currentRoom.getCanonicalAlias())) {
                if (currentRoom.getCanonicalAlias().contains(convertedParticipantName)) {
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

        String participantRoomAlias = getRoomIdentityFactory().buildProcessingPlantCanonicalAlias(subsystemParticipantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM);

        MatrixRoom subsystemRoom = roomCache.getRoomFromPseudoAlias(participantRoomAlias);

        if(subsystemRoom == null){
            getLogger().debug(".getSpaceRoomSetForSubsystemParticipant(): Exit, No Room/Space Found (alias not matched), returning null");
            return(null);
        }

        String spaceId = subsystemRoom.getRoomID();
        getLogger().trace(".getSpaceRoomSetForSubsystemParticipant(): Getting hierarchy for spaceId->{}", spaceId);
        List<MatrixRoom> containedRooms = matrixSpaceAPI.getContainedRoomIDs(subsystemRoom, 8);
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
                getLogger().trace(".addChildren(): Comparing against Room in roomList with roomId->{}", currentTestRoomID);
                if(currentKnownChildRoomId.contentEquals(currentTestRoomID)){
                    getLogger().trace(".addChildren(): Found child, adding ->{}", currentTestRoomID);
                    parent.getContainedRooms().add(currentTestRoom);
                    addChildren(currentTestRoom, roomList);
                    break;
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

    protected ITOpsSystemWideReportedTopologyMapDM getSystemWideTopologyMap(){
        return(this.systemWideTopologyMap);
    }

    protected ITOpsKnownRoomAndSpaceMapDM getRoomCache(){
        return(this.roomCache);
    }

    protected ParticipantTopologyIntoReplicaFactory getMatrixBridgeFactories(){
        return(this.matrixBridgeFactories);
    }

    protected ProcessingPlantParticipantReplicaFactory getProcessingPlantReplicaServices(){
        return(this.processingPlantReplicaServices);
    }

    protected WorkUnitProcessorParticipantReplicaFactory getWupReplicaServices(){
        return(this.wupReplicaServices);
    }

    protected WorkshopParticipantReplicaFactory getWorkshopReplicaServices(){
        return(this.workshopReplicaServices);
    }

    protected EndpointParticipantReplicaFactory getEndpointReplicaServices(){
        return(this.endpointReplicaServices);
    }

    protected SynapseRoomMethods getSynapseRoomAPI(){
        return(this.synapseRoomAPI);
    }

    protected MatrixSpaceMethods getMatrixSpaceAPI(){
        return(this.matrixSpaceAPI);
    }

    protected ParticipantRoomIdentityFactory getRoomIdentityFactory(){
        return(this.roomIdentityFactory);
    }

    protected ITOpsKnownParticipantMapDM getParticipantCache(){
        return(this.participantCache);
    }
}
