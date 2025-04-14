package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.indiv.util.RegisterData;

@Path("/register")
public class RegisterResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	public RegisterResource() {}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerUser(RegisterData data, @Context HttpServletRequest request) {
		LOG.info("Received registration attempt for user: " + (data != null ? data.getUsername() : "null data object"));

		if (data == null) {
			LOG.warning("Validation failed: Received null data object.");
			return Response.status(Status.BAD_REQUEST).entity("Invalid registration data: No data provided.").build();
		}

		if (!data.validRegistration()) {
			LOG.warning("Validation failed for user: " + data.getUsername());
			return Response.status(Status.BAD_REQUEST)
						 .entity("Registration validation failed. Please check all fields.")
						 .build();
		}

		LOG.info("Validation passed for user: " + data.getUsername());

		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.getUsername());
			Entity user = txn.get(userKey);

			if (user != null) {
				txn.rollback();
				LOG.warning("Registration failed: Username " + data.getUsername() + " already exists.");
				return Response.status(Status.CONFLICT).entity("Username already exists.").build();
			} else {
				user = Entity.newBuilder(userKey)
						.set("user_name", data.getName())
						.set("user_pwd", data.getHashedPassword())
						.set("user_email", data.getEmail())
						.set("user_phone", data.getPhone())
						.set("user_profile", data.getProfile().toLowerCase())
						.set("user_role", "ENDUSER")
						.set("user_state", "DESATIVADA")
						.set("user_creation_time", Timestamp.of(java.util.Date.from(data.getRegistrationTime())))
						.set("user_client_ip", data.getClientIP())
						.set("user_agent", data.getUserAgent())
						.build();

				txn.put(user);
				txn.commit();
				LOG.info("User registered successfully: " + data.getUsername());
				return Response.ok().entity("User registered successfully.").build();
			}
		} catch (DatastoreException e) {
			if (txn.isActive()) txn.rollback();
			LOG.log(Level.SEVERE, "Datastore error during registration for user: " + data.getUsername(), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
						 .entity("Datastore error: " + e.getMessage())
						 .build();
		} catch (Exception e) {
			if (txn.isActive()) txn.rollback();
			LOG.log(Level.SEVERE, "Unexpected error during registration for user: " + data.getUsername(), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
						 .entity("Unexpected error: " + e.getMessage())
						 .build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				LOG.warning("Transaction was still active in finally block for user: " + data.getUsername() + "; rolling back.");
			}
		}
	}
}