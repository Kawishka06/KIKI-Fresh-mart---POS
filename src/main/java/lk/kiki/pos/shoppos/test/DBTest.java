package lk.kiki.pos.shoppos.test;
import lk.kiki.pos.shoppos.util.DB;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBTest {
    public static void main(String[] args) {
        try (Connection con = DB.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT DATABASE()")) {

            if (rs.next()) {
                System.out.println("✅ Connected to database: " + rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
