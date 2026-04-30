package com.nighttech.dailybazar.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.nighttech.dailybazar.MarketItem;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ItemMarketCardBinding;
import java.util.List;

public class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.ViewHolder> {

    private final List<MarketItem> items;

    public MarketAdapter(List<MarketItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMarketCardBinding binding = ItemMarketCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MarketItem item = items.get(position);
        holder.binding.tvProductName.setText(item.getName());
        holder.binding.tvProductPrice.setText(item.getPrice());
        holder.binding.ivProductImage.setImageResource(item.getImageResId());

        // Handle Trend Icon Logic
        if (item.isTrendUp()) {
            holder.binding.ivTrendIcon.setImageResource(R.drawable.ic_trend_up);
            holder.binding.ivTrendIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark));
        } else {
            holder.binding.ivTrendIcon.setImageResource(R.drawable.ic_trend_down);
            holder.binding.ivTrendIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemMarketCardBinding binding;
        ViewHolder(ItemMarketCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}