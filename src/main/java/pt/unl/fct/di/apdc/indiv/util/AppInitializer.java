package pt.unl.fct.di.apdc.indiv.util;

import org.mindrot.jbcrypt.BCrypt;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class AppInitializer implements ServletContextListener {
    private static final String PROJECT_ID = "indiv-project-456220";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Datastore datastore = DatastoreOptions.newBuilder()
                    .setProjectId(PROJECT_ID)
                    .build()
                    .getService();

            Key key = datastore.newKeyFactory().setKind("User").newKey("root");
            if (datastore.get(key) == null) {
                String hashedPassword = BCrypt.hashpw("root", BCrypt.gensalt());
                User root = new User(
                        "root",
                        "root@admin.com",
                        "System Administrator",
                        "+351900000000",
                        hashedPassword,
                        "PRIVATE",
                        "ADMIN",
                        "ACTIVATED"
                );
                datastore.put(root.toEntity(datastore));
                sce.getServletContext().log("Root user created successfully");
            }
        } catch (Exception e) {
            sce.getServletContext().log("Error initializing root user: " + e.getMessage());
            throw new RuntimeException("Failed to initialize application", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}