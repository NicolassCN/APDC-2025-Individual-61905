package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.exceptions.AppException;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.WorkSheet;
import pt.unl.fct.di.apdc.indiv.util.data.WorkSheetData;

@Path("/worksheet")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class WorkSheetResource {
    private static final Logger LOG = Logger.getLogger(WorkSheetResource.class.getName());
    private final Gson gson = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createWorkSheet(@Context ContainerRequestContext context, WorkSheetData data) {
        LOG.fine("Create/update worksheet attempt for reference: " + data.getReference());

        try {
            AuthToken token = (AuthToken) context.getProperty("authToken");
            if (!token.getRole().equals("BACKOFFICE") && !token.getRole().equals("PARTNER")) {
                throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "Insufficient permissions");
            }

            Key wsKey = datastore.newKeyFactory().setKind("WorkSheet").newKey(data.getReference());
            Entity existingWs = datastore.get(wsKey);
            WorkSheet ws;

            if (existingWs == null) {
                if (!token.getRole().equals("BACKOFFICE")) {
                    throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "Insufficient permissions");
                }
                validateRequiredFields(data);
                ws = new WorkSheet(data.getReference(), data.getDescription(), data.getTargetType(), data.getAwardStatus());
            } else {
                ws = WorkSheet.fromEntity(existingWs);
            }

            if (token.getRole().equals("BACKOFFICE")) {
                updateWorkSheetFields(ws, data);
            } else if (token.getRole().equals("PARTNER") && ws.getAssignedEntity().equals(token.getUsername())) {
                if (data.getWorkStatus() != null && (data.getWorkStatus().equals("IN_PROGRESS") || data.getWorkStatus().equals("COMPLETED"))) {
                    ws.setWorkStatus(data.getWorkStatus());
                    ws.setNotes(data.getNotes() != null ? data.getNotes() : ws.getNotes());
                } else {
                    throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "Insufficient permissions");
                }
            } else {
                throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "Insufficient permissions");
            }

            datastore.put(ws.toEntity(datastore));
            return Response.ok(gson.toJson(new WorkSheetResponse("Worksheet created/updated successfully"))).build();
        } catch (AppException e) {
            LOG.warning("Worksheet operation failed: " + e.getMessage());
            return Response.status(e.getStatus()).entity(gson.toJson(new ErrorResponse(e.getMessage()))).build();
        }
    }

    private void validateRequiredFields(WorkSheetData data) throws AppException {
        if (data.getReference() == null || data.getDescription() == null || data.getTargetType() == null || data.getAwardStatus() == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Missing required fields");
        }
        if (!data.getTargetType().equals("PUBLIC_PROPERTY") && !data.getTargetType().equals("PRIVATE_PROPERTY")) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Invalid target type");
        }
        if (!data.getAwardStatus().equals("AWARDED") && !data.getAwardStatus().equals("NOT_AWARDED")) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Invalid award status");
        }
    }

    private void updateWorkSheetFields(WorkSheet ws, WorkSheetData data) {
        ws.setDescription(data.getDescription() != null ? data.getDescription() : ws.getDescription());
        ws.setTargetType(data.getTargetType() != null ? data.getTargetType() : ws.getTargetType());
        ws.setAwardStatus(data.getAwardStatus() != null ? data.getAwardStatus() : ws.getAwardStatus());
        ws.setAwardDate(data.getAwardDate() != null ? data.getAwardDate() : ws.getAwardDate());
        ws.setStartDate(data.getStartDate() != null ? data.getStartDate() : ws.getStartDate());
        ws.setEndDate(data.getEndDate() != null ? data.getEndDate() : ws.getEndDate());
        ws.setAssignedEntity(data.getAssignedEntity() != null ? data.getAssignedEntity() : ws.getAssignedEntity());
        ws.setAwardedEntity(data.getAwardedEntity() != null ? data.getAwardedEntity() : ws.getAwardedEntity());
        ws.setCompanyTaxId(data.getCompanyTaxId() != null ? data.getCompanyTaxId() : ws.getCompanyTaxId());
        ws.setWorkStatus(data.getWorkStatus() != null ? data.getWorkStatus() : ws.getWorkStatus());
        ws.setNotes(data.getNotes() != null ? data.getNotes() : ws.getNotes());
    }

    private static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class WorkSheetResponse {
        public String message;
        public WorkSheetResponse(String message) {
            this.message = message;
        }
    }
}