package pt.unl.fct.di.apdc.indiv.util.data;

public class ChangeRoleData {
    public String username;
    public String newRole;
    public String token;

    public ChangeRoleData() {
    }

    public ChangeRoleData(String username, String newRole, String token) {
        this.username = username;
        this.newRole = newRole;
        this.token = token;
    }
}