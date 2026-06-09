package com.example.harudiary.fragment;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.MainActivity;
import com.example.harudiary.R;
import com.example.harudiary.activity.FriendRecordActivity;
import com.example.harudiary.activity.SettingsActivity;
import com.example.harudiary.adapter.CalendarAdapter;
import com.example.harudiary.adapter.ActivityListAdapter;
import com.example.harudiary.adapter.FriendBrowseAdapter;
import com.example.harudiary.api.DiaryApi;
import com.example.harudiary.api.FriendApi;
import com.example.harudiary.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.harudiary.model.FriendRecord;
import com.example.harudiary.model.Record;
import com.example.harudiary.model.User;
import com.example.harudiary.util.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * HomeFragment — D·LOG 스타일 메인 홈
 * 스트릭, 주간/월간 캘린더 토글, 오늘의 질문, 오늘 사진
 */
public class HomeFragment extends Fragment {

    /** 요일별 오늘의 질문 (0=일요일) */
    private static final String[] DAILY_QUESTIONS = {
        "이번 주, 가장 기억에 남는 순간은 무엇인가요? ✨",
        "새로운 한 주가 시작됐어요! 오늘 하루 어떤 목표가 있나요? 🌱",
        "오늘 가장 집중한 일은 무엇이었나요? 💪",
        "이번 주 중반이에요. 지금 기분은 어떤가요? 😊",
        "오늘 감사했던 일을 하나 기록해볼까요? 🙏",
        "한 주를 마무리하며, 이번 주에 잘한 일은 뭐가 있나요? 🎉",
        "주말이에요! 오늘 특별한 계획이 있나요? ☀️"
    };

    private int currentYear, currentMonth;
    private int todayYear, todayMonth, todayDay;
    private int selectedDay;
    // 오늘 섹션에서 보는 날짜 (◀▶ 이동용)
    private int viewDay, viewMonth, viewYear;

    private TextView tvMonth, tvEmpty, tvStreak, tvMonthlyCount;
    private TextView tvTodayDay, tvLastRecordTime, tvDearTitle, tvDailyQuestion;
    private LinearLayout llTodayThumbs, llTodayPhotos;
    private TextView tvNoPhotos;
    private RecyclerView rvCalendar, rvRecent;
    private CalendarAdapter calendarAdapter;
    private ActivityListAdapter activityListAdapter;

    private LinearLayout sectionFriendBrowse;
    private RecyclerView rvFriendBrowse;
    private TextView tvFriendBrowseEmpty, tvBadge;
    private FriendBrowseAdapter friendBrowseAdapter;

    private String userIdStr;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        int userIdInt = new SessionManager(requireContext()).getLoggedInUserId();
        userIdStr = String.valueOf(userIdInt);

        Calendar today = Calendar.getInstance();
        todayYear  = today.get(Calendar.YEAR);
        todayMonth = today.get(Calendar.MONTH);
        todayDay   = today.get(Calendar.DAY_OF_MONTH);
        currentYear = todayYear; currentMonth = todayMonth;
        selectedDay = todayDay;
        viewYear = todayYear; viewMonth = todayMonth; viewDay = todayDay;

        // ── 뷰 바인딩 ──────────────────────────────────
        tvMonth          = view.findViewById(R.id.tv_month);
        tvEmpty          = view.findViewById(R.id.tv_empty);
        tvStreak         = view.findViewById(R.id.tv_streak);
        tvMonthlyCount   = view.findViewById(R.id.tv_monthly_count);
        tvTodayDay       = view.findViewById(R.id.tv_today_day);
        tvLastRecordTime = view.findViewById(R.id.tv_last_record_time);
        tvDearTitle      = view.findViewById(R.id.tv_dear_title);
        tvDailyQuestion  = view.findViewById(R.id.tv_daily_question);
        llTodayThumbs    = view.findViewById(R.id.ll_today_thumbs);
        llTodayPhotos    = view.findViewById(R.id.ll_today_photos);
        tvNoPhotos       = view.findViewById(R.id.tv_no_photos);
        rvCalendar       = view.findViewById(R.id.rv_calendar);
        rvRecent         = view.findViewById(R.id.rv_recent);
        sectionFriendBrowse = view.findViewById(R.id.section_friend_browse);
        rvFriendBrowse      = view.findViewById(R.id.rv_friend_browse);
        tvFriendBrowseEmpty = view.findViewById(R.id.tv_friend_browse_empty);
        tvBadge             = view.findViewById(R.id.tv_badge);

