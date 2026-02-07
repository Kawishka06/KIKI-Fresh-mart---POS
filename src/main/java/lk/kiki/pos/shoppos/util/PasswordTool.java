package lk.kiki.pos.shoppos.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordTool {
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw("admin123", BCrypt.gensalt()));
        System.out.println(BCrypt.hashpw("cash123", BCrypt.gensalt()));
    }
}
