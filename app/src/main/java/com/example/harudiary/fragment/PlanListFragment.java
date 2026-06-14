package com.example.harudiary.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.MainActivity;
import com.example.harudiary.R;
import com.example.harudiary.adapter.PlanListAdapter;
import com.example.harudiary.api.DiaryApi;
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.api.TravelApi;
import com.example.harudiary.model.Record;
import com.example.harudiary.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlanListFragment extends Fragment {

    private RecyclerView rvPlans;
    private TextView tvEmpty;
    private PlanListAdapter adapter;
    private String userIdStr;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plan_list, container, false);

        userIdStr = new SessionManager(requireContext()).getUserId();

        rvPlans = view.findViewById(R.id.rv_plans);
        tvEmpty = view.findViewById(R.id.tv_empty_plans);

        rvPlans.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        loadPlans();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userIdStr != null) {
            loadPlans();
        }
    }

    private void loadPlans() {
        DiaryApi diaryApi = RetrofitClient.getClient().create(DiaryApi.class);
        diaryApi.getPlans(userIdStr).enqueue(new Callback<List<Record>>() {
            @Override
            public void onResponse(@NonNull Call<List<Record>> call, @NonNull Response<List<Record>> response) {
                List<Record> plans = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    plans = response.body();
                    // 서버가 잘못된 데이터를 내려줄 경우를 대비한 2차 필터링 (방어적 코드)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        plans.removeIf(r -> !r.isPlan());
                    } else {
                        java.util.Iterator<Record> it = plans.iterator();
                        while (it.hasNext()) {
                            if (!it.next().isPlan()) it.remove();
                        }
                    }
                }
                updateUI(plans);
            }

            @Override
            public void onFailure(@NonNull Call<List<Record>> call, @NonNull Throwable t) {
                updateUI(new ArrayList<>());
            }
        });
    }

    private void updateUI(List<Record> plans) {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            if (adapter == null) {
                adapter = new PlanListAdapter(plans, this::onPlanClick);
                rvPlans.setAdapter(adapter);
            } else {
                adapter.update(plans);
            }

            boolean empty = plans.isEmpty();
            rvPlans.setVisibility(empty ? View.GONE : View.VISIBLE);
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
    }

    private void onPlanClick(Record plan) {
        if (plan.getActivityId() == null) return;
        
        TravelApi travelApi = RetrofitClient.getClient().create(TravelApi.class);
        travelApi.getPlanById(plan.getActivityId()).enqueue(new Callback<com.example.harudiary.model.TravelPlanResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.example.harudiary.model.TravelPlanResponse> call, @NonNull Response<com.example.harudiary.model.TravelPlanResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (!isAdded() || getActivity() == null) return;
                    android.content.Intent intent = new android.content.Intent(getActivity(), com.example.harudiary.activity.TravelPlanActivity.class);
                    intent.putExtra("plan", response.body());
                    intent.putExtra("isConfirmMode", true);
                    intent.putExtra("diaryId", plan.getActivityId());
                    startActivity(intent);
                } else {
                    if (isAdded() && getActivity() != null) {
                        android.widget.Toast.makeText(requireContext(), "계획 상세를 불러오지 못했습니다.", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.example.harudiary.model.TravelPlanResponse> call, @NonNull Throwable t) {
                if (isAdded() && getActivity() != null) {
                    android.widget.Toast.makeText(requireContext(), "네트워크 오류", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
