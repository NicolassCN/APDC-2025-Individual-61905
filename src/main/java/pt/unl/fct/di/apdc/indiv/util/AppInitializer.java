package pt.unl.fct.di.apdc.indiv.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import pt.unl.fct.di.apdc.indiv.resources.RegisterResource;

@WebListener
public class AppInitializer implements ServletContextListener {
    private static final Logger LOG = Logger.getLogger(AppInitializer.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("Initializing application...");
        
        try {
            // Criar usuário root
            Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
            RegisterResource.createRootUser(datastore);
            LOG.info("Root user created successfully");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to create root user: " + e.getMessage());
            LOG.log(Level.WARNING, "This is expected during local development if Datastore is not properly configured");
        }
        
        LOG.info("Application initialized successfully");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("Application shutting down...");
    }
} 