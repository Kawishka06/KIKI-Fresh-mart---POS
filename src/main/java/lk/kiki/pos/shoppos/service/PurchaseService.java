package lk.kiki.pos.shoppos.service;

import lk.kiki.pos.shoppos.model.PurchaseItem;
import lk.kiki.pos.shoppos.util.DB;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.List;

public class PurchaseService {

    /**
     * Records ONE purchase with multiple items.
     * - Inserts purchases row
     * - Inserts purchase_items rows
     * - Updates products.stock_qty
     * - Updates products.avg_cost using weighted average cost
     * - Inserts stock_movements rows (PURCHASE)
     */
    public long recordPurchase(String supplier, List<PurchaseItem> items) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Purchase items cannot be empty.");
        }

        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);

            try {
                long purchaseId = insertPurchaseHeader(con, supplier);

                BigDecimal purchaseTotal = BigDecimal.ZERO;

                for (PurchaseItem item : items) {
                    validateItem(item);

                    // Lock product row so stock/avg_cost can't be corrupted
                    ProductStockCost sc = lockAndGetStockCost(con, item.getProductId());

                    // Weighted average cost update
                    int currentStock = sc.stockQty;
                    BigDecimal currentAvgCost = sc.avgCost;

                    int buyQty = item.getQty();
                    BigDecimal buyUnitCost = item.getUnitCost();

                    BigDecimal lineTotal = buyUnitCost.multiply(BigDecimal.valueOf(buyQty));
                    purchaseTotal = purchaseTotal.add(lineTotal);

                    // newAvgCost = (S*C + Q*U) / (S+Q)
                    int newStock = currentStock + buyQty;

                    BigDecimal newAvgCost;
                    if (newStock == 0) {
                        newAvgCost = BigDecimal.ZERO;
                    } else {
                        BigDecimal numerator = BigDecimal.valueOf(currentStock).multiply(currentAvgCost)
                                .add(BigDecimal.valueOf(buyQty).multiply(buyUnitCost));
                        newAvgCost = numerator
                                .divide(BigDecimal.valueOf(newStock), 2, RoundingMode.HALF_UP);
                    }

                    // Insert purchase item
                    insertPurchaseItem(con, purchaseId, item.getProductId(), buyQty, buyUnitCost, lineTotal);

                    // Update product stock + avg_cost
                    updateProductStockAndAvgCost(con, item.getProductId(), newStock, newAvgCost);

                    // Insert stock movement
                    insertStockMovementPurchase(con, item.getProductId(), buyQty, buyUnitCost, purchaseId);
                }

                // Update purchase header total
                updatePurchaseTotal(con, purchaseId, purchaseTotal);

                con.commit();
                return purchaseId;

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private void validateItem(PurchaseItem item) {
        if (item.getProductId() <= 0) throw new IllegalArgumentException("Invalid productId.");
        if (item.getQty() <= 0) throw new IllegalArgumentException("Qty must be > 0.");
        if (item.getUnitCost() == null || item.getUnitCost().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Unit cost must be > 0.");
    }

    private long insertPurchaseHeader(Connection con, String supplier) throws SQLException {
        String sql = "INSERT INTO purchases (supplier, total_cost) VALUES (?, 0.00)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, supplier);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to create purchase header.");
    }

    private void insertPurchaseItem(Connection con, long purchaseId, int productId, int qty,
                                    BigDecimal unitCost, BigDecimal lineTotal) throws SQLException {
        String sql = """
                INSERT INTO purchase_items (purchase_id, product_id, qty, unit_cost, line_total)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, purchaseId);
            ps.setInt(2, productId);
            ps.setInt(3, qty);
            ps.setBigDecimal(4, unitCost);
            ps.setBigDecimal(5, lineTotal);
            ps.executeUpdate();
        }
    }

    private void updateProductStockAndAvgCost(Connection con, int productId, int newStock, BigDecimal newAvgCost)
            throws SQLException {
        String sql = "UPDATE products SET stock_qty = ?, avg_cost = ? WHERE product_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setBigDecimal(2, newAvgCost);
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
    }

    private void insertStockMovementPurchase(Connection con, int productId, int qtyIn,
                                             BigDecimal unitCost, long purchaseId) throws SQLException {
        String sql = """
                INSERT INTO stock_movements
                (product_id, movement_type, qty_in, qty_out, unit_cost, reference_table, reference_id, note)
                VALUES (?, 'PURCHASE', ?, 0, ?, 'PURCHASE', ?, 'Stock In')
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, qtyIn);
            ps.setBigDecimal(3, unitCost);
            ps.setLong(4, purchaseId);
            ps.executeUpdate();
        }
    }

    private void updatePurchaseTotal(Connection con, long purchaseId, BigDecimal total) throws SQLException {
        String sql = "UPDATE purchases SET total_cost = ? WHERE purchase_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, total);
            ps.setLong(2, purchaseId);
            ps.executeUpdate();
        }
    }

    // Locks product row and reads current stock & avg_cost
    private ProductStockCost lockAndGetStockCost(Connection con, int productId) throws SQLException {
        String sql = "SELECT stock_qty, avg_cost FROM products WHERE product_id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Product not found: " + productId);
                return new ProductStockCost(rs.getInt("stock_qty"), rs.getBigDecimal("avg_cost"));
            }
        }
    }

    private static class ProductStockCost {
        int stockQty;
        BigDecimal avgCost;

        ProductStockCost(int stockQty, BigDecimal avgCost) {
            this.stockQty = stockQty;
            this.avgCost = avgCost == null ? BigDecimal.ZERO : avgCost;
        }
    }
}
