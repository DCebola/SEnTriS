package pt.fct.nova.id.srv.presentation.controllers;

import pt.fct.nova.id.srv.application.storage.Redis;
import pt.fct.nova.id.srv.presentation.api.ControlAPI;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;

/**
 * Resource providing endpoints with control information.
 */
@Path("/ctrl")
public class ControlController implements ControlAPI {

    public Response version() {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            System.out.println(jedis.ping());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.ok(System.getenv("VERSION")).build();
    }
}
