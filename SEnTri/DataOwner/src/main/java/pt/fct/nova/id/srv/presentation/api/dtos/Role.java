package pt.fct.nova.id.srv.presentation.api.dtos;

public enum Role {
    ADMIN, MANAGER, DATA_OWNER, PRIVILEGED_USER, BASIC_USER;

    public static Role fromString(String name) {
        return valueOf(name.toUpperCase());
    }

}
