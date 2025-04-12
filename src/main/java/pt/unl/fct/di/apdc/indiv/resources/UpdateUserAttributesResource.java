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
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.data.UpdateUserAttributesData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class UpdateUserAttributesResource {
    private static final Logger LOG = Logger.getLogger(UpdateUserAttributesResource.class.getName());
    private final Gson gson = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/update-attributes")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAttributes(@Context ContainerRequestContext context, UpdateUserAttributesData data) {
        LOG.fine("Update attributes attempt for identifier: " + data.getIdentifier());

        try {
            AuthToken token = (AuthToken) context.getProperty("authToken");
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

            Entity userEntity = results.next();
            User user = User.fromEntity(userEntity);

            if (token.getRole().equals("ENDUSER") && !user.getUsername().equals(token.getUsername())) {
                throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "Insufficient permissions");
            }
            if (token.getRole().equals("BACKOFFICE") && !user.getRole().equals("ENDUSER") && !user.getRole().equals("PARTNER")) {
                throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "Insufficient permissions");
            }

            Entity.Builder builder = Entity.newBuilder(userEntity);
            for (String key : data.getAttributes().keySet()) {
                if (key.equals("username") || key.equals("email") || key.equals("role") || key.equals("accountState") ||
                    (key.equals("fullName") && token.getRole().equals("BACKOFFICE"))) {
                    if (!token.getRole().equals("ADMIN")) {
                        continue;
                    }
                }
                builder.set(key, data.getAttributes().get(key));
            }
            datastore.update(builder.build());

            return Response.ok(gson.toJson(new UpdateAttributesResponse("Attributes updated successfully", data.getIdentifier()))).build();
        } catch (AppException e) {
            LOG.warning("Update attributes failed: " + e.getMessage());
            return Response.status(e.getStatus()).entity(gson.toJson(new ErrorResponse(e.getMessage()))).build();
        }
    }

    private static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class UpdateAttributesResponse {
        public String message;
        public String identifier;
        public UpdateAttributesResponse(String message, String identifier) {
            this.message = message;
            this.identifier = identifier;
        }
    }
}