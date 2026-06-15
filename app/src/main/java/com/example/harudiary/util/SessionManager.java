package com.example.harudiary.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME     = "haru_session";
    private static final String KEY_USER_ID   = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_PROFILE_URI = "profile_uri";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final int    NO_USER = -1;

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    public void saveLogin(String userId, String name) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    public String getUserId() {
        try {
            return prefs.getString(KEY_USER_ID, "-1");
        } catch (ClassCastException e) {
            int legacyId = prefs.getInt(KEY_USER_ID, -1);
            String strId = String.valueOf(legacyId);
            prefs.edit().putString(KEY_USER_ID, strId).apply();
            return strId;
        }
    }

    public boolean isLoggedIn() {
        String id = getUserId();
        return id != null && !id.equals("-1") && !id.isEmpty();
    }

    public String getLoggedInUserName() { return prefs.getString(KEY_USER_NAME, ""); }

    public void updateUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    /** ★ 프로필 이미지 URI 저장/조회 */
    public void saveProfileImageUri(String uri) {
        prefs.edit().putString(KEY_PROFILE_URI, uri).apply();
    }

    public String getProfileImageUri() {
        return prefs.getString(KEY_PROFILE_URI, null);
    }



    public void logout() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_PROFILE_URI)
            .remove(KEY_IS_LOGGED_IN)
            .apply();
    }
}
