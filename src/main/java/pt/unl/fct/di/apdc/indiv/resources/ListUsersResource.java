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
import pt.unl.fct.di.apdc.indiv.util.User;

@Path("/listusers")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ListUsersResource {
    private static final Logger LOG = Logger.getLogger(ListUsersResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listUsers(User requestingUser) {
        LOG.info("List users request from: " + requestingUser.getUsername());
        
        try {
            // Verificar se o usuário existe e está autenticado
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(requestingUser.getUsername());
            Entity userEntity = datastore.get(userKey);
            
            if (userEntity == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(g.toJson("Usuário não encontrado"))
                        .build();
            }
            
            String userRole = userEntity.getString("role");
            List<User> users = new ArrayList<>();
            
            // Construir query base
            Query<Entity> query;
            
            // Aplicar filtros baseados no role do usuário
            if ("ENDUSER".equals(userRole)) {
                // ENDUSER só pode ver outros ENDUSER com perfil público e conta ativa
                query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .setFilter(StructuredQuery.CompositeFilter.and(
                        StructuredQuery.PropertyFilter.eq("role", "ENDUSER"),
                        StructuredQuery.PropertyFilter.eq("profile", "public"),
                        StructuredQuery.PropertyFilter.eq("accountState", "ATIVADA")
                    ))
                    .build();
            } else if ("BACKOFFICE".equals(userRole)) {
                // BACKOFFICE pode ver todos os ENDUSER
                query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .setFilter(StructuredQuery.PropertyFilter.eq("role", "ENDUSER"))
                    .build();
            } else {
                // ADMIN pode ver todos os usuários (sem filtro)
                query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .build();
            }
            
            QueryResults<Entity> results = datastore.run(query);
            
            while (results.hasNext()) {
                Entity entity = results.next();
                User user = User.fromEntity(entity);
                
                // Filtrar campos baseado no role do usuário
                if ("ENDUSER".equals(userRole)) {
                    // ENDUSER só vê username, email e nome
                    User filteredUser = new User();
                    filteredUser.setUsername(user.getUsername());
                    filteredUser.setEmail(user.getEmail());
                    filteredUser.setFullName(user.getFullName());
                    users.add(filteredUser);
                } else if ("BACKOFFICE".equals(userRole)) {
                    // BACKOFFICE vê todos os campos dos ENDUSER
                    users.add(user);
                } else if ("ADMIN".equals(userRole)) {
                    // ADMIN vê todos os campos de todos os usuários
                    users.add(user);
                }
            }
            
            return Response.ok(g.toJson(users)).build();
        } catch (Exception e) {
            LOG.severe("Error listing users: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(g.toJson("Erro ao listar usuários: " + e.getMessage()))
                    .build();
        }
    }
} 