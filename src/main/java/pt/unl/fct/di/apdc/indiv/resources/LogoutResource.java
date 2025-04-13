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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.data.LogoutData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LogoutResource {
    private static final Logger LOG = Logger.getLogger(LogoutResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson gson = new Gson();

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logout(LogoutData data) {
        LOG.info("Logout attempt for token: " + data.getToken());

        try {
            if (data.getToken() == null || data.getToken().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson("Token is required."))
                        .build();
            }

            // Get token from Datastore
            Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(data.getToken());
            Entity tokenEntity = datastore.get(tokenKey);

            if (tokenEntity == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("Invalid or expired session."))
                        .build();
            }

            // Convert to AuthToken object using fromEntity method
            AuthToken token = AuthToken.fromEntity(tokenEntity);

            // Check if token is expired
            if (token.isExpired()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("Invalid or expired session."))
                        .build();
            }

            // Invalidate token and update in Datastore
            token.invalidate();
            datastore.put(token.toEntity(datastore));

            return Response.ok(gson.toJson("Logout successful. Session has been terminated.")).build();

        } catch (Exception e) {
            LOG.severe("Error during logout: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(gson.toJson("Internal error processing logout."))
                    .build();
        }
    }
}