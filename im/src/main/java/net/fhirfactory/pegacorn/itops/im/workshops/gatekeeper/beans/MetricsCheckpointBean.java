package net.fhirfactory.pegacorn.itops.im.workshops.gatekeeper.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.PolicyEnforcementPointApprovalStatusEnum;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWProcessingOutcomeEnum;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MetricsCheckpointBean {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsCheckpointBean.class);

    protected Logger getLogger(){
        return(LOG);
    }

    ObjectMapper jsonMapper;

    public MetricsCheckpointBean(){
        jsonMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        this.jsonMapper.registerModule(module);
        this.jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public UoW enforceInboundPolicy(UoW uow, Exchange camelExchange){
        getLogger().debug(".enforceIngresPolicy(): Entry, uow->{}", uow);
        if(uow == null){
            return(null);
        }
        if(!uow.hasIngresContent()){
            return(uow);
        }
        UoWPayload egressPayload = SerializationUtils.clone(uow.getIngresContent());
        egressPayload.getPayloadManifest().setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_POSITIVE);
        egressPayload.getPayloadManifest().setDataParcelFlowDirection(DataParcelDirectionEnum.INFORMATION_FLOW_INBOUND_DATA_PARCEL);
        egressPayload.getPayloadManifest().setInterSubsystemDistributable(true);
        uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
        uow.getEgressContent().addPayloadElement(egressPayload);
        return(uow);
    }
}
