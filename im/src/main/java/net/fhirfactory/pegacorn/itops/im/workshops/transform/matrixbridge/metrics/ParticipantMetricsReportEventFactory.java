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
package net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.metrics;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MRoomMessageTypeEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MTextContentType;
import net.fhirfactory.pegacorn.communicate.matrixbridge.workshops.matrixbridge.common.RoomServerTransactionIDProvider;
import net.fhirfactory.dricats.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.model.petasos.oam.metrics.reporting.PetasosComponentMetric;
import net.fhirfactory.dricats.model.petasos.oam.metrics.reporting.PetasosComponentMetricSet;
import net.fhirfactory.dricats.model.petasos.oam.metrics.reporting.valuesets.PetasosComponentMetricTypeEnum;
import net.fhirfactory.dricats.model.petasos.oam.topology.valuesets.PetasosMonitoredComponentTypeEnum;
import net.fhirfactory.pegacorn.itops.im.datatypes.MetricsReportContentBase;
import net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common.DefaultMetricsReportContentBodyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ParticipantMetricsReportEventFactory extends DefaultMetricsReportContentBodyFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantMetricsReportEventFactory.class);

    private DateTimeFormatter timeFormatter;

    @Inject
    private RoomServerTransactionIDProvider transactionIdProvider;

    @Inject
    private MatrixAccessToken matrixAccessToken;

    //
    // Constructor(s)
    //

    public ParticipantMetricsReportEventFactory(){
        timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS").withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));
    }

    //
    // Business Methods
    //


    public List<MRoomTextMessageEvent> createProcessingPlantMetricsEvent(String roomId, PetasosComponentMetricSet metricSet){
        getLogger().debug(".createProcessingPlantMetricsEvent(): Entry, metricSet->{}", metricSet);
        if(metricSet == null){
            getLogger().debug(".createProcessingPlantMetricsEvent(): Exit, metricSet is null, returning empty list");
            return(new ArrayList<>());
        }

        List<MRoomTextMessageEvent> metricsEventList = new ArrayList<>();

        MRoomTextMessageEvent currentMetricEvent = new MRoomTextMessageEvent();
        currentMetricEvent.setRoomIdentifier(roomId);
        currentMetricEvent.setEventIdentifier(transactionIdProvider.getNextAvailableID());
        currentMetricEvent.setSender(matrixAccessToken.getUserId());
        currentMetricEvent.setEventType("m.room.message");

        MetricsReportContentBase content = newDefaultMetricsContentReport(metricSet);

        MTextContentType textContent = new MTextContentType();
        textContent.setBody(content.getUnformatedText());
        textContent.setFormattedBody(content.getHtmlText());
        textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());
        textContent.setFormat("org.matrix.custom.html");

        currentMetricEvent.setContent(textContent);

        metricsEventList.add(currentMetricEvent);

        getLogger().debug(".createWorkUnitProcessorMetricsEvent(): Exit, metricsEventList->{}", metricsEventList);
        return(metricsEventList);
    }

    public List<MRoomTextMessageEvent> createEndpointMetricsEvent(String roomId, PetasosComponentMetricSet metricSet){
        getLogger().debug(".createEndpointMetricsEvent(): Entry, metricSet->{}", metricSet);
        if(metricSet == null){
            getLogger().debug(".createEndpointMetricsEvent(): Exit, metricSet is null, returning empty list");
            return(new ArrayList<>());
        }

        List<MRoomTextMessageEvent> metricsEventList = new ArrayList<>();

        MRoomTextMessageEvent currentMetricEvent = new MRoomTextMessageEvent();
        currentMetricEvent.setRoomIdentifier(roomId);
        currentMetricEvent.setEventIdentifier(transactionIdProvider.getNextAvailableID());
        currentMetricEvent.setSender(matrixAccessToken.getUserId());
        currentMetricEvent.setEventType("m.room.message");

        MetricsReportContentBase content = newDefaultMetricsContentReport(metricSet);

        MTextContentType textContent = new MTextContentType();
        textContent.setBody(content.getUnformatedText());
        textContent.setFormattedBody(content.getHtmlText());
        textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());
        textContent.setFormat("org.matrix.custom.html");

        currentMetricEvent.setContent(textContent);

        metricsEventList.add(currentMetricEvent);

        getLogger().debug(".createEndpointMetricsEvent(): Exit, metricsEventList->{}", metricsEventList);
        return(metricsEventList);
    }


    public List<MRoomTextMessageEvent> createWorkUnitProcessorMetricsEvent(String roomId, PetasosComponentMetricSet metricSet){
        getLogger().debug(".createWorkUnitProcessorMetricsEvent(): Entry, metricSet->{}", metricSet);
        if(metricSet == null){
            getLogger().debug(".createWorkUnitProcessorMetricsEvent(): Exit, metricSet is null, returning empty list");
            return(new ArrayList<>());
        }

        List<MRoomTextMessageEvent> metricsEventList = new ArrayList<>();


        if(metricSet.getComponentType().equals(PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_WORK_UNIT_PROCESSOR)){
            MRoomTextMessageEvent durationEvent = newTaskProcessingMetrics(roomId, metricSet);
            if(durationEvent != null){
                metricsEventList.add(durationEvent);
            }
            MRoomTextMessageEvent taskCountEvent = newTaskCountMetricReport(roomId, metricSet);
            if(durationEvent != null){
                metricsEventList.add(taskCountEvent);
            }
        }

        getLogger().debug(".createWorkUnitProcessorMetricsEvent(): Exit, metricsEventList->{}", metricsEventList);
        return(metricsEventList);
    }

    protected MRoomTextMessageEvent newTaskProcessingMetrics(String roomId, PetasosComponentMetricSet metricSet){
        getLogger().debug(".newTaskProcessingMetrics(): Entry, metricSet->{}", metricSet);
        if(metricSet == null){
            getLogger().debug(".newTaskProcessingMetrics(): Exit, metricSet is null, returning empty list");
            return(null);
        }

        MRoomTextMessageEvent currentMetricEvent = new MRoomTextMessageEvent();

        currentMetricEvent.setRoomIdentifier(roomId);
        currentMetricEvent.setEventIdentifier(transactionIdProvider.getNextAvailableID());
        currentMetricEvent.setSender(matrixAccessToken.getUserId());
        currentMetricEvent.setEventType("m.room.message");

        PetasosComponentMetric lastTaskProcessingTimeMetric = metricSet.getMetric(PetasosComponentMetricTypeEnum.LAST_TASK_PROCESSING_TIME.getDisplayName());
        PetasosComponentMetric rollingAverageProcessingTimeMetric = metricSet.getMetric(PetasosComponentMetricTypeEnum.ROLLING_TASK_PROCESSING_TIME.getDisplayName());
        PetasosComponentMetric cumulativeAverageProcessingTimeMetric = metricSet.getMetric(PetasosComponentMetricTypeEnum.CUMULATIVE_TASK_PROCESSING_TIME.getDisplayName());

        String lastTaskProcessingTime = "-";
        if(lastTaskProcessingTimeMetric != null){
            lastTaskProcessingTime = getMetricValueAsString(lastTaskProcessingTimeMetric.getMetricValue());
        }
        String rollingAverageProcessing = "-";
        if(rollingAverageProcessingTimeMetric != null){
            rollingAverageProcessing = getMetricValueAsString(rollingAverageProcessingTimeMetric.getMetricValue());
        }
        String cumulativeAverageProcessingTime = "-";
        if(cumulativeAverageProcessingTimeMetric != null){
            cumulativeAverageProcessingTime = getMetricValueAsString(cumulativeAverageProcessingTimeMetric.getMetricValue());
        }

        StringBuilder metricTextBodyBuilder = new StringBuilder();
        metricTextBodyBuilder.append("--- Task Processing Duration ---");
        metricTextBodyBuilder.append(PetasosComponentMetricTypeEnum.LAST_TASK_PROCESSING_TIME.getDisplayName() + " --> "+lastTaskProcessingTime);
        metricTextBodyBuilder.append(PetasosComponentMetricTypeEnum.ROLLING_TASK_PROCESSING_TIME.getDisplayName() + " --> "+rollingAverageProcessing);
        metricTextBodyBuilder.append(PetasosComponentMetricTypeEnum.CUMULATIVE_TASK_PROCESSING_TIME.getDisplayName() + " --> "+cumulativeAverageProcessingTime);
        metricTextBodyBuilder.append("--------------------------------");

        StringBuilder metricFormattedTextBodyBuilder = new StringBuilder();
        metricFormattedTextBodyBuilder.append("<hr width=50%>");
        metricFormattedTextBodyBuilder.append("<b>Work Unit Processor Task Processing Durations ("+timeFormatter.format(metricSet.getReportingInstant())+")</b>");
        metricFormattedTextBodyBuilder.append("<table style='width:100%'>");
        metricFormattedTextBodyBuilder.append("<tr>");
        metricFormattedTextBodyBuilder.append("<th>"+PetasosComponentMetricTypeEnum.LAST_TASK_PROCESSING_TIME.getDisplayName()+"</th>");
        metricFormattedTextBodyBuilder.append("<th>"+PetasosComponentMetricTypeEnum.ROLLING_TASK_PROCESSING_TIME.getDisplayName()+"</th>");
        metricFormattedTextBodyBuilder.append("<th>"+PetasosComponentMetricTypeEnum.CUMULATIVE_TASK_PROCESSING_TIME.getDisplayName()+"</th>");
        metricFormattedTextBodyBuilder.append("</tr>");

        metricFormattedTextBodyBuilder.append("<tr>");
        metricFormattedTextBodyBuilder.append("<td>"+lastTaskProcessingTime+"</td>");
        metricFormattedTextBodyBuilder.append("<td>"+rollingAverageProcessing+"</td>");
        metricFormattedTextBodyBuilder.append("<td>"+cumulativeAverageProcessingTime+"</td>");
        metricFormattedTextBodyBuilder.append("</tr>");
        metricFormattedTextBodyBuilder.append("</table>");

        MTextContentType textContent = new MTextContentType();
        textContent.setBody(metricTextBodyBuilder.toString());
        textContent.setFormattedBody(metricFormattedTextBodyBuilder.toString());
        textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());
        textContent.setFormat("org.matrix.custom.html");

        currentMetricEvent.setContent(textContent);

        return(currentMetricEvent);
    }

    protected MRoomTextMessageEvent newTaskCountMetricReport(String roomId, PetasosComponentMetricSet metricSet){
        getLogger().debug(".newTaskCountMetricReport(): Entry, metricSet->{}", metricSet);
        if(metricSet == null){
            getLogger().debug(".newTaskCountMetricReport(): Exit, metricSet is null, returning empty list");
            return(null);
        }

        MRoomTextMessageEvent currentMetricEvent = new MRoomTextMessageEvent();
        currentMetricEvent.setRoomIdentifier(roomId);
        currentMetricEvent.setEventIdentifier(transactionIdProvider.getNextAvailableID());
        currentMetricEvent.setSender(matrixAccessToken.getUserId());
        currentMetricEvent.setEventType("m.room.message");

        PetasosComponentMetric registrationCountMetric = metricSet.getMetric(PetasosComponentMetricTypeEnum.REGISTERED_TASK_COUNT.getDisplayName());
        PetasosComponentMetric startedCountMetric = metricSet.getMetric(PetasosComponentMetricTypeEnum.STARTED_TASK_COUNT.getDisplayName());
        PetasosComponentMetric finishedCountMetric = metricSet.getMetric(PetasosComponentMetricTypeEnum.FINISHED_TASK_COUNT.getDisplayName());
        PetasosComponentMetric failedCountMetric = metricSet.getMetric(PetasosComponentMetricTypeEnum.FAILED_TASK_COUNT.getDisplayName());
        PetasosComponentMetric finalisedCountMetric = metricSet.getMetric(PetasosComponentMetricTypeEnum.FINALISED_TASK_COUNT.getDisplayName());
        PetasosComponentMetric cancelledCountMetric = metricSet.getMetric(PetasosComponentMetricTypeEnum.FINALISED_TASK_COUNT.getDisplayName());

        String registrationCount = "-";
        if(registrationCountMetric != null){
            registrationCount = getMetricValueAsString(registrationCountMetric.getMetricValue());
        }
        String startedCount = "-";
        if(startedCountMetric != null){
            startedCount = getMetricValueAsString(startedCountMetric.getMetricValue());
        }
        String finishedCount = "-";
        if(finishedCountMetric != null){
            finishedCount = getMetricValueAsString(finishedCountMetric.getMetricValue());
        }
        String finalisedCount = "-";
        if(finalisedCountMetric != null){
            finalisedCount = getMetricValueAsString(finalisedCountMetric.getMetricValue());
        }
        String failedCount = "-";
        if(failedCountMetric != null){
            failedCount = getMetricValueAsString(failedCountMetric.getMetricValue());
        }
        String cancelledCount = "-";
        if(cancelledCountMetric != null){
            cancelledCount = getMetricValueAsString(cancelledCountMetric.getMetricValue());
        }

        StringBuilder metricTextBodyBuilder = new StringBuilder();
        metricTextBodyBuilder.append("--- Task Processing Counts ---");
        metricTextBodyBuilder.append(PetasosComponentMetricTypeEnum.REGISTERED_TASK_COUNT.getDisplayName() + " --> "+registrationCount);
        metricTextBodyBuilder.append(PetasosComponentMetricTypeEnum.STARTED_TASK_COUNT.getDisplayName() + " --> "+startedCount);
        metricTextBodyBuilder.append(PetasosComponentMetricTypeEnum.FINISHED_TASK_COUNT.getDisplayName() + " --> "+finishedCount);
        metricTextBodyBuilder.append(PetasosComponentMetricTypeEnum.FINALISED_TASK_COUNT.getDisplayName() + " --> "+finalisedCount);
        metricTextBodyBuilder.append(PetasosComponentMetricTypeEnum.CANCELLED_TASK_COUNT.getDisplayName() + " --> "+cancelledCount);
        metricTextBodyBuilder.append(PetasosComponentMetricTypeEnum.FAILED_TASK_COUNT.getDisplayName() + " --> "+failedCount);
        metricTextBodyBuilder.append("------------------------------");


        StringBuilder metricFormattedTextBodyBuilder = new StringBuilder();
        metricFormattedTextBodyBuilder.append("<b>Work Unit Processor Task Counters ("+timeFormatter.format(metricSet.getReportingInstant())+")</b>");
        metricFormattedTextBodyBuilder.append("<table style='width:100%'>");
        metricFormattedTextBodyBuilder.append("<tr align=center>");
        metricFormattedTextBodyBuilder.append("<th>"+PetasosComponentMetricTypeEnum.REGISTERED_TASK_COUNT.getDisplayName()+"</th>");
        metricFormattedTextBodyBuilder.append("<th>"+PetasosComponentMetricTypeEnum.STARTED_TASK_COUNT.getDisplayName()+"</th>");
        metricFormattedTextBodyBuilder.append("<th>"+PetasosComponentMetricTypeEnum.FINISHED_TASK_COUNT.getDisplayName()+"</th>");
        metricFormattedTextBodyBuilder.append("<th>"+PetasosComponentMetricTypeEnum.FINALISED_TASK_COUNT.getDisplayName()+"</th>");
        metricFormattedTextBodyBuilder.append("<th>"+PetasosComponentMetricTypeEnum.FAILED_TASK_COUNT.getDisplayName()+"</th>");
        metricFormattedTextBodyBuilder.append("<th>"+PetasosComponentMetricTypeEnum.CANCELLED_TASK_COUNT.getDisplayName()+"</th>");
        metricFormattedTextBodyBuilder.append("</tr>");

        metricFormattedTextBodyBuilder.append("<tr align=center>");
        metricFormattedTextBodyBuilder.append("<td>"+registrationCount+"</td>");
        metricFormattedTextBodyBuilder.append("<td>"+startedCount+"</td>");
        metricFormattedTextBodyBuilder.append("<td>"+finishedCount+"</td>");
        metricFormattedTextBodyBuilder.append("<td>"+finalisedCount+"</td>");
        metricFormattedTextBodyBuilder.append("<td>"+failedCount+"</td>");
        metricFormattedTextBodyBuilder.append("<td>"+cancelledCount+"</td>");
        metricFormattedTextBodyBuilder.append("</tr>");
        metricFormattedTextBodyBuilder.append("</table>");

        MTextContentType textContent = new MTextContentType();
        textContent.setBody(metricTextBodyBuilder.toString());
        textContent.setFormattedBody(metricFormattedTextBodyBuilder.toString());
        textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());
        textContent.setFormat("org.matrix.custom.html");

        currentMetricEvent.setContent(textContent);

        return(currentMetricEvent);
    }

    //
    // Getters and Setters
    //

    @Override
    protected Logger getLogger(){
        return(LOG);
    }

    protected DateTimeFormatter getTimeFormatter(){
        return(this.timeFormatter);
    }

}
