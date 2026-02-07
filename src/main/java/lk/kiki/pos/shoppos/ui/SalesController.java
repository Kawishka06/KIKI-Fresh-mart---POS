package lk.kiki.pos.shoppos.ui;

import lk.kiki.pos.shoppos.print.ReceiptPdfGenerator;
import java.nio.file.Path;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lk.kiki.pos.shoppos.dao.ProductDao;
import lk.kiki.pos.shoppos.model.CartItem;
import lk.kiki.pos.shoppos.model.Product;
import lk.kiki.pos.shoppos.model.SaleItem;
import lk.kiki.pos.shoppos.service.SaleService;
import lk.kiki.pos.shoppos.ui.UiEvents;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SalesController {

    @FXML private TextField txtSearch;

    // Products table
    @FXML private TableView<Product> tblProducts;
    @FXML private TableColumn<Product, String> colPSku;
    @FXML private TableColumn<Product, String> colPName;
    @FXML private TableColumn<Product, BigDecimal> colPPrice;
    @FXML private TableColumn<Product, Number> colPStock;

    // Cart table
    @FXML private TableView<CartItem> tblCart;
    @FXML private TableColumn<CartItem, String> colCName;
    @FXML private TableColumn<CartItem, Number> colCQty;
    @FXML private TableColumn<CartItem, BigDecimal> colCPrice;
    @FXML private TableColumn<CartItem, BigDecimal> colCTotal;

    @FXML private Label lblTotal;

    private final ProductDao productDAO = new ProductDao();
    private final SaleService saleService = new SaleService();

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final ObservableList<CartItem> cart = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Setup products table columns
        colPSku.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSku()));
        colPName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colPPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getSellingPrice()));
        colPStock.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getStockQty()));

        tblProducts.setItems(products);

        // Setup cart table columns
        colCName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colCQty.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getQty()));
        colCPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getUnitPrice()));
        colCTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getLineTotal()));

        tblCart.setItems(cart);

        // Double click on product -> add to cart
        tblProducts.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    addProductToCart(row.getItem());
                }
            });
            return row;
        });

        // Double click on cart -> change qty
        tblCart.setRowFactory(tv -> {
            TableRow<CartItem> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    changeCartQty(row.getItem());
                }
            });
            return row;
        });

        // Initial load
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
        CartItem selected = tblCart.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select an item", "Select a cart item to remove.");
            return;
        }
        cart.remove(selected);
        updateTotal();
    }

    @FXML
    private void onCheckout() {
        if (cart.isEmpty()) {
            showInfo("Cart empty", "Add items to cart before checkout.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Checkout");
        confirm.setHeaderText("Checkout this sale?");
        confirm.setContentText("Total: " + lblTotal.getText() + " (CASH)");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            // Convert cart -> SaleItem list (for DB)
            List<SaleItem> saleItems = cart.stream()
                    .map(ci -> new SaleItem(ci.getProductId(), ci.getQty(), ci.getUnitPrice()))
                    .collect(Collectors.toList());

            // Save sale in DB
            long saleId = saleService.recordSale("CASH", saleItems);

            // Build receipt lines (from cart) BEFORE clearing cart
            var receiptLines = cart.stream()
                    .map(ci -> new ReceiptPdfGenerator.ReceiptLine(
                            ci.getName(),
                            ci.getQty(),
                            ci.getUnitPrice(),
                            ci.getLineTotal()
                    ))
                    .toList();

            Path pdf = ReceiptPdfGenerator.generateReceipt(
                    saleId,
                    "KIKI SHOP", // change shop name
                    receiptLines,
                    new BigDecimal(lblTotal.getText())
            );

            // Optional: auto-open PDF
            ReceiptPdfGenerator.openFile(pdf);

            showInfo("Sale Completed", "Sale ID: " + saleId + "\nReceipt: " + pdf.toString());

            // Clear cart and refresh stock
            cart.clear();
            updateTotal();
            refreshProducts();
            UiEvents.fireStockChanged();

        } catch (Exception e) {
            showError("Checkout failed", e.getMessage());
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


    private void addProductToCart(Product p) {
        if (p.getStockQty() <= 0) {
            showInfo("Out of stock", "This item has no stock.");
            return;
        }

        // If already in cart, increase qty (but don't exceed stock)
        for (CartItem ci : cart) {
            if (ci.getProductId() == p.getProductid()) {
                if (ci.getQty() + 1 > p.getStockQty()) {
                    showError("Not enough stock", "Cannot add more than available stock.");
                    return;
                }
                ci.setQty(ci.getQty() + 1);
                tblCart.refresh();
                updateTotal();
                return;
            }
        }

        // Add new
        cart.add(new CartItem(
                p.getProductid(),
                p.getName(),
                1,
                p.getSellingPrice()
        ));

        updateTotal();
    }

    private void changeCartQty(CartItem item) {
        // Find current stock from products list
        Product p = products.stream()
                .filter(x -> x.getProductid() == item.getProductId())
                .findFirst()
                .orElse(null);

        int maxStock = (p != null) ? p.getStockQty() : Integer.MAX_VALUE;

        TextInputDialog dialog = new TextInputDialog(String.valueOf(item.getQty()));
        dialog.setTitle("Change Quantity");
        dialog.setHeaderText("Enter new quantity for: " + item.getName());
        dialog.setContentText("Qty (max " + maxStock + "):");

        dialog.showAndWait().ifPresent(val -> {
            try {
                int newQty = Integer.parseInt(val.trim());
                if (newQty <= 0) {
                    cart.remove(item);
                } else if (newQty > maxStock) {
                    showError("Not enough stock", "Max allowed: " + maxStock);
                    return;
                } else {
                    item.setQty(newQty);
                }
                tblCart.refresh();
                updateTotal();
            } catch (NumberFormatException e) {
                showError("Invalid number", "Please enter a valid quantity.");
            }
        });
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cart) {
            total = total.add(ci.getLineTotal());
        }
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
