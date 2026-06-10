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

import com.example.harudiary.R;
import com.example.harudiary.activity.RecordActivity;
import com.example.harudiary.model.Record;
import com.example.harudiary.util.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.widget.Button;
import android.widget.Toast;
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.api.DiaryApi;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DailyTimelineFragment — 하루 타임라인 (내 기록)
 * ★ 카드 클릭 시 친구들이 남긴 하트·댓글 정보 표시
 */
public class DailyTimelineFragment extends Fragment {

    private static final String ARG_DATE = "date";

    private String date;
    private String userId;
    private LayoutInflater cachedInflater;

    private TextView  tvMorningTime, tvLunchTime, tvEveningTime, tvOtherTime;
    private LinearLayout flMorning, flLunch, flEvening, flOther;
    private View sectionOther;
    

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
        cachedInflater = inflater;

        date        = getArguments() != null ? getArguments().getString(ARG_DATE, "") : "";
        SessionManager sm = new SessionManager(requireContext());
        int userIdInt = sm.getLoggedInUserId();
        userId = String.valueOf(userIdInt);

        tvMorningTime = view.findViewById(R.id.tv_morning_time);
        tvLunchTime   = view.findViewById(R.id.tv_lunch_time);
        tvEveningTime = view.findViewById(R.id.tv_evening_time);
        tvOtherTime   = view.findViewById(R.id.tv_other_time);
        flMorning     = view.findViewById(R.id.fl_morning);
        flLunch       = view.findViewById(R.id.fl_lunch);
        flEvening     = view.findViewById(R.id.fl_evening);
        flOther       = view.findViewById(R.id.fl_other);
        sectionOther  = view.findViewById(R.id.section_other);


