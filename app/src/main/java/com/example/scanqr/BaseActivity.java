package com.example.scanqr;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public abstract class BaseActivity extends AppCompatActivity {

    protected void setupBottomBar(int selectedMenuId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        FloatingActionButton fabScan   = findViewById(R.id.fabScan);

        // ================= BOTTOM NAV =================
        if (bottomNav != null) {
            // tandai tab yang aktif sekarang
            bottomNav.setSelectedItemId(selectedMenuId);

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                // Kalau user tap tab yang lagi aktif → jangan apa2
                if (id == selectedMenuId) {
                    return true;
                }

                Class<?> target = null;

                if (id == R.id.nav_home) {
                    target = MainActivity.class;
                } else if (id == R.id.nav_library) {

                    startActivity(new Intent(this, LibraryActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_collection) {
                    // nanti bisa diganti ke CollectionActivity kalau sudah di buat sama maya
                    target = CollectionActivity.class;
                } else if (id == R.id.nav_profile) {
                    target = profile.class;
                }

                if (target != null && this.getClass() != target) {
                    Intent i = new Intent(this, target);
                    startActivity(i);
                    // Transisi tanpa animasi biar kerasa kayak 1 halaman
                    overridePendingTransition(0, 0);
                    // Jangan Di otak atik ya guys yang ini tadi error dan errornya ngeselin
                }

                return true;
            });
        }

        // ================= FAB SCAN =================
        if (fabScan != null) {
            fabScan.setOnClickListener(v -> {
                // Biar kalau lagi di ScanActivity gak buka lagi
                if (!(this instanceof ScanActivity)) {
                    Intent i = new Intent(this, ScanActivity.class);
                    startActivity(i);
                    overridePendingTransition(0, 0);
                }
            });
        }
    }
}
