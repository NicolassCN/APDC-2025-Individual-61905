package pt.unl.fct.di.apdc.indiv.util;

import java.util.Date;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class WorkSheet {
    private Long id;
    private String username;
    private String title;
    private String description;
    private Date startDate;
    private Date endDate;
    private String status;
    private String priority;
    private String category;
    private String[] tags;
    private String[] attachments;
    private Date createdAt;
    private Date updatedAt;

    public WorkSheet() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.status = "PENDING";
    }

    public WorkSheet(String username, String title, String description) {
        this();
        this.username = username;
        this.title = title;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String[] getAttachments() {
        return attachments;
    }

    public void setAttachments(String[] attachments) {
        this.attachments = attachments;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Entity toEntity(Key workSheetKey) {
        return Entity.newBuilder(workSheetKey)
                .set("username", this.username)
                .set("title", this.title)
                .set("description", this.description)
                .set("startDate", this.startDate != null ? this.startDate.getTime() : 0)
                .set("endDate", this.endDate != null ? this.endDate.getTime() : 0)
                .set("status", this.status)
                .set("priority", this.priority != null ? this.priority : "")
                .set("category", this.category != null ? this.category : "")
                .set("tags", this.tags != null ? String.join(",", this.tags) : "")
                .set("attachments", this.attachments != null ? String.join(",", this.attachments) : "")
                .set("createdAt", this.createdAt.getTime())
                .set("updatedAt", this.updatedAt.getTime())
                .build();
    }

    public static WorkSheet fromEntity(Entity entity) {
        WorkSheet workSheet = new WorkSheet();
        workSheet.setId(entity.getKey().getId());
        workSheet.setUsername(entity.getString("username"));
        workSheet.setTitle(entity.getString("title"));
        workSheet.setDescription(entity.getString("description"));
        workSheet.setStartDate(new Date(entity.getLong("startDate")));
        workSheet.setEndDate(new Date(entity.getLong("endDate")));
        workSheet.setStatus(entity.getString("status"));
        workSheet.setPriority(entity.getString("priority"));
        workSheet.setCategory(entity.getString("category"));
        
        String tags = entity.getString("tags");
        if (!tags.isEmpty()) {
            workSheet.setTags(tags.split(","));
        }
        
        String attachments = entity.getString("attachments");
        if (!attachments.isEmpty()) {
            workSheet.setAttachments(attachments.split(","));
        }
        
        workSheet.setCreatedAt(new Date(entity.getLong("createdAt")));
        workSheet.setUpdatedAt(new Date(entity.getLong("updatedAt")));
        
        return workSheet;
    }
} 