package lk.kiki.pos.shoppos.service;

import lk.kiki.pos.shoppos.util.DB;

import java.math.BigDecimal;
import java.sql.*;

public class StockAdjustmentService {

    /**
     * Reduces stock due to loss (damaged/expired/theft/other).
     * - Decreases products.stock_qty
     * - Inserts stock_movements row with movement_type='LOSS'
     * - Stores unit_cost (avg_cost at the time)
     */
    public void recordLoss(int productid, int qty, String reason) throws Exception {
        if (productid <= 0) throw new IllegalArgumentException("Invalid productId");
        if (qty <= 0) throw new IllegalArgumentException("Qty must be > 0");
        if (reason == null || reason.isBlank()) reason = "OTHER";

        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);

            try {
                // Lock product row
                String lockSql = "SELECT stock_qty, avg_cost FROM products WHERE product_id=? FOR UPDATE";
                int currentStock;
                BigDecimal avgCost;

                try (PreparedStatement ps = con.prepareStatement(lockSql)) {
                    ps.setInt(1, productid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Product not found: " + productid);
                        currentStock = rs.getInt("stock_qty");
                        avgCost = rs.getBigDecimal("avg_cost");
                        if (avgCost == null) avgCost = BigDecimal.ZERO;
                    }
                }

                if (currentStock < qty) {
                    throw new IllegalStateException("Not enough stock (available=" + currentStock + ", requested=" + qty + ")");
                }

                // Reduce stock
                String updateSql = "UPDATE products SET stock_qty = stock_qty - ? WHERE product_id=?";
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setInt(1, qty);
                    ps.setInt(2, productid);
                    ps.executeUpdate();
                }

                // Stock movement (LOSS)
                String moveSql = """
                    INSERT INTO stock_movements
                    (product_id, movement_type, qty_in, qty_out, unit_cost, reference_table, reference_id, note)
                    VALUES (?, 'LOSS', 0, ?, ?, 'ADJUSTMENT', NULL, ?)
                    """;

                try (PreparedStatement ps = con.prepareStatement(moveSql)) {
                    ps.setInt(1, productid);
                    ps.setInt(2, qty);
                    ps.setBigDecimal(3, avgCost);
                    ps.setString(4, reason);
                    ps.executeUpdate();
                }

                con.commit();

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}
