package lk.kiki.pos.shoppos.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lk.kiki.pos.shoppos.dao.ProductDao;
import lk.kiki.pos.shoppos.model.Product;
import lk.kiki.pos.shoppos.model.PurchaseItem;
import lk.kiki.pos.shoppos.service.StockAdjustmentService;
import lk.kiki.pos.shoppos.service.PurchaseService;
import java.util.List;
import java.math.BigDecimal;
import java.util.List;
import javafx.scene.control.TableRow;


public class ProductsController {

    @FXML private TextField txtSearch;
    @FXML private TableView<Product> tblProducts;
    @FXML private Label lblTitle;
    @FXML private TableColumn<Product, Number> colId;
    @FXML private TableColumn<Product, String> colSku;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, BigDecimal> colSellPrice;
    @FXML private TableColumn<Product, Number> colStock;
    @FXML private TableColumn<Product, BigDecimal> colAvgCost;

    private final ProductDao productDAO = new ProductDao();
    private final ObservableList<Product> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getProductid()));
        colSku.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSku()));
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colCategory.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCategory()));
        colSellPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getSellingPrice()));
        colStock.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getStockQty()));
        colAvgCost.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getAvgCost()));

        tblProducts.setItems(data);

        // Low-stock highlight (stock < 5)
        tblProducts.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                } else if (item.getStockQty() < 5) {
                    setStyle("-fx-background-color: rgba(239,68,68,0.25);"); // soft red
                } else {
                    setStyle("");
                }
            }
        });


        // Load initial list
        onRefresh();
    }

    @FXML
    private void onSearch() {
        String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
        try {
            List<Product> list = keyword.isEmpty()
                    ? productDAO.findAllActive()
                    : productDAO.search(keyword);

            data.setAll(list);
        } catch (Exception e) {
            showError("Search failed", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onRefresh() {
        txtSearch.clear();
        try {
            data.setAll(productDAO.findAllActive());
        } catch (Exception e) {
            showError("Refresh failed", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onAddProduct() {
        // Simple dialog for now (we can upgrade to a proper form later)
        TextInputDialog skuDialog = new TextInputDialog();
        skuDialog.setHeaderText("Enter SKU (e.g. P101)");
        skuDialog.showAndWait().ifPresent(sku -> {
            try {
                TextInputDialog nameDialog = new TextInputDialog();
                nameDialog.setHeaderText("Enter product name");
                String name = nameDialog.showAndWait().orElse(null);
                if (name == null || name.trim().isEmpty()) return;

                TextInputDialog catDialog = new TextInputDialog();
                catDialog.setHeaderText("Enter category (optional)");
                String cat = catDialog.showAndWait().orElse("");

                TextInputDialog priceDialog = new TextInputDialog();
                priceDialog.setHeaderText("Enter selling price");
                String priceStr = priceDialog.showAndWait().orElse(null);
                if (priceStr == null) return;

                BigDecimal sellPrice = new BigDecimal(priceStr.trim());

                Product p = new Product();
                p.setSku(sku.trim());
                p.setName(name.trim());
                p.setCategory(cat.trim());
                p.setSellingPrice(sellPrice);

                int newId = productDAO.addProduct(p);
                System.out.println("Added product id=" + newId);

                onRefresh();

            } catch (Exception e) {
                showError("Add product failed", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void onUpdatePrice() {
        Product selected = tblProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select a product", "Please select a product from the table first.");
            return;
        }

        TextInputDialog priceDialog = new TextInputDialog(selected.getSellingPrice().toString());
        priceDialog.setHeaderText("Update selling price for: " + selected.getName());
        priceDialog.showAndWait().ifPresent(val -> {
            try {
                BigDecimal newPrice = new BigDecimal(val.trim());
                boolean ok = productDAO.updateSellingPrice(selected.getProductid(), newPrice);
                if (ok) onRefresh();
            } catch (Exception e) {
                showError("Update price failed", e.getMessage());
                e.printStackTrace();
            }
        });
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
    private final PurchaseService purchaseService = new PurchaseService();

    @FXML
    private void onStockIn() {
        Product selected = tblProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select a product", "Please select a product from the table first.");
            return;
        }

        TextInputDialog qtyDialog = new TextInputDialog("1");
        qtyDialog.setHeaderText("Stock In - Quantity for: " + selected.getName());
        String qtyStr = qtyDialog.showAndWait().orElse(null);
        if (qtyStr == null) return;

        TextInputDialog costDialog = new TextInputDialog("0.00");
        costDialog.setHeaderText("Stock In - Unit Cost (buying price) for: " + selected.getName());
        String costStr = costDialog.showAndWait().orElse(null);
        if (costStr == null) return;

        TextInputDialog supplierDialog = new TextInputDialog("Local Supplier");
        supplierDialog.setHeaderText("Supplier (optional)");
        String supplier = supplierDialog.showAndWait().orElse("Unknown");

        try {
            int qty = Integer.parseInt(qtyStr.trim());
            BigDecimal unitCost = new BigDecimal(costStr.trim());

            if (qty <= 0) {
                showError("Invalid quantity", "Quantity must be greater than 0.");
                return;
            }
            if (unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Invalid cost", "Unit cost must be greater than 0.");
                return;
            }

            long purchaseId = purchaseService.recordPurchase(
                    supplier,
                    List.of(new PurchaseItem(selected.getProductid(), qty, unitCost))
            );

            showInfo("Stock updated", "Stock in recorded. Purchase ID: " + purchaseId);
            onRefresh();
            UiEvents.fireStockChanged();


        } catch (NumberFormatException e) {
            showError("Invalid input", "Please enter numbers only for quantity and cost.");
        } catch (Exception e) {
            showError("Stock In failed", e.getMessage());
            e.printStackTrace();
        }
    }

    private final StockAdjustmentService stockAdjustmentService = new StockAdjustmentService();
    @FXML
    private void onStockOutLoss() {
        Product selected = tblProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select a product", "Please select a product from the table first.");
            return;
        }

        // Choose reason (fixed options)
        ChoiceDialog<String> reasonDialog = new ChoiceDialog<>(
                "DAMAGED",
                "DAMAGED", "EXPIRED", "THEFT", "OTHER"
        );
        reasonDialog.setHeaderText("Select loss reason for: " + selected.getName());
        String reason = reasonDialog.showAndWait().orElse(null);
        if (reason == null) return;

        TextInputDialog qtyDialog = new TextInputDialog("1");
        qtyDialog.setHeaderText("Enter quantity to remove (Loss) for: " + selected.getName());
        String qtyStr = qtyDialog.showAndWait().orElse(null);
        if (qtyStr == null) return;

        try {
            int qty = Integer.parseInt(qtyStr.trim());
            if (qty <= 0) {
                showError("Invalid quantity", "Quantity must be greater than 0.");
                return;
            }

            stockAdjustmentService.recordLoss(selected.getProductid(), qty, reason);

            showInfo("Stock updated", "Loss recorded: " + reason + " (qty " + qty + ")");
            onRefresh();
            UiEvents.fireStockChanged();


        } catch (NumberFormatException e) {
            showError("Invalid input", "Please enter a valid number for quantity.");
        } catch (Exception e) {
            showError("Stock Out (Loss) failed", e.getMessage());
            e.printStackTrace();
        }
    }

    public void showLowStockOnly(int threshold) {
        try {
            if (lblTitle != null) {
                lblTitle.setText("Low Stock Items");
            }

            data.setAll(productDAO.findLowStock(threshold));

        } catch (Exception e) {
            showError("Low stock load failed", e.getMessage());
            e.printStackTrace();
        }
    }





}
