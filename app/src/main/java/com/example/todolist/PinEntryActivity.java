package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class PinEntryActivity extends AppCompatActivity {

    private StringBuilder currentPin = new StringBuilder();
    private View[] pinDots;
    private TextView tvTitle, tvSubtitle, tvError;
    private PinManager pinManager;
    private boolean isSetupMode = false;
    private String firstPin = "";
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before calling super.onCreate()
        ThemeManager.getInstance(this).initializeTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_entry);

        pinManager = new PinManager(this);
        isSetupMode = getIntent().getBooleanExtra("setup_mode", false);

        initViews();
        setupClickListeners();
        updateUI();

        // Setup modern back press handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Prevent going back from PIN screen
                if (!isSetupMode) {
                    moveTaskToBack(true);
                } else {
                    // Allow going back during setup
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        tvError = findViewById(R.id.tv_error);

        pinDots = new View[]{
            findViewById(R.id.pin_dot1),
            findViewById(R.id.pin_dot2),
            findViewById(R.id.pin_dot3),
            findViewById(R.id.pin_dot4)
        };
    }

    private void setupClickListeners() {
        // Number buttons - setup individually to ensure correct mapping
        findViewById(R.id.btn_0).setOnClickListener(v -> addDigit("0"));
        findViewById(R.id.btn_1).setOnClickListener(v -> addDigit("1"));
        findViewById(R.id.btn_2).setOnClickListener(v -> addDigit("2"));
        findViewById(R.id.btn_3).setOnClickListener(v -> addDigit("3"));
        findViewById(R.id.btn_4).setOnClickListener(v -> addDigit("4"));
        findViewById(R.id.btn_5).setOnClickListener(v -> addDigit("5"));
        findViewById(R.id.btn_6).setOnClickListener(v -> addDigit("6"));
        findViewById(R.id.btn_7).setOnClickListener(v -> addDigit("7"));
        findViewById(R.id.btn_8).setOnClickListener(v -> addDigit("8"));
        findViewById(R.id.btn_9).setOnClickListener(v -> addDigit("9"));

        // Delete button
        findViewById(R.id.btn_delete).setOnClickListener(v -> removeDigit());
    }

    private void updateUI() {
        if (isSetupMode) {
            if (firstPin.isEmpty()) {
                tvTitle.setText("Set PIN");
                tvSubtitle.setText("Create a 4-digit PIN");
            } else {
                tvTitle.setText("Confirm PIN");
                tvSubtitle.setText("Enter your PIN again");
            }
        } else {
            tvTitle.setText("Enter PIN");
            tvSubtitle.setText("Please enter your 4-digit PIN");
        }
    }

    private void addDigit(String digit) {
        if (isProcessing) return; // Prevent input while processing
        
        if (currentPin.length() < 4) {
            currentPin.append(digit);
            updatePinDisplay();
            tvError.setVisibility(View.GONE); // Clear any previous error

            if (currentPin.length() == 4) {
                isProcessing = true;
                // Add a small delay to let user see the complete PIN entry
                findViewById(R.id.btn_0).postDelayed(() -> {
                    processPinInput();
                }, 300); // 300ms delay
            }
        }
    }

    private void removeDigit() {
        if (currentPin.length() > 0) {
            currentPin.deleteCharAt(currentPin.length() - 1);
            updatePinDisplay();
            tvError.setVisibility(View.GONE);
        }
    }

    private void updatePinDisplay() {
        for (int i = 0; i < pinDots.length; i++) {
            if (i < currentPin.length()) {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_filled);
            } else {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_empty);
            }
        }
    }

    private void processPinInput() {
        String pin = currentPin.toString();
        
        // Validate PIN length
        if (pin.length() != 4) {
            showError("Please enter a 4-digit PIN.");
            resetPinInput();
            return;
        }

        try {
            if (isSetupMode) {
                if (firstPin.isEmpty()) {
                    // First PIN entry
                    firstPin = pin;
                    resetPinInput();
                    updateUI();
                } else {
                    // Confirm PIN
                    if (firstPin.equals(pin)) {
                        pinManager.setPin(pin);
                        navigateToMainActivity();
                    } else {
                        showError("PINs don't match. Try again.");
                        firstPin = "";
                        resetPinInput();
                        updateUI();
                    }
                }
            } else {
                // Verify PIN
                if (pinManager.verifyPin(pin)) {
                    navigateToMainActivity();
                } else {
                    showError("Incorrect PIN. Try again.");
                    resetPinInput();
                }
            }
        } catch (Exception e) {
            showError("An error occurred. Please try again.");
            resetPinInput();
            android.util.Log.e("PinEntryActivity", "Error processing PIN", e);
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void resetPinInput() {
        currentPin.setLength(0);
        updatePinDisplay();
        isProcessing = false;
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
