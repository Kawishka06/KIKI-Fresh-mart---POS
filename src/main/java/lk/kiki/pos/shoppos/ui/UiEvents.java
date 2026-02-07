package lk.kiki.pos.shoppos.ui;

import java.util.ArrayList;
import java.util.List;

public class UiEvents {

    private static final List<Runnable> stockListeners = new ArrayList<>();

    public static void onStockChanged(Runnable r) {
        stockListeners.add(r);
    }

    public static void fireStockChanged() {
        for (Runnable r : stockListeners) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }
}
