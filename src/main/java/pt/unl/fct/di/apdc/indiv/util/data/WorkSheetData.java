package pt.unl.fct.di.apdc.indiv.util.data;

public class WorkSheetData {
    private String reference;
    private String description;
    private String targetType;
    private String awardStatus;
    private String awardDate;
    private String startDate;
    private String endDate;
    private String assignedEntity;
    private String awardedEntity;
    private String companyTaxId;
    private String workStatus;
    private String notes;

    public WorkSheetData() {}

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
}