package com.example.harudiary.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.harudiary.MainActivity;
import com.example.harudiary.R;
import com.example.harudiary.db.DBHelper;
import com.example.harudiary.db.UserDAO;
import com.example.harudiary.model.User;
import com.example.harudiary.util.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvSignUp = findViewById(R.id.tv_signup);

        btnLogin.setOnClickListener(v -> onLoginClick());
        tvSignUp.setOnClickListener(v ->
            startActivity(new Intent(this, SignUpActivity.class)));
    }

    private void onLoginClick() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.msg_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("id", email);
        payload.put("password", password);

        com.example.harudiary.api.RetrofitClient.getInstance().create(com.example.harudiary.api.UserApi.class).login(payload).enqueue(new retrofit2.Callback<User>() {
            @Override
            public void onResponse(retrofit2.Call<User> call, retrofit2.Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    new SessionManager(LoginActivity.this).saveLogin(user.getUserId(), user.getName());
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, R.string.msg_login_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<User> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
