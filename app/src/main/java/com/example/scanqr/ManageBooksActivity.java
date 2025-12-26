package com.example.scanqr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageBooksActivity extends AppCompatActivity {

    private RecyclerView rvBooks;
    private FloatingActionButton fabAddBook;
    private TextView txtEmptyBooks, txtStatusBooks;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private final List<BookItem> books = new ArrayList<>();
    private BooksAdapter adapter;

    // ====== pick cover dari gallery di dialog ======
    private static final int REQ_READ_IMAGES = 201;

    // referensi view dialog
    private ImageView dialogCoverPreview;
    private Uri selectedCoverUri;          // cover yang dipilih (belum upload)
    private String existingCoverUrlCache;  // cover lama saat edit

    private final ActivityResultLauncher<Intent> pickCoverLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                selectedCoverUri = uri;

                                // preview lokal saja
                                if (dialogCoverPreview != null) {
                                    dialogCoverPreview.setImageURI(uri);
                                }
                            }
                        }
                    }
            );

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_books);

        rvBooks        = findViewById(R.id.rvBooksAdmin);
        fabAddBook     = findViewById(R.id.fabAddBook);
        txtEmptyBooks  = findViewById(R.id.txtEmptyBooks);
        txtStatusBooks = findViewById(R.id.txtStatusBooks);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // optional guard: pastikan login (biar storage write allowed)
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            Toast.makeText(this, "Harus login dulu", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        rvBooks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BooksAdapter(
                this,
                books,
                new BooksAdapter.BookListener() {
                    @Override
                    public void onEdit(BookItem item) {
                        showBookDialog(item);
                    }

                    @Override
                    public void onDelete(BookItem item) {
                        confirmDelete(item);
                    }
                }
        );
        rvBooks.setAdapter(adapter);

        fabAddBook.setOnClickListener(v -> showBookDialog(null));

        loadBooks();
    }

    // ==================== LOAD BOOKS ====================

    private void loadBooks() {
        txtStatusBooks.setText("Loading books...");
        books.clear();
        adapter.notifyDataSetChanged();
        txtEmptyBooks.setVisibility(View.GONE);

        db.collection("books")
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        BookItem item = new BookItem();
                        item.id          = doc.getId();
                        item.tittle      = doc.getString("tittle");
                        item.author      = doc.getString("author");
                        item.isbn        = doc.getString("isbn");
                        item.categoryId  = doc.getString("categoryId");
                        item.sypnosis    = doc.getString("sypnosis");
                        item.coverUrl    = doc.getString("coverUrl");

                        item.year = getIntField(doc, "year");
                        item.totalCopies = getIntField(doc, "totalCopies");
                        item.availableCopies = getIntField(doc, "availableCopies");

                        books.add(item);
                    }

                    adapter.notifyDataSetChanged();
                    txtStatusBooks.setText("");
                    txtEmptyBooks.setVisibility(books.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    txtStatusBooks.setText("Failed load books: " + e.getMessage());
                    Toast.makeText(this,
                            "Failed load books: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private int parseIntSafe(String s) {
        if (TextUtils.isEmpty(s)) return 0;
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private int getIntField(QueryDocumentSnapshot doc, String fieldName) {
        Object value = doc.get(fieldName);
        if (value == null) return 0;

        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Double) return ((Double) value).intValue();
        if (value instanceof String) return parseIntSafe((String) value);
        return 0;
    }

    // ==================== PICK COVER (permission + gallery) ====================

    private void startPickCoverWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQ_READ_IMAGES);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQ_READ_IMAGES);
                return;
            }
        }
        openGalleryForCover();
    }

    private void openGalleryForCover() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickCoverLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_IMAGES) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGalleryForCover();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==================== UPLOAD COVER (ONLY WHEN SAVE) ====================

    private void uploadCoverThenSaveBook(
            boolean isEdit,
            String docIdIfEdit,
            Map<String, Object> data,
            AlertDialog dialog,
            Button btnSave
    ) {
        // tidak pilih cover baru
        if (selectedCoverUri == null) {
            if (isEdit && !TextUtils.isEmpty(existingCoverUrlCache)) {
                data.put("coverUrl", existingCoverUrlCache);
            }
            saveBookFirestore(isEdit, docIdIfEdit, data, dialog, btnSave);
            return;
        }

        btnSave.setEnabled(false);
        Toast.makeText(this, "Uploading cover...", Toast.LENGTH_SHORT).show();

        String fileName = "covers/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(selectedCoverUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String url = downloadUri.toString();
                    data.put("coverUrl", url);

                    // reset
                    selectedCoverUri = null;

                    saveBookFirestore(isEdit, docIdIfEdit, data, dialog, btnSave);
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this,
                            "Upload cover gagal: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveBookFirestore(
            boolean isEdit,
            String docIdIfEdit,
            Map<String, Object> data,
            AlertDialog dialog,
            Button btnSave
    ) {
        if (isEdit) {
            db.collection("books").document(docIdIfEdit)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Book updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadBooks();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this,
                                "Failed update book: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } else {
            db.collection("books")
                    .add(data)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Book added", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadBooks();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this,
                                "Failed add book: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        }
    }

    // ==================== DIALOG TAMBAH / EDIT ====================

    private void showBookDialog(BookItem existing) {
        boolean isEdit = (existing != null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Edit Book" : "Add Book");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_book_admin, null, false);
        builder.setView(view);

        EditText edtTitle         = view.findViewById(R.id.edtBookTitle);
        EditText edtAuthor        = view.findViewById(R.id.edtBookAuthor);
        EditText edtYear          = view.findViewById(R.id.edtBookYear);
        EditText edtIsbn          = view.findViewById(R.id.edtBookIsbn);
        EditText edtCategoryId    = view.findViewById(R.id.edtBookCategoryId);
        EditText edtSynopsis      = view.findViewById(R.id.edtBookSynopsis);
        EditText edtTotalCopies   = view.findViewById(R.id.edtBookTotalCopies);
        EditText edtAvailableCopy = view.findViewById(R.id.edtBookAvailableCopies);

        ImageView imgCoverPreview = view.findViewById(R.id.imgBookCoverPreview);
        Button btnPickCover       = view.findViewById(R.id.btnPickCover);

        dialogCoverPreview = imgCoverPreview;
        selectedCoverUri = null;

        existingCoverUrlCache = isEdit ? (existing.coverUrl == null ? "" : existing.coverUrl) : "";

        if (isEdit) {
            edtTitle.setText(existing.tittle);
            edtAuthor.setText(existing.author);
            edtYear.setText(existing.year > 0 ? String.valueOf(existing.year) : "");
            edtIsbn.setText(existing.isbn);
            edtCategoryId.setText(existing.categoryId);
            edtSynopsis.setText(existing.sypnosis);
            edtTotalCopies.setText(existing.totalCopies > 0 ? String.valueOf(existing.totalCopies) : "");
            edtAvailableCopy.setText(existing.availableCopies > 0 ? String.valueOf(existing.availableCopies) : "");

            if (!TextUtils.isEmpty(existing.coverUrl)) {
                Glide.with(this)
                        .load(existing.coverUrl)
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(imgCoverPreview);
            } else {
                imgCoverPreview.setImageResource(R.mipmap.ic_launcher);
            }
        } else {
            imgCoverPreview.setImageResource(R.mipmap.ic_launcher);
        }

        btnPickCover.setOnClickListener(v -> {
            dialogCoverPreview = imgCoverPreview;
            startPickCoverWithPermission();
        });

        builder.setPositiveButton(isEdit ? "Save" : "Add", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> {
            Button btnSave = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {
                String title   = edtTitle.getText().toString().trim();
                String author  = edtAuthor.getText().toString().trim();
                String yearStr = edtYear.getText().toString().trim();
                String isbn    = edtIsbn.getText().toString().trim();
                String catId   = edtCategoryId.getText().toString().trim();
                String syn     = edtSynopsis.getText().toString().trim();
                String totalS  = edtTotalCopies.getText().toString().trim();
                String availS  = edtAvailableCopy.getText().toString().trim();

                if (TextUtils.isEmpty(title)) {
                    edtTitle.setError("Title required");
                    return;
                }

                int year            = parseIntSafe(yearStr);
                int totalCopies     = parseIntSafe(totalS);
                int availableCopies = parseIntSafe(availS);

                if (!isEdit && availableCopies == 0 && totalCopies > 0) {
                    availableCopies = totalCopies;
                }

                if (totalCopies < 0) {
                    edtTotalCopies.setError("Invalid total copies");
                    return;
                }
                if (availableCopies < 0) {
                    edtAvailableCopy.setError("Invalid available copies");
                    return;
                }
                if (totalCopies > 0 && availableCopies > totalCopies) {
                    edtAvailableCopy.setError("Available cannot exceed total");
                    return;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("tittle", title);
                data.put("author", author);
                data.put("isbn", isbn);
                data.put("categoryId", catId);
                data.put("sypnosis", syn);

                if (year > 0) data.put("year", year);
                data.put("totalCopies", totalCopies);
                data.put("availableCopies", availableCopies);

                uploadCoverThenSaveBook(
                        isEdit,
                        isEdit ? existing.id : null,
                        data,
                        dialog,
                        btnSave
                );
            });
        });

        dialog.show();
    }

    private void confirmDelete(BookItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Book")
                .setMessage("Yakin hapus buku \"" + item.tittle + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBook(item.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBook(String id) {
        db.collection("books")
                .document(id)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Book deleted", Toast.LENGTH_SHORT).show();
                    loadBooks();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed delete book: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ==================== MODEL & ADAPTER ====================

    static class BookItem {
        String id;
        String tittle;
        String author;
        String isbn;
        String categoryId;
        String sypnosis;
        String coverUrl;
        int year;
        int totalCopies;
        int availableCopies;
    }

    static class BooksAdapter extends RecyclerView.Adapter<BooksAdapter.BookVH> {

        interface BookListener {
            void onEdit(BookItem item);
            void onDelete(BookItem item);
        }

        private final List<BookItem> items;
        private final BookListener listener;
        private final Context context;

        BooksAdapter(Context context, List<BookItem> items, BookListener listener) {
            this.context  = context;
            this.items    = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public BookVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book_admin, parent, false);
            return new BookVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BookVH holder, int position) {
            holder.bind(items.get(position), listener, context);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class BookVH extends RecyclerView.ViewHolder {

            TextView txtTitle, txtAuthor, txtCategory, txtCopies, txtQrLabel;
            View btnEdit, btnDelete, btnQr, layoutQr, btnDownloadQr;
            ImageView imgQrPreview;
            ImageView imgCover;
            Bitmap qrBitmap;

            BookVH(@NonNull View itemView) {
                super(itemView);
                txtTitle      = itemView.findViewById(R.id.txtBookTitle);
                txtAuthor     = itemView.findViewById(R.id.txtBookAuthor);
                txtCategory   = itemView.findViewById(R.id.txtBookCategory);
                txtCopies     = itemView.findViewById(R.id.txtBookCopies);
                btnEdit       = itemView.findViewById(R.id.btnEditBook);
                btnDelete     = itemView.findViewById(R.id.btnDeleteBook);
                btnQr         = itemView.findViewById(R.id.btnQrBook);

                layoutQr      = itemView.findViewById(R.id.layoutQr);
                txtQrLabel    = itemView.findViewById(R.id.txtQrLabel);
                imgQrPreview  = itemView.findViewById(R.id.imgQrPreview);
                btnDownloadQr = itemView.findViewById(R.id.btnDownloadQr);

                imgCover      = itemView.findViewById(R.id.imgBookCoverAdmin);
            }

            void bind(BookItem item, BookListener listener, Context context) {
                if (imgCover != null) {
                    if (!TextUtils.isEmpty(item.coverUrl)) {
                        Glide.with(context)
                                .load(item.coverUrl)
                                .placeholder(R.mipmap.ic_launcher)
                                .error(R.mipmap.ic_launcher)
                                .into(imgCover);
                    } else {
                        imgCover.setImageResource(R.mipmap.ic_launcher);
                    }
                }

                txtTitle.setText(!TextUtils.isEmpty(item.tittle) ? item.tittle : "(no title)");
                txtAuthor.setText(!TextUtils.isEmpty(item.author) ? "By " + item.author : "By -");
                txtCategory.setText(!TextUtils.isEmpty(item.categoryId) ? "Category: " + item.categoryId : "Category: -");

                String copiesText = item.availableCopies + " of " + item.totalCopies + " copies";
                txtCopies.setText(copiesText);

                if (layoutQr != null) layoutQr.setVisibility(View.GONE);
                qrBitmap = null;
                if (imgQrPreview != null) imgQrPreview.setImageDrawable(null);

                btnEdit.setOnClickListener(v -> listener.onEdit(item));
                btnDelete.setOnClickListener(v -> listener.onDelete(item));

                if (btnQr != null) {
                    btnQr.setOnClickListener(v -> {
                        if (layoutQr == null || imgQrPreview == null) {
                            Toast.makeText(context, "Layout QR belum di-setup di XML", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (layoutQr.getVisibility() == View.VISIBLE) {
                            layoutQr.setVisibility(View.GONE);
                            return;
                        }

                        if (txtQrLabel != null) {
                            txtQrLabel.setText("QR for: " +
                                    (!TextUtils.isEmpty(item.tittle) ? item.tittle : item.id));
                        }

                        qrBitmap = generateQrBitmap(item.id, 800, 800);
                        if (qrBitmap != null) {
                            imgQrPreview.setImageBitmap(qrBitmap);
                            layoutQr.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(context, "Failed generate QR", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                if (btnDownloadQr != null) {
                    btnDownloadQr.setOnClickListener(v -> {
                        if (qrBitmap == null) {
                            qrBitmap = generateQrBitmap(item.id, 800, 800);
                            if (qrBitmap != null && imgQrPreview != null) {
                                imgQrPreview.setImageBitmap(qrBitmap);
                            }
                        }

                        if (qrBitmap == null) {
                            Toast.makeText(context, "QR belum tersedia", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        saveQrToGallery(context, qrBitmap,
                                "QR_" + (TextUtils.isEmpty(item.tittle) ? item.id : item.tittle));
                    });
                }
            }

            private Bitmap generateQrBitmap(String text, int width, int height) {
                try {
                    BitMatrix matrix = new MultiFormatWriter()
                            .encode(text, BarcodeFormat.QR_CODE, width, height, null);

                    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                        }
                    }
                    return bmp;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            private void saveQrToGallery(Context context, Bitmap bmp, String baseName) {
                OutputStream out = null;
                try {
                    String displayName = baseName.replace(" ", "_")
                            + "_" + System.currentTimeMillis() + ".png";

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LibraryQR");
                    values.put(MediaStore.Images.Media.IS_PENDING, 1);

                    ContentResolver resolver = context.getContentResolver();
                    Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) {
                        Toast.makeText(context, "Gagal membuat file", Toast.LENGTH_LONG).show();
                        return;
                    }

                    out = resolver.openOutputStream(uri);
                    if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        throw new IOException("Failed to save bitmap");
                    }

                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(uri, values, null, null);

                    Toast.makeText(context,
                            "QR disimpan ke Gallery (Pictures/LibraryQR)",
                            Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Gagal simpan QR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    if (out != null) {
                        try { out.close(); } catch (IOException ignored) {}
                    }
                }
            }
        }
    }
}
