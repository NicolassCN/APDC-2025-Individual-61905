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

    public static String getUsernameFromToken(String tokenId) {
        for (Map.Entry<String, AuthToken> entry : tokens.entrySet()) {
            if (entry.getValue().getTokenID().equals(tokenId) && entry.getValue().isValid()) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static AuthToken getTokenFromUsername(String username) {
        AuthToken token = tokens.get(username);
        if (token != null && token.isValid()) {
            return token;
        }
        return null;
    }

    public static AuthToken getTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String tokenId = authHeader.substring("Bearer ".length());
        String username = getUsernameFromToken(tokenId);
        if (username == null) {
            return null;
        }
        return getTokenFromUsername(username);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        
        // Skip authentication for login and register endpoints
        if (path.equals("api/user/register") || path.equals("api/user/login")) {
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
        String username = getUsernameFromToken(token);

        if (username == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Invalid or expired token\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
    }
} 