package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1000; // 1 second
    private PinManager pinManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before calling super.onCreate()
        ThemeManager.getInstance(this).initializeTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        pinManager = new PinManager(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (pinManager.isPinSet()) {
                // PIN is set, show PIN entry screen
                Intent intent = new Intent(SplashActivity.this, PinEntryActivity.class);
                startActivity(intent);
            } else {
                // No PIN set, show PIN setup screen
                Intent intent = new Intent(SplashActivity.this, PinEntryActivity.class);
                intent.putExtra("setup_mode", true);
                startActivity(intent);
            }
            finish();
        }, SPLASH_DELAY);
    }
}
