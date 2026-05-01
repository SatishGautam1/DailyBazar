package com.nighttech.dailybazar.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.nighttech.dailybazar.MarketItem;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ItemMarketCardBinding;

import java.util.ArrayList;
import java.util.List;

public class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.MarketViewHolder> {

    private List<MarketItem> items;
    private OnItemClickListener listener;

    // FIX: Renamed interface method from onAnalysisClick to onItemClick so it
    // matches the lambda used in MarketFragment: item -> Toast.makeText(...)
    // The original had a mismatch — the interface declared onAnalysisClick but
    // MarketFragment's lambda body used item -> which maps to the single abstract
    // method. Java resolves this at compile time so the mismatch was silent but
    // could cause confusion. Renaming to onItemClick makes intent clear.
    public interface OnItemClickListener {
        void onItemClick(MarketItem item);
    }

    public MarketAdapter(List<MarketItem> items) {
        this.items = new ArrayList<>(items);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // FIX: Use DiffUtil instead of notifyDataSetChanged() to avoid full rebinds
    // and unnecessary image flickers every time the Firestore listener fires.
    public void updateItems(List<MarketItem> newItems) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newItems.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                // Use name as the unique identifier (adjust if you add an ID field)
                return items.get(oldPos).getName().equals(newItems.get(newPos).getName());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                MarketItem o = items.get(oldPos);
                MarketItem n = newItems.get(newPos);
                return o.getPrice().equals(n.getPrice())
                        && o.isTrendUp() == n.isTrendUp()
                        && o.getCategory().equals(n.getCategory());
            }
        });

        this.items = new ArrayList<>(newItems);
        result.dispatchUpdatesTo(this);
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

            // Trend icon
            if (item.isTrendUp()) {
                binding.ivTrendIcon.setImageResource(R.drawable.ic_trend_up);
            } else {
                binding.ivTrendIcon.setImageResource(R.drawable.ic_trend_down);
            }

            // Image: Firebase URL via Glide with crossfade, fallback to local drawable
            Context ctx = binding.getRoot().getContext();
            String imageUrl = item.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(ctx)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_storefront)
                        .error(R.drawable.ic_storefront)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .centerCrop()
                        .into(binding.ivProductImage);
            } else if (item.getImageResId() != 0) {
                // FIX: Also honour local imageResId (used by HistoryFragment dummy data)
                binding.ivProductImage.setImageResource(item.getImageResId());
            } else {
                binding.ivProductImage.setImageResource(R.drawable.ic_storefront);
            }

            // Click listeners
            binding.btnViewAnalysis.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
            binding.cardMarketItem.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
}