package pt.unl.fct.di.apdc.indiv.resources;

import java.util.ArrayList;
import java.util.List;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.util.AccessControl;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.UserValidator;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final String USER_KIND = "User";

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUser(User user) {
        // Validate user input
        UserValidator.ValidationResult validationResult = UserValidator.validateUser(user);
        if (!validationResult.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(validationResult.getMessage()).build();
        }

        // Check if user already exists
        Key userKey = datastore.newKeyFactory().setKind(USER_KIND).newKey(user.getUsername());
        if (datastore.get(userKey) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Username already exists").build();
        }

        // Check if email is already in use
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(USER_KIND)
                .setFilter(StructuredQuery.PropertyFilter.eq("email", user.getEmail()))
                .build();
        if (datastore.run(query).hasNext()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Email already in use").build();
        }

        // Store the user
        Entity userEntity = Entity.newBuilder(userKey)
                .set("email", user.getEmail())
                .set("password", user.getPassword()) // In production, this should be hashed
                .set("role", user.getRole().toString())
                .set("name", user.getName())
                .set("phone", user.getPhone())
                .set("address", user.getAddress())
                .set("nif", user.getNif())
                .set("cc", user.getCc())
                .set("isActive", user.isActive())
                .set("token", user.getToken())
                .build();

        datastore.put(userEntity);
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(LoginData loginData) {
        // Try to find user by username or email
        Entity userEntity = null;
        Key userKey = datastore.newKeyFactory().setKind(USER_KIND).newKey(loginData.identifier);
        userEntity = datastore.get(userKey);

        if (userEntity == null) {
            // Try to find by email
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind(USER_KIND)
                    .setFilter(StructuredQuery.PropertyFilter.eq("email", loginData.identifier))
                    .build();
            QueryResults<Entity> results = datastore.run(query);
            if (results.hasNext()) {
                userEntity = results.next();
            }
        }

        if (userEntity == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid credentials").build();
        }

        User user = entityToUser(userEntity);
        if (!user.getPassword().equals(loginData.password)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid credentials").build();
        }

        if (!user.isActive()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Account is not active").build();
        }

        AuthToken token = new AuthToken(user.getUsername(), user.getRole());
        user.setToken(token.getTokenId());
        updateUserEntity(userEntity.getKey(), user);

        return Response.ok(token).build();
    }

    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Authorization") String token) {
        if (token == null || token.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("No token provided").build();
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(USER_KIND)
                .setFilter(StructuredQuery.PropertyFilter.eq("token", token))
                .build();
        QueryResults<Entity> results = datastore.run(query);

        if (!results.hasNext()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token").build();
        }

        Entity userEntity = results.next();
        User user = entityToUser(userEntity);
        user.setToken(null);
        updateUserEntity(userEntity.getKey(), user);

        return Response.ok("Logged out successfully").build();
    }

    @GET
    @Path("/{username}")
    public Response getUser(@PathParam("username") String username,
                          @HeaderParam("Authorization") String token) {
        // Get the requesting user from token
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(USER_KIND)
                .setFilter(StructuredQuery.PropertyFilter.eq("token", token))
                .build();
        QueryResults<Entity> results = datastore.run(query);
        
        if (!results.hasNext()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Entity requestingUserEntity = results.next();
        User requestingUser = entityToUser(requestingUserEntity);

        // Get the requested user
        Key userKey = datastore.newKeyFactory().setKind(USER_KIND).newKey(username);
        Entity userEntity = datastore.get(userKey);
        
        if (userEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        User targetUser = entityToUser(userEntity);

        // Check access control
        AccessControl ac = new AccessControl(requestingUser, User.Role.USER);
        if (!ac.hasAccess() && !requestingUser.getUsername().equals(username)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return Response.ok(targetUser).build();
    }

    @PUT
    @Path("/{username}/role")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeRole(@PathParam("username") String username,
                             @HeaderParam("Authorization") String token,
                             RoleChangeData roleData) {
        // Validate token and get requesting user
        User requestingUser = getUserFromToken(token);
        if (requestingUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // Check if user has permission to change roles
        AccessControl ac = new AccessControl(requestingUser, User.Role.ADMIN);
        if (!ac.hasAccess()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Insufficient permissions").build();
        }

        // Get target user
        Key userKey = datastore.newKeyFactory().setKind(USER_KIND).newKey(username);
        Entity userEntity = datastore.get(userKey);
        if (userEntity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("User not found").build();
        }

        // Update role
        User targetUser = entityToUser(userEntity);
        try {
            targetUser.setRole(User.Role.valueOf(roleData.newRole));
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid role").build();
        }

        updateUserEntity(userKey, targetUser);
        return Response.ok("Role updated successfully").build();
    }

    @PUT
    @Path("/{username}/status")
    public Response toggleStatus(@PathParam("username") String username,
                               @HeaderParam("Authorization") String token) {
        // Validate token and get requesting user
        User requestingUser = getUserFromToken(token);
        if (requestingUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // Check if user has permission to change status
        AccessControl ac = new AccessControl(requestingUser, User.Role.GBO);
        if (!ac.hasAccess()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Insufficient permissions").build();
        }

        // Get target user
        Key userKey = datastore.newKeyFactory().setKind(USER_KIND).newKey(username);
        Entity userEntity = datastore.get(userKey);
        if (userEntity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("User not found").build();
        }

        // Toggle status
        User targetUser = entityToUser(userEntity);
        targetUser.setActive(!targetUser.isActive());
        updateUserEntity(userKey, targetUser);

        return Response.ok("Status updated successfully").build();
    }

    @GET
    @Path("/list")
    public Response listUsers(@HeaderParam("Authorization") String token) {
        // Validate token and get requesting user
        User requestingUser = getUserFromToken(token);
        if (requestingUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // Check if user has permission to list users
        AccessControl ac = new AccessControl(requestingUser, User.Role.GBO);
        if (!ac.hasAccess()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Insufficient permissions").build();
        }

        // Get all users
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(USER_KIND)
                .build();
        QueryResults<Entity> results = datastore.run(query);

        List<User> users = new ArrayList<>();
        results.forEachRemaining(entity -> users.add(entityToUser(entity)));

        return Response.ok(users).build();
    }

    private User getUserFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(USER_KIND)
                .setFilter(StructuredQuery.PropertyFilter.eq("token", token))
                .build();
        QueryResults<Entity> results = datastore.run(query);

        if (!results.hasNext()) {
            return null;
        }

        return entityToUser(results.next());
    }

    private void updateUserEntity(Key userKey, User user) {
        Entity userEntity = Entity.newBuilder(userKey)
                .set("email", user.getEmail())
                .set("password", user.getPassword())
                .set("role", user.getRole().toString())
                .set("name", user.getName())
                .set("phone", user.getPhone())
                .set("address", user.getAddress())
                .set("nif", user.getNif())
                .set("cc", user.getCc())
                .set("isActive", user.isActive())
                .set("token", user.getToken() != null ? user.getToken() : "")
                .build();
        datastore.put(userEntity);
    }

    private User entityToUser(Entity entity) {
        User user = new User();
        user.setUsername(entity.getKey().getName());
        user.setEmail(entity.getString("email"));
        user.setRole(User.Role.valueOf(entity.getString("role")));
        user.setName(entity.getString("name"));
        user.setPhone(entity.getString("phone"));
        user.setAddress(entity.getString("address"));
        user.setNif(entity.getString("nif"));
        user.setCc(entity.getString("cc"));
        user.setActive(entity.getBoolean("isActive"));
        user.setToken(entity.getString("token"));
        return user;
    }
}

class LoginData {
    public String identifier;
    public String password;
}

class RoleChangeData {
    public String newRole;
} 