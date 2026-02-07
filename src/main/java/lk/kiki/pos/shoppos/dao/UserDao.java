package lk.kiki.pos.shoppos.dao;

import lk.kiki.pos.shoppos.model.User;
import lk.kiki.pos.shoppos.util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDao {

    public User login(String username, String password) throws Exception {
        String sql = """
        SELECT user_id, username, password_hash, role
        FROM users
        WHERE username=? AND active=1
    """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String hash = rs.getString("password_hash");
                boolean ok = org.mindrot.jbcrypt.BCrypt.checkpw(password, hash);

                if (!ok) return null;

                return new User(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("role")
                );
            }
        }
    }

}
