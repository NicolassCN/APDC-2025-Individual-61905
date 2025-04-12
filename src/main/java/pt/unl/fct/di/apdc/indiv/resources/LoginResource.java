package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import com.google.appengine.repackaged.com.google.gson.Gson;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.exceptions.AppException;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.LoginData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private final Gson gson = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(LoginData data) {
        LOG.fine("Login attempt for identifier: " + data.getIdentifier());

        try {
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .setFilter(CompositeFilter.or(
                            PropertyFilter.eq("username", data.getIdentifier()),
                            PropertyFilter.eq("email", data.getIdentifier())))
                    .build();
            QueryResults<Entity> results = datastore.run(query);

            if (!results.hasNext()) {
                throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), "Invalid credentials");
            }

            Entity userEntity = results.next();
            if (!userEntity.getString("password").equals(data.getPassword())) {
                throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), "Invalid credentials");
            }

            AuthToken token = new AuthToken(userEntity.getString("username"), userEntity.getString("role"));
            return Response.ok(gson.toJson(new LoginResponse(token))).build();
        } catch (AppException e) {
            LOG.warning("Login failed: " + e.getMessage());
            return Response.status(e.getStatus()).entity(gson.toJson(new ErrorResponse(e.getMessage()))).build();
        }
    }

    private static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class LoginResponse {
        public TokenResponse token;
        public LoginResponse(AuthToken token) {
            this.token = new TokenResponse(token);
        }
    }

    private static class TokenResponse {
        public String user;
        public String role;
        public Validity validity;
        public TokenResponse(AuthToken token) {
            this.user = token.getUsername();
            this.role = token.getRole();
            this.validity = new Validity(token.getValidFrom(), token.getValidTo(), token.getVerifier());
        }
    }

    private static class Validity {
        public String validFrom;
        public String validTo;
        public String verifier;
        public Validity(String validFrom, String validTo, String verifier) {
            this.validFrom = validFrom;
            this.validTo = validTo;
            this.verifier = verifier;
        }
    }
}