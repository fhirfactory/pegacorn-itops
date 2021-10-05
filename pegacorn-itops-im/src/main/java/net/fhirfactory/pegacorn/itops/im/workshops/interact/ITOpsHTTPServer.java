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
package net.fhirfactory.pegacorn.itops.im.workshops.interact;

import java.util.ArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import net.fhirfactory.pegacorn.itops.im.workshops.interact.beans.*;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.hl7.fhir.r4.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.common.model.componentid.TopologyNodeFDN;
import net.fhirfactory.pegacorn.components.transaction.valuesets.exceptions.ResourceNotFoundException;
import net.fhirfactory.pegacorn.components.transaction.valuesets.exceptions.ResourceUpdateException;
import net.fhirfactory.pegacorn.deployment.topology.model.endpoints.base.IPCTopologyEndpoint;
import net.fhirfactory.pegacorn.deployment.topology.model.endpoints.technologies.HTTPServerClusterServiceTopologyEndpointPort;
import net.fhirfactory.pegacorn.deployment.topology.model.nodes.ProcessingPlantTopologyNode;
import net.fhirfactory.pegacorn.deployment.topology.model.nodes.WorkshopTopologyNode;
import net.fhirfactory.pegacorn.internals.PegacornReferenceProperties;
import net.fhirfactory.pegacorn.itops.im.common.ITOpsIMNames;
import net.fhirfactory.pegacorn.petasos.core.moa.wup.MessageBasedWUPEndpoint;
import net.fhirfactory.pegacorn.petasos.model.itops.metrics.ITOpsMetricsSet;
import net.fhirfactory.pegacorn.workshops.InteractWorkshop;
import net.fhirfactory.pegacorn.workshops.base.Workshop;
import net.fhirfactory.pegacorn.wups.archetypes.unmanaged.NonResilientWithAuditTrailWUP;

