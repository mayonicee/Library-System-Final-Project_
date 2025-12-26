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
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RecentBorrowAdapter extends RecyclerView.Adapter<RecentBorrowAdapter.RBViewHolder> {

    public interface OnRecentBorrowClickListener {
        void onClick(BorrowRecord record);
    }

    private final List<BorrowRecord> items;
    private final OnRecentBorrowClickListener listener;
    private final SimpleDateFormat df =
            new SimpleDateFormat("dd MMM", Locale.getDefault());

    public RecentBorrowAdapter(List<BorrowRecord> items,
                               OnRecentBorrowClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RBViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_borrow, parent, false);
        return new RBViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RBViewHolder holder, int position) {
        holder.bind(items.get(position), listener, df);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RBViewHolder extends RecyclerView.ViewHolder {

        ImageView imgCover;
        TextView txtTitle, txtStatus, txtMeta;

        public RBViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgRecentCover);
            txtTitle = itemView.findViewById(R.id.txtRecentTitle);
            txtStatus = itemView.findViewById(R.id.txtRecentStatus);
            txtMeta = itemView.findViewById(R.id.txtRecentMeta);
        }

        void bind(BorrowRecord r,
                  OnRecentBorrowClickListener listener,
                  SimpleDateFormat df) {

            String title = !TextUtils.isEmpty(r.getBookTittle())
                    ? r.getBookTittle()
                    : "(No title)";
            txtTitle.setText(title);

            String rawStatus = r.getStatus();
            if (TextUtils.isEmpty(rawStatus)) rawStatus = "PENDING";
            String statusUpper = rawStatus.toUpperCase(Locale.ROOT);

            txtStatus.setText(rawStatus);

            if (statusUpper.startsWith("PENDING") || statusUpper.startsWith("ON_APPROVAL")) {
                txtStatus.setTextColor(0xFFFFCC80); // oranye
            } else if (statusUpper.startsWith("BORROWED") || statusUpper.startsWith("APPROVED")) {
                txtStatus.setTextColor(0xFF80CBC4); // hijau kebiruan
            } else if (statusUpper.startsWith("RETURNED")) {
                txtStatus.setTextColor(0xFF81C784); // hijau
            } else if (statusUpper.startsWith("REJECTED") || statusUpper.contains("LATE")) {
                txtStatus.setTextColor(0xFFE57373); // merah
            } else {
                txtStatus.setTextColor(0xFFFFFFFF);
            }

            // Meta: due date + locationType
            StringBuilder meta = new StringBuilder();
            Timestamp due = r.getDueDate();
            if (due != null) {
                meta.append("Due ").append(df.format(due.toDate()));
            }
            if (!TextUtils.isEmpty(r.getLocationType())) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append(r.getLocationType());
            }
            if (meta.length() == 0) meta.append("No due date");
            txtMeta.setText(meta.toString());

            String url = r.getCoverUrl();
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
                if (listener != null) listener.onClick(r);
            });
        }
    }
}
