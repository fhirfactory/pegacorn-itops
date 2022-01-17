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
package net.fhirfactory.pegacorn.itops.im.workshops.matrixbridge.topology;

import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixRoomMethods;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixSpaceMethods;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomCreation;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomPresetEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomVisibilityEnum;
import net.fhirfactory.pegacorn.communicate.synapse.credentials.SynapseAdminAccessToken;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseRoomMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.EndpointSummary;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.WorkUnitProcessorSummary;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.OAMToMatrixBridgeCache;
import net.fhirfactory.pegacorn.itops.im.workshops.matrixbridge.common.ParticipantRoomIdentityFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.matrixbridge.topology.common.BaseParticipantReplicaServices;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class WorkUnitProcessorParticipantReplicaServices extends BaseParticipantReplicaServices {
    private static final Logger LOG = LoggerFactory.getLogger(WorkUnitProcessorParticipantReplicaServices.class);

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

    public String createWorkUnitProcessorSpace(String workshopId, List<SynapseRoom> roomList, WorkUnitProcessorSummary wupSummary) {
        getLogger().info(".createWorkUnitProcessorSpace(): Entry, workshopId->{}, wup->{}", workshopId, wupSummary);
        String wupParticipantName = wupSummary.getParticipantName();
        String wupParticipantDisplayName = wupSummary.getParticipantDisplayName();
        boolean wupSpaceAlreadyExists = false;
        String wupAlias = OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP.getAliasPrefix() + wupParticipantName.toLowerCase(Locale.ROOT).replace(".", "-");
        String wupRoomId = null;
        SynapseRoom foundRoom = getMatrixBridgeCache().scanForExistingRoomWithAlias(roomList, wupAlias);
        if (foundRoom != null) {
            wupRoomId = foundRoom.getRoomID();
        } else {
            getLogger().info(".topologyReplicationSynchronisationDaemon(): [Add Space(s) For WUP As Required] Creating Space for WUP ->{}", wupAlias);
            String wupTopic = "WorkUnitProcessor, " + wupSummary.getComponentID().getId();
            MRoomCreation mRoomCreation = getMatrixBridgeFactories().newSpaceInSpaceCreationRequest(wupParticipantDisplayName, wupAlias, wupTopic, workshopId, MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);
            SynapseRoom createdRoom = getMatrixSpaceAPI().createSpace(getSynapseAccessToken().getUserName(), mRoomCreation);
            wupRoomId = createdRoom.getRoomID();
            getLogger().info(".topologyReplicationSynchronisationDaemon(): [Add Space(s) For WUP As Required] Created Space ->{}", createdRoom);
            getMatrixBridgeCache().addRoomFromMatrix(createdRoom);
            roomList.add(createdRoom);
            waitALittleBit();
        }
        //
        // TODO, Should check if it is already a child
        getMatrixSpaceAPI().addChildToSpace(workshopId, wupRoomId);
        waitALittleBit();
        getLogger().info(".createProcessingPlantSpace(): [Add Rooms If Required] Start...");
        installAnOAMRoom(wupParticipantName, wupParticipantDisplayName, wupRoomId, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_EVENTS,roomList);
        installAnOAMRoom(wupParticipantName, wupParticipantDisplayName, wupRoomId, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_METRICS, roomList);
        installAnOAMRoom(wupParticipantName, wupParticipantDisplayName, wupRoomId, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_SUBSCRIPTIONS, roomList);
        installAnOAMRoom(wupParticipantName, wupParticipantDisplayName, wupRoomId, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_TASKS, roomList);
        getLogger().info(".createProcessingPlantSpace(): [Add Rooms If Required] Finish...");

        getLogger().info(".createWorkUnitProcessorSpace(): Exit, wupRoomId->{}", wupRoomId);
        return(wupRoomId);
    }


}
