package lk.kiki.pos.shoppos.model;

import java.math.BigDecimal;

public class SaleItem {
    private int productId;
    private int qty;
    private BigDecimal unitSellPrice; // selling price used for THIS sale

    public SaleItem() {}

    public SaleItem(int productId, int qty, BigDecimal unitSellPrice) {
        this.productId = productId;
        this.qty = qty;
        this.unitSellPrice = unitSellPrice;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public BigDecimal getUnitSellPrice() { return unitSellPrice; }
    public void setUnitSellPrice(BigDecimal unitSellPrice) { this.unitSellPrice = unitSellPrice; }
}
