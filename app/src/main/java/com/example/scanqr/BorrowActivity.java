package com.example.scanqr;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BorrowActivity extends AppCompatActivity {

    // Data buku dari Intent
    private String bookDocId;   // id dokumen di /books
    private String bookIdValue; // "Book_001" (qrCodeValue)
    private String bookTitle;
    private String bookAuthor;
    private String coverUrl;

    // UI
    private TextView txtBookTitle, txtBookAuthor;
    private EditText edtBorrowerName, edtBorrowerId, edtGuarantee, edtDurationDays;
    private RadioGroup rgLocationType;
    private RadioButton rbInside, rbOutside;
    private MaterialButton btnSubmit, btnCancel;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_borrow);

        // ===== AMBIL DATA DARI INTENT =====
        bookDocId   = getIntent().getStringExtra("BOOK_DOC_ID");
        bookIdValue = safe(getIntent().getStringExtra("BOOK_ID"));          // "Book_001"
        bookTitle   = safe(getIntent().getStringExtra("BOOK_TITLE"));
        bookAuthor  = safe(getIntent().getStringExtra("BOOK_AUTHOR"));
        coverUrl    = safe(getIntent().getStringExtra("BOOK_COVER_URL"));

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // ===== INIT VIEW =====
        txtBookTitle   = findViewById(R.id.txtBorrowBookTitle);
        txtBookAuthor  = findViewById(R.id.txtBorrowBookAuthor);

        edtBorrowerName = findViewById(R.id.edtBorrowerName);
        edtBorrowerId   = findViewById(R.id.edtBorrowerId);
        edtGuarantee    = findViewById(R.id.edtGuarantee);
        edtDurationDays = findViewById(R.id.edtDurationDays);

        rgLocationType  = findViewById(R.id.rgLocationType);
        rbInside        = findViewById(R.id.rbInside);
        rbOutside       = findViewById(R.id.rbOutside);

        btnSubmit = findViewById(R.id.btnSubmitBorrow);
        btnCancel = findViewById(R.id.btnCancelBorrow);

        // Tampilkan informasi buku
        txtBookTitle.setText(!TextUtils.isEmpty(bookTitle) ? bookTitle : "Unknown title");
        txtBookAuthor.setText(!TextUtils.isEmpty(bookAuthor) ? "By " + bookAuthor : "By -");

        // Default pilih INSIDE
        rbInside.setChecked(true);

        // ===== AUTO TARIK DATA USER SAAT INI =====
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            // GANTI NAMA FIELD SESUAI STRUKTUR USERS KAMU
                            String name = doc.getString("name");
                            String studentId = doc.getString("studentId"); // atau "nim"

                            if (!TextUtils.isEmpty(name)) {
                                edtBorrowerName.setText(name);
                            } else if (!TextUtils.isEmpty(user.getDisplayName())) {
                                edtBorrowerName.setText(user.getDisplayName());
                            }

                            if (!TextUtils.isEmpty(studentId)) {
                                edtBorrowerId.setText(studentId);
                            } else if (!TextUtils.isEmpty(user.getEmail())) {
                                edtBorrowerId.setText(user.getEmail());
                            }
                        } else {
                            if (!TextUtils.isEmpty(user.getDisplayName())) {
                                edtBorrowerName.setText(user.getDisplayName());
                            }
                            if (!TextUtils.isEmpty(user.getEmail())) {
                                edtBorrowerId.setText(user.getEmail());
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!TextUtils.isEmpty(user.getDisplayName())) {
                            edtBorrowerName.setText(user.getDisplayName());
                        }
                        if (!TextUtils.isEmpty(user.getEmail())) {
                            edtBorrowerId.setText(user.getEmail());
                        }
                    });
        }

        // Kalau mau di-lock:
        // edtBorrowerName.setEnabled(false);
        // edtBorrowerId.setEnabled(false);

        // Tombol Cancel
        btnCancel.setOnClickListener(v -> finish());

        // Tombol Submit
        btnSubmit.setOnClickListener(v -> submitBorrow());
    }

    @NonNull
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void submitBorrow() {
        String borrowerName = edtBorrowerName.getText().toString().trim();
        String borrowerId   = edtBorrowerId.getText().toString().trim();
        String guarantee    = edtGuarantee.getText().toString().trim();
        String durationStr  = edtDurationDays.getText().toString().trim();

        // Baca dari radio button
        int checkedId = rgLocationType.getCheckedRadioButtonId();
        String locationType;
        if (checkedId == R.id.rbInside) {
            locationType = "INSIDE";
        } else if (checkedId == R.id.rbOutside) {
            locationType = "OUTSIDE";
        } else {
            Toast.makeText(this, "Please choose location type", Toast.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(borrowerName)) {
            edtBorrowerName.setError("Required");
            edtBorrowerName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(borrowerId)) {
            edtBorrowerId.setError("Required");
            edtBorrowerId.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(guarantee)) {
            edtGuarantee.setError("Required");
            edtGuarantee.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(durationStr)) {
            edtDurationDays.setError("Required");
            edtDurationDays.requestFocus();
            return;
        }

        int durationDays;
        try {
            durationDays = Integer.parseInt(durationStr);
        } catch (NumberFormatException e) {
            edtDurationDays.setError("Must be a number");
            edtDurationDays.requestFocus();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_LONG).show();
            return;
        }

        String uid = user.getUid();

        Timestamp borrowedAt = Timestamp.now();
        // Hitung dueDate = borrowedAt + durationDays
        Calendar cal = Calendar.getInstance();
        cal.setTime(borrowedAt.toDate());
        cal.add(Calendar.DAY_OF_YEAR, durationDays);
        Timestamp dueDate = new Timestamp(cal.getTime());

        // ===== MAP KE STRUKTUR FIRESTORE =====
        Map<String, Object> data = new HashMap<>();
        data.put("bookId", !TextUtils.isEmpty(bookIdValue) ? bookIdValue : bookDocId);
        data.put("bookTittle", bookTitle);
        data.put("coverUrl", coverUrl);

        data.put("borrowedAt", borrowedAt);
        data.put("dueDate", dueDate);
        data.put("durationDays", durationDays);
        data.put("guarantee", guarantee);
        data.put("locationType", locationType);  // "INSIDE" / "OUTSIDE"
        data.put("returnedAt", null);
        data.put("status", "ON_APPROVAL");

        data.put("borrowerName", borrowerName);
        data.put("borrowerId", borrowerId);
        data.put("userUid", uid);

        db.collection("users")
                .document(uid)
                .collection("borrow")
                .add(data)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this,
                            "Borrow record saved!",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to save: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }
}
