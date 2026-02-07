package lk.kiki.pos.shoppos.test1;

import lk.kiki.pos.shoppos.dao.ProductDao;
import lk.kiki.pos.shoppos.model.Product;

import java.math.BigDecimal;

public class ProductDAOTest {
    public static void main(String[] args) {
        try {
            ProductDao dao = new ProductDao();

            Product p = new Product();
            p.setSku("P100");
            p.setName("Test Biscuit");
            p.setCategory("Snacks");
            p.setSellingPrice(new BigDecimal("150.00"));

            int newId = dao.addProduct(p);
            System.out.println("✅ Inserted product_id = " + newId);

            System.out.println("🔎 Search results:");
            dao.search("Biscuit").forEach(x ->
                    System.out.println(x.getProductid() + " | " + x.getSku() + " | " + x.getName() + " | " + x.getSellingPrice())
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

