package lk.kiki.pos.shoppos.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lk.kiki.pos.shoppos.dao.ProductDao;
import lk.kiki.pos.shoppos.model.Product;
import lk.kiki.pos.shoppos.model.PurchaseCartItem;
import lk.kiki.pos.shoppos.model.PurchaseItem;
import lk.kiki.pos.shoppos.service.PurchaseService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PurchaseController {

    @FXML private TextField txtSupplier;
    @FXML private TextField txtSearch;

    // products table
    @FXML private TableView<Product> tblProducts;
    @FXML private TableColumn<Product, String> colSku;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Number> colStock;

    // cart table
    @FXML private TableView<PurchaseCartItem> tblCart;
    @FXML private TableColumn<PurchaseCartItem, String> colCName;
    @FXML private TableColumn<PurchaseCartItem, Number> colCQty;
    @FXML private TableColumn<PurchaseCartItem, BigDecimal> colCCost;
    @FXML private TableColumn<PurchaseCartItem, BigDecimal> colCTotal;

    @FXML private Label lblTotal;

    private final ProductDao productDAO = new ProductDao();
    private final PurchaseService purchaseService = new PurchaseService();

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final ObservableList<PurchaseCartItem> cart = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colSku.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSku()));
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colStock.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getStockQty()));
        tblProducts.setItems(products);

        colCName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colCQty.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getQty()));
        colCCost.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getUnitCost()));
        colCTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getLineTotal()));
        tblCart.setItems(cart);

        // Double click product -> add to cart (ask qty and cost)
        tblProducts.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    addToPurchaseCart(row.getItem());
                }
            });
            return row;
        });

        // Double click cart row -> edit qty/cost
        tblCart.setRowFactory(tv -> {
            TableRow<PurchaseCartItem> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    editCartRow(row.getItem());
                }
            });
            return row;
        });

        refreshProducts();
        updateTotal();
    }

    @FXML
    private void onSearch() {
        String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
        try {
            if (keyword.isEmpty()) {
                refreshProducts();
            } else {
                products.setAll(productDAO.search(keyword));
            }
        } catch (Exception e) {
            showError("Search failed", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onRemoveItem() {
        PurchaseCartItem selected = tblCart.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select an item", "Select a cart item to remove.");
            return;
        }
        cart.remove(selected);
        updateTotal();
    }

    @FXML
    private void onSavePurchase() {
        if (cart.isEmpty()) {
            showInfo("Empty purchase", "Add items before saving purchase.");
            return;
        }

        String supplier = txtSupplier.getText() == null ? "" : txtSupplier.getText().trim();
        if (supplier.isEmpty()) supplier = "Unknown Supplier";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Purchase");
        confirm.setHeaderText("Save this purchase?");
        confirm.setContentText("Supplier: " + supplier + "\nTotal Cost: " + lblTotal.getText());

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            List<PurchaseItem> items = cart.stream()
                    .map(ci -> new PurchaseItem(ci.getProductId(), ci.getQty(), ci.getUnitCost()))
                    .collect(Collectors.toList());

            long purchaseId = purchaseService.recordPurchase(supplier, items);

            showInfo("Purchase Saved", "Purchase recorded. Purchase ID: " + purchaseId);

            cart.clear();
            updateTotal();
            refreshProducts();
            UiEvents.fireStockChanged();


        } catch (Exception e) {
            showError("Save purchase failed", e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshProducts() {
        try {
            products.setAll(productDAO.findAllActive());
        } catch (Exception e) {
            showError("Load products failed", e.getMessage());
            e.printStackTrace();
        }
    }

    private void addToPurchaseCart(Product p) {
        // Ask qty
        TextInputDialog qtyDialog = new TextInputDialog("1");
        qtyDialog.setHeaderText("Enter purchase quantity for: " + p.getName());
        String qtyStr = qtyDialog.showAndWait().orElse(null);
        if (qtyStr == null) return;

        // Ask unit cost
        TextInputDialog costDialog = new TextInputDialog("0.00");
        costDialog.setHeaderText("Enter unit cost for: " + p.getName());
        String costStr = costDialog.showAndWait().orElse(null);
        if (costStr == null) return;

        try {
            int qty = Integer.parseInt(qtyStr.trim());
            BigDecimal cost = new BigDecimal(costStr.trim());

            if (qty <= 0) { showError("Invalid qty", "Qty must be > 0"); return; }
            if (cost.compareTo(BigDecimal.ZERO) <= 0) { showError("Invalid cost", "Cost must be > 0"); return; }

            // If already exists in cart, merge
            for (PurchaseCartItem ci : cart) {
                if (ci.getProductId() == p.getProductid()) {
                    ci.setQty(ci.getQty() + qty);
                    ci.setUnitCost(cost); // keep latest cost
                    tblCart.refresh();
                    updateTotal();
                    return;
                }
            }

            cart.add(new PurchaseCartItem(p.getProductid(), p.getName(), qty, cost));
            updateTotal();

        } catch (NumberFormatException e) {
            showError("Invalid input", "Qty must be an integer and cost must be a number.");
        }
    }

    private void editCartRow(PurchaseCartItem item) {
        TextInputDialog qtyDialog = new TextInputDialog(String.valueOf(item.getQty()));
        qtyDialog.setHeaderText("Update qty for: " + item.getName());
        String qtyStr = qtyDialog.showAndWait().orElse(null);
        if (qtyStr == null) return;

        TextInputDialog costDialog = new TextInputDialog(item.getUnitCost().toString());
        costDialog.setHeaderText("Update unit cost for: " + item.getName());
        String costStr = costDialog.showAndWait().orElse(null);
        if (costStr == null) return;

        try {
            int qty = Integer.parseInt(qtyStr.trim());
            BigDecimal cost = new BigDecimal(costStr.trim());

            if (qty <= 0) {
                cart.remove(item);
            } else {
                item.setQty(qty);
                item.setUnitCost(cost);
            }

            tblCart.refresh();
            updateTotal();

        } catch (NumberFormatException e) {
            showError("Invalid input", "Qty must be integer and cost must be a number.");
        }
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseCartItem ci : cart) total = total.add(ci.getLineTotal());
        lblTotal.setText(total.toString());
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
