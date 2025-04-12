package pt.unl.fct.di.apdc.indiv.util;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.StringValue;

public class User {
    private String userId;
    private String email;
    private String username;
    private String fullName;
    private String phone;
    private String password;
    private String profile; // public, private
    private String citizenId;
    private String role; // ENDUSER, BACKOFFICE, ADMIN, PARTNER
    private String taxId;
    private String employer;
    private String position;
    private String address;
    private String employerTaxId;
    private String accountState; // ACTIVATED, SUSPENDED, DEACTIVATED
    private String photo;

    public User() {}

    public User(String userId, String email, String username, String fullName, String phone, String password,
                String profile, String role, String accountState) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.password = password;
        this.profile = profile;
        this.role = role;
        this.accountState = accountState;
        this.citizenId = "NOT_DEFINED";
        this.taxId = "NOT_DEFINED";
        this.employer = "NOT_DEFINED";
        this.position = "NOT_DEFINED";
        this.address = "NOT_DEFINED";
        this.employerTaxId = "NOT_DEFINED";
        this.photo = "NOT_DEFINED";
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getProfile() {
        return profile;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public String getRole() {
        return role;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getEmployer() {
        return employer;
    }

    public String getPosition() {
        return position;
    }

    public String getAddress() {
        return address;
    }

    public String getEmployerTaxId() {
        return employerTaxId;
    }

    public String getAccountState() {
        return accountState;
    }

    public String getPhoto() {
        return photo;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setEmployerTaxId(String employerTaxId) {
        this.employerTaxId = employerTaxId;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public Entity toEntity(com.google.cloud.datastore.Datastore datastore) {
        return Entity.newBuilder(datastore.newKeyFactory().setKind("User").newKey(userId))
                .set("email", email)
                .set("username", username)
                .set("fullName", fullName)
                .set("phone", phone)
                .set("password", password)
                .set("profile", profile)
                .set("citizenId", StringValue.of(citizenId))
                .set("role", role)
                .set("taxId", StringValue.of(taxId))
                .set("employer", StringValue.of(employer))
                .set("position", StringValue.of(position))
                .set("address", StringValue.of(address))
                .set("employerTaxId", StringValue.of(employerTaxId))
                .set("accountState", accountState)
                .set("photo", StringValue.of(photo))
                .build();
    }

    public static User fromEntity(Entity entity) {
        User user = new User();
        user.userId = entity.getKey().getName();
        user.email = entity.getString("email");
        user.username = entity.getString("username");
        user.fullName = entity.getString("fullName");
        user.phone = entity.getString("phone");
        user.password = entity.getString("password");
        user.profile = entity.getString("profile");
        user.citizenId = entity.getString("citizenId");
        user.role = entity.getString("role");
        user.taxId = entity.getString("taxId");
        user.employer = entity.getString("employer");
        user.position = entity.getString("position");
        user.address = entity.getString("address");
        user.employerTaxId = entity.getString("employerTaxId");
        user.accountState = entity.getString("accountState");
        user.photo = entity.getString("photo");
        return user;
    }
}