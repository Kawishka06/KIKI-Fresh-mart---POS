module lk.kiki.pos.shoppos {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.apache.pdfbox;
    requires java.desktop;
    requires jbcrypt;

    opens lk.kiki.pos.shoppos to javafx.fxml;
    opens lk.kiki.pos.shoppos.util to javafx.fxml;
    opens lk.kiki.pos.shoppos.ui to javafx.fxml;


    exports lk.kiki.pos.shoppos;
    exports lk.kiki.pos.shoppos.util;
}