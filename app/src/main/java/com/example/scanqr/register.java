package com.example.scanqr;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class register extends AppCompatActivity {

    private static final String TAG = "RegisterDebug";

    private EditText edtName, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private TextView txtGoLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtName            = findViewById(R.id.edtName);
        edtEmail           = findViewById(R.id.edtEmail);
        edtPassword        = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister        = findViewById(R.id.btnRegister);
        txtGoLogin         = findViewById(R.id.txtGoLogin);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> doRegister());
        txtGoLogin.setOnClickListener(v ->
                startActivity(new Intent(register.this, Login.class)));
    }

    private void doRegister() {
        final String name    = edtName.getText().toString().trim();
        final String email   = edtEmail.getText().toString().trim();
        final String pass    = edtPassword.getText().toString().trim();
        final String confirm = edtConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) ) {
            edtName.setError("Name required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Email required");
            return;
        }
        String emailLower = email.toLowerCase().trim();
        if(!Patterns.EMAIL_ADDRESS.matcher(emailLower).matches()){
            edtEmail.setError("Invalid format");
            return;
        }
        if(!emailLower.endsWith("@gmail.com") || emailLower.endsWith("@student.president.ac.id")){
            edtEmail.setError("Incorect email address");
            return;
        }
        if (TextUtils.isEmpty(pass) || pass.length() < 6) {
            edtPassword.setError("Min 6 characters");
            return;
        }
        if (!pass.equals(confirm)) {
            edtConfirmPassword.setError("Password not match");
            return;
        }

        btnRegister.setEnabled(false);

        auth.createUserWithEmailAndPassword(emailLower, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        btnRegister.setEnabled(true);

                        if (!task.isSuccessful()) {
                            String err = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Unknown error";
                            Toast.makeText(register.this,
                                    "Register failed: " + err,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(register.this,
                                    "User null after register",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        String uid = user.getUid();

                        // Dokumen users/{uid}
                        Map<String, Object> data = new HashMap<>();
                        data.put("SID", "");
                        data.put("name", name);
                        data.put("email", email);
                        data.put("photoUrl", "");
                        data.put("role", "member");     // default member
                        data.put("blocked", false);     // default tidak diblok
                        data.put("createdAt", FieldValue.serverTimestamp());
                        data.put("updatedAt", FieldValue.serverTimestamp());

                        db.collection("users")
                                .document(uid)
                                .set(data)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(register.this,
                                            "Register success, please login",
                                            Toast.LENGTH_SHORT).show();
                                    goToLoginAndFinish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(register.this,
                                            "Failed save profile: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    goToLoginAndFinish();
                                });
                    }
                });
    }

    private void goToLoginAndFinish() {
        // penting: jangan auto-login
        if (auth != null) auth.signOut();

        Intent i = new Intent(register.this, Login.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
