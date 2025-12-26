package com.example.scanqr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class CategoryBooksAdapter extends RecyclerView.Adapter<CategoryBooksAdapter.CategoryBookViewHolder> {

    private final List<Book> books;
    private final Context context;

    public CategoryBooksAdapter(List<Book> books, Context context) {
        this.books = books;
        this.context = context;
    }

    @NonNull
    @Override
    public CategoryBookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Pakai parent.getContext supaya lebih aman
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_book, parent, false);
        return new CategoryBookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryBookViewHolder holder, int position) {
        Book b = books.get(position);

        String title  = b.getTittle() != null ? b.getTittle() : "(No title)";
        String author = b.getAuthor() != null ? b.getAuthor() : "(Unknown author)";
        long available = b.getAvailableCopies();
        long total     = b.getTotalCopies();

        holder.txtTitle.setText(title);
        holder.txtAuthor.setText(author);
        holder.txtCopies.setText("Available: " + available + " / " + total);

        String url = b.getCoverUrl();
        if (url != null && url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }

        Glide.with(holder.itemView.getContext())
                .load(url)
                .centerCrop()
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(holder.imgCover);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class CategoryBookViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView txtTitle, txtAuthor, txtCopies;

        public CategoryBookViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCoverCategory);
            txtTitle = itemView.findViewById(R.id.txtTitleCategory);
            txtAuthor = itemView.findViewById(R.id.txtAuthorCategory);
            txtCopies = itemView.findViewById(R.id.txtCopiesCategory);
        }
    }
}
