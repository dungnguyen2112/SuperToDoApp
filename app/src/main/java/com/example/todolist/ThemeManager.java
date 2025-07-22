package com.example.todolist;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String THEME_KEY = "current_theme";

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;

    private static ThemeManager instance;
    private SharedPreferences sharedPreferences;

    private ThemeManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setTheme(int theme) {
        sharedPreferences.edit().putInt(THEME_KEY, theme).apply();
        applyTheme(theme);
    }

    public int getCurrentTheme() {
        return sharedPreferences.getInt(THEME_KEY, THEME_SYSTEM);
    }

    public int getCurrentThemeIndex() {
        int theme = getCurrentTheme();
        switch (theme) {
            case THEME_LIGHT:
                return 2; // Light theme index in dialog
            case THEME_DARK:
                return 1; // Dark theme index in dialog
            case THEME_SYSTEM:
            default:
                return 0; // System default index in dialog
        }
    }

    public void applyThemeByIndex(int themeIndex) {
        int theme;
        switch (themeIndex) {
            case 1:
                theme = THEME_DARK;
                break;
            case 2:
                theme = THEME_LIGHT;
                break;
            case 0:
            default:
                theme = THEME_SYSTEM;
                break;
        }
        setTheme(theme);
    }

    public void applyTheme(int theme) {
        switch (theme) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public void initializeTheme() {
        applyTheme(getCurrentTheme());
    }

    public String getThemeName(int theme) {
        switch (theme) {
            case THEME_LIGHT:
                return "Light";
            case THEME_DARK:
                return "Dark";
            case THEME_SYSTEM:
            default:
                return "System Default";
        }
    }
}
