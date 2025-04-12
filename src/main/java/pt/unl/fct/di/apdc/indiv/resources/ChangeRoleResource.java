package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;

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
import pt.unl.fct.di.apdc.indiv.util.ChangeRoleData;
import pt.unl.fct.di.apdc.indiv.util.UserValidator;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeRoleResource {
    private static final Logger LOG = Logger.getLogger(ChangeRoleResource.class.getName());
    private final Gson gson = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/change-role")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeRole(@Context ContainerRequestContext context, ChangeRoleData data) {
        LOG.fine("Change role attempt for username: " + data.getUsername());

        try {
            AuthToken token = (AuthToken) context.getProperty("authToken");
            if (!token.getRole().equals("BACKOFFICE") && !token.getRole().equals("ADMIN")) {
                throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "Insufficient permissions");
            }

            if (!UserValidator.isValidRole(data.getNewRole())) {
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Invalid role");
            }

            Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.getUsername());
            Entity user = datastore.get(userKey);
            if (user == null) {
                throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), "User not found");
            }

            if (token.getRole().equals("BACKOFFICE")) {
                String currentRole = user.getString("role");
                if (!(currentRole.equals("ENDUSER") && data.getNewRole().equals("PARTNER")) &&
                    !(currentRole.equals("PARTNER") && data.getNewRole().equals("ENDUSER"))) {
                    throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "Insufficient permissions");
                }
            }

            Entity updatedUser = Entity.newBuilder(user).set("role", data.getNewRole()).build();
            datastore.update(updatedUser);

            return Response.ok(gson.toJson(new ChangeRoleResponse("Role updated successfully", data.getUsername(), data.getNewRole()))).build();
        } catch (AppException e) {
            LOG.warning("Change role failed: " + e.getMessage());
            return Response.status(e.getStatus()).entity(gson.toJson(new ErrorResponse(e.getMessage()))).build();
        }
    }

    private static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class ChangeRoleResponse {
        public String message;
        public String username;
        public String newRole;
        public ChangeRoleResponse(String message, String username, String newRole) {
            this.message = message;
            this.username = username;
            this.newRole = newRole;
        }
    }
}