@ApplicationScoped
public class ITOpsHTTPServer extends NonResilientWithAuditTrailWUP {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsHTTPServer.class);

    private static String WUP_VERSION = "1.0.0";

    private String serverHostName;
    private int serverHostPort;
    private String hTTPScheme;

    @Inject
    private PegacornReferenceProperties pegacornReferenceProperties;

    @Inject
    private InteractWorkshop workshop;

    @Inject
    private ITOpsIMNames names;

    @Inject
    private ProcessingPlantTopologyNodeHandler processingPlantHandler;

    @Inject
    private ITOpsTopologyGraphHandler topologyGraphHandler;

    @Inject
    private ITOpsMetricsHandler metricsHandler;

    @Inject
    private ITOpsPubSubReportHandler pubSubReportHandler;

    @Inject
    private AuditEventHandler auditEventHandler;

    //
    // Post Construct Activities
    //

    @Override
    protected void executePostConstructActivities(){
        deriveEndpointDetails();
    }

    //
    // Actual RESTful Server
    //
    @Override
    public void configure() throws Exception {

         restConfiguration()
                    .component("netty-http")
                    .scheme(getHTTPScheme())
                    .bindingMode(RestBindingMode.json)
                    .dataFormatProperty("moduleClassNames", "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule")
                    .dataFormatProperty("disableFeatures", "WRITE_DATES_AS_TIMESTAMPS")
                    .dataFormatProperty("prettyPrint", "true")
                    .contextPath(getPegacornReferenceProperties().getITOpsContextPath())
                    .host(getServerHostName())
                    .port(getServerHostPort());
//                    .enableCORS(true)
//                    .corsAllowCredentials(true)
//                    .corsHeaderProperty("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, login");

        rest("/ITOpsTopologyGraph")
                .get("")
                    .to("direct:ITOpsTopologyGraphGET");

        rest("/ProcessingPlant")
                .get("/{componentId}").outType(ProcessingPlantTopologyNode.class)
                    .to("direct:ProcessingPlantTopologyNodeGET")
                .get("?pageSize={pageSize}&page={page}&sortBy={sortBy}&sortOrder={sortOrder}")
                    .param().name("pageSize").type(RestParamType.query).required(false).endParam()
                    .param().name("page").type(RestParamType.query).required(false).endParam()
                    .param().name("sortBy").type(RestParamType.query).required(false).endParam()
                    .param().name("sortOrder").type(RestParamType.query).required(false).endParam()
                    .to("direct:ProcessingPlantTopologyNodeListGET");      

        rest("/AuditEvents")
                .get("/{nodeName}")
                .to("direct:AuditEventGET");


        from("direct:ITOpsTopologyGraphGET")
                .log(LoggingLevel.INFO, "GET TopologyGraph")
                .bean(topologyGraphHandler, "getTopologyGraph");

        from("direct:ProcessingPlantTopologyNodeGET")
                .log(LoggingLevel.INFO, "GET Request --> ${body}")
                .bean(processingPlantHandler, "getProcessingPlantTopologyNode");

        from("direct:ProcessingPlantTopologyNodeListGET")
                .log(LoggingLevel.INFO, "GET All Request")
                .bean(processingPlantHandler, "getProcessingPlantTopologyNodeList");
  
        from("direct:AuditEventGET")
                .log(LoggingLevel.INFO, "GET Request --> ${body}")
                .bean(auditEventHandler, "getSiteAuditRecords");

        rest("/AuditEvents")
                .get("/{componentId}").outType(AuditEvent.class)
                .to("direct:AuditEventGET");

        //
        // Metrics
        //

        rest("/ProcessingPlant")
                .get("/{componentId}/ITOpsMetrics").outType(ITOpsMetricsSet.class)
                .to("direct:ProcessingPlantMetricsGET");

        from("direct:ProcessingPlantMetricsGET")
                .log(LoggingLevel.INFO, "GET Metrics Request")
                .bean(metricsHandler, "retrieveMetrics");

        rest("/WorkUnitProcessor")
                .get("/{componentId}/ITOpsMetrics").outType(ITOpsMetricsSet.class)
                .to("direct:WUPMetricsGET");

        from("direct:WUPMetricsGET")
                .log(LoggingLevel.INFO, "GET Metrics Request")
                .bean(metricsHandler, "retrieveMetrics");

        //
        // PubSub Report
        //

        rest("/ProcessingPlant")
                .get("/{componentId}/PublishSubscribeReport").outType(ITOpsMetricsSet.class)
                .to("direct:ProcessingPlantPubSubReportGET");

        from("direct:ProcessingPlantPubSubReportGET")
                .log(LoggingLevel.INFO, "GET PubSub Report Request")
                .bean(pubSubReportHandler, "retrieveProcessingPlantPubSubReport");

        rest("/WorkUnitProcessor")
                .get("/{componentId}/PublishSubscribeReport").outType(ITOpsMetricsSet.class)
                .to("direct:WUPPubSubReportGET");

        from("direct:WUPPubSubReportGET")
                .log(LoggingLevel.INFO, "GET PubSub Report Request")
                .bean(pubSubReportHandler, "retrieveWorkUnitProcessorPubSubReport");
    }

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    @Override
    protected String specifyWUPInstanceName() {
        return (getClass().getSimpleName());
    }

    @Override
    protected String specifyWUPInstanceVersion() {
        return (WUP_VERSION);
    }

    @Override
    protected Workshop specifyWorkshop() {
        return (workshop);
    }

    //
    // Getters (and Setters)
    //

    public PegacornReferenceProperties getPegacornReferenceProperties() {
        return pegacornReferenceProperties;
    }

    protected String getServerHostName(){
        return(this.serverHostName);
    }

    protected int getServerHostPort(){
        return(this.serverHostPort);
    }

    protected String getHTTPScheme(){
        return(this.hTTPScheme);
    }

    //
    // Endpoint Details Derivation
    //

    protected void deriveEndpointDetails() {
        MessageBasedWUPEndpoint endpoint = new MessageBasedWUPEndpoint();
        HTTPServerClusterServiceTopologyEndpointPort serverTopologyEndpoint = (HTTPServerClusterServiceTopologyEndpointPort) getTopologyEndpoint(names.getInteractITOpsIMHTTPServerName());
        this.serverHostPort = serverTopologyEndpoint.getPortValue();
        this.serverHostName = serverTopologyEndpoint.getHostDNSName();
        if(serverTopologyEndpoint.isEncrypted()){
            this.hTTPScheme = "https";
        } else {
            this.hTTPScheme = "http";
        }
    }

    protected IPCTopologyEndpoint getTopologyEndpoint(String topologyEndpointName){
        getLogger().debug(".getTopologyEndpoint(): Entry, topologyEndpointName->{}", topologyEndpointName);
        ArrayList<TopologyNodeFDN> endpointFDNs = getProcessingPlant().getProcessingPlantNode().getEndpoints();
        for(TopologyNodeFDN currentEndpointFDN: endpointFDNs){
            IPCTopologyEndpoint endpointTopologyNode = (IPCTopologyEndpoint)getTopologyIM().getNode(currentEndpointFDN);
            if(endpointTopologyNode.getName().contentEquals(topologyEndpointName)){
                getLogger().debug(".getTopologyEndpoint(): Exit, node found -->{}", endpointTopologyNode);
                return(endpointTopologyNode);
            }
        }
        getLogger().debug(".getTopologyEndpoint(): Exit, Could not find node!");
        return(null);
    }

    protected OnExceptionDefinition getResourceNotFoundException() {
        OnExceptionDefinition exceptionDef = onException(ResourceNotFoundException.class)
                .handled(true)
                .log(LoggingLevel.INFO, "ResourceNotFoundException...")
                // use HTTP status 404 when data was not found
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
                .setBody(simple("${exception.message}\n"));

        return(exceptionDef);
    }

    protected OnExceptionDefinition getResourceUpdateException() {
        OnExceptionDefinition exceptionDef = onException(ResourceUpdateException.class)
                .handled(true)
                .log(LoggingLevel.INFO, "ResourceUpdateException...")
                // use HTTP status 404 when data was not found
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                .setBody(simple("${exception.message}\n"));

        return(exceptionDef);
    }

    protected OnExceptionDefinition getGeneralException() {
        OnExceptionDefinition exceptionDef = onException(Exception.class)
                .handled(true)
                // use HTTP status 500 when we had a server side error
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setBody(simple("${exception.message}\n"));
        return (exceptionDef);
    }
}
