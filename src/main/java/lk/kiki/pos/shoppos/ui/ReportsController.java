package lk.kiki.pos.shoppos.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lk.kiki.pos.shoppos.model.SalesProfitReport;
import lk.kiki.pos.shoppos.service.ReportsService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.stream.IntStream;

public class ReportsController {

    @FXML private DatePicker dpDate;

    @FXML private ComboBox<Integer> cmbYear;
    @FXML private ComboBox<Month> cmbMonth;

    @FXML private Label lblDailySales;
    @FXML private Label lblDailyPurchases;
    @FXML private Label lblDailyLoss;

    @FXML private Label lblMonthlySales;
    @FXML private Label lblMonthlyPurchases;
    @FXML private Label lblRevenue;
    @FXML private Label lblCogs;
    @FXML private Label lblProfit;
    @FXML private Label lblMonthlyLoss;

    @FXML private Label lblStockUnits;
    @FXML private Label lblStockValue;

    private final ReportsService reportsService = new ReportsService();

    @FXML
    public void initialize() {
        dpDate.setValue(LocalDate.now());

        int currentYear = LocalDate.now().getYear();
        cmbYear.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(currentYear - 3, currentYear + 1).boxed().toList()
        ));
        cmbYear.setValue(currentYear);

        cmbMonth.setItems(FXCollections.observableArrayList(Month.values()));
        cmbMonth.setValue(LocalDate.now().getMonth());

        // Load default values
        onLoadDaily();
        onLoadMonthly();
        onRefreshStock();
    }

    @FXML
    private void onLoadDaily() {
        try {
            LocalDate date = dpDate.getValue();
            if (date == null) date = LocalDate.now();

            lblDailySales.setText(fmt(reportsService.getDailySalesTotal(date)));
            lblDailyPurchases.setText(fmt(reportsService.getDailyPurchasesTotal(date)));
            lblDailyLoss.setText(fmt(reportsService.getDailyLossValue(date)));

        } catch (Exception e) {
            showError("Daily report failed", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onLoadMonthly() {
        try {
            Integer year = cmbYear.getValue();
            Month month = cmbMonth.getValue();
            if (year == null || month == null) return;

            YearMonth ym = YearMonth.of(year, month);

            lblMonthlySales.setText(fmt(reportsService.getMonthlySalesTotal(ym)));
            lblMonthlyPurchases.setText(fmt(reportsService.getMonthlyPurchasesTotal(ym)));

            SalesProfitReport pr = reportsService.getMonthlyProfitReport(ym);
            lblRevenue.setText(fmt(pr.getRevenue()));
            lblCogs.setText(fmt(pr.getCogs()));
            lblProfit.setText(fmt(pr.getProfit()));

            lblMonthlyLoss.setText(fmt(reportsService.getMonthlyLossValue(ym)));

        } catch (Exception e) {
            showError("Monthly report failed", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onRefreshStock() {
        try {
            lblStockUnits.setText(String.valueOf(reportsService.getTotalStockUnits()));
            lblStockValue.setText(fmt(reportsService.getStockValueAtCost()));
        } catch (Exception e) {
            showError("Stock refresh failed", e.getMessage());
            e.printStackTrace();
        }
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "0.00";
        return v.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
