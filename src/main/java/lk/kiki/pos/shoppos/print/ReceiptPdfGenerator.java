package lk.kiki.pos.shoppos.print;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Desktop;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceiptPdfGenerator {

    public record ReceiptLine(String name, int qty, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public static Path generateReceipt(long saleId, String shopName, List<ReceiptLine> lines, BigDecimal total) throws Exception {
        Files.createDirectories(Path.of("receipts"));

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path out = Path.of("receipts", "SALE_" + saleId + "_" + ts + ".pdf");

        // Receipt sizing (80mm style)
        float width = 226;
        float topMargin = 20;
        float bottomMargin = 20;

        float lineHeight = 14f;
        float headerBlockLines = 8;
        float footerBlockLines = 6;

        float height = topMargin
                + (headerBlockLines * lineHeight)
                + (lines.size() * lineHeight)
                + (footerBlockLines * lineHeight)
                + bottomMargin;

        if (height < 400) height = 400;

        PDRectangle pageSize = new PDRectangle(width, height);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float x = 12;
                float y = height - topMargin;

                // Title
                drawLine(cs, x, y, PDType1Font.HELVETICA_BOLD, 14, shopName);
                y -= (lineHeight + 4);

                // Meta
                drawLine(cs, x, y, PDType1Font.HELVETICA, 10, "Sale ID: " + saleId);
                y -= lineHeight;

                drawLine(cs, x, y, PDType1Font.HELVETICA, 10,
                        "Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                y -= lineHeight;

                drawLine(cs, x, y, PDType1Font.HELVETICA, 10, "----------------------------------------");
                y -= lineHeight;

                // Header
                drawLine(cs, x, y, PDType1Font.HELVETICA_BOLD, 10,
                        String.format("%-18s %3s %7s %7s", "Item", "Qty", "Price", "Total"));
                y -= lineHeight;

                drawLine(cs, x, y, PDType1Font.HELVETICA, 10, "----------------------------------------");
                y -= lineHeight;

                // Lines
                for (var l : lines) {
                    String name = l.name().length() > 18 ? l.name().substring(0, 18) : l.name();
                    String row = String.format("%-18s %3d %7s %7s",
                            name,
                            l.qty(),
                            fmt(l.unitPrice()),
                            fmt(l.lineTotal())
                    );

                    drawLine(cs, x, y, PDType1Font.HELVETICA, 10, row);
                    y -= lineHeight;
                }

                // Footer
                drawLine(cs, x, y, PDType1Font.HELVETICA, 10, "----------------------------------------");
                y -= (lineHeight + 2);

                drawLine(cs, x, y, PDType1Font.HELVETICA_BOLD, 12, "TOTAL: " + fmt(total));
                y -= (lineHeight + 2);

                drawLine(cs, x, y, PDType1Font.HELVETICA, 10, "Payment: CASH");
                y -= lineHeight;

                drawLine(cs, x, y, PDType1Font.HELVETICA, 10, "Thank you!");
            }

            doc.save(out.toFile());
        }

        return out;
    }

    // ✅ helper method (must be OUTSIDE generateReceipt)
    private static void drawLine(PDPageContentStream cs, float x, float y, PDFont font, int fontSize, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    public static void openFile(Path file) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file.toFile());
        } catch (Exception ignored) {}
    }

    private static String fmt(BigDecimal v) {
        if (v == null) return "0.00";
        return v.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }
}
