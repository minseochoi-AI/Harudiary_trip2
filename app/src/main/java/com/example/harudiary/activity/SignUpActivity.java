package com.example.harudiary.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.harudiary.R;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etPasswordConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etPasswordConfirm = findViewById(R.id.et_password_confirm);
        Button btnSignUp = findViewById(R.id.btn_signup);
        TextView tvLogin = findViewById(R.id.tv_login);

        btnSignUp.setOnClickListener(v -> onSignUpClick());
        tvLogin.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onSignUpClick() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String passwordConfirm = etPasswordConfirm.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(password) || TextUtils.isEmpty(passwordConfirm)) {
            Toast.makeText(this, R.string.msg_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.msg_invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, R.string.msg_password_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("id", email);
        payload.put("nickname", name);

        com.example.harudiary.api.RetrofitClient.getInstance().create(com.example.harudiary.api.UserApi.class).register(payload).enqueue(new retrofit2.Callback<com.example.harudiary.model.User>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.harudiary.model.User> call, retrofit2.Response<com.example.harudiary.model.User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SignUpActivity.this, R.string.msg_signup_complete, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(SignUpActivity.this, "Signup Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.harudiary.model.User> call, Throwable t) {
                Toast.makeText(SignUpActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
