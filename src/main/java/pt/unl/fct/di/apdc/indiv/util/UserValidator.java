package pt.unl.fct.di.apdc.indiv.util;

import java.util.regex.Pattern;

public class UserValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^\\+?[1-9]\\d{1,14}$");
    }

    public static boolean isValidProfile(String profile) {
        return profile != null && (profile.equals("public") || profile.equals("private"));
    }

    public static boolean isValidRole(String role) {
        return role != null && (role.equals("ENDUSER") || role.equals("BACKOFFICE") || role.equals("ADMIN") || role.equals("PARTNER"));
    }

    public static boolean isValidAccountState(String state) {
        return state != null && (state.equals("ACTIVATED") || state.equals("SUSPENDED") || state.equals("DEACTIVATED"));
    }
}