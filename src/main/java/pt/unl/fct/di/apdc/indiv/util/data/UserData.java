package pt.unl.fct.di.apdc.indiv.util.data;

import pt.unl.fct.di.apdc.indiv.util.User;

public class UserData {
    public static class RegisterResponse {
        public String message;
        public UserInfo user;

        public RegisterResponse(String message, User user) {
            this.message = message;
            this.user = new UserInfo(user);
        }

        static class UserInfo {
            public String username;
            public String email;
            public String accountState;
            public String role;

            public UserInfo(User user) {
                this.username = user.getUsername();
                this.email = user.getEmail();
                this.accountState = user.getAccountState();
                this.role = user.getRole();
            }
        }
    }

    public static class LoginData {
        public String identifier;
        public String password;
    }

    public static class LogoutData {
        public String token;
    }

    public static class ChangeRoleData {
        public String requesterUsername;
        public String username;
        public String newRole;
    }

    public static class ChangeAccountStateData {
        public String requesterUsername;
        public String username;
        public String newState;
    }

    public static class RemoveAccountData {
        public String requesterUsername;
        public String username;
        public String email;
    }

    public static class ListUsersData {
        public String requesterUsername;
    }

    public static class ChangeAttributesData {
        public String requesterUsername;
        public String identifier;
        public java.util.Map<String, String> attributes;
    }

    public static class ChangePasswordData {
        public String username;
        public String currentPassword;
        public String newPassword;
        public String confirmPassword;
    }
} 