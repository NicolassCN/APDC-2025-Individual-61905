package pt.unl.fct.di.apdc.indiv.util.data;

public class LogoutData {
    private String token;

    public LogoutData() {
    }

    public LogoutData(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}