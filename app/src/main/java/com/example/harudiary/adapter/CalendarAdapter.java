package com.example.harudiary.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.harudiary.R;

import java.util.Calendar;
import java.util.Set;

/**
 * CalendarAdapter — 월별/주간 캘린더 그리드 어댑터
 * ★ weekMode: 오늘이 포함된 주(행)만 표시 (접기 기능)
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private static final String[] HEADERS = {"일", "월", "화", "수", "목", "금", "토"};

    private int year, month, selectedDay, todayDay;
    private Set<String> recordDates;
    private int firstDayOffset, daysInMonth;

    // ★ 주간 모드
    private boolean weekMode = false;
    private int[] weekDays = new int[7]; // 현재 주의 7개 day값 (0=빈 셀)

    public interface OnDayClickListener { void onDayClick(String date); }
    private final OnDayClickListener listener;

    public CalendarAdapter(int year, int month, int selectedDay, int todayDay,
                           Set<String> recordDates, OnDayClickListener listener) {
        this.listener = listener;
        updateMonth(year, month, selectedDay, todayDay, recordDates);
    }

    public void updateMonth(int year, int month, int selectedDay, int todayDay,
                            Set<String> recordDates) {
        this.year = year; this.month = month;
        this.selectedDay = selectedDay; this.todayDay = todayDay;
        this.recordDates = recordDates;

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - 1;
        daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        calculateWeekDays(todayDay > 0 ? todayDay : 1);
        notifyDataSetChanged();
    }

    /** ★ 오늘이 포함된 행의 7개 날짜 계산 */
    private void calculateWeekDays(int day) {
        // 그리드에서 해당 day의 위치 (0-based)
        int pos = firstDayOffset + day - 1;
        int rowStart = (pos / 7) * 7; // 행 시작 위치

        for (int i = 0; i < 7; i++) {
            int gridPos = rowStart + i;
            int d = gridPos < firstDayOffset ? 0 : gridPos - firstDayOffset + 1;
            if (d < 1 || d > daysInMonth) d = 0;
            weekDays[i] = d;
        }
    }

    /** ★ 주간/월간 모드 전환 (애니메이션은 Fragment에서 처리) */
    public void setWeekMode(boolean weekMode) {
        this.weekMode = weekMode;
        notifyDataSetChanged();
    }

    public boolean isWeekMode() { return weekMode; }

    public void setRecordDates(Set<String> dates) {
        this.recordDates = dates;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (weekMode) return 7 + 7; // 헤더 + 주간 7개
        return 7 + firstDayOffset + daysInMonth;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder vh, int position) {
        int col = position % 7;

        // 요일 헤더
        if (position < 7) {
            vh.tvDay.setText(HEADERS[position]);
            vh.tvDay.setTextSize(12);
            vh.tvDay.setBackground(null);
            vh.vDot.setVisibility(View.INVISIBLE);
            vh.itemView.setClickable(false);
            vh.tvDay.setTextColor(headerColor(vh, col));
            return;
        }

        // 날짜 셀
        int day;
        if (weekMode) {
            day = weekDays[position - 7];
        } else {
            int dayIndex = position - 7;
            day = (dayIndex < firstDayOffset) ? 0 : dayIndex - firstDayOffset + 1;
        }

        if (day <= 0 || day > daysInMonth) {
            vh.tvDay.setText("");
            vh.tvDay.setBackground(null);
            vh.vDot.setVisibility(View.INVISIBLE);
            vh.itemView.setClickable(false);
            return;
        }

        vh.tvDay.setText(String.valueOf(day));
        vh.tvDay.setTextSize(13);
        vh.itemView.setClickable(true);

        String dateStr = String.format("%04d-%02d-%02d", year, month + 1, day);
        boolean isHighlighted = (day == selectedDay) || (day == todayDay);

        if (isHighlighted) {
            vh.tvDay.setBackground(ContextCompat.getDrawable(vh.itemView.getContext(),
                    R.drawable.bg_calendar_selected));
            vh.tvDay.setTextColor(Color.WHITE);
        } else {
            vh.tvDay.setBackground(null);
            vh.tvDay.setTextColor(dayColor(vh, col));
        }

        boolean hasDot = recordDates != null && recordDates.contains(dateStr);
        vh.vDot.setVisibility(hasDot ? View.VISIBLE : View.INVISIBLE);

        vh.itemView.setOnClickListener(v -> { if (listener != null) listener.onDayClick(dateStr); });
    }

    private int headerColor(DayViewHolder vh, int col) {
        if (col == 0) return ContextCompat.getColor(vh.itemView.getContext(), R.color.danger);
        if (col == 6) return ContextCompat.getColor(vh.itemView.getContext(), R.color.primary_blue);
        return ContextCompat.getColor(vh.itemView.getContext(), R.color.gray_label);
    }

    private int dayColor(DayViewHolder vh, int col) {
        if (col == 0) return ContextCompat.getColor(vh.itemView.getContext(), R.color.danger);
        if (col == 6) return ContextCompat.getColor(vh.itemView.getContext(), R.color.primary_blue);
        return ContextCompat.getColor(vh.itemView.getContext(), R.color.primary);
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay; View vDot;
        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            vDot = itemView.findViewById(R.id.v_dot);
        }
    }
}
