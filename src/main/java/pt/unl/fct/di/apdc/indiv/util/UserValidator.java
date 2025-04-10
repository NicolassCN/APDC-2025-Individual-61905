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
        // Validar email
        if (user.getEmail() == null || !EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            return new ValidationResult(false, "Email inválido. Deve seguir o formato: usuario@dominio.tld");
        }

        // Validar username
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return new ValidationResult(false, "Username é obrigatório");
        }

        // Validar nome completo
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            return new ValidationResult(false, "Nome completo é obrigatório");
        }

        // Validar telefone
        if (user.getPhone() == null || !PHONE_PATTERN.matcher(user.getPhone()).matches()) {
            return new ValidationResult(false, "Telefone inválido. Deve conter pelo menos 9 dígitos");
        }

        // Validar senha
        if (user.getPassword() == null || !PASSWORD_PATTERN.matcher(user.getPassword()).matches()) {
            return new ValidationResult(false, "Senha inválida. Deve conter pelo menos 8 caracteres, incluindo maiúsculas, minúsculas, números e caracteres especiais");
        }

        // Validar perfil
        if (user.getProfile() == null || (!user.getProfile().equals("public") && !user.getProfile().equals("private"))) {
            return new ValidationResult(false, "Perfil deve ser 'public' ou 'private'");
        }

        // Validar role se fornecido
        if (user.getRole() != null && !isValidRole(user.getRole())) {
            return new ValidationResult(false, "Role inválido. Deve ser 'ENDUSER', 'BACKOFFICE', 'ADMIN' ou 'PARTNER'");
        }

        // Validar estado da conta se fornecido
        if (user.getAccountState() != null && !isValidAccountState(user.getAccountState())) {
            return new ValidationResult(false, "Estado da conta inválido. Deve ser 'ATIVADA', 'DESATIVADA' ou 'SUSPENSA'");
        }

        // Validar NIF se fornecido
        if (user.getNif() != null && !user.getNif().matches("^[0-9]{9}$")) {
            return new ValidationResult(false, "NIF inválido. Deve conter 9 dígitos");
        }

        // Validar NIF da entidade empregadora se fornecido
        if (user.getEmployerNif() != null && !user.getEmployerNif().matches("^[0-9]{9}$")) {
            return new ValidationResult(false, "NIF da entidade empregadora inválido. Deve conter 9 dígitos");
        }

        return new ValidationResult(true, "Usuário válido");
    }
    
    public static String hashPassword(String password) {
        return org.apache.commons.codec.digest.DigestUtils.sha512Hex(password);
    }
    
    public static boolean isValidRole(String role) {
        if (role == null) return false;
        String normalizedRole = role.toUpperCase();
        return normalizedRole.equals("ENDUSER") || 
               normalizedRole.equals("BACKOFFICE") || 
               normalizedRole.equals("ADMIN") || 
               normalizedRole.equals("PARTNER");
    }
    
    public static boolean isValidAccountState(String state) {
        return state != null && (state.equals("ATIVADA") || state.equals("DESATIVADA") || state.equals("SUSPENSA"));
    }
} 