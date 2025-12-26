package com.example.scanqr;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageUserActivity extends BaseActivity {

    private String myRole = "member";
    private String myUid  = null;

    private TextView txtTitle;
    private RecyclerView rvUsers;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private final List<AppUser> userList = new ArrayList<>();
    private ManageUserAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user);

        // role dikirim lewat Intent
        myRole = getIntent().getStringExtra("ROLE");
        if (myRole == null) myRole = "member";

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            myUid = currentUser.getUid();
        }

        // SAMAIN DENGAN LAYOUT: txtTitleManageUsers & rvManageUsers
        txtTitle = findViewById(R.id.txtTitleManageUsers);
        rvUsers  = findViewById(R.id.rvManageUsers);

        if (txtTitle != null) {
            txtTitle.setText("Manage Users (role ku: " + myRole + ")");
        }

        if (rvUsers == null) {
            Toast.makeText(this,
                    "Layout error: rvManageUsers tidak ditemukan di activity_manage_user.xml",
                    Toast.LENGTH_LONG).show();
            return;
        }

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManageUserAdapter(userList, myRole, myUid);
        rvUsers.setAdapter(adapter);

        // optional, tab mana bebas
        setupBottomBar(R.id.nav_collection);

        loadAllUsers();
    }

    private void loadAllUsers() {
        // cuma admin / super_admin boleh buka activity ini
        if (!"admin".equals(myRole) && !"super_admin".equals(myRole)) {
            Toast.makeText(this,
                    "You don't have permission to manage users.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    userList.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        AppUser u = new AppUser();
                        u.setId(doc.getId());
                        u.setName(doc.getString("name"));
                        u.setEmail(doc.getString("email"));

                        String role = doc.getString("role");
                        if (TextUtils.isEmpty(role)) role = "member";
                        u.setRole(role);

                        Boolean active = doc.getBoolean("active");
                        if (active == null) active = true;
                        u.setActive(active);

                        userList.add(u);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed load users: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
