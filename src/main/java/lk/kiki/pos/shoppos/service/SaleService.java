package lk.kiki.pos.shoppos.service;

import lk.kiki.pos.shoppos.model.SaleItem;
import lk.kiki.pos.shoppos.util.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

public class SaleService {

    public long recordSale(String paymentMethod, List<SaleItem> items) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Sale items cannot be empty.");
        }

        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);

            try {
                long saleId = insertSaleHeader(con, paymentMethod);

                BigDecimal saleTotal = BigDecimal.ZERO;

                for (SaleItem item : items) {
                    validateItem(item);

                    // Lock product row for safe stock + cost retrieval
                    ProductForSale p = lockAndGetProductForSale(con, item.getProductId());

                    if (p.stockQty < item.getQty()) {
                        throw new IllegalStateException("Not enough stock for product_id=" + item.getProductId()
                                + " (available=" + p.stockQty + ", requested=" + item.getQty() + ")");
                    }

                    BigDecimal unitSell = item.getUnitSellPrice() != null ? item.getUnitSellPrice() : p.sellingPrice;
                    BigDecimal unitCostAtSale = p.avgCost; // IMPORTANT: capture cost at sale time

                    BigDecimal lineTotal = unitSell.multiply(BigDecimal.valueOf(item.getQty()));
                    saleTotal = saleTotal.add(lineTotal);

                    // Insert sale item (stores sell price + cost at sale)
                    insertSaleItem(con, saleId, item.getProductId(), item.getQty(), unitSell, unitCostAtSale, lineTotal);

                    // Decrease stock
                    decreaseStock(con, item.getProductId(), item.getQty());

                    // Stock movement
                    insertStockMovementSale(con, item.getProductId(), item.getQty(), unitCostAtSale, unitSell, saleId);
                }

                // Update sale header total
                updateSaleTotal(con, saleId, saleTotal);

                con.commit();
                return saleId;

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private void validateItem(SaleItem item) {
        if (item.getProductId() <= 0) throw new IllegalArgumentException("Invalid productId.");
        if (item.getQty() <= 0) throw new IllegalArgumentException("Qty must be > 0.");
        // unitSellPrice can be null (use product selling_price)
    }

    private long insertSaleHeader(Connection con, String paymentMethod) throws SQLException {
        String sql = "INSERT INTO sales (total, payment_method, status) VALUES (0.00, ?, 'COMPLETED')";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, paymentMethod == null ? "CASH" : paymentMethod);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to create sale header.");
    }

    private void insertSaleItem(Connection con, long saleId, int productId, int qty,
                                BigDecimal unitSell, BigDecimal unitCostAtSale, BigDecimal lineTotal) throws SQLException {
        String sql = """
                INSERT INTO sale_items (sale_id, product_id, qty, unit_sell_price, unit_cost_at_sale, line_total)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, saleId);
            ps.setInt(2, productId);
            ps.setInt(3, qty);
            ps.setBigDecimal(4, unitSell);
            ps.setBigDecimal(5, unitCostAtSale);
            ps.setBigDecimal(6, lineTotal);
            ps.executeUpdate();
        }
    }

    private void decreaseStock(Connection con, int productId, int qty) throws SQLException {
        String sql = "UPDATE products SET stock_qty = stock_qty - ? WHERE product_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    private void insertStockMovementSale(Connection con, int productId, int qtyOut,
                                         BigDecimal unitCostAtSale, BigDecimal unitSell, long saleId) throws SQLException {
        String sql = """
                INSERT INTO stock_movements
                (product_id, movement_type, qty_in, qty_out, unit_cost, unit_sell_price, reference_table, reference_id, note)
                VALUES (?, 'SALE', 0, ?, ?, ?, 'SALE', ?, 'Stock Out - Sale')
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, qtyOut);
            ps.setBigDecimal(3, unitCostAtSale);
            ps.setBigDecimal(4, unitSell);
            ps.setLong(5, saleId);
            ps.executeUpdate();
        }
    }

    private void updateSaleTotal(Connection con, long saleId, BigDecimal total) throws SQLException {
        String sql = "UPDATE sales SET total = ? WHERE sale_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, total);
            ps.setLong(2, saleId);
            ps.executeUpdate();
        }
    }

    // Locks row and gets current stock + avgCost + sellingPrice
    private ProductForSale lockAndGetProductForSale(Connection con, int productId) throws SQLException {
        String sql = "SELECT stock_qty, avg_cost, selling_price FROM products WHERE product_id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Product not found: " + productId);

                ProductForSale p = new ProductForSale();
                p.stockQty = rs.getInt("stock_qty");
                p.avgCost = rs.getBigDecimal("avg_cost");
                p.sellingPrice = rs.getBigDecimal("selling_price");
                return p;
            }
        }
    }

    private static class ProductForSale {
        int stockQty;
        BigDecimal avgCost;
        BigDecimal sellingPrice;
    }
}
