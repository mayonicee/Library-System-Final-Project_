package com.example.scanqr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends BaseActivity {

    private Button btnManageBooks, btnManageUsers, btnReviewLoans, btnManageCategories;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        btnManageBooks      = findViewById(R.id.btnManageBooks);
        btnManageUsers      = findViewById(R.id.btnManageUsers);
        btnReviewLoans      = findViewById(R.id.btnReviewLoans);


        // pastikan cuma admin/super_admin yang boleh masuk
        checkRoleAndSetup();
        setupBottomBar(R.id.nav_profile);
    }

    private void checkRoleAndSetup() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Silakan login dulu.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this,
                                "Profil tidak ditemukan.",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String role = doc.getString("role");
                    if (role == null ||
                            !(role.equals("admin") || role.equals("super_admin"))) {
                        Toast.makeText(this,
                                "Kamu tidak punya akses admin.",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    // role valid → set onClick button
                    setupButtonListeners(role);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Gagal cek role: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void setupButtonListeners(String role) {

        // ========== MANAGE BOOKS ==========
        btnManageBooks.setOnClickListener(v -> {
            Intent i = new Intent(AdminDashboardActivity.this, ManageBooksActivity.class);
            startActivity(i);
        });

        // ========== MANAGE USERS ==========
        btnManageUsers.setOnClickListener(v -> {
            Intent u = new Intent(AdminDashboardActivity.this, ManageUserActivity.class);
            u.putExtra("ROLE", role); // kita lempar role ke activity, biar tau super_admin atau admin
            startActivity(u);
        });

        // ========== REVIEW LOANS ==========
        btnReviewLoans.setOnClickListener(v -> {
            Intent k = new Intent(AdminDashboardActivity.this, ManageBorrowsActivity.class);
            startActivity(k);
        });


    }
}
