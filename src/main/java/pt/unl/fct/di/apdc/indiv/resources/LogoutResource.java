package pt.unl.fct.di.apdc.indiv.resources;

import java.util.logging.Logger;

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
import pt.unl.fct.di.apdc.indiv.util.data.LogoutData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LogoutResource {
    private static final Logger LOG = Logger.getLogger(LogoutResource.class.getName());
    private final Gson gson = new Gson();

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logout(@Context ContainerRequestContext context, LogoutData data) {
        LOG.fine("Logout attempt");

        try {
            AuthToken token = (AuthToken) context.getProperty("authToken");
            if (!token.getToken().equals(data.getToken())) {
                throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), "Invalid session");
            }

            // In production, add token to blacklist
            return Response.ok(gson.toJson(new LogoutResponse("Logout successful. Session terminated"))).build();
        } catch (AppException e) {
            LOG.warning("Logout failed: " + e.getMessage());
            return Response.status(e.getStatus()).entity(gson.toJson(new ErrorResponse(e.getMessage()))).build();
        }
    }

    private static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class LogoutResponse {
        public String message;
        public LogoutResponse(String message) {
            this.message = message;
        }
    }
}