        loadAndBind();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (flMorning != null) loadAndBind();
    }

    // ── 데이터 바인딩 ────────────────────────────────────────────

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
        List<Record> morningList = new ArrayList<>();
        List<Record> lunchList   = new ArrayList<>();
        List<Record> eveningList = new ArrayList<>();
        List<Record> otherList   = new ArrayList<>();

        for (Record r : records) {
            String slot = r.getTimeSlot();
            if ("morning".equals(slot))      morningList.add(r);
            else if ("lunch".equals(slot))   lunchList.add(r);
            else if ("evening".equals(slot)) eveningList.add(r);
            else                             otherList.add(r);
        }

        bindSlot(flMorning, tvMorningTime, morningList, "morning");
        bindSlot(flLunch,   tvLunchTime,   lunchList,   "lunch");
        bindSlot(flEvening, tvEveningTime, eveningList, "evening");
        bindOtherSlot(otherList);
    }

    private void bindSlot(LinearLayout container, TextView tvTime,
                          List<Record> records, String slot) {
        container.removeAllViews();
        if (records.isEmpty()) {
            showEmptyCard(container, tvTime, slot);
        } else {
            tvTime.setText(DateFormat.format("HH:mm",
                    new Date(records.get(0).getTimestamp())).toString());
            for (Record r : records) {
                container.addView(buildActivityCard(container, r));
            }
            addPlusButton(container, slot);
        }
    }

    private void bindOtherSlot(List<Record> otherList) {
        if (sectionOther == null) return;
        if (otherList.isEmpty()) {
            sectionOther.setVisibility(View.GONE);
        } else {
            sectionOther.setVisibility(View.VISIBLE);
            flOther.removeAllViews();
            tvOtherTime.setText(DateFormat.format("HH:mm",
                    new Date(otherList.get(0).getTimestamp())).toString());
            for (Record r : otherList) {
                flOther.addView(buildActivityCard(flOther, r));
            }
            addPlusButton(flOther, null);
        }
    }

    // ── 카드 빌드 ─────────────────────────────────────────────────

    private View buildActivityCard(LinearLayout parent, Record record) {
        View card = cachedInflater.inflate(R.layout.item_timeline_card, parent, false);

        // 사진
        ImageView ivPhoto = card.findViewById(R.id.iv_photo);
        if (record.getPhotoUri() != null && !record.getPhotoUri().isEmpty()) {
            try {
                ivPhoto.setImageURI(Uri.parse(record.getPhotoUri()));
                ivPhoto.setVisibility(View.VISIBLE);
            } catch (Exception ignored) {
                ivPhoto.setVisibility(View.GONE);
            }
        } else {
            ivPhoto.setVisibility(View.GONE);
        }

        // 시각
        TextView tvCardTime = card.findViewById(R.id.tv_card_time);
        if (tvCardTime != null) tvCardTime.setText("🕐 " +
                DateFormat.format("HH:mm", new Date(record.getTimestamp())));

        // 날씨
        TextView tvCardWeather = card.findViewById(R.id.tv_card_weather);
        if (tvCardWeather != null) {
            String w = record.getWeather();
            if (w != null && !w.isEmpty()) {
                tvCardWeather.setText(weatherToEmoji(w) + " " + (int) record.getTemperature() + "°");
                tvCardWeather.setVisibility(View.VISIBLE);
            } else {
                tvCardWeather.setVisibility(View.GONE);
            }
        }

        // 내용 · 별점 · 주소
        ((TextView) card.findViewById(R.id.tv_content))
                .setText(record.getContent() != null ? record.getContent() : "");
        ((TextView) card.findViewById(R.id.tv_rating)).setText(buildStars(record.getRating()));
        String addr = record.getAddress();
        ((TextView) card.findViewById(R.id.tv_location))
                .setText((addr != null && !addr.isEmpty()) ? "📍 " + addr : "");

        // ★ 리액션 바 설정 (친구들이 남긴 하트·댓글)
        setupReactionBar(card, record);

        return card;
    }

    /**
     * ★ 리액션 바 — 내 기록에 친구들이 남긴 반응 조회
     *  - 카드 클릭 → 리액션 바 슬라이드 토글
     *  - 하트 수·댓글 수 표시 (내 기록이므로 집계만 보여줌)
     *  - 댓글 버튼 클릭 → CommentBottomSheetFragment 오픈
     */
    private void setupReactionBar(View card, Record record) {
        int activityId = record.getActivityId();

        LinearLayout reactionBar  = card.findViewById(R.id.reaction_bar);
        TextView tvHeartIcon      = card.findViewById(R.id.tv_heart_icon);
        TextView tvHeartCount     = card.findViewById(R.id.tv_heart_count);
        TextView tvCommentCount   = card.findViewById(R.id.tv_comment_count);
        LinearLayout btnHeart     = card.findViewById(R.id.btn_heart);
        LinearLayout btnComment   = card.findViewById(R.id.btn_comment);
        TextView btnClose         = card.findViewById(R.id.btn_close_reaction);

        if (reactionBar == null) return;

        // 리액션 바가 클릭 이벤트를 소비 → 카드 토글 리스너로 전파 방지
        reactionBar.setClickable(true);
        reactionBar.setOnClickListener(v -> { /* 이벤트 소비 */ });

        // 내 기록은 자신이 하트를 남길 수 없으므로 하트 아이콘은 집계 표시 전용
        tvHeartIcon.setText("❤️");
        if (btnHeart != null) btnHeart.setClickable(false); // 터치 비활성

        // 댓글 버튼 → 바텀 시트 오픈
        if (btnComment != null) {
            btnComment.setClickable(true);
            btnComment.setOnClickListener(v -> {
                if (!isAdded()) return;
                try {
                    CommentBottomSheetFragment sheet =
                            CommentBottomSheetFragment.newInstance(activityId);
                    sheet.setOnCommentChangedListener(() ->
                            loadReactionCounts(activityId, reactionBar,
                                    tvHeartCount, tvCommentCount));
                    sheet.show(getChildFragmentManager(), "comments_" + activityId);
                } catch (IllegalStateException e) {
                    android.util.Log.e("DailyTimeline", "comment sheet error", e);
                }
            });
        }

        // 닫기 버튼
        if (btnClose != null) {
            btnClose.setOnClickListener(v ->
                    hideReactionBar(reactionBar));
        }

        // ★ 카드 클릭 → 리액션 바 토글
        card.setOnClickListener(v -> {
            if (reactionBar.getVisibility() == View.VISIBLE) {
                hideReactionBar(reactionBar);
            } else {
                showReactionBar(reactionBar);
            }
        });

        // 초기 카운트 로드 (반응이 있으면 자동 표시)
        loadReactionCounts(activityId, reactionBar, tvHeartCount, tvCommentCount);
    }

    private void showReactionBar(LinearLayout bar) {
        bar.setVisibility(View.VISIBLE);
        bar.setAlpha(0f);
        bar.setScaleY(0.5f);
        bar.setPivotY(0f);
        bar.animate().alpha(1f).scaleY(1f).setDuration(200).start();
    }

    private void hideReactionBar(LinearLayout bar) {
        bar.animate().alpha(0f).scaleY(0f).setDuration(180)
                .withEndAction(() -> bar.setVisibility(View.GONE)).start();
    }

    /**
     * 하트 수·댓글 수 비동기 로드
     * 반응이 하나라도 있으면 리액션 바를 자동으로 노출
     */
    private void loadReactionCounts(int activityId, LinearLayout reactionBar,
                                     TextView tvHeartCount, TextView tvCommentCount) {
        // TODO: 백엔드 API로 좋아요/댓글 수 가져오기 (현재는 N+1 문제 방지를 위해 임시로 0 처리)
        int heartCount   = 0;
        int commentCount = 0;
        boolean hasReaction = heartCount > 0 || commentCount > 0;

        if (tvHeartCount   != null) tvHeartCount.setText(String.valueOf(heartCount));
        if (tvCommentCount != null) tvCommentCount.setText(String.valueOf(commentCount));
        // 반응이 있으면 바 자동 노출
        if (reactionBar != null && hasReaction
                && reactionBar.getVisibility() != View.VISIBLE) {
            reactionBar.setAlpha(1f);
            reactionBar.setScaleY(1f);
            reactionBar.setVisibility(View.VISIBLE);
        }
    }

    // ── 빈 카드 / 추가 버튼 ────────────────────────────────────────

    private void addPlusButton(LinearLayout container, @Nullable String slot) {
        View empty = cachedInflater.inflate(R.layout.item_timeline_empty, container, false);
        ((TextView) empty.findViewById(R.id.tv_add_label))
                .setText("+ " + slotToKorean(slot) + " 기록 추가");
        empty.setOnClickListener(v -> navigateToRecord(slot));
        container.addView(empty);
    }

    private void showEmptyCard(LinearLayout container, TextView tvTime, String slot) {
        View empty = cachedInflater.inflate(R.layout.item_timeline_empty, container, false);
        ((TextView) empty.findViewById(R.id.tv_add_label))
                .setText("+ " + slotToKorean(slot) + " 기록 추가");
        tvTime.setText("--:--");
        empty.setOnClickListener(v -> navigateToRecord(slot));
        container.addView(empty);
    }

    private void navigateToRecord(@Nullable String slot) {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), RecordActivity.class);
        intent.putExtra(RecordActivity.EXTRA_DATE, date);
        if (slot != null) intent.putExtra(RecordActivity.EXTRA_SLOT, slot);
        startActivity(intent);
    }

    // ── 유틸 ─────────────────────────────────────────────────────

    private static String weatherToEmoji(String weather) {
        if (weather == null) return "☀️";
        switch (weather) {
            case "비":            return "🌧";
            case "비/눈":
            case "빗방울눈날림": return "🌨";
            case "눈":
            case "눈날림":        return "❄️";
            case "빗방울":        return "🌦";
            default:              return "☀️";
        }
    }

    private static String buildStars(float rating) {
        int full = Math.round(rating);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < full ? "★" : "☆");
        return sb.toString();
    }

    private static String slotToKorean(@Nullable String slot) {
        if ("morning".equals(slot)) return "아침";
        if ("lunch".equals(slot))   return "점심";
        if ("evening".equals(slot)) return "저녁";
        return "기타";
    }

}
