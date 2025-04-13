package pt.unl.fct.di.apdc.indiv.resources;

import java.util.UUID;
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
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.UserValidator;
import pt.unl.fct.di.apdc.indiv.util.data.RegisterData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {
    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson gson = new Gson();

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUser(RegisterData data) {
        LOG.info("Attempting to register user: " + data.username);

        try {
            // Validate required fields
            if (!UserValidator.isValidEmail(data.email)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson("Invalid email format."))
                        .build();
            }

            if (!UserValidator.isValidUsername(data.username)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson("Invalid username."))
                        .build();
            }

            if (!UserValidator.isValidPassword(data.password)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson("Invalid password. Must contain uppercase, lowercase, numbers, and special characters."))
                        .build();
            }

            if (!data.password.equals(data.confirmPassword)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson("Passwords do not match."))
                        .build();
            }

            if (!UserValidator.isValidPhone(data.phone)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson("Invalid phone number."))
                        .build();
            }

            // Check if username already exists
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
            if (datastore.get(userKey) != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(gson.toJson("Username already exists."))
                        .build();
            }

            // Check if email already exists
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .setFilter(StructuredQuery.PropertyFilter.eq("email", data.email))
                    .build();
            QueryResults<Entity> results = datastore.run(query);
            if (results.hasNext()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(gson.toJson("Email already in use."))
                        .build();
            }

            // Create new user
            String userId = UUID.randomUUID().toString();
            String hashedPassword = UserValidator.hashPassword(data.password);
            
            User newUser = new User(
                data.username,
                data.email,
                data.fullName,
                data.phone,
                hashedPassword,
                data.profile,
                "ENDUSER", // Default role
                "DEACTIVATED" // Default state
            );

            // Set optional fields if provided
            if (data.citizenId != null) newUser.setCitizenCardNumber(data.citizenId);
            if (data.taxId != null) newUser.setTaxId(data.taxId);
            if (data.employer != null) newUser.setEmployer(data.employer);
            if (data.position != null) newUser.setPosition(data.position);
            if (data.address != null) newUser.setAddress(data.address);
            if (data.employerTaxId != null) newUser.setEmployerTaxId(data.employerTaxId);
            if (data.photo != null) newUser.setPhoto(data.photo);

            // Save to Datastore
            datastore.put(newUser.toEntity(datastore));

            // Return success response
            return Response.status(Response.Status.CREATED)
                    .entity(gson.toJson(new RegisterResponse("Account created successfully.", newUser)))
                    .build();

        } catch (Exception e) {
            LOG.severe("Error registering user: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(gson.toJson("Internal error processing registration."))
                    .build();
        }
    }

    private static class RegisterResponse {
        public final String message;
        public final UserInfo user;

        public RegisterResponse(String message, User user) {
            this.message = message;
            this.user = new UserInfo(user);
        }
    }

    private static class UserInfo {
        public final String username;
        public final String email;
        public final String accountState;
        public final String role;

        public UserInfo(User user) {
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.accountState = user.getAccountState().name();
            this.role = user.getRole().name();
        }
    }
}