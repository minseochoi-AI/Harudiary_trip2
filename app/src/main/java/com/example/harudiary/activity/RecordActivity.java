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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.harudiary.R;
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.api.DiaryApi;
import com.example.harudiary.api.TravelApi;
import com.example.harudiary.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RecordActivity extends AppCompatActivity {

    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");
    private static final int LOCATION_TIMEOUT_MS = 10_000;
    private final Handler locationTimeoutHandler = new Handler(Looper.getMainLooper());

    public static final String EXTRA_DATE = "extra_date";
    public static final String EXTRA_SLOT = "extra_slot";
    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_PREFILL_CONTENT = "extra_prefill_content";
    public static final String EXTRA_PREFILL_ADDRESS = "extra_prefill_address";
    public static final String EXTRA_PREFILL_LAT = "extra_prefill_lat";
    public static final String EXTRA_PREFILL_LNG = "extra_prefill_lng";

    private static final int REQ_PHOTO    = 1001;
    private static final int REQ_LOCATION = 1002;

    private String selectedSlot = null;
    private String selectedDate;
    private String selectedPhotoUri = null;

    private double currentLat = 0, currentLng = 0;
    private String currentAddress = "";
    private String currentWeather = "";
    private float  currentTemperature = 0f;

    private TextView tvAutoDatetime, tvAutoWeather, tvLocation, tvActivityTitle;
    private ImageView ivPhoto;
    private android.widget.EditText etContent;
    private View layoutPhotoContainer;
    private com.google.android.material.button.MaterialButton btnAddPhoto;

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
        setupPhotoButton();
        setupSaveClose();
        requestLocationAndFetchData();
    }

    private void initViews() {
        tvAutoDatetime = findViewById(R.id.tv_auto_datetime);
        tvAutoWeather  = findViewById(R.id.tv_auto_weather);
        tvLocation     = findViewById(R.id.tv_location);
        tvActivityTitle = findViewById(R.id.tv_activity_title);
        ivPhoto        = findViewById(R.id.iv_photo);
        etContent      = findViewById(R.id.et_content);
        layoutPhotoContainer = findViewById(R.id.layout_photo_container);
        btnAddPhoto    = findViewById(R.id.btn_add_photo);
    }

    private void applyIntentExtras() {
        selectedDate = getIntent().getStringExtra(EXTRA_DATE);
        if (selectedDate == null || selectedDate.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            sdf.setTimeZone(KST);
            selectedDate = sdf.format(new Date());
        }
        
        selectedSlot = getIntent().getStringExtra(EXTRA_SLOT);

        String prefillContent = getIntent().getStringExtra(EXTRA_PREFILL_CONTENT);
        if (prefillContent != null) {
            etContent.setText(prefillContent);
            etContent.setHint("이 장소에서 어떤 추억을 남겼나요?");
            if (tvActivityTitle != null) tvActivityTitle.setText("방문 인증하기");
        }
        
        String prefillAddress = getIntent().getStringExtra(EXTRA_PREFILL_ADDRESS);
        if (prefillAddress != null) {
            currentAddress = prefillAddress;
            tvLocation.setText("📍 " + prefillAddress);
        }
        
        currentLat = getIntent().getDoubleExtra(EXTRA_PREFILL_LAT, 0);
        currentLng = getIntent().getDoubleExtra(EXTRA_PREFILL_LNG, 0);
    }

    private void setAutoDatetime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        sdf.setTimeZone(KST);
        tvAutoDatetime.setText(sdf.format(new Date()));
    }

    private void setupPhotoButton() {
        View.OnClickListener pickPhotoListener = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQ_PHOTO);
        };
        btnAddPhoto.setOnClickListener(pickPhotoListener);
        ivPhoto.setOnClickListener(pickPhotoListener);
        
        findViewById(R.id.btn_remove_photo).setOnClickListener(v -> {
            selectedPhotoUri = null;
            layoutPhotoContainer.setVisibility(View.GONE);
            btnAddPhoto.setVisibility(View.VISIBLE);
        });
    }

    private void setupSaveClose() {
        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> saveRecord());
    }

    private String getAutoTimeSlot() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) return "morning";
        if (hour >= 11 && hour < 17) return "lunch";
        return "evening";
    }

    private void saveRecord() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "활동 내용을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = new SessionManager(this).getUserId();
        String slot = (selectedSlot != null) ? selectedSlot : getAutoTimeSlot();

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("userId", userId);
        payload.put("date", selectedDate);
        payload.put("timeSlot", slot);
        payload.put("content", content);
        payload.put("photoUri", selectedPhotoUri);
        payload.put("rating", 0.0f); // 별점 삭제됨
        payload.put("latitude", currentLat);
        payload.put("longitude", currentLng);
        payload.put("address", currentAddress);
        payload.put("weather", currentWeather);
        payload.put("temperature", currentTemperature);

        RetrofitClient.getInstance().create(DiaryApi.class).createDiary(payload).enqueue(new retrofit2.Callback<com.example.harudiary.model.Record>() {
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
                Toast.makeText(RecordActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
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
                layoutPhotoContainer.setVisibility(View.VISIBLE);
                btnAddPhoto.setVisibility(View.GONE);
            }
        }
    }

    private void requestLocationAndFetchData() {
        if (currentLat != 0 && currentLng != 0) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            return;
        }
        startLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) return;

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                locationTimeoutHandler.removeCallbacksAndMessages(null);
                onLocationAcquired(location);
                try { locationManager.removeUpdates(this); } catch (Exception ignored) {}
            }
        };

        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        } catch (Exception ignored) {}
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
                }
            }
            @Override public void onFailure(@NonNull retrofit2.Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationTimeoutHandler.removeCallbacksAndMessages(null);
        if (locationManager != null && locationListener != null) {
            try { locationManager.removeUpdates(locationListener); } catch (Exception ignored) {}
        }
    }
}
