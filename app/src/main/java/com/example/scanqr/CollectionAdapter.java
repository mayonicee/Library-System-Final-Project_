package com.example.scanqr;

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

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.CollectionVH> {

    public interface OnCollectionClickListener {
        void onClick(CollectionItem item);      // tap biasa
        void onLongClick(CollectionItem item);  // long-press
    }

    private final List<CollectionItem> items;
    private final OnCollectionClickListener listener;

    public CollectionAdapter(List<CollectionItem> items,
                             OnCollectionClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CollectionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collection_book, parent, false);
        return new CollectionVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionVH holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CollectionVH extends RecyclerView.ViewHolder {

        ImageView imgCover;
        TextView txtTitle, txtStatus;

        public CollectionVH(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCollectionCover);
            txtTitle = itemView.findViewById(R.id.txtCollectionTitle);
            txtStatus = itemView.findViewById(R.id.txtCollectionStatus);
        }

        void bind(CollectionItem item, OnCollectionClickListener listener) {
            String title = !TextUtils.isEmpty(item.getBookTitle())
                    ? item.getBookTitle()
                    : "(No title)";

            String rawStatus = item.getStatus();
            String statusLabel;

            if ("FAVORITE".equals(rawStatus)) {
                statusLabel = "Favorite";
            } else if ("TO_READ".equals(rawStatus)) {
                statusLabel = "To Read";
            } else if ("READING".equals(rawStatus)) {
                statusLabel = "Reading";
            } else if ("FINISHED".equals(rawStatus)) {
                statusLabel = "Finished";
            } else if (!TextUtils.isEmpty(rawStatus)) {
                statusLabel = rawStatus;
            } else {
                statusLabel = "Unknown";
            }

            txtTitle.setText(title);
            txtStatus.setText(statusLabel);

            String url = item.getCoverUrl();
            if (!TextUtils.isEmpty(url) && url.startsWith("http://")) {
                url = url.replace("http://", "https://");
            }

            Glide.with(itemView.getContext())
                    .load(url)
                    .centerCrop()
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(imgCover);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(item);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(item);
                return true;
            });
        }
    }
}
