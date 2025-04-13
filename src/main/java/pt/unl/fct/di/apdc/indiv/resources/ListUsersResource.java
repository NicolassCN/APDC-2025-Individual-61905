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

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ListUsersResource {
    private static final Logger LOG = Logger.getLogger(ListUsersResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson gson = new Gson();

    @POST
    @Path("/all")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listUsers(@HeaderParam("Authorization") String authHeader) {
        LOG.info("Tentativa de listar usuários");

        try {
            // Validar token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("Token de autenticação é obrigatório."))
                        .build();
            }

            String tokenId = authHeader.substring("Bearer ".length());
            Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenId);
            Entity tokenEntity = datastore.get(tokenKey);

            if (tokenEntity == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("Token inválido."))
                        .build();
            }

            AuthToken token = AuthToken.fromEntity(tokenEntity);
            if (token.isExpired()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("Token expirado."))
                        .build();
            }

            // Buscar usuário que está fazendo a requisição
            Key requesterKey = datastore.newKeyFactory().setKind("User").newKey(token.getUsername());
            Entity requesterEntity = datastore.get(requesterKey);
            if (requesterEntity == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(gson.toJson("Usuário não encontrado."))
                        .build();
            }

            User requester = User.fromEntity(requesterEntity);

            // Buscar todos os usuários
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .build();
            QueryResults<Entity> results = datastore.run(query);

            List<UserInfo> userList = new ArrayList<>();
            while (results.hasNext()) {
                Entity userEntity = results.next();
                User user = User.fromEntity(userEntity);

                // Verificar se o usuário tem permissão para ver este usuário
                if (requester.canViewUserDetails(user)) {
                    userList.add(new UserInfo(user, requester.getRole()));
                }
            }

            return Response.ok(gson.toJson(userList)).build();

        } catch (Exception e) {
            LOG.severe("Erro ao listar usuários: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(gson.toJson("Erro interno ao processar a listagem de usuários."))
                    .build();
        }
    }

    private static class UserInfo {
        public final String username;
        public final String email;
        public final String name;
        public final String phone;
        public final String state;
        public final String profile;
        public final String role;
        public final String address;
        public final String taxId;
        public final String employer;
        public final String position;
        public final String employerTaxId;
        public final String photo;

        public UserInfo(User user, User.Role requesterRole) {
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.name = user.getFullName();
            this.phone = user.getPhone();
            this.state = user.getAccountState().name();
            this.profile = user.getProfile().name();
            this.role = user.getRole().name();

            // If requester is ADMIN or BACKOFFICE, show all attributes
            if (requesterRole == User.Role.ADMIN || 
                (requesterRole == User.Role.BACKOFFICE && user.getRole() == User.Role.ENDUSER)) {
                this.address = user.getAddress();
                this.taxId = user.getTaxId();
                this.employer = user.getEmployer();
                this.position = user.getPosition();
                this.employerTaxId = user.getEmployerTaxId();
                this.photo = user.getPhoto();
            } else {
                // For ENDUSER, show only basic information
                this.address = "NOT_DEFINED";
                this.taxId = "NOT_DEFINED";
                this.employer = "NOT_DEFINED";
                this.position = "NOT_DEFINED";
                this.employerTaxId = "NOT_DEFINED";
                this.photo = "NOT_DEFINED";
            }
        }
    }
}