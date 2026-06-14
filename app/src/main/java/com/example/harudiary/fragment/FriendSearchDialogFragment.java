package com.example.harudiary.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;
import com.example.harudiary.adapter.FriendSearchAdapter;
import com.example.harudiary.api.FriendApi;
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.model.User;
import com.example.harudiary.util.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FriendSearchDialogFragment — 친구 검색 다이얼로그
 * 이름 또는 이메일로 검색 → 친구 요청 보내기
 */
public class FriendSearchDialogFragment extends DialogFragment {

    public interface OnFriendAddedListener {
        void onFriendRequestSent();
    }

    private FriendApi friendApi;
    private String userId;
    private FriendSearchAdapter adapter;
    private OnFriendAddedListener listener;

    public void setOnFriendAddedListener(OnFriendAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_Harudiary_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_friend_search, container, false);

        userId = new SessionManager(requireContext()).getUserId();
        friendApi = RetrofitClient.getClient().create(FriendApi.class);

        EditText etSearch = view.findViewById(R.id.et_search);
        RecyclerView rvResults = view.findViewById(R.id.rv_search_results);
        TextView tvEmpty = view.findViewById(R.id.tv_search_empty);

        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new FriendSearchAdapter(new ArrayList<>(), new ArrayList<>(),
                (user, position) -> {
                    Map<String, String> payload = new HashMap<>();
                    payload.put("fromUserId", String.valueOf(userId));
                    payload.put("toUserId", user.getUserId());
                    friendApi.requestFriend(payload).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                adapter.updateStatus(position, "pending_sent");
                                Toast.makeText(requireContext(),
                                        user.getName() + "님에게 친구 요청을 보냈습니다", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onFriendRequestSent();
                            } else {
                                Toast.makeText(requireContext(),
                                        "요청을 보낼 수 없습니다", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {}
                    });
                });
        rvResults.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() < 1) {
                    adapter.update(new ArrayList<>(), new ArrayList<>());
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("이름 또는 이메일을 입력하여\n친구를 검색해보세요");
                    rvResults.setVisibility(View.GONE);
                    return;
                }

                friendApi.searchUsers(String.valueOf(userId), query).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<User> results = new ArrayList<>();
                            List<String> statuses = new ArrayList<>();
                            for (Map<String, Object> map : response.body()) {
                                User u = new User();
                                u.setUserId(map.get("userId") != null ? String.valueOf(map.get("userId")) : "");
                                u.setName(map.get("nickname") != null ? String.valueOf(map.get("nickname")) : "");
                                u.setEmail("");
                                results.add(u);
                                statuses.add(map.get("status") != null ? String.valueOf(map.get("status")) : "none");
                            }

                            adapter.update(results, statuses);

                            if (results.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                tvEmpty.setText("\"" + query + "\"에 대한 검색 결과가 없습니다");
                                rvResults.setVisibility(View.GONE);
                            } else {
                                tvEmpty.setVisibility(View.GONE);
                                rvResults.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
                });
            }
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
