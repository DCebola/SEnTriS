package pt.fct.nova.id.srv.application.clients.exception;

public class TriplestoreClientException extends Exception{
    private final String message;
    public TriplestoreClientException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
