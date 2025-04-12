package pt.unl.fct.di.apdc.indiv.resources;

import java.util.UUID;
import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.exceptions.AppException;
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.UserValidator;
import pt.unl.fct.di.apdc.indiv.util.data.RegisterData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {
    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
    private final Gson gson = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(RegisterData data) {
        LOG.fine("Register attempt for username: " + data.getUsername());

        try {
            validateRegistration(data);

            Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.getUsername());
            if (datastore.get(userKey) != null) {
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Username already exists");
            }

            String userId = UUID.randomUUID().toString();
            User user = new User(userId, data.getEmail(), data.getUsername(), data.getFullName(),
                    data.getPhone(), data.getPassword(), data.getProfile(), "ENDUSER", "DEACTIVATED");
            datastore.put(user.toEntity(datastore));

            return Response.status(Response.Status.CREATED)
                    .entity(gson.toJson(new RegisterResponse("Account created successfully", user))).build();
        } catch (AppException e) {
            LOG.warning("Registration failed: " + e.getMessage());
            return Response.status(e.getStatus()).entity(gson.toJson(new ErrorResponse(e.getMessage()))).build();
        }
    }

    private void validateRegistration(RegisterData data) throws AppException {
        if (!UserValidator.isValidEmail(data.getEmail())) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Invalid email");
        }
        if (!data.getPassword().equals(data.getConfirmPassword())) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Passwords do not match");
        }
        if (!UserValidator.isValidPassword(data.getPassword())) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Password does not meet requirements");
        }
        if (!UserValidator.isValidPhone(data.getPhone())) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Invalid phone number");
        }
        if (!UserValidator.isValidProfile(data.getProfile())) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Invalid profile");
        }
    }

    private static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class RegisterResponse {
        public String message;
        public UserResponse user;
        public RegisterResponse(String message, User user) {
            this.message = message;
            this.user = new UserResponse(user);
        }
    }

    private static class UserResponse {
        public String username;
        public String email;
        public String accountState;
        public String role;
        public UserResponse(User user) {
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.accountState = user.getAccountState();
            this.role = user.getRole();
        }
    }
}