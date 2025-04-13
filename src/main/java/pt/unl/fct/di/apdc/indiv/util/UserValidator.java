package pt.unl.fct.di.apdc.indiv.util;

import java.util.regex.Pattern;

import org.mindrot.jbcrypt.BCrypt;

public class UserValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$"
    );

    private static final Pattern FULLNAME_PATTERN = Pattern.compile("^[A-Za-z]+(\\s[A-Za-z]+)+$");

    public static void validateUser(String userId, String email, String username, String fullName, String phone,
                                   String password, String confirmPassword, String profile) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (!isValidFullName(fullName)) {
            throw new IllegalArgumentException("Full name must contain at least two names");
        }
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException("Invalid phone number");
        }
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException("Password must contain uppercase, lowercase, number, and special character");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (!isValidProfile(profile)) {
            throw new IllegalArgumentException("Profile must be public or private");
        }
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && username.length() >= 3 && username.length() <= 30;
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidFullName(String fullName) {
        return fullName != null && FULLNAME_PATTERN.matcher(fullName).matches();
    }

    public static boolean isValidProfile(String profile) {
        return profile != null && (profile.equals("public") || profile.equals("private"));
    }

    public static boolean isValidRole(String role) {
        try {
            User.Role.valueOf(role);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isValidAccountState(String state) {
        try {
            User.AccountState.valueOf(state);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}