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
import pt.unl.fct.di.apdc.indiv.filters.AuthenticationFilter;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.User;
import pt.unl.fct.di.apdc.indiv.util.WorkSheet;

@Path("/api/worksheet")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class WorkSheetResource {
    private static final Logger LOG = Logger.getLogger(WorkSheetResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createWorkSheet(WorkSheet workSheet) {
        AuthToken token = AuthenticationFilter.getTokenFromUsername(workSheet.getUsername());
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson("Invalid or expired session"))
                    .build();
        }

        User user = User.fromEntity(datastore.get(
                datastore.newKeyFactory().setKind("User").newKey(workSheet.getUsername())));

        if (!user.isActive()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson("Account is not active"))
                    .build();
        }

        Key workSheetKey = datastore.allocateId(
                datastore.newKeyFactory().setKind("WorkSheet").newKey());
        workSheet.setId(workSheetKey.getId());

        datastore.put(workSheet.toEntity(workSheetKey));
        return Response.ok(g.toJson(workSheet)).build();
    }

    @POST
    @Path("/list")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listWorkSheets(String username) {
        AuthToken token = AuthenticationFilter.getTokenFromUsername(username);
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson("Invalid or expired session"))
                    .build();
        }

        User user = User.fromEntity(datastore.get(
                datastore.newKeyFactory().setKind("User").newKey(username)));

        if (!user.isActive()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson("Account is not active"))
                    .build();
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("WorkSheet")
                .setFilter(StructuredQuery.PropertyFilter.eq("username", username))
                .build();

        QueryResults<Entity> results = datastore.run(query);
        List<WorkSheet> workSheets = new ArrayList<>();
        
        while (results.hasNext()) {
            workSheets.add(WorkSheet.fromEntity(results.next()));
        }

        return Response.ok(g.toJson(workSheets)).build();
    }

    @POST
    @Path("/get")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getWorkSheet(GetWorkSheetRequest request) {
        AuthToken token = AuthenticationFilter.getTokenFromUsername(request.username);
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson("Invalid or expired session"))
                    .build();
        }

        User user = User.fromEntity(datastore.get(
                datastore.newKeyFactory().setKind("User").newKey(request.username)));

        if (!user.isActive()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson("Account is not active"))
                    .build();
        }

        Key workSheetKey = datastore.newKeyFactory().setKind("WorkSheet").newKey(request.workSheetId);
        Entity workSheetEntity = datastore.get(workSheetKey);

        if (workSheetEntity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(g.toJson("Work sheet not found"))
                    .build();
        }

        WorkSheet workSheet = WorkSheet.fromEntity(workSheetEntity);
        if (!workSheet.getUsername().equals(request.username)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson("Access denied to this work sheet"))
                    .build();
        }

        return Response.ok(g.toJson(workSheet)).build();
    }

    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWorkSheet(WorkSheet workSheet) {
        AuthToken token = AuthenticationFilter.getTokenFromUsername(workSheet.getUsername());
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson("Invalid or expired session"))
                    .build();
        }

        User user = User.fromEntity(datastore.get(
                datastore.newKeyFactory().setKind("User").newKey(workSheet.getUsername())));

        if (!user.isActive()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson("Account is not active"))
                    .build();
        }

        Key workSheetKey = datastore.newKeyFactory().setKind("WorkSheet").newKey(workSheet.getId());
        Entity existingWorkSheet = datastore.get(workSheetKey);

        if (existingWorkSheet == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(g.toJson("Work sheet not found"))
                    .build();
        }

        if (!WorkSheet.fromEntity(existingWorkSheet).getUsername().equals(workSheet.getUsername())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson("Access denied to this work sheet"))
                    .build();
        }

        datastore.put(workSheet.toEntity(workSheetKey));
        return Response.ok(g.toJson(workSheet)).build();
    }

    @POST
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteWorkSheet(DeleteWorkSheetRequest request) {
        AuthToken token = AuthenticationFilter.getTokenFromUsername(request.username);
        if (token == null || !token.isValid()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(g.toJson("Invalid or expired session"))
                    .build();
        }

        User user = User.fromEntity(datastore.get(
                datastore.newKeyFactory().setKind("User").newKey(request.username)));

        if (!user.isActive()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson("Account is not active"))
                    .build();
        }

        Key workSheetKey = datastore.newKeyFactory().setKind("WorkSheet").newKey(request.workSheetId);
        Entity workSheetEntity = datastore.get(workSheetKey);

        if (workSheetEntity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(g.toJson("Work sheet not found"))
                    .build();
        }

        if (!WorkSheet.fromEntity(workSheetEntity).getUsername().equals(request.username)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson("Access denied to this work sheet"))
                    .build();
        }

        datastore.delete(workSheetKey);
        return Response.ok(g.toJson("Work sheet deleted successfully")).build();
    }

    private static class GetWorkSheetRequest {
        public String username;
        public long workSheetId;
    }

    private static class DeleteWorkSheetRequest {
        public String username;
        public long workSheetId;
    }
} 