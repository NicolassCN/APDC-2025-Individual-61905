package pt.unl.fct.di.apdc.indiv.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class User {
    public enum Role {
        ENDUSER, BACKOFFICE, ADMIN, PARTNER
    }

    public enum Profile {
        PRIVATE, PUBLIC
    }

    public enum AccountState {
        ACTIVATED, DEACTIVATED
    }

    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String password;
    private Profile profile;
    private Role role;
    private AccountState state;
    private String citizenCardNumber;
    private String taxId;
    private String employer;
    private String position;
    private String address;
    private String employerTaxId;
    private String photo;

    public User(String username, String email, String fullName, String phone, String password, String profile, String role, String state) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.password = password;
        this.profile = Profile.valueOf(profile.toUpperCase());
        this.role = Role.valueOf(role.toUpperCase());
        this.state = AccountState.valueOf(state.toUpperCase());
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public Profile getProfile() { return profile; }
    public Role getRole() { return role; }
    public AccountState getAccountState() { return state; }
    public String getCitizenCardNumber() { return citizenCardNumber; }
    public String getTaxId() { return taxId; }
    public String getEmployer() { return employer; }
    public String getPosition() { return position; }
    public String getAddress() { return address; }
    public String getEmployerTaxId() { return employerTaxId; }
    public String getPhoto() { return photo; }

    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPassword(String password) { this.password = password; }
    public void setProfile(Profile profile) { this.profile = profile; }
    public void setRole(Role role) { this.role = role; }
    public void setAccountState(AccountState state) { this.state = state; }
    public void setCitizenCardNumber(String citizenCardNumber) { this.citizenCardNumber = citizenCardNumber; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public void setEmployer(String employer) { this.employer = employer; }
    public void setPosition(String position) { this.position = position; }
    public void setAddress(String address) { this.address = address; }
    public void setEmployerTaxId(String employerTaxId) { this.employerTaxId = employerTaxId; }
    public void setPhoto(String photo) { this.photo = photo; }

    public boolean isPasswordValid(String password) {
        return UserValidator.checkPassword(password, this.password);
    }

    public boolean canChangeRole(User target) {
        if (this.role == Role.ADMIN) return true;
        if (this.role == Role.BACKOFFICE) {
            return target.role == Role.ENDUSER || target.role == Role.PARTNER;
        }
        return false;
    }

    public boolean canChangeAccountState(User target) {
        if (this.role == Role.ADMIN) return true;
        if (this.role == Role.BACKOFFICE) {
            return target.role == Role.ENDUSER || target.role == Role.PARTNER;
        }
        return false;
    }

    public boolean canViewUserDetails(User target) {
        if (this.role == Role.ADMIN) return true;
        if (this.role == Role.BACKOFFICE) {
            return target.role == Role.ENDUSER || target.role == Role.PARTNER;
        }
        return target.profile == Profile.PUBLIC || this.username.equals(target.username);
    }

    public boolean canRemoveUser(User target) {
        if (this.role == Role.ADMIN) return true;
        if (this.role == Role.BACKOFFICE) {
            return target.role == Role.ENDUSER || target.role == Role.PARTNER;
        }
        return false;
    }

    public Entity toEntity(Datastore datastore) {
        Key key = datastore.newKeyFactory().setKind("User").newKey(username);
        return Entity.newBuilder(key)
                .set("username", username)
                .set("email", email)
                .set("fullName", fullName)
                .set("phone", phone)
                .set("password", password)
                .set("profile", profile.name())
                .set("role", role.name())
                .set("state", state.name())
                .set("citizenCardNumber", citizenCardNumber != null ? citizenCardNumber : "")
                .set("taxId", taxId != null ? taxId : "")
                .set("employer", employer != null ? employer : "")
                .set("position", position != null ? position : "")
                .set("address", address != null ? address : "")
                .set("employerTaxId", employerTaxId != null ? employerTaxId : "")
                .set("photo", photo != null ? photo : "")
                .build();
    }

    public static User fromEntity(Entity entity) {
        User user = new User(
            entity.getString("username"),
            entity.getString("email"),
            entity.getString("fullName"),
            entity.getString("phone"),
            entity.getString("password"),
            entity.getString("profile"),
            entity.getString("role"),
            entity.getString("state")
        );
        
        user.citizenCardNumber = entity.getString("citizenCardNumber");
        user.taxId = entity.getString("taxId");
        user.employer = entity.getString("employer");
        user.position = entity.getString("position");
        user.address = entity.getString("address");
        user.employerTaxId = entity.getString("employerTaxId");
        user.photo = entity.getString("photo");
        
        return user;
    }
}