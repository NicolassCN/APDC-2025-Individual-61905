package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
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
import pt.unl.fct.di.apdc.indiv.util.data.RemoveUserData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RemoveUserResource {
    private static final Logger LOG = Logger.getLogger(RemoveUserResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson gson = new Gson();

    @POST
    @Path("/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeUser(@HeaderParam("Authorization") String authHeader, RemoveUserData data) {
        LOG.info("Attempting to remove user: " + data.identificador);

        try {
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
                        .entity(gson.toJson("Token expired."))
                        .build();
            }

            Key requesterKey = datastore.newKeyFactory().setKind("User").newKey(token.getUsername());
            Entity requesterEntity = datastore.get(requesterKey);
            if (requesterEntity == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("User not found."))
                        .build();
            }

            User requester = User.fromEntity(requesterEntity);

            Entity targetEntity = null;
            String targetIdentifier = data.identificador;

            Key usernameKey = datastore.newKeyFactory().setKind("User").newKey(targetIdentifier);
            targetEntity = datastore.get(usernameKey);

            if (targetEntity == null) {
                Query<Entity> query = Query.newEntityQueryBuilder()
                        .setKind("User")
                        .setFilter(StructuredQuery.PropertyFilter.eq("email", targetIdentifier))
                        .build();
                QueryResults<Entity> results = datastore.run(query);
                if (results.hasNext()) {
                    targetEntity = results.next();
                }
            }

            if (targetEntity == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(gson.toJson("Target user not found."))
                        .build();
            }

            User target = User.fromEntity(targetEntity);

            if (!requester.canRemoveUser(target)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(gson.toJson("Insufficient permissions to remove this account."))
                        .build();
            }

            datastore.delete(targetEntity.getKey());

            return Response.ok(gson.toJson(new RemoveUserResponse("Account removed successfully.", targetIdentifier)))
                    .build();

        } catch (Exception e) {
            LOG.severe("Error removing user: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(gson.toJson("Internal error processing user removal."))
                    .build();
        }
    }

    private static class RemoveUserResponse {
        public final String message;
        public final String identifier;

        public RemoveUserResponse(String message, String identifier) {
            this.message = message;
            this.identifier = identifier;
        }
    }
}