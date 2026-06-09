package com.example.harudiary.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.example.harudiary.receiver.AlarmReceiver;

import java.util.Calendar;

public class AlarmHelper {

    public static final String PREFS_NAME = "haru_alarm_prefs";
    public static final String KEY_ALARM_ENABLED = "alarm_enabled";

    public static final String SLOT_MORNING = "morning";
    public static final String SLOT_LUNCH   = "lunch";
    public static final String SLOT_EVENING = "evening";

    private static final int REQ_MORNING = 101;
    private static final int REQ_LUNCH   = 102;
    private static final int REQ_EVENING = 103;

    public static void setDailyAlarm(Context ctx, String slot, int hour, int minute) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = buildPendingIntent(ctx, slot);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
    }

    public static void cancelAlarm(Context ctx, String slot) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(buildPendingIntent(ctx, slot));
    }

    public static void reregisterAll(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_ALARM_ENABLED, false)) return;

        int mh = prefs.getInt("morning_hour", 8);
        int mm = prefs.getInt("morning_min", 0);
        int lh = prefs.getInt("lunch_hour", 12);
        int lm = prefs.getInt("lunch_min", 30);
        int eh = prefs.getInt("evening_hour", 20);
        int em = prefs.getInt("evening_min", 0);

        setDailyAlarm(ctx, SLOT_MORNING, mh, mm);
        setDailyAlarm(ctx, SLOT_LUNCH,   lh, lm);
        setDailyAlarm(ctx, SLOT_EVENING, eh, em);
    }

    public static void cancelAll(Context ctx) {
        cancelAlarm(ctx, SLOT_MORNING);
        cancelAlarm(ctx, SLOT_LUNCH);
        cancelAlarm(ctx, SLOT_EVENING);
    }

    private static PendingIntent buildPendingIntent(Context ctx, String slot) {
        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.setAction("com.example.harudiary.ALARM");
        intent.putExtra(AlarmReceiver.EXTRA_SLOT, slot);

        int reqCode = requestCode(slot);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(ctx, reqCode, intent, flags);
    }

    private static int requestCode(String slot) {
        switch (slot) {
            case SLOT_MORNING: return REQ_MORNING;
            case SLOT_LUNCH:   return REQ_LUNCH;
            default:           return REQ_EVENING;
        }
    }
}
