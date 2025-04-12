package pt.unl.fct.di.apdc.indiv.util;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.StringValue;

public class WorkSheet {
    private String reference;
    private String description;
    private String targetType; // PUBLIC_PROPERTY, PRIVATE_PROPERTY
    private String awardStatus; // AWARDED, NOT_AWARDED
    private String awardDate;
    private String startDate;
    private String endDate;
    private String assignedEntity;
    private String awardedEntity;
    private String companyTaxId;
    private String workStatus; // NOT_STARTED, IN_PROGRESS, COMPLETED
    private String notes;

    public WorkSheet() {}

    public WorkSheet(String reference, String description, String targetType, String awardStatus) {
        this.reference = reference;
        this.description = description;
        this.targetType = targetType;
        this.awardStatus = awardStatus;
        this.awardDate = "";
        this.startDate = "";
        this.endDate = "";
        this.assignedEntity = "";
        this.awardedEntity = "";
        this.companyTaxId = "";
        this.workStatus = "";
        this.notes = "";
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getAwardStatus() {
        return awardStatus;
    }

    public String getAwardDate() {
        return awardDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getAssignedEntity() {
        return assignedEntity;
    }

    public String getAwardedEntity() {
        return awardedEntity;
    }

    public String getCompanyTaxId() {
        return companyTaxId;
    }

    public String getWorkStatus() {
        return workStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setAwardStatus(String awardStatus) {
        this.awardStatus = awardStatus;
    }

    public void setAwardDate(String awardDate) {
        this.awardDate = awardDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setAssignedEntity(String assignedEntity) {
        this.assignedEntity = assignedEntity;
    }

    public void setAwardedEntity(String awardedEntity) {
        this.awardedEntity = awardedEntity;
    }

    public void setCompanyTaxId(String companyTaxId) {
        this.companyTaxId = companyTaxId;
    }

    public void setWorkStatus(String workStatus) {
        this.workStatus = workStatus;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Entity toEntity(com.google.cloud.datastore.Datastore datastore) {
        return Entity.newBuilder(datastore.newKeyFactory().setKind("WorkSheet").newKey(reference))
                .set("description", description)
                .set("targetType", targetType)
                .set("awardStatus", awardStatus)
                .set("awardDate", StringValue.of(awardDate))
                .set("startDate", StringValue.of(startDate))
                .set("endDate", StringValue.of(endDate))
                .set("assignedEntity", StringValue.of(assignedEntity))
                .set("awardedEntity", StringValue.of(awardedEntity))
                .set("companyTaxId", StringValue.of(companyTaxId))
                .set("workStatus", StringValue.of(workStatus))
                .set("notes", StringValue.of(notes))
                .build();
    }

    public static WorkSheet fromEntity(Entity entity) {
        WorkSheet ws = new WorkSheet();
        ws.reference = entity.getKey().getName();
        ws.description = entity.getString("description");
        ws.targetType = entity.getString("targetType");
        ws.awardStatus = entity.getString("awardStatus");
        ws.awardDate = entity.getString("awardDate");
        ws.startDate = entity.getString("startDate");
        ws.endDate = entity.getString("endDate");
        ws.assignedEntity = entity.getString("assignedEntity");
        ws.awardedEntity = entity.getString("awardedEntity");
        ws.companyTaxId = entity.getString("companyTaxId");
        ws.workStatus = entity.getString("workStatus");
        ws.notes = entity.getString("notes");
        return ws;
    }
}