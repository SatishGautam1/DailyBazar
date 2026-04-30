package com.nighttech.dailybazar.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.nighttech.dailybazar.MarketItem;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.FragmentMarketBinding;
import java.util.ArrayList;
import java.util.List;

public class MarketFragment extends Fragment {

    private FragmentMarketBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMarketBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        // 2-Column Grid as defined in your XML tools
        binding.rvMarketItems.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Dummy Data for Night Tech / DailyBazar
        List<MarketItem> itemList = new ArrayList<>();
        itemList.add(new MarketItem("Tomato", "Rs. 120/kg", R.drawable.ic_storefront, true, "Vegetable"));
        itemList.add(new MarketItem("Potato", "Rs. 60/kg", R.drawable.ic_storefront, false, "Vegetable"));
        itemList.add(new MarketItem("Onion", "Rs. 90/kg", R.drawable.ic_storefront, true, "Vegetable"));
        itemList.add(new MarketItem("Rice", "Rs. 2500/bag", R.drawable.ic_storefront, false, "Grain"));

        // You will need the MarketAdapter (Code provided below)
        // MarketAdapter adapter = new MarketAdapter(itemList);
        // binding.rvMarketItems.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}