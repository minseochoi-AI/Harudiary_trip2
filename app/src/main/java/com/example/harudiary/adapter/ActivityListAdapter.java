package com.example.harudiary.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;
import com.example.harudiary.model.Record;

import java.util.Calendar;
import java.util.List;

/**
 * ActivityListAdapter — 최근 기록 ListView 어댑터
 * 썸네일 + 날짜/시간대 + 내용 + 평점 + 날씨/위치 표시
 */
public class ActivityListAdapter extends RecyclerView.Adapter<ActivityListAdapter.ViewHolder> {

    private List<Record> records;
    private final OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(String date);
    }

    public ActivityListAdapter(List<Record> records, OnDateClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    /** 데이터 갱신 */
    public void update(List<Record> newRecords) {
        this.records = newRecords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder vh, int position) {
        Record r = records.get(position);

        // 날짜 + 시간대 (예: "5/21 (수) 아침")
        vh.tvDateSlot.setText(formatDate(r.getDate()) + " " + slotToKorean(r.getTimeSlot()));

        // 활동 내용
        vh.tvContent.setText(r.getContent() != null ? r.getContent() : "");

        // 평점 + 날씨 또는 위치
        StringBuilder meta = new StringBuilder("⭐ " + r.getRating());
        if (r.getWeather() != null && !r.getWeather().isEmpty()) {
            meta.append("    ").append(r.getWeather());
        } else if (r.getAddress() != null && !r.getAddress().isEmpty()) {
            meta.append("    📍 ").append(r.getAddress());
        }
        vh.tvMeta.setText(meta.toString());

        // 썸네일
        vh.ivThumbnail.setBackground(
                ContextCompat.getDrawable(vh.itemView.getContext(), R.drawable.bg_thumbnail));
        com.example.harudiary.util.ImageUtil.setSafeImageURI(vh.itemView.getContext(), vh.ivThumbnail, r.getPhotoUri());

        // AI 여행계획 생성 버튼 로직
        if (r.isPlan()) {
            vh.btnGeneratePlan.setVisibility(View.GONE);
        } else {
            vh.btnGeneratePlan.setVisibility(View.VISIBLE);
            vh.btnGeneratePlan.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(vh.itemView.getContext(), com.example.harudiary.activity.PlanInputActivity.class);
                intent.putExtra(com.example.harudiary.activity.PlanInputActivity.EXTRA_DATE, r.getDate());
                if (r.getContent() != null && !r.getContent().isEmpty()) {
                    intent.putExtra("EXTRA_CONTENT", r.getContent());
                }
                vh.itemView.getContext().startActivity(intent);
            });
        }

        // 항목 클릭 시 상세(Daily)로 이동하는 로직 제거 (홈 상세 타임라인 보기에서만 쓰이도록 리다이렉트 수정)
        // vh.itemView.setOnClickListener(v -> { ... });
    }

    @Override
    public int getItemCount() {
        return records == null ? 0 : records.size();
    }

    /** "morning" → "아침" 변환 */
    private String slotToKorean(String slot) {
        if ("morning".equals(slot)) return "아침";
        if ("lunch".equals(slot)) return "점심";
        if ("evening".equals(slot)) return "저녁";
        return slot;
    }

    /** "2026-05-21" → "5/21 (수)" */
    private String formatDate(String date) {
        try {
            String[] p = date.split("-");
            int y = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int d = Integer.parseInt(p[2]);
            Calendar cal = Calendar.getInstance();
            cal.set(y, m - 1, d);
            String[] dayNames = {"일", "월", "화", "수", "목", "금", "토"};
            return m + "/" + d + " (" + dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1] + ")";
        } catch (Exception e) {
            return date;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvDateSlot, tvContent, tvMeta, btnGeneratePlan;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvDateSlot = itemView.findViewById(R.id.tv_date_slot);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            btnGeneratePlan = itemView.findViewById(R.id.btn_generate_plan);
        }
    }
}