        // 캘린더
        rvCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        rvCalendar.setHasFixedSize(false);
        rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecent.setHasFixedSize(false);
        rvFriendBrowse.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        // ── 버튼 리스너 ───────────────────────────────
        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            if (currentMonth == 0) { currentMonth = 11; currentYear--; }
            else currentMonth--;
            selectedDay = 0;
            loadCalendar();
        });
        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            if (currentMonth == 11) { currentMonth = 0; currentYear++; }
            else currentMonth++;
            selectedDay = 0;
            loadCalendar();
        });

        // 접기/펼치기
        view.findViewById(R.id.btn_toggle_calendar).setOnClickListener(v -> toggleCalendar());

        // 오늘 날짜 ◀▶
        view.findViewById(R.id.btn_prev_day).setOnClickListener(v -> navigateDay(-1));
        view.findViewById(R.id.btn_next_day).setOnClickListener(v -> navigateDay(+1));

        // 친구 검색
        view.findViewById(R.id.btn_friend_search).setOnClickListener(v -> {
            FriendSearchDialogFragment dialog = new FriendSearchDialogFragment();
            dialog.setOnFriendAddedListener(this::refreshFriendSection);
            dialog.show(getChildFragmentManager(), "friend_search");
        });

        // 알림
        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            FriendRequestsDialogFragment dialog = new FriendRequestsDialogFragment();
            dialog.setOnRequestHandledListener(this::refreshFriendSection);
            dialog.show(getChildFragmentManager(), "friend_requests");
        });

        // 오늘의 질문 + 버튼
        view.findViewById(R.id.btn_add_from_question).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToRecord();
            }
        });

        // 설정 (hidden, 호환성)
        view.findViewById(R.id.tv_settings).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), SettingsActivity.class)));

        loadAll();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userIdStr != null) loadAll();
    }

    private void loadAll() {
        loadCalendar();
        loadStreak();
        loadTodaySection();
        loadDailyQuestion();
        loadTodayPhotos();
        loadFriendBrowse();
        updateBadge();
    }

    // ── 캘린더 ─────────────────────────────────────────

    private void loadCalendar() {
        tvMonth.setText(currentYear + "." + String.format("%02d", currentMonth + 1));

        String yearMonth = String.format("%04d-%02d", currentYear, currentMonth + 1);
        int todayInMonth = (currentYear == todayYear && currentMonth == todayMonth) ? todayDay : 0;

        DiaryApi diaryApi = RetrofitClient.getClient().create(DiaryApi.class);
        diaryApi.getRecordDates(userIdStr, yearMonth).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(@NonNull Call<List<String>> call, @NonNull Response<List<String>> response) {
                Set<String> recordDates = new java.util.HashSet<>();
                if (response.isSuccessful() && response.body() != null) {
                    recordDates.addAll(response.body());
                }
                updateCalendarUI(recordDates, todayInMonth);
            }

            @Override
            public void onFailure(@NonNull Call<List<String>> call, @NonNull Throwable t) {
                updateCalendarUI(new java.util.HashSet<>(), todayInMonth);
            }
        });
    }

    private void updateCalendarUI(Set<String> recordDates, int todayInMonth) {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            if (calendarAdapter == null) {
                calendarAdapter = new CalendarAdapter(currentYear, currentMonth, selectedDay,
                        todayInMonth, recordDates, this::onDateClick);
            } else {
                calendarAdapter.updateMonth(currentYear, currentMonth, selectedDay,
                        todayInMonth, recordDates);
            }
            rvCalendar.setAdapter(calendarAdapter);
        });
    }

    private void toggleCalendar() {
        if (calendarAdapter == null) return;
        boolean toWeek = !calendarAdapter.isWeekMode();
        calendarAdapter.setWeekMode(toWeek);

        // 부드러운 높이 애니메이션
        rvCalendar.post(() -> {
            int fromH = rvCalendar.getHeight();
            rvCalendar.measure(
                View.MeasureSpec.makeMeasureSpec(rvCalendar.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int toH = rvCalendar.getMeasuredHeight();

            ValueAnimator anim = ValueAnimator.ofInt(fromH, toH);
            anim.setDuration(250);
            anim.addUpdateListener(a -> {
                ViewGroup.LayoutParams lp = rvCalendar.getLayoutParams();
                lp.height = (int) a.getAnimatedValue();
                rvCalendar.setLayoutParams(lp);
            });
            anim.start();
        });
    }

    // ── 스트릭 + 이달 기록 ──────────────────────────────

    private void loadStreak() {
        DiaryApi diaryApi = RetrofitClient.getClient().create(DiaryApi.class);
        String yearMonth = String.format("%04d-%02d", todayYear, todayMonth + 1);

        diaryApi.getStreak(userIdStr).enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(@NonNull Call<Integer> call, @NonNull Response<Integer> response) {
                int streak = (response.isSuccessful() && response.body() != null) ? response.body() : 0;
                updateStreakUI(streak);
            }
            @Override
            public void onFailure(@NonNull Call<Integer> call, @NonNull Throwable t) { updateStreakUI(0); }
        });

        diaryApi.getMonthlyCount(userIdStr, yearMonth).enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(@NonNull Call<Integer> call, @NonNull Response<Integer> response) {
                int count = (response.isSuccessful() && response.body() != null) ? response.body() : 0;
                updateMonthlyCountUI(count);
            }
            @Override
            public void onFailure(@NonNull Call<Integer> call, @NonNull Throwable t) { updateMonthlyCountUI(0); }
        });
    }

    private void updateStreakUI(int streak) {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> tvStreak.setText(streak + "일째 작성 중"));
    }

    private void updateMonthlyCountUI(int count) {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> tvMonthlyCount.setText("🌸" + count + "일"));
    }

    // ── 오늘 섹션 (Today 숫자 + 마지막 기록 시각) ──────

    private void navigateDay(int delta) {
        Calendar cal = Calendar.getInstance();
        cal.set(viewYear, viewMonth, viewDay);
        cal.add(Calendar.DAY_OF_MONTH, delta);
        viewYear  = cal.get(Calendar.YEAR);
        viewMonth = cal.get(Calendar.MONTH);
        viewDay   = cal.get(Calendar.DAY_OF_MONTH);
        loadTodaySection();
        loadTodayPhotos();
    }

    private void loadTodaySection() {
        String dateStr = String.format("%04d-%02d-%02d", viewYear, viewMonth + 1, viewDay);

        tvTodayDay.setText(String.valueOf(viewDay));
        SessionManager sm = new SessionManager(requireContext());
        String name = sm.getLoggedInUserName();
        if (name == null || name.isEmpty()) name = "D·LOGGER";
        tvDearTitle.setText("DEAR. " + name + "님");
        
        tvTodayDay.setOnClickListener(v -> onDateClick(dateStr));
        tvLastRecordTime.setOnClickListener(v -> onDateClick(dateStr));

        DiaryApi diaryApi = RetrofitClient.getClient().create(DiaryApi.class);
        diaryApi.getActivitiesByDate(userIdStr, dateStr).enqueue(new Callback<List<Record>>() {
            @Override
            public void onResponse(@NonNull Call<List<Record>> call, @NonNull Response<List<Record>> response) {
                List<Record> dayRecords = (response.isSuccessful() && response.body() != null) ? response.body() : new ArrayList<>();
                updateTodaySectionUI(dayRecords);
            }
            @Override
            public void onFailure(@NonNull Call<List<Record>> call, @NonNull Throwable t) { updateTodaySectionUI(new ArrayList<>()); }
        });
    }
    
    private void updateTodaySectionUI(List<Record> dayRecords) {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            if (dayRecords.isEmpty()) {
                tvLastRecordTime.setText("아직 기록이 없어요");
            } else {
                Record last = dayRecords.get(dayRecords.size() - 1);
                String time = DateFormat.format("오후 h:mm", new Date(last.getTimestamp())).toString();
                tvLastRecordTime.setText(time + "에 마지막으로 기록했어요 >");
            }

            llTodayThumbs.removeAllViews();
            int count = Math.min(dayRecords.size(), 3);
            for (int i = 0; i < count; i++) {
                Record r = dayRecords.get(i);
                if (r.getPhotoUri() == null || r.getPhotoUri().isEmpty()) continue;
                ImageView iv = new ImageView(requireContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44));
                lp.setMarginStart(dpToPx(4));
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackgroundResource(R.drawable.bg_thumbnail);
                try { iv.setImageURI(Uri.parse(r.getPhotoUri())); } catch (Exception ignored) {}
                llTodayThumbs.addView(iv);
            }
        });
    }

    // ── 오늘의 질문 ─────────────────────────────────────

    private void loadDailyQuestion() {
        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=일
        tvDailyQuestion.setText(DAILY_QUESTIONS[dow]);
    }

    // ── 오늘 사진 스트립 ─────────────────────────────────

    private void loadTodayPhotos() {
        String dateStr = String.format("%04d-%02d-%02d", viewYear, viewMonth + 1, viewDay);
        DiaryApi diaryApi = RetrofitClient.getClient().create(DiaryApi.class);
        diaryApi.getActivitiesByDate(userIdStr, dateStr).enqueue(new Callback<List<Record>>() {
            @Override
            public void onResponse(@NonNull Call<List<Record>> call, @NonNull Response<List<Record>> response) {
                List<Record> records = (response.isSuccessful() && response.body() != null) ? response.body() : new ArrayList<>();
                updateTodayPhotosUI(records, dateStr);
            }
            @Override
            public void onFailure(@NonNull Call<List<Record>> call, @NonNull Throwable t) { updateTodayPhotosUI(new ArrayList<>(), dateStr); }
        });
    }
    
    private void updateTodayPhotosUI(List<Record> records, String dateStr) {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            llTodayPhotos.removeAllViews();
            List<Record> withPhotos = new ArrayList<>();
            for (Record r : records) {
                if (r.getPhotoUri() != null && !r.getPhotoUri().isEmpty()) withPhotos.add(r);
            }

            if (withPhotos.isEmpty()) {
                llTodayPhotos.setVisibility(View.GONE);
                tvNoPhotos.setVisibility(View.VISIBLE);
            } else {
                llTodayPhotos.setVisibility(View.VISIBLE);
                tvNoPhotos.setVisibility(View.GONE);
                for (Record r : withPhotos) {
                    View item = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_today_photo, llTodayPhotos, false);
                    ImageView iv = item.findViewById(R.id.iv_photo);
                    TextView tvSlot = item.findViewById(R.id.tv_slot);
                    try { iv.setImageURI(Uri.parse(r.getPhotoUri())); } catch (Exception ignored) {}
                    String slot = r.getTimeSlot();
                    if (slot != null) {
                        tvSlot.setText(slotToKorean(slot));
                        tvSlot.setVisibility(View.VISIBLE);
                    } else {
                        tvSlot.setVisibility(View.GONE);
                    }
                    item.setOnClickListener(v -> onDateClick(dateStr));
                    llTodayPhotos.addView(item);
                }
            }
        });
    }

    // ── 친구 둘러보기 ────────────────────────────────────

    private void loadFriendBrowse() {
        FriendApi friendApi = RetrofitClient.getClient().create(FriendApi.class);
        friendApi.getFriendBrowseList(userIdStr).enqueue(new Callback<List<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<java.util.Map<String, Object>>> call, @NonNull Response<List<java.util.Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FriendRecord> records = new ArrayList<>();
                    for (java.util.Map<String, Object> map : response.body()) {
                        FriendRecord r = new FriendRecord();
                        r.setUserId((String) map.get("userId"));
                        r.setUserName((String) map.get("userName"));
                        r.setDate((String) map.get("date"));
                        r.setLatestActivity((String) map.get("latestActivity"));
                        r.setPhotoUri((String) map.get("photoUri"));
                        records.add(r);
                    }
                    updateFriendBrowseUI(records);
                } else {
                    updateFriendBrowseUI(new ArrayList<>());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<java.util.Map<String, Object>>> call, @NonNull Throwable t) {
                updateFriendBrowseUI(new ArrayList<>());
            }
        });
    }
    
    private void updateFriendBrowseUI(List<FriendRecord> finalRecords) {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            boolean hasFriends = true; // 서버에서 빈 리스트면 친구가 없거나 기록이 없는 것
            sectionFriendBrowse.setVisibility(View.VISIBLE);
            if (finalRecords.isEmpty()) {
                rvFriendBrowse.setVisibility(View.GONE);
                tvFriendBrowseEmpty.setVisibility(View.VISIBLE);
            } else {
                rvFriendBrowse.setVisibility(View.VISIBLE);
                tvFriendBrowseEmpty.setVisibility(View.GONE);
                if (friendBrowseAdapter == null) {
                    friendBrowseAdapter = new FriendBrowseAdapter(finalRecords, this::onFriendRecordClick);
                } else {
                    friendBrowseAdapter.update(finalRecords);
                }
                rvFriendBrowse.setAdapter(friendBrowseAdapter);
            }
        });
    }

    private void onFriendRecordClick(FriendRecord record) {
        Intent intent = new Intent(requireContext(), FriendRecordActivity.class);
        intent.putExtra(FriendRecordActivity.EXTRA_FRIEND_USER_ID, record.getUserId());
        intent.putExtra(FriendRecordActivity.EXTRA_FRIEND_NAME, record.getUserName());
        intent.putExtra(FriendRecordActivity.EXTRA_TARGET_DATE, record.getDate());
        startActivity(intent);
    }

    private void updateBadge() {
        FriendApi friendApi = RetrofitClient.getClient().create(FriendApi.class);
        friendApi.getPendingRequestCount(userIdStr).enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(@NonNull Call<Integer> call, @NonNull Response<Integer> response) {
                int count = (response.isSuccessful() && response.body() != null) ? response.body() : 0;
                updateBadgeUI(count);
            }
            @Override
            public void onFailure(@NonNull Call<Integer> call, @NonNull Throwable t) { updateBadgeUI(0); }
        });
    }
    
    private void updateBadgeUI(int count) {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            if (count > 0) {
                tvBadge.setVisibility(View.VISIBLE);
                tvBadge.setText(count > 9 ? "9+" : String.valueOf(count));
            } else { tvBadge.setVisibility(View.GONE); }
        });
    }

    private void refreshFriendSection() { loadFriendBrowse(); updateBadge(); }

    private void onDateClick(String date) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToDaily(date);
        }
    }

    private static String slotToKorean(String slot) {
        if ("morning".equals(slot)) return "아침";
        if ("lunch".equals(slot))   return "점심";
        if ("evening".equals(slot)) return "저녁";
        return "기타";
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
