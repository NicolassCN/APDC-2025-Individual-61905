package pt.unl.fct.di.apdc.indiv.util;

import java.util.regex.Pattern;

public class UserValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[A-Za-z0-9_]{3,20}$"
    );
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[0-9]{9,15}$"
    );
    private static final Pattern NIF_PATTERN = Pattern.compile(
        "^[0-9]{9}$"
    );
    private static final Pattern CC_PATTERN = Pattern.compile(
        "^[0-9]{8}$"
    );

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    public static ValidationResult validateUser(User user) {
        // Check required fields
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            return new ValidationResult(false, "Username is required");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return new ValidationResult(false, "Password is required");
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            return new ValidationResult(false, "Email is required");
        }

        // Validate username
        if (!USERNAME_PATTERN.matcher(user.getUsername()).matches()) {
            return new ValidationResult(false, 
                "Username must be 3-20 characters long and contain only letters, numbers, and underscores");
        }

        // Validate password
        if (!PASSWORD_PATTERN.matcher(user.getPassword()).matches()) {
            return new ValidationResult(false, 
                "Password must be at least 8 characters long and contain at least one uppercase letter, " +
                "one lowercase letter, one number, and one special character");
        }

        // Validate email
        if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            return new ValidationResult(false, "Invalid email format");
        }

        // Validate phone if provided
        if (user.getPhone() != null && !user.getPhone().isEmpty() && 
            !PHONE_PATTERN.matcher(user.getPhone()).matches()) {
            return new ValidationResult(false, "Invalid phone number format");
        }

        // Validate NIF if provided
        if (user.getNif() != null && !user.getNif().isEmpty() && 
            !NIF_PATTERN.matcher(user.getNif()).matches()) {
            return new ValidationResult(false, "Invalid NIF format");
        }

        // Validate CC if provided
        if (user.getCc() != null && !user.getCc().isEmpty() && 
            !CC_PATTERN.matcher(user.getCc()).matches()) {
            return new ValidationResult(false, "Invalid CC format");
        }

        return new ValidationResult(true, "Validation successful");
    }

    public static ValidationResult validatePasswordChange(
            String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword == null || currentPassword.isEmpty()) {
            return new ValidationResult(false, "Current password is required");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return new ValidationResult(false, "New password is required");
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return new ValidationResult(false, "Password confirmation is required");
        }
        if (!newPassword.equals(confirmPassword)) {
            return new ValidationResult(false, "New password and confirmation do not match");
        }
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            return new ValidationResult(false, 
                "New password must be at least 8 characters long and contain at least one uppercase letter, " +
                "one lowercase letter, one number, and one special character");
        }
        return new ValidationResult(true, "Password validation successful");
    }
} 