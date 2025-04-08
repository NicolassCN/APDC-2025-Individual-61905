package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.indiv.filters.AuthenticationFilter;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;
import pt.unl.fct.di.apdc.indiv.util.User;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private final Gson g = new Gson();

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response login(LoginData data) {
		LOG.fine("Login attempt for user: " + data.username);

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity user = datastore.get(userKey);

		if (user == null) {
			LOG.warning("Failed login attempt for username: " + data.username);
			return Response.status(Response.Status.FORBIDDEN).entity(g.toJson("User does not exist")).build();
		}

		User u = User.fromEntity(user);
		if (!u.getPassword().equals(data.password)) {
			LOG.warning("Failed login attempt for username: " + data.username);
			return Response.status(Response.Status.FORBIDDEN).entity(g.toJson("Wrong password")).build();
		}

		if (!u.isActive()) {
			LOG.warning("Failed login attempt for inactive user: " + data.username);
			return Response.status(Response.Status.FORBIDDEN).entity(g.toJson("User is not active")).build();
		}

		AuthToken token = new AuthToken(data.username, u.getRole());
		AuthenticationFilter.addToken(data.username, token);

		LOG.info("User logged in successfully: " + data.username);
		return Response.ok(g.toJson(token)).build();
	}

	@GET
	@Path("/{username}")
	public Response checkUsernameAvailable(@PathParam("username") String username) {
		
		return Response.ok().entity(g.toJson(true)).build();
	}
}

class LoginData {
	public String username;
	public String password;
}