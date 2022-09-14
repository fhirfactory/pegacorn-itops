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
package net.fhirfactory.pegacorn.itops.im.workshops.issi.topology;

import net.fhirfactory.pegacorn.communicate.matrix.model.r110.api.common.MAPIResponse;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.model.petasos.endpoint.JGroupsIntegrationPointNamingUtilities;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.valuesets.PetasosComponentITOpsNotificationTypeEnum;
import net.fhirfactory.pegacorn.itops.im.common.ITOpsIMNames;
import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;
import net.fhirfactory.pegacorn.itops.im.workshops.issi.common.OAMRoomMessageInjectorBase;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.notifications.ParticipantNotificationEventFactory;
import net.fhirfactory.pegacorn.petasos.endpoints.services.topology.PetasosDistributedSoftwareComponentMapDM;
import net.fhirfactory.pegacorn.petasos.endpoints.services.topology.PetasosTopologyServicesEndpoint;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@ApplicationScoped
public class ParticipantTopologyWatchdog extends OAMRoomMessageInjectorBase {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantTopologyWatchdog.class);

    private boolean initialised;

    private boolean firstRunComplete;

    private boolean topologyConnectivityCheckDaemonIsStillRunning;
    private Instant topologyConnectivityCheckDaemonLastRunTime;
    private Instant topologyConnectivityFullReportInstant;

    private List<String> lastSeenEndpoints;

    private DateTimeFormatter timeFormatter;

    private Long ENDPOINT_CONNECTIVITY_CHECK_WATCHDOG_STARTUP_DELAY = 120000L; // Milliseconds
    private Long ENDPOINT_CONNECTIVITY_CHECK_WATCHDOG_PERIOD = 30000L; // Milliseconds
    private Long ENDPOINT_CONNECTIVITY_CHECK_OVERRIDE_PERIOD = 900L; // Seconds
    private Long ENDPOINT_CONNECTIVITY_FULL_REPORT_PERIOD = 900L; // Seconds

    @Inject
    private PetasosDistributedSoftwareComponentMapDM probedNodeMap;

    @Inject
    private ITOpsIMNames itOpsIMNames;

    @Inject
    private PetasosTopologyServicesEndpoint topologyServicesEndpoint;

    @Inject
    private ProcessingPlantInterface processingPlant;

    @Inject
    private ParticipantNotificationEventFactory notificationEventFactory;

    @Inject
    private JGroupsIntegrationPointNamingUtilities jgroupsIPNamingUtilities;

    @Inject
    private ProducerTemplate camelRouteInjector;

    //
    // Constructor(s)
    //

    public ParticipantTopologyWatchdog() {
        super();
        this.initialised = false;
        this.firstRunComplete = false;
        this.topologyConnectivityCheckDaemonIsStillRunning = false;
        this.topologyConnectivityCheckDaemonLastRunTime = Instant.EPOCH;
        this.timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));
        this.lastSeenEndpoints = new ArrayList<>();
        this.topologyConnectivityFullReportInstant = Instant.EPOCH;
    }

    //
    // Post Construct
    //

    @PostConstruct
    public void initialise() {
        getLogger().debug(".initialise(): Entry");
        if (initialised) {
            getLogger().debug(".initialise(): Exit, already initialised, nothing to do");
            return;
        }
        getLogger().info(".initialise(): Initialisation Start...");

        scheduleConnectivityCheckDaemon();

        this.initialised = true;

        getLogger().info(".initialise(): Initialisation Finish...");
    }

    //
    // Check Endpoint Scheduler
    //

    protected void scheduleConnectivityCheckDaemon(){
        getLogger().debug(".scheduleConnectivityCheckDaemon");
        TimerTask connectivityCheckDaemonTimerTask = new TimerTask() {
            public void run() {
                getLogger().debug(".connectivityCheckDaemonTimerTask(): Entry");
                if (!isTopologyConnectivityCheckDaemonIsStillRunning()) {
                    connectivityCheckDaemon();
                    setTopologyConnectivityCheckDaemonLastRunTime(Instant.now());
                } else {
                    Long ageSinceRun = Instant.now().getEpochSecond() - getTopologyConnectivityCheckDaemonLastRunTime().getEpochSecond();
                    if (ageSinceRun > getEndpointConnectivityCheckOverridePeriod()) {
                        connectivityCheckDaemon();
                    }
                }
                getLogger().debug(".connectivityCheckDaemonTimerTask(): Exit");
            }
        };
        Timer timer = new Timer("ConnectivityCheckDaemonTimer");
        timer.schedule(connectivityCheckDaemonTimerTask, getEndpointConnectivityCheckWatchdogStartupDelay(), getEndpointConnectivityCheckWatchdogPeriod());
        getLogger().debug(".scheduleConnectivityCheckDaemon(): Exit");
    }

    //
    // Check Endpoint Daemon
    //

    protected void connectivityCheckDaemon(){
        getLogger().debug(".connectivityCheckDaemon(): Start");
        setTopologyConnectivityCheckDaemonIsStillRunning(true);

        try {
            List<String> allClusterMembers = topologyServicesEndpoint.getAllClusterMembers();
            if(allClusterMembers != null) {
                PetasosComponentITOpsNotification connectivityNotification = null;
                Long timeSinceLastFullReport = Instant.now().getEpochSecond() - getTopologyConnectivityFullReportInstant().getEpochSecond();
                if (timeSinceLastFullReport > getEndpointConnectivityFullReportPeriod()) {
                    connectivityNotification = buildFullConnectivityReport(allClusterMembers);
                    setTopologyConnectivityFullReportInstant(Instant.now());
                } else {
                    if (someDeltaToReport(allClusterMembers)) {
                        connectivityNotification = buildDeltaConnectivityReport(allClusterMembers);
                        sendSubsystemStatusCommunicateNotifications(allClusterMembers);
                        resetLastSeenEndpoints(allClusterMembers);
                    }
                }
                if (connectivityNotification != null) {
                    sendConnectivityReport(connectivityNotification);
                }
            }
        } catch(Exception ex){
            getLogger().error(".connectivityCheckDaemon(): Daemon Failed: Error Message->{}, stackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }

        setTopologyConnectivityCheckDaemonIsStillRunning(false);
        getLogger().debug(".buildConnectivityReport(): Entry");
    }

    protected boolean someDeltaToReport(List<String> allClusterMembers){
        if(allClusterMembers.size() != getLastSeenEndpoints().size()){
            return(true);
        }
        for(String inListNode: getLastSeenEndpoints()){
            if(allClusterMembers.contains(inListNode)){
                // do nothing
            } else {
                return(true);
            }
        }
        for(String inClusterNode: allClusterMembers){
            if(getLastSeenEndpoints().contains(inClusterNode)){
                // do nothing
            } else {
                return(true);
            }
        }
        return(false);
    }

    protected PetasosComponentITOpsNotification buildDeltaConnectivityReport(List<String> allClusterMembers){
        getLogger().debug(".buildConnectivityReport(): Entry");

        StringBuilder reportBuilder = new StringBuilder();
        StringBuilder formattedReportBuilder = new StringBuilder();

        reportBuilder.append("- Node (Delta) Visibility Report: " + getTimeFormatter().format(Instant.now()) + "-------\n");

        formattedReportBuilder.append("<b> Node (Delta) Visibility Report: "+getTimeFormatter().format(Instant.now()) + " </b>");

        formattedReportBuilder.append("<table>");
        formattedReportBuilder.append("<tr>");
        formattedReportBuilder.append("<th>Event</th><th>Site</th><th>Zone</th><th>Subsystem</th><th>UUID</th>");
        formattedReportBuilder.append("</tr>");

        for(String currentNode: allClusterMembers) {
            if(!getLastSeenEndpoints().contains(currentNode)) {
                reportBuilder.append("Added Node --> " + currentNode + "\n");
                formattedReportBuilder.append("<tr>");
                formattedReportBuilder.append("<td>Added</td>");
                formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointSiteFromChannelName(currentNode)+"</td>");
                formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointZoneFromChannelName(currentNode)+"</td>");
                formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointSubsystemNameFromChannelName(currentNode)+"</td>");
                formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointUniqueIDFromChannelName(currentNode)+"</td>");
                formattedReportBuilder.append("</tr>");
            }
        }

        for(String currentKnownNode: getLastSeenEndpoints()) {
            if(!allClusterMembers.contains(currentKnownNode)) {
                reportBuilder.append("Removed Node --> " + currentKnownNode + "\n");
                formattedReportBuilder.append("<tr>");
                formattedReportBuilder.append("<td>Removed</td>");
                formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointSiteFromChannelName(currentKnownNode)+"</td>");
                formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointZoneFromChannelName(currentKnownNode)+"</td>");
                formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointSubsystemNameFromChannelName(currentKnownNode)+"</td>");
                formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointUniqueIDFromChannelName(currentKnownNode)+"</td>");
                formattedReportBuilder.append("</tr>");
            }
        }

        formattedReportBuilder.append("</table>");

        String report = reportBuilder.toString();
        String formattedReport = formattedReportBuilder.toString();

        PetasosComponentITOpsNotification notification = new PetasosComponentITOpsNotification();
        notification.setContent(report);
        notification.setFormattedContent(formattedReport);
        notification.setParticipantName(processingPlant.getSubsystemParticipantName());
        notification.setNotificationType(PetasosComponentITOpsNotificationTypeEnum.SUCCESS_NOTIFICATION_TYPE);
        notification.setComponentId(processingPlant.getMeAsASoftwareComponent().getComponentID());
        notification.setContentHeading("Node Visibility Report");

        getLogger().debug(".buildConnectivityReport(): Exit");
        return(notification);
    }

    protected void resetLastSeenEndpoints(List<String> allClusterMembers){
        getLogger().debug(".resetLastSeenEndpoints(): Entry");
        getLastSeenEndpoints().clear();
        getLastSeenEndpoints().addAll(allClusterMembers);
        getLogger().debug(".resetLastSeenEndpoints(): Exit");
    }

    protected void sendSubsystemStatusCommunicateNotifications(List<String> allClusterMembers){
        getLogger().debug(".sendSubsystemStatusCommunicateNotifications(): Entry");
        for(String currentNode: allClusterMembers) {
            if(!getLastSeenEndpoints().contains(currentNode)) {
                sendCommunicateNotification(currentNode, "Started");
            }
        }

        for(String currentKnownNode: getLastSeenEndpoints()) {
            if(!allClusterMembers.contains(currentKnownNode)) {
                sendCommunicateNotification(currentKnownNode, "Stopped");
            }
        }
        getLogger().debug(".sendSubsystemStatusCommunicateNotifications(): Exit");
    }

    protected void sendCommunicateNotification(String subsystemNode, String status){
        getLogger().debug(".sendCommunicateNotification(): Entry");
        PetasosComponentITOpsNotification notification = new PetasosComponentITOpsNotification();

        String nodeSite = getJGroupsIPNamingUtilities().getEndpointSiteFromChannelName(subsystemNode);
        String nodeZone = getJGroupsIPNamingUtilities().getEndpointZoneFromChannelName(subsystemNode);
        String nodeSubsystemName = getJGroupsIPNamingUtilities().getEndpointSubsystemNameFromChannelName(subsystemNode);
        String nodeUniqueId = getJGroupsIPNamingUtilities().getEndpointUniqueIDFromChannelName(subsystemNode);

        StringBuilder formattedReportBuilder = new StringBuilder();
        formattedReportBuilder.append("<table>");
        formattedReportBuilder.append("<tr>");
        formattedReportBuilder.append("<th>Event</th><th>Site</th><th>Zone</th><th>Subsystem</th><th>UUID</th>");
        formattedReportBuilder.append("</tr>");
        formattedReportBuilder.append("<tr>");
        formattedReportBuilder.append("<th>"+ status + "</th><th>" + nodeSite + "</th><th>" + nodeZone + "</th><th>" + nodeSubsystemName + "</th><th>" + nodeUniqueId + "</th>");
        formattedReportBuilder.append("</tr>");
        formattedReportBuilder.append("</table>");
        String formattedReport = formattedReportBuilder.toString();

        notification.setContent("Node: " + nodeSite + "." + nodeZone + "." + nodeSubsystemName + "." + nodeUniqueId + "  has " + status);
        notification.setFormattedContent(formattedReport);
        notification.setParticipantName(nodeSubsystemName);
        notification.setNotificationType(PetasosComponentITOpsNotificationTypeEnum.NORMAL_NOTIFICATION_TYPE);
        notification.setComponentId(processingPlant.getMeAsASoftwareComponent().getComponentID());
        notification.setContentHeading("Node: " + nodeSite + "." + nodeZone + "." + nodeSubsystemName  + "  has " + status);

        try {
            camelRouteInjector.sendBody(itOpsIMNames.getITOpsNotificationToCommunicateMessageIngresFeed(), ExchangePattern.InOnly, notification);
        } catch (Exception ex){
            getLogger().warn(".sendConnectivityReport(): Failed to send CommunicateMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }

        getLogger().debug(".sendCommunicateNotification(): Exit");
    }

    protected PetasosComponentITOpsNotification buildFullConnectivityReport(List<String> allClusterMembers){
        getLogger().debug(".buildConnectivityReport(): Entry");

        StringBuilder reportBuilder = new StringBuilder();
        StringBuilder formattedReportBuilder = new StringBuilder();

        reportBuilder.append("- Node (Full) Visibility Report: " + getTimeFormatter().format(Instant.now()) + "-------\n");
        formattedReportBuilder.append("<b> Node Visibility Report: "+getTimeFormatter().format(Instant.now()) + " </b>");

        formattedReportBuilder.append("<table>");
        formattedReportBuilder.append("<tr>");
        formattedReportBuilder.append("<th>Site</th><th>Zone</th><th>Subsystem</th><th>UUID</th>");
        formattedReportBuilder.append("</tr>");

        for(String currentNode: allClusterMembers) {
            reportBuilder.append("Node --> " + currentNode + "\n");
            formattedReportBuilder.append("<tr>");
            formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointSiteFromChannelName(currentNode)+"</td>");
            formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointZoneFromChannelName(currentNode)+"</td>");
            formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointSubsystemNameFromChannelName(currentNode)+"</td>");
            formattedReportBuilder.append("<td>"+getJGroupsIPNamingUtilities().getEndpointUniqueIDFromChannelName(currentNode)+"</td>");
            formattedReportBuilder.append("</tr>");
        }

        formattedReportBuilder.append("</table>");

        String report = reportBuilder.toString();
        String formattedReport = formattedReportBuilder.toString();

        PetasosComponentITOpsNotification notification = new PetasosComponentITOpsNotification();
        notification.setContent(report);
        notification.setFormattedContent(formattedReport);
        notification.setParticipantName(processingPlant.getSubsystemParticipantName());
        notification.setNotificationType(PetasosComponentITOpsNotificationTypeEnum.SUCCESS_NOTIFICATION_TYPE);
        notification.setComponentId(processingPlant.getMeAsASoftwareComponent().getComponentID());
        notification.setContentHeading("Node Visibility Report");

        getLogger().debug(".buildConnectivityReport(): Exit");
        return(notification);
    }

    protected void sendConnectivityReport(PetasosComponentITOpsNotification notification){
        getLogger().debug(".sendConnectivityReport(): Entry, notification->{}", notification);
        try {
            String roomAlias = getRoomIdentityFactory().buildProcessingPlantRoomPseudoAlias(notification.getParticipantName(),OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM_CONSOLE);

            getLogger().debug(".forwardWUPNotification(): roomAlias for Events->{}", roomAlias);

            String roomIdFromAlias =  getRoomIdFromPseudoAlias(roomAlias);

            getLogger().debug(".forwardWUPNotification(): roomId for Events->{}", roomIdFromAlias);

            if(roomIdFromAlias != null) {
                getLogger().debug(".sendConnectivityReport(): [Building MessageEvent] Start");
                MRoomTextMessageEvent notificationEvent = notificationEventFactory.newNotificationEvent(roomIdFromAlias, notification);
                getLogger().debug(".sendConnectivityReport(): [Building MessageEvent] Finish");
                try {
                    getLogger().debug(".sendConnectivityReport(): [Sending MessageEvent] Start");
                    MAPIResponse mapiResponse = getMatrixInstantMessageAPI().postTextMessage(roomIdFromAlias, getMatrixAccessToken().getUserId(), notificationEvent);
                    getLogger().debug(".sendConnectivityReport(): [Sending MessageEvent] mapiResponse->{}", mapiResponse);
                    getLogger().debug(".sendConnectivityReport(): [Sending MessageEvent] Finish");
                } catch(Exception ex){
                    getLogger().warn(".sendConnectivityReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
                    return;
                }
            } else {
                getLogger().warn(".sendConnectivityReport(): No room to forward work unit processor notifications into (ProcessingPlant->{}), ITOps Room Pseudo Alias ->{}", notification.getParticipantName(), roomAlias);
                // TODO either re-queue or send to DeadLetter
                return;
            }
        } catch (Exception ex) {
            getLogger().warn(".sendConnectivityReport(): Failed to send InstantMessage, message->{}, stackTrace{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
            return;
        }
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected boolean isTopologyConnectivityCheckDaemonIsStillRunning() {
        return topologyConnectivityCheckDaemonIsStillRunning;
    }

    protected void setTopologyConnectivityCheckDaemonIsStillRunning(boolean topologyConnectivityCheckDaemonIsStillRunning) {
        this.topologyConnectivityCheckDaemonIsStillRunning = topologyConnectivityCheckDaemonIsStillRunning;
    }

    protected Instant getTopologyConnectivityCheckDaemonLastRunTime() {
        return topologyConnectivityCheckDaemonLastRunTime;
    }

    protected void setTopologyConnectivityCheckDaemonLastRunTime(Instant topologyConnectivityCheckDaemonLastRunTime) {
        this.topologyConnectivityCheckDaemonLastRunTime = topologyConnectivityCheckDaemonLastRunTime;
    }

    protected Long getEndpointConnectivityCheckWatchdogStartupDelay() {
        return ENDPOINT_CONNECTIVITY_CHECK_WATCHDOG_STARTUP_DELAY;
    }

    protected Long getEndpointConnectivityCheckOverridePeriod() {
        return ENDPOINT_CONNECTIVITY_CHECK_OVERRIDE_PERIOD;
    }

    protected Long getEndpointConnectivityCheckWatchdogPeriod(){
        return(ENDPOINT_CONNECTIVITY_CHECK_WATCHDOG_PERIOD);
    }

    protected DateTimeFormatter getTimeFormatter() {
        return timeFormatter;
    }
    protected Long getEndpointConnectivityFullReportPeriod(){
        return(ENDPOINT_CONNECTIVITY_FULL_REPORT_PERIOD);
    }

    protected List<String> getLastSeenEndpoints() {
        return lastSeenEndpoints;
    }

    protected Instant getTopologyConnectivityFullReportInstant(){
        return(this.topologyConnectivityFullReportInstant);
    }

    protected void setTopologyConnectivityFullReportInstant(Instant instant){
        this.topologyConnectivityFullReportInstant = instant;
    }

    protected JGroupsIntegrationPointNamingUtilities getJGroupsIPNamingUtilities(){
        return(this.jgroupsIPNamingUtilities);
    }
}
