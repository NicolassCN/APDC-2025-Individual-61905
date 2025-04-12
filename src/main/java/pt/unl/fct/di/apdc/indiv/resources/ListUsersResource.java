package pt.unl.fct.di.apdc.indiv.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.User;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ListUsersResource {
    private static final Logger LOG = Logger.getLogger(ListUsersResource.class.getName());
    private final Gson gson = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/all")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listUsers(@Context ContainerRequestContext context) {
        LOG.fine("List users attempt");

        try {
            AuthToken token = (AuthToken) context.getProperty("authToken");
            Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();
            QueryResults<Entity> results = datastore.run(query);
            List<UserResponse> users = new ArrayList<>();

            while (results.hasNext()) {
                Entity entity = results.next();
                User user = User.fromEntity(entity);

                if (token.getRole().equals("ENDUSER")) {
                    if (user.getRole().equals("ENDUSER") && user.getProfile().equals("public") && user.getAccountState().equals("ACTIVATED")) {
                        users.add(new UserResponse(user, true));
                    }
                } else if (token.getRole().equals("BACKOFFICE")) {
                    if (user.getRole().equals("ENDUSER")) {
                        users.add(new UserResponse(user, false));
                    }
                } else if (token.getRole().equals("ADMIN")) {
                    users.add(new UserResponse(user, false));
                }
            }

            return Response.ok(gson.toJson(users)).build();
        } catch (Exception e) {
            LOG.warning("List users failed: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(gson.toJson(new ErrorResponse("Internal server error"))).build();
        }
    }

    private static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class UserResponse {
        public String username;
        public String email;
        public String fullName;
        public String phone;
        public String accountState;
        public String profile;
        public String role;
        public String address;
        public String taxId;
        public String employer;
        public String position;
        public String employerTaxId;
        public String photo;

        public UserResponse(User user, boolean limited) {
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.fullName = user.getFullName();
            if (limited) {
                this.phone = null;
                this.accountState = null;
                this.profile = null;
                this.role = null;
                this.address = null;
                this.taxId = null;
                this.employer = null;
                this.position = null;
                this.employerTaxId = null;
                this.photo = null;
            } else {
                this.phone = user.getPhone();
                this.accountState = user.getAccountState();
                this.profile = user.getProfile();
                this.role = user.getRole();
                this.address = user.getAddress();
                this.taxId = user.getTaxId();
                this.employer = user.getEmployer();
                this.position = user.getPosition();
                this.employerTaxId = user.getEmployerTaxId();
                this.photo = user.getPhoto();
            }
        }
    }
}