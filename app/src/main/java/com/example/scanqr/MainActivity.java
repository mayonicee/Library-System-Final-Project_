package com.example.scanqr;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends BaseActivity {

    // Dashboard views
    private TextView txtGreeting;
    private EditText edtSearch;
    private RecyclerView rvTopBooks, rvGenres, rvRecentBorrow;
    private ImageView imgAvatarHome;

    // Firestore
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // ========= DATA & ADAPTER "AVAILABLE FOR YOU" =========
    private List<Book> topAvailableBooks;
    private TopBooksAdapter topBooksAdapter;

    // ========= SEMUA BUKU (UNTUK SEARCH) =========
    private List<Book> allBooks;

    // ========= DATA & ADAPTER GENRE/CATEGORY =========
    private List<Genre> genres;
    private GenreAdapter genreAdapter;

    // ========= DATA & ADAPTER RECENT BORROW =========
    private List<BorrowRecord> recentBorrows;
    private RecentBorrowAdapter recentBorrowAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ================= CEK LOGIN DULU =================
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Intent loginIntent = new Intent(MainActivity.this, Login.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        // ================= INISIALISASI VIEW DASHBOARD =================
        imgAvatarHome  = findViewById(R.id.imgAvatarHome);
        txtGreeting    = findViewById(R.id.txtGreeting);
        edtSearch      = findViewById(R.id.edtSearch);
        rvTopBooks     = findViewById(R.id.rvTopBooks);
        rvGenres       = findViewById(R.id.rvGenres);
        rvRecentBorrow = findViewById(R.id.rvRecentBorrow);

        rvTopBooks.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvGenres.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRecentBorrow.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // ========= LIST DATA =========
        allBooks          = new ArrayList<>();
        topAvailableBooks = new ArrayList<>();
        genres            = new ArrayList<>();
        recentBorrows     = new ArrayList<>();

        // ========= ADAPTER TOP AVAILABLE BOOKS =========
        topBooksAdapter = new TopBooksAdapter(topAvailableBooks, this, book -> {
            // klik item → buka detail
            openBookDetail(book);
        });
        rvTopBooks.setAdapter(topBooksAdapter);

        // ========= ADAPTER GENRE (CATEGORY) =========
        genreAdapter = new GenreAdapter(genres, this);
        rvGenres.setAdapter(genreAdapter);

        // ========= ADAPTER RECENT BORROW =========
        recentBorrowAdapter = new RecentBorrowAdapter(recentBorrows, record -> {
            String bookDocId = record.getBookId();
            if (!TextUtils.isEmpty(bookDocId)) {
                Intent i = new Intent(MainActivity.this, book_detail_activity.class);
                i.putExtra("BOOK_ID", bookDocId);
                startActivity(i);
            } else {
                Toast.makeText(this, "Book not found for this record", Toast.LENGTH_SHORT).show();
            }
        });
        rvRecentBorrow.setAdapter(recentBorrowAdapter);

        // Header user
        loadUserHeader();

        // Dengerin data books (available + categories + allBooks)
        listenTopAvailableBooksAndCategories();

        // Dengerin recent borrow user
        listenRecentBorrows();

        // Setup Search
        setupSearchBox();

        // Bottom nav
        setupBottomBar(R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (currentUser == null) {
            currentUser = FirebaseAuth.getInstance().getCurrentUser();
        }

        if (currentUser != null) {
            loadUserHeader();   // refresh tiap balik ke MainActivity
        }
    }

    // =====================================================
    //                 HEADER USER / AVATAR
    // =====================================================
    private void loadUserHeader() {
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String photoUrl = doc.getString("photoUrl");

                    if (name == null || name.trim().isEmpty()) {
                        name = fallbackName();
                    }
                    txtGreeting.setText("Hello, " + name + "!");

                    if (photoUrl != null) {
                        photoUrl = photoUrl.trim();
                        if (photoUrl.startsWith("http://")) {
                            photoUrl = photoUrl.replace("http://", "https://");
                        }
                    }

                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(this)
                                .load(photoUrl)
                                .centerCrop()
                                .placeholder(R.mipmap.ic_launcher_round)
                                .error(R.mipmap.ic_launcher_round)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .into(imgAvatarHome);
                    } else {
                        imgAvatarHome.setImageResource(R.mipmap.ic_launcher_round);
                    }
                })
                .addOnFailureListener(e -> {
                    String name = fallbackName();
                    txtGreeting.setText("Hello, " + name + "!");
                    imgAvatarHome.setImageResource(R.mipmap.ic_launcher_round);
                });
    }

    private String fallbackName() {
        String name = currentUser.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = currentUser.getEmail();
        }
        if (name == null || name.isEmpty()) {
            name = "Reader";
        }
        return name;
    }

    // =====================================================
    //  LISTEN "BOOKS" → isi allBooks, topAvailableBooks & genres
    // =====================================================
    private void listenTopAvailableBooksAndCategories() {
        db.collection("books")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        return;
                    }
                    if (snapshot == null) return;

                    allBooks.clear();
                    topAvailableBooks.clear();
                    Set<String> categorySet = new LinkedHashSet<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            Book b = new Book();
                            b.setId(doc.getId());
                            b.setTittle(doc.getString("tittle"));
                            b.setAuthor(doc.getString("author"));
                            b.setCoverUrl(doc.getString("coverUrl"));
                            b.setCategoryId(doc.getString("categoryId"));
                            b.setQrCodeValue(doc.getString("qrCodeValue"));
                            b.setSypnosis(doc.getString("sypnosis"));

                            String catId = doc.getString("categoryId");
                            if (!TextUtils.isEmpty(catId)) {
                                categorySet.add(catId.trim());
                            }

                            long available = 0;
                            Object availObj = doc.get("availableCopies");
                            if (availObj instanceof Number) {
                                available = ((Number) availObj).longValue();
                            } else if (availObj instanceof String) {
                                try {
                                    available = Long.parseLong((String) availObj);
                                } catch (NumberFormatException ignored) {
                                    available = 0;
                                }
                            }
                            b.setAvailableCopies(available);

                            long total = 0;
                            Object totalObj = doc.get("totalCopies");
                            if (totalObj instanceof Number) {
                                total = ((Number) totalObj).longValue();
                            } else if (totalObj instanceof String) {
                                try {
                                    total = Long.parseLong((String) totalObj);
                                } catch (NumberFormatException ignored) {
                                    total = 0;
                                }
                            }
                            b.setTotalCopies(total);

                            // masukkan ke allBooks (untuk search)
                            allBooks.add(b);

                            // hanya yg stok > 0 tampil di "Available For You"
                            if (available > 0) {
                                topAvailableBooks.add(b);
                            }

                        } catch (Exception ex) {
                            // skip dokumen yang error
                        }
                    }

                    topBooksAdapter.notifyDataSetChanged();

                    // === BANGUN LIST GENRE DARI CATEGORY UNIQUE ===
                    genres.clear();
                    for (String cat : categorySet) {
                        Genre g = new Genre();
                        g.setName(cat);
                        genres.add(g);
                    }
                    genreAdapter.notifyDataSetChanged();
                });
    }

    // =====================================================
    //            LISTEN & TAMPILKAN RECENT BORROW USER
    // =====================================================
    private void listenRecentBorrows() {
        if (currentUser == null) return;

        db.collection("users")
                .document(currentUser.getUid())
                .collection("borrow")
                .orderBy("borrowedAt", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        return;
                    }
                    if (snapshot == null) return;

                    recentBorrows.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            BorrowRecord r = new BorrowRecord();
                            r.setDocRef(doc.getReference());
                            r.setBookId(getStringSafe(doc, "bookId"));
                            r.setBookTittle(getStringSafe(doc, "bookTittle"));
                            r.setCoverUrl(getStringSafe(doc, "coverUrl"));
                            r.setStatus(getStringSafe(doc, "status"));
                            r.setLocationType(getStringSafe(doc, "locationType"));
                            r.setGuarantee(getStringSafe(doc, "guarantee"));
                            r.setBorrowerName(getStringSafe(doc, "borrowerName"));
                            r.setBorrowerId(getStringSafe(doc, "borrowerId"));
                            r.setUserUid(getStringSafe(doc, "userUid"));
                            r.setBorrowedAt(doc.getTimestamp("borrowedAt"));
                            r.setDueDate(doc.getTimestamp("dueDate"));
                            r.setReturnedAt(doc.getTimestamp("returnedAt"));

                            Object durObj = doc.get("durationDays");
                            long durationDays = 0;
                            if (durObj instanceof Number) {
                                durationDays = ((Number) durObj).longValue();
                            }
                            r.setDurationDays(durationDays);

                            recentBorrows.add(r);

                        } catch (Exception ex) {
                            // skip yg error
                        }
                    }

                    recentBorrowAdapter.notifyDataSetChanged();
                });
    }

    private String getStringSafe(DocumentSnapshot doc, String key) {
        String v = doc.getString(key);
        return v == null ? "" : v.trim();
    }

    // =====================================================
    //                    SIMPLE SEARCH
    // =====================================================
    private void setupSearchBox() {
        // 1) Jalan kalau user tekan tombol Search / Enter (optional, bisa kamu keep)
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnterKey = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;

            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || isEnterKey) {

                String query = edtSearch.getText().toString().trim();
                performSearch(query);
                hideKeyboard();
                return true;
            }
            return false;
        });

        // 2) LIVE SEARCH: tiap kali teks berubah, hasil ikut berubah
        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nggak dipakai
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();

                // Biar nggak terlalu berat, bisa kasih minimal 2 huruf baru search
                // Kalau mau 1 huruf juga boleh, tinggal hapus if ini
                if (query.length() == 0) {
                    // kosong → balik ke daftar "Available For You"
                    performSearch("");
                } else if (query.length() >= 2) {
                    performSearch(query);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // nggak dipakai
            }
        });
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) {
            // Kalau kosong → balik ke "Available For You" (stok > 0)
            topAvailableBooks.clear();
            for (Book b : allBooks) {
                if (b.getAvailableCopies() > 0) {
                    topAvailableBooks.add(b);
                }
            }
            topBooksAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Showing available books", Toast.LENGTH_SHORT).show();
            return;
        }

        String qLower = query.toLowerCase(Locale.ROOT);

        List<Book> filtered = new ArrayList<>();
        for (Book b : allBooks) {
            String title  = b.getTittle() != null ? b.getTittle().toLowerCase(Locale.ROOT) : "";
            String author = b.getAuthor() != null ? b.getAuthor().toLowerCase(Locale.ROOT) : "";

            if (title.contains(qLower) || author.contains(qLower)) {
                filtered.add(b);
            }
        }

        topAvailableBooks.clear();
        topAvailableBooks.addAll(filtered);
        topBooksAdapter.notifyDataSetChanged();

        Toast.makeText(this,
                "Found " + filtered.size() + " result(s) for \"" + query + "\"",
                Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && edtSearch != null) {
            imm.hideSoftInputFromWindow(edtSearch.getWindowToken(), 0);
        }
    }

    // =====================================================
    //           BUKA DETAIL KETIKA BUKU DIKLIK
    // =====================================================
    private void openBookDetail(@NonNull Book book) {
        String docId = book.getId();
        if (docId == null || docId.trim().isEmpty()) {
            Toast.makeText(this, "Book ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(MainActivity.this, book_detail_activity.class);
        intent.putExtra("BOOK_ID", docId);
        startActivity(intent);
    }
}
