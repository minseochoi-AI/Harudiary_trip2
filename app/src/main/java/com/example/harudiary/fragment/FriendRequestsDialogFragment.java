package com.example.harudiary.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;
import com.example.harudiary.adapter.FriendRequestAdapter;
import com.example.harudiary.api.FriendApi;
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.model.FriendRequest;
import com.example.harudiary.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FriendRequestsDialogFragment — 받은 친구 요청 목록
 * 수락/거절 처리
 */
public class FriendRequestsDialogFragment extends DialogFragment {

    public interface OnRequestHandledListener {
        void onRequestHandled();
    }

    private FriendApi friendApi;
    private int userId;
    private FriendRequestAdapter adapter;
    private OnRequestHandledListener listener;

    public void setOnRequestHandledListener(OnRequestHandledListener listener) {
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
        View view = inflater.inflate(R.layout.dialog_friend_requests, container, false);

        userId = new SessionManager(requireContext()).getLoggedInUserId();
        friendApi = RetrofitClient.getClient().create(FriendApi.class);

        RecyclerView rvRequests = view.findViewById(R.id.rv_requests);
        TextView tvEmpty = view.findViewById(R.id.tv_requests_empty);

        rvRequests.setLayoutManager(new LinearLayoutManager(requireContext()));

        friendApi.getPendingRequests(String.valueOf(userId)).enqueue(new Callback<List<FriendRequest>>() {
            @Override
            public void onResponse(@NonNull Call<List<FriendRequest>> call, @NonNull Response<List<FriendRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FriendRequest> requests = response.body();
                    adapter = new FriendRequestAdapter(requests, new FriendRequestAdapter.OnRequestActionListener() {
                        @Override
                        public void onAccept(FriendRequest request, int position) {
                            friendApi.acceptFriendRequest(request.getRequestId()).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                                    adapter.removeAt(position);
                                    Toast.makeText(requireContext(), request.getFromUserName() + "님과 친구가 되었습니다! 🎉", Toast.LENGTH_SHORT).show();
                                    updateEmptyState(tvEmpty, rvRequests);
                                    if (listener != null) listener.onRequestHandled();
                                }
                                @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
                            });
                        }

                        @Override
                        public void onReject(FriendRequest request, int position) {
                            friendApi.rejectFriendRequest(request.getRequestId()).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(@androidx.annotation.NonNull Call<Void> call, @androidx.annotation.NonNull Response<Void> response) {
                                    requireActivity().runOnUiThread(() -> {
                                        adapter.removeAt(position);
                                        Toast.makeText(requireContext(), "요청을 거절했습니다", Toast.LENGTH_SHORT).show();
                                        updateEmptyState(tvEmpty, rvRequests);
                                        if (listener != null) listener.onRequestHandled();
                                    });
                                }
                                @Override public void onFailure(@androidx.annotation.NonNull Call<Void> call, @androidx.annotation.NonNull Throwable t) {}
                            });
                        }
                    });
                    requireActivity().runOnUiThread(() -> {
                        rvRequests.setAdapter(adapter);
                        updateEmptyState(tvEmpty, rvRequests);
                    });
                }
            }

            @Override
            public void onFailure(@androidx.annotation.NonNull Call<java.util.List<com.example.harudiary.model.FriendRequest>> call, @androidx.annotation.NonNull Throwable t) {
                requireActivity().runOnUiThread(() -> {
                    adapter = new FriendRequestAdapter(new java.util.ArrayList<>(), null);
                    rvRequests.setAdapter(adapter);
                    updateEmptyState(tvEmpty, rvRequests);
                });
            }
        });

        return view;
    }

    private void updateEmptyState(TextView tvEmpty, RecyclerView rvRequests) {
        boolean empty = adapter.getItemCount() == 0;
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvRequests.setVisibility(empty ? View.GONE : View.VISIBLE);
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
