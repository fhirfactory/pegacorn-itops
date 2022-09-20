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
package net.fhirfactory.pegacorn.itops.im.workshops.workflow.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.fhirfactory.dricats.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.internals.communicate.entities.message.factories.CommunicateMessageTopicFactory;
import org.slf4j.Logger;

import javax.inject.Inject;

public abstract class ITOpsNotificationToCommunicateMessage {

    private ObjectMapper jsonMapper;

    private static String LOCAL_ITOPS_NOTIFICATION_MESSAGE = "ITOps.Notification.Message";

    @Inject
    private ProcessingPlantInterface processingPlant;

    @Inject
    private CommunicateMessageTopicFactory messageTopicFactory;

    //
    // Constructor(s)
    //

    public ITOpsNotificationToCommunicateMessage(){
        this.jsonMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        this.jsonMapper.registerModule(module);
        this.jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    //
    // Abstract Methods
    //

    abstract protected Logger getLogger();

    //
    // Getters (and Setters)
    //

    protected ObjectMapper getJSONMapper(){
        return(this.jsonMapper);
    }

    protected ProcessingPlantInterface getProcessingPlant(){
        return(this.processingPlant);
    }

    protected CommunicateMessageTopicFactory getMessageTopicFactory(){
        return(this.messageTopicFactory);
    }

    protected String getLocalItopsNotificationMessagePropertyName(){
        return(LOCAL_ITOPS_NOTIFICATION_MESSAGE);
    }
}
