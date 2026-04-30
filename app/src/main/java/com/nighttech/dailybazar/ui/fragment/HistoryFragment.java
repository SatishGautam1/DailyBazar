package com.nighttech.dailybazar.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nighttech.dailybazar.MarketItem;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.FragmentHistoryBinding;
import com.nighttech.dailybazar.adapter.MarketAdapter;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupHistoryList();
    }

    private void setupHistoryList() {
        // Use LinearLayoutManager for a vertical list in History
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        // Dummy Data: Reusing MarketItem for consistency
        List<MarketItem> historyList = new ArrayList<>();
        historyList.add(new MarketItem("Mustard Oil", "Rs. 210/Ltr", R.drawable.ic_storefront, false, "Oil"));
        historyList.add(new MarketItem("Basmati Rice", "Rs. 180/kg", R.drawable.ic_storefront, true, "Grains"));
        historyList.add(new MarketItem("Sugar", "Rs. 95/kg", R.drawable.ic_storefront, true, "Essentials"));

        // Using the same MarketAdapter we created earlier
        MarketAdapter adapter = new MarketAdapter(historyList);
        binding.rvHistory.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}