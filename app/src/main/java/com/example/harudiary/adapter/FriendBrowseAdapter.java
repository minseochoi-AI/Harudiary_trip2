package com.example.harudiary.adapter;

import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;
import com.example.harudiary.model.FriendRecord;

import java.util.Date;
import java.util.List;

/**
 * FriendBrowseAdapter — 친구 기록 둘러보기 (수평 스크롤 카드)
 * D·LOG 스타일의 카드형 미리보기
 */
public class FriendBrowseAdapter extends RecyclerView.Adapter<FriendBrowseAdapter.ViewHolder> {

    public interface OnFriendRecordClickListener {
        void onFriendRecordClick(FriendRecord record);
    }

    private List<FriendRecord> records;
    private final OnFriendRecordClickListener listener;

    public FriendBrowseAdapter(List<FriendRecord> records, OnFriendRecordClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    public void update(List<FriendRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_record_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRecord record = records.get(position);

        // 아바타
        String initial = record.getUserName() != null && !record.getUserName().isEmpty()
                ? record.getUserName().substring(0, 1) : "?";
        holder.tvFriendAvatar.setText(initial);
        holder.tvFriendName.setText(record.getUserName());

        // 제목: 날짜 기반
        holder.tvRecordTitle.setText(formatDateTitle(record.getDate()));

        // 시간
        String time = DateFormat.format("a hh:mm",
                new Date(record.getTimestamp())).toString();
        String slotEmoji = getSlotEmoji(record.getTimeSlot());
        holder.tvTime.setText(slotEmoji + " " + time);

        // 장소
        String address = record.getAddress();
        if (address != null && !address.isEmpty()) {
            holder.tvLocation.setText("📍 " + address);
            holder.tvLocation.setVisibility(View.VISIBLE);
        } else {
            holder.tvLocation.setVisibility(View.GONE);
        }

        // 내용 미리보기
        String content = record.getContent();
        if (content != null && !content.isEmpty()) {
            holder.tvContentPreview.setText(content);
            holder.tvContentPreview.setVisibility(View.VISIBLE);
        } else {
            holder.tvContentPreview.setVisibility(View.GONE);
        }

        // 사진
        if (record.getPhotoUri() != null && !record.getPhotoUri().isEmpty()) {
            try {
                com.example.harudiary.util.ImageUtil.setSafeImageURI(holder.itemView.getContext(), holder.ivPhoto, record.getPhotoUri());
                holder.ivPhoto.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                holder.ivPhoto.setVisibility(View.GONE);
            }
        } else {
            holder.ivPhoto.setVisibility(View.GONE);
        }

        // 클릭 → 친구 기록 상세 보기
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFriendRecordClick(record);
        });
    }

    @Override
    public int getItemCount() {
        return records != null ? records.size() : 0;
    }

    private String getSlotEmoji(String slot) {
        if ("morning".equals(slot)) return "🌤";
        if ("lunch".equals(slot)) return "☀️";
        if ("evening".equals(slot)) return "🌙";
        return "📝";
    }

    /** "2026-01-20" → "1월 20일 기록" */
    private String formatDateTitle(String date) {
        try {
            String[] parts = date.split("-");
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            return month + "월 " + day + "일 기록";
        } catch (Exception e) {
            return date;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFriendAvatar, tvFriendName, tvRecordTitle;
        TextView tvTime, tvLocation, tvContentPreview;
        ImageView ivPhoto;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFriendAvatar = itemView.findViewById(R.id.tv_friend_avatar);
            tvFriendName = itemView.findViewById(R.id.tv_friend_name);
            tvRecordTitle = itemView.findViewById(R.id.tv_record_title);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvContentPreview = itemView.findViewById(R.id.tv_content_preview);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
        }
    }
}
