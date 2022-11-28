package pt.fct.nova.id.srv;

import pt.fct.nova.id.srv.presentation.controllers.ControlController;

import jakarta.ws.rs.core.Application;
import pt.fct.nova.id.srv.presentation.controllers.SecureTriplestoreController;
import pt.fct.nova.id.srv.presentation.controllers.TriplestoreController;
import pt.fct.nova.id.srv.presentation.controllers.UsersController;

import java.util.HashSet;
import java.util.Set;

public class MainApplication extends Application {
    private final Set<Class<?>> resources = new HashSet<>();

    public MainApplication() {
        resources.add(ControlController.class);
        resources.add(UsersController.class);
        resources.add(SecureTriplestoreController.class);
        resources.add(TriplestoreController.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return resources;
    }

}