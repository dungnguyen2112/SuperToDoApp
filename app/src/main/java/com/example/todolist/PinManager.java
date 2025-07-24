package com.example.todolist;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.util.Base64;

public class PinManager {
    private static final String PREFS_NAME = "pin_prefs";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_SET = "pin_set";
    private static final String KEY_ALIAS = "TodoListPinKey";

    private Context context;
    private SharedPreferences prefs;

    public PinManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isPinSet() {
        return prefs.getBoolean(KEY_PIN_SET, false);
    }

    public void setPin(String pin) {
        try {
            String hashedPin = hashPin(pin);
            prefs.edit()
                .putString(KEY_PIN_HASH, hashedPin)
                .putBoolean(KEY_PIN_SET, true)
                .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean verifyPin(String pin) {
        try {
            if (pin == null || pin.length() != 4) {
                android.util.Log.d("PinManager", "Invalid PIN format");
                return false;
            }
            
            String storedHash = prefs.getString(KEY_PIN_HASH, "").trim();
            if (storedHash.isEmpty()) {
                android.util.Log.d("PinManager", "No stored PIN hash found");
                return false;
            }
            
            String hashedPin = hashPin(pin).trim();
            boolean isValid = hashedPin.equals(storedHash);
            android.util.Log.d("PinManager", "PIN verification result: " + isValid);
            return isValid;
        } catch (Exception e) {
            android.util.Log.e("PinManager", "Error verifying PIN", e);
            return false;
        }
    }

    public void clearPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_PIN_SET, false)
            .apply();
    }

    private String hashPin(String pin) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(hash, Base64.DEFAULT);
    }
}
