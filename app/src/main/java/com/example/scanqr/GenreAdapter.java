package com.example.scanqr;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {

    private final List<Genre> genres;
    private final Context context;

    public GenreAdapter(List<Genre> genres, Context context) {
        this.genres = genres;
        this.context = context;
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_genre, parent, false);
        return new GenreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        Genre g = genres.get(position);
        holder.txtGenreName.setText(g.getName());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CategoryBooksActivity.class);
            intent.putExtra("CATEGORY_ID", g.getName());

            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return genres.size();
    }

    static class GenreViewHolder extends RecyclerView.ViewHolder {
        TextView txtGenreName;

        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            txtGenreName = itemView.findViewById(R.id.txtGenreName);
        }
    }
}
