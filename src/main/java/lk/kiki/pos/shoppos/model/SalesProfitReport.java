package lk.kiki.pos.shoppos.model;

import java.math.BigDecimal;

public class SalesProfitReport {
    private BigDecimal revenue;
    private BigDecimal cogs;
    private BigDecimal profit;

    public SalesProfitReport(BigDecimal revenue, BigDecimal cogs, BigDecimal profit) {
        this.revenue = revenue;
        this.cogs = cogs;
        this.profit = profit;
    }

    public BigDecimal getRevenue() { return revenue; }
    public BigDecimal getCogs() { return cogs; }
    public BigDecimal getProfit() { return profit; }
}
