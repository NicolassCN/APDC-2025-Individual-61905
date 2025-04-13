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
import pt.unl.fct.di.apdc.indiv.filters.AuthenticationFilter;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.UserValidator;
import pt.unl.fct.di.apdc.indiv.util.UserValidator.ValidationResult;
import pt.unl.fct.di.apdc.indiv.util.data.UserData.ChangePasswordData;
import pt.unl.fct.di.apdc.indiv.util.data.UserData.LoginData;
import pt.unl.fct.di.apdc.indiv.util.data.UserData.LogoutData;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class AuthResource {
    private static final Logger LOG = Logger.getLogger(AuthResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(User user) {
        LOG.fine("Register attempt for user: " + user.getUsername());

        ValidationResult validationResult = UserValidator.validateUser(user);
        if (!validationResult.isValid()) {
            LOG.warning("Registration attempt with invalid data: " + validationResult.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(g.toJson(validationResult.getMessage()))
                    .build();
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(user.getUsername());
        Entity existingUser = datastore.get(userKey);
        if (existingUser != null) {
            LOG.warning("Registration attempt with existing username: " + user.getUsername());
            return Response.status(Response.Status.CONFLICT)
                    .entity(g.toJson("Username already exists"))
                    .build();
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("User")
                .setFilter(StructuredQuery.PropertyFilter.eq("email", user.getEmail()))
                .build();
        QueryResults<Entity> results = datastore.run(query);
        if (results.hasNext()) {
            LOG.warning("Registration attempt with existing email: " + user.getEmail());
            return Response.status(Response.Status.CONFLICT)
                    .entity(g.toJson("Email already in use"))
                    .build();
        }

        Entity userEntity = user.toEntity(userKey);
        datastore.put(userEntity);

        LOG.info("User registered successfully: " + user.getUsername());
        return Response.status(Response.Status.CREATED)
                .entity(g.toJson(new RegisterResponse("Account created successfully", user)))
                .build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(LoginData data) {
        LOG.fine("Login attempt for user: " + data.identifier);

        Entity user = null;
        
        // Try to find user by username
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.identifier);
        user = datastore.get(userKey);
        
        // If not found by username, try email
        if (user == null) {
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .setFilter(StructuredQuery.PropertyFilter.eq("email", data.identifier))
                    .build();
            QueryResults<Entity> results = datastore.run(query);
            if (results.hasNext()) {
                user = results.next();
            }
        }

        if (user == null) {
            LOG.warning("Failed login attempt for identifier: " + data.identifier);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson("Invalid credentials"))
                    .build();
        }

        User u = User.fromEntity(user);
        if (!u.getPassword().equals(data.password)) {
            LOG.warning("Failed login attempt for user: " + u.getUsername());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson("Invalid credentials"))
                    .build();
        }

        if (!u.isActive()) {
            LOG.warning("Failed login attempt for inactive user: " + u.getUsername());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson("Account is not active"))
                    .build();
        }

        AuthToken token = new AuthToken(u.getUsername(), u.getRole());
        AuthenticationFilter.addToken(u.getUsername(), token);

        LOG.info("User logged in successfully: " + u.getUsername());
        return Response.ok(g.toJson(token)).build();
    }

    @POST
    @Path("/logout")
    public Response logout(LogoutData data) {
        String username = AuthenticationFilter.getUsernameFromToken(data.token);
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson("Invalid or expired session"))
                    .build();
        }

        AuthenticationFilter.removeToken(username);
        return Response.ok(g.toJson("Logout successful. Session has been terminated.")).build();
    }

    @POST
    @Path("/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePassword(ChangePasswordData data) {
        AuthToken token = AuthenticationFilter.getTokenFromUsername(data.username);
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson("Invalid or expired session"))
                    .build();
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity userEntity = datastore.get(userKey);
        if (userEntity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(g.toJson("User not found"))
                    .build();
        }

        User user = User.fromEntity(userEntity);
        if (!user.getPassword().equals(data.currentPassword)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson("Current password is incorrect"))
                    .build();
        }

        ValidationResult validationResult = UserValidator.validatePasswordChange(
            data.currentPassword, data.newPassword, data.confirmPassword);
        if (!validationResult.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(g.toJson(validationResult.getMessage()))
                    .build();
        }

        user.setPassword(data.newPassword);
        datastore.put(user.toEntity(userKey));

        return Response.ok(g.toJson("Password changed successfully")).build();
    }
} 