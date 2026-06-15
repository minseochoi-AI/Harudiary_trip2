package com.example.harudiary.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.example.harudiary.R;
import com.example.harudiary.api.FriendApi;
import com.example.harudiary.api.ReactionApi;
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.model.TimelineDTO;
import com.example.harudiary.fragment.CommentBottomSheetFragment;
import com.example.harudiary.util.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRecordActivity extends AppCompatActivity {

    public static final String EXTRA_FRIEND_USER_ID = "friend_user_id";
    public static final String EXTRA_FRIEND_NAME    = "friend_name";
    public static final String EXTRA_TARGET_DATE    = "target_date";

    private static final String TAG = "FriendRecordActivity";

    // ── 데이터 ──
    private String myUserId;
    private String friendUserId;
    private String friendName;
    private String currentDate;
    private List<String> allDates = new ArrayList<>();

    // 위치 데이터가 있는 기록과 대응 View (마커 클릭 → 스크롤)
    private final List<TimelineDTO> locatedRecords   = new ArrayList<>();
    private final Map<Long, View> activityIdToView = new LinkedHashMap<>();

    // ── 지도 ──
    private MapView mapView;
    private KakaoMap kakaoMap;
    private boolean mapStarted = false;

    // ── BottomSheet ──
    private LinearLayout bottomSheet;
    private BottomSheetBehavior<LinearLayout> bsBehavior;
    private NestedScrollView nestedScroll;

    // ── 뷰 ──
    private TextView tvHeaderDate, tvFriendAvatar, tvFriendName, tvTripTitle;
    private TextView tvMoreAvatar, tvMoreTitle;
    private TextView tvMorningTime, tvLunchTime, tvEveningTime, tvOtherTime;
    private LinearLayout flMorning, flLunch, flEvening, flOther;
    private LinearLayout sectionOther, rowLastLog;
    private LinearLayout cardRoutePreview, llRoutePhotos, llRoutePins, llDateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_record);

        friendUserId = getIntent().getStringExtra(EXTRA_FRIEND_USER_ID);
        friendName   = getIntent().getStringExtra(EXTRA_FRIEND_NAME);
        currentDate  = getIntent().getStringExtra(EXTRA_TARGET_DATE);

        myUserId = new SessionManager(this).getUserId();

        bindViews();
        setupBottomSheet();
        startKakaoMap();
        loadInitialDates();
    }

    private void bindViews() {
        mapView       = findViewById(R.id.mapView);
        bottomSheet   = findViewById(R.id.bottom_sheet);
        nestedScroll  = findViewById(R.id.nested_scroll);

        tvHeaderDate   = findViewById(R.id.tv_header_date);
        tvFriendAvatar = findViewById(R.id.tv_friend_avatar);
        tvFriendName   = findViewById(R.id.tv_friend_name);
        tvTripTitle    = findViewById(R.id.tv_trip_title);
        tvMoreAvatar   = findViewById(R.id.tv_more_avatar);
        tvMoreTitle    = findViewById(R.id.tv_more_title);
        tvMorningTime  = findViewById(R.id.tv_morning_time);
        tvLunchTime    = findViewById(R.id.tv_lunch_time);
        tvEveningTime  = findViewById(R.id.tv_evening_time);
        tvOtherTime    = findViewById(R.id.tv_other_time);
        flMorning      = findViewById(R.id.fl_morning);
        flLunch        = findViewById(R.id.fl_lunch);
        flEvening      = findViewById(R.id.fl_evening);
        flOther        = findViewById(R.id.fl_other);
        sectionOther   = findViewById(R.id.section_other);
        rowLastLog     = findViewById(R.id.row_last_log);
        cardRoutePreview = findViewById(R.id.card_route_preview);
        llRoutePhotos  = findViewById(R.id.ll_route_photos);
        View pinRow = findViewById(R.id.ll_route_pins);
        llRoutePins = (pinRow instanceof LinearLayout) ? (LinearLayout) pinRow : null;
        llDateList     = findViewById(R.id.ll_date_list);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_header_icon).setOnClickListener(v -> {
            if (bsBehavior != null) bsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }

    private void setupBottomSheet() {
        bsBehavior = BottomSheetBehavior.from(bottomSheet);
        bsBehavior.setHideable(true);
        bsBehavior.setSkipCollapsed(false);

        bottomSheet.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                bottomSheet.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int screenH = getResources().getDisplayMetrics().heightPixels;
                bsBehavior.setPeekHeight((int)(screenH * 0.55f));
                bsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    private void loadInitialDates() {
        FriendApi api = RetrofitClient.getClient().create(FriendApi.class);
        api.getFriendDates(friendUserId).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(@NonNull Call<List<String>> call, @NonNull Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allDates = response.body();
                    if (allDates.isEmpty()) { finish(); return; }
                    if (currentDate == null || !allDates.contains(currentDate)) {
                        currentDate = allDates.get(0);
                    }
                    render();
                } else {
                    Toast.makeText(FriendRecordActivity.this, "데이터를 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<String>> call, @NonNull Throwable t) {
                Toast.makeText(FriendRecordActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void render() {
        String initial = (friendName != null && !friendName.isEmpty())
                ? friendName.substring(0, 1) : "?";

        tvHeaderDate.setText(currentDate.replace("-", "."));
        tvFriendAvatar.setText(initial);
        tvFriendName.setText(friendName);
        tvTripTitle.setText(formatTitle(currentDate));
        tvMoreAvatar.setText(initial);
        tvMoreTitle.setText(friendName + "의 공유 기록 더보기");

        FriendApi api = RetrofitClient.getClient().create(FriendApi.class);
        api.getFriendTimeline(myUserId, friendUserId, currentDate).enqueue(new Callback<List<TimelineDTO>>() {
            @Override
            public void onResponse(@NonNull Call<List<TimelineDTO>> call, @NonNull Response<List<TimelineDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TimelineDTO> records = response.body();
                    activityIdToView.clear();
                    locatedRecords.clear();

                    renderTimeline(records);
                    renderRoutePreview(records);

                    boolean isOldest = allDates.indexOf(currentDate) == allDates.size() - 1;
                    rowLastLog.setVisibility(isOldest ? View.VISIBLE : View.GONE);

                    renderDateList();

                    if (mapStarted && kakaoMap != null) {
                        if (kakaoMap.getLabelManager() != null && kakaoMap.getLabelManager().getLayer() != null) {
                            kakaoMap.getLabelManager().getLayer().removeAll();
                        }
                        addPhotoMarkers(kakaoMap);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TimelineDTO>> call, @NonNull Throwable t) {
                Log.e(TAG, "Timeline fetch failed", t);
            }
        });
    }

    private void renderTimeline(List<TimelineDTO> records) {
        List<TimelineDTO> morning = new ArrayList<>(),
                          lunch   = new ArrayList<>(),
                          evening = new ArrayList<>(),
                          other   = new ArrayList<>();

        for (TimelineDTO r : records) {
            if (r.getLatitude() != null && r.getLatitude() != 0) locatedRecords.add(r);
            String s = r.getTimeSlot();
            if ("morning".equals(s))      morning.add(r);
            else if ("lunch".equals(s))   lunch.add(r);
            else if ("evening".equals(s)) evening.add(r);
            else                          other.add(r);
        }

        LinearLayout secM = findViewById(R.id.section_morning);
        LinearLayout secL = findViewById(R.id.section_lunch);
        LinearLayout secE = findViewById(R.id.section_evening);

        bindSlot(secM, flMorning, tvMorningTime, morning);
        bindSlot(secL, flLunch,   tvLunchTime,   lunch);
        bindSlot(secE, flEvening, tvEveningTime, evening);
        bindOtherSlot(other);
    }

    private void bindSlot(LinearLayout section, LinearLayout container,
                          TextView tvTime, List<TimelineDTO> records) {
        container.removeAllViews();
        if (records.isEmpty()) {
            section.setVisibility(View.GONE);
        } else {
            section.setVisibility(View.VISIBLE);
            tvTime.setText(DateFormat.format("HH:mm", new Date(records.get(0).getTimestamp())).toString());
            for (TimelineDTO r : records) {
                View card = buildRecordCard(r, container);
                container.addView(card);
                activityIdToView.put(r.getId(), card);
            }
        }
    }

    private void bindOtherSlot(List<TimelineDTO> records) {
        flOther.removeAllViews();
        if (records.isEmpty()) {
            sectionOther.setVisibility(View.GONE);
        } else {
            sectionOther.setVisibility(View.VISIBLE);
            tvOtherTime.setText(DateFormat.format("HH:mm", new Date(records.get(0).getTimestamp())).toString());
            for (TimelineDTO r : records) {
                View card = buildRecordCard(r, flOther);
                flOther.addView(card);
                activityIdToView.put(r.getId(), card);
            }
        }
    }

    private View buildRecordCard(TimelineDTO record, LinearLayout container) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_timeline_card, container, false);

        ImageView ivPhoto = card.findViewById(R.id.iv_photo);
        if (record.getPhotoUri() != null && !record.getPhotoUri().isEmpty()) {
            try {
                com.example.harudiary.util.ImageUtil.setSafeImageURI(FriendRecordActivity.this, ivPhoto, record.getPhotoUri());
                ivPhoto.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                ivPhoto.setVisibility(View.GONE);
            }
        } else {
            ivPhoto.setVisibility(View.GONE);
        }

        TextView tvCardTime = card.findViewById(R.id.tv_card_time);
        if (tvCardTime != null) {
            String timeStr = DateFormat.format("HH:mm", new Date(record.getTimestamp())).toString();
            tvCardTime.setText("🕐 " + timeStr);
        }

        TextView tvCardWeather = card.findViewById(R.id.tv_card_weather);
        if (tvCardWeather != null) {
            String weather = record.getWeather();
            Float temp = record.getTemperature();
            if (weather != null && !weather.isEmpty() && temp != null) {
                tvCardWeather.setText(weatherToEmoji(weather) + " " + temp.intValue() + "°");
                tvCardWeather.setVisibility(View.VISIBLE);
            } else {
                tvCardWeather.setVisibility(View.GONE);
            }
        }

        ((TextView) card.findViewById(R.id.tv_content)).setText(record.getContent() != null ? record.getContent() : "");
        
        TextView tvPlanBadge = card.findViewById(R.id.tv_plan_badge);
        TextView tvRating = card.findViewById(R.id.tv_rating);

        if (record.isPlan()) {
            if (tvPlanBadge != null) tvPlanBadge.setVisibility(View.VISIBLE);
            if (tvRating != null) tvRating.setVisibility(View.GONE);
        } else {
            if (tvPlanBadge != null) tvPlanBadge.setVisibility(View.GONE);
            if (tvRating != null) {
                tvRating.setVisibility(View.VISIBLE);
                tvRating.setText(buildStars(record.getRating() != null ? record.getRating() : 0));
            }
        }

        String addr = record.getAddress();
        ((TextView) card.findViewById(R.id.tv_location)).setText((addr != null && !addr.isEmpty()) ? "📍 " + addr : "");

        TextView btnDelete = card.findViewById(R.id.btn_delete);
        if (btnDelete != null) btnDelete.setVisibility(View.GONE);

        TextView btnGeneratePlan = card.findViewById(R.id.btn_generate_plan);
        if (btnGeneratePlan != null) {
            btnGeneratePlan.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, PlanInputActivity.class);
                intent.putExtra(PlanInputActivity.EXTRA_DATE, record.getDate());
                if (record.getContent() != null && !record.getContent().isEmpty()) {
                    intent.putExtra("EXTRA_CONTENT", record.getContent());
                }
                if (record.getAddress() != null && !record.getAddress().isEmpty()) {
                    intent.putExtra("EXTRA_DESTINATION", record.getAddress());
                }
                startActivity(intent);
            });
        }

        Long activityId = record.getId();
        LinearLayout reactionBar = card.findViewById(R.id.reaction_bar);
        TextView tvHeartIcon = card.findViewById(R.id.tv_heart_icon);
        TextView tvHeartCount = card.findViewById(R.id.tv_heart_count);
        TextView tvCommentCount = card.findViewById(R.id.tv_comment_count);
        LinearLayout btnHeart = card.findViewById(R.id.btn_heart);
        LinearLayout btnComment = card.findViewById(R.id.btn_comment);
        TextView btnCloseReaction = card.findViewById(R.id.btn_close_reaction);

        reactionBar.setClickable(true);
        reactionBar.setOnClickListener(v -> {});

        // Use DTO aggregated fields instantly (No SQLite threading needed!)
        boolean hearted = record.isHeartedByMe();
        int heartCount = record.getHeartCount();
        int commentCount = record.getCommentCount();

        tvHeartIcon.setText(hearted ? "❤️" : "🤍");
        tvHeartCount.setText(String.valueOf(heartCount));
        tvCommentCount.setText(String.valueOf(commentCount));

        if (heartCount > 0 || commentCount > 0) {
            reactionBar.setAlpha(1f);
            reactionBar.setScaleY(1f);
            reactionBar.setVisibility(View.VISIBLE);
        }

        card.setOnClickListener(v -> {
            boolean visible = reactionBar.getVisibility() == View.VISIBLE;
            if (visible) {
                reactionBar.animate().alpha(0f).scaleY(0f).setDuration(180)
                    .withEndAction(() -> reactionBar.setVisibility(View.GONE)).start();
            } else {
                reactionBar.setVisibility(View.VISIBLE);
                reactionBar.setAlpha(0f);
                reactionBar.setScaleY(0.5f);
                reactionBar.setPivotY(0f);
                reactionBar.animate().alpha(1f).scaleY(1f).setDuration(200).start();
            }
        });

        // Optimistic UI toggle for Heart
        btnHeart.setOnClickListener(v -> {
            boolean nowHearted = !record.isHeartedByMe();
            int newHeartCount = record.getHeartCount() + (nowHearted ? 1 : -1);
            
            // Optimistic update
            record.setHeartedByMe(nowHearted);
            record.setHeartCount(newHeartCount);
            tvHeartIcon.setText(nowHearted ? "❤️" : "🤍");
            tvHeartCount.setText(String.valueOf(Math.max(newHeartCount, 0)));

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", myUserId);
            payload.put("diaryId", activityId);

            RetrofitClient.getClient().create(ReactionApi.class).toggleHeart(payload).enqueue(new Callback<Boolean>() {
                @Override
                public void onResponse(@NonNull Call<Boolean> call, @NonNull Response<Boolean> response) {
                    if (!response.isSuccessful()) {
                        // Rollback on failure
                        record.setHeartedByMe(!nowHearted);
                        record.setHeartCount(record.getHeartCount() + (!nowHearted ? 1 : -1));
                        tvHeartIcon.setText(!nowHearted ? "❤️" : "🤍");
                        tvHeartCount.setText(String.valueOf(Math.max(record.getHeartCount(), 0)));
                        Toast.makeText(FriendRecordActivity.this, "하트 처리에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<Boolean> call, @NonNull Throwable t) {
                    // Rollback on failure
                    record.setHeartedByMe(!nowHearted);
                    record.setHeartCount(record.getHeartCount() + (!nowHearted ? 1 : -1));
                    tvHeartIcon.setText(!nowHearted ? "❤️" : "🤍");
                    tvHeartCount.setText(String.valueOf(Math.max(record.getHeartCount(), 0)));
                    Toast.makeText(FriendRecordActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnComment.setOnClickListener(v -> {
            CommentBottomSheetFragment sheet = CommentBottomSheetFragment.newInstance(activityId);
            sheet.setOnCommentChangedListener(() -> render()); // Re-render to refresh counts
            try {
                sheet.show(getSupportFragmentManager(), "comments_" + activityId);
            } catch (Exception ignored) {}
        });

        btnCloseReaction.setOnClickListener(v ->
            reactionBar.animate().alpha(0f).scaleY(0f).setDuration(150)
                .withEndAction(() -> reactionBar.setVisibility(View.GONE)).start()
        );

        return card;
    }

    private static String weatherToEmoji(String weather) {
        if (weather == null) return "☀️";
        switch (weather) {
            case "비":             return "🌧";
            case "비/눈":
            case "빗방울눈날림":  return "🌨";
            case "눈":
            case "눈날림":         return "❄️";
            case "빗방울":         return "🌦";
            default:               return "☀️";
        }
    }

    private void renderRoutePreview(List<TimelineDTO> records) {
        List<TimelineDTO> withPhoto = new ArrayList<>();
        for (TimelineDTO r : records) {
            if (r.getPhotoUri() != null && !r.getPhotoUri().isEmpty()) withPhoto.add(r);
        }

        if (withPhoto.isEmpty()) { cardRoutePreview.setVisibility(View.GONE); return; }

        cardRoutePreview.setVisibility(View.VISIBLE);
        llRoutePhotos.removeAllViews();
        if (llRoutePins != null) llRoutePins.removeAllViews();

        int maxShow = 5, total = withPhoto.size(), showCount = Math.min(maxShow, total);

        for (int i = 0; i < showCount; i++) {
            TimelineDTO r = withPhoto.get(i);
            View item = LayoutInflater.from(this).inflate(R.layout.item_route_photo, llRoutePhotos, false);
            ImageView iv = item.findViewById(R.id.iv_route_photo);
            try { com.example.harudiary.util.ImageUtil.setSafeImageURI(FriendRecordActivity.this, iv, r.getPhotoUri()); } catch (Exception ignored) {}

            final TimelineDTO rec = r;
            item.setOnClickListener(v -> onRoutePhotoClick(rec));

            llRoutePhotos.addView(item);
        }

        if (total > maxShow) {
            int overflow = total - maxShow;
            TextView tvBadge = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(54), dpToPx(54));
            tvBadge.setLayoutParams(lp);
            tvBadge.setBackgroundResource(R.drawable.bg_route_overflow);
            tvBadge.setGravity(0x11);
            tvBadge.setText("+" + overflow);
            tvBadge.setTextColor(Color.WHITE);
            tvBadge.setTextSize(14);
            llRoutePhotos.addView(tvBadge);
        }
    }

    private void onRoutePhotoClick(TimelineDTO record) {
        if ((record.getLatitude() != null && record.getLatitude() != 0) && kakaoMap != null) {
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(
                    LatLng.from(record.getLatitude(), record.getLongitude()), 16));
        }
        if (bsBehavior != null) {
            bsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        View target = activityIdToView.get(record.getId());
        if (target != null) {
            nestedScroll.postDelayed(() -> scrollToView(target), 300);
        }
    }

    private void renderDateList() {
        llDateList.removeAllViews();
        for (String date : allDates) {
            if (date.equals(currentDate)) continue;
            
            View row = LayoutInflater.from(this).inflate(R.layout.item_friend_date_row, llDateList, false);
            ((TextView) row.findViewById(R.id.tv_date)).setText(formatDateFull(date));
            ((TextView) row.findViewById(R.id.tv_title)).setText(formatTitle(date));

            // 내용과 썸네일은 보이지 않도록 숨김 처리 (가벼운 로딩)
            row.findViewById(R.id.tv_content).setVisibility(View.GONE);
            row.findViewById(R.id.iv_thumbnail).setVisibility(View.GONE);

            String d = date;
            row.setOnClickListener(v -> navigateTo(d));
            llDateList.addView(row);
        }
    }

    private void navigateTo(String date) {
        currentDate = date;
        nestedScroll.scrollTo(0, 0);
        render(); // Fetch new timeline via API
        bsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void startKakaoMap() {
        mapView.start(new MapLifeCycleCallback() {
            @Override public void onMapDestroy() {}
            @Override public void onMapError(Exception e) {
                Log.e(TAG, "지도 오류: " + e.getMessage());
                runOnUiThread(() -> {
                    TextView tvEmpty = findViewById(R.id.tv_map_empty);
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                });
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                mapStarted = true;
                kakaoMap   = map;
                addPhotoMarkers(map);

                map.setOnMapClickListener((kMap, position, screenPoint, poi) -> {
                    TimelineDTO nearest = findNearestRecord(position);
                    if (nearest != null) onMapMarkerTapped(nearest);
                });
            }
        });
    }

    private void addPhotoMarkers(KakaoMap map) {
        if (locatedRecords.isEmpty()) return;

        LabelLayer layer = map.getLabelManager() != null ? map.getLabelManager().getLayer() : null;
        if (layer == null) return;

        for (int i = 0; i < locatedRecords.size(); i++) {
            TimelineDTO r = locatedRecords.get(i);
            LatLng pos = LatLng.from(r.getLatitude(), r.getLongitude());
            Bitmap marker = buildCircleMarker(r, i + 1);

            LabelStyle style = LabelStyle.from(marker).setAnchorPoint(0.5f, 1.0f);
            LabelStyles styles = map.getLabelManager().addLabelStyles(LabelStyles.from(style));
            layer.addLabel(LabelOptions.from(pos).setStyles(styles));
        }

        TimelineDTO first = locatedRecords.get(0);
        int zoom = locatedRecords.size() == 1 ? 15 : 13;
        map.moveCamera(CameraUpdateFactory.newCenterPosition(
                LatLng.from(first.getLatitude(), first.getLongitude()), zoom));
    }

    private TimelineDTO findNearestRecord(LatLng tapped) {
        TimelineDTO nearest = null;
        double minDist = Double.MAX_VALUE;
        double threshold = 0.003; 

        for (TimelineDTO r : locatedRecords) {
            double dLat = r.getLatitude()  - tapped.latitude;
            double dLng = r.getLongitude() - tapped.longitude;
            double dist = dLat * dLat + dLng * dLng;
            if (dist < minDist && dist < threshold * threshold) {
                minDist = dist;
                nearest = r;
            }
        }
        return nearest;
    }

    private void onMapMarkerTapped(TimelineDTO record) {
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(
                LatLng.from(record.getLatitude(), record.getLongitude()), 16));

        if (bsBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            bsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        View target = activityIdToView.get(record.getId());
        if (target != null) {
            nestedScroll.postDelayed(() -> scrollToView(target), 400);
        }
    }

    private void scrollToView(View target) {
        try {
            int[] pos = new int[2];
            target.getLocationOnScreen(pos);
            int[] scrollPos = new int[2];
            nestedScroll.getLocationOnScreen(scrollPos);
            int offset = pos[1] - scrollPos[1] + nestedScroll.getScrollY();
            nestedScroll.smoothScrollTo(0, Math.max(0, offset - dpToPx(16)));
        } catch (Exception e) {
            Log.e(TAG, "scrollToView 오류: " + e.getMessage());
        }
    }

    private Bitmap buildCircleMarker(TimelineDTO record, int index) {
        int size   = dpToPx(52);
        int border = dpToPx(3);
        int tail   = dpToPx(8);
        int total  = size + tail;

        Bitmap bmp    = Bitmap.createBitmap(size, total, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(0xFF4CAF50);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, borderPaint);

        boolean drew = false;
        if (record.getPhotoUri() != null && !record.getPhotoUri().isEmpty()) {
            Bitmap circle = loadCircleBitmap(record.getPhotoUri(), size - border * 2);
            if (circle != null) {
                canvas.drawBitmap(circle, border, border, null);
                drew = true;
            }
        }
        if (!drew) {
            Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
            inner.setColor(slotColor(record.getTimeSlot()));
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - border, inner);

            Paint num = new Paint(Paint.ANTI_ALIAS_FLAG);
            num.setColor(Color.WHITE);
            num.setTextSize(dpToPx(14));
            num.setTextAlign(Paint.Align.CENTER);
            float numY = size / 2f - (num.ascent() + num.descent()) / 2f;
            canvas.drawText(String.valueOf(index), size / 2f, numY, num);
        }

        Path tail2 = new Path();
        float cx = size / 2f;
        tail2.moveTo(cx - dpToPx(5), size);
        tail2.lineTo(cx + dpToPx(5), size);
        tail2.lineTo(cx, size + tail);
        tail2.close();
        Paint tailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tailPaint.setColor(0xFF4CAF50);
        canvas.drawPath(tail2, tailPaint);

        return bmp;
    }

    private Bitmap loadCircleBitmap(String uriStr, int size) {
        try {
            Uri uri = Uri.parse(uriStr);
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            int ratio = Math.max(opts.outWidth / size, opts.outHeight / size);
            opts.inSampleSize   = Math.max(ratio, 1);
            opts.inJustDecodeBounds = false;

            is = getContentResolver().openInputStream(uri);
            Bitmap orig = BitmapFactory.decodeStream(is, null, opts);
            if (orig == null) return null;
            is.close();

            int minEdge = Math.min(orig.getWidth(), orig.getHeight());
            Bitmap cropped = Bitmap.createBitmap(orig,
                    (orig.getWidth() - minEdge) / 2, (orig.getHeight() - minEdge) / 2,
                    minEdge, minEdge);
            Bitmap scaled = Bitmap.createScaledBitmap(cropped, size, size, true);

            Bitmap circle = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(circle);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            cv.drawCircle(size / 2f, size / 2f, size / 2f, p);
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            cv.drawBitmap(scaled, 0, 0, p);
            return circle;
        } catch (Exception e) {
            Log.e(TAG, "loadCircleBitmap 오류: " + e.getMessage());
            return null;
        }
    }

    private int slotColor(String slot) {
        if ("morning".equals(slot)) return 0xFFF5A623;
        if ("lunch".equals(slot))   return 0xFF4CAF50;
        if ("evening".equals(slot)) return 0xFF9C6BC4;
        return 0xFF5B8DEF;
    }

    private String buildStars(float rating) {
        int full = Math.round(rating);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < full ? "★" : "☆");
        return sb.toString();
    }

    private String formatTitle(String date) {
        try {
            String[] p = date.split("-");
            int y=Integer.parseInt(p[0]), m=Integer.parseInt(p[1]), d=Integer.parseInt(p[2]);
            Calendar cal = Calendar.getInstance();
            cal.set(y, m-1, d);
            String[] days={"일","월","화","수","목","금","토"};
            return m + "월 " + d + "일 (" + days[cal.get(Calendar.DAY_OF_WEEK)-1] + ")";
        } catch (Exception e) { return date; }
    }

    private String formatDateFull(String date) {
        try {
            String[] p = date.split("-");
            int y=Integer.parseInt(p[0]), m=Integer.parseInt(p[1]), d=Integer.parseInt(p[2]);
            Calendar cal = Calendar.getInstance();
            cal.set(y, m-1, d);
            String[] days={"일","월","화","수","목","금","토"};
            return String.format("%d.%02d.%02d (%s)", y, m, d, days[cal.get(Calendar.DAY_OF_WEEK)-1]);
        } catch (Exception e) { return date; }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override protected void onResume() {
        super.onResume();
        if (mapStarted && mapView != null) mapView.resume();
    }

    @Override protected void onPause() {
        super.onPause();
        if (mapStarted && mapView != null) mapView.pause();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) { mapView.finish(); mapView = null; }
        mapStarted = false;
        kakaoMap   = null;
    }
}
