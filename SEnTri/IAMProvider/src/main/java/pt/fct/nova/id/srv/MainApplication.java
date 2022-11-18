package pt.fct.nova.id.srv;

import pt.fct.nova.id.srv.presentation.controllers.ControlController;

import jakarta.ws.rs.core.Application;
import pt.fct.nova.id.srv.presentation.controllers.IAMController;
import pt.fct.nova.id.srv.presentation.controllers.LocksController;

import java.util.HashSet;
import java.util.Set;

public class MainApplication extends Application {
    private final Set<Class<?>> resources = new HashSet<>();

    public MainApplication() {
        resources.add(ControlController.class);
        resources.add(LocksController.class);
        resources.add(IAMController.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return resources;
    }

}