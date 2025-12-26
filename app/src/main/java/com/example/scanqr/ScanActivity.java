package com.example.scanqr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class ScanActivity extends BaseActivity {

    private static final String TAG = "ScanActivityDebug";

    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBottomBar(R.id.nav_library);
        // Launcher buat minta izin kamera
        requestCameraPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        startScanner();
                    } else {
                        Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        checkPermissionAndScan();
    }

    private void checkPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanner();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startScanner() {
        // Hanya scan QR Code
        GmsBarcodeScannerOptions options =
                new GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    // ===== DEBUG START =====
                    String bookIdRaw = barcode.getRawValue();          // nilai mentah dari QR
                    String bookId    = bookIdRaw != null
                            ? bookIdRaw.trim()                          // setelah di-trim
                            : null;


                    StringBuilder asciiBuilder = new StringBuilder();
                    if (bookIdRaw != null) {
                        for (int i = 0; i < bookIdRaw.length(); i++) {
                            char c = bookIdRaw.charAt(i);
                            asciiBuilder
                                    .append("'").append(c).append("'=")
                                    .append((int) c);
                            if (i < bookIdRaw.length() - 1) {
                                asciiBuilder.append(", ");
                            }
                        }
                    }

                    // Log lengkap ke Logcat
                    Log.d(TAG, "== QR DEBUG ==");
                    Log.d(TAG, "RAW      : \"" + bookIdRaw + "\"");
                    Log.d(TAG, "RAW LEN  : " + (bookIdRaw == null ? -1 : bookIdRaw.length()));
                    Log.d(TAG, "TRIMMED  : \"" + bookId + "\"");
                    Log.d(TAG, "TRIM LEN : " + (bookId == null ? -1 : bookId.length()));
                    Log.d(TAG, "ASCII    : " + asciiBuilder);

                    // Toast singkat di layar
                    Toast.makeText(
                            this,
                            "RAW: " + bookIdRaw + "\nTRIM: " + bookId,
                            Toast.LENGTH_LONG
                    ).show();
                    // ===== DEBUG END =====

                    if (bookId == null || bookId.isEmpty()) {
                        Toast.makeText(this, "Book ID kosong dari QR", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Pindah ke book_detail_activity (pakai yang sudah di-trim)
                    Intent intent = new Intent(ScanActivity.this, book_detail_activity.class);
                    intent.putExtra("BOOK_ID", bookId);   // KEY: "BOOK_ID"
                    startActivity(intent);

                    finish();
                })
                .addOnCanceledListener(() -> {
                    Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal scan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saat scan QR", e);
                    finish();
                });
    }
}
