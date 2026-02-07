package lk.kiki.pos.shoppos.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lk.kiki.pos.shoppos.util.Session;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import lk.kiki.pos.shoppos.ui.UiEvents;


public class MainController {

    @FXML private StackPane contentPane;
    @FXML private VBox sideMenu;
    @FXML private Label lblUserInfo;
    @FXML private Button btnPurchases;
    @FXML private Button btnReports;
    @FXML private Button btnLowStock;


    private boolean menuVisible = true;

    // ✅ ONE initialize method
    @FXML
    public void initialize() {
        openProducts(); // default screen
        refreshLowStockBadge();
        UiEvents.onStockChanged(this::refreshLowStockBadge);

        var u = lk.kiki.pos.shoppos.util.Session.get();
        if (u != null) {
            lblUserInfo.setText("Logged in as: " + u.getUsername() + " (" + u.getRole() + ")");
        }


        boolean isAdmin = Session.isAdmin();
        if (!isAdmin) {
            btnPurchases.setDisable(true);
            btnReports.setDisable(true);

            // If you prefer hide instead of disable:
            // btnPurchases.setVisible(false); btnPurchases.setManaged(false);
            // btnReports.setVisible(false); btnReports.setManaged(false);
        }

        try {
            int low = new lk.kiki.pos.shoppos.dao.ProductDao().countLowStock(5);
            if (low > 0) {
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("Low Stock Warning");
                a.setHeaderText("Low stock items detected");
                a.setContentText(low + " product(s) have stock below 5.");
                a.showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void openProducts() {
        loadView("products-view.fxml");
    }

    @FXML
    private void openSales() {
        loadView("sales-view.fxml");
    }

    @FXML
    private void openPurchases() {
        loadView("purchase-view.fxml");
    }

    @FXML
    private void openReports() {
        loadView("reports-view.fxml");
    }

    @FXML
    private void toggleMenu() {
        menuVisible = !menuVisible;
        sideMenu.setManaged(menuVisible);
        sideMenu.setVisible(menuVisible);
    }

    @FXML
    private void onLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Logout");
        confirm.setHeaderText("Logout from the system?");
        confirm.setContentText("You will be returned to the login screen.");

        var res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            lk.kiki.pos.shoppos.util.Session.clear();
            lk.kiki.pos.shoppos.ui.AppNavigator.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onExit() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Exit");
        confirm.setHeaderText("Exit KIKI FRESH MART POS?");
        confirm.setContentText("Any unsaved work will be lost.");

        var res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        System.exit(0);
    }

    private void loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/lk/kiki/pos/shoppos/" + fxml)
            );
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Navigation Error");
            a.setHeaderText("Failed to load screen");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }
    @FXML
    private void openLowStock() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/lk/kiki/pos/shoppos/products-view.fxml")
            );

            Node view = loader.load();

            ProductsController controller = loader.getController();
            controller.showLowStockOnly(5); // threshold = 5

            contentPane.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Navigation Error");
            a.setHeaderText("Failed to load Low Stock screen");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    private void refreshLowStockBadge() {
        try {
            int low = new lk.kiki.pos.shoppos.dao.ProductDao().countLowStock(5);
            btnLowStock.setText("Low Stock (" + low + ")");

            // Optional: highlight only if > 0
            if (low > 0) {
                if (!btnLowStock.getStyleClass().contains("lowStockBtn")) {
                    btnLowStock.getStyleClass().add("lowStockBtn");
                }
            } else {
                btnLowStock.getStyleClass().remove("lowStockBtn");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
