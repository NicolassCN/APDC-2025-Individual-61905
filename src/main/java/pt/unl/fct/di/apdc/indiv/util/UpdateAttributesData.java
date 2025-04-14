package pt.unl.fct.di.apdc.indiv.util;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public class UpdateAttributesData {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{9,15}$");
    private static final Pattern NIF_PATTERN = Pattern.compile("^[0-9]{9}$");
    
    private String targetUser;
    private String name;
    private String email;
    private String phone;
    private String profile;
    private String taxId;
    private String address;
    private String employer;
    private String employerTaxId;
    private String jobTitle;
    private Instant updateTime;
    private String updatedBy;
    
    public UpdateAttributesData() {
        this.updateTime = Instant.now();
    }
    
    public void setTargetUser(String targetUser) {
        this.targetUser = Objects.requireNonNull(targetUser, "Target user cannot be null");
    }
    
    public boolean isValid() {
        if (!hasTargetUser()) {
            return false;
        }
        
        if (hasEmail() && !EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }
        
        if (hasPhone() && !PHONE_PATTERN.matcher(phone).matches()) {
            return false;
        }
        
        if (hasProfile() && !isValidProfile()) {
            return false;
        }
        
        if (hasTaxId() && !NIF_PATTERN.matcher(taxId).matches()) {
            return false;
        }
        
        if (hasEmployerTaxId() && !NIF_PATTERN.matcher(employerTaxId).matches()) {
            return false;
        }
        
        return true;
    }
    
    private boolean isValidProfile() {
        return profile.equalsIgnoreCase("publico") || 
               profile.equalsIgnoreCase("privado");
    }

    // Enhanced field presence checks
    public boolean hasTargetUser() {
        return targetUser != null && !targetUser.isBlank();
    }
    
    public boolean hasName() { return name != null && !name.isBlank(); }
    public boolean hasEmail() { return email != null && !email.isBlank(); }
    public boolean hasPhone() { return phone != null && !phone.isBlank(); }
    public boolean hasProfile() { return profile != null && !profile.isBlank(); }
    public boolean hasTaxId() { return taxId != null && !taxId.isBlank(); }
    public boolean hasAddress() { return address != null && !address.isBlank(); }
    public boolean hasEmployer() { return employer != null && !employer.isBlank(); }
    public boolean hasEmployerTaxId() { return employerTaxId != null && !employerTaxId.isBlank(); }
    public boolean hasJobTitle() { return jobTitle != null && !jobTitle.isBlank(); }

    // Getters and Setters with validation
    public String getTargetUser() { return targetUser; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getProfile() { return profile; }
    public String getTaxId() { return taxId; }
    public String getAddress() { return address; }
    public String getEmployer() { return employer; }
    public String getEmployerTaxId() { return employerTaxId; }
    public String getJobTitle() { return jobTitle; }
    public Instant getUpdateTime() { return updateTime; }
    public String getUpdatedBy() { return updatedBy; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setProfile(String profile) { this.profile = profile; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public void setAddress(String address) { this.address = address; }
    public void setEmployer(String employer) { this.employer = employer; }
    public void setEmployerTaxId(String employerTaxId) { this.employerTaxId = employerTaxId; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    @Override
    public String toString() {
        return String.format("UpdateAttributes[target=%s, updatedBy=%s, time=%s]",
            targetUser, updatedBy, updateTime);
    }
}
