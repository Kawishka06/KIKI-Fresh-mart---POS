package lk.kiki.pos.shoppos.model;

import java.math.BigDecimal;

public class CartItem {
    private int productId;
    private String name;
    private int qty;
    private BigDecimal unitPrice;

    public CartItem(int productId, String name, int qty, BigDecimal unitPrice) {
        this.productId = productId;
        this.name = name;
        this.qty = qty;
        this.unitPrice = unitPrice;
    }

    public int getProductId() { return productId; }
    public String getName() { return name; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(qty));
    }
}
