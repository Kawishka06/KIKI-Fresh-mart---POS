package lk.kiki.pos.shoppos.dao;

import lk.kiki.pos.shoppos.model.Product;
import lk.kiki.pos.shoppos.util.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    // 1) Add a new product (stock starts at 0, avg_cost starts at 0.00)
    public int addProduct(Product p) throws Exception {
        String sql = """
                INSERT INTO products (sku, name, category, selling_price, avg_cost, stock_qty, active)
                VALUES (?, ?, ?, ?, 0.00, 0, 1)
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getSku());
            ps.setString(2, p.getName());
            ps.setString(3, p.getCategory());
            ps.setBigDecimal(4, p.getSellingPrice());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating product failed, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1); // new product_id
                }
            }
            throw new SQLException("Creating product failed, no ID obtained.");
        }
    }

    // 2) Update selling price
    public boolean updateSellingPrice(int productId, BigDecimal newPrice) throws Exception {
        String sql = "UPDATE products SET selling_price = ? WHERE product_id = ?";

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBigDecimal(1, newPrice);
            ps.setInt(2, productId);

            return ps.executeUpdate() == 1;
        }
    }

    // 3) Search products by name or sku (fast for 500 items)
    public List<Product> search(String keyword) throws Exception {
        String sql = """
                SELECT product_id, sku, name, category, selling_price, avg_cost, stock_qty, active
                FROM products
                WHERE active = 1 AND (name LIKE ? OR sku LIKE ?)
                ORDER BY name
                LIMIT 200
                """;

        String like = "%" + keyword + "%";
        List<Product> list = new ArrayList<>();

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    // 4) List all active products (use for initial load)
    public List<Product> findAllActive() throws Exception {
        String sql = """
                SELECT product_id, sku, name, category, selling_price, avg_cost, stock_qty, active
                FROM products
                WHERE active = 1
                ORDER BY name
                """;

        List<Product> list = new ArrayList<>();

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // Helper: convert ResultSet row -> Product object
    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductid(rs.getInt("product_id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setCategory(rs.getString("category"));
        p.setSellingPrice(rs.getBigDecimal("selling_price"));
        p.setAvgCost(rs.getBigDecimal("avg_cost"));
        p.setStockQty(rs.getInt("stock_qty"));
        p.setActive(rs.getInt("active") == 1);
        return p;
    }

    public int countLowStock(int threshold) throws Exception {
        String sql = "SELECT COUNT(*) FROM products WHERE active=1 AND stock_qty < ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<Product> findLowStock(int threshold) throws Exception {
        String sql = """
        SELECT product_id, sku, name, category, selling_price, stock_qty, avg_cost
        FROM products
        WHERE active = 1 AND stock_qty < ?
        ORDER BY stock_qty ASC
    """;

        try (var con = DB.getConnection();
             var ps = con.prepareStatement(sql)) {

            ps.setInt(1, threshold);

            try (var rs = ps.executeQuery()) {
                var list = new java.util.ArrayList<Product>();
                while (rs.next()) {
                    Product p = new Product();
                    p.setProductid(rs.getInt("product_id"));
                    p.setSku(rs.getString("sku"));
                    p.setName(rs.getString("name"));
                    p.setCategory(rs.getString("category"));
                    p.setSellingPrice(rs.getBigDecimal("selling_price"));
                    p.setStockQty(rs.getInt("stock_qty"));
                    p.setAvgCost(rs.getBigDecimal("avg_cost"));
                    list.add(p);
                }
                return list;
            }
        }
    }


}
