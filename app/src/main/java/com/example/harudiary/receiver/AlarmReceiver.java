package com.example.harudiary.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.harudiary.R;
import com.example.harudiary.activity.RecordActivity;
import com.example.harudiary.util.AlarmHelper;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_SLOT = "slot";
    private static final String CHANNEL_ID = "haru_diary_reminders";
    private static final int NOTIF_ID_MORNING = 201;
    private static final int NOTIF_ID_LUNCH   = 202;
    private static final int NOTIF_ID_EVENING = 203;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            AlarmHelper.reregisterAll(context);
            return;
        }

        String slot = intent.getStringExtra(EXTRA_SLOT);
        if (slot == null) return;

        // 알림 채널 생성 (Android 8.0+)
        createChannel(context);

        // 클릭 시 RecordActivity 열기
        Intent tapIntent = new Intent(context, RecordActivity.class);
        tapIntent.putExtra(RecordActivity.EXTRA_SLOT, slot);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(context, notifId(slot), tapIntent, flags);

        String title = slotTitle(slot);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText("오늘의 " + slotKorean(slot) + " 기록을 남겨보세요 📝")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(notifId(slot), builder.build());

        // 다음 날 같은 시간에 재등록
        SharedPreferences prefs = context.getSharedPreferences(AlarmHelper.PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(AlarmHelper.KEY_ALARM_ENABLED, false)) {
            int hour = prefs.getInt(slot + "_hour", defaultHour(slot));
            int min  = prefs.getInt(slot + "_min",  defaultMin(slot));
            AlarmHelper.setDailyAlarm(context, slot, hour, min);
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "하루 기록 알림", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("아침/점심/저녁 기록 알림");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private String slotTitle(String slot) {
        switch (slot) {
            case AlarmHelper.SLOT_MORNING: return "☀️ 아침 기록 시간";
            case AlarmHelper.SLOT_LUNCH:   return "🌤 점심 기록 시간";
            default:                        return "🌙 저녁 기록 시간";
        }
    }

    private String slotKorean(String slot) {
        switch (slot) {
            case AlarmHelper.SLOT_MORNING: return "아침";
            case AlarmHelper.SLOT_LUNCH:   return "점심";
            default:                        return "저녁";
        }
    }

    private int notifId(String slot) {
        switch (slot) {
            case AlarmHelper.SLOT_MORNING: return NOTIF_ID_MORNING;
            case AlarmHelper.SLOT_LUNCH:   return NOTIF_ID_LUNCH;
            default:                        return NOTIF_ID_EVENING;
        }
    }

    private int defaultHour(String slot) {
        switch (slot) {
            case AlarmHelper.SLOT_MORNING: return 8;
            case AlarmHelper.SLOT_LUNCH:   return 12;
            default:                        return 20;
        }
    }

    private int defaultMin(String slot) {
        return AlarmHelper.SLOT_LUNCH.equals(slot) ? 30 : 0;
    }
}
