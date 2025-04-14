package pt.unl.fct.di.apdc.indiv.filters;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    private static final ConcurrentHashMap<String, AuthToken> tokens = new ConcurrentHashMap<>();
    private static final String AUTHENTICATION_HEADER = "Authorization";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        
        // Skip authentication for registration and login
        if (path.equals("user/register") || path.equals("user/login")) {
            return;
        }

        String authHeader = requestContext.getHeaderString(AUTHENTICATION_HEADER);
        if (authHeader == null || authHeader.isEmpty()) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("No authentication token provided")
                        .build());
            return;
        }

        AuthToken token = tokens.get(authHeader);
        if (token == null || !token.isValid()) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Invalid or expired token")
                        .build());
            return;
        }
    }

    public static void addToken(String username, AuthToken token) {
        tokens.put(token.getTokenId(), token);
    }

    public static void removeToken(String username) {
        tokens.values().removeIf(token -> token.getUsername().equals(username));
    }

    public static AuthToken getTokenFromHeader(String authHeader) {
        return tokens.get(authHeader);
    }

    public static String getUsernameFromToken(String tokenId) {
        AuthToken token = tokens.get(tokenId);
        return token != null ? token.getUsername() : null;
    }
} 