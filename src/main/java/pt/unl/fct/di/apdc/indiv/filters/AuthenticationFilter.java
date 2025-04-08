package pt.unl.fct.di.apdc.indiv.filters;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    private static final Map<String, AuthToken> tokens = new HashMap<>();

    public static void addToken(String username, AuthToken token) {
        tokens.put(username, token);
    }

    public static AuthToken getToken(String username) {
        return tokens.get(username);
    }

    public static void removeToken(String username) {
        tokens.remove(username);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        
        // Skip authentication for login and register endpoints
        if (path.equals("login") || path.equals("register")) {
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"No authorization header\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build());
            return;
        }

        String token = authHeader.substring("Bearer ".length());
        boolean validToken = false;

        for (AuthToken authToken : tokens.values()) {
            if (authToken.getTokenID().equals(token) && authToken.isValid()) {
                validToken = true;
                break;
            }
        }

        if (!validToken) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Invalid or expired token\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
    }
} 