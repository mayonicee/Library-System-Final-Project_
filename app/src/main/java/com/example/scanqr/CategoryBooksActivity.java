package com.example.scanqr;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CategoryBooksActivity extends AppCompatActivity {

    private TextView txtCategoryHeader, txtEmpty;
    private RecyclerView rvCategoryBooks;

    private FirebaseFirestore db;
    private String categoryId;

    private List<Book> categoryBooks;
    private CategoryBooksAdapter categoryBooksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_books);

        categoryId = getIntent().getStringExtra("CATEGORY_ID");
        if (categoryId == null) {
            categoryId = "Unknown";
        }

        db = FirebaseFirestore.getInstance();

        txtCategoryHeader = findViewById(R.id.txtCategoryHeader);
        txtEmpty          = findViewById(R.id.txtEmpty);
        rvCategoryBooks   = findViewById(R.id.rvCategoryBooks);

        if (txtCategoryHeader != null) {
            txtCategoryHeader.setText("Category: " + categoryId);
        }

        if (rvCategoryBooks == null) {
            if (txtEmpty != null) {
                txtEmpty.setVisibility(View.VISIBLE);
                txtEmpty.setText("Layout error: rvCategoryBooks not found.");
            }
            return;
        }

        rvCategoryBooks.setLayoutManager(new LinearLayoutManager(this));
        categoryBooks = new ArrayList<>();
        categoryBooksAdapter = new CategoryBooksAdapter(categoryBooks, this);
        rvCategoryBooks.setAdapter(categoryBooksAdapter);

        loadBooksByCategory();
    }

    private void loadBooksByCategory() {
        db.collection("books")
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    try {
                        categoryBooks.clear();

                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Book b = new Book();
                            b.setId(doc.getId());
                            b.setTittle(doc.getString("tittle"));
                            b.setAuthor(doc.getString("author"));
                            b.setCoverUrl(doc.getString("coverUrl"));
                            b.setCategoryId(doc.getString("categoryId"));
                            b.setQrCodeValue(doc.getString("qrCodeValue"));
                            b.setSypnosis(doc.getString("sypnosis"));

                            // ====== HANDLE availableCopies fleksibel ======
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

                            // ====== HANDLE totalCopies fleksibel ======
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

                            categoryBooks.add(b);
                        }

                        categoryBooksAdapter.notifyDataSetChanged();

                        if (txtEmpty != null) {
                            if (categoryBooks.isEmpty()) {
                                txtEmpty.setVisibility(View.VISIBLE);
                                txtEmpty.setText("No books found in this category.");
                            } else {
                                txtEmpty.setVisibility(View.GONE);
                            }
                        }
                    } catch (Exception ex) {
                        if (txtEmpty != null) {
                            txtEmpty.setVisibility(View.VISIBLE);
                            txtEmpty.setText("Error parsing data: " + ex.getMessage());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (txtEmpty != null) {
                        txtEmpty.setVisibility(View.VISIBLE);
                        txtEmpty.setText("Failed to load books: " + e.getMessage());
                    }
                });
    }
}
