package com.example.scanqr;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ManageCategoriesActivity extends AppCompatActivity {
    // bentar ya say aku capek sama adek kamu :v
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_categories);

        TextView tv = findViewById(R.id.txtTitleManageCategories);
        tv.setText("Manage Categories (TODO: CRUD genre/kategori)");
    }
}
