package pt.unl.fct.di.apdc.indiv.resources;

import java.util.Date;
import java.util.logging.Level; // Para converter Long para Date para Timestamp
import java.util.logging.Logger;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.UpdateWorkStateData;
import pt.unl.fct.di.apdc.indiv.util.WorkSheetData;


@Path("/worksheet")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class WorkSheetResource {

    private static final Logger LOG = Logger.getLogger(WorkSheetResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory workSheetKeyFactory = datastore.newKeyFactory().setKind("WorkSheet");
    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthToken");
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    // Constantes para propriedades da entidade WorkSheet (mantidas como antes)
    private static final String WS_REF = "ws_ref";
    private static final String WS_DESC = "ws_desc";
    private static final String WS_TARGET_TYPE = "ws_target_type";
    private static final String WS_ADJUDICATION_STATE = "ws_adjudication_state";
    private static final String WS_ADJUDICATION_DATE = "ws_adjudication_date";
    private static final String WS_START_DATE = "ws_start_date";
    private static final String WS_END_DATE = "ws_end_date";
    private static final String WS_PARTNER_ACCOUNT = "ws_partner_account";
    private static final String WS_ENTITY_NAME = "ws_entity_name";
    private static final String WS_ENTITY_NIF = "ws_entity_nif";
    private static final String WS_WORK_STATE = "ws_work_state";
    private static final String WS_OBSERVATIONS = "ws_observations";
    private static final String WS_CREATION_TIME = "ws_creation_time";
    private static final String WS_LAST_UPDATE_TIME = "ws_last_update_time";
    // Constantes de User
    private static final String USER_ROLE_PROPERTY = "user_role";

    private final Gson g = new Gson();

    public WorkSheetResource() {}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveWorkSheet(@HeaderParam("Authorization") String authorizationHeader, WorkSheetData data) {
        AuthToken token = validateToken(authorizationHeader);
        if (token == null) { return Response.status(Status.FORBIDDEN).entity("Invalid/Expired token.").build(); }
        LOG.info("saveWorkSheet attempt by: " + token.getUsername() + "...");
        if (!token.getRole().equals("BACKOFFICE") && !token.getRole().equals("ADMIN")) {
            return Response.status(Status.FORBIDDEN).build();
        }
        if (data == null || !data.isValid()) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        String estadoAdjUpper = data.getEstadoAdjudicacao().toUpperCase();
        String tipoAlvoUpper = data.getTipoAlvo().toUpperCase();
        Timestamp now = Timestamp.now();

        Transaction txn = datastore.newTransaction();
        try {
            Key workSheetKey = workSheetKeyFactory.newKey(data.getReferencia());
            Entity existingWorkSheet = txn.get(workSheetKey);
            Entity.Builder builder;
            boolean isCreating = (existingWorkSheet == null);

            if (isCreating) {
                builder = Entity.newBuilder(workSheetKey).set(WS_CREATION_TIME, now);
            } else {
                builder = Entity.newBuilder(existingWorkSheet);
            }

            builder.set(WS_REF, data.getReferencia())
                   .set(WS_DESC, data.getDescricao())
                   .set(WS_TARGET_TYPE, tipoAlvoUpper)
                   .set(WS_ADJUDICATION_STATE, estadoAdjUpper);

            if (data.getObservacoes() != null) {
                builder.set(WS_OBSERVATIONS, data.getObservacoes());
            } else if (!isCreating && existingWorkSheet.contains(WS_OBSERVATIONS)) {
                builder.remove(WS_OBSERVATIONS);
            }

            if (estadoAdjUpper.equals("ADJUDICADO")) {
                builder.set(WS_ADJUDICATION_DATE, Timestamp.of(new Date(data.getDataAdjudicacao())))
                       .set(WS_START_DATE, Timestamp.of(new Date(data.getDataInicioPrevista())))
                       .set(WS_END_DATE, Timestamp.of(new Date(data.getDataFimPrevista())))
                       .set(WS_PARTNER_ACCOUNT, data.getContaEntidade())
                       .set(WS_ENTITY_NAME, data.getNomeEmpresa())
                       .set(WS_ENTITY_NIF, data.getNifEmpresa());

                boolean needsInitialWorkState = isCreating || !existingWorkSheet.contains(WS_WORK_STATE);
                if (needsInitialWorkState) {
                    builder.set(WS_WORK_STATE, "NÃO INICIADO");
                }
            } else {
                builder.remove(WS_ADJUDICATION_DATE)
                       .remove(WS_START_DATE)
                       .remove(WS_END_DATE)
                       .remove(WS_PARTNER_ACCOUNT)
                       .remove(WS_ENTITY_NAME)
                       .remove(WS_ENTITY_NIF)
                       .remove(WS_WORK_STATE);
            }

            builder.set(WS_LAST_UPDATE_TIME, now);
            txn.put(builder.build());
            txn.commit();

            String action = isCreating ? "created" : "updated";
            return Response.ok().entity("Worksheet " + action + " successfully.").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "saveWorkSheet Error", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/updatestate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWorkState(@HeaderParam("Authorization") String authorizationHeader, UpdateWorkStateData data) {
        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            return Response.status(Status.FORBIDDEN).entity("Invalid/Expired token.").build();
        }

        LOG.info("updateWorkState attempt by: " + token.getUsername() + " (Role: " + token.getRole() + ") for ref: " + (data != null ? data.getReferencia() : "null"));

        if (!token.getRole().equals("PARTNER")) {
            LOG.warning("updateWorkState failed: Permission denied for user " + token.getUsername() + " (Role: " + token.getRole() + ")");
            return Response.status(Status.FORBIDDEN).entity("Only PARTNER users can update work state.").build();
        }

        if (data == null) {
            LOG.warning("updateWorkState failed: No data provided. User: " + token.getUsername());
            return Response.status(Status.BAD_REQUEST).entity("No update data provided.").build();
        }

        // Set the updater to the authenticated user
        data.setUpdatedBy(token.getUsername());
        
        if (!data.isValid()) {
            LOG.warning("updateWorkState failed: Invalid input data. User: " + token.getUsername());
            return Response.status(Status.BAD_REQUEST).entity("Invalid input data. Provide referencia and newWorkState (NÃO INICIADO, EM CURSO, CONCLUÍDO).").build();
        }
        
        String newWorkStateUpper = data.getNewWorkState().toUpperCase();

        Transaction txn = datastore.newTransaction();
        try {
            Key workSheetKey = workSheetKeyFactory.newKey(data.getReferencia());
            Entity workSheet = txn.get(workSheetKey);

            if (workSheet == null) {
                txn.rollback();
                LOG.warning("updateWorkState failed: Worksheet " + data.getReferencia() + " not found. Requested by: " + token.getUsername());
                return Response.status(Status.NOT_FOUND).entity("Worksheet not found.").build();
            }

            String adjudicationState = workSheet.contains(WS_ADJUDICATION_STATE) ? workSheet.getString(WS_ADJUDICATION_STATE) : "NÃO ADJUDICADO";
            if (!adjudicationState.equals("ADJUDICADO")) {
                txn.rollback();
                LOG.warning("updateWorkState failed: Worksheet " + data.getReferencia() + " is not adjudicated (State: " + adjudicationState + "). Requested by: " + token.getUsername());
                return Response.status(Status.BAD_REQUEST).entity("Worksheet is not adjudicated.").build();
            }

            String assignedPartner = workSheet.contains(WS_PARTNER_ACCOUNT) ? workSheet.getString(WS_PARTNER_ACCOUNT) : null;
            if (assignedPartner == null || !assignedPartner.equals(token.getUsername())) {
                txn.rollback();
                LOG.warning("updateWorkState failed: User " + token.getUsername() + " is not the assigned partner for worksheet " + data.getReferencia() + " (Assigned: " + assignedPartner + ")");
                return Response.status(Status.FORBIDDEN).entity("User is not the assigned partner for this worksheet.").build();
            }

            Entity.Builder builder = Entity.newBuilder(workSheet)
                    .set(WS_WORK_STATE, newWorkStateUpper)
                    .set(WS_LAST_UPDATE_TIME, Timestamp.now());

            // Add reason if provided
            if (data.getReason() != null && !data.getReason().isBlank()) {
                builder.set("ws_state_change_reason", data.getReason());
            }

            // Add comments if provided
            if (data.getComments() != null && !data.getComments().isBlank()) {
                builder.set("ws_state_change_comments", data.getComments());
            }

            txn.put(builder.build());
            txn.commit();

            LOG.info("User " + token.getUsername() + " successfully updated work state for worksheet " + data.getReferencia() + " to " + newWorkStateUpper);
            return Response.ok().entity("Worksheet state updated successfully.").build();

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "updateWorkState Datastore error for ref " + data.getReferencia() + " by " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error updating work state (Datastore).").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "updateWorkState Unexpected error for ref " + data.getReferencia() + " by " + token.getUsername(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error updating work state (Unexpected).").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    private AuthToken validateToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String tokenId = authorizationHeader.substring(7).trim();
        if (tokenId.isEmpty()) {
            return null;
        }
        LOG.fine("Validating TokenID (Worksheet): " + tokenId);
        try {
            Key tokenKey = tokenKeyFactory.newKey(tokenId);
            Entity tokenEntity = datastore.get(tokenKey);
            if (tokenEntity == null) {
                LOG.warning("Token not found (Worksheet): " + tokenId);
                return null;
            }
            AuthToken token = new AuthToken();
            token.setTokenID(tokenId);
            token.setUsername(tokenEntity.contains("username") ? tokenEntity.getString("username") : null);
            token.setRole(tokenEntity.contains("role") ? tokenEntity.getString("role") : null);
            
            // Set creation time
            if (tokenEntity.contains("creationData")) {
                token.setCreationTime(tokenEntity.getLong("creationData"));
            }
            
            // Set expiration time
            if (tokenEntity.contains("expirationData")) {
                token.setExpirationTime(tokenEntity.getLong("expirationData"));
            }
            
            // Set last access IP
            if (tokenEntity.contains("lastAccessIP")) {
                token.updateLastAccessIP(tokenEntity.getString("lastAccessIP"));
            }
            
            // Check if token is revoked
            if (tokenEntity.contains("isRevoked") && tokenEntity.getBoolean("isRevoked")) {
                LOG.warning("Token is revoked (Worksheet): " + tokenId);
                return null;
            }

            if (token.getUsername() == null || token.getRole() == null || !token.isValid()) {
                LOG.warning("Token validation failed: Invalid token data (Worksheet): " + tokenId);
                return null;
            }

            LOG.info("Token validated successfully (Worksheet) for user: " + token.getUsername());
            return token;

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Token validation error (Worksheet): " + tokenId, e);
            return null;
        }
    }

}