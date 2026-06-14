package com.example.harudiary.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;
import com.example.harudiary.model.Record;

import java.util.List;

public class PlanListAdapter extends RecyclerView.Adapter<PlanListAdapter.ViewHolder> {

    private List<Record> plans;
    private OnPlanClickListener listener;

    public interface OnPlanClickListener {
        void onPlanClick(String date);
    }

    public PlanListAdapter(List<Record> plans, OnPlanClickListener listener) {
        this.plans = plans;
        this.listener = listener;
    }

    public void update(List<Record> newPlans) {
        this.plans = newPlans;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Record plan = plans.get(position);

        // UI Mapping
        holder.tvContent.setText(plan.getTitle() != null ? plan.getTitle() : "새로운 여행 계획");
        holder.tvTime.setText(plan.getDate() != null ? plan.getDate() : "");
        
        holder.tvPlanBadge.setVisibility(View.VISIBLE);
        holder.tvPlanBadge.setText("✨ AI 계획");
        
        holder.tvRating.setVisibility(View.GONE);
        holder.tvLocation.setVisibility(View.GONE);
        holder.btnGeneratePlan.setVisibility(View.GONE);
        holder.ivPhoto.setVisibility(View.GONE);
        
        if (holder.layoutHeader != null) {
            // Remove any dynamically added Visit buttons if they exist
            View btnVisit = holder.itemView.findViewWithTag("btnVisit");
            if (btnVisit != null) {
                holder.layoutHeader.removeView(btnVisit);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && plan.getDate() != null) {
                listener.onPlanClick(plan.getDate());
            }
        });
    }

    @Override
    public int getItemCount() {
        return plans == null ? 0 : plans.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime, tvPlanBadge, tvRating, tvLocation, btnGeneratePlan;
        ImageView ivPhoto;
        LinearLayout layoutHeader;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvTime = itemView.findViewById(R.id.tv_card_time);
            tvPlanBadge = itemView.findViewById(R.id.tv_plan_badge);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvLocation = itemView.findViewById(R.id.tv_location);
            btnGeneratePlan = itemView.findViewById(R.id.btn_generate_plan);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            layoutHeader = itemView.findViewById(R.id.layout_header); // Wait, layout_header is from item_travel_place_default.xml usually, but let's check item_timeline_card.xml
        }
    }
}
