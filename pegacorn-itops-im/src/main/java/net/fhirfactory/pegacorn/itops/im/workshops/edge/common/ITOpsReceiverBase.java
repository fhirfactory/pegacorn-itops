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
package net.fhirfactory.pegacorn.itops.im.workshops.edge.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.fhirfactory.pegacorn.components.capabilities.CapabilityFulfillmentInterface;
import net.fhirfactory.pegacorn.components.capabilities.base.CapabilityUtilisationResponse;
import net.fhirfactory.pegacorn.components.interfaces.topology.ProcessingPlantInterface;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;

public abstract class ITOpsReceiverBase extends RouteBuilder implements CapabilityFulfillmentInterface {

    private ObjectMapper jsonMapper;
    private boolean initialised;

    public ITOpsReceiverBase(){
        super();
        this.initialised = false;
        jsonMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        jsonMapper.registerModule(module);
        jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Inject
    private ProcessingPlantInterface processingPlant;

    @PostConstruct
    public void initialise(){
        getLogger().debug(".initialise(): Entry");
        if(!this.initialised){
            getLogger().info(".initialise(): Initialising...");
            getLogger().info(".initialise(): [Register Capability] Start");
            registerCapabilities();
            getLogger().info(".initialise(): [Register Capability] Finish");
            this.initialised = true;
            getLogger().info(".initialise(): Done.");
        } else {
            getLogger().debug(".initialise(): Already initiailised, nothing to be done...");
        }
        getLogger().debug(".initialise(): Exit");
    }

    protected abstract void registerCapabilities();
    protected abstract Logger getLogger();

    public ProcessingPlantInterface getProcessingPlant() {
        return processingPlant;
    }

    public ObjectMapper getJsonMapper() {
        return jsonMapper;
    }

    protected CapabilityUtilisationResponse generateBadResponse(String requestID){
        CapabilityUtilisationResponse response = new CapabilityUtilisationResponse();
        if(StringUtils.isEmpty(requestID)){
            response.setAssociatedRequestID("Unknown");
        } else {
            response.setAssociatedRequestID(requestID);
        }
        response.setSuccessful(false);
        response.setDateCompleted(Instant.now());
        return(response);
    }

    @Override
    public void configure() throws Exception {
        String receiverName = getClass().getSimpleName();

        from("timer://"+receiverName+"?delay=1000&repeatCount=1")
                .routeId("ITOpsReceiver::"+receiverName)
                .log(LoggingLevel.DEBUG, "Starting....");
    }
}
