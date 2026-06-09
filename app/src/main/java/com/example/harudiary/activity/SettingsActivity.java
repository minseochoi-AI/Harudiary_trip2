package com.example.harudiary.activity;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.harudiary.R;
import com.example.harudiary.db.DBHelper;
import com.example.harudiary.db.UserDAO;
import com.example.harudiary.model.User;
import com.example.harudiary.util.AlarmHelper;
import com.example.harudiary.util.SessionManager;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private SessionManager session;

    private SwitchCompat switchAlarm;
    private TextView tvMorning, tvLunch, tvEvening;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);

        prefs = getSharedPreferences(AlarmHelper.PREFS_NAME, MODE_PRIVATE);
        session = new SessionManager(this);

        initViews();
        loadUserEmail();
        loadAlarmSettings();
        setupListeners();
    }

    private void initViews() {
        switchAlarm = findViewById(R.id.switch_alarm);
        tvMorning   = findViewById(R.id.tv_alarm_morning);
        tvLunch     = findViewById(R.id.tv_alarm_lunch);
        tvEvening   = findViewById(R.id.tv_alarm_evening);
    }

    private void loadUserEmail() {
        int userId = session.getLoggedInUserId();
        UserDAO userDAO = new UserDAO(DBHelper.getInstance(this));
        User user = userDAO.getUserById(userId);
        TextView tvEmail = findViewById(R.id.tv_user_email);
        if (user != null) tvEmail.setText(user.getEmail());
    }

    private void loadAlarmSettings() {
        boolean enabled = prefs.getBoolean(AlarmHelper.KEY_ALARM_ENABLED, false);
        switchAlarm.setChecked(enabled);

        tvMorning.setText(formatTime(prefs.getInt("morning_hour", 8),  prefs.getInt("morning_min", 0)));
        tvLunch.setText(  formatTime(prefs.getInt("lunch_hour",   12), prefs.getInt("lunch_min",  30)));
        tvEvening.setText(formatTime(prefs.getInt("evening_hour", 20), prefs.getInt("evening_min", 0)));
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        switchAlarm.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(AlarmHelper.KEY_ALARM_ENABLED, isChecked).apply();
            if (isChecked) {
                AlarmHelper.reregisterAll(this);
            } else {
                AlarmHelper.cancelAll(this);
            }
        });

        findViewById(R.id.row_alarm_morning).setOnClickListener(v ->
                showTimePicker(AlarmHelper.SLOT_MORNING, tvMorning,
                        prefs.getInt("morning_hour", 8), prefs.getInt("morning_min", 0)));

        findViewById(R.id.row_alarm_lunch).setOnClickListener(v ->
                showTimePicker(AlarmHelper.SLOT_LUNCH, tvLunch,
                        prefs.getInt("lunch_hour", 12), prefs.getInt("lunch_min", 30)));

        findViewById(R.id.row_alarm_evening).setOnClickListener(v ->
                showTimePicker(AlarmHelper.SLOT_EVENING, tvEvening,
                        prefs.getInt("evening_hour", 20), prefs.getInt("evening_min", 0)));

        findViewById(R.id.btn_logout).setOnClickListener(v -> logout());
        findViewById(R.id.btn_withdraw).setOnClickListener(v -> confirmWithdraw());
    }

    private void showTimePicker(String slot, TextView tvDisplay, int defHour, int defMin) {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            prefs.edit()
                    .putInt(slot + "_hour", hourOfDay)
                    .putInt(slot + "_min", minute)
                    .apply();
            tvDisplay.setText(formatTime(hourOfDay, minute));

            if (prefs.getBoolean(AlarmHelper.KEY_ALARM_ENABLED, false)) {
                AlarmHelper.setDailyAlarm(this, slot, hourOfDay, minute);
            }
        }, defHour, defMin, true).show();
    }

    private void logout() {
        AlarmHelper.cancelAll(this);
        session.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void confirmWithdraw() {
        new AlertDialog.Builder(this)
                .setTitle("회원탈퇴")
                .setMessage("탈퇴하면 모든 기록이 삭제됩니다.\n정말 탈퇴하시겠습니까?")
                .setPositiveButton("탈퇴", (dialog, which) -> withdraw())
                .setNegativeButton("취소", null)
                .show();
    }

    private void withdraw() {
        int userId = session.getLoggedInUserId();
        UserDAO userDAO = new UserDAO(DBHelper.getInstance(this));
        userDAO.deleteUser(userId);
        AlarmHelper.cancelAll(this);
        session.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String formatTime(int hour, int min) {
        return String.format(Locale.KOREA, "%02d:%02d", hour, min);
    }
}
