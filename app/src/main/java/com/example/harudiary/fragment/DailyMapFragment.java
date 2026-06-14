package com.example.harudiary.fragment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.harudiary.R;
import com.example.harudiary.api.DiaryApi;
import com.example.harudiary.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.harudiary.model.Record;
import com.example.harudiary.util.SessionManager;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DailyMapFragment
 *
 * 트러플(Truffl) 스타일 카드 마커:
 *   ┌──────────────────────────┐
 *   │  (1)  09:30  아침         │
 *   │       청라에메랄드로 112   │
 *   └────────────┬─────────────┘
 *                ▼  (포인터)
 *
 * - 번호 원: 슬롯 색상 (아침=주황, 점심=초록, 저녁=보라, 기타=파랑)
 * - 시간: 굵게, 슬롯 색상
 * - 주소: 작게, 회색
 */
public class DailyMapFragment extends Fragment {

    private static final String TAG = "DailyMapFragment";
    private static final String ARG_DATE = "date";

    private MapView  mapView;
    private TextView tvEmpty;
    private final List<Record> located = new ArrayList<>();
    private boolean mapStarted = false;

    // 슬롯 색상
    private static final int COLOR_MORNING = 0xFFF5A623; // 아침: 주황
    private static final int COLOR_LUNCH   = 0xFF4CAF50; // 점심: 초록
    private static final int COLOR_EVENING = 0xFF9C6BC4; // 저녁: 보라
    private static final int COLOR_OTHER   = 0xFF5B8DEF; // 기타: 파랑

    public static DailyMapFragment newInstance(String date) {
        DailyMapFragment f = new DailyMapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String date   = getArguments() != null ? getArguments().getString(ARG_DATE, "") : "";
        String userId = new SessionManager(requireContext()).getUserId();

        tvEmpty = view.findViewById(R.id.tv_map_empty);
        mapView = view.findViewById(R.id.mapView);

        DiaryApi diaryApi = RetrofitClient.getClient().create(DiaryApi.class);
        diaryApi.getActivitiesByDate(userId, date).enqueue(new Callback<List<Record>>() {
            @Override
            public void onResponse(@NonNull Call<List<Record>> call, @NonNull Response<List<Record>> response) {
                List<Record> records = (response.isSuccessful() && response.body() != null) ? response.body() : new ArrayList<>();
                processRecords(records);
            }
            @Override
            public void onFailure(@NonNull Call<List<Record>> call, @NonNull Throwable t) {
                processRecords(new ArrayList<>());
            }
        });
    }

