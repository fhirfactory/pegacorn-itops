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
package net.fhirfactory.pegacorn.itops.im.workshops.gatekeeper;

import net.fhirfactory.pegacorn.core.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelNormalisationStatusEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelValidationStatusEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.PolicyEnforcementPointApprovalStatusEnum;
import net.fhirfactory.pegacorn.internals.communicate.entities.message.factories.CommunicateMessageTopicFactory;
import net.fhirfactory.pegacorn.itops.im.workshops.gatekeeper.beans.MetricsCheckpointBean;
import net.fhirfactory.pegacorn.workshops.PolicyEnforcementWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.MOAStandardWUP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class MetricsCheckpointWUP extends MOAStandardWUP {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsCheckpointWUP.class);

    private static String WUP_VERSION = "1.0.0";

    @Inject
    private PolicyEnforcementWorkshop policyEnforcementWorkshop;

    @Inject
    private MetricsCheckpointBean metricsCheckpoint;

    @Inject
    private CommunicateMessageTopicFactory messageTopicFactory;

    //
    // Getters (and Setters)
    //

    @Override
    protected Logger specifyLogger() {
        return (LOG);
    }

    protected CommunicateMessageTopicFactory getMessageTopicFactory(){
        return(messageTopicFactory);
    }


    //
    // Subscription Logic (NOT!)
    //

    @Override
    protected List<DataParcelManifest> specifySubscriptionTopics() {
        List<DataParcelManifest> subscribedTopics = new ArrayList<>();

        DataParcelManifest emailManifest = new DataParcelManifest();
        DataParcelTypeDescriptor emailDescriptor = getMessageTopicFactory().createEmailTypeDescriptor();
        emailManifest.setContentDescriptor(emailDescriptor);
        emailManifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_TRUE);
        emailManifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
        emailManifest.setDataParcelFlowDirection(DataParcelDirectionEnum.INFORMATION_FLOW_WORKFLOW_OUTPUT);
        emailManifest.setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_NEGATIVE);
        emailManifest.setSourceSystem(DataParcelManifest.WILDCARD_CHARACTER);
        emailManifest.setSourceProcessingPlantParticipantName(DataParcelManifest.WILDCARD_CHARACTER);
        emailManifest.setInterSubsystemDistributable(false);
        subscribedTopics.add(emailManifest);

        DataParcelManifest smsManifest = new DataParcelManifest();
        DataParcelTypeDescriptor smsDescriptor = getMessageTopicFactory().createSMSTypeDescriptor();
        smsManifest.setContentDescriptor(smsDescriptor);
        smsManifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_TRUE);
        smsManifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
        smsManifest.setDataParcelFlowDirection(DataParcelDirectionEnum.INFORMATION_FLOW_WORKFLOW_OUTPUT);
        smsManifest.setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_NEGATIVE);
        smsManifest.setSourceSystem(DataParcelManifest.WILDCARD_CHARACTER);
        smsManifest.setSourceProcessingPlantParticipantName(DataParcelManifest.WILDCARD_CHARACTER);
        emailManifest.setInterSubsystemDistributable(false);
        subscribedTopics.add(smsManifest);

        return(subscribedTopics);
    }

    @Override
    protected List<DataParcelManifest> declarePublishedTopics() {
        return (new ArrayList<>());
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
    protected WorkshopInterface specifyWorkshop() {
        return (policyEnforcementWorkshop);
    }

    @Override
    public void configure() throws Exception {
        getLogger().trace("{}:: ingresFeed() --> {}", getClass().getName(), ingresFeed());
        getLogger().trace("{}:: egressFeed() --> {}", getClass().getName(), egressFeed());

        fromIncludingPetasosServices(ingresFeed())
                .routeId(getNameSet().getRouteCoreWUP())
                .bean(metricsCheckpoint, "enforceInboundPolicy")
                .to(egressFeed());
    }
}
