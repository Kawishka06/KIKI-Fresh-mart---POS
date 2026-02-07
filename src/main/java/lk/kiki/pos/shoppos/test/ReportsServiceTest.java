package lk.kiki.pos.shoppos.test;

import lk.kiki.pos.shoppos.model.SalesProfitReport;
import lk.kiki.pos.shoppos.service.ReportsService;

import java.time.LocalDate;
import java.time.YearMonth;

public class ReportsServiceTest {
    public static void main(String[] args) {
        try {
            ReportsService rs = new ReportsService();

            LocalDate today = LocalDate.now();
            YearMonth thisMonth = YearMonth.now();

            System.out.println("Daily Sales: " + rs.getDailySalesTotal(today));
            System.out.println("Daily Purchases: " + rs.getDailyPurchasesTotal(today));

            System.out.println("Monthly Sales: " + rs.getMonthlySalesTotal(thisMonth));
            System.out.println("Monthly Purchases: " + rs.getMonthlyPurchasesTotal(thisMonth));

            SalesProfitReport pr = rs.getMonthlyProfitReport(thisMonth);
            System.out.println("Monthly Revenue: " + pr.getRevenue());
            System.out.println("Monthly COGS: " + pr.getCogs());
            System.out.println("Monthly Profit: " + pr.getProfit());

            System.out.println("Total Stock Units: " + rs.getTotalStockUnits());
            System.out.println("Stock Value @ Cost: " + rs.getStockValueAtCost());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
