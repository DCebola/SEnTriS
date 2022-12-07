package pt.fct.nova.id.srv.application;

public record AccessRequest(String requestID, String username, boolean write) {

}
