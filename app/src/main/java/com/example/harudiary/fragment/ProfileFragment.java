package com.example.harudiary.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.harudiary.R;
import com.example.harudiary.activity.LoginActivity;
import com.example.harudiary.db.DBHelper;
import com.example.harudiary.db.UserDAO;
import com.example.harudiary.model.User;
import com.example.harudiary.util.SessionManager;

/**
 * ProfileFragment — 이름 변경 + 프로필 이미지 설정
 */
public class ProfileFragment extends Fragment {

    private static final int REQ_PICK_IMAGE = 2001;

    private SessionManager session;
    private UserDAO userDAO;
    private int userId;

    private TextView tvAvatar, tvEmail, btnSave;
    private EditText etName;
    private ImageView ivProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        session = new SessionManager(requireContext());
        userDAO = new UserDAO(DBHelper.getInstance(requireContext()));
        userId  = session.getLoggedInUserId();

        tvAvatar  = view.findViewById(R.id.tv_profile_avatar);
        tvEmail   = view.findViewById(R.id.tv_profile_email);
        etName    = view.findViewById(R.id.et_profile_name);
        ivProfile = view.findViewById(R.id.iv_profile_image);
        btnSave   = view.findViewById(R.id.btn_save_profile);

        // 현재 정보 로드
        loadUserInfo();

        // 프로필 사진 변경
        view.findViewById(R.id.btn_change_photo).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQ_PICK_IMAGE);
        });

        // 저장
        btnSave.setOnClickListener(v -> saveProfile());

        // 로그아웃
        view.findViewById(R.id.btn_logout).setOnClickListener(v ->
            new AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("확인", (d, w) -> {
                    session.logout();
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                    requireActivity().finish();
                })
                .setNegativeButton("취소", null)
                .show()
        );

        // 회원탈퇴
        view.findViewById(R.id.btn_withdraw).setOnClickListener(v ->
            new AlertDialog.Builder(requireContext())
                .setTitle("회원탈퇴")
                .setMessage("탈퇴 시 모든 기록이 삭제됩니다.\n정말 탈퇴하시겠습니까?")
                .setPositiveButton("탈퇴", (d, w) -> {
                    userDAO.deleteUser(userId);
                    session.logout();
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                    requireActivity().finish();
                })
                .setNegativeButton("취소", null)
                .show()
        );

        return view;
    }

    private void loadUserInfo() {
        User user = userDAO.getUserById(userId);
        if (user == null) return;

        String name = user.getName();
        String initial = (name != null && !name.isEmpty()) ? name.substring(0, 1) : "?";
        tvAvatar.setText(initial);
        etName.setText(name);
        tvEmail.setText(user.getEmail());

        // 저장된 프로필 이미지
        String profileUri = session.getProfileImageUri();
        if (profileUri != null) {
            try {
                ivProfile.setImageURI(Uri.parse(profileUri));
                ivProfile.setVisibility(View.VISIBLE);
                tvAvatar.setVisibility(View.GONE);
            } catch (Exception e) {
                ivProfile.setVisibility(View.GONE);
                tvAvatar.setVisibility(View.VISIBLE);
            }
        }
    }

    private void saveProfile() {
        String newName = etName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(requireContext(), "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userDAO.updateUserName(userId, newName)) {
            session.updateUserName(newName);
            // 아바타 이니셜 갱신
            tvAvatar.setText(newName.substring(0, 1));
            Toast.makeText(requireContext(), "프로필이 저장되었습니다", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "저장에 실패했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE
                && resultCode == android.app.Activity.RESULT_OK
                && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                session.saveProfileImageUri(uri.toString());
                try {
                    ivProfile.setImageURI(uri);
                    ivProfile.setVisibility(View.VISIBLE);
                    tvAvatar.setVisibility(View.GONE);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "이미지 불러오기 실패", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
