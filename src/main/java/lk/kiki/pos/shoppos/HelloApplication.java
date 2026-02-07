package lk.kiki.pos.shoppos;

import javafx.application.Application;
import javafx.stage.Stage;
import lk.kiki.pos.shoppos.ui.AppNavigator;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        AppNavigator.setStage(stage);
        AppNavigator.showLogin();   // start app with login screen
    }

    public static void main(String[] args) {
        launch(args);
    }
}
