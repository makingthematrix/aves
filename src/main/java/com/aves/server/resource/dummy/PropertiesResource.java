package com.aves.server.resource.dummy;

import com.aves.server.DAO.PropertiesDAO;
import com.aves.server.tools.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;

@Api
@Path("/properties")
@Produces(MediaType.APPLICATION_JSON)
public class PropertiesResource {
    private final PropertiesDAO propertiesDAO;

    public PropertiesResource(Jdbi jdbi) {
        propertiesDAO = jdbi.onDemand(PropertiesDAO.class);
    }

    @GET
    @Path("{key}")
    @ApiOperation(value = "Get properties")
    @Authorization("Bearer")
    public Response getProperty(@ApiParam @PathParam("key") String key) {
        String value = propertiesDAO.get(key);
        if (value == null)
            return Response.status(404).build();

        return Response.ok(value).build();
    }

    @PUT
    @Path("{key}")
    @ApiOperation(value = "Put properties")
    @Authorization("Bearer")
    public Response putProperty(@ApiParam @PathParam("key") String key, String value) {
        Logger.info("PropertiesResource.put(%s): %s", key, value);
        propertiesDAO.upsert(key, value);
        return Response.ok().build();
    }

    public static class _Result {
        public int version = 1;
        public boolean enable_debugging;
        public HashMap<String, HashMap<String, Object>> settings = new HashMap<>();
    }
}
