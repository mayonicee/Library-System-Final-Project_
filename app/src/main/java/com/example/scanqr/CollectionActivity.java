package com.example.scanqr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CollectionActivity extends BaseActivity {

    private RecyclerView rvCollection;
    private TextView txtCollectionTitle, txtCollectionEmpty;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private final List<CollectionItem> collectionItems = new ArrayList<>();
    private CollectionAdapter collectionAdapter;

    // Status yang sama dengan di book_detail_activity
    private static final String[] COLLECTION_STATUSES = {
            "FAVORITE",
            "TO_READ",
            "READING",
            "FINISHED"
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        txtCollectionTitle = findViewById(R.id.txtCollectionTitle);
        txtCollectionEmpty = findViewById(R.id.txtCollectionEmpty);
        rvCollection       = findViewById(R.id.rvCollection);

        if (txtCollectionTitle != null) {
            txtCollectionTitle.setText("My Collection");
        }

        if (rvCollection == null) {
            Toast.makeText(this,
                    "Layout error: rvCollection not found in activity_collection.xml",
                    Toast.LENGTH_LONG).show();
            return;
        }

        rvCollection.setLayoutManager(new LinearLayoutManager(this));

        // Adapter: klik → buka detail, long-klik → ganti status
        collectionAdapter = new CollectionAdapter(
                collectionItems,
                new CollectionAdapter.OnCollectionClickListener() {
                    @Override
                    public void onClick(CollectionItem item) {
                        openBookFromCollection(item);
                    }

                    @Override
                    public void onLongClick(CollectionItem item) {
                        showStatusDialog(item);
                    }
                }
        );

        rvCollection.setAdapter(collectionAdapter);

        // bottom nav
        setupBottomBar(R.id.nav_collection);

        listenUserCollection();
    }

    private void listenUserCollection() {
        db.collection("users")
                .document(currentUser.getUid())
                .collection("collections")  // <- pakai 'collections'
                .orderBy("lastOpenedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this,
                                "Error load collection: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snapshot == null) return;

                    collectionItems.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        CollectionItem item = new CollectionItem();
                        item.setId(doc.getId());                       // doc id = bookDocId
                        item.setBookId(doc.getString("bookId"));       // docId buku di "books"
                        item.setBookTitle(doc.getString("bookTitle"));
                        item.setCoverUrl(doc.getString("coverUrl"));
                        item.setStatus(doc.getString("status"));
                        item.setLastOpenedAt(doc.getTimestamp("lastOpenedAt"));

                        collectionItems.add(item);
                    }

                    collectionAdapter.notifyDataSetChanged();

                    if (txtCollectionEmpty != null) {
                        if (collectionItems.isEmpty()) {
                            txtCollectionEmpty.setText("No books in your collection.");
                            txtCollectionEmpty.setVisibility(android.view.View.VISIBLE);
                        } else {
                            txtCollectionEmpty.setVisibility(android.view.View.GONE);
                        }
                    }
                });
    }

    // Klik biasa → buka detail buku
    private void openBookFromCollection(CollectionItem item) {
        String bookId = item.getBookId(); // documentId buku di "books"
        if (TextUtils.isEmpty(bookId)) {
            Toast.makeText(this,
                    "Book ID not found in collection item",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(CollectionActivity.this, book_detail_activity.class);
        intent.putExtra("BOOK_ID", bookId);
        startActivity(intent);
    }

    // Long-klik → pilih status baru / hapus
    private void showStatusDialog(CollectionItem item) {
        if (currentUser == null) return;

        String[] options = new String[] {
                "Mark as Favorite",
                "Mark as To Read",
                "Mark as Reading",
                "Mark as Finished",
                "Remove from collection"
        };

        new AlertDialog.Builder(this)
                .setTitle(item.getBookTitle() != null ? item.getBookTitle() : "Change status")
                .setItems(options, (dialog, which) -> {
                    if (which == 4) {
                        // Remove
                        removeFromCollection(item);
                    } else {
                        String status = COLLECTION_STATUSES[which];
                        updateCollectionStatus(item, status);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateCollectionStatus(CollectionItem item, String status) {
        String uid = currentUser.getUid();
        // doc id = id koleksi (kita set = bookDocId)
        String collectionDocId = item.getId();

        db.collection("users")
                .document(uid)
                .collection("collections")
                .document(collectionDocId)
                .update(
                        "status", status,
                        "lastOpenedAt", Timestamp.now()
                )
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Status updated to " + status,
                            Toast.LENGTH_SHORT).show();
                    // snapshotListener akan refresh list otomatis
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed update status: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private void removeFromCollection(CollectionItem item) {
        String uid = currentUser.getUid();
        String collectionDocId = item.getId();

        db.collection("users")
                .document(uid)
                .collection("collections")
                .document(collectionDocId)
                .delete()
                .addOnSuccessListener(unused -> Toast.makeText(this,
                        "Removed from collection",
                        Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed remove: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }
}
