package pt.fct.nova.id.srv.application.storage;

public record AccessRequest(String requestID, String username, boolean write) {

}
