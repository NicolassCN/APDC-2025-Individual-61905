package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.UserValidator;
import pt.unl.fct.di.apdc.indiv.util.data.ChangeRoleData;

@Path("/role")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeRoleResource {
    private static final Logger LOG = Logger.getLogger(ChangeRoleResource.class.getName());
    private static final Gson gson = new Gson();

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeRole(ChangeRoleData data) {
        LOG.info("Attempting to change role for user: " + data.username);
        
        // Validate role
        if (!UserValidator.isValidRole(data.newRole)) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(gson.toJson("Invalid role specified."))
                    .build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            // Get token
            Key tokenKey = tokenKeyFactory.newKey(data.token);
            Entity tokenEntity = txn.get(tokenKey);
            if (tokenEntity == null) {
                txn.rollback();
                return Response.status(Status.UNAUTHORIZED)
                        .entity(gson.toJson("Invalid token."))
                        .build();
            }

            AuthToken token = AuthToken.fromEntity(tokenEntity);
            if (!token.isValid()) {
                txn.rollback();
                return Response.status(Status.UNAUTHORIZED)
                        .entity(gson.toJson("Token expired."))
                        .build();
            }

            // Get requester user
            Key requesterKey = userKeyFactory.newKey(token.getUsername());
            Entity requesterEntity = txn.get(requesterKey);
            if (requesterEntity == null) {
                txn.rollback();
                return Response.status(Status.UNAUTHORIZED)
                        .entity(gson.toJson("User not found."))
                        .build();
            }

            User requester = User.fromEntity(requesterEntity);

            // Get target user
            Key targetKey = userKeyFactory.newKey(data.username);
            Entity targetEntity = txn.get(targetKey);
            if (targetEntity == null) {
                txn.rollback();
                return Response.status(Status.NOT_FOUND)
                        .entity(gson.toJson("Target user not found."))
                        .build();
            }

            User target = User.fromEntity(targetEntity);

            // Check if requester can change target's role
            if (!requester.canChangeRole(target)) {
                txn.rollback();
                return Response.status(Status.FORBIDDEN)
                        .entity(gson.toJson("Insufficient permissions to change this user's role."))
                        .build();
            }

            // Update target's role
            target.setRole(User.Role.valueOf(data.newRole.toUpperCase()));
            txn.put(target.toEntity(datastore));
            txn.commit();

            return Response.ok(gson.toJson(new ChangeRoleResponse("Role changed successfully", data.username, data.newRole)))
                    .build();

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Error changing role: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(gson.toJson("Internal error processing role change."))
                    .build();
        }
    }

    private static class ChangeRoleResponse {
        public final String message;
        public final String username;
        public final String newRole;

        public ChangeRoleResponse(String message, String username, String newRole) {
            this.message = message;
            this.username = username;
            this.newRole = newRole;
        }
    }
}