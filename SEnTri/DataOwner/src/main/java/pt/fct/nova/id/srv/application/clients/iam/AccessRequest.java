package pt.fct.nova.id.srv.application.clients.iam;

public record AccessRequest(String target, String storeID, boolean write, boolean read) {

}
