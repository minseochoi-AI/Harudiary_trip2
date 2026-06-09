package com.example.harudiary.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.harudiary.R;
import com.example.harudiary.model.Record;
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.api.TravelApi;
import com.example.harudiary.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import android.app.DatePickerDialog;

public class RecordActivity extends AppCompatActivity {

    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");

    private static final int LOCATION_TIMEOUT_MS = 10_000;
    private final Handler locationTimeoutHandler = new Handler(Looper.getMainLooper());

    public static final String EXTRA_DATE = "extra_date";
    public static final String EXTRA_SLOT = "extra_slot";

    private static final int REQ_PHOTO    = 1001;
    private static final int REQ_LOCATION = 1002;

    private String selectedSlot = null;   // null = 시간대 미지정
    private String selectedDate;
    private String selectedPhotoUri = null;

    private double currentLat = 0, currentLng = 0;
    private String currentAddress = "";
    private String currentWeather = "";
    private float  currentTemperature = 0f;

    private TextView btnMorning, btnLunch, btnEvening;
    private TextView tvAutoDatetime, tvAutoWeather, tvLocation, tvEndDate;
    private ImageView ivPhoto;
    private android.widget.EditText etContent;
    private RatingBar rbRating;
    private android.widget.Button btnGenerateTravelPlan, btnSelectEndDate;

