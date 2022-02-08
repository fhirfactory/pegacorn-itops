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
package net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps;

import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixRoom;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.core.model.petasos.oam.topology.valuesets.PetasosMonitoredComponentTypeEnum;
import net.fhirfactory.pegacorn.core.model.petasos.participant.PetasosParticipant;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.PetasosParticipantSummary;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.ParticipantRoomIdentityFactory;
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
public class ITOpsKnownParticipantMapDM {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsKnownParticipantMapDM.class);

    // ConcurrentHashMap<participantName, participant>
    private ConcurrentHashMap<String, PetasosParticipantSummary> participantMap;



    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    //
    // Constructor(s)
    //

    public ITOpsKnownParticipantMapDM(){
        this.participantMap = new ConcurrentHashMap<>();
    }

    //
    // Getters (and Setters)
    //


    public ConcurrentHashMap<String, PetasosParticipantSummary> getParticipantMap() {
        return participantMap;
    }

    protected ParticipantRoomIdentityFactory getRoomIdentityFactory() {
        return roomIdentityFactory;
    }

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Methods
    //


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
}