    private void processRecords(List<Record> records) {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            located.clear();
            for (Record r : records) {
                if (r.getLatitude() != 0 || r.getLongitude() != 0) located.add(r);
            }

            if (located.isEmpty()) {
                mapView.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("위치 정보가 있는 기록이 없습니다");
                return;
            }

            mapView.start(new MapLifeCycleCallback() {
                @Override public void onMapDestroy() { }

                @Override
                public void onMapError(Exception error) {
                    Log.e(TAG, "지도 오류: " + error.getMessage());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (mapView != null) mapView.setVisibility(View.GONE);
                            if (tvEmpty != null) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                tvEmpty.setText("지도 오류: " + error.getMessage());
                            }
                            Toast.makeText(getContext(),
                                    "[지도 오류] " + error.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
            }, new KakaoMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull KakaoMap kakaoMap) {
                    mapStarted = true;
                    try {
                        addCardMarkers(kakaoMap, located);
                    } catch (Exception e) {
                        Log.e(TAG, "마커 추가 오류", e);
                        Toast.makeText(getContext(),
                                "마커 오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 마커 추가
    // ─────────────────────────────────────────────────────────────

    private void addCardMarkers(KakaoMap kakaoMap, List<Record> records) {
        LabelLayer layer = kakaoMap.getLabelManager().getLayer();

        for (int i = 0; i < records.size(); i++) {
            Record r = records.get(i);
            LatLng  pos    = LatLng.from(r.getLatitude(), r.getLongitude());
            Bitmap  marker = buildMarkerBitmap(i + 1, r);

            // 앵커: 삼각형 포인터 끝(하단 중앙)이 좌표에 닿도록 (0.5, 1.0)
            LabelStyle style = LabelStyle.from(marker)
                    .setAnchorPoint(0.5f, 1.0f);

            LabelStyles styles = kakaoMap.getLabelManager()
                    .addLabelStyles(LabelStyles.from(style));
            layer.addLabel(LabelOptions.from(pos).setStyles(styles));
        }

        // 카메라: 마커가 1개면 zoom 15, 여러 개면 13
        Record first = records.get(0);
        int zoom = records.size() == 1 ? 15 : 13;
        kakaoMap.moveCamera(
                CameraUpdateFactory.newCenterPosition(
                        LatLng.from(first.getLatitude(), first.getLongitude()), zoom));
    }

    // ─────────────────────────────────────────────────────────────
    // 카드 마커 비트맵 생성
    //
    //   ┌──────────────────────────┐  ← cardH
    //   │  [No] 시간  슬롯           │
    //   │       장소명 (gray)        │
    //   └────────────┬─────────────┘
    //                ▼ tailH
    // ─────────────────────────────────────────────────────────────
    private Bitmap buildMarkerBitmap(int index, Record record) {
        float d = requireContext().getResources().getDisplayMetrics().density;

        // 치수 (dp→px)
        int cardW  = px(d, 170);
        int cardH  = px(d, 54);
        int tailH  = px(d, 9);
        int corner = px(d, 10);

        int cirR   = px(d, 15);   // 번호 원 반지름
        int padL   = px(d, 10);   // 카드 좌측 패딩
        int padR   = px(d, 8);    // 카드 우측 패딩
        int gap    = px(d, 7);    // 원과 텍스트 간격

        int totalH = cardH + tailH;
        Bitmap bmp = Bitmap.createBitmap(cardW, totalH, Bitmap.Config.ARGB_8888);
        Canvas cv  = new Canvas(bmp);

        int slotColor = slotColor(record.getTimeSlot());

        // ① 카드 흰색 배경 (그림자 효과)
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);
        bgPaint.setShadowLayer(px(d, 3), 0, px(d, 2), 0x44000000);
        cv.drawRoundRect(new RectF(0, 0, cardW, cardH), corner, corner, bgPaint);

        // ② 삼각형 포인터 (카드 하단 중앙)
        Paint tailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tailPaint.setColor(Color.WHITE);
        float cx = cardW / 2f;
        Path tail = new Path();
        tail.moveTo(cx - px(d, 8), cardH);
        tail.lineTo(cx + px(d, 8), cardH);
        tail.lineTo(cx, cardH + tailH);
        tail.close();
        cv.drawPath(tail, tailPaint);

        // ③ 슬롯 색 왼쪽 바 (카드 좌측 강조선)
        Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(slotColor);
        cv.drawRoundRect(new RectF(0, 0, px(d, 4), cardH), px(d, 2), px(d, 2), barPaint);

        // ④ 번호 원
        Paint cirPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cirPaint.setColor(slotColor);
        float cirX = padL + px(d, 4) + cirR; // 왼쪽 바 뒤에 배치
        float cirY = cardH / 2f;
        cv.drawCircle(cirX, cirY, cirR, cirPaint);

        // ⑤ 번호 텍스트
        Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numPaint.setColor(Color.WHITE);
        numPaint.setTextSize(px(d, 13));
        numPaint.setTypeface(Typeface.DEFAULT_BOLD);
        numPaint.setTextAlign(Paint.Align.CENTER);
        float numY = cirY - (numPaint.ascent() + numPaint.descent()) / 2f;
        cv.drawText(String.valueOf(index), cirX, numY, numPaint);

        // ⑥ 시간 텍스트
        String time = new SimpleDateFormat("HH:mm", Locale.KOREA)
                .format(new Date(record.getTimestamp()));
        Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setColor(slotColor);
        timePaint.setTextSize(px(d, 13));
        timePaint.setTypeface(Typeface.DEFAULT_BOLD);
        float textX = cirX + cirR + gap;
        float timeY = cirY - px(d, 4);
        cv.drawText(time, textX, timeY, timePaint);

        // ⑦ 슬롯 라벨 (아침/점심/저녁) — 시간 옆에 작게
        String slotLabel = slotLabel(record.getTimeSlot());
        if (!slotLabel.isEmpty()) {
            Paint slPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            slPaint.setColor(slotColor);
            slPaint.setAlpha(170);
            slPaint.setTextSize(px(d, 10));
            float slotX = textX + timePaint.measureText(time) + px(d, 4);
            cv.drawText(slotLabel, slotX, timeY, slPaint);
        }

        // ⑧ 주소 텍스트 (회색, 작게)
        String addr = record.getAddress();
        if (addr == null || addr.isEmpty()) addr = "위치 정보";
        Paint addrPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        addrPaint.setColor(0xFF666666);
        addrPaint.setTextSize(px(d, 11));
        float maxW = cardW - textX - padR;
        addr = ellipsize(addr, addrPaint, maxW);
        float addrY = cirY + px(d, 13);
        cv.drawText(addr, textX, addrY, addrPaint);

        return bmp;
    }

    // ─────────────────────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────────────────────

    private int px(float density, int dp) {
        return Math.round(density * dp);
    }

    private int slotColor(@Nullable String slot) {
        if ("morning".equals(slot)) return COLOR_MORNING;
        if ("lunch".equals(slot))   return COLOR_LUNCH;
        if ("evening".equals(slot)) return COLOR_EVENING;
        return COLOR_OTHER;
    }

    private String slotLabel(@Nullable String slot) {
        if ("morning".equals(slot)) return "아침";
        if ("lunch".equals(slot))   return "점심";
        if ("evening".equals(slot)) return "저녁";
        return "";
    }

    /** 텍스트가 maxWidth 초과 시 "..." 처리 */
    private String ellipsize(String text, Paint paint, float maxWidth) {
        if (paint.measureText(text) <= maxWidth) return text;
        while (text.length() > 1 && paint.measureText(text + "…") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "…";
    }

    // ─────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        if (mapStarted && mapView != null) mapView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapStarted && mapView != null) mapView.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.finish();
            mapView = null;
        }
        mapStarted = false;
    }
}
