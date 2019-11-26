package com.aves.server.filters;

import com.aves.server.Server;
import io.jsonwebtoken.Jwts;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.UUID;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        Cookie authCookie = requestContext.getCookies().get("zuid");
        String access_token = requestContext.getUriInfo().getQueryParameters(true).getFirst("access_token");

        if (auth == null && authCookie == null && access_token == null) {
            Exception cause = new IllegalArgumentException("Authorization Header was not specified");
            throw new WebApplicationException(cause, Response.Status.UNAUTHORIZED);
        }

        if (authCookie != null)
            auth = String.format("Bearer %s", authCookie.getValue());

        if (access_token != null)
            auth = String.format("Bearer %s", access_token);

        String[] split = auth.split(" ");

        if (split.length != 2) {
            Exception cause = new IllegalArgumentException("Bad Authorization");
            throw new WebApplicationException(cause, Response.Status.UNAUTHORIZED);
        }

        String type = split[0];
        if (!type.equalsIgnoreCase("Bearer")) {
            Exception cause = new IllegalArgumentException("Missing Bearer in the Authorization");
            throw new WebApplicationException(cause, Response.Status.UNAUTHORIZED);
        }

        String token = split[1];

        try {
            String subject = Jwts.parser()
                    .setSigningKey(Server.getKey())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            UUID userId = UUID.fromString(subject);
            requestContext.setProperty("zuid", userId);
        } catch (Exception e) {
            Exception cause = new IllegalArgumentException(e.getMessage());
            throw new WebApplicationException(cause, Response.Status.UNAUTHORIZED);
        }
    }
}