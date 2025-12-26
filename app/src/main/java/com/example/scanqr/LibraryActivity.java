package com.example.scanqr;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends BaseActivity {

    private TextView txtLibraryTitle, txtEmptyLibrary;
    private RecyclerView rvGenres, rvBooks;

    private FirebaseFirestore db;

    private final List<String> genreList = new ArrayList<>();
    private GenreChipAdapter genreAdapter;

    private final List<Book> bookList = new ArrayList<>();
    private LibraryBookAdapter bookAdapter;

    private String selectedCategoryId = null; // categoryId yang sedang aktif

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        db = FirebaseFirestore.getInstance();

        txtLibraryTitle = findViewById(R.id.txtLibraryTitle);
        txtEmptyLibrary = findViewById(R.id.txtEmptyLibrary);
        rvGenres        = findViewById(R.id.rvLibraryGenres);
        rvBooks         = findViewById(R.id.rvLibraryBooks);

        if (txtLibraryTitle != null) {
            txtLibraryTitle.setText("Library");
        }

        // Genres: horizontal
        rvGenres.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        genreAdapter = new GenreChipAdapter(genreList, genreName -> {
            selectedCategoryId = genreName.equals("All") ? null : genreName;
            loadBooksForSelectedCategory();
        });
        rvGenres.setAdapter(genreAdapter);

        // Books: horizontal
        rvBooks.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        bookAdapter = new LibraryBookAdapter(bookList, book -> {
            // klik buku → buka detail
            String docId = book.getId();
            if (TextUtils.isEmpty(docId)) {
                Toast.makeText(this, "Book document not found", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(LibraryActivity.this, book_detail_activity.class);
            intent.putExtra("BOOK_ID", docId);
            startActivity(intent);
        });
        rvBooks.setAdapter(bookAdapter);

        // Bottom nav: set tab library active
        setupBottomBar(R.id.nav_library);

        // Load genres dulu, setelah itu load books
        loadGenres();
    }

    // ================== LOAD GENRES ==================
    // ================== LOAD GENRES (dari books) ==================
    private void loadGenres() {
        genreList.clear();
        genreList.add("All"); // default All

        db.collection("books")
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String cat = doc.getString("categoryId");
                        if (!TextUtils.isEmpty(cat)) {
                            cat = cat.trim();
                            if (!genreList.contains(cat)) {
                                genreList.add(cat);
                            }
                        }
                    }

                    genreAdapter.notifyDataSetChanged();

                    // Setelah genre ada, load semua buku (All)
                    selectedCategoryId = null;
                    loadBooksForSelectedCategory();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed load categories from books: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    // tetap coba load buku all
                    selectedCategoryId = null;
                    loadBooksForSelectedCategory();
                });
    }

    // ================== LOAD BOOKS BY CATEGORY ==================
    private void loadBooksForSelectedCategory() {
        // kalau selectedCategoryId null → semua buku
        if (selectedCategoryId == null) {
            db.collection("books")
                    .get()
                    .addOnSuccessListener(query -> {
                        fillBooksFromQuery(query.getDocuments());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Failed load books: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } else {
            db.collection("books")
                    .whereEqualTo("categoryId", selectedCategoryId)
                    .get()
                    .addOnSuccessListener(query -> {
                        fillBooksFromQuery(query.getDocuments());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Failed load books: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void fillBooksFromQuery(List<DocumentSnapshot> docs) {
        bookList.clear();

        for (DocumentSnapshot doc : docs) {
            try {
                Book b = new Book();
                b.setId(doc.getId());
                b.setTittle(doc.getString("tittle"));
                b.setAuthor(doc.getString("author"));
                b.setCoverUrl(doc.getString("coverUrl"));
                b.setCategoryId(doc.getString("categoryId"));
                b.setQrCodeValue(doc.getString("qrCodeValue"));
                b.setSypnosis(doc.getString("sypnosis"));

                // optional buat available/total
                long available = 0;
                Object availObj = doc.get("availableCopies");
                if (availObj instanceof Number) {
                    available = ((Number) availObj).longValue();
                }
                b.setAvailableCopies(available);

                long total = 0;
                Object totalObj = doc.get("totalCopies");
                if (totalObj instanceof Number) {
                    total = ((Number) totalObj).longValue();
                }
                b.setTotalCopies(total);

                bookList.add(b);
            } catch (Exception ignore) { }
        }

        bookAdapter.notifyDataSetChanged();

        if (bookList.isEmpty()) {
            txtEmptyLibrary.setText("No books found.");
            txtEmptyLibrary.setVisibility(android.view.View.VISIBLE);
        } else {
            txtEmptyLibrary.setVisibility(android.view.View.GONE);
        }
    }

    // ================== ADAPTER GENRE (CHIP SIMPLE) ==================
    static class GenreChipAdapter extends RecyclerView.Adapter<GenreChipAdapter.GenreVH> {

        public interface OnGenreClickListener {
            void onClick(String genreName);
        }

        private final List<String> data;
        private final OnGenreClickListener listener;
        private int selectedPos = 0;

        GenreChipAdapter(List<String> data, OnGenreClickListener listener) {
            this.data = data;
            this.listener = listener;
        }

        @NonNull
        @Override
        public GenreVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
            ));
            tv.setPadding(32, 8, 32, 8);
            tv.setTextSize(14);
            tv.setBackgroundResource(R.drawable.bg_genre_chip); // bikin shape sederhana
            tv.setTextColor(0xFF333333);
            tv.setAllCaps(false);

            RecyclerView.LayoutParams lp =
                    (RecyclerView.LayoutParams) tv.getLayoutParams();
            lp.setMargins(8, 0, 8, 0);
            tv.setLayoutParams(lp);

            return new GenreVH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull GenreVH holder, int position) {
            String name = data.get(position);
            holder.bind(name, position == selectedPos, v -> {
                int old = selectedPos;
                selectedPos = holder.getAdapterPosition();
                notifyItemChanged(old);
                notifyItemChanged(selectedPos);

                if (listener != null) listener.onClick(name);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class GenreVH extends RecyclerView.ViewHolder {
            private final TextView tv;

            GenreVH(@NonNull android.view.View itemView) {
                super(itemView);
                tv = (TextView) itemView;
            }

            void bind(String name, boolean selected, android.view.View.OnClickListener clickListener) {
                tv.setText(name);
                if (selected) {
                    tv.setAlpha(1f);
                } else {
                    tv.setAlpha(0.6f);
                }
                tv.setOnClickListener(clickListener);
            }
        }
    }

    // ================== ADAPTER BOOKS (HORIZONTAL) ==================
    static class LibraryBookAdapter extends RecyclerView.Adapter<LibraryBookAdapter.BookVH> {

        public interface OnBookClickListener {
            void onClick(Book book);
        }

        private final List<Book> data;
        private final OnBookClickListener listener;

        LibraryBookAdapter(List<Book> data, OnBookClickListener listener) {
            this.data = data;
            this.listener = listener;
        }

        @NonNull
        @Override
        public BookVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_similar_book, parent, false);
            return new BookVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BookVH holder, int position) {
            holder.bind(data.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class BookVH extends RecyclerView.ViewHolder {
            ImageView imgCover;
            TextView txtTitle;

            BookVH(@NonNull android.view.View itemView) {
                super(itemView);
                imgCover = itemView.findViewById(R.id.imgSimilarCover);
                txtTitle = itemView.findViewById(R.id.txtSimilarTitle);
            }

            void bind(Book b, OnBookClickListener listener) {
                String title = b.getTittle() != null ? b.getTittle() : "(No title)";
                txtTitle.setText(title);

                String url = b.getCoverUrl();
                if (url != null && url.startsWith("http://")) {
                    url = url.replace("http://", "https://");
                }

                Glide.with(itemView.getContext())
                        .load(url)
                        .centerCrop()
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(imgCover);

                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onClick(b);
                });
            }
        }
    }
}
