package com.example.harudiary.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.harudiary.R;
import com.example.harudiary.activity.DeleteRecordsActivity;
import com.example.harudiary.activity.PlanInputActivity;
import com.example.harudiary.util.SessionManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * DailyFragment — 일별 조회
 * ★ D·LOG 스타일 헤더 + 하루 제목 입력/저장
 */
public class DailyFragment extends Fragment {

    private static final String ARG_DATE = "date";

    private String userId;
    private String date;

    public static DailyFragment newInstance(String date) {
        DailyFragment f = new DailyFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily, container, false);

        date   = getArguments() != null ? getArguments().getString(ARG_DATE, "") : "";
        userId = new SessionManager(requireContext()).getUserId();

        // 뒤로가기
        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // 날짜 표시 (2026.06.06 형식)
        TextView tvDateTitle = view.findViewById(R.id.tv_date_title);
        tvDateTitle.setText(date.replace("-", "."));

        // 날씨 초기화
        ((TextView) view.findViewById(R.id.tv_weather)).setText("");

        // 삭제 버튼
        view.findViewById(R.id.btn_delete_records).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DeleteRecordsActivity.class);
            intent.putExtra(DeleteRecordsActivity.EXTRA_DATE, date);
            startActivity(intent);
        });

        // ★ 하루 제목 입력
        EditText etTitle = view.findViewById(R.id.et_day_title);
        etTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
            }
        });

        // 타임라인 프래그먼트 직접 로드 (단일 뷰 통합)
        if (savedInstanceState == null) {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_timeline, DailyTimelineFragment.newInstance(date))
                    .commit();
        }

        return view;
    }
}
