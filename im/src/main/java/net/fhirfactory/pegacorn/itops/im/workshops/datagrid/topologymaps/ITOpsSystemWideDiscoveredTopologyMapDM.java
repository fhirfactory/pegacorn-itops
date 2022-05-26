/*
 * Copyright (c) 2021 Mark A. Hunter (ACT Health)
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

import net.fhirfactory.pegacorn.core.model.component.SoftwareComponent;
import net.fhirfactory.pegacorn.petasos.endpoints.services.topology.PetasosDistributedSoftwareComponentMapDM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

@ApplicationScoped
public class ITOpsSystemWideDiscoveredTopologyMapDM {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsSystemWideDiscoveredTopologyMapDM.class);

    @Inject
    private PetasosDistributedSoftwareComponentMapDM discoveredComponentMap;

    //
    // Constructor(s)
    //


    //
    // Business Methods
    //

    public Set<SoftwareComponent> getDiscoveredSubsystems(){
        getLogger().debug(".getDiscoveredSubsystems(): Entry");

        Set<SoftwareComponent> topologyNodeSet = getDiscoveredComponentMap().getTopologyNodeSet();

        getLogger().debug(".getDiscoveredSubsystems(): Exit");
        return(topologyNodeSet);
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected PetasosDistributedSoftwareComponentMapDM getDiscoveredComponentMap(){
        return(this.discoveredComponentMap);
    }
}
