package com.example.scanqr;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ManageUserAdapter extends RecyclerView.Adapter<ManageUserAdapter.UserVH> {

    private final List<AppUser> users;
    private final String myRole;  // "admin" atau "super_admin"
    private final String myUid;   // uid ku sendiri
    private final FirebaseFirestore db;

    public ManageUserAdapter(List<AppUser> users, String myRole, String myUid) {
        this.users = users;
        this.myRole = myRole;
        this.myUid = myUid;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_user, parent, false);
        return new UserVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserVH holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserVH extends RecyclerView.ViewHolder {

        TextView txtUserName, txtUserEmail, txtUserRole, txtUserStatus;
        MaterialButton btnChangeRole, btnToggleActive;

        UserVH(@NonNull View itemView) {
            super(itemView);
            txtUserName   = itemView.findViewById(R.id.txtUserName);
            txtUserEmail  = itemView.findViewById(R.id.txtUserEmail);
            txtUserRole   = itemView.findViewById(R.id.txtUserRole);
            txtUserStatus = itemView.findViewById(R.id.txtUserStatus);
            btnChangeRole = itemView.findViewById(R.id.btnChangeRole);
            btnToggleActive = itemView.findViewById(R.id.btnToggleActive);
        }

        void bind(AppUser user) {
            String name  = !TextUtils.isEmpty(user.getName()) ? user.getName() : "(No name)";
            String email = !TextUtils.isEmpty(user.getEmail()) ? user.getEmail() : "(No email)";
            String role  = !TextUtils.isEmpty(user.getRole()) ? user.getRole() : "member";

            boolean active = user.isActive();
            boolean isMe   = user.getId() != null && user.getId().equals(myUid);

            txtUserName.setText(name);
            txtUserEmail.setText(email);
            txtUserRole.setText("Role: " + role);

            txtUserStatus.setText(active ? "Active" : "Blocked");
            int statusColor = active
                    ? Color.parseColor("#2E7D32")   // hijau
                    : Color.parseColor("#C62828");   // merah
            txtUserStatus.setTextColor(statusColor);

            // ===== Atur label button =====
            if ("super_admin".equals(role)) {
                btnChangeRole.setText("Super Admin");
            } else if ("admin".equals(role)) {
                btnChangeRole.setText("Make Member");
            } else {
                btnChangeRole.setText("Make Admin");
            }

            btnToggleActive.setText(active ? "Block" : "Unblock");

            // ===== Atur ENABLE / DISABLE sesuai peran =====
            boolean canChangeRole;
            boolean canToggleActive;

            if ("super_admin".equals(myRole)) {
                // super_admin: bebas kecuali dirinya sendiri
                if (!isMe) {
                    canToggleActive = true;
                    if (!"super_admin".equals(role)) {
                        // boleh ubah role admin/member
                        canChangeRole = true;
                    } else {
                        canChangeRole = false;
                    }
                } else {
                    canToggleActive = false;
                    canChangeRole = false;
                }
            } else if ("admin".equals(myRole)) {
                // admin:
                // - hanya bisa block/unblock member lain
                // - tidak bisa ubah role siapa pun
                if (!isMe && "member".equals(role)) {
                    canToggleActive = true;
                } else {
                    canToggleActive = false;
                }
                canChangeRole = false;
            } else {
                canToggleActive = false;
                canChangeRole = false;
            }

            btnChangeRole.setEnabled(canChangeRole);
            btnToggleActive.setEnabled(canToggleActive);

            // sedikit efek visual
            float alphaRole  = canChangeRole ? 1f : 0.5f;
            float alphaBlock = canToggleActive ? 1f : 0.5f;
            btnChangeRole.setAlpha(alphaRole);
            btnToggleActive.setAlpha(alphaBlock);

            // ====== CLICK: CHANGE ROLE ======
            btnChangeRole.setOnClickListener(v -> {
                if (!canChangeRole) {
                    Toast.makeText(
                            itemView.getContext(),
                            "You can't change this user's role.",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                String oldRole = user.getRole();
                if (oldRole == null) oldRole = "member";

                // toggle member <-> admin
                String newRole = "member".equals(oldRole) ? "admin" : "member";

                db.collection("users")
                        .document(user.getId())
                        .update("role", newRole)
                        .addOnSuccessListener(unused -> {
                            user.setRole(newRole);
                            notifyItemChanged(pos);
                            Toast.makeText(
                                    itemView.getContext(),
                                    "Role changed to " + newRole,
                                    Toast.LENGTH_SHORT
                            ).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(
                                itemView.getContext(),
                                "Failed change role: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show());
            });

            // ====== CLICK: TOGGLE ACTIVE ======
            btnToggleActive.setOnClickListener(v -> {
                if (!canToggleActive) {
                    Toast.makeText(
                            itemView.getContext(),
                            "You can't change this user's status.",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                boolean newActive = !user.isActive();

                db.collection("users")
                        .document(user.getId())
                        .update("active", newActive)
                        .addOnSuccessListener(unused -> {
                            user.setActive(newActive);
                            notifyItemChanged(pos);
                            Toast.makeText(
                                    itemView.getContext(),
                                    newActive ? "User unblocked" : "User blocked",
                                    Toast.LENGTH_SHORT
                            ).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(
                                itemView.getContext(),
                                "Failed update status: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show());
            });
        }
    }
}
