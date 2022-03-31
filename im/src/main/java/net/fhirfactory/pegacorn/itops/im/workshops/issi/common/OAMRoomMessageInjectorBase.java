/*
 * Copyright (c) 2022 Mark A. Hunter
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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.common;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.methods.MatrixInstantMessageMethods;
import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixRoom;
import net.fhirfactory.pegacorn.communicate.synapse.credentials.SynapseAdminAccessToken;
import net.fhirfactory.pegacorn.communicate.synapse.methods.SynapseRoomMethods;
import net.fhirfactory.pegacorn.communicate.synapse.model.SynapseRoom;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.ITOpsSystemWideMetricsDM;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsKnownRoomAndSpaceMapDM;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.ParticipantRoomIdentityFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.metrics.ParticipantMetricsReportEventFactory;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;

abstract public class OAMRoomMessageInjectorBase extends RouteBuilder {

    @Inject
    private SynapseAdminAccessToken synapseAccessToken;

    @Inject
    private MatrixInstantMessageMethods matrixInstantMessageAPI;

    @Inject
    private MatrixAccessToken matrixAccessToken;

    @Inject
    private ITOpsKnownRoomAndSpaceMapDM matrixBridgeCache;

    @Inject
    private ITOpsSystemWideMetricsDM systemWideMetricsCache;

    @Inject
    private ParticipantRoomIdentityFactory roomIdentityFactory;

    @Inject
    private ParticipantMetricsReportEventFactory metricsReportEventFactory;

    @Inject
    private SynapseRoomMethods synapseRoomAPI;

    //
    // Abstract Methods
    //

    abstract protected Logger getLogger();

    //
    // Getters (and Setters)
    //

    protected SynapseAdminAccessToken getSynapseAccessToken() {
        return synapseAccessToken;
    }

    protected MatrixInstantMessageMethods getMatrixInstantMessageAPI() {
        return matrixInstantMessageAPI;
    }

    protected MatrixAccessToken getMatrixAccessToken() {
        return matrixAccessToken;
    }

    protected ITOpsKnownRoomAndSpaceMapDM getMatrixBridgeCache() {
        return matrixBridgeCache;
    }

    protected ITOpsSystemWideMetricsDM getSystemWideMetricsCache() {
        return systemWideMetricsCache;
    }

    protected ParticipantRoomIdentityFactory getRoomIdentityFactory() {
        return roomIdentityFactory;
    }

    protected ParticipantMetricsReportEventFactory getMetricsReportEventFactory() {
        return metricsReportEventFactory;
    }

    protected SynapseRoomMethods getSynapseRoomAPI()  {
        return synapseRoomAPI;
    }

    //
    // Business Methods
    //

    public String getRoomIdFromPseudoAlias(String pseudoAlias){
        getLogger().debug(".getRoomIdFromPseudoAlias(): Entry, pseudoAlias->{}", pseudoAlias);

        String roomID = null;
        //
        // Check to see if room is in the cache.
        roomID = getMatrixBridgeCache().getRoomIdFromPseudoAlias(pseudoAlias);

        //
        // If we haven't found room in cache, check to see if room already exists (within Synapse)
        if(roomID == null) {
            List<SynapseRoom> rooms = getSynapseRoomAPI().getRooms(pseudoAlias);
            if (!rooms.isEmpty()) {
                SynapseRoom roomFromPseudoAlias = new MatrixRoom(rooms.get(0));
                roomID = roomFromPseudoAlias.getRoomID();
            }
        }

        //
        // return -null- or the room_id
        getLogger().debug(".getRoomIdFromPseudoAlias(): Exit, roomFromPseudoAlias->{}", roomID);
        return(roomID);
    }

    //
    // Mechanism to ensure Startup
    //

    @Override
    public void configure() throws Exception {
        String wupName = getClass().getSimpleName();

        from("timer://"+wupName+"?delay=1000&repeatCount=1")
                .routeId("ProcessingPlant::"+wupName)
                .log(LoggingLevel.DEBUG, "Starting....");
    }

}
