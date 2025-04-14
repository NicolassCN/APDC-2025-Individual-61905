package pt.unl.fct.di.apdc.indiv.util;

public class AccessControl {
    private final User user;
    private final User.Role minimumRole;

    public AccessControl(User user, User.Role minimumRole) {
        this.user = user;
        this.minimumRole = minimumRole;
    }

    public boolean hasAccess() {
        if (user == null || !user.isActive()) {
            return false;
        }

        switch (minimumRole) {
            case USER:
                return true;
            case GBO:
                return user.getRole() == User.Role.GBO || 
                       user.getRole() == User.Role.GS ||
                       user.getRole() == User.Role.SU ||
                       user.getRole() == User.Role.ADMIN;
            case GS:
                return user.getRole() == User.Role.GS ||
                       user.getRole() == User.Role.SU ||
                       user.getRole() == User.Role.ADMIN;
            case SU:
                return user.getRole() == User.Role.SU ||
                       user.getRole() == User.Role.ADMIN;
            case ADMIN:
                return user.getRole() == User.Role.ADMIN;
            default:
                return false;
        }
    }

    public boolean isAdmin() {
        return user != null && user.isActive() && user.getRole() == User.Role.ADMIN;
    }

    public boolean isSU() {
        return user != null && user.isActive() && 
               (user.getRole() == User.Role.SU || user.getRole() == User.Role.ADMIN);
    }

    public boolean isGS() {
        return user != null && user.isActive() && 
               (user.getRole() == User.Role.GS || user.getRole() == User.Role.SU || 
                user.getRole() == User.Role.ADMIN);
    }

    public boolean isGBO() {
        return user != null && user.isActive() && 
               (user.getRole() == User.Role.GBO || user.getRole() == User.Role.GS || 
                user.getRole() == User.Role.SU || user.getRole() == User.Role.ADMIN);
    }

    public static boolean canManageWorksheets(User.Role role) {
        return role == User.Role.ADMIN || role == User.Role.GBO || role == User.Role.PARTNER;
    }

    public static boolean canUpdateWorksheetState(User.Role role, String partnerId, String username) {
        // Admins and GBOs can update any worksheet
        if (role == User.Role.ADMIN || role == User.Role.GBO) {
            return true;
        }
        
        // Partners can only update their own worksheets
        if (role == User.Role.PARTNER) {
            return username.equals(partnerId);
        }
        
        return false;
    }
} 