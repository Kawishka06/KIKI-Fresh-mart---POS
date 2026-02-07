package lk.kiki.pos.shoppos.util;

import lk.kiki.pos.shoppos.model.User;

public class Session {
    private static User currentUser;

    public static void set(User user) { currentUser = user; }
    public static User get() { return currentUser; }
    public static void clear() { currentUser = null; }

    public static boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
    }
}
