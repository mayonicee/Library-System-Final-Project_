package com.example.scanqr;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class ManageBorrowsAdapter extends RecyclerView.Adapter<ManageBorrowsAdapter.BorrowViewHolder> {

    public interface OnBorrowActionListener {
        void onApprove(BorrowRecord record);
        void onReject(BorrowRecord record);
        void onMarkReturned(BorrowRecord record);
    }

    private final List<BorrowRecord> items;
    private final OnBorrowActionListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

    public ManageBorrowsAdapter(List<BorrowRecord> items,
                                OnBorrowActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BorrowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_borrow_record, parent, false);
        return new BorrowViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BorrowViewHolder holder, int position) {
        BorrowRecord r = items.get(position);
        holder.bind(r, listener, dateFormat);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BorrowViewHolder extends RecyclerView.ViewHolder {

        ImageView imgBorrowCover;
        TextView txtBorrowBookTitle, txtBorrowBorrower, txtBorrowStatus, txtBorrowDue;
        Button btnReject, btnApprove, btnMarkReturned;

        public BorrowViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBorrowCover = itemView.findViewById(R.id.imgBorrowCover);
            txtBorrowBookTitle = itemView.findViewById(R.id.txtBorrowBookTitle);
            txtBorrowBorrower = itemView.findViewById(R.id.txtBorrowBorrower);
            txtBorrowStatus = itemView.findViewById(R.id.txtBorrowStatus);
            txtBorrowDue = itemView.findViewById(R.id.txtBorrowDue);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnMarkReturned = itemView.findViewById(R.id.btnMarkReturned);
        }

        void bind(BorrowRecord r,
                  OnBorrowActionListener listener,
                  SimpleDateFormat df) {

            // ====== TEXT DASAR ======
            String title = !TextUtils.isEmpty(r.getBookTittle())
                    ? r.getBookTittle()
                    : "(No title)";

            String borrowerText = "";
            if (!TextUtils.isEmpty(r.getBorrowerName())) {
                borrowerText += r.getBorrowerName();
            }
            if (!TextUtils.isEmpty(r.getBorrowerId())) {
                if (!borrowerText.isEmpty()) borrowerText += " · ";
                borrowerText += r.getBorrowerId();
            }
            if (TextUtils.isEmpty(borrowerText)) {
                borrowerText = "(Unknown borrower)";
            }

            txtBorrowBookTitle.setText(title);
            txtBorrowBorrower.setText(borrowerText);

            // ====== STATUS ======
            String rawStatus = r.getStatus();
            if (TextUtils.isEmpty(rawStatus)) rawStatus = "PENDING";

            String statusUpper = rawStatus.trim().toUpperCase(Locale.ROOT);

            // tampilkan status yang konsisten (upper biar rapi)
            txtBorrowStatus.setText(statusUpper);

            // ====== WARNA STATUS ======
            if (statusUpper.startsWith("PENDING") || statusUpper.startsWith("ON_APPROVAL")) {
                txtBorrowStatus.setTextColor(0xFFFFCC80); // oranye
            } else if (statusUpper.startsWith("BORROWED") || statusUpper.startsWith("APPROVED") || statusUpper.contains("LATE")) {
                txtBorrowStatus.setTextColor(0xFF80CBC4); // hijau kebiruan
            } else if (statusUpper.startsWith("RETURNED")) {
                txtBorrowStatus.setTextColor(0xFF81C784); // hijau
            } else if (statusUpper.startsWith("REJECTED")) {
                txtBorrowStatus.setTextColor(0xFFE57373); // merah
            } else {
                txtBorrowStatus.setTextColor(0xFFFFFFFF);
            }

            // ====== DUE DATE + LOCATION ======
            StringBuilder dueInfo = new StringBuilder();
            Timestamp dueTs = r.getDueDate();
            if (dueTs != null) {
                dueInfo.append("Due: ").append(df.format(dueTs.toDate()));
            }

            if (!TextUtils.isEmpty(r.getLocationType())) {
                if (dueInfo.length() > 0) dueInfo.append(" · ");
                dueInfo.append(r.getLocationType());
            }

            if (dueInfo.length() == 0) {
                dueInfo.append("No due date");
            }
            txtBorrowDue.setText(dueInfo.toString());

            // ====== COVER ======
            String url = r.getCoverUrl();
            if (!TextUtils.isEmpty(url) && url.startsWith("http://")) {
                url = url.replace("http://", "https://");
            }

            if (TextUtils.isEmpty(url)) {
                imgBorrowCover.setImageResource(R.mipmap.ic_launcher_round);
            } else {
                Glide.with(itemView.getContext())
                        .load(url)
                        .centerCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(imgBorrowCover);
            }

            // ====== LOGIKA TOMBOL ======
            boolean isPending = statusUpper.startsWith("PENDING") || statusUpper.startsWith("ON_APPROVAL");

            // NOTE: LATE masih dianggap borrowed (bukan final)
            boolean isBorrowed = statusUpper.startsWith("BORROWED")
                    || statusUpper.startsWith("APPROVED")
                    || statusUpper.contains("LATE");

            boolean isFinal = statusUpper.startsWith("RETURNED")
                    || statusUpper.startsWith("REJECTED");

            if (isFinal) {
                btnApprove.setVisibility(View.GONE);
                btnReject.setVisibility(View.GONE);
                btnMarkReturned.setVisibility(View.GONE);

            } else if (isPending) {
                btnApprove.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.VISIBLE);
                btnMarkReturned.setVisibility(View.GONE);

            } else if (isBorrowed) {
                btnApprove.setVisibility(View.GONE);       // sudah approved/borrowed → ga perlu approve lagi
                btnReject.setVisibility(View.GONE);        // sudah borrowed → reject ga relevan
                btnMarkReturned.setVisibility(View.VISIBLE);

            } else {
                btnApprove.setVisibility(View.GONE);
                btnReject.setVisibility(View.GONE);
                btnMarkReturned.setVisibility(View.GONE);
            }

            // ====== LISTENER ======
            btnApprove.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(r);
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(r);
            });

            btnMarkReturned.setOnClickListener(v -> {
                if (listener != null) listener.onMarkReturned(r);
            });
        }
    }
}
