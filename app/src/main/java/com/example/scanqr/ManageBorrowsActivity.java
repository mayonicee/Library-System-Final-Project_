package com.example.scanqr;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageBorrowsActivity extends AppCompatActivity {

    private static final String TAG = "ManageBorrows";

    // UI
    private RecyclerView rvBorrowList;
    private TextView txtTitleReviewLoans, txtEmptyState, txtSubtitleReviewLoans;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    // Data
    private final List<BorrowRecord> borrowRecords = new ArrayList<>();
    private ManageBorrowsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_borrows);

        // Init Firebase
        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Logged in as uid = " + currentUser.getUid());

        // Cek role dulu → hanya admin/super_admin yang boleh masuk
        checkAdminAndInit();
    }

    // ===========================================================
    //          CEK ROLE ADMIN DI /users/{uid}
    // ===========================================================
    private void checkAdminAndInit() {
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this,
                                "User profile not found",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String role = doc.getString("role");
                    if (role == null) role = "";
                    Log.d(TAG, "User role = " + role);

                    if (!(role.equals("admin") || role.equals("super_admin"))) {
                        Toast.makeText(this,
                                "You are not admin",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    initViewsAndAdapter();
                    listenAllBorrows();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check admin role", e);
                    Toast.makeText(this,
                            "Failed to check role: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    // ===========================================================
    //          INIT VIEW + ADAPTER
    // ===========================================================
    private void initViewsAndAdapter() {
        txtTitleReviewLoans    = findViewById(R.id.txtTitleReviewLoans);
        txtSubtitleReviewLoans = findViewById(R.id.txtSubtitleReviewLoans);
        txtEmptyState          = findViewById(R.id.txtEmptyState);
        rvBorrowList           = findViewById(R.id.rvBorrowList);

        if (txtTitleReviewLoans != null) txtTitleReviewLoans.setText("Review Loans");
        if (txtSubtitleReviewLoans != null) {
            txtSubtitleReviewLoans.setText("List peminjaman aktif dari semua user");
        }

        rvBorrowList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManageBorrowsAdapter(
                borrowRecords,
                new ManageBorrowsAdapter.OnBorrowActionListener() {
                    @Override
                    public void onApprove(BorrowRecord record) {
                        // FIX: approve harus jadi BORROWED / APPROVED
                        updateStatus(record, "BORROWED", false);
                    }

                    @Override
                    public void onReject(BorrowRecord record) {
                        updateStatus(record, "REJECTED", false);
                    }

                    @Override
                    public void onMarkReturned(BorrowRecord record) {
                        updateStatus(record, "RETURNED", true);
                    }
                }
        );
        rvBorrowList.setAdapter(adapter);
    }

    // ===========================================================
    //       LISTEN SEMUA peminjaman dari collectionGroup("borrow")
    //       HANYA TAMPILKAN YG BELUM FINAL (RETURNED/REJECTED)
    // ===========================================================
    private void listenAllBorrows() {
        db.collectionGroup("borrow")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "listenAllBorrows error: ", e);
                        Toast.makeText(this,
                                "Error load borrows: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) {
                        Log.w(TAG, "listenAllBorrows: snapshot null");
                        return;
                    }

                    dumpSnapshotInfo(snap);

                    borrowRecords.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        try {
                            String status = getStringSafe(doc, "status");
                            if (status.isEmpty()) status = "PENDING";

                            String statusUpper = status.toUpperCase(Locale.ROOT);

                            // FINAL STATUS → tidak ditampilkan di panel admin
                            boolean isFinal = statusUpper.startsWith("RETURNED")
                                    || statusUpper.startsWith("REJECTED");
                            if (isFinal) continue;

                            BorrowRecord r = new BorrowRecord();
                            r.setDocRef(doc.getReference());

                            r.setBookId(getStringSafe(doc, "bookId"));
                            r.setBookTittle(getStringSafe(doc, "bookTittle"));
                            r.setCoverUrl(getStringSafe(doc, "coverUrl"));

                            r.setBorrowerName(getStringSafe(doc, "borrowerName"));
                            r.setBorrowerId(getStringSafe(doc, "borrowerId"));
                            r.setUserUid(getStringSafe(doc, "userUid"));

                            r.setStatus(status);
                            r.setGuarantee(getStringSafe(doc, "guarantee"));
                            r.setLocationType(getStringSafe(doc, "locationType"));

                            Timestamp borrowedAt = doc.getTimestamp("borrowedAt");
                            Timestamp dueDate    = doc.getTimestamp("dueDate");
                            Timestamp returnedAt = doc.getTimestamp("returnedAt");

                            r.setBorrowedAt(borrowedAt);
                            r.setDueDate(dueDate);
                            r.setReturnedAt(returnedAt);

                            Object durObj = doc.get("durationDays");
                            long durationDays = 0;
                            if (durObj instanceof Number) {
                                durationDays = ((Number) durObj).longValue();
                            }
                            r.setDurationDays(durationDays);

                            borrowRecords.add(r);

                        } catch (Exception ex) {
                            Log.e(TAG, "Error parsing borrow doc "
                                    + doc.getReference().getPath(), ex);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (txtEmptyState != null) {
                        if (borrowRecords.isEmpty()) {
                            txtEmptyState.setVisibility(View.VISIBLE);
                            txtEmptyState.setText("No active borrow requests.");
                        } else {
                            txtEmptyState.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void dumpSnapshotInfo(QuerySnapshot snap) {
        Log.d(TAG, "listenAllBorrows: total documents = " + snap.size());
        for (DocumentSnapshot doc : snap.getDocuments()) {
            Log.d(
                    TAG,
                    "doc path: " + doc.getReference().getPath()
                            + " status=" + doc.getString("status")
                            + " borrower=" + doc.getString("borrowerName")
            );
        }
    }

    private String getStringSafe(DocumentSnapshot doc, String key) {
        String v = doc.getString(key);
        return v == null ? "" : v.trim();
    }

    // ===========================================================
    //          UPDATE STATUS (APPROVE / REJECT / RETURNED)
    // ===========================================================
    private void updateStatus(BorrowRecord record,
                              String newStatus,
                              boolean setReturnedAt) {
        if (record.getDocRef() == null) {
            Toast.makeText(this, "DocumentRef null", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "updateStatus: " + record.getDocRef().getPath()
                + " -> " + newStatus);

        if (setReturnedAt) {
            record.getDocRef()
                    .update("status", newStatus,
                            "returnedAt", Timestamp.now())
                    .addOnSuccessListener(unused -> Toast.makeText(this,
                            "Status updated to " + newStatus,
                            Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "updateStatus (returned) failed: ", e);
                        Toast.makeText(this,
                                "Failed to update: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } else {
            record.getDocRef()
                    .update("status", newStatus)
                    .addOnSuccessListener(unused -> Toast.makeText(this,
                            "Status updated to " + newStatus,
                            Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "updateStatus failed: ", e);
                        Toast.makeText(this,
                                "Failed to update: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        }
    }
}
