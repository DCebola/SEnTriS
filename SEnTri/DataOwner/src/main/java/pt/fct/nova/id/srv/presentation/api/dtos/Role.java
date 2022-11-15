package pt.fct.nova.id.srv.presentation.api.dtos;

public enum Role {
    ADMIN, MANAGER, PRIVILEGED, BASIC;

    public static Role fromString(String name) {
        return valueOf(name.toUpperCase());
    }

}
