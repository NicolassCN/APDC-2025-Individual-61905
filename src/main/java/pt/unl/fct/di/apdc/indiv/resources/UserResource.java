package pt.unl.fct.di.apdc.indiv.resources;

import java.util.ArrayList;
import java.util.List;
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

@Path("/api/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class UserResource {
    private static final Logger LOG = Logger.getLogger(UserResource.class.getName());
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
                    .entity(g.toJson(new ErrorResponse(validationResult.getMessage())))
                    .build();
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(user.getUsername());
        Entity existingUser = datastore.get(userKey);
        if (existingUser != null) {
            LOG.warning("Registration attempt with existing username: " + user.getUsername());
            return Response.status(Response.Status.CONFLICT)
                    .entity(g.toJson(new ErrorResponse("Username already exists")))
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
                    .entity(g.toJson(new ErrorResponse("Email already in use")))
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

        Entity userEntity = null;
        
        // Try to find user by username
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.identifier);
        userEntity = datastore.get(userKey);
        
        // If not found by username, try email
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
            LOG.warning("Failed login attempt for identifier: " + data.identifier);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson(new ErrorResponse("Invalid credentials")))
                    .build();
        }

        User user = User.fromEntity(userEntity);
        if (!user.getPassword().equals(data.password)) {
            LOG.warning("Failed login attempt for user: " + user.getUsername());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson(new ErrorResponse("Invalid credentials")))
                    .build();
        }

        if (!user.isActive()) {
            LOG.warning("Failed login attempt for inactive user: " + user.getUsername());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson(new ErrorResponse("Account is not active")))
                    .build();
        }

        AuthToken token = new AuthToken(user.getUsername(), user.getRole());
        AuthenticationFilter.addToken(user.getUsername(), token);

        LOG.info("User logged in successfully: " + user.getUsername());
        return Response.ok(g.toJson(new LoginResponse(token))).build();
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logout(LogoutData data) {
        String username = AuthenticationFilter.getUsernameFromToken(data.token);
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson(new ErrorResponse("Invalid or expired session")))
                    .build();
        }

        AuthenticationFilter.removeToken(username);
        return Response.ok(g.toJson(new SuccessResponse("Logout successful. Session has been terminated."))).build();
    }

    @POST
    @Path("/change-role")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeRole(ChangeRoleData data) {
        AuthToken token = AuthenticationFilter.getTokenFromHeader(data.authHeader);
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson(new ErrorResponse("Invalid or expired session")))
                    .build();
        }

        Key targetUserKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity targetUserEntity = datastore.get(targetUserKey);
        if (targetUserEntity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(g.toJson(new ErrorResponse("User not found")))
                    .build();
        }

        User targetUser = User.fromEntity(targetUserEntity);
        User requester = User.fromEntity(datastore.get(
                datastore.newKeyFactory().setKind("User").newKey(token.getUser())));

        if (!requester.isAdmin() && !requester.isBackOffice()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson(new ErrorResponse("Insufficient permissions to change user role")))
                    .build();
        }

        if (requester.isBackOffice() && 
            (!targetUser.isEndUser() && !targetUser.isPartner() ||
             !data.newRole.equals("ENDUSER") && !data.newRole.equals("PARTNER"))) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson(new ErrorResponse("BACKOFFICE can only change roles between ENDUSER and PARTNER")))
                    .build();
        }

        targetUser.setRole(data.newRole);
        datastore.put(targetUser.toEntity(targetUserKey));

        return Response.ok(g.toJson(new ChangeRoleResponse("Role updated successfully", data.username, data.newRole))).build();
    }

    @POST
    @Path("/change-account-state")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAccountState(ChangeAccountStateData data) {
        AuthToken token = AuthenticationFilter.getTokenFromHeader(data.authHeader);
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson(new ErrorResponse("Invalid or expired session")))
                    .build();
        }

        Key targetUserKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity targetUserEntity = datastore.get(targetUserKey);
        if (targetUserEntity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(g.toJson(new ErrorResponse("User not found")))
                    .build();
        }

        User requester = User.fromEntity(datastore.get(
                datastore.newKeyFactory().setKind("User").newKey(token.getUser())));

        if (!requester.isAdmin() && !requester.isBackOffice()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson(new ErrorResponse("Insufficient permissions to change account state")))
                    .build();
        }

        User targetUser = User.fromEntity(targetUserEntity);
        targetUser.setAccountState(data.newState);
        datastore.put(targetUser.toEntity(targetUserKey));

        return Response.ok(g.toJson(new ChangeAccountStateResponse(
            "Account state updated successfully", data.username, data.newState))).build();
    }

    @POST
    @Path("/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeAccount(RemoveAccountData data) {
        AuthToken token = AuthenticationFilter.getTokenFromHeader(data.authHeader);
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson(new ErrorResponse("Invalid or expired session")))
                    .build();
        }

        Entity targetUserEntity = null;
        Key targetUserKey = null;

        if (data.username != null) {
            targetUserKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
            targetUserEntity = datastore.get(targetUserKey);
        } else if (data.email != null) {
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .setFilter(StructuredQuery.PropertyFilter.eq("email", data.email))
                    .build();
            QueryResults<Entity> results = datastore.run(query);
            if (results.hasNext()) {
                targetUserEntity = results.next();
                targetUserKey = targetUserEntity.getKey();
            }
        }

        if (targetUserEntity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(g.toJson(new ErrorResponse("User not found")))
                    .build();
        }

        User requester = User.fromEntity(datastore.get(
                datastore.newKeyFactory().setKind("User").newKey(token.getUser())));
        User targetUser = User.fromEntity(targetUserEntity);

        if (!requester.isAdmin() && !requester.isBackOffice()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson(new ErrorResponse("Insufficient permissions to remove account")))
                    .build();
        }

        if (requester.isBackOffice() && !targetUser.isEndUser() && !targetUser.isPartner()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson(new ErrorResponse("BACKOFFICE can only remove ENDUSER or PARTNER accounts")))
                    .build();
        }

        datastore.delete(targetUserKey);
        String identifier = data.username != null ? data.username : data.email;
        return Response.ok(g.toJson(new RemoveAccountResponse("Account removed successfully", identifier))).build();
    }

    @POST
    @Path("/all")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listUsers(ListUsersData data) {
        AuthToken token = AuthenticationFilter.getTokenFromHeader(data.authHeader);
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson(new ErrorResponse("Invalid or expired session")))
                    .build();
        }

        User requester = User.fromEntity(datastore.get(
                datastore.newKeyFactory().setKind("User").newKey(token.getUser())));

        Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();
        QueryResults<Entity> results = datastore.run(query);

        List<User> users = new ArrayList<>();
        while (results.hasNext()) {
            User user = User.fromEntity(results.next());

            if (requester.isEndUser()) {
                if (user.isEndUser() && user.isPublicProfile() && user.isActive()) {
                    users.add(filterUserFields(user, "ENDUSER"));
                }
            } else if (requester.isBackOffice()) {
                if (user.isEndUser()) {
                    users.add(user);
                }
            } else if (requester.isAdmin()) {
                users.add(user);
            }
        }

        return Response.ok(g.toJson(users)).build();
    }

    @POST
    @Path("/change-attributes")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAttributes(ChangeAttributesData data) {
        AuthToken token = AuthenticationFilter.getTokenFromHeader(data.authHeader);
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson(new ErrorResponse("Invalid or expired session")))
                    .build();
        }

        Entity targetUserEntity = null;
        Key targetUserKey = null;

        if (data.identifier != null) {
            targetUserKey = datastore.newKeyFactory().setKind("User").newKey(data.identifier);
            targetUserEntity = datastore.get(targetUserKey);
            if (targetUserEntity == null) {
                Query<Entity> query = Query.newEntityQueryBuilder()
                        .setKind("User")
                        .setFilter(StructuredQuery.PropertyFilter.eq("email", data.identifier))
                        .build();
                QueryResults<Entity> results = datastore.run(query);
                if (results.hasNext()) {
                    targetUserEntity = results.next();
                    targetUserKey = targetUserEntity.getKey();
                }
            }
        }

        if (targetUserEntity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(g.toJson(new ErrorResponse("User not found")))
                    .build();
        }

        User requester = User.fromEntity(datastore.get(
                datastore.newKeyFactory().setKind("User").newKey(token.getUser())));
        User targetUser = User.fromEntity(targetUserEntity);

        if (!requester.isAdmin() && !requester.isBackOffice() && 
            !requester.getUsername().equals(targetUser.getUsername())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson(new ErrorResponse("Insufficient permissions to modify attributes")))
                    .build();
        }

        if (requester.isEndUser()) {
            if (data.attributes.containsKey("username") || data.attributes.containsKey("email") ||
                data.attributes.containsKey("role") || data.attributes.containsKey("account_state")) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(g.toJson(new ErrorResponse("Cannot modify restricted attributes")))
                        .build();
            }
        } else if (requester.isBackOffice()) {
            if (targetUser.isEndUser() || targetUser.isPartner()) {
                if (data.attributes.containsKey("username") || data.attributes.containsKey("email")) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(g.toJson(new ErrorResponse("Cannot modify username or email")))
                            .build();
                }
            } else {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(g.toJson(new ErrorResponse("Can only modify ENDUSER or PARTNER accounts")))
                        .build();
            }
        }

        updateUserAttributes(targetUser, data.attributes);
        datastore.put(targetUser.toEntity(targetUserKey));

        return Response.ok(g.toJson(new ChangeAttributesResponse(
            "Account attributes modified successfully", data.identifier))).build();
    }

    @POST
    @Path("/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePassword(ChangePasswordData data) {
        AuthToken token = AuthenticationFilter.getTokenFromHeader(data.authHeader);
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson(new ErrorResponse("Invalid or expired session")))
                    .build();
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(token.getUser());
        Entity userEntity = datastore.get(userKey);
        if (userEntity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(g.toJson(new ErrorResponse("User not found")))
                    .build();
        }

        User user = User.fromEntity(userEntity);
        if (!user.getPassword().equals(data.currentPassword)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson(new ErrorResponse("Current password is incorrect")))
                    .build();
        }

        ValidationResult validationResult = UserValidator.validatePasswordChange(
            data.currentPassword, data.newPassword, data.confirmPassword);
        if (!validationResult.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(g.toJson(new ErrorResponse(validationResult.getMessage())))
                    .build();
        }

        user.setPassword(data.newPassword);
        datastore.put(user.toEntity(userKey));

        return Response.ok(g.toJson(new SuccessResponse("Password changed successfully"))).build();
    }

    private User filterUserFields(User user, String viewerRole) {
        User filtered = new User();
        filtered.setUsername(user.getUsername());
        filtered.setEmail(user.getEmail());
        filtered.setFullName(user.getFullName());

        if (viewerRole.equals("ENDUSER")) {
            return filtered;
        }

        filtered.setPhone(user.getPhone());
        filtered.setProfile(user.getProfile());
        filtered.setCitizenCard(user.getCitizenCard());
        filtered.setRole(user.getRole());
        filtered.setNif(user.getNif());
        filtered.setEmployer(user.getEmployer());
        filtered.setJobTitle(user.getJobTitle());
        filtered.setAddress(user.getAddress());
        filtered.setEmployerNif(user.getEmployerNif());
        filtered.setAccountState(user.getAccountState());
        filtered.setPhoto(user.getPhoto());

        return filtered;
    }

    private void updateUserAttributes(User user, java.util.Map<String, String> attributes) {
        for (java.util.Map.Entry<String, String> entry : attributes.entrySet()) {
            switch (entry.getKey()) {
                case "phone": user.setPhone(entry.getValue()); break;
                case "profile": user.setProfile(entry.getValue()); break;
                case "citizen_card": user.setCitizenCard(entry.getValue()); break;
                case "role": user.setRole(entry.getValue()); break;
                case "tax_id": user.setNif(entry.getValue()); break;
                case "employer": user.setEmployer(entry.getValue()); break;
                case "job_title": user.setJobTitle(entry.getValue()); break;
                case "address": user.setAddress(entry.getValue()); break;
                case "employer_tax_id": user.setEmployerNif(entry.getValue()); break;
                case "account_state": user.setAccountState(entry.getValue()); break;
                case "photo": user.setPhoto(entry.getValue()); break;
            }
        }
    }
}

