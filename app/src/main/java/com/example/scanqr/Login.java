package com.example.scanqr;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    private static final String TAG = "LoginDebug";

    private EditText edtEmailLogin, edtPasswordLogin;
    private Button btnLogin;
    private TextView txtGoRegister;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmailLogin   = findViewById(R.id.edtEmailLogin);
        edtPasswordLogin= findViewById(R.id.edtPasswordLogin);
        btnLogin        = findViewById(R.id.btnLogin);
        txtGoRegister   = findViewById(R.id.txtGoRegister);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // Kalau sudah login & tidak diblok → langsung ke Main
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            checkUserAndGo(current);
        }

        btnLogin.setOnClickListener(v -> doLogin());
        txtGoRegister.setOnClickListener(v ->
                startActivity(new Intent(Login.this, register.class)));
    }

    private void doLogin() {
        String email = edtEmailLogin.getText().toString().trim();
        String pass  = edtPasswordLogin.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmailLogin.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            edtPasswordLogin.setError("Password required");
            return;
        }

        btnLogin.setEnabled(false);

        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        btnLogin.setEnabled(true);

                        if (!task.isSuccessful()) {
                            String err = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Unknown error";
                            Toast.makeText(Login.this,
                                    "Login failed: " + err,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(Login.this,
                                    "User null setelah login",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        checkUserAndGo(user);
                    }
                });
    }

    private void checkUserAndGo(FirebaseUser user) {
        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this,
                                "Profil user belum ada di Firestore",
                                Toast.LENGTH_LONG).show();
                        // boleh lanjut, tapi sebaiknya dibuat manual
                    }

                    Boolean blocked = doc.getBoolean("blocked");
                    String role     = doc.getString("role");

                    if (blocked != null && blocked) {
                        Toast.makeText(this,
                                "Akun kamu diblokir oleh admin.",
                                Toast.LENGTH_LONG).show();
                        auth.signOut();
                        return;
                    }

                    Log.d(TAG, "Login ok, role = " + role);

                    // lanjut ke MainActivity
                    Intent i = new Intent(Login.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Gagal cek profil: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
