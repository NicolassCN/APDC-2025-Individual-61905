package pt.unl.fct.di.apdc.indiv.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class AppInitializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Datastore datastore = DatastoreOptions.newBuilder().setProjectId("indiv-project-456220").build().getService();
        Key key = datastore.newKeyFactory().setKind("User").newKey("root");
        if (datastore.get(key) == null) {
            User root = new User("root", "root@admin.com", "root", "System Administrator",
                    "+351900000000", "RootAdmin123!", "private", "ADMIN", "ACTIVATED");
            datastore.put(root.toEntity(datastore));
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}