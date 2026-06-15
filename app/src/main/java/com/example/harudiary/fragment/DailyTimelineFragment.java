package com.example.harudiary.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;
import com.example.harudiary.activity.PlanInputActivity;
import com.example.harudiary.activity.RecordActivity;
import com.example.harudiary.model.Record;
import com.example.harudiary.MainActivity;
import com.example.harudiary.util.SessionManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.api.DiaryApi;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DailyTimelineFragment extends Fragment {

    private static final String ARG_DATE = "date";
    private String date;
    private String userId;

    private RecyclerView rvTimeline;
    private TextView tvEmpty;
    private TimelineAdapter adapter;

    public static DailyTimelineFragment newInstance(String date) {
        DailyTimelineFragment f = new DailyTimelineFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_timeline, container, false);

        date = getArguments() != null ? getArguments().getString(ARG_DATE, "") : "";
        userId = new SessionManager(requireContext()).getUserId();

        rvTimeline = view.findViewById(R.id.rv_timeline);
        tvEmpty = view.findViewById(R.id.tv_empty);

        rvTimeline.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TimelineAdapter();
        rvTimeline.setAdapter(adapter);

        loadAndBind();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) loadAndBind();
    }

    private void loadAndBind() {
        DiaryApi diaryApi = RetrofitClient.getClient().create(DiaryApi.class);
        diaryApi.getActivitiesByDate(String.valueOf(userId), date).enqueue(new Callback<List<Record>>() {
            @Override
            public void onResponse(@NonNull Call<List<Record>> call, @NonNull Response<List<Record>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (isAdded()) requireActivity().runOnUiThread(() -> processRecords(response.body()));
                } else {
                    if (isAdded()) requireActivity().runOnUiThread(() -> processRecords(new ArrayList<>()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Record>> call, @NonNull Throwable t) {
                if (isAdded()) requireActivity().runOnUiThread(() -> processRecords(new ArrayList<>()));
            }
        });
    }

    private void processRecords(List<Record> records) {
        // 필터링: plan과 실제 기록이 중복될 경우, 실제 기록 우선
        boolean hasActual = false;
        for (Record r : records) {
            if (!r.isPlan()) {
                hasActual = true;
                break;
            }
        }
        if (hasActual) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                records.removeIf(Record::isPlan);
            } else {
                java.util.Iterator<Record> it = records.iterator();
                while (it.hasNext()) {
                    if (it.next().isPlan()) it.remove();
                }
            }
        }

        // 시간순(오름차순) 정렬
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            records.sort((r1, r2) -> Long.compare(r1.getTimestamp(), r2.getTimestamp()));
        } else {
            java.util.Collections.sort(records, (r1, r2) -> Long.compare(r1.getTimestamp(), r2.getTimestamp()));
        }

        if (records.isEmpty()) {
            rvTimeline.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvTimeline.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            adapter.setRecords(records);
        }
    }

    private class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {
        private List<Record> records = new ArrayList<>();

        public void setRecords(List<Record> newRecords) {
            this.records = newRecords;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Record record = records.get(position);

            // 사진
            if (record.getPhotoUri() != null && !record.getPhotoUri().isEmpty()) {
                try {
                    com.example.harudiary.util.ImageUtil.setSafeImageURI(requireContext(), holder.ivPhoto, record.getPhotoUri());
                } catch (Exception e) {
                    holder.ivPhoto.setVisibility(View.GONE);
                }
            } else {
                holder.ivPhoto.setVisibility(View.GONE);
            }

            // 시각
            if (record.isPlan()) {
                String slot = record.getTimeSlot();
                if (slot != null && slot.contains("일차")) {
                    holder.tvCardTime.setText("🕐 " + slot + " 일정");
                } else {
                    String koreanSlot = "전체";
                    if ("morning".equals(slot)) koreanSlot = "아침";
                    else if ("lunch".equals(slot)) koreanSlot = "점심";
                    else if ("evening".equals(slot)) koreanSlot = "저녁";
                    holder.tvCardTime.setText("🕐 " + koreanSlot + " 일정");
                }
            } else {
                holder.tvCardTime.setText("🕐 " + DateFormat.format("HH:mm", new Date(record.getTimestamp())));
            }

            // 날씨
            String w = record.getWeather();
            if (w != null && !w.isEmpty()) {
                holder.tvCardWeather.setText(weatherToEmoji(w) + " " + (int) record.getTemperature() + "°");
                holder.tvCardWeather.setVisibility(View.VISIBLE);
            } else {
                holder.tvCardWeather.setVisibility(View.GONE);
            }

            // 내용, 별점, 주소
            holder.tvContent.setText(record.getContent() != null ? record.getContent() : "");
            holder.tvRating.setText(buildStars(record.getRating()));
            String addr = record.getAddress();
            holder.tvLocation.setText((addr != null && !addr.isEmpty()) ? addr : "위치 정보 없음");

            // 계획 배지
            if (record.isPlan()) {
                holder.tvPlanBadge.setVisibility(View.VISIBLE);
                holder.cardRoot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF3E0")));
            } else {
                holder.tvPlanBadge.setVisibility(View.GONE);
                holder.cardRoot.setBackgroundTintList(null);
            }

            // AI 추천 버튼
            holder.btnGeneratePlan.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), PlanInputActivity.class);
                intent.putExtra(PlanInputActivity.EXTRA_DATE, date);
                if (record.getContent() != null && !record.getContent().isEmpty()) {
                    intent.putExtra("EXTRA_CONTENT", record.getContent());
                }
                if (record.getAddress() != null && !record.getAddress().isEmpty()) {
                    intent.putExtra("EXTRA_DESTINATION", record.getAddress());
                }
                startActivity(intent);
            });

            // 삭제 버튼 로직
            holder.btnDelete.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("기록 삭제")
                    .setMessage("이 기록을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        DiaryApi api = RetrofitClient.getClient().create(DiaryApi.class);
                        api.deleteDiary(record.getActivityId(), userId).enqueue(new retrofit2.Callback<Void>() {
                            @Override
                            public void onResponse(@NonNull retrofit2.Call<Void> call, @NonNull retrofit2.Response<Void> response) {
                                if (response.isSuccessful()) {
                                    int pos = holder.getAdapterPosition();
                                    if (pos != RecyclerView.NO_POSITION) {
                                        records.remove(pos);
                                        notifyItemRemoved(pos);
                                        android.widget.Toast.makeText(requireContext(), "삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show();
                                        if (records.isEmpty()) {
                                            rvTimeline.setVisibility(View.GONE);
                                            tvEmpty.setVisibility(View.VISIBLE);
                                        }
                                    }
                                } else {
                                    android.widget.Toast.makeText(requireContext(), "삭제 실패", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(@NonNull retrofit2.Call<Void> call, @NonNull Throwable t) {
                                android.widget.Toast.makeText(requireContext(), "네트워크 오류", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("취소", null)
                    .show();
            });

            // 리액션 바 설정
            setupReactionBar(holder, record);
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        private void setupReactionBar(ViewHolder holder, Record record) {
            Long activityId = record.getActivityId();

            holder.reactionBar.setClickable(true);
            holder.reactionBar.setOnClickListener(v -> {});

            holder.tvHeartIcon.setText("❤️");
            holder.btnHeart.setClickable(false);

            holder.btnComment.setClickable(true);
            holder.btnComment.setOnClickListener(v -> {
                if (!isAdded()) return;
                try {
                    CommentBottomSheetFragment sheet = CommentBottomSheetFragment.newInstance(activityId);
                    sheet.setOnCommentChangedListener(() -> loadReactionCounts(holder, record));
                    sheet.show(getChildFragmentManager(), "comments_" + activityId);
                } catch (Exception ignored) {}
            });

            holder.btnCloseReaction.setOnClickListener(v -> hideReactionBar(holder.reactionBar));

            holder.cardRoot.setOnClickListener(v -> {
                if (record.isPlan()) {
                    if (record.getActivityId() == null) return;
                    com.example.harudiary.api.TravelApi travelApi = com.example.harudiary.api.RetrofitClient.getClient().create(com.example.harudiary.api.TravelApi.class);
                    travelApi.getPlanById(record.getActivityId()).enqueue(new retrofit2.Callback<com.example.harudiary.model.TravelPlanResponse>() {
                        @Override
                        public void onResponse(@NonNull retrofit2.Call<com.example.harudiary.model.TravelPlanResponse> call, @NonNull retrofit2.Response<com.example.harudiary.model.TravelPlanResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                if (!isAdded() || getActivity() == null) return;
                                Intent intent = new Intent(requireContext(), com.example.harudiary.activity.TravelPlanActivity.class);
                                intent.putExtra("plan", response.body());
                                intent.putExtra("isConfirmMode", true);
                                intent.putExtra("diaryId", record.getActivityId());
                                intent.putExtra("date", record.getDate());
                                startActivity(intent);
                            } else {
                                if (isAdded() && getActivity() != null) {
                                    android.widget.Toast.makeText(requireContext(), "계획 상세를 불러오지 못했습니다.", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onFailure(@NonNull retrofit2.Call<com.example.harudiary.model.TravelPlanResponse> call, @NonNull Throwable t) {
                            if (isAdded() && getActivity() != null) {
                                android.widget.Toast.makeText(requireContext(), "네트워크 오류", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToRecordEdit(record);
                    }
                }
            });

            holder.cardRoot.setOnLongClickListener(v -> {
                if (holder.reactionBar.getVisibility() == View.VISIBLE) {
                    hideReactionBar(holder.reactionBar);
                } else {
                    showReactionBar(holder.reactionBar);
                }
                return true;
            });

            loadReactionCounts(holder, record);
        }

        private void loadReactionCounts(ViewHolder holder, Record record) {
            if (record.getActivityId() == null) return;
            
            ReactionApi api = RetrofitClient.getClient().create(ReactionApi.class);
            api.getReactionCounts(record.getActivityId()).enqueue(new retrofit2.Callback<java.util.Map<String, Integer>>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<java.util.Map<String, Integer>> call, @NonNull retrofit2.Response<java.util.Map<String, Integer>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        int heartCount = response.body().getOrDefault("likes", 0);
                        int commentCount = response.body().getOrDefault("comments", 0);
                        
                        holder.tvHeartCount.setText(String.valueOf(heartCount));
                        holder.tvCommentCount.setText(String.valueOf(commentCount));

                        if ((heartCount > 0 || commentCount > 0) && holder.reactionBar.getVisibility() != View.VISIBLE) {
                            showReactionBar(holder.reactionBar);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull retrofit2.Call<java.util.Map<String, Integer>> call, @NonNull Throwable t) {
                    // Ignore failure
                }
            });
        }

        private void showReactionBar(View bar) {
            bar.setVisibility(View.VISIBLE);
            bar.setAlpha(0f);
            bar.setScaleY(0.5f);
            bar.setPivotY(0f);
            bar.animate().alpha(1f).scaleY(1f).setDuration(200).start();
        }

        private void hideReactionBar(View bar) {
            bar.animate().alpha(0f).scaleY(0f).setDuration(180)
                    .withEndAction(() -> bar.setVisibility(View.GONE)).start();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout cardRoot, reactionBar, btnHeart, btnComment;
            ImageView ivPhoto;
            TextView tvCardTime, tvCardWeather, tvContent, tvRating, tvLocation, tvPlanBadge;
            TextView tvHeartIcon, tvHeartCount, tvCommentCount, btnCloseReaction, btnGeneratePlan, btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardRoot = itemView.findViewById(R.id.card_root);
                ivPhoto = itemView.findViewById(R.id.iv_photo);
                tvCardTime = itemView.findViewById(R.id.tv_card_time);
                tvCardWeather = itemView.findViewById(R.id.tv_card_weather);
                tvContent = itemView.findViewById(R.id.tv_content);
                tvRating = itemView.findViewById(R.id.tv_rating);
                tvLocation = itemView.findViewById(R.id.tv_location);
                tvPlanBadge = itemView.findViewById(R.id.tv_plan_badge);
                btnGeneratePlan = itemView.findViewById(R.id.btn_generate_plan);
                btnDelete = itemView.findViewById(R.id.btn_delete);

                reactionBar = itemView.findViewById(R.id.reaction_bar);
                btnHeart = itemView.findViewById(R.id.btn_heart);
                btnComment = itemView.findViewById(R.id.btn_comment);
                tvHeartIcon = itemView.findViewById(R.id.tv_heart_icon);
                tvHeartCount = itemView.findViewById(R.id.tv_heart_count);
                tvCommentCount = itemView.findViewById(R.id.tv_comment_count);
                btnCloseReaction = itemView.findViewById(R.id.btn_close_reaction);
            }
        }
    }

    private static String weatherToEmoji(String weather) {
        if (weather == null) return "☀️";
        switch (weather) {
            case "비": return "🌧";
            case "비/눈":
            case "빗방울눈날림": return "🌨";
            case "눈":
            case "눈날림": return "❄️";
            case "빗방울": return "🌦";
            default: return "☀️";
        }
    }

    private static String buildStars(float rating) {
        int full = Math.round(rating);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < full ? "★" : "☆");
        return sb.toString();
    }
}
