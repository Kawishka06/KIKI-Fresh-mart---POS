package lk.kiki.pos.shoppos.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppNavigator {

    private static Stage primaryStage;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource("/lk/kiki/pos/shoppos/login-view.fxml"));
        Scene scene = new Scene(loader.load(), 420, 320);
        scene.getStylesheets().add(AppNavigator.class.getResource("/lk/kiki/pos/shoppos/app.css").toExternalForm());

        primaryStage.setTitle("Login - KIKI FRESH MART");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void openMainWindow() throws Exception {
        FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource("/lk/kiki/pos/shoppos/main-layout.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 750);
        scene.getStylesheets().add(AppNavigator.class.getResource("/lk/kiki/pos/shoppos/app.css").toExternalForm());

        primaryStage.setTitle("KIKI FRESH MART");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
