package pt.fct.nova.id.srv.application;

public record AccessRequest(String target, String storeID, boolean write, boolean read) {

}
