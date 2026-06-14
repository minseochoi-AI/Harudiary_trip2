package com.example.harudiary.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.MainActivity;
import com.example.harudiary.R;
import com.example.harudiary.adapter.ActivityListAdapter;
import com.example.harudiary.api.DiaryApi;
import com.example.harudiary.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.harudiary.model.Record;
import com.example.harudiary.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * RecordListFragment — 전체 기록 목록 (최신순) + 검색 기능
 */
public class RecordListFragment extends Fragment {

    private RecyclerView rvRecords;
    private TextView tvEmpty;
    private LinearLayout layoutSearchBar;
    private EditText etSearch;
    private ImageView btnSearch;
    private ImageView btnSearchClose;

    private ActivityListAdapter adapter;
    private String userIdStr;

    private List<Record> allRecords = new ArrayList<>();
    private boolean searchVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record_list, container, false);

        userIdStr = new SessionManager(requireContext()).getUserId();

        rvRecords       = view.findViewById(R.id.rv_all_records);
        tvEmpty         = view.findViewById(R.id.tv_empty_records);
        layoutSearchBar = view.findViewById(R.id.layout_search_bar);
        etSearch        = view.findViewById(R.id.et_search);
        btnSearch       = view.findViewById(R.id.btn_search_record);
        btnSearchClose  = view.findViewById(R.id.btn_search_close);

        rvRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecords.setHasFixedSize(false);

        // 돋보기 아이콘 클릭 → 검색바 토글
        btnSearch.setOnClickListener(v -> toggleSearchBar());

        // X 버튼 클릭 → 검색바 닫기
        btnSearchClose.setOnClickListener(v -> closeSearchBar());

        // 실시간 검색 필터
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecords(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 키보드 검색 버튼 처리
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        loadRecords();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userIdStr != null) loadRecords();
    }

    private void toggleSearchBar() {
        if (searchVisible) {
            closeSearchBar();
        } else {
            searchVisible = true;
            layoutSearchBar.setVisibility(View.VISIBLE);
            etSearch.requestFocus();
            showKeyboard();
        }
    }

    private void closeSearchBar() {
        searchVisible = false;
        layoutSearchBar.setVisibility(View.GONE);
        etSearch.setText("");
        hideKeyboard();
        filterRecords(""); // 전체 목록 복원
    }

    private void filterRecords(String query) {
        if (query.isEmpty()) {
            updateUI(allRecords);
            return;
        }
        String lower = query.toLowerCase();
        List<Record> filtered = new ArrayList<>();
        for (Record r : allRecords) {
            boolean matchContent = r.getContent() != null && r.getContent().toLowerCase().contains(lower);
            boolean matchDate    = r.getDate() != null && r.getDate().contains(lower);
            boolean matchWeather = r.getWeather() != null && r.getWeather().toLowerCase().contains(lower);
            boolean matchAddress = r.getAddress() != null && r.getAddress().toLowerCase().contains(lower);
            if (matchContent || matchDate || matchWeather || matchAddress) {
                filtered.add(r);
            }
        }
        updateUI(filtered);
    }

    private void loadRecords() {
        DiaryApi diaryApi = RetrofitClient.getClient().create(DiaryApi.class);
        diaryApi.getDiaries(userIdStr).enqueue(new Callback<List<Record>>() {
            @Override
            public void onResponse(@NonNull Call<List<Record>> call, @NonNull Response<List<Record>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Record> fetched = response.body();
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        fetched.removeIf(Record::isPlan);
                    } else {
                        java.util.Iterator<Record> it = fetched.iterator();
                        while (it.hasNext()) {
                            if (it.next().isPlan()) it.remove();
                        }
                    }
                    allRecords = fetched;
                    // 내림차순 정렬 (날짜 -> activityId)
                    allRecords.sort((r1, r2) -> {
                        String d1 = r1.getDate() != null ? r1.getDate() : "";
                        String d2 = r2.getDate() != null ? r2.getDate() : "";
                        int dateCmp = d2.compareTo(d1);
                        if (dateCmp != 0) return dateCmp;
                        Long id1 = r1.getActivityId();
                        Long id2 = r2.getActivityId();
                        if (id1 == null && id2 == null) return 0;
                        if (id1 == null) return 1;
                        if (id2 == null) return -1;
                        return Long.compare(id2, id1);
                    });
                } else {
                    allRecords = new ArrayList<>();
                }
                processRecords();
            }

            @Override
            public void onFailure(@NonNull Call<List<Record>> call, @NonNull Throwable t) {
                allRecords = new ArrayList<>();
                processRecords();
            }
        });
    }

    private void processRecords() {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            String currentQuery = etSearch != null ? etSearch.getText().toString().trim() : "";
            if (currentQuery.isEmpty()) {
                updateUI(allRecords);
            } else {
                filterRecords(currentQuery);
            }
        });
    }

    private void updateUI(List<Record> records) {
        if (adapter == null) {
            adapter = new ActivityListAdapter(records, this::onDateClick);
            rvRecords.setAdapter(adapter);
        } else {
            adapter.update(records);
        }
        boolean empty = records.isEmpty();
        rvRecords.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

        // 검색 중이지만 결과 없는 경우 메시지 변경
        if (empty && searchVisible && !etSearch.getText().toString().isEmpty()) {
            tvEmpty.setText("검색 결과가 없습니다 🔍");
        } else if (empty) {
            tvEmpty.setText("아직 기록이 없습니다\n+ 버튼으로 첫 기록을 남겨보세요 ✏️");
        }
    }

    private void onDateClick(String date) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToDaily(date);
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null)
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }
}
