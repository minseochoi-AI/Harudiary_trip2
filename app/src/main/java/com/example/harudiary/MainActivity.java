package com.example.harudiary;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.harudiary.activity.RecordActivity;
import com.example.harudiary.fragment.DailyFragment;
import com.example.harudiary.fragment.FriendListFragment;
import com.example.harudiary.fragment.HomeFragment;
import com.example.harudiary.fragment.ListFragment;
import com.example.harudiary.fragment.ProfileFragment;
import com.example.harudiary.fragment.RecordListFragment;

public class MainActivity extends AppCompatActivity {

    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ━━━ Edge-to-Edge: 앱이 시스템 바 뒤까지 그림 ━━━
        // 이후 fragment_container 상단 / bottom_nav 하단에 inset 패딩을 직접 적용
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // 상태바 배경 투명, 아이콘 어둡게
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        // 시스템 내비바 배경 흰색
        getWindow().setNavigationBarColor(Color.WHITE);

        WindowInsetsControllerCompat wic =
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(true);
        wic.setAppearanceLightNavigationBars(true);

        // ━━━ Inset 적용 ━━━
        applyWindowInsets();

        if (savedInstanceState == null) showTab(0);
        setupBottomNav();
        setupBackButton();
    }

    /**
     * ★ 핵심 수정:
     *  - fragment_container : 상단 = statusBar inset → 콘텐츠가 상태바 아래에서 시작
     *  - bottom_nav         : 하단 = navigationBar inset → 탭바가 시스템 내비바 위에 배치
     */
    private void applyWindowInsets() {
        FrameLayout container = findViewById(R.id.fragment_container);
        LinearLayout bottomNav = findViewById(R.id.bottom_nav);

        ViewCompat.setOnApplyWindowInsetsListener(
            getWindow().getDecorView(),
            (v, insets) -> {
                // 상태바 높이
                int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                // 시스템 내비게이션바 높이 (제스처 모드에서는 0)
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

                // fragment_container: 위쪽에 상태바만큼 패딩 추가
                container.setPadding(0, topInset, 0, 0);

                // bottom_nav: 아래쪽에 내비게이션바만큼 패딩 추가
                // (wrap_content이므로 패딩 추가로 자동으로 키가 늘어남)
                bottomNav.setPadding(
                    bottomNav.getPaddingLeft(),
                    bottomNav.getPaddingTop(),
                    bottomNav.getPaddingRight(),
                    bottomInset  // 시스템 내비바 높이만큼 여백
                );

                return WindowInsetsCompat.CONSUMED;
            }
        );
    }

    // ── 뒤로가기 ────────────────────────────────────────────────

    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else if (currentTab != 0) {
                    showTab(0);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // ── 탭 전환 ─────────────────────────────────────────────────

    public void showTab(int tab) {
        currentTab = tab;

        androidx.fragment.app.Fragment fragment;
        switch (tab) {
            case 1:  fragment = new ListFragment();       break;
            case 2:  fragment = new FriendListFragment(); break;
            case 3:  fragment = new ProfileFragment();    break;
            default: fragment = new HomeFragment();       break;
        }

        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        updateNavHighlight(tab);
    }

    private void updateNavHighlight(int tab) {
        int[] iconIds = {
            R.id.nav_home_icon,
            R.id.nav_list_icon,
            R.id.nav_friend_icon,
            R.id.nav_settings_icon
        };
        // 이모지는 색상 틴트로 활성/비활성 표현 (알파값 조절)
        float activeAlpha   = 1.0f;
        float inactiveAlpha = 0.4f;

        for (int i = 0; i < iconIds.length; i++) {
            android.widget.TextView icon = findViewById(iconIds[i]);
            if (icon != null) {
                icon.setAlpha(i == tab ? activeAlpha : inactiveAlpha);
            }
        }
    }

    // ── Bottom Nav 셋업 ─────────────────────────────────────────

    private void setupBottomNav() {
        LinearLayout navHome     = findViewById(R.id.nav_home);
        LinearLayout navList     = findViewById(R.id.nav_list);
        LinearLayout navFriend   = findViewById(R.id.nav_friend);
        LinearLayout navSettings = findViewById(R.id.nav_settings);
        View navAdd = findViewById(R.id.nav_add);

        navHome.setOnClickListener(v -> showTab(0));
        navList.setOnClickListener(v -> showTab(1));
        navFriend.setOnClickListener(v -> showTab(2));
        navSettings.setOnClickListener(v -> showTab(3));
        navAdd.setOnClickListener(v -> {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            String[] options = {"📝 기록 작성", "✈️ 여행 계획"};
            builder.setItems(options, (dialog, which) -> {
                if (which == 1) {
                    // 여행 계획 선택
                    startActivityForResult(new Intent(this, com.example.harudiary.activity.PlanInputActivity.class), 100);
                } else {
                    // 기록 작성 선택
                    startActivity(new Intent(this, RecordActivity.class));
                }
            });
            builder.show();
        });

        updateNavHighlight(0);
    }

    // ── Fragment에서 호출 ────────────────────────────────────────

    public void navigateToDaily(String date) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, DailyFragment.newInstance(date))
                .addToBackStack(null)
                .commit();
    }

    public void navigateToRecordEdit(com.example.harudiary.model.Record record) {
        Intent intent = new Intent(this, RecordActivity.class);
        intent.putExtra("EXTRA_DIARY_ID", record.getActivityId());
        intent.putExtra(RecordActivity.EXTRA_DATE, record.getDate());
        intent.putExtra(RecordActivity.EXTRA_SLOT, record.getTimeSlot());
        intent.putExtra("EXTRA_CONTENT", record.getContent());
        intent.putExtra("EXTRA_PHOTO_URI", record.getPhotoUri());
        intent.putExtra("EXTRA_RATING", record.getRating());
        intent.putExtra("EXTRA_ADDRESS", record.getAddress());
        intent.putExtra("EXTRA_WEATHER", record.getWeather());
        intent.putExtra("EXTRA_TEMPERATURE", record.getTemperature());
        intent.putExtra(RecordActivity.EXTRA_PREFILL_LAT, record.getLatitude());
        intent.putExtra(RecordActivity.EXTRA_PREFILL_LNG, record.getLongitude());
        startActivity(intent);
    }

    public void navigateToRecord() {
        startActivity(new Intent(this, RecordActivity.class));
    }

    public void navigateToRecordWithSlot(String date, String slot) {
        Intent intent = new Intent(this, RecordActivity.class);
        intent.putExtra(RecordActivity.EXTRA_DATE, date);
        intent.putExtra(RecordActivity.EXTRA_SLOT, slot);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            String date = data != null ? data.getStringExtra("date") : null;
            if (date != null) {
                navigateToDaily(date);
            } else {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA);
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));
                navigateToDaily(sdf.format(new java.util.Date()));
            }
        }
    }
}
