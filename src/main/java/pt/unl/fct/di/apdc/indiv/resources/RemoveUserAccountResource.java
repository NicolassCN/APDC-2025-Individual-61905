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
import pt.unl.fct.di.apdc.indiv.util.RemoveUserAccountData;

@Path("/removeuseraccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RemoveUserAccountResource {

    private static final Logger LOG = Logger.getLogger(RemoveUserAccountResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/") // Ensure path is distinct if needed
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeUserAccount(@Context HttpHeaders headers, RemoveUserAccountData data) {
        LOG.fine("Attempt to remove user account: " + data.targetUsername);

        String authTokenHeader = headers.getHeaderString("Authorization");
        String tokenString = authTokenHeader != null ? authTokenHeader.replace("Bearer ", "") : null;

        if (tokenString == null) {
            LOG.warning("Remove user account attempt without token.");
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
                LOG.warning("Remove user account attempt with invalid token: " + tokenString);
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
                LOG.warning("Remove user account attempt with expired token for user: " + token.getUsername());
                return Response.status(Response.Status.UNAUTHORIZED).entity("Expired token.").build();
            }

            String requesterUsername = token.getUsername();
            String requesterRole = token.getRole();

            // Get target user entity
            Key targetUserKey;
            if (data.isEmail) {
                // If targetUsername is an email, query by email
                Query<Entity> userQuery = Query.newEntityQueryBuilder()
                        .setKind("User")
                        .setFilter(StructuredQuery.PropertyFilter.eq("email", data.targetUsername))
                        .build();
                QueryResults<Entity> userResults = txn.run(userQuery);
                if (!userResults.hasNext()) {
                    LOG.warning("Remove user account attempt for non-existent target user with email: " + data.targetUsername);
                    return Response.status(Response.Status.NOT_FOUND).entity("Target user not found.").build();
                }
                targetUserKey = userResults.next().getKey();
            } else {
                // Otherwise, assume targetUsername is the username
                targetUserKey = datastore.newKeyFactory().setKind("User").newKey(data.targetUsername);
            }

            Entity targetUser = txn.get(targetUserKey);
            if (targetUser == null) {
                LOG.warning("Remove user account attempt for non-existent target user: " + data.targetUsername);
                return Response.status(Response.Status.NOT_FOUND).entity("Target user not found.").build();
            }

            String targetRole = targetUser.getString("role");

            // Check permissions based on requester's role
            boolean canRemoveAccount = false;
            switch (requesterRole) {
                case "ADMIN":
                    canRemoveAccount = true; // Admin can remove any account
                    break;
                case "BACKOFFICE":
                    // Backoffice can remove ENDUSER accounts
                    canRemoveAccount = targetRole.equals("ENDUSER");
                    break;
                // ENDUSER has no permission (default case)
            }

            if (!canRemoveAccount) {
                LOG.warning("User '" + requesterUsername + "' (Role: " + requesterRole + ") attempted to remove account of user '" + data.targetUsername + "' (Role: " + targetRole + ") without permission.");
                return Response.status(Response.Status.FORBIDDEN).entity("Insufficient permissions to remove user account.").build();
            }

            // Perform the account removal
            txn.delete(targetUserKey);
            txn.commit();

            LOG.info("User '" + requesterUsername + "' successfully removed account of user '" + data.targetUsername + "'.");
            return Response.ok(g.toJson("User account removed successfully.")).build();

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Error removing user account " + data.targetUsername + ": " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error processing account removal.").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
} 