// Response classes
class ErrorResponse {
    public String error;
    public ErrorResponse(String error) { this.error = error; }
}

class SuccessResponse {
    public String message;
    public SuccessResponse(String message) { this.message = message; }
}

class RegisterResponse {
    public String message;
    public UserInfo user;

    public RegisterResponse(String message, User user) {
        this.message = message;
        this.user = new UserInfo(user);
    }

    static class UserInfo {
        public String username;
        public String email;
        public String accountState;
        public String role;

        public UserInfo(User user) {
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.accountState = user.getAccountState();
            this.role = user.getRole();
        }
    }
}

class LoginResponse {
    public AuthToken token;
    public LoginResponse(AuthToken token) { this.token = token; }
}

class ChangeRoleResponse {
    public String message;
    public String username;
    public String newRole;

    public ChangeRoleResponse(String message, String username, String newRole) {
        this.message = message;
        this.username = username;
        this.newRole = newRole;
    }
}

class ChangeAccountStateResponse {
    public String message;
    public String username;
    public String newState;

    public ChangeAccountStateResponse(String message, String username, String newState) {
        this.message = message;
        this.username = username;
        this.newState = newState;
    }
}

class RemoveAccountResponse {
    public String message;
    public String identifier;

    public RemoveAccountResponse(String message, String identifier) {
        this.message = message;
        this.identifier = identifier;
    }
}

class ChangeAttributesResponse {
    public String message;
    public String identifier;

    public ChangeAttributesResponse(String message, String identifier) {
        this.message = message;
        this.identifier = identifier;
    }
}

// Request classes
class LoginData {
    public String identifier;
    public String password;
}

class LogoutData {
    public String token;
    public String authHeader;
}

class ChangeRoleData {
    public String username;
    public String newRole;
    public String authHeader;
}

class ChangeAccountStateData {
    public String username;
    public String newState;
    public String authHeader;
}

class RemoveAccountData {
    public String username;
    public String email;
    public String authHeader;
}

class ListUsersData {
    public String authHeader;
}

class ChangeAttributesData {
    public String identifier;
    public java.util.Map<String, String> attributes;
    public String authHeader;
}

class ChangePasswordData {
    public String currentPassword;
    public String newPassword;
    public String confirmPassword;
    public String authHeader;
} 