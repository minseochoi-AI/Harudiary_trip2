package com.example.harudiary.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;
import com.example.harudiary.model.User;

import java.util.List;

/**
 * FriendSearchAdapter — 친구 검색 결과 리스트
 */
public class FriendSearchAdapter extends RecyclerView.Adapter<FriendSearchAdapter.ViewHolder> {

    public interface OnActionClickListener {
        void onActionClick(User user, int position);
    }

    private List<User> users;
    private List<String> statuses; // "none", "pending_sent", "pending_received", "friend"
    private final OnActionClickListener listener;

    public FriendSearchAdapter(List<User> users, List<String> statuses, OnActionClickListener listener) {
        this.users = users;
        this.statuses = statuses;
        this.listener = listener;
    }

    public void update(List<User> users, List<String> statuses) {
        this.users = users;
        this.statuses = statuses;
        notifyDataSetChanged();
    }

    public void updateStatus(int position, String newStatus) {
        if (position >= 0 && position < statuses.size()) {
            statuses.set(position, newStatus);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        String status = statuses.get(position);

        // 아바타: 이름 첫 글자
        String initial = user.getName() != null && !user.getName().isEmpty()
                ? user.getName().substring(0, 1) : "?";
        holder.tvAvatar.setText(initial);

        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());

        // 상태에 따라 버튼 텍스트 및 스타일 변경
        switch (status) {
            case "friend":
                holder.btnAction.setText("친구");
                holder.btnAction.setBackgroundResource(R.drawable.bg_button_sent);
                holder.btnAction.setEnabled(false);
                break;
            case "pending_sent":
                holder.btnAction.setText("요청됨");
                holder.btnAction.setBackgroundResource(R.drawable.bg_button_sent);
                holder.btnAction.setEnabled(false);
                break;
            case "pending_received":
                holder.btnAction.setText("수락 대기");
                holder.btnAction.setBackgroundResource(R.drawable.bg_button_sent);
                holder.btnAction.setEnabled(false);
                break;
            default: // "none"
                holder.btnAction.setText("친구 요청");
                holder.btnAction.setBackgroundResource(R.drawable.bg_button_friend);
                holder.btnAction.setEnabled(true);
                break;
        }

        holder.btnAction.setOnClickListener(v -> {
            if (listener != null && "none".equals(status)) {
                listener.onActionClick(user, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvEmail, btnAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            btnAction = itemView.findViewById(R.id.btn_action);
        }
    }
}
