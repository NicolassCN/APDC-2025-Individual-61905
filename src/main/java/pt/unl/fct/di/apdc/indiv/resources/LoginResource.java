package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.LoginData;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doLogin(LoginData data) {
        LOG.info("Login attempt for user: " + data.username);

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Transaction txn = datastore.newTransaction();

        try {
            Entity user = txn.get(userKey);
            if (user == null) {
                LOG.warning("Failed login attempt for non-existent user: " + data.username);
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials.").build();
            }

            String hashedPWD = user.getString("password");
            String inputHashedPWD = DigestUtils.sha512Hex(data.password);
            
            LOG.info("User found: " + data.username);
            LOG.info("Account state: " + user.getString("accountState"));
            
            if (!hashedPWD.equals(inputHashedPWD)) {
                LOG.warning("Failed login attempt (wrong password) for user: " + data.username);
                LOG.warning("Expected hash: " + hashedPWD);
                LOG.warning("Received hash: " + inputHashedPWD);
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials.").build();
            }

            // Check account state
            String accountState = user.getString("accountState");
            if (!accountState.equals("ATIVADA")) {
                LOG.warning("Failed login attempt for inactive user: " + data.username);
                return Response.status(Response.Status.FORBIDDEN).entity("User is not active.").build();
            }

            // If credentials are correct and user is active, generate token
            AuthToken token = new AuthToken(data.username, user.getString("role"));
            Key tokenKey = datastore.newKeyFactory()
                    .addAncestors(PathElement.of("User", data.username))
                    .setKind("AuthToken")
                    .newKey(token.getTokenString());

            Entity tokenEntity = Entity.newBuilder(tokenKey)
                    .set("username", token.getUsername())
                    .set("role", token.getRole())
                    .set("creationDate", token.getCreationDate())
                    .set("expirationDate", token.getExpirationDate())
                    .set("verifier", token.getVerifier())
                    .build();

            txn.put(tokenEntity);
            txn.commit();

            LOG.info("User '" + data.username + "' logged in successfully.");
            return Response.ok(g.toJson(token)).build();
        } catch (DatastoreException e) {
            txn.rollback();
            LOG.severe("Error during login for user " + data.username + ": " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Datastore error during login.").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}