package pt.fct.nova.id.srv.application.protocols;

public enum ProtocolVersion {
    V1, V2;

    public static ProtocolVersion fromString(String name) {
        return valueOf(name.toUpperCase());
    }
}
