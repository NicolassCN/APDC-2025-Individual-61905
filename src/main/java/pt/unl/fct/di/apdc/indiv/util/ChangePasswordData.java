package pt.unl.fct.di.apdc.indiv.util;

import java.time.Instant;
import java.util.regex.Pattern;

public class ChangePasswordData {
    private static final int MIN_PASSWORD_LENGTH = 10;
    private static final int MIN_CRITERIA_REQUIRED = 3;
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    
    private String currentPassword;
    private String newPassword;
    private String confirmation;
    private Instant requestTimestamp;
    private int attemptCount;

    public ChangePasswordData() {
        this.requestTimestamp = Instant.now();
        this.attemptCount = 0;
    }

    public ChangePasswordData(String currentPassword, String newPassword, String confirmation) {
        this();
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmation = confirmation;
    }

    public boolean isValid() {
        incrementAttempt();
        
        if (hasEmptyFields()) {
            return false;
        }
        
        if (currentPassword.equals(newPassword)) {
            return false; // New password cannot be the same as current
        }

        if (!newPassword.equals(confirmation)) {
            return false;
        }

        return validatePasswordComplexity(newPassword);
    }

    private boolean hasEmptyFields() {
        return currentPassword == null || currentPassword.isBlank() ||
               newPassword == null || newPassword.isBlank() ||
               confirmation == null || confirmation.isBlank();
    }

    private boolean validatePasswordComplexity(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }

        int criteriaCount = 0;
        
        if (containsUpperCase(password)) criteriaCount++;
        if (containsLowerCase(password)) criteriaCount++;
        if (containsDigit(password)) criteriaCount++;
        if (containsSpecialChar(password)) criteriaCount++;
        
        return criteriaCount >= MIN_CRITERIA_REQUIRED;
    }

    private boolean containsUpperCase(String str) {
        return str.chars().anyMatch(Character::isUpperCase);
    }

    private boolean containsLowerCase(String str) {
        return str.chars().anyMatch(Character::isLowerCase);
    }

    private boolean containsDigit(String str) {
        return str.chars().anyMatch(Character::isDigit);
    }

    private boolean containsSpecialChar(String str) {
        return SPECIAL_CHARS.matcher(str).find();
    }

    private void incrementAttempt() {
        this.attemptCount++;
    }

    // Getters and setters
    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public Instant getRequestTimestamp() {
        return requestTimestamp;
    }

    public int getAttemptCount() {
        return attemptCount;
    }
}
