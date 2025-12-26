package com.example.scanqr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class book_detail_activity extends BaseActivity {

    private static final String TAG = "BookDetailRealtime";

    // List status yang bisa dipilih
    private static final String[] COLLECTION_STATUSES = {
            "FAVORITE",
            "TO_READ",
            "READING",
            "FINISHED"
    };

    // UI detail
    private ImageView imgCover, btnBack;
    private TextView txtTitle, txtAuthor, txtYear, txtIsbn, txtCategory, txtAvailable;
    private TextView txtSynopsis, txtStatusMessage, txtBookId;
    private MaterialButton btnBorrow;
    private ImageButton btnBookmark;   // icon bookmark

    // Similar books
    private RecyclerView rvSimilarBooks;
    private TextView txtSimilarHeader;
    private SimilarBooksAdapter similarAdapter;
    private final List<SimilarBookItem> similarBooks = new ArrayList<>();

    // Firestore
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // book id saat ini (document id di /books)
    private String currentDocId = null;

    // untuk collection (bookmark) -> pakai docId /books
    private String currentCollectionBookId = null;
    private boolean isBookmarked = false;

    // listener realtime dokumen buku
    private ListenerRegistration bookListener;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        // ===== INIT VIEW =====
        imgCover         = findViewById(R.id.imgCover);
        btnBack          = findViewById(R.id.btnBack);
        txtTitle         = findViewById(R.id.Title);
        txtAuthor        = findViewById(R.id.Author);
        txtYear          = findViewById(R.id.Year);
        txtIsbn          = findViewById(R.id.Isbn);
        txtCategory      = findViewById(R.id.Category);
        txtAvailable     = findViewById(R.id.Available);
        txtSynopsis      = findViewById(R.id.Synopsis);
        txtStatusMessage = findViewById(R.id.StatusMessage);
        txtBookId        = findViewById(R.id.BookId);
        btnBorrow        = findViewById(R.id.btnBorrow);
        btnBookmark      = findViewById(R.id.btnBookmarkIcon);  // dari XML

        // Similar section
        rvSimilarBooks   = findViewById(R.id.rvSimilarBooks);
        txtSimilarHeader = findViewById(R.id.txtSimilarTitle);

        rvSimilarBooks.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        similarAdapter = new SimilarBooksAdapter(similarBooks, item -> {
            // Buka detail buku yang lain
            Intent i = new Intent(book_detail_activity.this, book_detail_activity.class);
            i.putExtra("BOOK_ID", item.id);
            startActivity(i);
        });
        rvSimilarBooks.setAdapter(similarAdapter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        btnBack.setOnClickListener(v -> onBackPressed());

        // ===== AMBIL DOC ID DARI INTENT (BOOK_ID) =====
        String docId = getIntent().getStringExtra("BOOK_ID");
        if (docId != null) docId = docId.trim();

        // Fallback buat testing manual
        if (TextUtils.isEmpty(docId)) {
            docId = "mvQSAZypiu6SWdT4nSE2"; // contoh
        }
        currentDocId = docId;

        txtBookId.setText("Doc ID: " + docId);
        txtStatusMessage.setText("");

        // 🔹 Bottom nav
        setupBottomBar(R.id.nav_collection);

        // 🔴 Realtime listener ke dokumen /books/{docId}
        listenBookRealtime(docId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookListener != null) {
            bookListener.remove();
            bookListener = null;
        }
    }

    // ===========================================================
    //               LISTEN DOKUMEN BUKU REALTIME
    // ===========================================================
    private void listenBookRealtime(String docId) {
        bookListener = db.collection("books")
                .document(docId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        Log.e(TAG, "listenBookRealtime error", e);
                        txtStatusMessage.setText("Failed to load book data.");
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (doc == null || !doc.exists()) {
                        Log.w(TAG, "Book doc not found: " + docId);
                        txtStatusMessage.setText("Book not found.");
                        clearDetailFields();
                        return;
                    }

                    bindFromSnapshot(doc);
                });
    }

    private void bindFromSnapshot(DocumentSnapshot doc) {
        // Ambil field dari Firestore
        String title      = safeString(doc.getString("tittle"));
        String author     = safeString(doc.getString("author"));
        String isbn       = safeString(doc.getString("isbn"));
        String categoryId = safeString(doc.getString("categoryId"));
        String synopsis   = safeString(doc.getString("sypnosis"));
        String coverUrl   = safeString(doc.getString("coverUrl"));
        String qrCodeVal  = safeString(doc.getString("qrCodeValue")); // kalau masih dipakai

        // 🔹 PAKAI helper fleksibel: bisa Number / String / null
        Long yearLong  = getLongFlexible(doc, "year");
        Long availLong = getLongFlexible(doc, "availableCopies");
        Long totalLong = getLongFlexible(doc, "totalCopies");

        String yearStr      = (yearLong  != null) ? String.valueOf(yearLong) : "";
        String availableStr = (availLong != null) ? String.valueOf(availLong) : "";
        String totalStr     = (totalLong != null) ? String.valueOf(totalLong) : "";

        Log.d(TAG, "bindFromSnapshot: "
                + "title=" + title
                + ", available=" + availableStr
                + ", total=" + totalStr);

        bindToViews(
                title,
                author,
                isbn,
                categoryId,
                synopsis,
                coverUrl,
                yearStr,
                availableStr,
                totalStr,
                qrCodeVal
        );

        // Setelah detail terisi, kita load similar books (pakai categoryId)
        loadSimilarBooks(categoryId);
    }

    // String aman
    private String safeString(String v) {
        return v == null ? "" : v.trim();
    }

    // 🔧 Helper: baca Number / String jadi Long (hindari crash)
    private Long getLongFlexible(DocumentSnapshot doc, String key) {
        Object v = doc.get(key);
        if (v == null) return null;

        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (v instanceof String) {
            try {
                return Long.parseLong(((String) v).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // ===========================================================
    //                    BIND KE UI (+ COVER + BORROW + BOOKMARK)
    // ===========================================================
    private void bindToViews(
            String title,
            String author,
            String isbn,
            String categoryId,
            String synopsis,
            String coverUrl,
            String yearStr,
            String availableStr,
            String totalStr,
            String qrCodeValue   // "Book_001" (opsional)
    ) {
        // ==== TEXT ====
        txtTitle.setText(!TextUtils.isEmpty(title) ? title : "-");
        txtAuthor.setText(!TextUtils.isEmpty(author) ? "By " + author : "By -");
        txtYear.setText(!TextUtils.isEmpty(yearStr) ? yearStr : "-");
        txtCategory.setText(!TextUtils.isEmpty(categoryId) ? categoryId : "Not found");
        txtIsbn.setText(!TextUtils.isEmpty(isbn) ? "ISBN: " + isbn : "ISBN: Not found");
        txtSynopsis.setText(!TextUtils.isEmpty(synopsis) ? synopsis : "Not found");

        if (!TextUtils.isEmpty(availableStr) && !TextUtils.isEmpty(totalStr)) {
            txtAvailable.setText(
                    availableStr + " of " + totalStr + " copies available"
            );
        } else if (!TextUtils.isEmpty(availableStr)) {
            txtAvailable.setText("Available copies: " + availableStr);
        } else {
            txtAvailable.setText("Not found");
        }

        // Disable tombol borrow kalau stok 0
        int availableInt = 0;
        try {
            if (!TextUtils.isEmpty(availableStr)) {
                availableInt = Integer.parseInt(availableStr);
            }
        } catch (NumberFormatException ignore) { }

        if (availableInt <= 0) {
            btnBorrow.setEnabled(false);
            btnBorrow.setAlpha(0.5f);
        } else {
            btnBorrow.setEnabled(true);
            btnBorrow.setAlpha(1.0f);
        }

        // ====== COVER URL CLEAN + DEBUG ======
        String tmpCover = coverUrl == null ? "" : coverUrl.trim();
        if (tmpCover.contains("ibb.co.com")) {
            tmpCover = tmpCover.replace("ibb.co.com", "ibb.co");
        }
        final String finalCoverUrl = tmpCover;

        Log.d(TAG, "coverUrl from Firestore = \"" + coverUrl + "\"");
        Log.d(TAG, "finalCoverUrl           = \"" + finalCoverUrl + "\"");

        if (!TextUtils.isEmpty(finalCoverUrl)) {
            Glide.with(this)
                    .load(finalCoverUrl)
                    .centerCrop()
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e,
                                                    Object model,
                                                    Target<Drawable> target,
                                                    boolean isFirstResource) {
                            Log.e(TAG, "Glide load FAILED: " + finalCoverUrl, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource,
                                                       Object model,
                                                       Target<Drawable> target,
                                                       DataSource dataSource,
                                                       boolean isFirstResource) {
                            Log.d(TAG, "Glide cover loaded OK from: " + finalCoverUrl);
                            return false;
                        }
                    })
                    .into(imgCover);
        } else {
            imgCover.setImageResource(R.mipmap.ic_launcher);
        }

        // ================== KIRIM DATA KE BORROW ACTIVITY ==================
        final String safeTitle      = title;
        final String safeAuthor     = author;
        final String safeIsbn       = isbn;
        final String safeCategoryId = categoryId;

        btnBorrow.setOnClickListener(v -> {
            Intent intent = new Intent(book_detail_activity.this, BorrowActivity.class);
            // doc id di koleksi /books
            intent.putExtra("BOOK_DOC_ID", currentDocId);
            // id buku utk riwayat kalau masih dipakai
            intent.putExtra("BOOK_ID", qrCodeValue);
            intent.putExtra("BOOK_TITLE", safeTitle);
            intent.putExtra("BOOK_AUTHOR", safeAuthor);
            intent.putExtra("BOOK_ISBN", safeIsbn);
            intent.putExtra("BOOK_CATEGORY_ID", safeCategoryId);
            intent.putExtra("BOOK_COVER_URL", finalCoverUrl);
            startActivity(intent);
        });

        // ================== BOOKMARK / COLLECTION ==================
        currentCollectionBookId = currentDocId;
        setupBookmarkButton(currentCollectionBookId, title, finalCoverUrl);
    }

    private void setupBookmarkButton(String bookDocId, String bookTitle, String coverUrl) {
        if (btnBookmark == null) return;

        if (currentUser == null) {
            btnBookmark.setEnabled(false);
            btnBookmark.setAlpha(0.3f);
            return;
        }

        if (TextUtils.isEmpty(bookDocId)) {
            btnBookmark.setEnabled(false);
            btnBookmark.setAlpha(0.3f);
            return;
        }

        String uid = currentUser.getUid();

        // Cek apakah sudah ada di collection (users/{uid}/collections/{docIdBuku})
        db.collection("users")
                .document(uid)
                .collection("collections")
                .document(bookDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    isBookmarked = doc.exists();
                    refreshBookmarkButton();
                })
                .addOnFailureListener(e -> Log.e(TAG, "check bookmark failed", e));

        btnBookmark.setOnClickListener(v -> {
            if (isBookmarked) {
                // UNBOOKMARK → delete doc
                db.collection("users")
                        .document(uid)
                        .collection("collections")
                        .document(bookDocId)
                        .delete()
                        .addOnSuccessListener(unused -> {
                            isBookmarked = false;
                            refreshBookmarkButton();
                            Toast.makeText(this,
                                    "Removed from collection",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            } else {
                // ADD BOOKMARK → pilih status dulu
                showStatusDialog(uid, bookDocId, bookTitle, coverUrl);
            }
        });
    }

    private void showStatusDialog(String uid,
                                  String bookDocId,
                                  String bookTitle,
                                  String coverUrl) {

        // Tampilkan pilihan: FAVORITE, TO_READ, READING, FINISHED
        new AlertDialog.Builder(this)
                .setTitle("Save to collection as")
                .setItems(new CharSequence[]{
                        "Favorite",
                        "To Read",
                        "Reading",
                        "Finished"
                }, (dialog, which) -> {
                    String chosenStatus = COLLECTION_STATUSES[which]; // ambil kode aslinya
                    saveBookmark(uid, bookDocId, bookTitle, coverUrl, chosenStatus);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveBookmark(String uid,
                              String bookDocId,
                              String bookTitle,
                              String coverUrl,
                              String status) {

        Map<String, Object> data = new HashMap<>();
        data.put("bookId", bookDocId); // documentId buku /books
        data.put("bookTitle", bookTitle != null ? bookTitle : "");
        data.put("coverUrl", coverUrl != null ? coverUrl : "");
        data.put("lastOpenedAt", Timestamp.now());
        data.put("status", status);  // FAVORITE / TO_READ / READING / FINISHED

        db.collection("users")
                .document(uid)
                .collection("collections")
                .document(bookDocId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    isBookmarked = true;
                    refreshBookmarkButton();
                    Toast.makeText(this,
                            "Added to collection (" + status + ")",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private void refreshBookmarkButton() {
        if (btnBookmark == null) return;
        if (isBookmarked) {
            btnBookmark.setAlpha(1f);
        } else {
            btnBookmark.setAlpha(0.6f);
        }
    }

    private void clearDetailFields() {
        txtTitle.setText("-");
        txtAuthor.setText("By -");
        txtYear.setText("-");
        txtIsbn.setText("ISBN: Not found");
        txtCategory.setText("Not found");
        txtAvailable.setText("Not found");
        txtSynopsis.setText("Not found");
    }

    // ===========================================================
    //                    LOAD SIMILAR BOOKS
    // ===========================================================
    private void loadSimilarBooks(String categoryId) {
        if (TextUtils.isEmpty(categoryId)) {
            txtSimilarHeader.setText("Similar books (no category)");
            similarBooks.clear();
            similarAdapter.notifyDataSetChanged();
            return;
        }

        txtSimilarHeader.setText("Similar books");

        db.collection("books")
                .whereEqualTo("categoryId", categoryId)
                .limit(10)
                .get()
                .addOnSuccessListener(query -> {
                    similarBooks.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        // jangan masukin buku yang lagi dibuka
                        if (doc.getId().equals(currentDocId)) continue;

                        SimilarBookItem item = new SimilarBookItem();
                        item.id       = doc.getId();
                        item.title    = doc.getString("tittle");
                        item.coverUrl = doc.getString("coverUrl");
                        similarBooks.add(item);
                    }
                    similarAdapter.notifyDataSetChanged();

                    if (similarBooks.isEmpty()) {
                        txtSimilarHeader.setText("No similar books found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadSimilarBooks error", e);
                    Toast.makeText(this,
                            "Failed load similar books: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ==================== MODEL + ADAPTER SIMILAR ====================

    static class SimilarBookItem {
        String id;
        String title;
        String coverUrl;
    }

    interface OnSimilarClickListener {
        void onClick(SimilarBookItem item);
    }

    static class SimilarBooksAdapter extends RecyclerView.Adapter<SimilarBooksAdapter.SimilarVH> {

        private final List<SimilarBookItem> items;
        private final OnSimilarClickListener listener;

        SimilarBooksAdapter(List<SimilarBookItem> items, OnSimilarClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public SimilarVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_similar_book, parent, false);
            return new SimilarVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SimilarVH holder, int position) {
            holder.bind(items.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class SimilarVH extends RecyclerView.ViewHolder {
            ImageView imgCover;
            TextView txtTitle;

            SimilarVH(@NonNull android.view.View itemView) {
                super(itemView);
                imgCover = itemView.findViewById(R.id.imgSimilarCover);
                txtTitle = itemView.findViewById(R.id.txtSimilarTitle);
            }

            void bind(SimilarBookItem item, OnSimilarClickListener listener) {
                txtTitle.setText(
                        !TextUtils.isEmpty(item.title) ? item.title : "(no title)"
                );

                if (!TextUtils.isEmpty(item.coverUrl)) {
                    Glide.with(itemView.getContext())
                            .load(item.coverUrl)
                            .centerCrop()
                            .placeholder(R.mipmap.ic_launcher)
                            .into(imgCover);
                } else {
                    imgCover.setImageResource(R.mipmap.ic_launcher);
                }

                itemView.setOnClickListener(v -> listener.onClick(item));
            }
        }
    }
}
