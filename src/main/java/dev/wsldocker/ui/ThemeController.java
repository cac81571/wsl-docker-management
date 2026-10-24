package dev.wsldocker.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public final class ThemeController {
    private ThemeController() {
    }

    public static void install(boolean dark) {
        applyLookAndFeel(dark);
        applyTweaks();
    }

    public static void switchTheme(boolean dark) {
        applyLookAndFeel(dark);
        applyTweaks();
        FlatLaf.updateUI();
    }

    private static void applyLookAndFeel(boolean dark) {
        try {
            UIManager.setLookAndFeel(dark ? new FlatDarculaLaf() : new FlatIntelliJLaf());
        } catch (UnsupportedLookAndFeelException e) {
            throw new IllegalStateException("Look and Feel を設定できません", e);
        }
    }

    private static void applyTweaks() {
        UIManager.put("Component.arrowType", "chevron");
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
        UIManager.put("TitlePane.unifiedBackground", Boolean.TRUE);
    }
}
