package com.example.harudiary.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    private TextView tvEndDate;

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
        tvEndDate = findViewById(R.id.tv_end_date);

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());

        Button btnSelectEndDate = findViewById(R.id.btn_select_end_date);
        btnSelectEndDate.setOnClickListener(v -> showDatePickerDialog());

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
    }

    private void showDatePickerDialog() {
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
        String destination = etDestination.getText().toString().trim();
        String content = etContent.getText().toString().trim();

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

        Toast.makeText(this, calculatedDays + "일 여행 일정 생성 중...", Toast.LENGTH_SHORT).show();

        TravelApi api = RetrofitClient.getClient().create(TravelApi.class);
        // GPS 좌표는 목적지 검색이므로 0, 0 전달
        TravelApi.DiaryRecommendRequest req = new TravelApi.DiaryRecommendRequest(selectedDate, calculatedDays, 0, 0, fullContent);

        api.recommendByDiary(req).enqueue(new retrofit2.Callback<com.example.harudiary.model.TravelPlanResponse>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<com.example.harudiary.model.TravelPlanResponse> call, @NonNull retrofit2.Response<com.example.harudiary.model.TravelPlanResponse> response) {
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
                runOnUiThread(() -> Toast.makeText(PlanInputActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
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
