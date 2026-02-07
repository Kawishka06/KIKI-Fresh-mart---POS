package lk.kiki.pos.shoppos.model;

import java.math.BigDecimal;

public class PurchaseCartItem {
    private int productId;
    private String name;
    private int qty;
    private BigDecimal unitCost;

    public PurchaseCartItem(int productId, String name, int qty, BigDecimal unitCost) {
        this.productId = productId;
        this.name = name;
        this.qty = qty;
        this.unitCost = unitCost;
    }

    public int getProductId() { return productId; }
    public String getName() { return name; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getLineTotal() {
        return unitCost.multiply(BigDecimal.valueOf(qty));
    }
}
