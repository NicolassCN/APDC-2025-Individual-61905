package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

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
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.indiv.util.ApiResponse;
import pt.unl.fct.di.apdc.indiv.util.RegisterData;
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
    public Response register(RegisterData registerData) {
        LOG.info("Register attempt for user: " + registerData.getUser().getUsername());
        
        try {
            // Verificar se as senhas coincidem
            if (!registerData.getUser().getPassword().equals(registerData.getConfirmPassword())) {
                LOG.warning("Registration attempt with password mismatch for user: " + registerData.getUser().getUsername());
                return Response.status(Status.BAD_REQUEST)
                        .entity(g.toJson(new ApiResponse("As senhas não coincidem", false)))
                        .build();
            }
            
            User user = registerData.getUser();
            
            // Validar dados do usuário
            UserValidator.ValidationResult validationResult = UserValidator.validateUser(user);
            if (!validationResult.isValid()) {
                LOG.warning("Registration attempt with invalid data: " + validationResult.getMessage());
                return Response.status(Status.BAD_REQUEST)
                        .entity(g.toJson(new ApiResponse(validationResult.getMessage(), false)))
                        .build();
            }

            // Verificar se username já existe
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(user.getUsername());
            Entity existingUser = datastore.get(userKey);
            if (existingUser != null) {
                LOG.warning("Registration attempt with existing username: " + user.getUsername());
                return Response.status(Status.CONFLICT)
                        .entity(g.toJson(new ApiResponse("Username já existe", false)))
                        .build();
            }

            // Verificar se email já existe
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .setFilter(StructuredQuery.PropertyFilter.eq("email", user.getEmail()))
                    .build();
            QueryResults<Entity> results = datastore.run(query);
            if (results.hasNext()) {
                LOG.warning("Registration attempt with existing email: " + user.getEmail());
                return Response.status(Status.CONFLICT)
                        .entity(g.toJson(new ApiResponse("Email já está em uso", false)))
                        .build();
            }

            // Normalizar o role para maiúsculas e garantir que seja ENDUSER
            user.setRole("ENDUSER");
            
            // Garantir que a conta está DESATIVADA inicialmente
            user.setAccountState("DESATIVADA");
            LOG.info("Setting account state to DESATIVADA for user: " + user.getUsername());

            // Hash da senha antes de salvar
            String hashedPassword = DigestUtils.sha512Hex(user.getPassword());
            user.setPassword(hashedPassword);
            LOG.info("Password hashed for user: " + user.getUsername());

            // Salvar usuário no datastore
            Entity userEntity = user.toEntity(userKey);
            datastore.put(userEntity);

            LOG.info("User registered successfully: " + user.getUsername() + " with role: " + user.getRole() + " and state: " + user.getAccountState());
            return Response.ok(g.toJson(new ApiResponse("Usuário registrado com sucesso", true))).build();
        } catch (Exception e) {
            LOG.severe("Error registering user: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(g.toJson(new ApiResponse("Erro ao registrar usuário: " + e.getMessage(), false)))
                    .build();
        }
    }

    @GET
    @Path("/check/{username}")
    public Response checkUsernameAvailable(@PathParam("username") String username) {
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
        Entity user = datastore.get(userKey);
        return Response.ok(g.toJson(new ApiResponse(user == null ? "Username disponível" : "Username já existe", user == null))).build();
    }

    // Método para criar o usuário root automaticamente
    public static void createRootUser(Datastore datastore) {
        LOG.info("Checking if root user exists...");
        Key rootKey = datastore.newKeyFactory().setKind("User").newKey("root");
        Entity rootUser = datastore.get(rootKey);

        if (rootUser == null) {
            LOG.info("Root user does not exist, creating...");
            User root = new User();
            root.setUsername("root");
            // Hash da senha do root
            String hashedPassword = DigestUtils.sha512Hex("root");
            root.setPassword(hashedPassword);
            root.setEmail("root@fct.unl.pt");
            root.setFullName("Root Administrator");
            root.setPhone("+3512895629");
            root.setProfile("private");
            root.setRole("ADMIN");
            root.setAccountState("ATIVADA");

            Entity rootEntity = root.toEntity(rootKey);
            datastore.put(rootEntity);
            LOG.info("Root user created successfully with password hash: " + hashedPassword);
        } else {
            LOG.info("Root user already exists, updating...");
            // Se o root já existe, garantir que a senha está hasheada e a conta está ativada
            String hashedPassword = DigestUtils.sha512Hex("root");
            Entity updatedRoot = Entity.newBuilder(rootKey, rootUser)
                .set("password", hashedPassword) // Atualizar senha para garantir
                .set("accountState", "ATIVADA") // Garantir que a conta está ativada
                .build();
            datastore.update(updatedRoot);
            LOG.info("Root user updated with password hash: " + hashedPassword);
        }
    }
} 