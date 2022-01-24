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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.model.capabilities.CapabilityFulfillmentInterface;
import net.fhirfactory.pegacorn.core.model.capabilities.base.CapabilityUtilisationResponse;
import net.fhirfactory.pegacorn.services.oam.endpoint.PetasosOAMMetricsCollectorEndpoint;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

public abstract class ITOpsReceiverBase extends RouteBuilder implements CapabilityFulfillmentInterface {

    private ObjectMapper jsonMapper;
    private boolean initialised;
    private Instant lastUpdate;

    private static Long CACHE_MONITOR_PERIOD = 30000L;
    private static Long CACHE_INITIAL_WAIT = 60000L;


    @Inject
    private ProcessingPlantInterface processingPlant;

    @Inject
    private PetasosOAMMetricsCollectorEndpoint metricsCollectorEndpoint;

    //
    // Constructor(s)
    //

    public ITOpsReceiverBase(){
        super();
        this.initialised = false;
        jsonMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        jsonMapper.registerModule(module);
        jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.lastUpdate = Instant.EPOCH;
    }

    //
    // Post Construct
    //

    @PostConstruct
    public void initialise(){
        getLogger().debug(".initialise(): Entry");
        if(!this.initialised){
            getLogger().info(".initialise(): Initialising...");
            getLogger().info(".initialise(): [Register Capability] Start");
            registerCapabilities();
            getLogger().info(".initialise(): [Register Capability] Finish");
            getLogger().info(".initialise(): [Initialise Collection Endpoint] Start");
            metricsCollectorEndpoint.initialise();
            getLogger().info(".initialise(): [Initialise Collection Endpoint] Finish");
            this.initialised = true;
            getLogger().info(".initialise(): Done.");
        } else {
            getLogger().debug(".initialise(): Already initiailised, nothing to be done...");
        }
        getLogger().debug(".initialise(): Exit");
    }

    protected abstract void registerCapabilities();
    protected abstract Logger getLogger();
    protected abstract void cacheMonitorProcess();
    protected abstract String cacheMonitorProcessTimerName();

    protected CapabilityUtilisationResponse generateBadResponse(String requestID){
        CapabilityUtilisationResponse response = new CapabilityUtilisationResponse();
        if(StringUtils.isEmpty(requestID)){
            response.setAssociatedRequestID("Unknown");
        } else {
            response.setAssociatedRequestID(requestID);
        }
        response.setSuccessful(false);
        response.setInstantCompleted(Instant.now());
        return(response);
    }

    @Override
    public void configure() throws Exception {
        String receiverName = getClass().getSimpleName();

        from("timer://"+receiverName+"?delay=1000&repeatCount=1")
                .routeId("ITOpsReceiver::"+receiverName)
                .log(LoggingLevel.DEBUG, "Starting....");
    }

    //
    // Scheduler
    //

    public void scheduleOngoingCacheUpdateNotificationService() {
        getLogger().debug(".scheduleOngoingCacheUpdateNotificationService(): Entry");
        TimerTask ongoingWatchdogTask = new TimerTask() {
            public void run() {
                getLogger().debug(".ongoingWatchdogTask(): Entry");
                cacheMonitorProcess();
                getLogger().debug(".ongoingWatchdogTask(): Exit");
            }
        };
        Timer timer = new Timer(cacheMonitorProcessTimerName());
        timer.schedule(ongoingWatchdogTask, CACHE_INITIAL_WAIT, CACHE_MONITOR_PERIOD);

        getLogger().debug(".scheduleOngoingCacheUpdateNotificationService(): Exit");
    }

    //
    // Getters (and Setters)
    //

    public ProcessingPlantInterface getProcessingPlant() {
        return processingPlant;
    }

    public ObjectMapper getJsonMapper() {
        return jsonMapper;
    }

    protected Instant getLastUpdate() {
        return lastUpdate;
    }

    protected void touchLastUpdateInstant() {
        this.lastUpdate = Instant.now();
    }

    public Long getCacheMonitorPeriod() {
        return CACHE_MONITOR_PERIOD;
    }
}
