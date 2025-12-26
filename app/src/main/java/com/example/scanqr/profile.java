package com.example.scanqr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class profile extends BaseActivity {

    private static final String TAG = "ProfileActivity";

    // View mode
    private ImageView imgAvatar;
    private TextView txtFullName, txtSid, txtEmail, txtStatusProfile;
    private Button btnEditProfile;

    // Edit mode
    private LinearLayout editContainer;
    private EditText edtFullName, edtSid, edtEmail;
    private TextView txtStatusEdit;
    private Button btnCancelEdit, btnSaveProfile;

    private Button btnLogout;
    private Button btnAdminPanel;  // tombol buat admin / super_admin

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String currentUid;
    private String currentPhotoUrl = "";
    private String currentRole = "member";

    private boolean isEditMode = false;
    private boolean isAvatarUploading = false;

    private ActivityResultLauncher<String> pickImageLauncher;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ===== INIT VIEW =====
        imgAvatar        = findViewById(R.id.imgAvatar);
        txtFullName      = findViewById(R.id.txtFullName);
        txtSid           = findViewById(R.id.txtSid);
        txtEmail         = findViewById(R.id.txtEmail);
        txtStatusProfile = findViewById(R.id.txtStatusProfile);

        btnEditProfile   = findViewById(R.id.btnEditProfile);

        editContainer    = findViewById(R.id.editContainer);
        edtFullName      = findViewById(R.id.edtFullName);
        edtSid           = findViewById(R.id.edtSid);
        edtEmail         = findViewById(R.id.edtEmail);
        txtStatusEdit    = findViewById(R.id.txtStatusEdit);
        btnCancelEdit    = findViewById(R.id.btnCancelEdit);
        btnSaveProfile   = findViewById(R.id.btnSaveProfile);

        btnLogout        = findViewById(R.id.btnLogout);
        btnAdminPanel    = findViewById(R.id.btnAdminPanel); // default gone di XML

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_LONG).show();
            startActivity(new Intent(profile.this, Login.class));
            finish();
            return;
        }
        currentUid = user.getUid();

        // Picker untuk image (gallery)
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;

                    // preview dulu
                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .into(imgAvatar);

                    // upload hanya saat edit mode
                    if (isEditMode) {
                        uploadImageToFirebaseStorage(uri);
                    }
                }
        );

        imgAvatar.setOnClickListener(v -> {
            if (!isEditMode) return;
            if (isAvatarUploading) {
                Toast.makeText(this, "Sedang upload foto...", Toast.LENGTH_SHORT).show();
                return;
            }
            pickImageLauncher.launch("image/*");
        });

        btnEditProfile.setOnClickListener(v -> enterEditMode());
        btnCancelEdit.setOnClickListener(v -> exitEditMode(false));
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> doLogout());

        btnAdminPanel.setOnClickListener(v ->
                startActivity(new Intent(profile.this, AdminDashboardActivity.class)));

        // Load profile
        loadProfile();
        setupBottomBar(R.id.nav_profile);
    }

    private void doLogout() {
        auth.signOut();
        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(profile.this, Login.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    // ==================== MODE VIEW / EDIT ====================

    private void enterEditMode() {
        isEditMode = true;
        editContainer.setVisibility(android.view.View.VISIBLE);
        btnEditProfile.setVisibility(android.view.View.GONE);

        String nameView  = txtFullName.getText().toString();
        String sidView   = txtSid.getText().toString().replace("SID: ", "");
        String emailView = txtEmail.getText().toString().replace("Email: ", "");

        if (nameView.equals("-")) nameView = "";
        if (sidView.equals("-")) sidView = "";
        if (emailView.equals("-")) emailView = "";

        edtFullName.setText(nameView);
        edtSid.setText(sidView);
        edtEmail.setText(emailView);

        // Email read-only biar gak mismatch sama FirebaseAuth
        edtEmail.setEnabled(false);
        edtEmail.setAlpha(0.6f);

        txtStatusEdit.setText("Edit mode aktif. Tap avatar untuk ganti foto.");
    }

    private void exitEditMode(boolean fromSave) {
        isEditMode = false;
        editContainer.setVisibility(android.view.View.GONE);
        btnEditProfile.setVisibility(android.view.View.VISIBLE);

        if (!fromSave) {
            txtStatusEdit.setText("Edit dibatalkan.");
        }
    }

    // ==================== LOAD PROFILE ====================

    private void loadProfile() {
        txtStatusProfile.setText("Memuat profile...");

        db.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(this::bindProfile)
                .addOnFailureListener(e -> {
                    String msg = "Gagal load profile: " + e.getMessage();
                    txtStatusProfile.setText(msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

                    txtFullName.setText("-");
                    txtSid.setText("SID: -");
                    txtEmail.setText("Email: -");
                    imgAvatar.setImageResource(R.mipmap.ic_launcher);
                });
    }

    private void bindProfile(DocumentSnapshot doc) {
        if (!doc.exists()) {
            txtStatusProfile.setText("Profile belum dibuat.");
            txtFullName.setText("-");
            txtSid.setText("SID: -");
            txtEmail.setText("Email: -");
            imgAvatar.setImageResource(R.mipmap.ic_launcher);
            return;
        }

        String name  = doc.getString("name");
        String sid   = doc.getString("SID");
        String email = doc.getString("email");
        String photo = doc.getString("photoUrl");
        String role  = doc.getString("role");

        currentRole = (role != null && !role.trim().isEmpty()) ? role.trim() : "member";

        boolean isAdminRole =
                "admin".equals(currentRole) || "super_admin".equals(currentRole);

        btnAdminPanel.setVisibility(isAdminRole ? android.view.View.VISIBLE : android.view.View.GONE);

        // Kalau nanti ada fitur blocked, tinggal cek disini
        btnEditProfile.setEnabled(true);
        imgAvatar.setEnabled(true);

        if (photo != null) {
            photo = photo.trim();
            if (photo.startsWith("http://")) {
                photo = photo.replace("http://", "https://");
            }
        }

        currentPhotoUrl = (photo != null) ? photo : "";

        txtFullName.setText(name != null && !name.isEmpty() ? name : "-");
        txtSid.setText("SID: " + (sid != null && !sid.isEmpty() ? sid : "-"));
        txtEmail.setText("Email: " + (email != null && !email.isEmpty() ? email : "-"));

        if (!TextUtils.isEmpty(currentPhotoUrl)) {
            String finalPhoto = currentPhotoUrl;
            Glide.with(this)
                    .load(finalPhoto)
                    .centerCrop()
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e,
                                                    Object model,
                                                    Target<android.graphics.drawable.Drawable> target,
                                                    boolean isFirstResource) {
                            Log.e(TAG, "Glide load avatar FAILED: " + finalPhoto, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                       Object model,
                                                       Target<android.graphics.drawable.Drawable> target,
                                                       DataSource dataSource,
                                                       boolean isFirstResource) {
                            Log.d(TAG, "Glide avatar loaded OK from: " + finalPhoto);
                            return false;
                        }
                    })
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.mipmap.ic_launcher);
        }

        txtStatusProfile.setText("");
    }

    // ==================== SAVE PROFILE ====================

    private void saveProfile() {
        if (isAvatarUploading) {
            Toast.makeText(this, "Tunggu upload foto selesai dulu", Toast.LENGTH_SHORT).show();
            return;
        }

        String newName  = edtFullName.getText().toString().trim();
        String newSid   = edtSid.getText().toString().trim();

        // email tetap ditampilkan, tapi tidak diedit
        String newEmail = edtEmail.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            edtFullName.setError("Nama tidak boleh kosong");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", newName);
        data.put("SID", newSid);
        data.put("email", newEmail);

        if (!TextUtils.isEmpty(currentPhotoUrl)) {
            data.put("photoUrl", currentPhotoUrl);
        }

        // jangan sentuh role di sini
        data.put("updatedAt", FieldValue.serverTimestamp());

        txtStatusEdit.setText("Menyimpan profile...");
        btnSaveProfile.setEnabled(false);

        db.collection("users")
                .document(currentUid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    btnSaveProfile.setEnabled(true);
                    txtStatusEdit.setText("Profile tersimpan.");
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    loadProfile();
                    exitEditMode(true);
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    String msg = "Gagal simpan profile: " + e.getMessage();
                    txtStatusEdit.setText(msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
    }

    // ==================== UPLOAD AVATAR KE FIREBASE STORAGE ====================

    private void uploadImageToFirebaseStorage(Uri imageUri) {
        if (imageUri == null) return;
        if (isAvatarUploading) return;

        isAvatarUploading = true;
        imgAvatar.setEnabled(false);
        btnSaveProfile.setEnabled(false);

        txtStatusEdit.setText("Upload foto ke server...");

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("avatars")
                .child(currentUid)
                .child("avatar.jpg");

        storageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return storageRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    currentPhotoUrl = downloadUrl;

                    db.collection("users")
                            .document(currentUid)
                            .update(
                                    "photoUrl", downloadUrl,
                                    "updatedAt", FieldValue.serverTimestamp()
                            )
                            .addOnSuccessListener(unused -> {
                                txtStatusEdit.setText("Upload foto berhasil.");

                                Glide.with(this)
                                        .load(downloadUrl)
                                        .centerCrop()
                                        .placeholder(R.mipmap.ic_launcher)
                                        .error(R.mipmap.ic_launcher)
                                        .into(imgAvatar);

                                isAvatarUploading = false;
                                imgAvatar.setEnabled(true);
                                btnSaveProfile.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                txtStatusEdit.setText("Gagal simpan URL foto: " + e.getMessage());

                                isAvatarUploading = false;
                                imgAvatar.setEnabled(true);
                                btnSaveProfile.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    txtStatusEdit.setText("Upload ke server gagal: " + e.getMessage());

                    isAvatarUploading = false;
                    imgAvatar.setEnabled(true);
                    btnSaveProfile.setEnabled(true);
                });
    }
}
