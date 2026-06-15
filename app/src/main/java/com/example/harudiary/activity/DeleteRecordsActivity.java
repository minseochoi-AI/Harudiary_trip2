package com.example.harudiary.activity;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.harudiary.R;
import com.example.harudiary.api.DiaryApi;
import com.example.harudiary.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.harudiary.model.Record;
import com.example.harudiary.util.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DeleteRecordsActivity — 기록 삭제 화면
 * - 날짜의 전체 기록 한 번에 삭제
 * - 개별 기록 선택 후 삭제
 */
public class DeleteRecordsActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "extra_date";

    private String date;
    private String userIdStr;

    private List<Record>  records = new ArrayList<>();
    private Set<Long>  selectedIds = new HashSet<>();

    private LinearLayout llRecordList;
    private TextView     btnDeleteSelected;
    private CheckBox     cbSelectAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_records);

        date   = getIntent().getStringExtra(EXTRA_DATE);
        userIdStr = new SessionManager(this).getUserId();

        llRecordList     = findViewById(R.id.ll_record_list);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);
        cbSelectAll      = findViewById(R.id.cb_select_all);

        // 헤더 날짜 표시
        TextView tvHeaderDate = findViewById(R.id.tv_header_date);
        if (tvHeaderDate != null && date != null) {
            tvHeaderDate.setText(formatDateTitle(date));
        }

        // 취소 버튼
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());

        // 전체 삭제 버튼
        findViewById(R.id.btn_delete_all).setOnClickListener(v -> confirmDeleteAll());

        // 전체 선택 체크박스
        cbSelectAll.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) selectAll();
            else         deselectAll();
        });

        // 선택 삭제 버튼
        btnDeleteSelected.setOnClickListener(v -> {
            if (!selectedIds.isEmpty()) confirmDeleteSelected();
        });

        loadRecords();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecords();
    }

    private void loadRecords() {
        DiaryApi diaryApi = RetrofitClient.getClient().create(DiaryApi.class);
        diaryApi.getActivitiesByDate(userIdStr, date).enqueue(new Callback<List<Record>>() {
            @Override
            public void onResponse(@androidx.annotation.NonNull Call<List<Record>> call, @androidx.annotation.NonNull Response<List<Record>> response) {
                records = (response.isSuccessful() && response.body() != null) ? response.body() : new ArrayList<>();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    records.removeIf(Record::isPlan);
                } else {
                    java.util.Iterator<Record> it = records.iterator();
                    while (it.hasNext()) {
                        if (it.next().isPlan()) it.remove();
                    }
                }
                processRecords();
            }

            @Override
            public void onFailure(@androidx.annotation.NonNull Call<List<Record>> call, @androidx.annotation.NonNull Throwable t) {
                records = new ArrayList<>();
                processRecords();
            }
        });
    }

    private void processRecords() {
        runOnUiThread(() -> {
            selectedIds.clear();
            llRecordList.removeAllViews();

            if (records.isEmpty()) {
                Toast.makeText(this, "삭제할 기록이 없습니다", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            for (Record r : records) {
                View item = buildRecordItem(r);
                llRecordList.addView(item);
            }

            updateDeleteButton();
        });
    }

    private View buildRecordItem(Record record) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_delete_record, llRecordList, false);

        CheckBox  cb      = item.findViewById(R.id.cb_record);
        ImageView ivThumb = item.findViewById(R.id.iv_thumb);
        TextView  tvSlot  = item.findViewById(R.id.tv_slot_badge);
        TextView  tvTime  = item.findViewById(R.id.tv_time);
        TextView  tvContent = item.findViewById(R.id.tv_content);

        // 썸네일
        if (record.getPhotoUri() != null && !record.getPhotoUri().isEmpty()) {
            try {
                com.example.harudiary.util.ImageUtil.setSafeImageURI(DeleteRecordsActivity.this, ivThumb, record.getPhotoUri());
                ivThumb.setVisibility(View.VISIBLE);
            } catch (Exception ignored) {}
        }

        // 시간대 배지
        String slot = record.getTimeSlot();
        if (slot != null) {
            tvSlot.setText(slotToKorean(slot));
            tvSlot.setBackgroundTintList(ColorStateList.valueOf(slotColor(slot)));
            tvSlot.setVisibility(View.VISIBLE);
        } else {
            tvSlot.setVisibility(View.GONE);
        }

        // 시각
        tvTime.setText(DateFormat.format("HH:mm", new Date(record.getTimestamp())).toString());

        // 내용
        tvContent.setText(record.getContent() != null ? record.getContent() : "");

        // 체크박스 상태 반영
        Long id = record.getActivityId();
        cb.setChecked(selectedIds.contains(id));
        cb.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) selectedIds.add(id);
            else        selectedIds.remove(id);
            // 전체 선택 체크박스 동기화
            cbSelectAll.setOnCheckedChangeListener(null);
            cbSelectAll.setChecked(selectedIds.size() == records.size());
            cbSelectAll.setOnCheckedChangeListener((b2, c2) -> {
                if (c2) selectAll(); else deselectAll();
            });
            updateDeleteButton();
        });

        // 행 전체 클릭 → 체크박스 토글
        item.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));

        return item;
    }

    private void selectAll() {
        for (Record r : records) selectedIds.add(r.getActivityId());
        refreshCheckboxes();
        updateDeleteButton();
    }

    private void deselectAll() {
        selectedIds.clear();
        refreshCheckboxes();
        updateDeleteButton();
    }

    private void refreshCheckboxes() {
        for (int i = 0; i < llRecordList.getChildCount(); i++) {
            View item = llRecordList.getChildAt(i);
            CheckBox cb = item.findViewById(R.id.cb_record);
            if (cb != null && i < records.size()) {
                cb.setOnCheckedChangeListener(null);
                cb.setChecked(selectedIds.contains(records.get(i).getActivityId()));
                final Record r = records.get(i);
                cb.setOnCheckedChangeListener((btn, checked) -> {
                    if (checked) selectedIds.add(r.getActivityId());
                    else        selectedIds.remove(r.getActivityId());
                    cbSelectAll.setOnCheckedChangeListener(null);
                    cbSelectAll.setChecked(selectedIds.size() == records.size());
                    cbSelectAll.setOnCheckedChangeListener((b2, c2) -> {
                        if (c2) selectAll(); else deselectAll();
                    });
                    updateDeleteButton();
                });
            }
        }
    }

    private void updateDeleteButton() {
        int count = selectedIds.size();
        btnDeleteSelected.setText("선택한 기록 삭제 (" + count + "개)");
        btnDeleteSelected.setAlpha(count > 0 ? 1.0f : 0.4f);
        btnDeleteSelected.setEnabled(count > 0);
    }

    // ─── 전체 삭제 확인 다이얼로그 ──────────────────────────────

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
            .setTitle("전체 삭제")
            .setMessage(formatDateTitle(date) + "의 기록 " + records.size() + "개를\n모두 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("전체 삭제", (d, w) -> {
                DiaryApi diaryApi = RetrofitClient.getClient().create(DiaryApi.class);
                for (Record r : records) {
                    diaryApi.deleteDiary(r.getActivityId(), userIdStr).enqueue(new Callback<Void>() {
                        @Override public void onResponse(@androidx.annotation.NonNull Call<Void> call, @androidx.annotation.NonNull Response<Void> response) {}
                        @Override public void onFailure(@androidx.annotation.NonNull Call<Void> call, @androidx.annotation.NonNull Throwable t) {}
                    });
                }
                Toast.makeText(this, "모든 기록이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    // ─── 선택 삭제 확인 다이얼로그 ──────────────────────────────

    private void confirmDeleteSelected() {
        int count = selectedIds.size();
        new AlertDialog.Builder(this)
            .setTitle("선택 삭제")
            .setMessage("선택한 기록 " + count + "개를 삭제하시겠습니까?")
            .setPositiveButton("삭제", (d, w) -> {
                DiaryApi diaryApi = RetrofitClient.getClient().create(DiaryApi.class);
                for (Long id : selectedIds) {
                    diaryApi.deleteDiary(id, userIdStr).enqueue(new Callback<Void>() {
                        @Override public void onResponse(@androidx.annotation.NonNull Call<Void> call, @androidx.annotation.NonNull Response<Void> response) {}
                        @Override public void onFailure(@androidx.annotation.NonNull Call<Void> call, @androidx.annotation.NonNull Throwable t) {}
                    });
                }
                Toast.makeText(this, count + "개 기록이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    // ─── 유틸 ────────────────────────────────────────────────────

    private String formatDateTitle(String d) {
        try {
            String[] p = d.split("-");
            int y = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int dd = Integer.parseInt(p[2]);
            Calendar cal = Calendar.getInstance();
            cal.set(y, m - 1, dd);
            String[] days = {"일", "월", "화", "수", "목", "금", "토"};
            return m + "월 " + dd + "일 (" + days[cal.get(Calendar.DAY_OF_WEEK) - 1] + ")";
        } catch (Exception e) { return d; }
    }

    private static String slotToKorean(String slot) {
        if ("morning".equals(slot)) return "아침";
        if ("lunch".equals(slot))   return "점심";
        if ("evening".equals(slot)) return "저녁";
        return "기타";
    }

    private static int slotColor(String slot) {
        if ("morning".equals(slot)) return 0xFF3B82F6;
        if ("lunch".equals(slot))   return 0xFFF59E0B;
        if ("evening".equals(slot)) return 0xFF7C6FD4;
        return 0xFF9E9E9E;
    }
}
