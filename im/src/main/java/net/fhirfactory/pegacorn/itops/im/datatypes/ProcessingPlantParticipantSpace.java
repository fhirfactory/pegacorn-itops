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
package net.fhirfactory.pegacorn.itops.im.datatypes;

import com.fasterxml.jackson.annotation.JsonFormat;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.dricats.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.ProcessingPlantSummary;

import java.io.Serializable;
import java.time.Instant;

public class ProcessingPlantParticipantSpace implements Serializable {
    private ProcessingPlantSummary processingPlant;
    private SynapseRoom room;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSXXX", timezone = PetasosPropertyConstants.DEFAULT_TIMEZONE)
    private Instant lastSynchronisationCheckInstant;

    //
    // Getters and Setters
    //

    public ProcessingPlantSummary getProcessingPlant() {
        return processingPlant;
    }

    public void setProcessingPlant(ProcessingPlantSummary processingPlant) {
        this.processingPlant = processingPlant;
    }

    public SynapseRoom getRoom() {
        return room;
    }

    public void setRoom(SynapseRoom room) {
        this.room = room;
    }

    public Instant getLastSynchronisationCheckInstant() {
        return lastSynchronisationCheckInstant;
    }

    public void setLastSynchronisationCheckInstant(Instant lastSynchronisationCheckInstant) {
        this.lastSynchronisationCheckInstant = lastSynchronisationCheckInstant;
    }

    //
    // To String
    //

    @Override
    public String toString() {
        return "ProcessingPlantParticipantSpace{" +
                "processingPlant=" + processingPlant +
                ", room=" + room +
                ", lastSynchronisationCheckInstant=" + lastSynchronisationCheckInstant +
                '}';
    }
}
