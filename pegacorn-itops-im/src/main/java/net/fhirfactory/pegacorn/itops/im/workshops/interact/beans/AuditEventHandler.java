/*
 * Copyright (c) 2021 Kelly Skye (ACT Health)
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
package net.fhirfactory.pegacorn.itops.im.workshops.interact.beans;

import net.fhirfactory.pegacorn.common.model.componentid.TopologyNodeFDN;
import net.fhirfactory.pegacorn.common.model.componentid.TopologyNodeRDN;
import net.fhirfactory.pegacorn.common.model.componentid.TopologyNodeTypeEnum;
import net.fhirfactory.pegacorn.components.transaction.valuesets.exceptions.ResourceInvalidSearchException;
import net.fhirfactory.pegacorn.itops.im.workshops.cache.ITOpsSystemWideTopologyDM;
import net.fhirfactory.pegacorn.petasos.model.itops.topology.ITOpsMonitoredProcessingPlant;
import net.fhirfactory.pegacorn.petasos.model.itops.topology.common.ITOpsMonitoredNode;
import org.apache.camel.Header;
import org.apache.maven.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AuditEventHandler {
    private static final String LIMIT = "5";

    private static final Logger LOG = LoggerFactory.getLogger(AuditEventHandler.class);

    @Inject
    private ITOpsSystemWideTopologyDM nodeDM;

    protected ITOpsSystemWideTopologyDM getNodeDM() {
        return (this.nodeDM);
    }

    protected Logger getLogger() {
        return (LOG);
    }

    protected List<String> getSiteAuditRecords(@Header("componentId") String componentId) throws ResourceInvalidSearchException {
        // Derive the site and entity type from the topologyNode
        // Site is in ITOpsMonitoredProcessingPlant
        // EntityType can be derived from ... ?


        getLogger().debug(".getSiteAuditRecords(): Entry, nodeKey --> {}", componentId);
        ITOpsMonitoredNode node = getNodeDM().getNode(componentId);
        if (node.getNodeType().equals(TopologyNodeTypeEnum.PROCESSING_PLANT)) {
            ITOpsMonitoredProcessingPlant subsystemNode = (ITOpsMonitoredProcessingPlant) node;
            String site = extractSiteFromNode(subsystemNode.getTopologyNodeFDN());
            String entityType = extractEntityTypeFromNode(subsystemNode.getTopologyNodeFDN());
            String limit = LIMIT; // Must be int

            if(StringUtils.isNotBlank(site) && StringUtils.isNotBlank(entityType))
            {
            //TODO call Hestia-Audit-DM via camel
                List<String> audits = new ArrayList<String>();
                
                return audits;
            }

        }

        return null;
    }


    private String extractSiteFromNode(TopologyNodeFDN nodeFDN) {
        for (TopologyNodeRDN currentRDN : nodeFDN.getHierarchicalNameSet()) {
            if (currentRDN.getNodeType().equals(TopologyNodeTypeEnum.SITE)) {
                return currentRDN.getNodeName();
            }
        }
        return null;
    }

    private String extractEntityTypeFromNode(TopologyNodeFDN nodeFDN) {
        // TODO Work out where entity-type can be found on the topology
        return null;
    }

}
