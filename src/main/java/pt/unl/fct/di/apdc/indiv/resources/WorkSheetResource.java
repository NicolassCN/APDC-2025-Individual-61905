package pt.unl.fct.di.apdc.indiv.resources;

import java.util.ArrayList;
import java.util.List;
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
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.util.AccessControl;
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.Worksheet;

@Path("/worksheet")
public class WorksheetResource {
    private static final Logger LOG = Logger.getLogger(WorksheetResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createWorksheet(@HeaderParam("Authorization") String authHeader,
                                  Worksheet worksheet) {
        // Get role from auth token
        String token = authHeader.substring("Bearer ".length()).trim();
        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(token);
        Entity tokenEntity = datastore.get(tokenKey);
        
        if (tokenEntity == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        User.Role role = User.Role.valueOf(tokenEntity.getString("role"));
        if (!AccessControl.canManageWorksheets(role)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        // Generate unique ID if not provided
        if (worksheet.getId() == null) {
            worksheet.setId(UUID.randomUUID().toString());
        }

        // Set creator
        worksheet.setCreatedBy(tokenEntity.getString("username"));

        // Create worksheet entity
        Entity worksheetEntity = Entity.newBuilder(datastore.newKeyFactory().setKind("Worksheet").newKey(worksheet.getId()))
                .set("title", worksheet.getTitle())
                .set("description", worksheet.getDescription())
                .set("partnerId", worksheet.getPartnerId())
                .set("state", worksheet.getState().toString())
                .set("createdBy", worksheet.getCreatedBy())
                .set("createdAt", worksheet.getCreatedAt())
                .set("lastModified", worksheet.getLastModified())
                .build();
        
        datastore.put(worksheetEntity);
        
        return Response.status(Response.Status.CREATED).entity(worksheet).build();
    }

    @POST
    @Path("/updatestate")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateWorksheetState(@HeaderParam("Authorization") String authHeader,
                                       @FormParam("worksheetId") String worksheetId,
                                       @FormParam("newState") String newState) {
        // Get role from auth token
        String token = authHeader.substring("Bearer ".length()).trim();
        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(token);
        Entity tokenEntity = datastore.get(tokenKey);
        
        if (tokenEntity == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // Get worksheet
        Key worksheetKey = datastore.newKeyFactory().setKind("Worksheet").newKey(worksheetId);
        Entity worksheetEntity = datastore.get(worksheetKey);
        
        if (worksheetEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        User.Role role = User.Role.valueOf(tokenEntity.getString("role"));
        String username = tokenEntity.getString("username");
        String partnerId = worksheetEntity.getString("partnerId");

        if (!AccessControl.canUpdateWorksheetState(role, partnerId, username)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        // Update state
        Entity updatedWorksheet = Entity.newBuilder(worksheetEntity)
                .set("state", newState)
                .set("lastModified", java.time.Instant.now().toString())
                .build();
        
        if (newState.equals(Worksheet.State.COMPLETED.toString())) {
            updatedWorksheet = Entity.newBuilder(updatedWorksheet)
                    .set("completedAt", java.time.Instant.now().toString())
                    .build();
        }
        
        datastore.update(updatedWorksheet);
        
        return Response.ok().build();
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listWorksheets(@HeaderParam("Authorization") String authHeader) {
        // Get role from auth token
        String token = authHeader.substring("Bearer ".length()).trim();
        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(token);
        Entity tokenEntity = datastore.get(tokenKey);
        
        if (tokenEntity == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        User.Role role = User.Role.valueOf(tokenEntity.getString("role"));
        String username = tokenEntity.getString("username");

        // Query worksheets
        Query<Entity> query;
        if (role == User.Role.PARTNER) {
            // Partners can only see their own worksheets
            query = Query.newEntityQueryBuilder()
                    .setKind("Worksheet")
                    .setFilter(StructuredQuery.PropertyFilter.eq("partnerId", username))
                    .build();
        } else {
            // Admin and BackOffice can see all worksheets
            query = Query.newEntityQueryBuilder()
                    .setKind("Worksheet")
                    .build();
        }
        
        QueryResults<Entity> results = datastore.run(query);
        List<Worksheet> worksheets = new ArrayList<>();
        
        while (results.hasNext()) {
            Entity entity = results.next();
            Worksheet worksheet = new Worksheet();
            worksheet.setId(entity.getKey().getName());
            worksheet.setTitle(entity.getString("title"));
            worksheet.setDescription(entity.getString("description"));
            worksheet.setPartnerId(entity.getString("partnerId"));
            worksheet.setState(Worksheet.State.valueOf(entity.getString("state")));
            worksheet.setCreatedBy(entity.getString("createdBy"));
            worksheet.setCreatedAt(entity.getString("createdAt"));
            worksheet.setLastModified(entity.getString("lastModified"));
            if (entity.contains("completedAt")) {
                worksheet.setCompletedAt(entity.getString("completedAt"));
            }
            worksheets.add(worksheet);
        }
        
        return Response.ok(worksheets).build();
    }
} 