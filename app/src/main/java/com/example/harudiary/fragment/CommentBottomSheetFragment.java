package com.example.harudiary.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;
import com.example.harudiary.api.CommentApi;
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.util.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CommentBottomSheetFragment — 댓글/반응 바텀 시트 (Server API 연동)
 */
public class CommentBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_ACTIVITY_ID = "activity_id";

    private Long activityId;
    private String myUserId;
    private CommentApi commentApi;
    private CommentAdapter adapter;

    // ★ 댓글 추가 후 리액션 바 카운트를 갱신하기 위한 콜백
    public interface OnCommentChangedListener { void onChanged(); }
    private OnCommentChangedListener listener;

    public static CommentBottomSheetFragment newInstance(Long activityId) {
        CommentBottomSheetFragment f = new CommentBottomSheetFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ACTIVITY_ID, activityId);
        f.setArguments(args);
        return f;
    }

    public void setOnCommentChangedListener(OnCommentChangedListener l) { this.listener = l; }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comment_sheet, container, false);

        activityId  = getArguments() != null ? getArguments().getLong(ARG_ACTIVITY_ID) : -1L;
        myUserId    = new SessionManager(requireContext()).getUserId();
        commentApi  = RetrofitClient.getClient().create(CommentApi.class);

        RecyclerView rv       = view.findViewById(R.id.rv_comments);
        TextView tvNoComments = view.findViewById(R.id.tv_no_comments);
        EditText etComment    = view.findViewById(R.id.et_comment);
        TextView btnSend      = view.findViewById(R.id.btn_send_comment);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 댓글 로드
        loadComments(rv, tvNoComments);

        // 전송 버튼
        btnSend.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (text.isEmpty()) return;
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", myUserId);
            payload.put("diaryId", activityId);
            payload.put("content", text);
            
            commentApi.addComment(payload).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        etComment.setText("");
                        loadComments(rv, tvNoComments);
                        if (listener != null) listener.onChanged();
                    } else {
                        Toast.makeText(requireContext(), "댓글 전송 실패", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                    Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
        });

        return view;
    }

    private void loadComments(RecyclerView rv, TextView tvEmpty) {
        commentApi.getComments(activityId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> comments = response.body();
                    if (comments.isEmpty()) {
                        rv.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rv.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                        if (adapter == null) {
                            adapter = new CommentAdapter(comments, myUserId, commentApi, () -> loadComments(rv, tvEmpty));
                            rv.setAdapter(adapter);
                        } else {
                            adapter.update(comments);
                        }
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                tvEmpty.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            }
        });
    }

    // ─── 내부 어댑터 ─────────────────────────────────────────────

    static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.VH> {

        private List<Map<String, Object>> data;
        private final String myUserId;
        private final CommentApi api;
        private final Runnable onDeleted;

        CommentAdapter(List<Map<String, Object>> data, String myUserId, CommentApi api, Runnable onDeleted) {
            this.data = data; this.myUserId = myUserId;
            this.api = api; this.onDeleted = onDeleted;
        }

        void update(List<Map<String, Object>> newData) { this.data = newData; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new VH(v);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onBindViewHolder(@NonNull VH vh, int position) {
            Map<String, Object> c = data.get(position);
            
            // Extract nested fromUser object
            Map<String, Object> fromUser = (Map<String, Object>) c.get("fromUser");
            String name = "?";
            String commentUserId = "";
            if (fromUser != null) {
                name = fromUser.get("nickname") != null ? String.valueOf(fromUser.get("nickname")) : "?";
                commentUserId = fromUser.get("id") != null ? String.valueOf(fromUser.get("id")) : "";
            }
            
            vh.tvAvatar.setText(name.isEmpty() ? "?" : name.substring(0, 1));
            vh.tvName.setText(name);
            vh.tvContent.setText(c.get("content") != null ? String.valueOf(c.get("content")) : "");
            vh.tvTime.setText(c.get("createdAt") != null ? String.valueOf(c.get("createdAt")).substring(0, Math.min(16, String.valueOf(c.get("createdAt")).length())) : "");

            // 본인 댓글 삭제 버튼
            if (commentUserId.equals(myUserId)) {
                vh.btnDelete.setVisibility(View.VISIBLE);
                long commentId = c.get("id") != null ? ((Number)c.get("id")).longValue() : -1;
                vh.btnDelete.setOnClickListener(v -> {
                    api.deleteComment(commentId, myUserId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            if (onDeleted != null) onDeleted.run();
                        }
                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
                    });
                });
            } else {
                vh.btnDelete.setVisibility(View.GONE);
            }
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvContent, tvTime, btnDelete;
            VH(@NonNull View v) {
                super(v);
                tvAvatar  = v.findViewById(R.id.tv_comment_avatar);
                tvName    = v.findViewById(R.id.tv_comment_name);
                tvContent = v.findViewById(R.id.tv_comment_content);
                tvTime    = v.findViewById(R.id.tv_comment_time);
                btnDelete = v.findViewById(R.id.btn_delete_comment);
            }
        }
    }
}
