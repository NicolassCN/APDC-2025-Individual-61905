package pt.unl.fct.di.apdc.indiv.filters;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    private static final Logger LOG = Logger.getLogger(AuthenticationFilter.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

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

        String tokenString = authHeader.substring("Bearer ".length());
        
        try {
            // Find the token entity in datastore
            Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenString);
            Entity tokenEntity = datastore.get(tokenKey);
            
            if (tokenEntity == null) {
                LOG.warning("Invalid token: " + tokenString);
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"Invalid token\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build());
                return;
            }
            
            // Check if token is expired
            long expirationDate = tokenEntity.getLong("expirationDate");
            if (System.currentTimeMillis() > expirationDate) {
                LOG.warning("Expired token: " + tokenString);
                // Delete expired token
                datastore.delete(tokenKey);
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"Token expired\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build());
                return;
            }
            
            // Token is valid, add user info to request context
            requestContext.setProperty("username", tokenEntity.getString("username"));
            requestContext.setProperty("role", tokenEntity.getString("role"));
            
        } catch (Exception e) {
            LOG.severe("Error validating token: " + e.getMessage());
            requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error validating token\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
    }
} 