    private int calculatedDays = 1; // 기본 1일 (당일치기)

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);

        initViews();
        applyIntentExtras();
        setAutoDatetime();
        setupSlotButtons();
        setupPhotoButton();
        setupSaveClose();
        requestLocationAndFetchData();
    }

    private void initViews() {
        btnMorning     = findViewById(R.id.btn_morning);
        btnLunch       = findViewById(R.id.btn_lunch);
        btnEvening     = findViewById(R.id.btn_evening);
        tvAutoDatetime = findViewById(R.id.tv_auto_datetime);
        tvAutoWeather  = findViewById(R.id.tv_auto_weather);
        tvLocation     = findViewById(R.id.tv_location);
        tvEndDate      = findViewById(R.id.tv_end_date);
        ivPhoto        = findViewById(R.id.iv_photo);
        etContent      = findViewById(R.id.et_content);
        rbRating       = findViewById(R.id.rb_rating);
        btnGenerateTravelPlan = findViewById(R.id.btn_generate_travel_plan);
        btnSelectEndDate      = findViewById(R.id.btn_select_end_date);

        btnGenerateTravelPlan.setOnClickListener(v -> generateTravelPlan());
        btnSelectEndDate.setOnClickListener(v -> showDatePickerDialog());

        rbRating.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });
    }

    private void applyIntentExtras() {
        selectedDate = getIntent().getStringExtra(EXTRA_DATE);
        if (selectedDate == null || selectedDate.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            sdf.setTimeZone(KST);
            selectedDate = sdf.format(new Date());
        }
        // ★ EXTRA_SLOT이 명시적으로 전달된 경우에만 시간대 선택, 아니면 null(미선택) 상태 유지
        String extraSlot = getIntent().getStringExtra(EXTRA_SLOT);
        selectedSlot = (extraSlot != null && !extraSlot.isEmpty()) ? extraSlot : null;
    }

    private void setAutoDatetime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA);
        sdf.setTimeZone(KST);
        tvAutoDatetime.setText(sdf.format(new Date()));
    }

    private void setupSlotButtons() {
        updateSlotUI();
        btnMorning.setOnClickListener(v -> toggleSlot("morning"));
        btnLunch.setOnClickListener(v -> toggleSlot("lunch"));
        btnEvening.setOnClickListener(v -> toggleSlot("evening"));
    }

    /** 같은 슬롯 재클릭 → 해제(null), 다른 슬롯 → 선택 */
    private void toggleSlot(String slot) {
        selectedSlot = slot.equals(selectedSlot) ? null : slot;
        updateSlotUI();
    }

    private void updateSlotUI() {
        setSlotSelected(btnMorning, "morning".equals(selectedSlot));
        setSlotSelected(btnLunch,   "lunch".equals(selectedSlot));
        setSlotSelected(btnEvening, "evening".equals(selectedSlot));
    }

    private void setSlotSelected(TextView btn, boolean selected) {
        if (selected) {
            btn.setBackgroundResource(R.drawable.bg_time_slot_selected);
            btn.setTextColor(getColor(R.color.white));
        } else {
            btn.setBackgroundResource(R.drawable.bg_time_slot_default);
            btn.setTextColor(getColor(R.color.gray_label));
        }
    }

    private void setupPhotoButton() {
        ivPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQ_PHOTO);
        });
    }

    private void setupSaveClose() {
        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> saveRecord());
    }

    private void showDatePickerDialog() {
        if (selectedDate == null) return;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            sdf.setTimeZone(KST);
            Date start = sdf.parse(selectedDate);
            Calendar cal = Calendar.getInstance(KST);
            if (start != null) cal.setTime(start);

            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar endCal = Calendar.getInstance(KST);
                endCal.set(year, month, dayOfMonth);
                
                try {
                    Date startObj = sdf.parse(selectedDate);
                    Date endObj = endCal.getTime();
                    
                    if (endObj.before(startObj)) {
                        Toast.makeText(this, "종료일은 시작일 이후여야 합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    long diffInMillis = endObj.getTime() - startObj.getTime();
                    calculatedDays = (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;
                    
                    String endDateStr = sdf.format(endObj);
                    tvEndDate.setText(String.format(Locale.KOREA, "%s (%d박 %d일)", endDateStr, calculatedDays - 1, calculatedDays));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateTravelPlan() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "활동 내용을 먼저 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, calculatedDays + "일 여행 일정 생성 중...", Toast.LENGTH_SHORT).show();

        TravelApi api = RetrofitClient.getClient().create(TravelApi.class);
        TravelApi.DiaryRecommendRequest req = new TravelApi.DiaryRecommendRequest(selectedDate, calculatedDays, currentLat, currentLng, content);
        
        api.recommendByDiary(req).enqueue(new retrofit2.Callback<com.example.harudiary.model.TravelPlanResponse>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<com.example.harudiary.model.TravelPlanResponse> call, @NonNull retrofit2.Response<com.example.harudiary.model.TravelPlanResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    runOnUiThread(() -> Toast.makeText(RecordActivity.this, "일정 생성 완료! " + response.body().getTripTitle(), Toast.LENGTH_LONG).show());
                    // 추가적으로 결과를 보여주는 액티비티 띄우기 등의 로직을 연동할 수 있습니다.
                } else {
                    runOnUiThread(() -> Toast.makeText(RecordActivity.this, "일정 생성 실패", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<com.example.harudiary.model.TravelPlanResponse> call, @NonNull Throwable t) {
                runOnUiThread(() -> Toast.makeText(RecordActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveRecord() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "활동 내용을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = new SessionManager(this).getUserId();

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("userId", userId);
        payload.put("date", selectedDate);
        payload.put("timeSlot", selectedSlot);
        payload.put("content", content);
        payload.put("photoUri", selectedPhotoUri);
        payload.put("rating", rbRating.getRating());
        payload.put("latitude", currentLat);
        payload.put("longitude", currentLng);
        payload.put("address", currentAddress);
        payload.put("weather", currentWeather);
        payload.put("temperature", currentTemperature);

        com.example.harudiary.api.RetrofitClient.getInstance().create(com.example.harudiary.api.DiaryApi.class).createDiary(payload).enqueue(new retrofit2.Callback<com.example.harudiary.model.Record>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<com.example.harudiary.model.Record> call, @NonNull retrofit2.Response<com.example.harudiary.model.Record> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RecordActivity.this, "기록이 저장되었습니다", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(RecordActivity.this, "저장에 실패했습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<com.example.harudiary.model.Record> call, @NonNull Throwable t) {
                Toast.makeText(RecordActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PHOTO && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                selectedPhotoUri = uri.toString();
                ivPhoto.setImageURI(uri);
            }
        }
    }

    private void requestLocationAndFetchData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION);
            return;
        }
        startLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            tvLocation.setText("위치 권한이 없습니다");
            tvAutoWeather.setText("날씨 정보 없음");
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            tvLocation.setText("위치 서비스 사용 불가");
            tvAutoWeather.setText("날씨 정보 없음");
            return;
        }

        Location last = null;
        try {
            last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last == null)
                last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) { }
        if (last != null) {
            onLocationAcquired(last);
            return;
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                locationTimeoutHandler.removeCallbacksAndMessages(null);
                onLocationAcquired(location);
                try { locationManager.removeUpdates(this); } catch (Exception ignored) { }
            }
        };

        boolean registered = false;
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                registered = true;
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                registered = true;
            }
        } catch (Exception e) {
            tvLocation.setText("위치 정보를 가져올 수 없습니다");
            tvAutoWeather.setText("날씨 정보 없음");
            return;
        }

        if (!registered) {
            tvLocation.setText("위치 서비스가 꺼져 있습니다");
            tvAutoWeather.setText("날씨 정보 없음");
            return;
        }

        locationTimeoutHandler.postDelayed(() -> {
            if (currentLat == 0 && currentLng == 0) {
                tvLocation.setText("위치를 가져올 수 없습니다 (GPS 확인)");
                tvAutoWeather.setText("날씨 정보 없음");
            }
        }, LOCATION_TIMEOUT_MS);
    }

    private void onLocationAcquired(Location location) {
        currentLat = location.getLatitude();
        currentLng = location.getLongitude();

        TravelApi api = RetrofitClient.getClient().create(TravelApi.class);
        api.getEnvCurrent(currentLat, currentLng).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<java.util.Map<String, Object>> call, @NonNull retrofit2.Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> body = response.body();
                    currentAddress = (String) body.get("address");
                    currentWeather = (String) body.get("weather");
                    currentTemperature = ((Number) body.get("temperature")).floatValue();

                    runOnUiThread(() -> {
                        tvLocation.setText("📍 " + currentAddress);
                        tvAutoWeather.setText(currentWeather + " " + currentTemperature + "℃");
                    });
                } else {
                    runOnUiThread(() -> {
                        tvLocation.setText("주소를 가져올 수 없습니다");
                        tvAutoWeather.setText("날씨 정보 없음");
                    });
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    tvLocation.setText("주소를 가져올 수 없습니다");
                    tvAutoWeather.setText("날씨 정보 없음");
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationTimeoutHandler.removeCallbacksAndMessages(null);
        if (locationManager != null && locationListener != null) {
            try { locationManager.removeUpdates(locationListener); } catch (Exception ignored) { }
        }
    }
}
