package lk.kiki.pos.shoppos.test;

import lk.kiki.pos.shoppos.model.SaleItem;
import lk.kiki.pos.shoppos.service.SaleService;

import java.math.BigDecimal;
import java.util.List;

public class SaleServiceTest {
    public static void main(String[] args) {
        try {
            SaleService service = new SaleService();

            // Sell 2 units of product_id=2
            List<SaleItem> items = List.of(
                    new SaleItem(2, 2, new BigDecimal("150.00"))
            );

            long saleId = service.recordSale("CASH", items);
            System.out.println("✅ Sale recorded. sale_id=" + saleId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
