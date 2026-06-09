package com.example.harudiary.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;
import com.example.harudiary.model.FriendRequest;

import java.util.List;

/**
 * FriendRequestAdapter — 받은 친구 요청 리스트 (수락/거절)
 */
public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(FriendRequest request, int position);
        void onReject(FriendRequest request, int position);
    }

    private List<FriendRequest> requests;
    private final OnRequestActionListener listener;

    public FriendRequestAdapter(List<FriendRequest> requests, OnRequestActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    public void update(List<FriendRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        if (position >= 0 && position < requests.size()) {
            requests.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequest request = requests.get(position);

        String initial = request.getFromUserName() != null && !request.getFromUserName().isEmpty()
                ? request.getFromUserName().substring(0, 1) : "?";
        holder.tvAvatar.setText(initial);
        holder.tvName.setText(request.getFromUserName());
        holder.tvEmail.setText(request.getFromUserEmail());

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(request, holder.getAdapterPosition());
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(request, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvEmail, btnAccept, btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}
