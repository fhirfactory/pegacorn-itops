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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.factories;

import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixRoom;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomCreation;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomPresetEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.rooms.MRoomVisibilityEnum;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.WorkshopSummary;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.topology.common.BaseParticipantReplicaServices;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Locale;

@ApplicationScoped
public class WorkshopParticipantReplicaFactory extends BaseParticipantReplicaServices {
    private static final Logger LOG = LoggerFactory.getLogger(WorkshopParticipantReplicaFactory.class);

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

    public MatrixRoom createSubSpaceIfNotThere(String processingPlantSpaceId, MatrixRoom workshopSpace, WorkshopSummary workshopSummary) {
        getLogger().debug(".createSubSpaceIfNotThere(): Entry, processingPlantSpaceId->{}, roomList,  workshop->{}", processingPlantSpaceId,  workshopSummary.getTopologyNodeFDN());
        try {
            String workshopName = workshopSummary.getParticipantName();
            String workshopDisplayName = workshopSummary.getParticipantDisplayName();
            String workshopAlias = OAMRoomTypeEnum.OAM_ROOM_TYPE_WORKSHOP.getAliasPrefix() + workshopName.toLowerCase(Locale.ROOT).replace(".", "-");
            MatrixRoom workshopRoom = null;
            if (workshopSpace != null) {
                workshopRoom = workshopSpace;
            } else {
                getLogger().trace(".createSubSpaceIfNotThere(): Creating Space for ->{}", workshopAlias);
                String workshopTopic = "Workshop, " + workshopName;

                getLogger().trace(".createSubSpaceIfNotThere(): First double-checking the room isn't already create ->{}", workshopAlias);

                MatrixRoom existingRoom = getRoomCache().getRoomFromPseudoAlias(workshopAlias);
                if (existingRoom != null) {
                    getLogger().trace(".createSubSpaceIfNotThere(): Room already exists ->{}", workshopAlias);
                    workshopRoom = existingRoom;
                } else {
                    MRoomCreation mRoomCreation = getMatrixBridgeFactories().newSpaceInSpaceCreationRequest(workshopDisplayName, workshopAlias, workshopTopic, processingPlantSpaceId, MRoomPresetEnum.ROOM_PRESET_PUBLIC_CHAT, MRoomVisibilityEnum.ROOM_VISIBILITY_PUBLIC);
                    MatrixRoom createdRoom = getMatrixSpaceAPI().createSpace(getMatrixAccessToken().getUserId(), mRoomCreation);
                    if (createdRoom != null) {
                        workshopRoom = createdRoom;
                        getRoomCache().addRoom(createdRoom);
                    }
                }

                if (workshopRoom != null) {
                    getLogger().trace(".createSubSpaceIfNotThere(): Space ->{}", workshopRoom);
                    getMatrixSpaceAPI().addChildToSpace(processingPlantSpaceId, workshopRoom.getRoomID());
                }
            }

            getLogger().debug(".createSubSpaceIfNotThere(): Exit, workshopId->{}", workshopRoom.getRoomID());
            return (workshopRoom);
        } catch (Exception ex){
            getLogger().error(".createSubSpaceIfNotThere(): Error Creating WUP Space/Room, message->{}, stacktrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return(null);
        }
    }

}
