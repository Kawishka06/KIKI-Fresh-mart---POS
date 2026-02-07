package lk.kiki.pos.shoppos.model;

import java.math.BigDecimal;

public class PurchaseItem {
    private int productId;
    private int qty;
    private BigDecimal unitCost;

    public PurchaseItem() {}

    public PurchaseItem(int productId, int qty, BigDecimal unitCost) {
        this.productId = productId;
        this.qty = qty;
        this.unitCost = unitCost;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
}
