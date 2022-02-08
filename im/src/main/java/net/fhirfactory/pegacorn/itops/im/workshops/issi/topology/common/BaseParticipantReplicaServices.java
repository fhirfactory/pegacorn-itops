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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.common;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixRoomMethods;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixSpaceMethods;
import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixRoom;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomCreation;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomPresetEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomVisibilityEnum;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseRoomMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownRoomAndSpaceMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.ParticipantRoomIdentityFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.topology.ParticipantTopologyIntoReplicaFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

public abstract class BaseParticipantReplicaServices {
    private Long SHORT_GAPPING_PERIOD = 100L;
    private Long LONG_GAPPING_PERIOD = 1000L;

    @Inject
    private ITOpsKnownRoomAndSpaceMapDM roomCache;

    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    @Inject
    private ParticipantTopologyIntoReplicaFactory matrixBridgeFactories;

    @Inject
    private SynapseRoomMethods synapseRoomAPI;

    @Inject
    private MatrixRoomMethods matrixRoomAPI;

    @Inject
    private MatrixSpaceMethods matrixSpaceAPI;

    @Inject
    private MatrixAccessToken matrixAccessToken;


    //
    // Constructor(s)
    //

    //
    // Getters (and Setters)
    //

    abstract protected Logger getLogger();

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

    protected MatrixAccessToken getMatrixAccessToken() {
        return (matrixAccessToken);
    }

    //
    // Helper Methods
    //

    protected void waitALittleBit(){
        try {
            Thread.sleep(SHORT_GAPPING_PERIOD);
        } catch (Exception e) {
            getLogger().debug(".waitALittleBit():...{}, {}", ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
        }
    }

    protected void waitALittleBitLonger(){
        try {
            Thread.sleep(LONG_GAPPING_PERIOD);
        } catch (Exception e) {
            getLogger().debug(".waitALittleBitLonger():...{}, {}", ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
        }

    }

    protected void installAnOAMRoom(String participantName, String participantDisplayName, String participantSpaceId, OAMRoomTypeEnum roomType) {
        getLogger().debug(".installAnOAMRoom(): Entry, participantName->{}, participantDisplayName->{}, participantSpaceId->{}, roomType->{}",participantName,participantDisplayName, participantSpaceId,roomType);

        String roomAlias = getRoomIdentityFactory().buildOAMRoomAlias(participantName, roomType);
        getLogger().trace(".installAnOAMRoom(): roomAlias->{}", roomAlias);
        getLogger().trace(".installAnOAMRoom(): Room doesn't appear to exist, so creating it");
        String roomName = participantDisplayName + "." + roomType.getDisplayName();
        getLogger().trace(".installAnOAMRoom(): roomName->{}", roomName);
        String roomTopic = roomType.getDisplayName();
        getLogger().trace(".installAnOAMRoom(): roomName->{}", roomTopic);
        MRoomCreation mRoomCreation = getMatrixBridgeFactories().newRoomInSpaceCreationRequest(roomName, roomAlias, roomTopic, participantSpaceId, MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);
        getLogger().trace(".installAnOAMRoom(): mRoomCreation request->{}", mRoomCreation);
        SynapseRoom createdRoom = getMatrixRoomAPI().createRoom(getMatrixAccessToken().getUserId(), mRoomCreation);
        getLogger().trace(".installAnOAMRoom(): Created Room ->{}", createdRoom);
        if(createdRoom == null){
            createdRoom = getExistingRoom(roomAlias);
        }
        if(createdRoom != null) {
            getLogger().trace(".installAnOAMRoom(): adding as child to participantSpaceId");
            MatrixRoom matrixRoom = new MatrixRoom(createdRoom);
            getMatrixSpaceAPI().addChildToSpace(participantSpaceId, matrixRoom.getRoomID());
            getRoomCache().addRoom(matrixRoom);
        } else {
            getLogger().warn(".installAnOAMRoom(): Could not create room -> {}", roomAlias);
        }
        getLogger().debug(".installAnOAMRoom(): Exit");
    }


    protected MatrixRoom getExistingRoom(String pseudoAlias){
        MatrixRoom room = getRoomCache().getRoomFromPseudoAlias(pseudoAlias);
        return(room);
    }
}