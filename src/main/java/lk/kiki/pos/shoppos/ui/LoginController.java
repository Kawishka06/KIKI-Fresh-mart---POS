package lk.kiki.pos.shoppos.ui;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lk.kiki.pos.shoppos.dao.UserDao;
import lk.kiki.pos.shoppos.model.User;
import lk.kiki.pos.shoppos.util.Session;

public class LoginController {

    @FXML private TextField txtUsername;

    // hidden + visible password fields
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;

    @FXML private CheckBox chkShowPassword;
    @FXML private Label lblError;

    private final UserDao userDao = new UserDao();

    @FXML
    public void initialize() {
        // keep both password fields in sync
        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());

        // ensure starting state
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);
    }

    @FXML
    private void onToggleShowPassword() {
        boolean show = chkShowPassword.isSelected();

        txtPasswordVisible.setVisible(show);
        txtPasswordVisible.setManaged(show);

        txtPassword.setVisible(!show);
        txtPassword.setManaged(!show);

        if (show) txtPasswordVisible.requestFocus();
        else txtPassword.requestFocus();
    }

    @FXML
    private void onLogin() {
        try {
            String u = txtUsername.getText() == null ? "" : txtUsername.getText().trim();

            String p = chkShowPassword.isSelected()
                    ? (txtPasswordVisible.getText() == null ? "" : txtPasswordVisible.getText().trim())
                    : (txtPassword.getText() == null ? "" : txtPassword.getText().trim());

            if (u.isEmpty() || p.isEmpty()) {
                lblError.setText("Enter username and password.");
                return;
            }

            User user = userDao.login(u, p);
            if (user == null) {
                lblError.setText("Invalid login.");
                return;
            }

            Session.set(user);

            // go to main menu
            AppNavigator.openMainWindow();

        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("Login error: " + e.getMessage());
        }
    }
}
