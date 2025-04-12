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
import pt.unl.fct.di.apdc.indiv.util.UserValidator;
import pt.unl.fct.di.apdc.indiv.util.data.ChangePasswordData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangePasswordResource {
    private static final Logger LOG = Logger.getLogger(ChangePasswordResource.class.getName());
    private final Gson gson = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePassword(@Context ContainerRequestContext context, ChangePasswordData data) {
        LOG.fine("Change password attempt");

        try {
            AuthToken token = (AuthToken) context.getProperty("authToken");
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(token.getUsername());
            Entity user = datastore.get(userKey);
            if (user == null) {
                throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), "User not found");
            }

            if (!user.getString("password").equals(data.getCurrentPassword())) {
                throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), "Current password incorrect");
            }

            if (!data.getNewPassword().equals(data.getConfirmPassword())) {
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Passwords do not match");
            }

            if (!UserValidator.isValidPassword(data.getNewPassword())) {
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "New password does not meet requirements");
            }

            Entity updatedUser = Entity.newBuilder(user).set("password", data.getNewPassword()).build();
            datastore.update(updatedUser);

            return Response.ok(gson.toJson(new ChangePasswordResponse("Password changed successfully"))).build();
        } catch (AppException e) {
            LOG.warning("Change password failed: " + e.getMessage());
            return Response.status(e.getStatus()).entity(gson.toJson(new ErrorResponse(e.getMessage()))).build();
        }
    }

    private static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class ChangePasswordResponse {
        public String message;
        public ChangePasswordResponse(String message) {
            this.message = message;
        }
    }
}