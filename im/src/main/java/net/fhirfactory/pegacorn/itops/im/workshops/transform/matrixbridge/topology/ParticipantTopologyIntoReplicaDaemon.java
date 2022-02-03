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

import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixRoomMethods;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixSpaceMethods;
import net.fhirfactory.pegacorn.communicate.synapse.credentials.SynapseAdminAccessToken;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseRoomMethods;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseUserMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseAdminProxyInterface;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseUser;
import net.fhirfactory.pegacorn.core.model.petasos.endpoint.valuesets.PetasosEndpointTopologyTypeEnum;
import net.fhirfactory.pegacorn.core.model.petasos.participant.PetasosParticipantFulfillmentStatusEnum;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.*;
import net.fhirfactory.pegacorn.itops.im.datatypes.ProcessingPlantSpaceDetail;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsSystemWideTopologyMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.OAMToMatrixBridgeCache;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.factories.common.ParticipantRoomIdentityFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.factories.ParticipantTopologyIntoReplicaFactory;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ParticipantTopologyIntoReplicaDaemon extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantTopologyIntoReplicaDaemon.class);

    private boolean initialised;

    private boolean firstRunComplete;

    private boolean daemonIsStillRunning;
    private Instant daemonLastRunTime;

    private Long ROOM_SYNCHRONISATION_WATCHDOG_STARTUP_DELAY = 60000L;
    private Long ROOM_SYNCHRONISATION_WATCHDOG_CHECK_PERIOD = 30000L;
    private Long ROOM_SYNCHRONISATION_WATCHDOG_RESET_PERIOD = 900L;

    private Long SHORT_GAPPING_PERIOD = 100L;
    private Long LONG_GAPPING_PERIOD = 1000L;



    @Inject
    private MatrixRoomMethods matrixRoomAPI;

    @Inject
    private MatrixSpaceMethods matrixSpaceAPI;

    @Inject
    private SynapseAdminAccessToken synapseAccessToken;

    @Produce
    private ProducerTemplate camelRouteInjector;

    @Inject
    private SynapseAdminProxyInterface synapseAdminProxy;

    @Inject
    private SynapseRoomMethods synapseRoomAPI;

    @Inject
    private SynapseUserMethods synapseUserAPI;

    @Inject
    private OAMToMatrixBridgeCache matrixBridgeCache;

    @Inject
    private ITOpsSystemWideTopologyMapDM systemWideTopologyMap;

    @Inject
    private ParticipantTopologyIntoReplicaFactory matrixBridgeFactories;

    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    @Inject
    private WorkUnitProcessorParticipantReplicaServices wupReplicaServices;

    @Inject
    private WorkshopParticipantReplicaServices workshopReplicaServices;

    @Inject
    private ProcessingPlantParticipantReplicaServices processingPlantReplicaServices;

    @Inject
    private EndpointParticipantReplicaServices endpointReplicaServices;

    //
    // Constructor(s)
    //

    public ParticipantTopologyIntoReplicaDaemon() {
        super();
        this.initialised = false;
        this.firstRunComplete = false;
        this.daemonIsStillRunning = false;
        this.daemonLastRunTime = Instant.EPOCH;

    }

    //
    // Post Construct
    //

    @PostConstruct
    public void initialise() {
        getLogger().debug(".initialise(): Entry");
        if (initialised) {
            getLogger().debug(".initialise(): Exit, already initialised, nothing to do");
            return;
        }
        getLogger().info(".initialise(): Initialisation Start...");

        scheduleTopologyReplicationSynchronisation();

        this.initialised = true;

        getLogger().info(".initialise(): Initialisation Finish...");
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger() {
        return (LOG);
    }

    protected SynapseAdminAccessToken getSynapseAccessToken() {
        return (synapseAccessToken);
    }

    protected Instant getDaemonLastRunTime() {
        return (daemonLastRunTime);
    }

    protected void setDaemonLastRunTime(Instant instant){
        this.daemonLastRunTime = instant;
    }

    protected Long getRoomSynchronisationWatchdogResetPeriod() {
        return (this.ROOM_SYNCHRONISATION_WATCHDOG_RESET_PERIOD);
    }

    protected ConcurrentHashMap<String, SynapseRoom> getKnownRoomSet() {
        return (matrixBridgeCache.getKnownRoomSet());
    }

    protected ConcurrentHashMap<String, SynapseUser> getKnownUserSet() {
        return (matrixBridgeCache.getKnownUserSet());
    }

    protected OAMToMatrixBridgeCache getMatrixBridgeCache(){
        return(matrixBridgeCache);
    }

    protected ParticipantRoomIdentityFactory getRoomIdentityFactory(){
        return(roomIdentityFactory);
    }

    protected ParticipantTopologyIntoReplicaFactory getMatrixBridgeFactories(){
        return(matrixBridgeFactories);
    }

    protected SynapseRoomMethods getSynapseRoomAPI() {
        return synapseRoomAPI;
    }

    protected MatrixRoomMethods getMatrixRoomAPI() {
        return matrixRoomAPI;
    }

    protected MatrixSpaceMethods getMatrixSpaceAPI() {
        return matrixSpaceAPI;
    }

    protected WorkshopParticipantReplicaServices getWorkshopReplicaServices(){
        return(this.workshopReplicaServices);
    }

    protected ProcessingPlantParticipantReplicaServices getProcessingPlantReplicaServices(){
        return(this.processingPlantReplicaServices);
    }

    protected EndpointParticipantReplicaServices getEndpointReplicaServices(){
        return(this.endpointReplicaServices);
    }

    //
    // Scheduler
    //

    private void scheduleTopologyReplicationSynchronisation() {
        getLogger().debug(".scheduleTopologyReplicationSynchronisation(): Entry");
        TimerTask topologyReplicationSynchronisationTask = new TimerTask() {
            public void run() {
                getLogger().debug(".topologyReplicationSynchronisationTask(): Entry");
                if (!daemonIsStillRunning) {
                    topologyReplicationSynchronisationDaemon();
                    setDaemonLastRunTime(Instant.now());
                } else {
                    Long ageSinceRun = Instant.now().getEpochSecond() - getDaemonLastRunTime().getEpochSecond();
                    if (ageSinceRun > getRoomSynchronisationWatchdogResetPeriod()) {
                        topologyReplicationSynchronisationDaemon();
                    }
                }
                getLogger().debug(".topologyReplicationSynchronisationTask(): Exit");
            }
        };
        Timer timer = new Timer("TopologyReplicationSynchronisation");
        timer.schedule(topologyReplicationSynchronisationTask, ROOM_SYNCHRONISATION_WATCHDOG_STARTUP_DELAY, ROOM_SYNCHRONISATION_WATCHDOG_CHECK_PERIOD);
        getLogger().debug(".scheduleTopologyReplicationSynchronisation(): Exit");
    }

    //
    //
    //

    private void topologyReplicationSynchronisationDaemon() {
        getLogger().info(".topologyReplicationSynchronisationDaemon(): Entry");

        systemWideTopologyMap.printMap();

        daemonIsStillRunning = true;
        this.daemonLastRunTime = Instant.now();

        // 1st, Always check we have an access token
        if (StringUtils.isEmpty(getSynapseAccessToken().getRemoteAccessToken())) {
            getLogger().info(".topologyReplicationSynchronisationDaemon(): [Login] Start...");
            String accessToken = (String) camelRouteInjector.sendBody(synapseAdminProxy.getSynapseLoginIngresEndpoint(), ExchangePattern.InOut, "Do it!!!");
            getLogger().trace(".topologyReplicationSynchronisationDaemon(): [Login] accessToken->{}", accessToken);
            getLogger().info(".topologyReplicationSynchronisationDaemon(): [Login] Finish...");
        }

        // ONLY FOR DEVEL, CLEAN OUT SYNAPSE
        /*
        if (!firstRunComplete) {
            getLogger().info(".topologyReplicationSynchronisationDaemon(): [Clean Out Room List] Start...");
            List<SynapseRoom> startupRoomList = synapseRoomAPI.getRooms("*");
            for (SynapseRoom currentRoom : startupRoomList) {
                String creator = currentRoom.getCreator().toLowerCase(Locale.ROOT);
                if (creator.contains("hunter") || creator.contains("replicabridge")) {
                    getLogger().info(".topologyReplicationSynchronisationDaemon(): [Clean Out Room List] Deleting Room->{}", currentRoom.getName());
                    synapseRoomAPI.deleteRoom(currentRoom.getRoomID(), "cleaning up");
                }
                waitALittleBitLonger();
            }
            firstRunComplete = true;
            getLogger().info(".topologyReplicationSynchronisationDaemon(): [Clean Out Room List] Finish...");
        }

        if (getLogger().isTraceEnabled()) {
            topologyMap.printMap();
        }
         */

        // 2nd, Synchronise Existing Room List
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] Start...");
        List<SynapseRoom> roomList = synapseRoomAPI.getRooms("*");
        getLogger().trace(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] RoomList->{}", roomList);
        for (SynapseRoom currentRoom : roomList) {
            getLogger().trace(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] Processing Room ->{}", currentRoom);
            matrixBridgeCache.addRoomFromMatrix(currentRoom);
        }
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] Finish...");

        // 3rd, Synchronise Participant List
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Start...");
        List<ProcessingPlantSummary> processingPlants = systemWideTopologyMap.getProcessingPlants();
        if (processingPlants.isEmpty()) {
            // do nothing
        } else {
            getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Adding Processing Plants...");
            boolean shouldWaitAMoment = false;
            for (ProcessingPlantSummary currentProcessingPlant : processingPlants) {
                getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Processing ->{}", currentProcessingPlant);
                //
                // Check to see if entry exists... we don't automatically delete, merely update
                PetasosParticipantSummary participantSummary = matrixBridgeCache.getParticipant(currentProcessingPlant.getParticipantName());
                if (participantSummary == null) {
                    getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Participant Does Not Exit, Adding");
                    PetasosParticipantSummary newParticipantSummary = matrixBridgeFactories.newPetasosParticipantSummary(currentProcessingPlant);
                    matrixBridgeCache.addParticipant(newParticipantSummary);
                    participantSummary = newParticipantSummary;
                } else {
                    getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Participant Exits, Updating");
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
                getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] expectedFulfillerCount->{}, actualFilfillmentCount->{}", expectedFulfillerCount, actualFulfillerCount);
                if (expectedFulfillerCount > actualFulfillerCount) {
                    getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Participant Partially Fulfilled");
                    participantSummary.getFulfillmentState().setFulfillmentStatus(PetasosParticipantFulfillmentStatusEnum.PETASOS_PARTICIPANT_PARTIALLY_FULFILLED);
                }
                if (expectedFulfillerCount == actualFulfillerCount) {
                    getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Participant Fully Fulfilled");
                    participantSummary.getFulfillmentState().setFulfillmentStatus(PetasosParticipantFulfillmentStatusEnum.PETASOS_PARTICIPANT_FULLY_FULFILLED);
                }
            }
        }
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Finish...");

        // 4th, Adding Subsystem Space(s) If Required
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Add Space(s) As Required] Start...");
        List<String> spaceNameSet = matrixBridgeCache.getSpaceNameSet();
        List<String> subsystemNameSet = matrixBridgeCache.getSubsystemParticipantNameSet();

        // Adding Space(s) and Room(s) If Required
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Add Space(s) For Workshops As Required] Start...");
        for (ProcessingPlantSummary currentProcessingPlant : processingPlants) {
            ProcessingPlantSpaceDetail processingPlantSpace = getProcessingPlantReplicaServices().createProcessingPlantSpace(currentProcessingPlant.getParticipantName(), roomList);
            for (WorkshopSummary currentWorkshop : currentProcessingPlant.getWorkshops().values()) {
                String workshopId = getWorkshopReplicaServices().createSubSpace(processingPlantSpace.getComponentSpaceId(), roomList, currentWorkshop);
                for (WorkUnitProcessorSummary currentWUPSummary : currentWorkshop.getWorkUnitProcessors().values()) {
                    String wupSpaceId = wupReplicaServices.createWorkUnitProcessorSpace(workshopId, roomList, currentWUPSummary);
                    for(EndpointSummary currentEndpointSummary: currentWUPSummary.getEndpoints().values()){
                        String endPointId = getEndpointReplicaServices().createEndpointSpaceIfRequired(currentEndpointSummary.getParticipantName(),wupSpaceId, roomList, currentEndpointSummary);
                        boolean isMLLPClient = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.MLLP_CLIENT);
                        boolean isMLLPServer = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.MLLP_SERVER);
                        boolean isHTTPClient = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.HTTP_API_CLIENT);
                        boolean isHTTPServer = currentEndpointSummary.getEndpointType().equals(PetasosEndpointTopologyTypeEnum.HTTP_API_SERVER);
                        if( isHTTPClient || isHTTPServer || isMLLPClient || isMLLPServer ){
                            getMatrixSpaceAPI().addChildToSpace(processingPlantSpace.getParticipantSpaceId(), endPointId);
                        }
                    }
                }
            }
        }
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Add Space(s) For Workshops As Required] Finished...");

        //
        // Check to see if Rooms were added
        Set<SynapseRoom> addedRoomSet = new HashSet<>();
        for(SynapseRoom currentRoom: roomList){
            Enumeration<String> knownRoomIdEnumerator = getKnownRoomSet().keys();
            boolean alreadyKnown = false;
            while(knownRoomIdEnumerator.hasMoreElements()){
                String knownRoomId = knownRoomIdEnumerator.nextElement();
                if(currentRoom.getRoomID().equals(knownRoomId)){
                    alreadyKnown = true;
                    break;
                }
            }
            if (!alreadyKnown) {
                if(!addedRoomSet.contains(currentRoom)) {
                    addedRoomSet.add(currentRoom);
                }
            }
        }

        //
        // Add all users to the new rooms
        List<SynapseUser> userList = synapseUserAPI.getALLAccounts();
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Auto Join Users to Added Rooms] Start...");
        for (SynapseRoom currentRoom : addedRoomSet) {
            String currentRoomAlias = currentRoom.getCanonicalAlias();
            if (StringUtils.isNotEmpty(currentRoomAlias)) {
                if (currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_EVENTS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_METRICS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_TASKS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_SUBSCRIPTIONS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WORKSHOP.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_EVENTS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_TASKS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_METRICS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_SUBSCRIPTIONS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_EVENTS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_METRICS.getAliasPrefix())) {

                    getLogger().info(".topologyReplicationSynchronisationDaemon(): [AAuto Join Users to Added Rooms] Processing Space->{}", currentRoomAlias);
                    String roomId = currentRoom.getRoomID();
                    for (SynapseUser currentUser : userList) {
                        if (currentUser.getName().startsWith("@" + synapseAccessToken.getUserName())) {
                            // do nothing
                        } else {
                            getLogger().info(".topologyReplicationSynchronisationDaemon(): [Auto Join Users to Added Rooms] Processing User->{}", currentUser.getName());
                            synapseRoomAPI.addRoomMember(roomId, currentUser.getName());
                            waitALittleBit();
                        }
                    }
                }
            }
        }
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Auto Join Users to Added Rooms] Finish...");

        //
        // See if there are new Users
        Set<SynapseUser> addedUserSet = new HashSet<>();
        for(SynapseUser currentUser: userList){
            Enumeration<String> knownUserEnumeration = getKnownUserSet().keys();
            boolean alreadyKnown = false;
            while(knownUserEnumeration.hasMoreElements()){
                String knownUserId = knownUserEnumeration.nextElement();
                if(currentUser.getName().equals(knownUserId)){
                    alreadyKnown = true;
                    break;
                }
            }
            if (!alreadyKnown) {
                if(!addedUserSet.contains(currentUser)) {
                    addedUserSet.add(currentUser);
                }
            }
        }

        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Auto Join New Users to the Older Rooms] Start...");
        for (SynapseRoom currentRoom : getKnownRoomSet().values()) {
            String currentRoomAlias = currentRoom.getCanonicalAlias();
            if (StringUtils.isNotEmpty(currentRoomAlias)) {
                if (currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_EVENTS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_METRICS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_TASKS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_SUBSCRIPTIONS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WORKSHOP.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_EVENTS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_TASKS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_METRICS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_SUBSCRIPTIONS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_EVENTS.getAliasPrefix()) ||
                        currentRoomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_METRICS.getAliasPrefix())) {

                    getLogger().info(".topologyReplicationSynchronisationDaemon(): [uto Join New Users to the Older Rooms] Processing Room/Space->{}", currentRoomAlias);
                    String roomId = currentRoom.getRoomID();
                    for (SynapseUser currentUser : addedUserSet) {
                        if (currentUser.getName().contains(synapseAccessToken.getUserName())) {
                            // do nothing
                        } else {
                            getLogger().info(".topologyReplicationSynchronisationDaemon(): [uto Join New Users to the Older Rooms] Processing User->{}", currentUser.getName());
                            synapseRoomAPI.addRoomMember(roomId, currentUser.getName());
                            waitALittleBit();
                        }
                    }
                }
            }
        }
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Auto Join New Users to the Older Rooms] Finished...");

        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Update known room set] start");
        getKnownRoomSet().clear();
        for(SynapseRoom currentRoom: roomList){
            if(!getKnownRoomSet().containsKey(currentRoom.getRoomID())){
                getKnownRoomSet().put(currentRoom.getRoomID(), currentRoom);
            }
        }
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Update known room set] finish");

        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Update known user set] start");
        getKnownUserSet().clear();
        for(SynapseUser currentUser: userList){
            if(!getKnownUserSet().containsKey(currentUser.getName())){
                getKnownUserSet().put(currentUser.getName(), currentUser);
            }
        }
        getLogger().info(".topologyReplicationSynchronisationDaemon(): [Update known user set] finish");

        daemonIsStillRunning = false;

        getLogger().info(".topologyReplicationSynchronisationDaemon(): Exit");
    }

    //
    // Mechanism to ensure Startup
    //

    @Override
    public void configure () throws Exception {
        String processingPlantName = getClass().getSimpleName();

        from("timer://" + processingPlantName + "?delay=1000&repeatCount=1")
                .routeId("ProcessingPlant::" + processingPlantName)
                .log(LoggingLevel.DEBUG, "Starting....");
    }

    protected void waitALittleBit(){
        try {
            Thread.sleep(SHORT_GAPPING_PERIOD);
        } catch (Exception e) {
            getLogger().info(".waitALittleBit():...{}, {}", ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
        }
    }

    protected void waitALittleBitLonger(){
        try {
            Thread.sleep(LONG_GAPPING_PERIOD);
        } catch (Exception e) {
            getLogger().info(".waitALittleBitLonger():...{}, {}", ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
        }

    }

}
