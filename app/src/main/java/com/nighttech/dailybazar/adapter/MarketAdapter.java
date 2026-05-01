package com.nighttech.dailybazar.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.nighttech.dailybazar.MarketItem;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ItemMarketCardBinding;

import java.util.List;

public class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.MarketViewHolder> {

    private List<MarketItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onAnalysisClick(MarketItem item);
    }

    public MarketAdapter(List<MarketItem> items) {
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateItems(List<MarketItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MarketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMarketCardBinding binding = ItemMarketCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MarketViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MarketViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    class MarketViewHolder extends RecyclerView.ViewHolder {

        private final ItemMarketCardBinding binding;

        MarketViewHolder(ItemMarketCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MarketItem item) {
            binding.tvProductName.setText(item.getName());
            binding.tvProductPrice.setText(item.getPrice());
            binding.chipCategory.setText(item.getCategory());

            // Trend icon and color
            if (item.isTrendUp()) {
                binding.ivTrendIcon.setImageResource(R.drawable.ic_trend_up);
            } else {
                binding.ivTrendIcon.setImageResource(R.drawable.ic_trend_down);
            }

            // Image loading: Firebase Storage URL via Glide with shimmer placeholder
            Context ctx = binding.getRoot().getContext();
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(ctx)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_storefront)
                        .error(R.drawable.ic_storefront)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .centerCrop()
                        .into(binding.ivProductImage);
            } else {
                binding.ivProductImage.setImageResource(R.drawable.ic_storefront);
            }

            // CTA click
            binding.btnViewAnalysis.setOnClickListener(v -> {
                if (listener != null) listener.onAnalysisClick(item);
            });

            // Card ripple click
            binding.cardMarketItem.setOnClickListener(v -> {
                if (listener != null) listener.onAnalysisClick(item);
            });
        }
    }
}