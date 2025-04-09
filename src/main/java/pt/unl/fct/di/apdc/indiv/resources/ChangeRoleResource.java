package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.ChangeRoleData;

@Path("/changerole")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeRoleResource {

    private static final Logger LOG = Logger.getLogger(ChangeRoleResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/") // Ensure path is distinct if needed
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeUserRole(@Context HttpHeaders headers, ChangeRoleData data) {
        LOG.fine("Attempt to change role for user: " + data.targetUsername);

        String authTokenHeader = headers.getHeaderString("Authorization");
        String tokenString = authTokenHeader != null ? authTokenHeader.replace("Bearer ", "") : null;

        if (tokenString == null) {
            LOG.warning("Change role attempt without token.");
            return Response.status(Response.Status.UNAUTHORIZED).entity("Missing Authorization token.").build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            // Find the token entity
            Query<Entity> tokenQuery = Query.newEntityQueryBuilder()
                    .setKind("AuthToken")
                    .setFilter(StructuredQuery.PropertyFilter.eq("__key__", datastore.newKeyFactory().setKind("AuthToken").newKey(tokenString)))
                    .build();
            QueryResults<Entity> tokenResults = txn.run(tokenQuery);

            if (!tokenResults.hasNext()) {
                LOG.warning("Change role attempt with invalid token: " + tokenString);
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token.").build();
            }

            Entity tokenEntity = tokenResults.next();
            AuthToken token = new AuthToken(tokenEntity.getString("username"), tokenEntity.getString("role"));
            token.tokenString = tokenString; // Set the token string for reference
            token.creationDate = tokenEntity.getLong("creationDate");
            token.expirationDate = tokenEntity.getLong("expirationDate");

            if (!token.isValid()) {
                txn.delete(tokenEntity.getKey()); // Clean up expired token
                txn.commit();
                LOG.warning("Change role attempt with expired token for user: " + token.getUsername());
                return Response.status(Response.Status.UNAUTHORIZED).entity("Expired token.").build();
            }

            String requesterUsername = token.getUsername();
            String requesterRole = token.getRole();

            // Get target user entity
            Key targetUserKey = datastore.newKeyFactory().setKind("User").newKey(data.targetUsername);
            Entity targetUser = txn.get(targetUserKey);

            if (targetUser == null) {
                LOG.warning("Change role attempt for non-existent target user: " + data.targetUsername);
                return Response.status(Response.Status.NOT_FOUND).entity("Target user not found.").build();
            }

            String currentTargetRole = targetUser.getString("role");
            String newRole = data.newRole;

            // Validate new role
            if (!isValidRole(newRole)) {
                 LOG.warning("Change role attempt with invalid new role: " + newRole);
                 return Response.status(Response.Status.BAD_REQUEST).entity("Invalid target role.").build();
            }

            // Check permissions based on requester's role
            boolean canChangeRole = false;
            switch (requesterRole) {
                case "ADMIN":
                    canChangeRole = true; // Admin can change any role to any role
                    break;
                case "BACKOFFICE":
                    // Backoffice can change ENDUSER to PARTNER and vice-versa
                    if ((currentTargetRole.equals("ENDUSER") && newRole.equals("PARTNER")) ||
                        (currentTargetRole.equals("PARTNER") && newRole.equals("ENDUSER"))) {
                        canChangeRole = true;
                    }
                    break;
                // ENDUSER has no permission (default case)
            }

            if (!canChangeRole) {
                LOG.warning("User '" + requesterUsername + "' (Role: " + requesterRole + ") attempted to change role of '" + data.targetUsername + "' from '" + currentTargetRole + "' to '" + newRole + "' without permission.");
                return Response.status(Response.Status.FORBIDDEN).entity("Insufficient permissions to change role.").build();
            }

            // Perform the role change
            Entity updatedUser = Entity.newBuilder(targetUserKey, targetUser)
                    .set("role", newRole)
                    .build();
            txn.put(updatedUser);
            txn.commit();

            LOG.info("User '" + requesterUsername + "' successfully changed role of user '" + data.targetUsername + "' to '" + newRole + "'.");
            return Response.ok(g.toJson("Role updated successfully.")).build();

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Error changing role for user " + data.targetUsername + ": " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error processing role change.").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    private boolean isValidRole(String role) {
        return role != null && (role.equals("ENDUSER") || role.equals("BACKOFFICE") || role.equals("ADMIN") || role.equals("PARTNER"));
    }
} 