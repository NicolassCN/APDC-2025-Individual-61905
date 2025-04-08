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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.UserValidator;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {
    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(User user) {
        LOG.fine("Register attempt for user: " + user.getUsername());

        UserValidator.ValidationResult validationResult = UserValidator.validateUser(user);
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
                    .entity(g.toJson("Username já existe"))
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
                    .entity(g.toJson("Email já está em uso"))
                    .build();
        }

        // Salvar usuário no datastore
        Entity userEntity = user.toEntity(userKey);
        datastore.put(userEntity);

        LOG.info("User registered successfully: " + user.getUsername());
        return Response.ok(g.toJson("Usuário registrado com sucesso")).build();
    }

    @GET
    @Path("/check/{username}")
    public Response checkUsernameAvailable(@PathParam("username") String username) {
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
        Entity user = datastore.get(userKey);
        return Response.ok(g.toJson(user == null)).build();
    }

    
    public static void createRootUser(Datastore datastore) {
        Key rootKey = datastore.newKeyFactory().setKind("User").newKey("root");
        Entity rootUser = datastore.get(rootKey);
        
        if (rootUser == null) {
            User root = new User();
            root.setUsername("root");
            root.setPassword("2025adcAVALind!!!"); 
            root.setEmail("root@fct.unl.pt");
            root.setFullName("Root Administrator");
            root.setPhone("+3512895629");
            root.setProfile("private");
            root.setRole("ADMIN");
            root.setAccountState("ATIVADA");
            
            Entity rootEntity = root.toEntity(rootKey);
            datastore.put(rootEntity);
            LOG.info("Root user created successfully");
        }
    }
} 