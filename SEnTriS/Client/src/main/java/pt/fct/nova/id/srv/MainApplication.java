package pt.fct.nova.id.srv;

import pt.fct.nova.id.srv.presentation.controllers.*;

import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;

public class MainApplication extends Application {
    private final Set<Class<?>> resources = new HashSet<>();

    public MainApplication() {
        resources.add(ControlController.class);
        resources.add(UsersController.class);
        resources.add(EncryptedTriplestoreV1Controller.class);
        resources.add(EncryptedTriplestoreV2Controller.class);
        resources.add(TriplestoreController.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return resources;
    }

}