package com.example.harudiary.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.harudiary.R;
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.api.TravelApi;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PlanInputActivity extends AppCompatActivity {

    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");
    public static final String EXTRA_DATE = "extra_date";

    private String selectedDate;
    private int calculatedDays = 1;

    private EditText etDestination;
    private EditText etContent;
    private TextView tvStartDate;
    private TextView tvEndDate;
    private String endDateStr;
    private com.google.android.material.slider.Slider sliderPlacesPerDay;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_input);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);

        initViews();
        applyIntentExtras();
    }

    private void initViews() {
        etDestination = findViewById(R.id.et_destination);
        etContent = findViewById(R.id.et_content);
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
        sliderPlacesPerDay = findViewById(R.id.slider_places_per_day);

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());

        tvStartDate.setOnClickListener(v -> showStartDatePickerDialog());
        tvEndDate.setOnClickListener(v -> showEndDatePickerDialog());

        Button btnGenerate = findViewById(R.id.btn_generate_travel_plan);
        btnGenerate.setOnClickListener(v -> generateTravelPlan());
    }

    private void applyIntentExtras() {
        selectedDate = getIntent().getStringExtra(EXTRA_DATE);
        if (selectedDate == null || selectedDate.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            sdf.setTimeZone(KST);
            selectedDate = sdf.format(new Date());
        }
        endDateStr = selectedDate; // 초기에는 종료일도 시작일과 같게 설정
        
        String prefillContent = getIntent().getStringExtra("EXTRA_CONTENT");
        if (prefillContent != null && !prefillContent.isEmpty()) {
            etContent.setText(prefillContent);
        }
        
        String prefillDestination = getIntent().getStringExtra("EXTRA_DESTINATION");
        if (prefillDestination != null && !prefillDestination.isEmpty()) {
            etDestination.setText(prefillDestination);
        }
        
        updateDateUI();
    }

    private void updateDateUI() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            sdf.setTimeZone(KST);
            
            Date startObj = sdf.parse(selectedDate);
            Date endObj = sdf.parse(endDateStr);
            
            if (endObj.before(startObj)) {
                endDateStr = selectedDate;
                endObj = startObj;
            }
            
            long diffInMillis = endObj.getTime() - startObj.getTime();
            calculatedDays = (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;
            
            tvStartDate.setText(selectedDate);
            if (calculatedDays == 1) {
                tvEndDate.setText(endDateStr + " (당일치기)");
            } else {
                tvEndDate.setText(String.format(Locale.KOREA, "%s (%d박 %d일)", endDateStr, calculatedDays - 1, calculatedDays));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStartDatePickerDialog() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            sdf.setTimeZone(KST);
            Date start = sdf.parse(selectedDate);
            Calendar cal = Calendar.getInstance(KST);
            if (start != null) cal.setTime(start);

            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar newStartCal = Calendar.getInstance(KST);
                newStartCal.set(year, month, dayOfMonth);
                selectedDate = sdf.format(newStartCal.getTime());
                updateDateUI();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showEndDatePickerDialog() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            sdf.setTimeZone(KST);
            Date end = sdf.parse(endDateStr);
            Calendar cal = Calendar.getInstance(KST);
            if (end != null) cal.setTime(end);

            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar newEndCal = Calendar.getInstance(KST);
                newEndCal.set(year, month, dayOfMonth);
                
                try {
                    Date startObj = sdf.parse(selectedDate);
                    if (newEndCal.getTime().before(startObj)) {
                        Toast.makeText(this, "종료일은 시작일과 같거나 이후여야 합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    endDateStr = sdf.format(newEndCal.getTime());
                    updateDateUI();
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
        String destination = etDestination.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        int placesPerDay = (int) sliderPlacesPerDay.getValue();

        if (destination.isEmpty()) {
            Toast.makeText(this, "목적지를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.isEmpty()) {
            Toast.makeText(this, "원하시는 여행 스타일을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 목적지를 content와 결합하여 AI에 명확히 전달
        String fullContent = "목적지: " + destination + ". 내용: " + content;

        showLoadingDialog();

        TravelApi api = RetrofitClient.getClient().create(TravelApi.class);
        // GPS 좌표는 목적지 검색이므로 0, 0 전달
        TravelApi.DiaryRecommendRequest req = new TravelApi.DiaryRecommendRequest(selectedDate, calculatedDays, 0, 0, fullContent, placesPerDay);

        api.recommendByDiary(req).enqueue(new retrofit2.Callback<com.example.harudiary.model.TravelPlanResponse>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<com.example.harudiary.model.TravelPlanResponse> call, @NonNull retrofit2.Response<com.example.harudiary.model.TravelPlanResponse> response) {
                hideLoadingDialog();
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getDays() == null || response.body().getDays().isEmpty() || "지역을 알 수 없습니다".equals(response.body().getTripTitle())) {
                        runOnUiThread(() -> Toast.makeText(PlanInputActivity.this, "정확한 지역명을 포함해서 다시 작성해 주세요", Toast.LENGTH_LONG).show());
                        return;
                    }
                    Intent intent = new Intent(PlanInputActivity.this, TravelPlanActivity.class);
                    intent.putExtra("plan", response.body());
                    intent.putExtra("date", selectedDate);
                    // PlanInputActivity는 종료하고 TravelPlanActivity로 이동 (나중에 setResult 연동을 위해)
                    startActivityForResult(intent, 100);
                } else {
                    runOnUiThread(() -> Toast.makeText(PlanInputActivity.this, "일정 생성 실패", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<com.example.harudiary.model.TravelPlanResponse> call, @NonNull Throwable t) {
                hideLoadingDialog();
                runOnUiThread(() -> Toast.makeText(PlanInputActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 50, 50, 50);
            layout.setGravity(Gravity.CENTER);
            
            ProgressBar progressBar = new ProgressBar(this);
            layout.addView(progressBar);
            
            TextView tvMessage = new TextView(this);
            tvMessage.setText("AI가 여행 일정을 생성하고 있습니다...\n(최대 15초 소요될 수 있습니다)");
            tvMessage.setTextColor(Color.BLACK);
            tvMessage.setGravity(Gravity.CENTER);
            tvMessage.setPadding(0, 30, 0, 0);
            layout.addView(tvMessage);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(layout);
            builder.setCancelable(false);
            loadingDialog = builder.create();
        }
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // TravelPlanActivity에서 저장이 성공하면 PlanInputActivity도 연쇄 종료
            setResult(RESULT_OK, data);
            finish();
        }
    }
}
