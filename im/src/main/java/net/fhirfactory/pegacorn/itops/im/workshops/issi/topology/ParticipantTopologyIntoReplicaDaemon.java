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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.topology;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixRoom;
import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixUser;
import net.fhirfactory.pegacorn.communicate.matrixbridge.workshops.matrixbridge.common.SynapseServerConnectionInitialisation;
import net.fhirfactory.pegacorn.communicate.synapse.credentials.SynapseAdminAccessToken;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseUserMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseUser;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.ProcessingPlantSummary;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownParticipantMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownUserMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsSystemWideReportedTopologyMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownRoomAndSpaceMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.EndpointParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.ProcessingPlantParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.WorkUnitProcessorParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.WorkshopParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.oam.ITOpsIMConsoleEventLogger;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.tasks.ITOpsSubsystemParticipantTasks;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.tasks.ITOpsTopologySynchronisationTasks;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.tasks.ITOpsUserTasks;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.ParticipantRoomIdentityFactory;
import org.apache.camel.LoggingLevel;
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

@ApplicationScoped
public class ParticipantTopologyIntoReplicaDaemon extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantTopologyIntoReplicaDaemon.class);

    private boolean initialised;

    private boolean firstRunComplete;

    private Instant startupTime;

    private boolean topologySynchronisationDaemonIsStillRunning;
    private Instant topologySynchronisationDaemonLastRunTime;

    private boolean userRoomSynchronisationDaemonIsStillRunning;
    private Instant userRoomSynchronisationDaemonLastRunTime;

    private Instant lastFullUserUpdate;
    private Instant lastFullRoomUpdate;

    private Long ROOM_SYNCHRONISATION_WATCHDOG_STARTUP_DELAY = 60000L; // Milliseconds
    private Long USER_SYNCHRONISATION_OVERRIDE_PERIOD = 900L; // Seconds
    private Long ROOM_COMPLETE_SYNCHRONISATION_PERIOD = 900L; // Seconds
    private Long ROOM_SYNCHRONISATION_WATCHDOG_CHECK_PERIOD = 60000L;  // Milliseconds
    private Long ROOM_SYNCHRONISATION_WATCHDOG_RESET_PERIOD = 1800L;  // Milliseconds
    private static Long ROOM_INITIAL_CHECK_PERIOD = 300L;

    private Long SHORT_GAPPING_PERIOD = 100L;
    private Long LONG_GAPPING_PERIOD = 1000L;

    @Inject
    private SynapseServerConnectionInitialisation serverConnectionInitialisation;

    @Inject
    private ITOpsSubsystemParticipantTasks itopsSubsystemParticipantTasks;

    @Inject
    private MatrixAccessToken matrixAccessToken;

    @Inject
    private SynapseAdminAccessToken synapseAccessToken;

    @Inject
    private SynapseUserMethods synapseUserAPI;

    @Inject
    private ITOpsKnownParticipantMapDM participantMapDM;

    @Inject
    private ITOpsKnownRoomAndSpaceMapDM roomCache;

    @Inject
    private ITOpsKnownUserMapDM userCache;

    @Inject
    private ITOpsSystemWideReportedTopologyMapDM systemWideTopologyMap;

    @Inject
    private ITOpsTopologySynchronisationTasks matrixCacheSynchronisationTasks;

    @Inject
    private ITOpsUserTasks userTasks;

    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    @Inject
    private ITOpsIMConsoleEventLogger itopsConsoleLogger;

    @Inject
    private ProcessingPlantParticipantReplicaFactory processingPlantReplicaServices;
    
    @Inject
    private ITOpsIMConsoleEventLogger itopsConsoleLogger;

    //
    // Constructor(s)
    //

    public ParticipantTopologyIntoReplicaDaemon() {
        super();
        this.initialised = false;
        this.firstRunComplete = false;
        this.topologySynchronisationDaemonIsStillRunning = false;
        this.topologySynchronisationDaemonLastRunTime = Instant.EPOCH;
        this.lastFullUserUpdate = Instant.EPOCH;
        this.lastFullRoomUpdate = Instant.EPOCH;
        this.startupTime = Instant.now();
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
        scheduleUserRoomSynchronisation();

        this.initialised = true;

        getLogger().info(".initialise(): Initialisation Finish...");
    }

    //
    // Getters (and Setters)
    //

    protected Long getRoomCompleteSynchronisationPeriod(){
        return(ROOM_COMPLETE_SYNCHRONISATION_PERIOD);
    }

    protected Logger getLogger() {
        return (LOG);
    }

    protected Instant getTopologySynchronisationDaemonLastRunTime() {
        return (topologySynchronisationDaemonLastRunTime);
    }

    protected Instant getLastFullRoomUpdate(){
        return(this.lastFullRoomUpdate);
    }

    protected void setTopologySynchronisationDaemonLastRunTime(Instant instant){
        this.topologySynchronisationDaemonLastRunTime = instant;
    }

    protected Long getRoomSynchronisationWatchdogResetPeriod() {
        return (this.ROOM_SYNCHRONISATION_WATCHDOG_RESET_PERIOD);
    }

    protected ITOpsKnownUserMapDM getUserCache(){
        return(this.userCache);
    }

    protected ITOpsKnownRoomAndSpaceMapDM getRoomCache(){
        return(roomCache);
    }

    protected ITOpsSystemWideReportedTopologyMapDM getSystemWideTopologyMap(){
        return(this.systemWideTopologyMap);
    }

    public boolean isUserRoomSynchronisationDaemonIsStillRunning() {
        return userRoomSynchronisationDaemonIsStillRunning;
    }

    public void setUserRoomSynchronisationDaemonIsStillRunning(boolean userRoomSynchronisationDaemonIsStillRunning) {
        this.userRoomSynchronisationDaemonIsStillRunning = userRoomSynchronisationDaemonIsStillRunning;
    }

    public Instant getUserRoomSynchronisationDaemonLastRunTime() {
        return userRoomSynchronisationDaemonLastRunTime;
    }

    public void setUserRoomSynchronisationDaemonLastRunTime(Instant userRoomSynchronisationDaemonLastRunTime) {
        this.userRoomSynchronisationDaemonLastRunTime = userRoomSynchronisationDaemonLastRunTime;
    }

    public Instant getStartupTime(){
        return(this.startupTime);
    }

    protected Long getRoomInitialCheckPeriod(){
        return(ROOM_INITIAL_CHECK_PERIOD);
    }

    //
    // Topology Synchronisation Scheduler
    //

    private void scheduleTopologyReplicationSynchronisation() {
        getLogger().debug(".scheduleTopologyReplicationSynchronisation(): Entry");
        TimerTask topologyReplicationSynchronisationTask = new TimerTask() {
            public void run() {
                getLogger().debug(".topologyReplicationSynchronisationTask(): Entry");
                if (!topologySynchronisationDaemonIsStillRunning) {
                    topologyReplicationSynchronisationDaemon();
                    setTopologySynchronisationDaemonLastRunTime(Instant.now());
                } else {
                    Long ageSinceRun = Instant.now().getEpochSecond() - getTopologySynchronisationDaemonLastRunTime().getEpochSecond();
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
    // User Synchronisation Scheduler
    //

    private void scheduleUserRoomSynchronisation() {
        getLogger().debug(".scheduleUserRoomSynchronisation(): Entry");
        TimerTask userRoomSynchronisationTask = new TimerTask() {
            public void run() {
                getLogger().debug(".userRoomSynchronisationTask(): Entry");
                if (!userRoomSynchronisationDaemonIsStillRunning) {
                    userRoomSynchronisationDaemon();
                    setUserRoomSynchronisationDaemonLastRunTime(Instant.now());
                } else {
                    Long ageSinceRun = Instant.now().getEpochSecond() - getUserRoomSynchronisationDaemonLastRunTime().getEpochSecond();
                    if (ageSinceRun > getRoomSynchronisationWatchdogResetPeriod()) {
                        userRoomSynchronisationDaemon();
                    }
                }
                getLogger().debug(".userRoomSynchronisationTask(): Exit");
            }
        };
        Timer timer = new Timer("UserRoomSynchronisationTaskTimer");
        timer.schedule(userRoomSynchronisationTask, ROOM_SYNCHRONISATION_WATCHDOG_STARTUP_DELAY, ROOM_SYNCHRONISATION_WATCHDOG_CHECK_PERIOD);
        getLogger().debug(".scheduleUserRoomSynchronisation(): Exit");
    }

    //
    // User Synchronisation Task
    //

    private void userRoomSynchronisationDaemon() {
        getLogger().debug(".userRoomSynchronisationDaemon(): Entry");

        setUserRoomSynchronisationDaemonIsStillRunning(true);
        setUserRoomSynchronisationDaemonLastRunTime(Instant.now());

        if (StringUtils.isEmpty(synapseAccessToken.getSessionAccessToken()) || StringUtils.isEmpty(matrixAccessToken.getSessionAccessToken())) {
            getLogger().debug(".userRoomSynchronisationDaemon(): Exit, access tokens not yet set");
            setUserRoomSynchronisationDaemonIsStillRunning(false);
            return;
        }

        boolean isInitialStartupPeriod = (Instant.now().getEpochSecond() - getStartupTime().getEpochSecond()) < getRoomInitialCheckPeriod();

        try {
            //
            // Synchronise the User Set
            List<SynapseUser> userList = synapseUserAPI.getALLAccounts();
            getLogger().debug(".userRoomSynchronisationDaemon(): [Auto Join Users to Added Rooms] Start...");
            for (SynapseUser currentUser : userList) {
                MatrixUser matrixUser = new MatrixUser(currentUser);
                getUserCache().addMatrixUser(matrixUser);
            }
        } catch (Exception ex){
            getLogger().warn(".userRoomSynchronisationDaemon(): Problem Synchronising User Set (between Synapse and Local Cache), message->{}", ExceptionUtils.getMessage(ex));
        }

        try {
            //
            // Add new users to the known room set
            Set<MatrixUser> recentAddedUsers = userCache.getRecentAddedUsers();
            if (!recentAddedUsers.isEmpty() || isInitialStartupPeriod) {
                getLogger().debug(".userRoomSynchronisationDaemon(): [New Users Added] Start");
                userTasks.addUsersToAllRooms(recentAddedUsers);
                itopsConsoleLogger.logConsoleEvent("Joining recently discovered/added users to ITOps Rooms");
                getLogger().debug(".userRoomSynchronisationDaemon(): [New Users Added] Finish");
            }
        } catch (Exception ex){
            getLogger().warn(".userRoomSynchronisationDaemon(): Problem Joining Added Users to Rooms, message->{}", ExceptionUtils.getMessage(ex));
        }

        try{
            //
            // Check to see if Rooms were added and Add Users to them
            Set<MatrixRoom> addedRoomSet = getRoomCache().getRecentlyAddedRooms();
            if(!addedRoomSet.isEmpty() || isInitialStartupPeriod){
                getLogger().debug(".userRoomSynchronisationDaemon(): [New Rooms Added] Start");
                userTasks.addAllUsersToRoomSet(addedRoomSet);
                itopsConsoleLogger.logConsoleEvent("Joining users to recently discovered/added ITOps Rooms");
                getLogger().debug(".userRoomSynchronisationDaemon(): [New Rooms Added] Finish");
            }
        } catch (Exception ex){
            getLogger().warn(".userRoomSynchronisationDaemon(): Problem Adding User to New Rooms, message->{}", ExceptionUtils.getMessage(ex));
        }

        try {
            //
            // Add All Users to All Rooms (if required)
            Long secondsSinceLastFullUserUpdate = Instant.now().getEpochSecond() - this.lastFullUserUpdate.getEpochSecond();
            if (secondsSinceLastFullUserUpdate > this.USER_SYNCHRONISATION_OVERRIDE_PERIOD) {
                getLogger().debug(".userRoomSynchronisationDaemon(): [Full User/Room Remap] Start");
                userTasks.joinAllUsersToAllRooms();
                this.lastFullUserUpdate = Instant.now();
                itopsConsoleLogger.logConsoleEvent("Doing a full synchronisation of Users/ITOps-Rooms");
                getLogger().debug(".userRoomSynchronisationDaemon(): [Full User/Room Remap] Finish");
            }
        } catch (Exception ex){
            getLogger().warn(".userRoomSynchronisationDaemon(): Problem Performing User/Room Remap, message->{}", ExceptionUtils.getMessage(ex));
        }

        //
        // We're Done
        setUserRoomSynchronisationDaemonIsStillRunning(false);

        getLogger().debug(".userRoomSynchronisationDaemon(): Exit");
    }


    //
    // Topology Synchronisation Task
    //

    private void topologyReplicationSynchronisationDaemon() {
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): Entry");

        systemWideTopologyMap.printMap();

        topologySynchronisationDaemonIsStillRunning = true;
        this.topologySynchronisationDaemonLastRunTime = Instant.now();

        List<SynapseRoom> roomList = new ArrayList<>();

        boolean isInitialStartupPeriod = (Instant.now().getEpochSecond() - getStartupTime().getEpochSecond()) < getRoomInitialCheckPeriod();

        //
        // 1st, do check of the Synapse/Matrix-Application-Service Connection
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Application-Services Connection Initialisation] Start");
        try {
            if (!serverConnectionInitialisation.isConnectionEstablished()) {
                serverConnectionInitialisation.initialiseConnection();
            }
        } catch (Exception ex){
            getLogger().error(".topologyReplicationSynchronisationDaemon(): Failure to initialise Application-Services Connection, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Application-Services Connection Initialisation] Finish");

        //
        // 2nd, Synchronise Participant List
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Start...");
        try {
            itopsSubsystemParticipantTasks.updateParticipantListUsingReportedTopology();
        } catch (Exception ex) {
            getLogger().error(".topologyReplicationSynchronisationDaemon(): Failure to synchronise participant list, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Finish...");

        //
        // Check to See if Activity/Updates needed
        boolean shouldDoFullRoomSynchronisation = false;
        Set<String> foundParticipants = participantMapDM.getRecentlyDiscoveredParticipants();
        Long ageSinceRun = Instant.now().getEpochSecond() - getLastFullRoomUpdate().getEpochSecond();
        boolean doRegularCheck = ageSinceRun > getRoomCompleteSynchronisationPeriod();
        boolean updatedTopology = !foundParticipants.isEmpty();
        if(doRegularCheck){
            itopsConsoleLogger.logConsoleEvent("Doing a full synchronisation ITOps-Rooms: Periodic Refresh");
        }
        if(updatedTopology){
            itopsConsoleLogger.logConsoleEvent("Doing a full synchronisation ITOps-Rooms: Topology Update");
        }
        if(isInitialStartupPeriod){
            itopsConsoleLogger.logConsoleEvent("Doing a full synchronisation ITOps-Rooms: Initial Check Period");
        }
        if (doRegularCheck || updatedTopology || isInitialStartupPeriod) {
            shouldDoFullRoomSynchronisation = true;
            this.lastFullRoomUpdate = Instant.now();
        }

        //
        // 3rd, Perform Synchronisation of Room List (from Synapse --> Cache)
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Room Set Between Synapse and Local Cache] Start...");
        if(shouldDoFullRoomSynchronisation) {
            try {
                getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Room Set (From Synapse --> Cache)] Start...");
                matrixCacheSynchronisationTasks.synchroniseMatrixIntoLocalCache();
                getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Room Set (From Synapse --> Cache)] Finish...");
            } catch (Exception ex){
                getLogger().error(".topologyReplicationSynchronisationDaemon(): Error Synchronising Room List From Synapse with Cache, error->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            }
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Room Set Between Synapse and Local Cache] Finish...");

        //
        // 4th, Perform SpaceTree/Room Synchronisation
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Build SpaceTrees and Synchronise with Known Rooms] Start...");
        if(shouldDoFullRoomSynchronisation) {
            try {
                List<ProcessingPlantSummary> processingPlants = getSystemWideTopologyMap().getProcessingPlants();
                for (ProcessingPlantSummary currentProcessingPlant : processingPlants) {
                    getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Build SpaceTrees and Synchronise with Known Rooms] Processing ->{}", currentProcessingPlant.getParticipantName());
                    getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Build SpaceTrees and Synchronise with Known Rooms] Getting Space Tree!");
                    MatrixRoom subsystemParticipantSpace = matrixCacheSynchronisationTasks.getSpaceTreeForSubsystemParticipant(currentProcessingPlant.getParticipantName());
                    getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Build SpaceTrees and Synchronise with Known Rooms] Syncing Space Tree with Room Cache!");
                    synchroniseSpaceTreeRoomsWithCache(subsystemParticipantSpace);
                    getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Build SpaceTrees and Synchronise with Known Rooms] Synchronised, subsystemParticipantSpace->{}", subsystemParticipantSpace);
                }
            } catch (Exception ex){
                getLogger().error(".topologyReplicationSynchronisationDaemon(): Error processing ProcessingPlant Space Trees, error->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            }
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Build SpaceTrees and Synchronise with Known Rooms] Finish...");

        //
        // 5th, Adding Subsystem Space(s) If Required
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Add Space(s) & Rooms As Required] Start...");
        if(shouldDoFullRoomSynchronisation) {
            try {
                List<ProcessingPlantSummary> processingPlants = getSystemWideTopologyMap().getProcessingPlants();
                for (ProcessingPlantSummary currentProcessingPlant : processingPlants) {
                    getLogger().trace(".topologyReplicationSynchronisationDaemon(): [Add Space(s) & Rooms As Required] Processing ->{}", currentProcessingPlant.getParticipantName());
                    String participantName = currentProcessingPlant.getParticipantName();
                    String pseudoAlias = roomIdentityFactory.buildProcessingPlantSpacePseudoAlias(participantName);
                    MatrixRoom subsystemParticipantSpace = getRoomCache().getRoomFromPseudoAlias(pseudoAlias);
                    getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Add Space(s) & Rooms As Required] subsystemParticipantSpace ->{}", subsystemParticipantSpace);
                    matrixCacheSynchronisationTasks.createParticipantSpacesAndRoomsIfNotThere(currentProcessingPlant, subsystemParticipantSpace);
                }
            } catch (Exception ex) {
                getLogger().error(".topologyReplicationSynchronisationDaemon(): Failure to Add Spaces/Rooms to Synapse, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            }
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Add Space(s) As Required & Rooms As Required] Finish...");

        topologySynchronisationDaemonIsStillRunning = false;

        getLogger().debug(".topologyReplicationSynchronisationDaemon(): Exit");
    }

    //
    // Helper
    //

    public void synchroniseSpaceTreeRoomsWithCache(MatrixRoom spaceTreeRoot){
        getLogger().debug(".synchroniseSpaceTreeRoomsWithCache(): Entry, spaceTreeRoot->{}", spaceTreeRoot);
        if(spaceTreeRoot == null){
            return;
        }
        MatrixRoom cachedRoom = getRoomCache().getRoomFromRoomId(spaceTreeRoot.getRoomID());
        ArrayList<MatrixRoom> currentChildRooms = new ArrayList<>();
        currentChildRooms.addAll(spaceTreeRoot.getContainedRooms());
        if(cachedRoom == null){
            getRoomCache().addRoom(spaceTreeRoot);
            for(MatrixRoom currentChild: currentChildRooms){
                synchroniseSpaceTreeRoomsWithCache(currentChild);
            }
        } else {
            cachedRoom.getContainedRoomIds().clear();
            cachedRoom.getContainedRoomIds().addAll(spaceTreeRoot.getContainedRoomIds());
            for (MatrixRoom currentChild : currentChildRooms) {
                MatrixRoom currentCachedContainedRoom = getRoomCache().getRoomFromRoomId(currentChild.getRoomID());
                if (currentCachedContainedRoom == null) {
                    getRoomCache().addRoom(currentChild);
                    currentCachedContainedRoom = currentChild;
                } else {
                    for (MatrixRoom existingContainedRoomEntry : spaceTreeRoot.getContainedRooms()) {
                        if (existingContainedRoomEntry.getRoomID().contentEquals(currentCachedContainedRoom.getRoomID())) {
                            spaceTreeRoot.getContainedRooms().remove(existingContainedRoomEntry);
                            spaceTreeRoot.addChildRoom(currentCachedContainedRoom);
                            for (MatrixRoom containedRoomInSubRoom : existingContainedRoomEntry.getContainedRooms()) {
                                getLogger().debug(".synchroniseSpaceTreeRoomsWithCache(): Adding Child->{}", containedRoomInSubRoom.getCanonicalAlias());
                                currentCachedContainedRoom.addChildRoom(containedRoomInSubRoom);
                            }
                            break;
                        }
                    }

                }
                synchroniseSpaceTreeRoomsWithCache(currentCachedContainedRoom);
            }
        }
        getLogger().debug(".synchroniseSpaceTreeRoomsWithCache(): Exit");
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
}
