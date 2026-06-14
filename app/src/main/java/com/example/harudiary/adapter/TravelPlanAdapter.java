package com.example.harudiary.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.example.harudiary.R;
import com.example.harudiary.model.PlaceDto;

import java.util.List;

public class TravelPlanAdapter extends RecyclerView.Adapter<TravelPlanAdapter.ViewHolder> {

    public interface OnVisitCompleteListener {
        void onVisitComplete(PlaceDto place);
    }

    private static final int TYPE_FOOD = 0;
    private static final int TYPE_TOUR = 1;
    private static final int TYPE_LODGING = 2;
    private static final int TYPE_TRANSPORT = 3;
    private static final int TYPE_DEFAULT = 4;
    private static final int TYPE_HEADER = 5;

    private List<PlaceDto> places;
    private OnVisitCompleteListener listener;

    private boolean isConfirmMode;

    public TravelPlanAdapter(List<PlaceDto> places, OnVisitCompleteListener listener, boolean isConfirmMode) {
        this.places = places;
        this.listener = listener;
        this.isConfirmMode = isConfirmMode;
    }

    public void update(List<PlaceDto> newPlaces) {
        this.places = newPlaces;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        String category = places.get(position).getPlaceCategory();
        if ("HEADER".equals(category)) return TYPE_HEADER;
        if (category == null) return TYPE_DEFAULT;

        switch (category) {
            case "식도락":
                return TYPE_FOOD;
            case "관광/문화":
                return TYPE_TOUR;
            case "숙박/휴식":
                return TYPE_LODGING;
            case "교통/편의":
                return TYPE_TRANSPORT;
            default:
                return TYPE_DEFAULT;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        switch (viewType) {
            case TYPE_HEADER:
                layoutId = R.layout.item_travel_place_header;
                break;
            case TYPE_FOOD:
                layoutId = R.layout.item_travel_place_food;
                break;
            case TYPE_TOUR:
                layoutId = R.layout.item_travel_place_tour;
                break;
            case TYPE_LODGING:
                layoutId = R.layout.item_travel_place_lodging;
                break;
            case TYPE_TRANSPORT:
                layoutId = R.layout.item_travel_place_transport;
                break;
            default:
                layoutId = R.layout.item_travel_place_default;
                break;
        }

        View v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceDto place = places.get(position);
        holder.tvPlaceName.setText(place.getPlaceName());

        if (getItemViewType(position) == TYPE_HEADER) {
            return; // Headers only have tvPlaceName
        }

        String info = "다음 장소까지: " + place.getTravelTimeMinutesToNext() + "분 (" + place.getTransportMode() + ")";
        holder.tvTravelInfo.setText(info);
        
        if (place.getTimeSpentHours() != null) {
            holder.tvTimeSpent.setText("예상 체류 시간: " + place.getTimeSpentHours() + "시간");
            holder.tvTimeSpent.setVisibility(View.VISIBLE);
        } else {
            holder.tvTimeSpent.setVisibility(View.GONE);
        }

        // 초기화
        if (holder.layoutWebview != null) {
            holder.layoutWebview.setVisibility(View.GONE);
        }

        // Add Visit button dynamically
        if (holder.layoutHeader != null && !isConfirmMode) {
            android.widget.Button btnVisit = holder.itemView.findViewWithTag("btnVisit");
            if (btnVisit == null) {
                btnVisit = new android.widget.Button(holder.itemView.getContext());
                btnVisit.setTag("btnVisit");
                btnVisit.setText("✅ 방문");
                btnVisit.setTextSize(12);
                btnVisit.setPadding(8, 0, 8, 0);
                android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(16, 0, 0, 0);
                holder.layoutHeader.addView(btnVisit, params);
            }
            
            if (place.isVisited()) {
                btnVisit.setText("✅ 방문 완료");
                btnVisit.setEnabled(false);
                btnVisit.setBackgroundColor(android.graphics.Color.LTGRAY);
            } else {
                btnVisit.setText("✅ 방문");
                btnVisit.setEnabled(true);
                btnVisit.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")); // Green or default
            }
            
            btnVisit.setOnClickListener(v -> {
                if (listener != null && !place.isVisited()) listener.onVisitComplete(place);
            });
        }

        // 카드 전체 클릭 이벤트
        holder.itemView.setOnClickListener(v -> {
            if (holder.layoutWebview != null) {
                if (holder.layoutWebview.getVisibility() == View.GONE) {
                    holder.layoutWebview.setVisibility(View.VISIBLE);
                    String url = place.getPlaceUrl();
                    if (url != null && !url.isEmpty() && holder.webView != null) {
                        WebSettings webSettings = holder.webView.getSettings();
                        webSettings.setJavaScriptEnabled(true);
                        holder.webView.setWebViewClient(new WebViewClient());
                        holder.webView.loadUrl(url);
                    }
                } else {
                    holder.layoutWebview.setVisibility(View.GONE);
                }
            }
        });

        // 닫기 버튼 클릭 이벤트
        if (holder.btnClose != null) {
            holder.btnClose.setOnClickListener(v -> {
                holder.layoutWebview.setVisibility(View.GONE);
            });
        }
    }

    @Override
    public int getItemCount() {
        return places == null ? 0 : places.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaceName, tvTravelInfo, tvTimeSpent;
        FrameLayout layoutWebview;
        WebView webView;
        ImageButton btnClose;
        android.widget.LinearLayout layoutHeader;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tv_place_name);
            tvTravelInfo = itemView.findViewById(R.id.tv_travel_info);
            tvTimeSpent = itemView.findViewById(R.id.tv_time_spent);
            
            layoutWebview = itemView.findViewById(R.id.layout_webview);
            webView = itemView.findViewById(R.id.webview);
            btnClose = itemView.findViewById(R.id.btn_close_webview);
            layoutHeader = itemView.findViewById(R.id.layout_header);
        }
    }
}
