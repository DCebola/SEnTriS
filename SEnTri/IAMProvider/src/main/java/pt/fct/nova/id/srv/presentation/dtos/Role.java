package pt.fct.nova.id.srv.presentation.dtos;

public enum Role {
    ADMIN, PRIVILEGED, BASIC;
    public static Role fromString(String name) {
        return valueOf(name.toUpperCase());
    }

}
