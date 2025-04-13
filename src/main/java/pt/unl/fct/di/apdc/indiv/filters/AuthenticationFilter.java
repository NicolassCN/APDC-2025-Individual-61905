package pt.unl.fct.di.apdc.indiv.filters;

import java.io.IOException;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.unl.fct.di.apdc.indiv.util.AuthToken;

public class AuthenticationFilter implements Filter {
    private static final Datastore datastore = DatastoreOptions.newBuilder()
            .setProjectId("indiv-project-456220")
            .build()
            .getService();

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getServletPath();
        if (path.equals("/rest/register") || path.equals("/rest/login")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String tokenId = authHeader.substring(7);
        try {
            Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenId);
            Entity tokenEntity = datastore.get(tokenKey);
            
            if (tokenEntity == null) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
            
            long expirationDate = tokenEntity.getLong("expirationDate");
            if (System.currentTimeMillis() > expirationDate) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
                return;
            }
            
            AuthToken authToken = AuthToken.fromEntity(tokenEntity);
            req.setAttribute("authToken", authToken);
            chain.doFilter(request, response);
        } catch (Exception e) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token: " + e.getMessage());
        }
    }

    @Override
    public void destroy() {}
}