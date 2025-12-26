package com.example.scanqr;

import android.content.Context;
import android.text.TextUtils;
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

public class TopBooksAdapter extends RecyclerView.Adapter<TopBooksAdapter.TopBookViewHolder> {

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    private final List<Book> books;
    private final Context context;
    private final OnBookClickListener listener;

    public TopBooksAdapter(List<Book> books, Context context, OnBookClickListener listener) {
        this.books = books;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TopBookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                // ⬇⬇ GANTI kalau nama file layout-mu beda (misal: R.layout.item_top)
                .inflate(R.layout.item_top_book, parent, false);
        return new TopBookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopBookViewHolder holder, int position) {
        Book b = books.get(position);
        holder.bind(b, listener);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class TopBookViewHolder extends RecyclerView.ViewHolder {

        ImageView imgCoverTop;
        TextView txtTitleTop, txtAuthorTop;

        public TopBookViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCoverTop = itemView.findViewById(R.id.imgCoverTop);
            txtTitleTop = itemView.findViewById(R.id.txtTitleTop);
            txtAuthorTop = itemView.findViewById(R.id.txtAuthorTop);
        }

        void bind(Book b, OnBookClickListener listener) {
            String title  = b.getTittle() != null ? b.getTittle() : "(No title)";
            String author = b.getAuthor() != null ? b.getAuthor() : "(Unknown author)";

            txtTitleTop.setText(title);
            txtAuthorTop.setText(author);

            String url = b.getCoverUrl();
            if (!TextUtils.isEmpty(url) && url.startsWith("http://")) {
                url = url.replace("http://", "https://");
            }

            Glide.with(itemView.getContext())
                    .load(url)
                    .centerCrop()
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(imgCoverTop);

            // === KLIK ITEM BUKA DETAIL ===
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBookClick(b);
                }
            });
        }
    }
}
