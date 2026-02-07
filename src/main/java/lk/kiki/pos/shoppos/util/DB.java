package lk.kiki.pos.shoppos.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DB {
    private static final String URL ="jdbc:mysql://localhost:3306/shop_pos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Colombo";
    private static final String USER="pos_user";
    private static final String PASSWORD="PosUser@123";

    public static Connection getConnection()throws Exception{
        return DriverManager.getConnection(URL,USER,PASSWORD);

    }
}
