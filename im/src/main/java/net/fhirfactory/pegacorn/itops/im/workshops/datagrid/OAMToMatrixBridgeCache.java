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
package net.fhirfactory.pegacorn.itops.im.workshops.datagrid;

import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseUser;
import net.fhirfactory.pegacorn.core.model.petasos.oam.topology.valuesets.PetasosMonitoredComponentTypeEnum;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.PetasosParticipantSummary;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.factories.common.ParticipantRoomIdentityFactory;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class OAMToMatrixBridgeCache {
    private static final Logger LOG = LoggerFactory.getLogger(OAMToMatrixBridgeCache.class);

    // ConcurrentHashMap<participantName, PetasosParticipantSummary>
    private ConcurrentHashMap<String, PetasosParticipantSummary> participantMap;

    // ConcurrentHashMap<spaceId, participantName>
    private ConcurrentHashMap<String, String> spaceIdToParticipantIdNameMap;

    // ConcurrentHashMap<roomId, participantName>
    private ConcurrentHashMap<String, String> roomIdToParticipantIdNameMap;

    // ConcurrentHashMap<spaceId, SynapseRoom>
    private ConcurrentHashMap<String, SynapseRoom> matrixSpaceMap;

    // ConcurrentHashMap<roomId, SynapseRoom>
    private ConcurrentHashMap<String, SynapseRoom> matrixRoomMap;

    // ConcurrentHashMap<spaceId, List<roomId>>
    private ConcurrentHashMap<String, List<String>> spaceMembershipMap;

    // ConcurrentHashMap<roomAlias, roomId> (not the Canonical Alias Id, the Alias which is in the Id)
    private ConcurrentHashMap<String, String> aliasMap;


    private ConcurrentHashMap<String, SynapseRoom> knownRoomSet;
    private ConcurrentHashMap<String, SynapseUser> knownUserSet;


    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    //
    // Constructor(s)
    //

    public OAMToMatrixBridgeCache(){
        this.participantMap = new ConcurrentHashMap<>();
        this.spaceIdToParticipantIdNameMap = new ConcurrentHashMap<>();
        this.matrixRoomMap = new ConcurrentHashMap<>();
        this.spaceMembershipMap = new ConcurrentHashMap<>();
        this.roomIdToParticipantIdNameMap = new ConcurrentHashMap<>();
        this.matrixSpaceMap = new ConcurrentHashMap<>();
        this.aliasMap = new ConcurrentHashMap<>();
        this.knownRoomSet = new ConcurrentHashMap<>();
        this.knownUserSet = new ConcurrentHashMap<>();
    }

    //
    // Getters (and Setters)
    //

    public ConcurrentHashMap<String, SynapseRoom> getKnownRoomSet() {
        return knownRoomSet;
    }

    public ConcurrentHashMap<String, SynapseUser> getKnownUserSet() {
        return knownUserSet;
    }

    protected ConcurrentHashMap<String, PetasosParticipantSummary> getParticipantMap(){
        return(participantMap);
    }

    protected ConcurrentHashMap<String, String> getSpaceIdToParticipantIdNameMap() {
        return spaceIdToParticipantIdNameMap;
    }

    protected ConcurrentHashMap<String, SynapseRoom> getMatrixRoomMap() {
        return matrixRoomMap;
    }

    protected ConcurrentHashMap<String, List<String>> getSpaceMembershipMap() {
        return spaceMembershipMap;
    }

    protected ConcurrentHashMap<String, String> getRoomIdToParticipantIdNameMap() {
        return roomIdToParticipantIdNameMap;
    }

    protected ConcurrentHashMap<String, SynapseRoom> getMatrixSpaceMap() {
        return matrixSpaceMap;
    }

    protected ConcurrentHashMap<String, String> getAliasMap(){
        return(this.aliasMap);
    }

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Methods
    //

    public void addRoomFromMatrix(SynapseRoom synapseRoom){
        getLogger().debug(".addRoomFromMatrix(): Entry, synapseRoom->{}", synapseRoom);
        if(synapseRoom == null){
            getLogger().debug(".addRoomFromMatrix(): Exit, synapseRoom is null");
        }

        boolean storeAsSpace = false;
        boolean storeAsRoom = false;

        String alias = synapseRoom.getCanonicalAlias();
        if(StringUtils.isNotEmpty(alias)){
            String testAlias = alias.substring(1); // the alias returned to us from synapse has a "!" at the beginning
            OAMRoomTypeEnum oamRoomType = OAMRoomTypeEnum.fromAliasPrefix(testAlias);
            if(oamRoomType != null){
                if(oamRoomType.equals(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM)) {
                    getLogger().info(".addRoomFromMatrix(): Room is a Subsystem Space");
                    storeAsSpace = true;
                }
                if(oamRoomType.equals(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP)){
                    getLogger().info(".addRoomFromMatrix(): Room is a WUP Room");
                    storeAsRoom = true;
                }
            }
        }
        if(storeAsSpace){
            getLogger().debug(".addRoomFromMatrix(): Adding synapseRoom to matrixSpaceMap");
            if(getMatrixSpaceMap().containsKey(synapseRoom.getRoomID())){
                getMatrixSpaceMap().remove(synapseRoom.getRoomID());
            }
            getMatrixSpaceMap().put(synapseRoom.getRoomID(), synapseRoom);

        }
        if(storeAsRoom){
            getLogger().debug("addRoomFromMatrix(): Adding synapseRoom to matrixRoomMap");
            if(getMatrixRoomMap().containsKey(synapseRoom.getRoomID())){
                getMatrixRoomMap().remove(synapseRoom.getRoomID());
            }
            getMatrixRoomMap().put(synapseRoom.getRoomID(), synapseRoom);
        }

        //
        // Now add mapping to Participant

        if(storeAsSpace){
            getLogger().debug("addRoomFromMatrix(): Adding synapseRoom to spaceNameToParticipantIdNameMap");
            if(getSpaceIdToParticipantIdNameMap().containsKey(synapseRoom.getRoomID())){
                getSpaceIdToParticipantIdNameMap().remove(synapseRoom.getRoomID());
            }
            getSpaceIdToParticipantIdNameMap().put(synapseRoom.getRoomID(), synapseRoom.getName());
        }
        if(storeAsRoom){
            getLogger().debug("addRoomFromMatrix(): Adding synapseRoom to roomNameToParticipantIdNameMap");
            if(getRoomIdToParticipantIdNameMap().containsKey(synapseRoom.getRoomID())){
                getRoomIdToParticipantIdNameMap().remove(synapseRoom.getRoomID());
            }
            getRoomIdToParticipantIdNameMap().put(synapseRoom.getRoomID(), synapseRoom.getName());
        }

        //
        // Now update the Alias Map

        if(synapseRoom.getCanonicalAlias() != null){
            String roomAlias = getAliasFromAliasId(synapseRoom.getCanonicalAlias());
            if(getAliasMap().containsKey(roomAlias)) {
                getAliasMap().remove(roomAlias);
            }
            getAliasMap().put(roomAlias, synapseRoom.getRoomID());
        }
        getLogger().debug("addRoomFromMatrix(): Exit");
    }

    public void addParticipant(PetasosParticipantSummary participant){
        getLogger().debug(".addParticipant(): Entry, participant->{}", participant);
        if(participant == null){
            getLogger().debug(".addParticipant(): Exit, participant is null");
        }
        if(getParticipantMap().containsKey(participant.getParticipantName())){
            participantMap.remove(participant.getParticipantName());
        }
        getParticipantMap().put(participant.getParticipantName(), participant);
        getLogger().debug(".addParticipant(): Exit");
    }

    public PetasosParticipantSummary getParticipant(String participantName){
        getLogger().debug(".getParticipant(): Entry, participantName->{}", participantName);
        if(StringUtils.isEmpty(participantName)){
            getLogger().debug(".getParticipant(): Exit, participantName is null");
            return(null);
        }
        PetasosParticipantSummary petasosParticipantSummary = getParticipantMap().get(participantName);
        getLogger().debug("getParticipant(): Exit petasosParticipantSummary->{}", petasosParticipantSummary);
        return(petasosParticipantSummary);
    }

    public List<String> getSpaceNameSet(){
        getLogger().debug(".getSpaceNameSet(): Entry");
        List<String> spaceNameSet = new ArrayList<>();
        Enumeration<String> keys = getMatrixSpaceMap().keys();
        while(keys.hasMoreElements()){
            SynapseRoom synapseRoom = getMatrixSpaceMap().get(keys.nextElement());
            spaceNameSet.add(synapseRoom.getName());
        }
        getLogger().debug(".getSpaceNameSet(): Exit");
        return(spaceNameSet);
    }

    public List<String> getSubsystemParticipantNameSet(){
        getLogger().debug(".getSubsystemParticipantNameSet(): Entry");
        List<String> subsystemNameSet = new ArrayList<>();
        Enumeration<String> keys = getParticipantMap().keys();
        while(keys.hasMoreElements()){
            String currentParticipantName = keys.nextElement();
            PetasosParticipantSummary participant = getParticipant(currentParticipantName);
            if(participant.getNodeType().equals(PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_PROCESSING_PLANT)){
                subsystemNameSet.add(currentParticipantName);
            }
        }
        getLogger().debug(".getSubsystemParticipantNameSet(): Exit");
        return(subsystemNameSet);
    }

    public String getSpaceIdForParticipant(String participantName){
        getLogger().debug(".getSpaceIdForParticipant(): Entry, participantName->{}", participantName);
        Enumeration<String> keys = getSpaceIdToParticipantIdNameMap().keys();
        String spaceId = null;
        while(keys.hasMoreElements()){
            String currentSpaceId = keys.nextElement();
            String currentParticipantName = getSpaceIdToParticipantIdNameMap().get(currentSpaceId);
            if(currentParticipantName.equals(participantName)){
                spaceId = currentSpaceId;
                break;
            }
        }
        getLogger().debug(".getSpaceIdForParticipant(): Exit, spaceId->{}", spaceId);
        return(spaceId);
    }

    public String getRoomIdFromAlias(String alias){
        getLogger().debug(".getRoomIdFromAlias(): Entry, alias->{}", alias);

        if(StringUtils.isEmpty(alias)){
            getLogger().debug(".getRoomIdFromAlias(): Exit, alias is empty");
            return(null);
        }

        String roomId = null;
        if(getAliasMap().containsKey(alias)){
            roomId = getAliasMap().get(alias);
        }

        getLogger().debug(".getRoomIdFromAlias(): Exit, roomId->{}", roomId);
        return(roomId);
    }

    public String getRoomIdFromAliasId(String aliasId) {
        getLogger().debug(".getRoomIdFromAliasId(): Entry, aliasId->{}", aliasId);
        String alias = getAliasFromAliasId(aliasId);
        String roomIdFromAlias = getRoomIdFromAlias(alias);
        return(roomIdFromAlias);
    }

    private String getAliasFromAliasId(String aliasId){
        if(StringUtils.isEmpty(aliasId)){
            return(null);
        }
        String clonedAliasId = SerializationUtils.clone(aliasId);
        String aliasIdWithoutFirstChar = clonedAliasId.substring(1);
        String[] aliasSplit = aliasIdWithoutFirstChar.split(":");
        String alias = aliasSplit[0];
        return(alias);
    }


    public  List<String> extractRoomAliasListWithServer (List < SynapseRoom > roomList) {
        getLogger().info(".extractRoomAliasListWithServer(): Entry");
        List<String> roomAliasList = new ArrayList<>();
        for (SynapseRoom currentRoom : roomList) {
            String roomAlias = currentRoom.getCanonicalAlias();
            getLogger().info(".extractRoomAliasListWithServer(): processing roomAlias->{}", roomAlias);
            if (StringUtils.isNotEmpty(roomAlias)) {
                String clonedRoomAlias = SerializationUtils.clone(roomAlias);
                String[] split = roomAlias.split(":");
                String firstPart = split[0];
                String aliasOfInterest = firstPart.substring(1);
                roomAliasList.add(aliasOfInterest);
            }
        }
        getLogger().info(".extractRoomAliasListWithServer(): Exit");
        return (roomAliasList);
    }

    public SynapseRoom scanForExistingRoomWithAlias (List < SynapseRoom > roomList, String alias){
        if (roomList.isEmpty()) {
            return (null);
        }
        if (StringUtils.isEmpty(alias)) {
            return (null);
        }
        for (SynapseRoom currentRoom : roomList) {
            if (StringUtils.isNotEmpty(currentRoom.getCanonicalAlias())) {
                String roomAlias = SerializationUtils.clone(currentRoom.getCanonicalAlias()).toLowerCase(Locale.ROOT);
                String lowerCaseAlias = SerializationUtils.clone(alias).toLowerCase(Locale.ROOT);
                if (roomAlias.contains(lowerCaseAlias)) {
                    return (currentRoom);
                }
            }
        }
        return (null);
    }
}
