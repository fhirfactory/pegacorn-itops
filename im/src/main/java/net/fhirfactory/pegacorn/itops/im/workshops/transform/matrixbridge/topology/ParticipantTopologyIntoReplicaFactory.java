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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.datatypes.MCreationContent;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.datatypes.MStateEvent;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomCreation;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomPresetEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomVisibilityEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.contenttypes.MRoomPowerLevelsContentType;
import net.fhirfactory.pegacorn.core.model.componentid.ComponentIdType;
import net.fhirfactory.pegacorn.core.model.petasos.participant.PetasosParticipantFulfillment;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.PetasosParticipantSummary;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.ProcessingPlantSummary;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class ParticipantTopologyIntoReplicaFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantTopologyIntoReplicaFactory.class);

    private ObjectMapper jsonMapper;

    @Inject
    private MatrixAccessToken matrixAccessToken;

    //
    // Constructor(s)
    //

    public ParticipantTopologyIntoReplicaFactory(){
        jsonMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        jsonMapper.registerModule(module);
        jsonMapper.configure(JsonParser.Feature.ALLOW_MISSING_VALUES, true);
    }

    //
    // Business Methods
    //

    public MRoomCreation newSpaceCreationRequest(String spaceName, String spaceAlias, String topic, MRoomPresetEnum roomPreset, MRoomVisibilityEnum visibility ){
        getLogger().debug(".newSpaceCreationRequest(): Entry, spaceName->{}, spaceAlias->{}, topic->{}, roomPreset->{}, visibility->{}", spaceName, spaceAlias, topic, roomPreset, visibility);

        MRoomCreation roomCreationRequest = new MRoomCreation();
        roomCreationRequest.setName(spaceName);
        roomCreationRequest.setPreset(roomPreset.getRoomPreset());
        MRoomPowerLevelsContentType powerLevelsOverride = new MRoomPowerLevelsContentType();
        powerLevelsOverride.setEventDefaultPowerLevel(100);
        powerLevelsOverride.setInvite(0);
        roomCreationRequest.setPowerLevelContentOverride(powerLevelsOverride);
        roomCreationRequest.setRoomAliasName(spaceAlias);
        roomCreationRequest.setTopic(topic);
        roomCreationRequest.setVisibility(visibility.getRoomVisibility());
        MCreationContent creationContent = new MCreationContent();
        creationContent.setFederate(false);
        creationContent.setType("m.space");
        roomCreationRequest.setCreationContent(creationContent);
        //
        // Initial State Events
        MStateEvent guestAccessEvent = new MStateEvent();
        guestAccessEvent.setType("m.room.guest_access");
        JSONObject simpleObject = new JSONObject();
        simpleObject.put("guest_access", "can_join" );
        guestAccessEvent.setContent(simpleObject);
        ArrayList<MStateEvent> initialState = new ArrayList<>();
        initialState.add(guestAccessEvent);

        MStateEvent historyVisibilityEvent = new MStateEvent();
        historyVisibilityEvent.setType("m.room.history_visibility");
        JSONObject visibilityContent = new JSONObject();
        visibilityContent.put("history_visibility", "world_readable");
        historyVisibilityEvent.setContent(visibilityContent);
        initialState.add(historyVisibilityEvent);

        roomCreationRequest.setInitialState(initialState);

        getLogger().debug(".newSpaceCreationRequest(): Exit, roomCreationRequest->{}", roomCreationRequest);
        return(roomCreationRequest);
    }

    public MRoomCreation newSpaceInSpaceCreationRequest(String spaceName, String spaceAlias, String topic, String spaceId, MRoomPresetEnum roomPreset, MRoomVisibilityEnum visibility ){
        getLogger().debug(".newSpaceCreationRequest(): Entry, spaceName->{}, spaceAlias->{}, topic->{}, roomPreset->{}, visibility->{}", spaceName, spaceAlias, topic, roomPreset, visibility);

        MRoomCreation roomCreationRequest = new MRoomCreation();
        roomCreationRequest.setName(spaceName);
        roomCreationRequest.setPreset(roomPreset.getRoomPreset());
        MRoomPowerLevelsContentType powerLevelsOverride = new MRoomPowerLevelsContentType();
        powerLevelsOverride.setEventDefaultPowerLevel(100);
        powerLevelsOverride.setInvite(0);
        roomCreationRequest.setPowerLevelContentOverride(powerLevelsOverride);
        roomCreationRequest.setRoomAliasName(spaceAlias);
        roomCreationRequest.setTopic(topic);
        roomCreationRequest.setVisibility(visibility.getRoomVisibility());
        MCreationContent creationContent = new MCreationContent();
        creationContent.setFederate(false);
        creationContent.setType("m.space");
        roomCreationRequest.setCreationContent(creationContent);
        //
        // Initial State Events

        ArrayList<MStateEvent> initialState = new ArrayList<>();

        MStateEvent parent = new MStateEvent();
        parent.setType("m.space.parent");
        JSONObject parentContent = new JSONObject();
        JSONArray parentContentVia = new JSONArray();
        parentContentVia.put(matrixAccessToken.getHomeServer());
        parentContent.put("via", parentContentVia);
        parentContent.put("canonical", true);
        parent.setContent(parentContent);
        parent.setStateKey(spaceId);

        initialState.add(parent);

        MStateEvent guestAccessEvent = new MStateEvent();
        guestAccessEvent.setType("m.room.guest_access");
        JSONObject simpleObject = new JSONObject();
        simpleObject.put("guest_access", "can_join" );
        guestAccessEvent.setContent(simpleObject);

        initialState.add(guestAccessEvent);

        MStateEvent historyVisibilityEvent = new MStateEvent();
        historyVisibilityEvent.setType("m.room.history_visibility");
        JSONObject visibilityContent = new JSONObject();
        visibilityContent.put("history_visibility", "world_readable");
        historyVisibilityEvent.setContent(visibilityContent);
        initialState.add(historyVisibilityEvent);

        roomCreationRequest.setInitialState(initialState);

        getLogger().debug(".newSpaceCreationRequest(): Exit, roomCreationRequest->{}", roomCreationRequest);
        return(roomCreationRequest);
    }

    public MRoomCreation newRoomInSpaceCreationRequest(String roomName, String roomAlias, String topic, String spaceId, MRoomPresetEnum roomPreset, MRoomVisibilityEnum visibility ) {

        MRoomCreation roomCreationRequest = new MRoomCreation();

        roomCreationRequest.setName(roomName);
        roomCreationRequest.setPreset(roomPreset.getRoomPreset());
        MRoomPowerLevelsContentType powerLevelsOverride = new MRoomPowerLevelsContentType();
        powerLevelsOverride.setEventDefaultPowerLevel(100);
        powerLevelsOverride.setInvite(0);
        roomCreationRequest.setPowerLevelContentOverride(powerLevelsOverride);
        roomCreationRequest.setRoomAliasName(roomAlias);
        roomCreationRequest.setTopic(topic);
        roomCreationRequest.setVisibility(visibility.getRoomVisibility());
        MCreationContent creationContent = new MCreationContent();
        creationContent.setFederate(false);

        List<MStateEvent> initialState = new ArrayList<>();

        MStateEvent parent = new MStateEvent();
        parent.setType("m.space.parent");
        JSONObject parentContent = new JSONObject();
        JSONArray parentContentVia = new JSONArray();
        parentContentVia.put(matrixAccessToken.getHomeServer());
        parentContent.put("via", parentContentVia);
        parentContent.put("canonical", true);
        parent.setContent(parentContent);
        parent.setStateKey(spaceId);

        initialState.add(parent);

        MStateEvent historyVisibilityEvent = new MStateEvent();
        historyVisibilityEvent.setType("m.room.history_visibility");
        JSONObject visibilityContent = new JSONObject();
        visibilityContent.put("history_visibility", "world_readable");
        historyVisibilityEvent.setContent(visibilityContent);
        initialState.add(historyVisibilityEvent);

        initialState.add(historyVisibilityEvent);

        roomCreationRequest.setInitialState(initialState);

        getLogger().debug(".newSpaceCreationRequest(): Exit, roomCreationRequest->{}", roomCreationRequest);
        return(roomCreationRequest);
    }


    public PetasosParticipantSummary newPetasosParticipantSummary(ProcessingPlantSummary processingPlantSummary){
        getLogger().debug(".newPetasosParticipantSummary(): Entry, processingPlantSummary->{}", processingPlantSummary);
        PetasosParticipantSummary newParticipantSummary = new PetasosParticipantSummary();
        newParticipantSummary.setNodeType(processingPlantSummary.getNodeType());
        newParticipantSummary.setNodeVersion(processingPlantSummary.getNodeVersion());
        newParticipantSummary.setParticipantId(processingPlantSummary.getParticipantId());
        newParticipantSummary.setLastSynchronisationInstant(processingPlantSummary.getLastSynchronisationInstant());
        newParticipantSummary.setLastActivityInstant(processingPlantSummary.getLastActivityInstant());

        PetasosParticipantFulfillment participantFulfillment = new PetasosParticipantFulfillment();
        participantFulfillment.setNumberOfActualFulfillers(1);
        participantFulfillment.setNumberOfFulfillersExpected(processingPlantSummary.getReplicationCount());
        Set<ComponentIdType> fulfillers = new HashSet<>();
        fulfillers.add(processingPlantSummary.getComponentID());
        participantFulfillment.setFulfillerComponents(fulfillers);
        newParticipantSummary.setFulfillmentState(participantFulfillment);
        getLogger().debug(".newPetasosParticipantSummary(): Exit, newParticipantSummary->{}", newParticipantSummary);
        return(newParticipantSummary);
    }




    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }
}
