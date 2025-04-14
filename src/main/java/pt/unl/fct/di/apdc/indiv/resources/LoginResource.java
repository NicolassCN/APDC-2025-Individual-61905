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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.data.LoginData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson gson = new Gson();

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doLogin(LoginData data) {
        LOG.info("Login attempt for: " + data.identifier);

        try {
            if (data.identifier == null || data.password == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson("Identifier and password are required."))
                        .build();
            }

            // Search user by username or email
            Entity userEntity = null;
            
            // Try to find by username
            Key usernameKey = datastore.newKeyFactory().setKind("User").newKey(data.identifier);
            userEntity = datastore.get(usernameKey);
            
            // If not found by username, try by email
            if (userEntity == null) {
                Query<Entity> query = Query.newEntityQueryBuilder()
                        .setKind("User")
                        .setFilter(StructuredQuery.PropertyFilter.eq("email", data.identifier))
                        .build();
                QueryResults<Entity> results = datastore.run(query);
                if (results.hasNext()) {
                    userEntity = results.next();
                }
            }

            if (userEntity == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("User not found. Please check your username/email."))
                        .build();
            }

            User user = User.fromEntity(userEntity);
            
            // Verify password
            if (!user.isPasswordValid(data.password)) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("Invalid password. Please try again."))
                        .build();
            }

            // Check if account is activated
            if (user.getAccountState() != User.AccountState.ACTIVATED) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(gson.toJson("Your account is not activated. Please contact support."))
                        .build();
            }

            // Create authentication token
            AuthToken token = new AuthToken(user.getUsername(), user.getRole().name());
            
            // Save token in Datastore
            datastore.put(token.toEntity(datastore));

            // Create response
            LoginResponse response = new LoginResponse(token);
            
            return Response.ok(gson.toJson(response)).build();

        } catch (Exception e) {
            LOG.severe("Error during login: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(gson.toJson("Internal error processing login."))
                    .build();
        }
    }

    private static class LoginResponse {
        public final TokenInfo token;

        public LoginResponse(AuthToken authToken) {
            this.token = new TokenInfo(authToken);
        }
    }

    private static class TokenInfo {
        public final UserInfo user;
        public final ValidityInfo validity;

        public TokenInfo(AuthToken token) {
            this.user = new UserInfo(token.getUsername(), token.getRole());
            this.validity = new ValidityInfo(token);
        }
    }

    private static class UserInfo {
        public final String user;
        public final String role;

        public UserInfo(String username, String role) {
            this.user = username;
            this.role = role;
        }
    }

    private static class ValidityInfo {
        public final String valid_from;
        public final String valid_to;
        public final String verifier;

        public ValidityInfo(AuthToken token) {
            this.valid_from = new java.util.Date(token.getCreationDate()).toInstant().toString();
            this.valid_to = new java.util.Date(token.getExpirationDate()).toInstant().toString();
            this.verifier = token.getVerifier();
        }
    }
}