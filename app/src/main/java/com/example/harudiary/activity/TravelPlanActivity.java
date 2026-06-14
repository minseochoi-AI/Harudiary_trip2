package com.example.harudiary.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;
import com.example.harudiary.adapter.TravelPlanAdapter;
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.api.TravelApi;
import com.example.harudiary.model.DayPlanDto;
import com.example.harudiary.model.PlaceDto;
import com.example.harudiary.model.TravelPlanResponse;
import com.example.harudiary.util.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TravelPlanActivity extends AppCompatActivity implements TravelPlanAdapter.OnVisitCompleteListener {

    private RecyclerView rvTravelPlan;
    private TravelPlanAdapter adapter;
    private List<PlaceDto> flattenedList = new ArrayList<>();
    private String date;
    private TravelPlanResponse originalResponse;
    private String userId;
    private PlaceDto lastClickedPlace;
    private static final int REQ_RECORD = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel_plan);

        userId = new SessionManager(this).getUserId();

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tv_trip_title);
        Button btnSave = findViewById(R.id.btn_save_plan);
        rvTravelPlan = findViewById(R.id.rv_travel_plan);
        rvTravelPlan.setLayoutManager(new LinearLayoutManager(this));

        // Get intent data
        originalResponse = (TravelPlanResponse) getIntent().getSerializableExtra("plan");
        date = getIntent().getStringExtra("date");
        boolean isConfirmMode = getIntent().getBooleanExtra("isConfirmMode", false);
        long diaryId = getIntent().getLongExtra("diaryId", -1);

        ImageButton btnDelete = findViewById(R.id.btn_delete_plan);

        if (isConfirmMode) {
            btnSave.setVisibility(android.view.View.GONE);
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> showDeleteConfirmDialog(diaryId));
        }

        if (originalResponse != null) {
            if (originalResponse.getTripTitle() != null) {
                tvTitle.setText(originalResponse.getTripTitle());
            }

            // Flatten list with Headers
            if (originalResponse.getDays() != null) {
                for (DayPlanDto day : originalResponse.getDays()) {
                    PlaceDto header = new PlaceDto();
                    header.setPlaceCategory("HEADER");
                    header.setPlaceName("📅 Day " + day.getDayNumber());
                    header.setDayNumber(day.getDayNumber());
                    flattenedList.add(header);

                    if (day.getPlaces() != null) {
                        for (PlaceDto place : day.getPlaces()) {
                            place.setDayNumber(day.getDayNumber());
                            flattenedList.add(place);
                        }
                    }
                }
            }
        }

        adapter = new TravelPlanAdapter(flattenedList, this, isConfirmMode);
        rvTravelPlan.setAdapter(adapter);

        setupItemTouchHelper();

        btnSave.setOnClickListener(v -> savePlan());
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                
                // Header items shouldn't be swipeable, but can be movable or we just prevent moving headers.
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && "HEADER".equals(flattenedList.get(position).getPlaceCategory())) {
                    return makeMovementFlags(0, 0); // Disable drag and swipe for headers
                }
                
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                if ("HEADER".equals(flattenedList.get(toPosition).getPlaceCategory())) {
                    return false; // Don't allow swapping with a header
                }

                Collections.swap(flattenedList, fromPosition, toPosition);
                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                PlaceDto deletedItem = flattenedList.get(position);
                flattenedList.remove(position);
                adapter.notifyItemRemoved(position);

                com.google.android.material.snackbar.Snackbar.make(rvTravelPlan, "일정이 삭제되었습니다.", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setAction("실행 취소", v -> {
                            flattenedList.add(position, deletedItem);
                            adapter.notifyItemInserted(position);
                        }).show();
            }
        });
        itemTouchHelper.attachToRecyclerView(rvTravelPlan);
    }

    private void savePlan() {
        if (originalResponse == null) return;

        // Reconstruct List<DayPlanDto> based on headers
        List<DayPlanDto> newDays = new ArrayList<>();
        DayPlanDto currentDay = null;
        int currentDayNumber = 1;

        for (PlaceDto item : flattenedList) {
            if ("HEADER".equals(item.getPlaceCategory())) {
                if (currentDay != null && currentDay.getPlaces() != null && !currentDay.getPlaces().isEmpty()) {
                    newDays.add(currentDay);
                }
                currentDayNumber = item.getDayNumber();
                currentDay = new DayPlanDto();
                currentDay.setDayNumber(currentDayNumber);
                currentDay.setPlaces(new ArrayList<>());
            } else {
                if (currentDay == null) {
                    currentDay = new DayPlanDto();
                    currentDay.setDayNumber(currentDayNumber);
                    currentDay.setPlaces(new ArrayList<>());
                }
                item.setDayNumber(currentDayNumber);
                currentDay.getPlaces().add(item);
            }
        }
        if (currentDay != null && currentDay.getPlaces() != null && !currentDay.getPlaces().isEmpty()) {
            newDays.add(currentDay);
        }

        originalResponse.setDays(newDays);

        TravelApi api = RetrofitClient.getClient().create(TravelApi.class);
        api.savePlan(originalResponse, userId, null, date).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TravelPlanActivity.this, "일정이 확정되었습니다.", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("date", date);
                    setResult(RESULT_OK, resultIntent);
                    finish(); // Return to previous screen
                } else {
                    Toast.makeText(TravelPlanActivity.this, "저장 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TravelPlanActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onVisitComplete(PlaceDto place) {
        lastClickedPlace = place;
        Intent intent = new Intent(this, RecordActivity.class);
        intent.putExtra(RecordActivity.EXTRA_PREFILL_CONTENT, "📍 " + place.getPlaceName() + " 방문");
        intent.putExtra(RecordActivity.EXTRA_PREFILL_ADDRESS, place.getAddressName());
        
        if (place.getY() != null && place.getX() != null) {
            try {
                intent.putExtra(RecordActivity.EXTRA_PREFILL_LAT, Double.parseDouble(place.getY()));
                intent.putExtra(RecordActivity.EXTRA_PREFILL_LNG, Double.parseDouble(place.getX()));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        startActivityForResult(intent, REQ_RECORD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_RECORD && resultCode == RESULT_OK) {
            if (lastClickedPlace != null) {
                lastClickedPlace.setVisited(true);
                adapter.notifyDataSetChanged();
                savePlanSilently(); // Save updated status to backend
            }
        }
    }

    private void savePlanSilently() {
        if (originalResponse == null) return;
        TravelApi api = RetrofitClient.getClient().create(TravelApi.class);
        api.savePlan(originalResponse, userId, null, date).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // silently handle
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // silently handle
            }
        });
    }

    private void showDeleteConfirmDialog(long diaryId) {
        if (diaryId == -1) return;
        new android.app.AlertDialog.Builder(this)
                .setTitle("일정 삭제")
                .setMessage("이 추천 일정을 삭제하시겠습니까?\n삭제된 일정은 복구할 수 없습니다.")
                .setPositiveButton("삭제", (dialog, which) -> deletePlan(diaryId))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deletePlan(long diaryId) {
        TravelApi api = RetrofitClient.getClient().create(TravelApi.class);
        api.deletePlan(diaryId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TravelPlanActivity.this, "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(TravelPlanActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TravelPlanActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
