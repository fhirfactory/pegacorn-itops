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
package net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common;

import net.fhirfactory.dricats.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.model.petasos.oam.metrics.reporting.PetasosComponentMetric;
import net.fhirfactory.dricats.model.petasos.oam.metrics.reporting.PetasosComponentMetricSet;
import net.fhirfactory.dricats.model.petasos.oam.metrics.reporting.datatypes.PetasosComponentMetricValue;
import net.fhirfactory.pegacorn.itops.im.datatypes.MetricsReportContentBase;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public abstract class DefaultMetricsReportContentBodyFactory {

    private DateTimeFormatter timeFormatter;

    //
    // Constructor(s)
    //

    public DefaultMetricsReportContentBodyFactory(){
        timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS").withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));
    }

    //
    // Getters and Setters
    //

    abstract protected Logger getLogger();

    protected DateTimeFormatter getTimeFormatter(){
        return(this.timeFormatter);
    }

    //
    // Business Methods
    //

    public MetricsReportContentBase newDefaultMetricsContentReport(PetasosComponentMetricSet metricSet) {
        if (metricSet == null) {
            return (null);
        }

        String metricTimestamp = timeFormatter.format(metricSet.getReportingInstant());
        if (metricSet.getReportingInstant() != null) {
            metricTimestamp = getTimeFormatter().format(metricSet.getReportingInstant());
        }
        if (metricTimestamp == null) {
            metricTimestamp = "Not Specified";
        }

        StringBuilder unformattedTextBuilder = new StringBuilder();
        StringBuilder formattedTextBuilder = new StringBuilder();

        unformattedTextBuilder.append("Processing Plant Metric Report (" + metricTimestamp + ") \n");

        formattedTextBuilder.append("<b> Processing Plant Metric Report (" + metricTimestamp + ") </b> \n");
        formattedTextBuilder.append("<table style='width:100%'>");
        formattedTextBuilder.append("<tr>");
        formattedTextBuilder.append("<th>Metric Name</th>");
        formattedTextBuilder.append("<th>Metric Type</th>");
        formattedTextBuilder.append("<th>Metric Unit</th>");
        formattedTextBuilder.append("<th>Metric Value</th>");
        formattedTextBuilder.append("</tr>");

        for (PetasosComponentMetric currentMetric : metricSet.getMetrics().values()) {
            String metricName = currentMetric.getMetricName();
            String metricType = null;
            if (currentMetric.hasMetricType()) {
                metricType = currentMetric.getMetricType().getDisplayName();
            } else {
                metricType = "Not Specified";
            }
            String metricUnit = null;
            if (currentMetric.hasMetricUnit()) {
                metricUnit = currentMetric.getMetricUnit().getDisplayName();
            } else {
                metricUnit = "Not Specified";
            }

            String metricValue = getMetricValueAsString(currentMetric.getMetricValue());

            unformattedTextBuilder.append(metricName + ":" + metricType + ":" + metricUnit + ":" + metricValue + "\n");

            formattedTextBuilder.append("<tr>");
            formattedTextBuilder.append("<td>" + metricName + "</td>");
            formattedTextBuilder.append("<td>" + metricType + "</td>");
            formattedTextBuilder.append("<td>" + metricUnit + "</td>");
            formattedTextBuilder.append("<td>" + metricValue + "</td>");
            formattedTextBuilder.append("</tr>");
        }
        formattedTextBuilder.append("</table>");

        MetricsReportContentBase outputContent = new MetricsReportContentBase();
        outputContent.setHtmlText(formattedTextBuilder.toString());
        outputContent.setUnformatedText(unformattedTextBuilder.toString());
        return(outputContent);
    }

    //
    // Extract Metric Value
    //

    protected String getMetricValueAsString(PetasosComponentMetricValue metricValue){
        if(metricValue == null){
            return("-");
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
