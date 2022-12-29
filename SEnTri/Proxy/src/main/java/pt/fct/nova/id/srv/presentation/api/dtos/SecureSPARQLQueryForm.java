package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class SecureSPARQLQueryForm {
    @FormParam("key")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final byte[] key;
    @FormParam("queryExecutionPlan")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final byte[] queryExecutionPlan;

    public SecureSPARQLQueryForm(byte[] key, byte[] queryExecutionPlan) {
        this.key = key;
        this.queryExecutionPlan = queryExecutionPlan;
    }

    public SecureSPARQLQueryForm() {
        this.key = null;
        this.queryExecutionPlan = null;
    }

    public byte[] getKey() {
        return key;
    }


    public byte[] getQueryExecutionPlan() {
        return queryExecutionPlan;
    }
}
