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

import net.fhirfactory.dricats.model.petasos.participant.PetasosParticipantFulfillmentStatusEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownParticipantMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsSystemWideReportedTopologyMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.topology.ParticipantTopologyIntoReplicaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ITOpsSubsystemParticipantTasks {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsSubsystemParticipantTasks.class);

    @Inject
    private ITOpsSystemWideReportedTopologyMapDM systemWideTopologyMap;

    @Inject
    private ITOpsKnownParticipantMapDM participantCache;

    @Inject
    private ParticipantTopologyIntoReplicaFactory matrixBridgeFactories;

    //
    // Business Methods
    //

    /**
     * This method parses the reported Topology Map (getSystemWideTopologyMap()) and, for each ProcessingPlant,
     * builds a "Participant List" that contains each of the ProcessingPlants.
     *
     * The ITOps Replica service uses "participant names" as the primary key for all logic.
     *
     * It also checks the number of ProcessingPlant's fulfilling a particular Participant. If there is less ProcessingPlants
     * fulfilling/implementing a particular Participant than expected, then it sets that Participant's status to
     * "PARTIALLY_FULFILLED". If the number of ProcessingPlants is equal to the expected -> then the state is set
     * to "FULLY_FULFILLED". This function is mainly used to allow end-users/administrators to know if ALL PODs (where
     * the scaling of PODs is > 1) are operational.
     */
    public void updateParticipantListUsingReportedTopology(){
        getLogger().debug(".injectSubsystemParticipantRoomSet(): [Synchronise Participant List] Start...");
        List<ProcessingPlantSummary> processingPlants = systemWideTopologyMap.getProcessingPlants();
        if (processingPlants.isEmpty()) {
            // do nothing
        } else {
            getLogger().debug(".injectSubsystemParticipantRoomSet(): [Synchronise Participant List] Adding Processing Plants...");
            for (ProcessingPlantSummary currentProcessingPlant : processingPlants) {
                getLogger().trace(".injectSubsystemParticipantRoomSet(): [Synchronise Participant List] Processing ->{}", currentProcessingPlant);
                //
                // Check to see if entry exists... we don't automatically delete, merely update
                PetasosParticipantSummary participantSummary = participantCache.getParticipant(currentProcessingPlant.getParticipantName());
                if (participantSummary == null) {
                    getLogger().debug(".topologyReplicationSynchronisationDaemon(): [Synchronise Participant List] Participant Does Not Exit, Adding");
                    PetasosParticipantSummary newParticipantSummary = matrixBridgeFactories.newPetasosParticipantSummary(currentProcessingPlant);
                    participantCache.addParticipant(newParticipantSummary);
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

                //
                // Check for fulfillment of Participant replication/scale count
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

    //
    // Getters (and Setters)
    //

    protected Logger getLogger() {
        return (LOG);
    }

}
