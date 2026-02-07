package lk.kiki.pos.shoppos.service;

import lk.kiki.pos.shoppos.model.SalesProfitReport;
import lk.kiki.pos.shoppos.util.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.YearMonth;

public class ReportsService {

    // -------------------- Sales Totals --------------------

    public BigDecimal getDailySalesTotal(LocalDate date) throws Exception {
        String sql = """
                SELECT COALESCE(SUM(total), 0) AS daily_sales
                FROM sales
                WHERE status='COMPLETED' AND DATE(sold_at) = ?
                """;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("daily_sales");
            }
        }
    }

    public BigDecimal getMonthlySalesTotal(YearMonth month) throws Exception {
        String sql = """
                SELECT COALESCE(SUM(total), 0) AS monthly_sales
                FROM sales
                WHERE status='COMPLETED'
                  AND YEAR(sold_at) = ?
                  AND MONTH(sold_at) = ?
                """;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, month.getYear());
            ps.setInt(2, month.getMonthValue());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("monthly_sales");
            }
        }
    }

    // -------------------- Purchase Totals --------------------

    public BigDecimal getDailyPurchasesTotal(LocalDate date) throws Exception {
        String sql = """
                SELECT COALESCE(SUM(total_cost), 0) AS daily_purchases
                FROM purchases
                WHERE DATE(purchased_at) = ?
                """;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("daily_purchases");
            }
        }
    }

    public BigDecimal getMonthlyPurchasesTotal(YearMonth month) throws Exception {
        String sql = """
                SELECT COALESCE(SUM(total_cost), 0) AS monthly_purchases
                FROM purchases
                WHERE YEAR(purchased_at) = ?
                  AND MONTH(purchased_at) = ?
                """;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, month.getYear());
            ps.setInt(2, month.getMonthValue());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("monthly_purchases");
            }
        }
    }

    // -------------------- Profit / Loss (Correct COGS method) --------------------

    public SalesProfitReport getMonthlyProfitReport(YearMonth month) throws Exception {
        String sql = """
                SELECT
                  COALESCE(SUM(si.qty * si.unit_sell_price),0) AS revenue,
                  COALESCE(SUM(si.qty * si.unit_cost_at_sale),0) AS cogs,
                  COALESCE(SUM(si.qty * (si.unit_sell_price - si.unit_cost_at_sale)),0) AS profit
                FROM sale_items si
                JOIN sales s ON s.sale_id = si.sale_id
                WHERE s.status='COMPLETED'
                  AND YEAR(s.sold_at) = ?
                  AND MONTH(s.sold_at) = ?
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, month.getYear());
            ps.setInt(2, month.getMonthValue());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal revenue = rs.getBigDecimal("revenue");
                BigDecimal cogs = rs.getBigDecimal("cogs");
                BigDecimal profit = rs.getBigDecimal("profit");
                return new SalesProfitReport(revenue, cogs, profit);
            }
        }
    }

    // -------------------- Stock Count / Valuation --------------------

    public int getTotalStockUnits() throws Exception {
        String sql = "SELECT COALESCE(SUM(stock_qty), 0) AS total_units FROM products WHERE active=1";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt("total_units");
        }
    }

    public BigDecimal getStockValueAtCost() throws Exception {
        String sql = "SELECT COALESCE(SUM(stock_qty * avg_cost), 0) AS stock_value_cost FROM products WHERE active=1";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal("stock_value_cost");
        }
    }

    public BigDecimal getMonthlyLossValue(YearMonth month) throws Exception {
        String sql = """
            SELECT COALESCE(SUM(qty_out * unit_cost), 0) AS loss_value
            FROM stock_movements
            WHERE movement_type='LOSS'
              AND YEAR(moved_at)=?
              AND MONTH(moved_at)=?
            """;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, month.getYear());
            ps.setInt(2, month.getMonthValue());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("loss_value");
            }
        }
    }

    public BigDecimal getDailyLossValue(LocalDate date) throws Exception {
        String sql = """
            SELECT COALESCE(SUM(qty_out * unit_cost), 0) AS loss_value
            FROM stock_movements
            WHERE movement_type='LOSS'
              AND DATE(moved_at)=?
            """;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("loss_value");
            }
        }
    }

}
