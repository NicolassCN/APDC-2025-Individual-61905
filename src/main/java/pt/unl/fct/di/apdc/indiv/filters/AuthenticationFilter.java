package pt.unl.fct.di.apdc.indiv.filters;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    private static final Logger LOG = Logger.getLogger(AuthenticationFilter.class.getName());
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        String path = context.getUriInfo().getPath();
        if (path.endsWith("/register") || path.endsWith("/login")) {
            return; // Skip authentication for public endpoints
        }

        String authHeader = context.getHeaderString(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            LOG.warning("Missing or invalid Authorization header");
            context.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Authorization header missing or invalid\"}").build());
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        AuthToken authToken = AuthToken.validate(token);
        if (authToken == null) {
            LOG.warning("Invalid JWT token");
            context.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Invalid token\"}").build());
            return;
        }

        context.setProperty("authToken", authToken);
        LOG.fine("Authenticated user: " + authToken.getUsername());
    }
}