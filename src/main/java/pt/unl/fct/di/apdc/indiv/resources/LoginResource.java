package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.LoginData;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	private static final String MESSAGE_INVALID_CREDENTIALS = "Incorrect username or password.";

	private static final String LOG_MESSAGE_LOGIN_ATTEMPT = "Login attempt by user: ";
	private static final String LOG_MESSAGE_LOGIN_SUCCESSFUL = "Login successful for user: ";
	private static final String LOG_MESSAGE_WRONG_PASSWORD = "Wrong password provided for user: ";
	private static final String LOG_MESSAGE_USER_NOT_FOUND = "Login failed: User not found: ";
	private static final String LOG_MESSAGE_INACTIVE_ACCOUNT = "Login failed: Account inactive or invalid state for user: ";

	private static final String USER_PWD_PROPERTY = "user_pwd";
	private static final String USER_STATE_PROPERTY = "user_state";
	private static final String USER_ROLE_PROPERTY = "user_role";
	private static final String USER_LOGIN_TIME_PROPERTY = "user_login_time";

	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthToken");

	private final Gson g = new Gson();

	public LoginResource() {
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginData data, @Context HttpServletRequest request) {
		if (data == null || !data.isValid()) {
			LOG.warning("Login failed: Invalid or missing login data.");
			return Response.status(Status.BAD_REQUEST).entity("Invalid login data.").build();
		}

		// Set client information
		data = new LoginData(
			data.getUsername(),
			data.getPassword(),
			request.getRemoteAddr(),
			request.getHeader("User-Agent")
		);

		LOG.info(LOG_MESSAGE_LOGIN_ATTEMPT + data.getUsername());
		Key userKey = userKeyFactory.newKey(data.getUsername());

		try {
			Entity user = datastore.get(userKey);

			if (user != null) {
				LOG.info("User entity found for: " + data.getUsername());
				String hashedPWD_from_DB = user.getString(USER_PWD_PROPERTY);
				String hashedPWD_from_Input = DigestUtils.sha512Hex(data.getPassword());

				if (hashedPWD_from_DB != null && hashedPWD_from_DB.equals(hashedPWD_from_Input)) {
					LOG.info("Password match successful for user: " + data.getUsername());

					String userState = user.contains(USER_STATE_PROPERTY) ? user.getString(USER_STATE_PROPERTY) : "DESATIVADA";
					LOG.info("Account state for " + data.getUsername() + ": " + userState);

					if (!"ATIVADA".equalsIgnoreCase(userState)) {
						LOG.warning(LOG_MESSAGE_INACTIVE_ACCOUNT + data.getUsername() + " (State: " + userState + ")");
						return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS).build();
					}

					String userRole = user.contains(USER_ROLE_PROPERTY) ? user.getString(USER_ROLE_PROPERTY) : "ENDUSER";
					LOG.info("Account role for " + data.getUsername() + ": " + userRole);

					try {
						Entity updatedUser = Entity.newBuilder(user)
								.set(USER_LOGIN_TIME_PROPERTY, Timestamp.now())
								.build();
						datastore.update(updatedUser);
					} catch (DatastoreException e) {
						LOG.log(Level.WARNING, "Non-critical error: Failed to update login time for user " + data.getUsername(), e);
					}

					AuthToken token = new AuthToken(data.getUsername(), userRole, data.getClientIP());
					LOG.info("AuthToken created for user: " + data.getUsername() + " with TokenID: " + token.getTokenID());

					try {
						Key tokenKey = tokenKeyFactory.newKey(token.getTokenID());
						Entity tokenEntity = Entity.newBuilder(tokenKey)
								.set("username", token.getUsername())
								.set("role", token.getRole())
								.set("creationTime", Timestamp.of(java.util.Date.from(token.getCreationTime())))
								.set("expirationTime", Timestamp.of(java.util.Date.from(token.getExpirationTime())))
								.set("lastAccessIP", token.getLastAccessIP())
								.set("isRevoked", token.isRevoked())
								.build();
						datastore.put(tokenEntity);
						LOG.info("Token persisted to Datastore for TokenID: " + token.getTokenID());
					} catch (DatastoreException e) {
						LOG.log(Level.SEVERE, "Failed to persist token to Datastore for user: " + data.getUsername(), e);
					}

					LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.getUsername() + " (Role: " + userRole + ")");
					return Response.ok(g.toJson(token)).build();

				} else {
					data.incrementAttempt();
					LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.getUsername() + " (Attempt: " + data.getAttemptCount() + ")");
					return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS).build();
				}
			} else {
				LOG.warning(LOG_MESSAGE_USER_NOT_FOUND + data.getUsername());
				return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS).build();
			}
		} catch (DatastoreException e) {
			LOG.log(Level.SEVERE, "Datastore error during login for user: " + data.getUsername(), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Datastore error during login.").build();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Unexpected error during login for user: " + data.getUsername(), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unexpected error during login.").build();
		}
	}

	@POST
	@Path("/logout")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogout(@HeaderParam("Authorization") String authorizationHeader) {
		LOG.fine("Logout attempt received.");

		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			LOG.warning("Logout failed: Missing or invalid Authorization header format.");
			return Response.status(Status.BAD_REQUEST).entity("Invalid Authorization header.").build();
		}
		String tokenId = authorizationHeader.substring(7).trim();
		if (tokenId.isEmpty()) {
			LOG.warning("Logout failed: Token ID is empty.");
			return Response.status(Status.BAD_REQUEST).entity("Empty token ID.").build();
		}

		LOG.info("Logout attempt for TokenID: " + tokenId);

		try {
			Key tokenKey = tokenKeyFactory.newKey(tokenId);
			Entity tokenEntity = datastore.get(tokenKey);
			
			if (tokenEntity != null) {
				// Mark token as revoked instead of deleting
				Entity updatedToken = Entity.newBuilder(tokenEntity)
					.set("isRevoked", true)
					.build();
				datastore.update(updatedToken);
				
				LOG.info("Logout successful: Token revoked for TokenID: " + tokenId);
				return Response.ok().entity("Logout successful.").build();
			} else {
				LOG.warning("Logout failed: Token not found for TokenID: " + tokenId);
				return Response.status(Status.NOT_FOUND).entity("Token not found.").build();
			}
		} catch (DatastoreException e) {
			LOG.log(Level.SEVERE, "Logout Datastore error for TokenID: " + tokenId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error during logout (Datastore).").build();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Logout Unexpected error for TokenID: " + tokenId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error during logout (Unexpected).").build();
		}
	}
}