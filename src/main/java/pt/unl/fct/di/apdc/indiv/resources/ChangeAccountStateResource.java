package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.data.ChangeAccountStateData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeAccountStateResource {
    private static final Logger LOG = Logger.getLogger(ChangeAccountStateResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson gson = new Gson();

    @POST
    @Path("/changeAccountState")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAccountState(@HeaderParam("Authorization") String authHeader, ChangeAccountStateData data) {
        LOG.info("Attempting to change account state for user: " + data.username);

        try {
            // Validate token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("Authentication token is required."))
                        .build();
            }

            String tokenId = authHeader.substring("Bearer ".length());
            Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenId);
            Entity tokenEntity = datastore.get(tokenKey);

            if (tokenEntity == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("Invalid token."))
                        .build();
            }

            AuthToken token = AuthToken.fromEntity(tokenEntity);
            if (token.isExpired()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("Token has expired."))
                        .build();
            }

            // Get requesting user
            Key requesterKey = datastore.newKeyFactory().setKind("User").newKey(token.getUsername());
            Entity requesterEntity = datastore.get(requesterKey);
            if (requesterEntity == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("User not found."))
                        .build();
            }

            User requester = User.fromEntity(requesterEntity);

            // Get target user
            Key targetKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
            Entity targetEntity = datastore.get(targetKey);
            if (targetEntity == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(gson.toJson("Target user not found."))
                        .build();
            }

            User target = User.fromEntity(targetEntity);

            // Check permissions
            if (!requester.canChangeAccountState(target)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(gson.toJson("Insufficient permissions to change this account's state."))
                        .build();
            }

            // Validate new state
            try {
                User.AccountState newState = User.AccountState.valueOf(data.newState.toUpperCase());
                target.setAccountState(newState);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson("Invalid state."))
                        .build();
            }

            // Update in Datastore
            datastore.put(target.toEntity(datastore));

            return Response.ok(gson.toJson(new ChangeAccountStateResponse("Account state updated successfully.", data.username, data.newState)))
                    .build();

        } catch (Exception e) {
            LOG.severe("Error changing account state: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(gson.toJson("Internal error processing account state change."))
                    .build();
        }
    }

    private static class ChangeAccountStateResponse {
        public final String message;
        public final String username;
        public final String new_state;

        public ChangeAccountStateResponse(String message, String username, String newState) {
            this.message = message;
            this.username = username;
            this.new_state = newState;
        }
    }
}