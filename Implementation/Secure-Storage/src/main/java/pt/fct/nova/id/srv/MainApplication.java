package pt.fct.nova.id.srv;


import pt.fct.nova.id.srv.Resources.ControlResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class MainApplication extends Application {
    private final Set<Object> singletons = new HashSet<>();
    private final Set<Class<?>> resources = new HashSet<>();

    public MainApplication() {
        resources.add(ControlResource.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return resources;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}