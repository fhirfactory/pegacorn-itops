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
package net.fhirfactory.pegacorn.itops.im.services.oam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.fhirfactory.pegacorn.core.interfaces.oam.topology.PetasosTopologyReportingServiceProviderNameInterface;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.model.petasos.oam.topology.reporting.PetasosMonitoredTopologyGraph;
import net.fhirfactory.pegacorn.core.model.ui.resources.summaries.ProcessingPlantSummary;
import net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps.ITOpsSystemWideReportedTopologyMapDM;
import net.fhirfactory.pegacorn.petasos.oam.topology.PetasosMonitoredTopologyReportingAgent;
import net.fhirfactory.pegacorn.petasos.oam.topology.cache.PetasosLocalTopologyReportingDM;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Timer;
import java.util.TimerTask;

@ApplicationScoped
public class PetasosTopologyReportingService extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosTopologyReportingService.class);
    private boolean initialised;
    private static long SYNCHRONIZATION_CHECK_PERIOD = 30000;
    private static long INITIAL_CHECK_DELAY_PERIOD=60000;
    private boolean backgroundCheckInitiated;
    private ObjectMapper jsonMapper;

    @Inject
    private PetasosLocalTopologyReportingDM itOpsTopologyDM;

    @Inject
    private PetasosMonitoredTopologyReportingAgent topologyReportingAgent;

    @Inject
    private PetasosTopologyReportingServiceProviderNameInterface topologyReportingServiceProviderName;

    @Inject
    private ITOpsSystemWideReportedTopologyMapDM systemWideTopologyMapDM;

    @Inject
    private ProcessingPlantInterface processingPlant;

    public PetasosTopologyReportingService(){
        super();
        this.initialised = false;
        this.backgroundCheckInitiated = false;
        this.jsonMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        this.jsonMapper.registerModule(module);
        this.jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    protected Logger getLogger() {
        return (LOG);
    }


    protected ProcessingPlantInterface getProcessingPlant(){
        return(processingPlant);
    }


    protected void forwardTopologyDetails() {
        LOG.debug(".forwardTopologyDetails(): Entry");
        //
        // Building Map
        //
        LOG.trace(".forwardTopologyDetails(): [Build Topology Graph] Start");
        topologyReportingAgent.refreshLocalTopologyGraph();
        LOG.trace(".forwardTopologyDetails(): [Build Topology Graph] Finish");
        //
        // Get Data to be Reported ON
        //
        LOG.trace(".forwardTopologyDetails(): [Grab Current Topology Graph] Start");
        PetasosMonitoredTopologyGraph currentState = itOpsTopologyDM.getCurrentState();
        currentState.setDeploymentName(getProcessingPlant().getSolutionNode().getComponentRDN().getNodeName());
        LOG.trace(".forwardTopologyDetails(): [Grab Current Topology Graph] Finish");
        //
        // Send Update
        //
        if(currentState != null) {
            for (ProcessingPlantSummary currentProcessingPlant : currentState.getProcessingPlants().values()) {
                systemWideTopologyMapDM.addProcessingPlant(getProcessingPlant().getMeAsASoftwareComponent().getComponentID().getId(), currentProcessingPlant);
            }
        }
        //
        // All Done!
        //
        LOG.debug(".forwardTopologyDetails(): Exit");
    }

    //
    // Post Construct
    //

    @PostConstruct
    public void initialise(){
        if(!initialised){
            scheduleTopologyGraphForwarding();;
            this.initialised = true;
        }
    }

    //
    // Schedule Period Synchronisation Check/Update Activity
    //

    public void scheduleTopologyGraphForwarding() {

        getLogger().debug(".scheduleTopologyGraphForwarding(): Entry");
        if(isBackgroundCheckInitiated()){
            // do nothing
        } else {
            TimerTask ITOpsTopologyCacheSynchronisationCheck = new TimerTask() {
                public void run() {
                    getLogger().debug(".ITOpsTopologyCacheSynchronisationCheck(): Entry");
                    forwardTopologyDetails();
                    getLogger().debug(".ITOpsTopologyCacheSynchronisationCheck(): Exit");
                }
            };
            String timerName = "ITOpsTopologyCacheSynchronisationCheck";
            Timer timer = new Timer(timerName);
            timer.schedule(ITOpsTopologyCacheSynchronisationCheck, getInitialCheckDelayPeriod(), getSynchronizationCheckPeriod());
            setBackgroundCheckInitiated(true);
        }
        getLogger().debug(".scheduleTopologyGraphForwarding(): Exit");
    }

    //
    //
    //

    protected String getFriendlyName() {
        return ("PetasosTopologyReportingAgent");
    }

    //
    // Getters (and Setters)
    //

    public static long getSynchronizationCheckPeriod() {
        return SYNCHRONIZATION_CHECK_PERIOD;
    }

    public static long getInitialCheckDelayPeriod() {
        return INITIAL_CHECK_DELAY_PERIOD;
    }

    public boolean isBackgroundCheckInitiated() {
        return backgroundCheckInitiated;
    }

    public void setBackgroundCheckInitiated(boolean value){
        this.backgroundCheckInitiated = value;
    }

    public ObjectMapper getJsonMapper() {
        return jsonMapper;
    }


    //
    // Class Kickstarter
    //

    @Override
    public void configure() throws Exception {
        String name = getFriendlyName();

        from("timer://"+name+"?delay=1000&repeatCount=1")
                .routeId(getClass().getName())
                .log(LoggingLevel.DEBUG, "Starting....");
    }
}
