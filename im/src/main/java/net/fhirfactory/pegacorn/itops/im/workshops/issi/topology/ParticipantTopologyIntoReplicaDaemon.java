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
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixApplicationServiceMethods;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixRoomMethods;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixSpaceMethods;
import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixRoom;
import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixUser;
import net.fhirfactory.pegacorn.communicate.matrixbridge.workshops.matrixbridge.common.SynapseServerConnectionInitialisation;
import net.fhirfactory.pegacorn.communicate.synapse.credentials.SynapseAdminAccessToken;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseRoomMethods;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseUserMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseUser;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.ProcessingPlantSummary;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownUserMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsSystemWideReportedTopologyMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownRoomAndSpaceMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.common.ITOpsRoomHelpers;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.EndpointParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.ProcessingPlantParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.WorkUnitProcessorParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories.WorkshopParticipantReplicaFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.tasks.ITOpsSubsystemParticipantTasks;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.ParticipantRoomIdentityFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.topology.ParticipantTopologyIntoReplicaFactory;
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

    private boolean daemonIsStillRunning;
    private Instant daemonLastRunTime;

    private Instant lastFullUserUpdate;

    private Long ROOM_SYNCHRONISATION_WATCHDOG_STARTUP_DELAY = 60000L;
    private Long USER_SYNCHRONISATION_OVERRIDE_PERIOD = 900L; // Seconds
    private Long ROOM_SYNCHRONISATION_WATCHDOG_CHECK_PERIOD = 30000L;
    private Long ROOM_SYNCHRONISATION_WATCHDOG_RESET_PERIOD = 900L;

    private Long SHORT_GAPPING_PERIOD = 100L;
    private Long LONG_GAPPING_PERIOD = 1000L;

    @Inject
    private SynapseServerConnectionInitialisation serverConnectionInitialisation;

    @Inject
    private ITOpsRoomHelpers itopsRoomHelpers;

    @Inject
    private ITOpsSubsystemParticipantTasks itopsSubsystemParticipantTasks;

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

    @Inject
    private SynapseUserMethods synapseUserAPI;

    @Inject
    private ITOpsKnownRoomAndSpaceMapDM roomCache;

    @Inject
    private ITOpsKnownUserMapDM userCache;

    @Inject
    private ITOpsSystemWideReportedTopologyMapDM systemWideTopologyMap;

    @Inject
    private ParticipantTopologyIntoReplicaFactory matrixBridgeFactories;

    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    @Inject
    private WorkUnitProcessorParticipantReplicaFactory wupReplicaServices;

    @Inject
    private WorkshopParticipantReplicaFactory workshopReplicaServices;

    @Inject
    private ProcessingPlantParticipantReplicaFactory processingPlantReplicaServices;

    @Inject
    private EndpointParticipantReplicaFactory endpointReplicaServices;

    @Inject
    private MatrixApplicationServiceMethods matrixApplicationServiceMethods;

    //
    // Constructor(s)
    //

    public ParticipantTopologyIntoReplicaDaemon() {
        super();
        this.initialised = false;
        this.firstRunComplete = false;
        this.daemonIsStillRunning = false;
        this.daemonLastRunTime = Instant.EPOCH;
        this.lastFullUserUpdate = Instant.EPOCH;
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

    protected Instant getDaemonLastRunTime() {
        return (daemonLastRunTime);
    }

    protected void setDaemonLastRunTime(Instant instant){
        this.daemonLastRunTime = instant;
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

    protected WorkshopParticipantReplicaFactory getWorkshopReplicaServices(){
        return(this.workshopReplicaServices);
    }

    protected ProcessingPlantParticipantReplicaFactory getProcessingPlantReplicaServices(){
        return(this.processingPlantReplicaServices);
    }

    protected EndpointParticipantReplicaFactory getEndpointReplicaServices(){
        return(this.endpointReplicaServices);
    }

    protected MatrixAccessToken getMatrixAccessToken() {
        return (matrixAccessToken);
    }

    protected ITOpsSystemWideReportedTopologyMapDM getSystemWideTopologyMap(){
        return(this.systemWideTopologyMap);
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
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): Entry");

        systemWideTopologyMap.printMap();

        daemonIsStillRunning = true;
        this.daemonLastRunTime = Instant.now();

        List<SynapseRoom> roomList = new ArrayList<>();

        //
        // 1st, do check of the Synapse/Matrix-Application-Service Connection
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Application-Services Connection Initialisation] Start");
        try {
            if (!serverConnectionInitialisation.isConnectionEstablished()) {
                serverConnectionInitialisation.initialiseConnection();
            }
        } catch (Exception ex){
            getLogger().error(".topologyReplicationSynchronisationDaemon(): Failure to initialise Application-Services Connection, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return;
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Application-Services Connection Initialisation] Finish");

        // 2nd, Synchronise Existing Room List
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] Start...");
        try {
            roomList = synapseRoomAPI.getRooms("*");
            getLogger().trace(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] RoomList->{}", roomList);
            // remove if not available
            Set<MatrixRoom> knownRooms = roomCache.getFullRoomSet();
            for(MatrixRoom currentKnownRoom: knownRooms){
                boolean found = false;
                for(SynapseRoom currentRoom: roomList){
                    if(currentRoom.getRoomID().contentEquals(currentKnownRoom.getRoomID())){
                        found = true;
                        break;
                    }
                }
                if(!found){
                    roomCache.deleteRoom(currentKnownRoom.getRoomID());
                }
            }
            // add if absent
            for (SynapseRoom currentRoom : roomList) {
                getLogger().info(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] Processing Room ->{}", currentRoom);
                MatrixRoom matrixRoom = new MatrixRoom(currentRoom);
                roomCache.addRoom(matrixRoom);
            }
        } catch(Exception ex){
            getLogger().warn(".topologyReplicationSynchronisationDaemon(): Failure to synchronise room list, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return;
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Room List] Finish...");

        // 3rd, Synchronise Participant List
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Start...");
        try {
            itopsSubsystemParticipantTasks.updateParticipantListUsingReportedMetrics();
        } catch(Exception ex){
            getLogger().error(".topologyReplicationSynchronisationDaemon(): Failure to synchronise participant list, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return;
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Finish...");

        // 4th, Adding Subsystem Space(s) If Required
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Add Space(s) & Rooms As Required] Start...");
        try {
            List<ProcessingPlantSummary> processingPlants = getSystemWideTopologyMap().getProcessingPlants();
            for (ProcessingPlantSummary currentProcessingPlant : processingPlants) {
                getLogger().info(".topologyReplicationSynchronisationDaemon(): [Add Space(s) & Rooms As Required] Processing ->{}", currentProcessingPlant.getParticipantName());
                MatrixRoom subsystemParticipantSpace = itopsSubsystemParticipantTasks.getSpaceRoomSetForSubsystemParticipant(currentProcessingPlant.getParticipantName());
                getLogger().info(".topologyReplicationSynchronisationDaemon(): [Add Space(s) & Rooms As Required] subsystemParticipantSpace ->{}", subsystemParticipantSpace);
                itopsSubsystemParticipantTasks.createParticipantSpacesAndRoomsIfNotThere(currentProcessingPlant, subsystemParticipantSpace);
            }
        } catch(Exception ex){
            getLogger().error(".topologyReplicationSynchronisationDaemon(): Failure to Add Spaces/Rooms to Synapse, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return;
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Add Space(s) As Required & Rooms As Required] Finish...");

        //
        // Check to see if Rooms were added
        Set<MatrixRoom> addedRoomSet = getRoomCache().getRecentlyAddedRooms();

        //
        // Add all users to the new rooms
        List<SynapseUser> userList = synapseUserAPI.getALLAccounts();
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Auto Join Users to Added Rooms] Start...");
        for (SynapseUser currentUser : userList) {
            MatrixUser matrixUser = new MatrixUser(currentUser);
            getUserCache().addMatrixUser(matrixUser);
        }
        try {
            for (SynapseRoom currentRoom : addedRoomSet) {
                String currentRoomAlias = currentRoom.getCanonicalAlias();
                if (StringUtils.isNotEmpty(currentRoomAlias)) {
                    if (itopsRoomHelpers.isAnITOpsRoom(currentRoomAlias)) {
                        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [AAuto Join Users to Added Rooms] Processing Space->{}", currentRoomAlias);
                        String roomId = currentRoom.getRoomID();
                        List<String> roomMembers = synapseRoomAPI.getRoomMembers(roomId);
                        for (SynapseUser currentUser : userList) {
                            if (roomMembers.contains(currentUser) || currentUser.getName().contains(matrixAccessToken.getUserName()) || currentUser.getName().startsWith("@"+synapseAccessToken.getUserName())) {
                                // do nothing
                            } else {
                                getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Auto Join Users to Added Rooms] Processing User->{}", currentUser.getName());
                                synapseRoomAPI.addRoomMember(roomId, currentUser.getName());
                            }
                        }
                    }
                }
            }
        } catch (Exception ex){
            getLogger().warn(".topologyReplicationSynchronisationDaemon(): Failure to Add Users to New Spaces/Rooms, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return;
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Auto Join Users to Added Rooms] Finish...");

        //
        // See if there are new Users
        Set<MatrixUser> addedUserSet = getUserCache().getRecentAddedUsers();

        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Auto Join New Users to the Older Rooms] Start...");
        try {
            for (MatrixRoom currentRoom : getRoomCache().getFullRoomSet()) {
                String currentRoomAlias = currentRoom.getCanonicalAlias();
                if (StringUtils.isNotEmpty(currentRoomAlias)) {
                    if (itopsRoomHelpers.isAnITOpsRoom(currentRoomAlias)) {

                        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [uto Join New Users to the Older Rooms] Processing Room/Space->{}", currentRoomAlias);
                        String roomId = currentRoom.getRoomID();
                        for (SynapseUser currentUser : addedUserSet) {
                            if (currentUser.getName().contains(matrixAccessToken.getUserName()) || currentUser.getName().startsWith("@"+synapseAccessToken.getUserName())) {
                                // do nothing
                            } else {
                                getLogger().info(".topologyReplicationSynchronisationDaemon(): [uto Join New Users to the Older Rooms] Processing User->{}", currentUser.getName());
                                synapseRoomAPI.addRoomMember(roomId, currentUser.getName());
                            }
                        }
                    }
                }
            }
        } catch(Exception ex){
            getLogger().warn(".topologyReplicationSynchronisationDaemon(): Failure to add New Users to Spaces/Rooms, message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return;
        }
        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Auto Join New Users to the Older Rooms] Finished...");

        Long secondsSinceLastFullUserUpdate = Instant.now().getEpochSecond() - this.lastFullUserUpdate.getEpochSecond();
        Set<MatrixRoom> fullRoomSet = getRoomCache().getFullRoomSet();
        Set<MatrixUser> fullUserSet = getUserCache().getKnownUsers();
        if(secondsSinceLastFullUserUpdate > this.USER_SYNCHRONISATION_OVERRIDE_PERIOD){
            for (MatrixRoom currentRoom : fullRoomSet) {
                String currentRoomAlias = currentRoom.getCanonicalAlias();
                if (StringUtils.isNotEmpty(currentRoomAlias)) {
                    if (itopsRoomHelpers.isAnITOpsRoom(currentRoomAlias)) {
                        getLogger().debug(".topologyReplicationSynchronisationDaemon(): [uto Join New Users to the Older Rooms] Processing Room/Space->{}", currentRoomAlias);
                        String roomId = currentRoom.getRoomID();
                        for (MatrixUser currentUser : fullUserSet) {
                            if (currentUser.getName().contains(matrixAccessToken.getUserName()) || currentUser.getName().startsWith("@"+synapseAccessToken.getUserName())) {
                                // do nothing
                            } else {
                                getLogger().trace(".topologyReplicationSynchronisationDaemon(): [uto Join New Users to the Older Rooms] Processing User->{}", currentUser.getName());
                                synapseRoomAPI.addRoomMember(roomId, currentUser.getName());
                            }
                        }
                    }
                }
            }
            this.lastFullUserUpdate = Instant.now();
        }

        daemonIsStillRunning = false;

        getLogger().debug(".topologyReplicationSynchronisationDaemon(): Exit");
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
