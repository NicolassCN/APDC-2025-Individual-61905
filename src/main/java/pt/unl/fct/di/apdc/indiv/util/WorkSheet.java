package pt.unl.fct.di.apdc.indiv.util;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class WorkSheet {
    public enum TargetType { PUBLIC_PROPERTY, PRIVATE_PROPERTY }
    public enum AwardStatus { AWARDED, NOT_AWARDED }
    public enum WorkStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

    private String reference;
    private String description;
    private TargetType targetType;
    private AwardStatus awardStatus;
    private String awardDate;
    private String startDate;
    private String endDate;
    private String assignedEntity;
    private String awardedEntity;
    private String companyTaxId;
    private WorkStatus workStatus;
    private String notes;

    public WorkSheet() {}

    public WorkSheet(String reference, String description, String targetType, String awardStatus) {
        if (reference == null || reference.trim().isEmpty()) throw new IllegalArgumentException("Reference is required");
        if (description == null || description.trim().isEmpty()) throw new IllegalArgumentException("Description is required");
        this.reference = reference;
        this.description = description;
        this.targetType = TargetType.valueOf(targetType);
        this.awardStatus = AwardStatus.valueOf(awardStatus);
    }

    // Getters
    public String getReference() { return reference; }
    public String getDescription() { return description; }
    public TargetType getTargetType() { return targetType; }
    public AwardStatus getAwardStatus() { return awardStatus; }
    public String getAwardDate() { return awardDate; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getAssignedEntity() { return assignedEntity; }
    public String getAwardedEntity() { return awardedEntity; }
    public String getCompanyTaxId() { return companyTaxId; }
    public WorkStatus getWorkStatus() { return workStatus; }
    public String getNotes() { return notes; }

    // Setters
    public void setDescription(String description) { this.description = description; }
    public void setTargetType(String targetType) { this.targetType = TargetType.valueOf(targetType); }
    public void setAwardStatus(String awardStatus) { this.awardStatus = AwardStatus.valueOf(awardStatus); }
    public void setAwardDate(String awardDate) { this.awardDate = awardDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setAssignedEntity(String assignedEntity) { this.assignedEntity = assignedEntity; }
    public void setAwardedEntity(String awardedEntity) { this.awardedEntity = awardedEntity; }
    public void setCompanyTaxId(String companyTaxId) { this.companyTaxId = companyTaxId; }
    public void setWorkStatus(String workStatus) { this.workStatus = workStatus != null ? WorkStatus.valueOf(workStatus) : null; }
    public void setNotes(String notes) { this.notes = notes; }

    public Entity toEntity(com.google.cloud.datastore.Datastore datastore) {
        Key key = datastore.newKeyFactory().setKind("WorkSheet").newKey(reference);
        Entity.Builder builder = Entity.newBuilder(key)
                .set("description", description)
                .set("targetType", targetType.name())
                .set("awardStatus", awardStatus.name());
        if (awardDate != null) builder.set("awardDate", awardDate);
        if (startDate != null) builder.set("startDate", startDate);
        if (endDate != null) builder.set("endDate", endDate);
        if (assignedEntity != null) builder.set("assignedEntity", assignedEntity);
        if (awardedEntity != null) builder.set("awardedEntity", awardedEntity);
        if (companyTaxId != null) builder.set("companyTaxId", companyTaxId);
        if (workStatus != null) builder.set("workStatus", workStatus.name());
        if (notes != null) builder.set("notes", notes);
        return builder.build();
    }

    public static WorkSheet fromEntity(Entity entity) {
        WorkSheet ws = new WorkSheet();
        ws.reference = entity.getKey().getName();
        ws.description = entity.getString("description");
        ws.targetType = TargetType.valueOf(entity.getString("targetType"));
        ws.awardStatus = AwardStatus.valueOf(entity.getString("awardStatus"));
        ws.awardDate = entity.contains("awardDate") ? entity.getString("awardDate") : null;
        ws.startDate = entity.contains("startDate") ? entity.getString("startDate") : null;
        ws.endDate = entity.contains("endDate") ? entity.getString("endDate") : null;
        ws.assignedEntity = entity.contains("assignedEntity") ? entity.getString("assignedEntity") : null;
        ws.awardedEntity = entity.contains("awardedEntity") ? entity.getString("awardedEntity") : null;
        ws.companyTaxId = entity.contains("companyTaxId") ? entity.getString("companyTaxId") : null;
        ws.workStatus = entity.contains("workStatus") ? WorkStatus.valueOf(entity.getString("workStatus")) : null;
        ws.notes = entity.contains("notes") ? entity.getString("notes") : null;
        return ws;
    }
}