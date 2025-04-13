package pt.unl.fct.di.apdc.indiv.util;

import java.util.regex.Pattern;

public class UserValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[0-9]{9,}$");
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

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
        // Validate required fields
        if (user.getEmail() == null || user.getEmail().trim().isEmpty())
            return new ValidationResult(false, "Email é obrigatório");
        if (!EMAIL_PATTERN.matcher(user.getEmail()).matches())
            return new ValidationResult(false, "Email inválido");

        if (user.getUsername() == null || user.getUsername().trim().isEmpty())
            return new ValidationResult(false, "Username é obrigatório");

        if (user.getFullName() == null || user.getFullName().trim().isEmpty())
            return new ValidationResult(false, "Nome completo é obrigatório");

        if (user.getPhone() == null || user.getPhone().trim().isEmpty())
            return new ValidationResult(false, "Telefone é obrigatório");
        if (!PHONE_PATTERN.matcher(user.getPhone()).matches())
            return new ValidationResult(false, "Telefone inválido (deve começar com + e ter pelo menos 9 dígitos)");

        if (user.getPassword() == null || user.getPassword().trim().isEmpty())
            return new ValidationResult(false, "Password é obrigatória");
        if (!PASSWORD_PATTERN.matcher(user.getPassword()).matches())
            return new ValidationResult(false, "Password deve conter letras maiúsculas, minúsculas, números e caracteres especiais");

        if (user.getConfirmPassword() == null || !user.getPassword().equals(user.getConfirmPassword()))
            return new ValidationResult(false, "As passwords não coincidem");

        if (user.getProfile() == null || user.getProfile().trim().isEmpty())
            return new ValidationResult(false, "Perfil é obrigatório");
        if (!user.getProfile().equals("publico") && !user.getProfile().equals("privado"))
            return new ValidationResult(false, "Perfil deve ser 'publico' ou 'privado'");

        // Validate optional fields if present
        if (user.getRole() != null && !isValidRole(user.getRole()))
            return new ValidationResult(false, "Role inválido");

        if (user.getAccountState() != null && !isValidAccountState(user.getAccountState()))
            return new ValidationResult(false, "Estado de conta inválido");

        return new ValidationResult(true, "Validação bem sucedida");
    }

    public static ValidationResult validatePasswordChange(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword == null || currentPassword.trim().isEmpty())
            return new ValidationResult(false, "Password atual é obrigatória");

        if (newPassword == null || newPassword.trim().isEmpty())
            return new ValidationResult(false, "Nova password é obrigatória");

        if (!PASSWORD_PATTERN.matcher(newPassword).matches())
            return new ValidationResult(false, "Nova password deve conter letras maiúsculas, minúsculas, números e caracteres especiais");

        if (!newPassword.equals(confirmPassword))
            return new ValidationResult(false, "As passwords não coincidem");

        if (currentPassword.equals(newPassword))
            return new ValidationResult(false, "Nova password deve ser diferente da atual");

        return new ValidationResult(true, "Validação bem sucedida");
    }

    private static boolean isValidRole(String role) {
        return role.equals("ENDUSER") || role.equals("BACKOFFICE") || 
               role.equals("ADMIN") || role.equals("PARTNER");
    }

    private static boolean isValidAccountState(String state) {
        return state.equals("ATIVADA") || state.equals("DESATIVADA");
    }
} 