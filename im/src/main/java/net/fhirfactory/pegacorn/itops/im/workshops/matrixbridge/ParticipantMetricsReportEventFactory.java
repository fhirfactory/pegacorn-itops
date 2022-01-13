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
package net.fhirfactory.pegacorn.itops.im.workshops.matrixbridge;

import net.fhirfactory.pegacorn.communicate.matrix.credentials.MatrixAccessToken;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.MRoomTextMessageEvent;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MRoomMessageTypeEnum;
import net.fhirfactory.pegacorn.communicate.matrix.model.r110.events.room.message.contenttypes.MTextContentType;
import net.fhirfactory.pegacorn.communicate.matrixbridge.workshops.matrixbridge.common.RoomServerTransactionIDProvider;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.petasos.oam.metrics.reporting.PetasosComponentMetric;
import net.fhirfactory.pegacorn.core.model.petasos.oam.metrics.reporting.PetasosComponentMetricSet;
import net.fhirfactory.pegacorn.core.model.petasos.oam.metrics.reporting.datatypes.PetasosComponentMetricValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ParticipantMetricsReportEventFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantMetricsReportEventFactory.class);

    @Inject
    private RoomServerTransactionIDProvider transactionIdProvider;

    @Inject
    private MatrixAccessToken accessToken;

    //
    // Business Methods
    //

    public List<MRoomTextMessageEvent> createWorkUnitProcessorMetricsEvent(String roomId, PetasosComponentMetricSet metricSet){
        getLogger().debug(".createWorkUnitProcessorMetricsEvent(): Entry, metricSet->{}", metricSet);
        if(metricSet == null){
            getLogger().debug(".createWorkUnitProcessorMetricsEvent(): Exit, metricSet is null, returning empty list");
            return(new ArrayList<>());
        }

        List<MRoomTextMessageEvent> metricsEventList = new ArrayList<>();
        MRoomTextMessageEvent currentMetricEvent = new MRoomTextMessageEvent();
        currentMetricEvent.setRoomIdentifier(roomId);
        currentMetricEvent.setEventIdentifier(transactionIdProvider.getNextAvailableID());
        currentMetricEvent.setSender(accessToken.getMatrixUserId());
        currentMetricEvent.setEventType("m.room.message");

        StringBuilder metricTextBodyBuilder = new StringBuilder();
        StringBuilder metricFormattedTextBodyBuilder = new StringBuilder();

        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));

        metricFormattedTextBodyBuilder.append("<table style='width:100%'> <tr><th>Timestamp</th><th>Metric Name</th><th>Metric Type</th><th>Matric Unit</th><th>Metric Value</th></tr>");
        for(PetasosComponentMetric currentMetric: metricSet.getMetrics().values()){
            String metricName = currentMetric.getMetricName();
            String metricType = null;
            if (currentMetric.hasMetricType()) {
                metricType = currentMetric.getMetricType().getDisplayName();
            } else {
                metricType = "Not Specified";
            }
            String metricUnit = null;
            if(currentMetric.hasMetricUnit()) {
                metricUnit = currentMetric.getMetricUnit().getDisplayName();
            } else {
                metricUnit = "Not Specified";
            }

            String metricValue = getMetricValueAsString(currentMetric.getMetricValue());
            String metricTimestamp = null;
            if(currentMetric.getMetricTimestamp() != null) {
                if(currentMetric.getMetricTimestamp().hasMeasurementCaptureInstant()) {
                    metricTimestamp = formatter.format(currentMetric.getMetricTimestamp().getMeasurementCaptureInstant());
                }
            }
            if(metricTimestamp == null){
                metricTimestamp = "Not Specified";
            }
            metricTextBodyBuilder.append(metricTimestamp+":"+metricName+":"+metricType+":"+metricUnit+":"+metricValue);
            metricFormattedTextBodyBuilder.append("<tr><td>"+metricTimestamp+"</td><td>"+metricName+"</td><td>"+metricType+"</td><td>"+metricUnit+"</td><td>"+metricValue+"</td></tr>");
        }

        MTextContentType textContent = new MTextContentType();
        textContent.setBody(metricTextBodyBuilder.toString());
        textContent.setFormattedBody(metricFormattedTextBodyBuilder.toString());
        textContent.setMessageType(MRoomMessageTypeEnum.TEXT.getMsgtype());
        textContent.setFormat("org.matrix.custom.html");

        currentMetricEvent.setContent(textContent);

        metricsEventList.add(currentMetricEvent);

        getLogger().debug(".createWorkUnitProcessorMetricsEvent(): Exit, metricsEventList->{}", metricsEventList);
        return(metricsEventList);
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Extract Metric Value
    //

    protected String getMetricValueAsString(PetasosComponentMetricValue metricValue){
        if(metricValue == null){
            return("NULL");
        }
        if(metricValue.getObjectType().equals(String.class)){
            return(metricValue.getStringValue());
        }
        if(metricValue.getObjectType().equals(Long.class)){
            Long value = metricValue.getLongValue();
            String valueAsString = value.toString();
            return(valueAsString);
        }
        if(metricValue.getObjectType().equals(Boolean.class)){
            Boolean value = metricValue.getBooleanValue();
            return(value.toString());
        }
        if(metricValue.getObjectType().equals(Integer.class)){
            Integer value = metricValue.getIntegerValue();
            String valueAsString = value.toString();
            return(valueAsString);
        }
        if(metricValue.getObjectType().equals(Instant.class)){
//            String value = metricValue.getInstantValue().toString();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss").withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));
//            DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));
            String value = formatter.format(metricValue.getInstantValue());
            return(value);
        }
        if (metricValue.getObjectType().equals(Double.class)) {
            String value = metricValue.getDoubleValue().toString();
            return(value);
        }
        if(metricValue.getObjectType().equals(Float.class)){
            String value = metricValue.getFloatValue().toString();
            return(value);
        }
        return(metricValue.getObject().toString());
    }
}
