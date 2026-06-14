package com.example.harudiary.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;
import com.example.harudiary.activity.FriendRecordActivity;
import com.example.harudiary.api.FriendApi;
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.model.User;
import com.example.harudiary.util.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FriendListFragment — 내 친구 목록 + 삭제 기능
 */
public class FriendListFragment extends Fragment {

    private RecyclerView rvFriends;
    private LinearLayout layoutEmpty;
    private TextView tvFriendCount;
    private FriendApi friendApi;
    private String userIdStr;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_list, container, false);

        userIdStr = new SessionManager(requireContext()).getUserId();
        friendApi = RetrofitClient.getClient().create(FriendApi.class);

        rvFriends      = view.findViewById(R.id.rv_friends);
        layoutEmpty    = view.findViewById(R.id.layout_empty_friends);
        tvFriendCount  = view.findViewById(R.id.tv_friend_count);

        rvFriends.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFriends.setHasFixedSize(false);

        // 친구 추가 버튼
        view.findViewById(R.id.btn_search_friend).setOnClickListener(v -> {
            FriendSearchDialogFragment dialog = new FriendSearchDialogFragment();
            dialog.setOnFriendAddedListener(this::loadFriends);
            dialog.show(getChildFragmentManager(), "friend_search");
        });

        loadFriends();
        return view;
    }

    @Override
    public void onResume() { super.onResume(); if (friendApi != null) loadFriends(); }

    private void loadFriends() {
        if (userIdStr == null) return;
        friendApi.getFriends(userIdStr).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> friends = response.body();
                    tvFriendCount.setText("친구 " + friends.size() + "명");

                    if (friends.isEmpty()) {
                        rvFriends.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvFriends.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                        rvFriends.setAdapter(new FriendListAdapter(friends));
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {}
        });
    }

    // ─── 내부 Adapter ────────────────────────────────────────────

    private class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.VH> {

        private final List<User> data;

        FriendListAdapter(List<User> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH vh, int position) {
            User friend = data.get(position);
            String initial = (friend.getName() != null && !friend.getName().isEmpty())
                    ? friend.getName().substring(0, 1) : "?";

            vh.tvAvatar.setText(initial);
            vh.tvName.setText(friend.getName());
            vh.tvEmail.setText(friend.getEmail());

            // 기록 보기
            vh.btnViewRecords.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), FriendRecordActivity.class);
                intent.putExtra(FriendRecordActivity.EXTRA_FRIEND_USER_ID, friend.getUserId());
                intent.putExtra(FriendRecordActivity.EXTRA_FRIEND_NAME, friend.getName());
                startActivity(intent);
            });

            // 삭제 버튼
            vh.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                    .setTitle("친구 삭제")
                    .setMessage(friend.getName() + "님을 친구 목록에서 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (d, w) -> {
                        if (userIdStr != null) {
                            friendApi.deleteFriend(userIdStr, friend.getUserId()).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                                    loadFriends();
                                }
                                @Override
                                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
                            });
                        }
                    })
                    .setNegativeButton("취소", null)
                    .show();
            });
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvEmail, btnViewRecords, btnDelete;
            VH(@NonNull View v) {
                super(v);
                tvAvatar       = v.findViewById(R.id.tv_avatar);
                tvName         = v.findViewById(R.id.tv_friend_name);
                tvEmail        = v.findViewById(R.id.tv_friend_email);
                btnViewRecords = v.findViewById(R.id.btn_view_records);
                btnDelete      = v.findViewById(R.id.btn_delete_friend);
            }
        }
    }
}
