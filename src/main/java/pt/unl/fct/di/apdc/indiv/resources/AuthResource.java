package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
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
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.UserValidator;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final Logger LOG = Logger.getLogger(AuthResource.class.getName());
    private final Datastore datastore;
    private final KeyFactory userKeyFactory;
    private final Gson g = new Gson();

    public AuthResource() {
        this.datastore = DatastoreOptions.getDefaultInstance().getService();
        this.userKeyFactory = datastore.newKeyFactory().setKind("User");
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(User user) {
        UserValidator.ValidationResult validationResult = UserValidator.validateUser(user);
        if (!validationResult.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid user data: " + validationResult.getMessage())
                    .build();
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("User")
                .setFilter(StructuredQuery.CompositeFilter.and(
                        StructuredQuery.PropertyFilter.eq("username", user.getUsername()),
                        StructuredQuery.PropertyFilter.eq("email", user.getEmail())))
                .build();

        QueryResults<Entity> results = datastore.run(query);
        if (results.hasNext()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Username or email already exists")
                    .build();
        }

        Key userKey = userKeyFactory.newKey(user.getUsername());
        Entity userEntity = Entity.newBuilder(userKey)
                .set("username", user.getUsername())
                .set("email", user.getEmail())
                .set("password", user.getPassword())
                .set("name", user.getName())
                .set("phone", user.getPhone())
                .set("address", user.getAddress())
                .set("nif", user.getNif())
                .set("cc", user.getCc())
                .set("role", user.getRole().toString())
                .set("isActive", user.isActive())
                .set("token", "")
                .build();

        datastore.put(userEntity);
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(LoginData loginData) {
        if (loginData.getUsername() == null || loginData.getPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing credentials")
                    .build();
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("User")
                .setFilter(StructuredQuery.PropertyFilter.eq("username", loginData.getUsername()))
                .build();

        QueryResults<Entity> results = datastore.run(query);
        if (!results.hasNext()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid credentials")
                    .build();
        }

        Entity userEntity = results.next();
        if (!userEntity.getString("password").equals(loginData.getPassword())) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid credentials")
                    .build();
        }

        User user = entityToUser(userEntity);
        AuthToken token = new AuthToken(user.getUsername(), user.getRole());
        return Response.ok(token).build();
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logout(@HeaderParam("Authorization") String token) {
        if (token == null || token.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("No token provided")
                    .build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .setFilter(StructuredQuery.PropertyFilter.eq("token", token))
                    .build();
            QueryResults<Entity> results = txn.run(query);

            if (!results.hasNext()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Invalid token")
                        .build();
            }

            Entity userEntity = results.next();
            Entity updatedUser = Entity.newBuilder(userEntity)
                    .set("token", "")
                    .build();
            txn.put(updatedUser);
            txn.commit();

            return Response.ok("Logout successful").build();
        } catch (Exception e) {
            txn.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error during logout: " + e.getMessage())
                    .build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    private User entityToUser(Entity entity) {
        User user = new User(
                entity.getKey().getName(),
                entity.getString("password"),
                entity.getString("email"),
                entity.getString("name"),
                entity.getString("phone"),
                entity.getString("address"),
                entity.getString("nif"),
                entity.getString("cc")
        );
        user.setRole(User.Role.valueOf(entity.getString("role")));
        user.setActive(entity.getBoolean("isActive"));
        if (entity.contains("token")) {
            user.setToken(entity.getString("token"));
        }
        return user;
    }

    private static class LoginData {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
} 