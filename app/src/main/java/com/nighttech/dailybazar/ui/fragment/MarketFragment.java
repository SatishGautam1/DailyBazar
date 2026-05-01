package com.nighttech.dailybazar.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.search.SearchBar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.nighttech.dailybazar.MarketItem;
import com.nighttech.dailybazar.adapter.MarketAdapter;
import com.nighttech.dailybazar.databinding.FragmentMarketBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MarketFragment extends Fragment {

    private FragmentMarketBinding binding;
    private MarketAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;

    // Holds the complete unfiltered list from Firestore
    private final List<MarketItem> allItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMarketBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        setTodayDate();
        setupRecyclerView();
        setupSwipeRefresh();
        setupSearchBar();
        startShimmer();
        listenToFirestore();
    }

    // ── Date ─────────────────────────────────────────────────────────────────

    private void setTodayDate() {
        String date = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
                .format(new Date());
        binding.tvTodayDate.setText(date);
    }

    // ── RecyclerView ─────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new MarketAdapter(new ArrayList<>());
        adapter.setOnItemClickListener(item ->
                Toast.makeText(getContext(), "Analysis for: " + item.getName(),
                        Toast.LENGTH_SHORT).show()
        );
        binding.rvMarketItems.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvMarketItems.setAdapter(adapter);
        // Disable nested scrolling so NestedScrollView handles everything
        binding.rvMarketItems.setNestedScrollingEnabled(false);
    }

    // ── Swipe Refresh ────────────────────────────────────────────────────────

    private void setupSwipeRefresh() {
        if (getContext() != null) {
            binding.swipeRefresh.setColorSchemeResources(
                    com.nighttech.dailybazar.R.color.brand_primary,
                    com.nighttech.dailybazar.R.color.brand_secondary
            );
        }
        binding.swipeRefresh.setOnRefreshListener(() -> {
            // Re-attach the listener which fetches fresh data
            detachListener();
            listenToFirestore();
        });
    }

    // ── Search ───────────────────────────────────────────────────────────────

    private void setupSearchBar() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence query, int start, int before, int count) {
                filterItems(query.toString().trim());
            }
        });
    }

    /**
     * Filters allItems by name or category and pushes the filtered
     * list to the adapter. No Firestore call needed — instant, local.
     */
    private void filterItems(String query) {
        if (query.isEmpty()) {
            adapter.updateItems(new ArrayList<>(allItems));
            showEmptyState(allItems.isEmpty());
            return;
        }

        String lower = query.toLowerCase(Locale.getDefault());
        List<MarketItem> filtered = new ArrayList<>();
        for (MarketItem item : allItems) {
            if (item.getName().toLowerCase(Locale.getDefault()).contains(lower)
                    || item.getCategory().toLowerCase(Locale.getDefault()).contains(lower)) {
                filtered.add(item);
            }
        }
        adapter.updateItems(filtered);
        showEmptyState(filtered.isEmpty());
    }

    // ── Firestore Real-time Listener ─────────────────────────────────────────

    /**
     * Attaches a Firestore snapshot listener to the "marketItems" collection.
     * Any change pushed from the Firestore dashboard (price update, new item,
     * deletion) is reflected instantly in the RecyclerView without any refresh.
     *
     * Firestore document shape expected:
     * {
     *   name:     "Tomato",
     *   price:    "Rs. 120/kg",
     *   imageUrl: "https://firebasestorage.googleapis.com/...",
     *   isTrendUp: true,
     *   category: "Vegetable"
     * }
     */
    private void listenToFirestore() {
        Query query = db.collection("marketItems")
                .orderBy("name", Query.Direction.ASCENDING);

        listenerRegistration = query.addSnapshotListener((snapshots, error) -> {
            stopShimmer();
            binding.swipeRefresh.setRefreshing(false);

            if (error != null) {
                showError("Failed to load data. Check your connection.");
                showEmptyState(true);
                return;
            }

            if (snapshots == null || snapshots.isEmpty()) {
                showEmptyState(true);
                return;
            }

            allItems.clear();
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                String name      = doc.getString("name");
                String price     = doc.getString("price");
                String imageUrl  = doc.getString("imageUrl");
                Boolean trendUp  = doc.getBoolean("isTrendUp");
                String category  = doc.getString("category");

                if (name != null && price != null) {
                    MarketItem item = new MarketItem(
                            name, price,
                            imageUrl != null ? imageUrl : "",
                            trendUp != null && trendUp,
                            category != null ? category : "General"
                    );
                    allItems.add(item);
                }
            }

            // Apply current search filter (if any) on top of fresh data
            String currentQuery = binding.etSearch.getText().toString().trim();
            filterItems(currentQuery);

            showList();
        });
    }

    private void detachListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    // ── UI State Helpers ─────────────────────────────────────────────────────

    private void startShimmer() {
        binding.shimmerLayout.setVisibility(View.VISIBLE);
        binding.shimmerLayout.startShimmer();
        binding.rvMarketItems.setVisibility(View.GONE);
        binding.tvSectionTitle.setVisibility(View.GONE);
        binding.emptyStateMarket.getRoot().setVisibility(View.GONE);
    }

    private void stopShimmer() {
        binding.shimmerLayout.stopShimmer();
        binding.shimmerLayout.setVisibility(View.GONE);
    }

    private void showList() {
        binding.rvMarketItems.setVisibility(View.VISIBLE);
        binding.tvSectionTitle.setVisibility(View.VISIBLE);
        binding.emptyStateMarket.getRoot().setVisibility(View.GONE);
    }

    private void showEmptyState(boolean show) {
        if (show) {
            binding.rvMarketItems.setVisibility(View.GONE);
            binding.tvSectionTitle.setVisibility(View.GONE);
            binding.emptyStateMarket.getRoot().setVisibility(View.VISIBLE);
        } else {
            binding.rvMarketItems.setVisibility(View.VISIBLE);
            binding.tvSectionTitle.setVisibility(View.VISIBLE);
            binding.emptyStateMarket.getRoot().setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detachListener(); // Critical: prevents memory leaks and ghost callbacks
        binding = null;
    }
}