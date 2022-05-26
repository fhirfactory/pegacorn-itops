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

import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ITOpsRoomHelpers {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsRoomHelpers.class);

    public boolean isAnITOpsRoom(SynapseRoom room) {
        getLogger().debug(".isAnITOpsRoom(): Entry, room->{}", room);
        String roomAlias = room.getCanonicalAlias();

        if (isAnITOpsRoom(roomAlias)) {
            getLogger().debug(".isAnITOpsRoom(): Exit, returning->{}", true);
            return (true);
        } else {
            getLogger().debug(".isAnITOpsRoom(): Exit, returning->{}", false);
            return (false);
        }
    }

    public boolean isAnITOpsRoom(String roomAlias) {
        getLogger().debug(".isAnITOpsRoom(): Entry, room->{}", roomAlias);

        if (roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_CONSOLE.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_METRICS.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_TASKS.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP_SUBSCRIPTIONS.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_WORKSHOP.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_CONSOLE.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_TASKS.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_METRICS.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_SUBSCRIPTIONS.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_CONSOLE.getAliasPrefix()) ||
                roomAlias.contains(OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT_METRICS.getAliasPrefix())) {
            getLogger().debug(".isAnITOpsRoom(): Exit, returning->{}", true);
            return (true);
        } else {
            getLogger().debug(".isAnITOpsRoom(): Exit, returning->{}", false);
            return (false);
        }
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }
}
