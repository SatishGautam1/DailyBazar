// com/nighttech/dailybazar/adapter/LivePriceAdapter.java
package com.nighttech.dailybazar.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.nighttech.dailybazar.LivePriceItem;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ItemMarketCardBinding;

import java.util.ArrayList;
import java.util.List;

public class LivePriceAdapter extends RecyclerView.Adapter<LivePriceAdapter.LivePriceViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(LivePriceItem item);
    }

    private List<LivePriceItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    public void updateItems(List<LivePriceItem> newItems) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newItems.size(); }
            @Override public boolean areItemsTheSame(int o, int n) {
                return items.get(o).getName().equals(newItems.get(n).getName());
            }
            @Override public boolean areContentsTheSame(int o, int n) {
                LivePriceItem a = items.get(o), b = newItems.get(n);
                return a.getPrice().equals(b.getPrice()) && a.isTrendUp() == b.isTrendUp();
            }
        });
        this.items = new ArrayList<>(newItems);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public LivePriceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuses the same card layout as MarketAdapter — consistent look
        ItemMarketCardBinding binding = ItemMarketCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LivePriceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LivePriceViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class LivePriceViewHolder extends RecyclerView.ViewHolder {
        private final ItemMarketCardBinding binding;

        LivePriceViewHolder(ItemMarketCardBinding b) {
            super(b.getRoot());
            this.binding = b;
        }

        void bind(LivePriceItem item) {
            binding.tvProductName.setText(item.getName());
            binding.tvProductPrice.setText(item.getPrice());
            binding.chipCategory.setText(
                    item.getUnit().isEmpty() ? item.getCategory() : item.getCategory() + " · " + item.getUnit());
            binding.ivTrendIcon.setImageResource(
                    item.isTrendUp() ? R.drawable.ic_trend_up : R.drawable.ic_trend_down);

            String url = item.getImageUrl();
            if (url != null && !url.isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(url)
                        .placeholder(R.drawable.ic_storefront)
                        .error(R.drawable.ic_storefront)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .centerCrop()
                        .into(binding.ivProductImage);
            } else {
                binding.ivProductImage.setImageResource(R.drawable.ic_storefront);
            }

            binding.cardMarketItem.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
            binding.btnViewAnalysis.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
}