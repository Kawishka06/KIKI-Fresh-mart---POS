package lk.kiki.pos.shoppos.test;

import lk.kiki.pos.shoppos.model.PurchaseItem;
import lk.kiki.pos.shoppos.service.PurchaseService;

import java.math.BigDecimal;
import java.util.List;

public class PurchaseServiceTest {
    public static void main(String[] args) {
        try {
            PurchaseService service = new PurchaseService();

            // Example: Buy 5 units of product_id=2 at cost 100.00
            // Change productId to your actual product_id
            List<PurchaseItem> items = List.of(
                    new PurchaseItem(2, 5, new BigDecimal("100.00"))
            );

            long purchaseId = service.recordPurchase("Test Supplier", items);
            System.out.println("✅ Purchase recorded. purchase_id=" + purchaseId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
