package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.data.WorkSheetData;

@Path("/work-sheet")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class WorkSheetResource {
    private static final Logger LOG = Logger.getLogger(WorkSheetResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson gson = new Gson();

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createWorkSheet(@HeaderParam("Authorization") String authHeader, WorkSheetData data) {
        LOG.info("Attempt to create/modify work sheet: " + data.referenciaObra);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOG.warning("Missing or invalid authorization header");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing or invalid authorization header").build();
        }

        String token = authHeader.substring(7);

        Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(token);
        Entity tokenEntity = datastore.get(tokenKey);

        if (tokenEntity == null) {
            LOG.warning("Invalid token");
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token").build();
        }

        AuthToken authToken = AuthToken.fromEntity(tokenEntity);
        String role = authToken.getRole();

        if (!role.equals("BACKOFFICE") && !role.equals("PARTNER")) {
            LOG.warning("Unauthorized access attempt by user with role: " + role);
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Only BACKOFFICE and PARTNER users can manage work sheets").build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            Key worksheetKey = datastore.newKeyFactory().setKind("WorkSheet").newKey(data.referenciaObra);
            Entity existingWorksheet = txn.get(worksheetKey);

            if (existingWorksheet != null) {
                // Verificar se a transição de estado é válida
                String currentState = existingWorksheet.getString("estadoObra");
                if (!isValidStateTransition(currentState, data.estadoObra)) {
                    LOG.warning("Invalid state transition from " + currentState + " to " + data.estadoObra);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid state transition").build();
                }
            }

            Entity worksheet = Entity.newBuilder(worksheetKey)
                    .set("referenciaObra", data.referenciaObra)
                    .set("descricao", data.descricao)
                    .set("tipoAlvo", data.tipoAlvo)
                    .set("estadoAdjudicacao", data.estadoAdjudicacao)
                    .set("dataAdjudicacao", data.dataAdjudicacao != null ? data.dataAdjudicacao : "")
                    .set("dataInicioPrevista", data.dataInicioPrevista != null ? data.dataInicioPrevista : "")
                    .set("dataConclusaoPrevista", data.dataConclusaoPrevista != null ? data.dataConclusaoPrevista : "")
                    .set("contaEntidade", data.contaEntidade != null ? data.contaEntidade : "")
                    .set("entidadeAdjudicacao", data.entidadeAdjudicacao != null ? data.entidadeAdjudicacao : "")
                    .set("nifEmpresa", data.nifEmpresa != null ? data.nifEmpresa : "")
                    .set("estadoObra", data.estadoObra != null ? data.estadoObra : "PENDENTE")
                    .set("observacoes", data.observacoes != null ? data.observacoes : "")
                    .build();

            txn.put(worksheet);
            txn.commit();

            LOG.info("Work sheet created/modified successfully: " + data.referenciaObra);
            return Response.status(Response.Status.OK)
                    .entity("Work sheet created/modified successfully").build();

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Failed to create/modify work sheet: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to create/modify work sheet").build();
        }
    }

    private boolean isValidStateTransition(String currentState, String newState) {
        if (currentState == null || newState == null) {
            return true; // Permitir definição inicial do estado
        }

        switch (currentState) {
            case "PENDENTE":
                return newState.equals("EM_EXECUCAO") || newState.equals("CANCELADA");
            case "EM_EXECUCAO":
                return newState.equals("CONCLUIDA") || newState.equals("CANCELADA");
            case "CONCLUIDA":
            case "CANCELADA":
                return false; // Estados finais
            default:
                return false;
        }
    }
}