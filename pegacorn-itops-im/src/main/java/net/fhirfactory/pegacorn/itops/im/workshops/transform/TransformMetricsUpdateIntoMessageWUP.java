package net.fhirfactory.pegacorn.itops.im.workshops.transform;

import net.fhirfactory.pegacorn.core.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelNormalisationStatusEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelValidationStatusEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.PolicyEnforcementPointApprovalStatusEnum;
import net.fhirfactory.pegacorn.workshops.TransformWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.MOAStandardWUP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TransformMetricsUpdateIntoMessageWUP extends MOAStandardWUP {
    private static final Logger LOG = LoggerFactory.getLogger(TransformMetricsUpdateIntoMessageWUP.class);

    @Inject
    private TransformWorkshop transformWorkshop;

    //
    // Contstructor(s)
    //

    public TransformMetricsUpdateIntoMessageWUP(){
        super();
    }

    //
    // Post Construct
    //

    @Override
    protected Logger specifyLogger() {
        return (LOG);
    }

    @Override
    protected List<DataParcelManifest> specifySubscriptionTopics() {
        List<DataParcelManifest> subscribedTopics = new ArrayList<>();

        DataParcelManifest manifest = new DataParcelManifest();
        DataParcelTypeDescriptor descriptor = new DataParcelTypeDescriptor();
        descriptor.setDataParcelDefiner("FHIRFactory");
        descriptor.setDataParcelCategory("OAM");
        descriptor.setDataParcelSubCategory("Reporting");
        descriptor.setDataParcelResource("PetasosComponentMetric");
        manifest.setContentDescriptor(descriptor);
        manifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_FALSE);
        manifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
        manifest.setDataParcelFlowDirection(DataParcelDirectionEnum.INFORMATION_FLOW_INBOUND_DATA_PARCEL);
        manifest.setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_NEGATIVE);
        subscribedTopics.add(manifest);

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
        return ("1.0.0");
    }

    @Override
    protected WorkshopInterface specifyWorkshop() {
        return (transformWorkshop);
    }

    @Override
    public void configure() throws Exception {

    }
}
