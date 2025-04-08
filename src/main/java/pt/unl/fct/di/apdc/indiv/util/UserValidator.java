package pt.unl.fct.di.apdc.indiv.util;

import java.util.regex.Pattern;

public class UserValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{9,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}$");

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

        if (user.getEmail() == null || !EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            return new ValidationResult(false, "Email inválido. Deve seguir o formato: usuario@dominio.tld");
        }

        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return new ValidationResult(false, "Username é obrigatório");
        }

        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            return new ValidationResult(false, "Nome completo é obrigatório");
        }

        if (user.getPhone() == null || !PHONE_PATTERN.matcher(user.getPhone()).matches()) {
            return new ValidationResult(false, "Telefone inválido. Deve conter pelo menos 9 dígitos");
        }

        if (user.getPassword() == null || !PASSWORD_PATTERN.matcher(user.getPassword()).matches()) {
            return new ValidationResult(false, "Senha inválida. Deve conter pelo menos 8 caracteres, incluindo maiúsculas, minúsculas, números e caracteres especiais");
        }

        if (user.getProfile() == null || (!user.getProfile().equals("public") && !user.getProfile().equals("private"))) {
            return new ValidationResult(false, "Perfil deve ser 'public' ou 'private'");
        }

        return new ValidationResult(true, "Usuário válido");
    }
} 