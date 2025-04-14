package pt.unl.fct.di.apdc.indiv.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.ChangePasswordData;
import pt.unl.fct.di.apdc.indiv.util.ChangeRoleData;
import pt.unl.fct.di.apdc.indiv.util.ChangeStateData;
import pt.unl.fct.di.apdc.indiv.util.UpdateAttributesData;



@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class UserResource {

    private static final Logger LOG = Logger.getLogger(UserResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthToken");

    private static final String USER_ROLE_PROPERTY = "user_role";
    private static final String USER_STATE_PROPERTY = "user_state";
    private static final String USER_PWD_PROPERTY = "user_pwd";


    public UserResource() { }

    @POST
    @Path("/changestate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAccountState(@HeaderParam("Authorization") String authorizationHeader, ChangeStateData data) {

        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("ChangeState failed: Invalid or expired token provided.");
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        LOG.info("ChangeState attempt by user: " + token.getUsername() + " (Role: " + token.getRole() + ") for target: " + (data != null ? data.getTargetUser() : "null"));

        if (data == null || !data.isValid()) {
            LOG.warning("ChangeState failed: Invalid input data. User: " + token.getUsername());
            return Response.status(Status.BAD_REQUEST).entity("Invalid input data. Provide targetUser and newState (ATIVADA/DESATIVADA).").build();
        }
        String newStateUpper = data.getNewState().toUpperCase();

        if (!(token.getRole().equals("ADMIN") || token.getRole().equals("BACKOFFICE"))) {
            LOG.warning("ChangeState failed: User " + token.getUsername() + " (Role: " + token.getRole() + ") does not have permission.");
            return Response.status(Status.FORBIDDEN).entity("User does not have permission for this operation.").build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            Key targetUserKey = userKeyFactory.newKey(data.getTargetUser());
            Entity targetUser = txn.get(targetUserKey);

            if (targetUser == null) {
                txn.rollback();
                LOG.warning("ChangeState failed: Target user " + data.getTargetUser() + " not found. Requested by: " + token.getUsername());
                return Response.status(Status.NOT_FOUND).entity("Target user not found.").build();
            }

            String targetUserRole = targetUser.contains(USER_ROLE_PROPERTY) ? targetUser.getString(USER_ROLE_PROPERTY) : "ENDUSER";

            if (token.getRole().equals("BACKOFFICE")) {
                if (targetUserRole.equals("ADMIN") || targetUserRole.equals("BACKOFFICE")) {
                    txn.rollback();
                    LOG.warning("ChangeState failed: BACKOFFICE user " + token.getUsername() + " attempted to change state of ADMIN/BACKOFFICE user " + data.getTargetUser());
                    return Response.status(Status.FORBIDDEN).entity("BACKOFFICE users cannot change state of ADMIN or other BACKOFFICE users.").build();
                }
            }

            Entity updatedUser = Entity.newBuilder(targetUser)
                    .set(USER_STATE_PROPERTY, newStateUpper)
                    .build();
            txn.put(updatedUser);
            txn.commit();

            LOG.info("User " + token.getUsername() + " successfully changed state of user " + data.getTargetUser() + " to " + newStateUpper);
            return Response.ok().entity("User state changed successfully.").build();

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangeState Datastore error for target " + data.getTargetUser() + " by user " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing user state (Datastore).").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangeState Unexpected error for target " + data.getTargetUser() + " by user " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing user state (Unexpected).").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    private AuthToken validateToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            LOG.fine("Token validation failed: Missing or invalid Authorization header format.");
            return null;
        }

        String tokenId = authorizationHeader.substring(7).trim();
        if (tokenId.isEmpty()) {
            LOG.fine("Token validation failed: Token ID is empty.");
            return null;
        }

        LOG.fine("Validating TokenID: " + tokenId);

        try {
            Key tokenKey = tokenKeyFactory.newKey(tokenId);
            Entity tokenEntity = datastore.get(tokenKey);

            if (tokenEntity == null) {
                LOG.warning("Token validation failed: TokenID not found in Datastore: " + tokenId);
                return null;
            }

            AuthToken token = new AuthToken();
            token.setTokenID(tokenId);
            token.setUsername(tokenEntity.contains("username") ? tokenEntity.getString("username") : null);
            token.setRole(tokenEntity.contains("role") ? tokenEntity.getString("role") : null);
            token.setCreationTime(tokenEntity.contains("creationData") ? tokenEntity.getLong("creationData") : 0L);
            token.setExpirationTime(tokenEntity.contains("expirationData") ? tokenEntity.getLong("expirationData") : 0L);

            // if (token.getUsername() == null || token.getRole() == null || token.getExpirationTime() == 0L) {
            //     LOG.severe("Token validation failed: Missing essential data in token entity for TokenID: " + tokenId);
            //     return null;
            // }

            // long currentTime = System.currentTimeMillis();
            // if (currentTime > token.getExpirationTime()) {
            //     LOG.warning("Token validation failed: Token expired for user " + token.getUsername() + " (TokenID: " + tokenId + ")");
            //     return null;
            // }

            LOG.info("Token validated successfully for user: " + token.getUsername() + " (Role: " + token.getRole() + ")");
            return token;

        } catch (DatastoreException e) {
            LOG.log(Level.SEVERE, "Datastore error during token validation for TokenID: " + tokenId, e);
            return null;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected error during token validation for TokenID: " + tokenId, e);
            return null;
        }
    }

    @POST
    @Path("/changerole")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeRole(@HeaderParam("Authorization") String authorizationHeader, ChangeRoleData data) {

        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("ChangeRole failed: Invalid or expired token provided.");
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        LOG.info("ChangeRole attempt by user: " + token.getUsername() + " (Role: " + token.getRole() + ") for target: " + (data != null ? data.getTargetUser() : "null"));

        if (data == null || !data.isValid()) {
            LOG.warning("ChangeRole failed: Invalid input data. User: " + token.getUsername());
            return Response.status(Status.BAD_REQUEST).entity("Invalid input data. Provide targetUser and a valid newRole (ENDUSER, BACKOFFICE, ADMIN, PARTNER).").build();
        }
        String newRoleUpper = data.getNewRole().toUpperCase();

        if (!(token.getRole().equals("ADMIN") || token.getRole().equals("BACKOFFICE"))) {
            LOG.warning("ChangeRole failed: User " + token.getUsername() + " (Role: " + token.getRole() + ") does not have permission.");
            return Response.status(Status.FORBIDDEN).entity("User does not have permission for this operation.").build();
        }

        if (token.getUsername().equals(data.getTargetUser())) {
            LOG.warning("ChangeRole failed: User " + token.getUsername() + " attempted to change their own role.");
            return Response.status(Status.BAD_REQUEST).entity("Users cannot change their own role.").build();
        }


        Transaction txn = datastore.newTransaction();
        try {
            Key targetUserKey = userKeyFactory.newKey(data.getTargetUser());
            Entity targetUser = txn.get(targetUserKey);

            if (targetUser == null) {
                txn.rollback();
                LOG.warning("ChangeRole failed: Target user " + data.getTargetUser() + " not found. Requested by: " + token.getUsername());
                return Response.status(Status.NOT_FOUND).entity("Target user not found.").build();
            }

            String currentTargetRole = targetUser.contains(USER_ROLE_PROPERTY) ? targetUser.getString(USER_ROLE_PROPERTY) : "ENDUSER";

            if (token.getRole().equals("ADMIN")) {
                LOG.info("ADMIN " + token.getUsername() + " proceeding to change role of " + data.getTargetUser() + " from " + currentTargetRole + " to " + newRoleUpper);
            }
            else if (token.getRole().equals("BACKOFFICE")) {
                if (currentTargetRole.equals("ADMIN") || currentTargetRole.equals("BACKOFFICE")) {
                    txn.rollback();
                    LOG.warning("ChangeRole failed: BACKOFFICE user " + token.getUsername() + " attempted to change role of ADMIN/BACKOFFICE user " + data.getTargetUser());
                    return Response.status(Status.FORBIDDEN).entity("BACKOFFICE users cannot change role of ADMIN or other BACKOFFICE users.").build();
                }
                boolean isValidBackofficeChange = (currentTargetRole.equals("ENDUSER") && newRoleUpper.equals("PARTNER")) ||
                        (currentTargetRole.equals("PARTNER") && newRoleUpper.equals("ENDUSER"));
                if (!isValidBackofficeChange) {
                    txn.rollback();
                    LOG.warning("ChangeRole failed: BACKOFFICE user " + token.getUsername() + " attempted invalid role change for " + data.getTargetUser() + " (from " + currentTargetRole + " to " + newRoleUpper + ")");
                    return Response.status(Status.FORBIDDEN).entity("BACKOFFICE users can only change roles between ENDUSER and PARTNER.").build();
                }
                LOG.info("BACKOFFICE " + token.getUsername() + " proceeding to change role of " + data.getTargetUser() + " from " + currentTargetRole + " to " + newRoleUpper);
            }


            Entity updatedUser = Entity.newBuilder(targetUser)
                    .set(USER_ROLE_PROPERTY, newRoleUpper)
                    .build();
            txn.put(updatedUser);
            txn.commit();

            LOG.info("User " + token.getUsername() + " successfully changed role of user " + data.getTargetUser() + " to " + newRoleUpper);
            return Response.ok().entity("User role changed successfully.").build();

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangeRole Datastore error for target " + data.getTargetUser() + " by user " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing user role (Datastore).").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangeRole Unexpected error for target " + data.getTargetUser() + " by user " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing user role (Unexpected).").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }


    @POST
    @Path("/changepwd")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePassword(@HeaderParam("Authorization") String authorizationHeader, ChangePasswordData data) {

        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("ChangePassword failed: Invalid or expired token provided.");
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        String username = token.getUsername();
        LOG.info("ChangePassword attempt by user: " + username);

        if (data == null || !data.isValid()) {
            LOG.warning("ChangePassword failed: Invalid input data for user: " + username);
            return Response.status(Status.BAD_REQUEST).entity("Invalid input data. Provide currentPassword, newPassword, confirmation, and ensure new password meets complexity rules.").build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = userKeyFactory.newKey(username);
            Entity user = txn.get(userKey);

            if (user == null) {
                txn.rollback();
                LOG.severe("ChangePasswordConsistencyError: User " + username + " authenticated but not found in Datastore.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("User authenticated but not found.").build();
            }

            String currentHashedPwd = user.getString(USER_PWD_PROPERTY);
            String providedCurrentHash = DigestUtils.sha512Hex(data.getCurrentPassword());

            if (currentHashedPwd == null || !currentHashedPwd.equals(providedCurrentHash)) {
                txn.rollback();
                LOG.warning("ChangePassword failed: Incorrect current password provided by user: " + username);
                return Response.status(Status.FORBIDDEN).entity("Incorrect current password.").build();
            }


            String newHashedPwd = DigestUtils.sha512Hex(data.getNewPassword());

            Entity updatedUser = Entity.newBuilder(user)
                    .set(USER_PWD_PROPERTY, newHashedPwd)
                    .build();
            txn.put(updatedUser);
            txn.commit();

            LOG.info("User " + username + " successfully changed their password.");
            return Response.ok().entity("Password changed successfully.").build();

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangePassword Datastore error for user: " + username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing password (Datastore).").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangePassword Unexpected error for user: " + username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing password (Unexpected).").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/listusers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response listUsers(@HeaderParam("Authorization") String authorizationHeader) {

        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("ListUsers failed: Invalid or expired token provided.");
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        LOG.info("ListUsers attempt by user: " + token.getUsername() + " (Role: " + token.getRole() + ")");

        List<Map<String, Object>> usersData = new ArrayList<>();
        final String defaultNotDefined = "NOT DEFINED";

        try {
            com.google.cloud.datastore.EntityQuery.Builder queryBuilder = com.google.cloud.datastore.Query.newEntityQueryBuilder();
            queryBuilder.setKind("User");

            switch (token.getRole()) {
                case "ENDUSER":
                case "PARTNER":
                    queryBuilder.setFilter(
                            StructuredQuery.CompositeFilter.and(
                                    StructuredQuery.PropertyFilter.eq(USER_ROLE_PROPERTY, "ENDUSER"),
                                    StructuredQuery.PropertyFilter.eq(USER_STATE_PROPERTY, "ATIVADA"),
                                    StructuredQuery.PropertyFilter.eq("user_profile", "publico")
                            )
                    );
                    break;

                case "BACKOFFICE":
                    queryBuilder.setFilter(StructuredQuery.PropertyFilter.eq(USER_ROLE_PROPERTY, "ENDUSER"));
                    break;

                case "ADMIN":
                    break;

                default:
                    LOG.warning("ListUsers failed: Unknown role " + token.getRole() + " for user " + token.getUsername());
                    return Response.status(Status.FORBIDDEN).entity("User role cannot perform this operation.").build();
            }

            Query<Entity> query = queryBuilder.build();
            QueryResults<Entity> results = datastore.run(query);

            while (results.hasNext()) {
                Entity userEntity = results.next();
                Map<String, Object> userData = new HashMap<>();

                switch (token.getRole()) {
                    case "ENDUSER":
                    case "PARTNER":
                        userData.put("username", userEntity.getKey().getName());
                        userData.put("email", userEntity.contains("user_email") ? userEntity.getString("user_email") : defaultNotDefined);
                        userData.put("name", userEntity.contains("user_name") ? userEntity.getString("user_name") : defaultNotDefined);
                        break;

                    case "BACKOFFICE":
                    case "ADMIN":
                        userData.put("username", userEntity.getKey().getName());
                        userData.put("email", userEntity.contains("user_email") ? userEntity.getString("user_email") : defaultNotDefined);
                        userData.put("name", userEntity.contains("user_name") ? userEntity.getString("user_name") : defaultNotDefined);
                        userData.put("telefone", userEntity.contains("user_telefone") ? userEntity.getString("user_telefone") : defaultNotDefined);
                        userData.put("profile", userEntity.contains("user_profile") ? userEntity.getString("user_profile") : defaultNotDefined);
                        userData.put("role", userEntity.contains(USER_ROLE_PROPERTY) ? userEntity.getString(USER_ROLE_PROPERTY) : defaultNotDefined);
                        userData.put("state", userEntity.contains(USER_STATE_PROPERTY) ? userEntity.getString(USER_STATE_PROPERTY) : defaultNotDefined);
                        userData.put("nif", userEntity.contains("user_nif") ? userEntity.getString("user_nif") : defaultNotDefined);
                        userData.put("morada", userEntity.contains("user_morada") ? userEntity.getString("user_morada") : defaultNotDefined);
                        userData.put("funcao", userEntity.contains("user_funcao") ? userEntity.getString("user_funcao") : defaultNotDefined);
                        userData.put("entidade_empregadora", userEntity.contains("user_entidade_empregadora") ? userEntity.getString("user_entidade_empregadora") : defaultNotDefined);
                        userData.put("nif_entidade_empregadora", userEntity.contains("user_nif_entidade_empregadora") ? userEntity.getString("user_nif_entidade_empregadora") : defaultNotDefined);
                        break;
                }
                if (!userData.isEmpty()) {
                    usersData.add(userData);
                }
            }

            LOG.info("ListUsers successful for user: " + token.getUsername() + ". Returned " + usersData.size() + " users.");
            Gson g = new Gson();
            return Response.ok(g.toJson(usersData)).build();

        } catch (DatastoreException e) {
            LOG.log(Level.SEVERE, "ListUsers Datastore error for user: " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error listing users (Datastore).").build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "ListUsers Unexpected error for user: " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error listing users (Unexpected).").build();
        }
    }

    @DELETE
    @Path("/{username_to_delete}")
    public Response removeUserAccount(@HeaderParam("Authorization") String authorizationHeader,
                                      @PathParam("username_to_delete") String usernameToDelete) {

        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("RemoveUser failed: Invalid or expired token provided.");
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        LOG.info("RemoveUser attempt by user: " + token.getUsername() + " (Role: " + token.getRole() + ") for target: " + usernameToDelete);

        if (usernameToDelete == null || usernameToDelete.isBlank()) {
            LOG.warning("RemoveUser failed: Missing username to delete in path. Requested by: " + token.getUsername());
            return Response.status(Status.BAD_REQUEST).entity("Username to delete must be provided in the URL path.").build();
        }

        if (token.getUsername().equals(usernameToDelete)) {
            LOG.warning("RemoveUser failed: User " + token.getUsername() + " attempted to remove themselves.");
            return Response.status(Status.FORBIDDEN).entity("Users cannot remove their own account via this operation.").build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            Key targetUserKey = userKeyFactory.newKey(usernameToDelete);
            Entity targetUser = txn.get(targetUserKey);

            if (targetUser == null) {
                txn.rollback();
                LOG.warning("RemoveUser failed: Target user " + usernameToDelete + " not found. Requested by: " + token.getUsername());
                return Response.status(Status.FORBIDDEN).entity("Target user not found or insufficient permissions.").build();
            }

            String targetUserRole = targetUser.contains(USER_ROLE_PROPERTY) ? targetUser.getString(USER_ROLE_PROPERTY) : "ENDUSER";

            boolean canDelete = false;
            switch (token.getRole()) {
                case "ADMIN":
                    canDelete = true;
                    LOG.info("ADMIN " + token.getUsername() + " authorized to remove target " + usernameToDelete + " (Role: " + targetUserRole + ")");
                    break;
                case "BACKOFFICE":
                    if (targetUserRole.equals("ENDUSER") || targetUserRole.equals("PARTNER")) {
                        canDelete = true;
                        LOG.info("BACKOFFICE " + token.getUsername() + " authorized to remove target " + usernameToDelete + " (Role: " + targetUserRole + ")");
                    } else {
                        LOG.warning("RemoveUser failed: BACKOFFICE " + token.getUsername() + " cannot remove target " + usernameToDelete + " (Role: " + targetUserRole + ")");
                    }
                    break;
                case "ENDUSER":
                case "PARTNER":
                    LOG.warning("RemoveUser failed: ENDUSER/PARTNER " + token.getUsername() + " should not reach this point.");
                    canDelete = false;
                    break;
                default:
                    LOG.warning("RemoveUser failed: Unknown role " + token.getRole() + " for user " + token.getUsername());
                    canDelete = false;
                    break;
            }

            if (!canDelete) {
                txn.rollback();
                return Response.status(Status.FORBIDDEN).entity("User does not have permission to remove the target user.").build();
            }

            txn.delete(targetUserKey);

            try {
                Query<Key> tokenQuery = Query.newKeyQueryBuilder()
                        .setKind("AuthToken")
                        .setFilter(StructuredQuery.PropertyFilter.eq("username", usernameToDelete))
                        .build();
                QueryResults<Key> tokenKeys = txn.run(tokenQuery);
                int deletedTokens = 0;
                while(tokenKeys.hasNext()) {
                    txn.delete(tokenKeys.next());
                    deletedTokens++;
                }
                if (deletedTokens > 0) {
                    LOG.info("Deleted " + deletedTokens + " associated tokens for removed user: " + usernameToDelete);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error while trying to delete tokens for removed user: " + usernameToDelete, e);
            }


            txn.commit();

            LOG.info("User " + token.getUsername() + " successfully removed user: " + usernameToDelete);
            return Response.ok().entity("User removed successfully.").build();

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "RemoveUser Datastore error for target " + usernameToDelete + " by user " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error removing user (Datastore).").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "RemoveUser Unexpected error for target " + usernameToDelete + " by user " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error removing user (Unexpected).").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/updateatts")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAttributes(@HeaderParam("Authorization") String authorizationHeader, UpdateAttributesData data) {

        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("UpdateAtts failed: Invalid or expired token provided.");
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        LOG.info("UpdateAtts attempt by user: " + token.getUsername() + " (Role: " + token.getRole() + ") for target: " + (data != null ? data.getTargetUser() : "null"));

        if (data == null || !data.hasTargetUser()) {
            LOG.warning("UpdateAtts failed: Invalid input data (missing targetUser). User: " + token.getUsername());
            return Response.status(Status.BAD_REQUEST).entity("Invalid input data. Provide targetUser and attributes to update.").build();
        }

        boolean canUpdate;
        Entity targetUser;
        Transaction txn = datastore.newTransaction();

        try {
            Key targetUserKey = userKeyFactory.newKey(data.getTargetUser());
            targetUser = txn.get(targetUserKey);

            if (targetUser == null) {
                txn.rollback();
                LOG.warning("UpdateAtts failed: Target user " + data.getTargetUser() + " not found. Requested by: " + token.getUsername());
                return Response.status(Status.NOT_FOUND).entity("Target user not found.").build();
            }

            String targetUserRole = targetUser.contains(USER_ROLE_PROPERTY) ? targetUser.getString(USER_ROLE_PROPERTY) : "ENDUSER";

            switch (token.getRole()) {
                case "ADMIN":
                    canUpdate = true;
                    break;
                case "BACKOFFICE":
                    canUpdate = targetUserRole.equals("ENDUSER") || targetUserRole.equals("PARTNER");
                    if (!canUpdate) {
                        LOG.warning("UpdateAtts failed: BACKOFFICE " + token.getUsername() + " cannot update target " + data.getTargetUser() + " (Role: " + targetUserRole + ")");
                    }
                    break;
                case "ENDUSER":
                case "PARTNER":
                    canUpdate = token.getUsername().equals(data.getTargetUser());
                    if (!canUpdate) {
                        LOG.warning("UpdateAtts failed: User " + token.getUsername() + " cannot update target " + data.getTargetUser());
                    }
                    break;
                default:
                    canUpdate = false;
                    LOG.warning("UpdateAtts failed: Unknown role " + token.getRole() + " for user " + token.getUsername());
                    break;
            }

            if (!canUpdate) {
                txn.rollback();
                return Response.status(Status.FORBIDDEN).entity("User does not have permission to update the target user.").build();
            }

            Entity.Builder updatedUserBuilder = Entity.newBuilder(targetUser);
            boolean updated = false;

            if (data.hasName()) {
                updatedUserBuilder.set("user_name", data.getName());
                updated = true;
            }
            if (data.hasPhone()) {
                updatedUserBuilder.set("user_telefone", data.getPhone());
                updated = true;
            }
            if (data.hasProfile()) {
                if (data.getProfile().equalsIgnoreCase("publico") || data.getProfile().equalsIgnoreCase("privado")) {
                    updatedUserBuilder.set("user_profile", data.getProfile().toLowerCase());
                    updated = true;
                } else {
                    txn.rollback();
                    LOG.warning("UpdateAtts failed: Invalid profile value '" + data.getProfile() + "' provided by user " + token.getUsername());
                    return Response.status(Status.BAD_REQUEST).entity("Invalid profile value. Use 'publico' or 'privado'.").build();
                }
            }
            if (data.hasTaxId()) {
                updatedUserBuilder.set("user_nif", data.getTaxId());
                updated = true;
            }
            if (data.hasAddress()) {
                updatedUserBuilder.set("user_morada", data.getAddress());
                updated = true;
            }
            if (data.hasEmployer()) {
                updatedUserBuilder.set("user_entidade_empregadora", data.getEmployer());
                updated = true;
            }
            if (data.hasEmployerTaxId()) {
                updatedUserBuilder.set("user_nif_entidade_empregadora", data.getEmployerTaxId());
                updated = true;
            }
            if (data.hasJobTitle()) {
                updatedUserBuilder.set("user_funcao", data.getJobTitle());
                updated = true;
            }

            if (data.hasEmail()) {
                if (token.getRole().equals("ADMIN") || token.getRole().equals("BACKOFFICE")) {
                    if (data.getEmail().contains("@") && data.getEmail().contains(".")) {
                        updatedUserBuilder.set("user_email", data.getEmail());
                        updated = true;
                    } else {
                        txn.rollback();
                        LOG.warning("UpdateAtts failed: Invalid email format '" + data.getEmail() + "' provided by user " + token.getUsername());
                        return Response.status(Status.BAD_REQUEST).entity("Invalid email format provided.").build();
                    }
                } else {
                    txn.rollback();
                    LOG.warning("UpdateAtts failed: User " + token.getUsername() + " (Role: " + token.getRole() + ") attempted to change email.");
                    return Response.status(Status.FORBIDDEN).entity("Users cannot change their own email address.").build();
                }
            }

            if (updated) {
                Entity finalUpdatedUser = updatedUserBuilder.build();
                txn.put(finalUpdatedUser);
                txn.commit();
                LOG.info("User " + token.getUsername() + " successfully updated attributes for user " + data.getTargetUser());
                return Response.ok().entity("User attributes updated successfully.").build();
            } else {
                txn.rollback();
                LOG.info("UpdateAtts info: No valid attributes provided to update for user " + data.getTargetUser() + ". Requested by: " + token.getUsername());
                return Response.ok().entity("No attributes were updated.").build();
            }

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "UpdateAtts Datastore error for target " + data.getTargetUser() + " by user " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error updating attributes (Datastore).").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "UpdateAtts Unexpected error for target " + data.getTargetUser() + " by user " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error updating attributes (Unexpected).").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}