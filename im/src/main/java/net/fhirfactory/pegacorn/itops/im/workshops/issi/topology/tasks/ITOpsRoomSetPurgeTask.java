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

import net.fhirfactory.pegacorn.communicate.synapse.credentials.SynapseAdminAccessToken;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseRoomMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseAdminProxyInterface;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class ITOpsRoomSetPurgeTask {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsRoomSetPurgeTask.class);

    @Inject
    private SynapseAdminProxyInterface synapseAdminProxy;

    @Inject
    private SynapseAdminAccessToken synapseAccessToken;

    @Inject
    private SynapseRoomMethods synapseRoomAPI;


    public void purgeRoomSet() {
        getLogger().info(".purgeRoomSet(): [Clean Out Room List] Start...");
        List<SynapseRoom> startupRoomList = getSynapseRoomAPI().getRooms("*");
        for (SynapseRoom currentRoom : startupRoomList) {
            String creator = currentRoom.getCreator().toLowerCase(Locale.ROOT);
            if (creator.contains("hunter") || creator.contains("replicabridge")) {
                getLogger().info(".purgeRoomSet(): [Clean Out Room List] Deleting Room->{}", currentRoom.getName());
                synapseRoomAPI.deleteRoom(currentRoom.getRoomID(), "cleaning up");
            }
        }
        getLogger().info(".purgeRoomSet(): [Clean Out Room List] Finish...");
    }

    //
    // Getters (and Setters)
    //

    protected SynapseAdminAccessToken getSynapseAccessToken(){
        return(synapseAccessToken);
    }

    protected Logger getLogger(){
        return(LOG);
    }

    protected SynapseAdminProxyInterface getSynapseAdminProxy(){
        return(synapseAdminProxy);
    }

    protected SynapseRoomMethods getSynapseRoomAPI(){
        return(this.synapseRoomAPI);
    }
}
