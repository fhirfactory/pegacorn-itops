package net.fhirfactory.pegacorn.itops.im.processingplant.configuration;

import net.fhirfactory.pegacorn.communicate.matrixbridge.processingplant.configuration.MatrixBridgeTopologyFactory;
import net.fhirfactory.pegacorn.core.model.topology.nodes.*;
import net.fhirfactory.pegacorn.core.model.topology.nodes.common.EndpointProviderInterface;
import net.fhirfactory.pegacorn.deployment.properties.configurationfilebased.common.segments.ports.http.ClusteredHTTPServerPortSegment;
import net.fhirfactory.pegacorn.itops.im.common.ITOpsIMNames;
import net.fhirfactory.pegacorn.util.PegacornEnvironmentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ITOpsIMTopologyFactory extends MatrixBridgeTopologyFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsIMTopologyFactory.class);

    @Inject
    private ITOpsIMNames names;

    @Inject
    private PegacornEnvironmentProperties pegacornEnvironmentProperties;

    @Override
    protected Logger specifyLogger() {
        return (LOG);
    }

    @Override
    protected Class specifyPropertyFileClass() {
        return (ITOpsIMConfigurationFile.class);
    }

    @Override
    protected ProcessingPlantSoftwareComponent buildSubsystemTopology() {
        SubsystemTopologyNode subsystemTopologyNode = buildSubsystemNodeFromConfigurationFile();
        BusinessServiceTopologyNode businessServiceTopologyNode = buildBusinessServiceNode(subsystemTopologyNode);
        DeploymentSiteTopologyNode deploymentSiteTopologyNode = buildDeploymentSiteNode(businessServiceTopologyNode);
        ClusterServiceTopologyNode clusterServiceTopologyNode = buildClusterServiceNode(deploymentSiteTopologyNode, businessServiceTopologyNode);

        PlatformTopologyNode platformTopologyNode = buildPlatformNode(clusterServiceTopologyNode);
        ProcessingPlantSoftwareComponent processingPlantSoftwareComponent = buildProcessingPlant(platformTopologyNode, clusterServiceTopologyNode);
        addPrometheusPort(processingPlantSoftwareComponent);
        addJolokiaPort(processingPlantSoftwareComponent);
        addKubeLivelinessPort(processingPlantSoftwareComponent);
        addKubeReadinessPort(processingPlantSoftwareComponent);
        addEdgeAnswerPort(processingPlantSoftwareComponent);
        addAllJGroupsEndpoints(processingPlantSoftwareComponent);

        // Unique to ITOpsIM
        getLogger().trace(".buildSubsystemTopology(): Add the HTTP Server port to the ProcessingPlant Topology Node");
        addHTTPServerPorts(processingPlantSoftwareComponent);

        // For the Matrix Integration Services
        getLogger().trace(".buildSubsystemTopology(): Add the HTTP Server port to the ProcessingPlant Topology Node");
        addMatrixEventsReceiver(processingPlantSoftwareComponent);
        getLogger().trace(".buildSubsystemTopology(): Add the HTTP Client ports to the ProcessingPlant Topology Node");
        addMatrixActionsClient(processingPlantSoftwareComponent);
        addMatrixQueryClient(processingPlantSoftwareComponent);
        addSynapseAdminClientEndpoint(processingPlantSoftwareComponent);
        return(processingPlantSoftwareComponent);
    }

    protected void addHTTPServerPorts( EndpointProviderInterface endpointProvider) {
        getLogger().debug(".addHTTPServerPorts(): Entry, endpointProvider->{}", endpointProvider);

        getLogger().trace(".addHTTPServerPorts(): Creating the HTTP Server");
        ClusteredHTTPServerPortSegment interactHTTPServer = ((ITOpsIMConfigurationFile) getPropertyFile()).getItopsServerSegment();
        getHTTPTopologyEndpointFactory().newHTTPServerTopologyEndpoint(getPropertyFile(), endpointProvider, names.getInteractITOpsIMHTTPServerName(),interactHTTPServer );

        getLogger().debug(".addHTTPServerPorts(): Exit");
    }

    protected String specifyPropertyFileName() {
        LOG.info(".specifyPropertyFileName(): Entry");
        String configurationFileName = pegacornEnvironmentProperties.getMandatoryProperty("DEPLOYMENT_CONFIG_FILE");
        if(configurationFileName == null){
            throw(new RuntimeException("Cannot load configuration file!!!! (SUBSYSTEM-CONFIG_FILE="+configurationFileName+")"));
        }
        LOG.trace(".specifyPropertyFileName(): Exit, filename->{}", configurationFileName);
        LOG.info(".specifyPropertyFileName(): Exit");
        return configurationFileName;
    }
}
