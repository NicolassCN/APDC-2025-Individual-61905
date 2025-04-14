package pt.unl.fct.di.apdc.indiv.util;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class Worksheet {
    public enum State {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    private String id;
    private String title;
    private String description;
    private String partnerId;
    private State state;
    private String createdBy;
    private String createdAt;
    private String lastModified;
    private String completedAt;

    public Worksheet() {
        this.state = State.PENDING;
        this.createdAt = java.time.Instant.now().toString();
        this.lastModified = this.createdAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public State getState() { return state; }
    public void setState(State state) { 
        this.state = state;
        this.lastModified = java.time.Instant.now().toString();
        if (state == State.COMPLETED) {
            this.completedAt = this.lastModified;
        }
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public Entity toEntity(Key worksheetKey) {
        Entity.Builder builder = Entity.newBuilder(worksheetKey)
                .set("title", this.title)
                .set("description", this.description)
                .set("partnerId", this.partnerId)
                .set("state", this.state.toString())
                .set("createdBy", this.createdBy)
                .set("createdAt", this.createdAt)
                .set("lastModified", this.lastModified);
        
        if (this.completedAt != null) {
            builder.set("completedAt", this.completedAt);
        }
        
        return builder.build();
    }

    public static Worksheet fromEntity(Entity entity) {
        Worksheet worksheet = new Worksheet();
        worksheet.setId(entity.getKey().getName());
        worksheet.setTitle(entity.getString("title"));
        worksheet.setDescription(entity.getString("description"));
        worksheet.setPartnerId(entity.getString("partnerId"));
        worksheet.setState(State.valueOf(entity.getString("state")));
        worksheet.setCreatedBy(entity.getString("createdBy"));
        worksheet.setCreatedAt(entity.getString("createdAt"));
        worksheet.setLastModified(entity.getString("lastModified"));
        if (entity.contains("completedAt")) {
            worksheet.setCompletedAt(entity.getString("completedAt"));
        }
        return worksheet;
    }
} 