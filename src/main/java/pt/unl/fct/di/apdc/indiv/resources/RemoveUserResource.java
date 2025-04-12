package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import com.google.appengine.repackaged.com.google.gson.Gson;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.exceptions.AppException;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.data.RemoveUserData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RemoveUserResource {
    private static final Logger LOG = Logger.getLogger(RemoveUserResource.class.getName());
    private final Gson gson = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeUser(@Context ContainerRequestContext context, RemoveUserData data) {
        LOG.fine("Remove user attempt for identifier: " + data.getIdentifier());

        try {
            AuthToken token = (AuthToken) context.getProperty("authToken");
            if (!token.getRole().equals("BACKOFFICE") && !token.getRole().equals("ADMIN")) {
                throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "Insufficient permissions");
            }

            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .setFilter(StructuredQuery.CompositeFilter.or(
                            StructuredQuery.PropertyFilter.eq("username", data.getIdentifier()),
                            StructuredQuery.PropertyFilter.eq("email", data.getIdentifier())))
                    .build();
            QueryResults<Entity> results = datastore.run(query);
            if (!results.hasNext()) {
                throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), "User not found");
            }

            Entity user = results.next();
            if (token.getRole().equals("BACKOFFICE")) {
                String userRole = user.getString("role");
                if (!userRole.equals("ENDUSER") && !userRole.equals("PARTNER")) {
                    throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "Insufficient permissions");
                }
            }

            datastore.delete(user.getKey());
            return Response.ok(gson.toJson(new RemoveUserResponse("User removed successfully", data.getIdentifier()))).build();
        } catch (AppException e) {
            LOG.warning("Remove user failed: " + e.getMessage());
            return Response.status(e.getStatus()).entity(gson.toJson(new ErrorResponse(e.getMessage()))).build();
        }
    }

    private static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class RemoveUserResponse {
        public String message;
        public String identifier;
        public RemoveUserResponse(String message, String identifier) {
            this.message = message;
            this.identifier = identifier;
        }
    }
}