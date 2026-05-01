package com.nighttech.dailybazar.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nighttech.dailybazar.LivePriceItem;
import com.nighttech.dailybazar.adapter.LivePriceAdapter;
import com.nighttech.dailybazar.databinding.FragmentAlertsBinding;

import java.util.ArrayList;
import java.util.List;

public class AlertsFragment extends Fragment {

    private static final String RTDB_ROOT        = "livePrices";
    private static final String NODE_GOLD        = "Gold";
    private static final String NODE_VEGETABLES  = "Vegetables";

    private FragmentAlertsBinding binding;
    private LivePriceAdapter goldAdapter;
    private LivePriceAdapter vegAdapter;

    private DatabaseReference goldRef;
    private DatabaseReference vegRef;
    private ValueEventListener goldListener;
    private ValueEventListener vegListener;

    // Track whether both sections have received their first data payload.
    // Shimmer is hidden only after BOTH callbacks have fired at least once.
    private boolean goldLoaded = false;
    private boolean vegLoaded  = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Start shimmer immediately — hides stale/empty RecyclerViews while loading
        binding.shimmerLive.setVisibility(View.VISIBLE);
        binding.shimmerLive.startShimmer();

        // Hide content until data arrives
        binding.rvGoldPrices.setVisibility(View.GONE);
        binding.rvVegPrices.setVisibility(View.GONE);
        binding.cardGoldHeader.setVisibility(View.GONE);
        binding.cardVegHeader.setVisibility(View.GONE);
        binding.emptyStateLive.getRoot().setVisibility(View.GONE);

        setupRecyclerViews();
        attachRtdbListeners();
    }

    // ── RecyclerViews ────────────────────────────────────────────────────────

    private void setupRecyclerViews() {
        goldAdapter = new LivePriceAdapter();
        goldAdapter.setOnItemClickListener(item ->
                Toast.makeText(getContext(),
                        item.getName() + ": " + item.getPrice(),
                        Toast.LENGTH_SHORT).show());

        vegAdapter = new LivePriceAdapter();
        vegAdapter.setOnItemClickListener(item ->
                Toast.makeText(getContext(),
                        item.getName() + ": " + item.getPrice(),
                        Toast.LENGTH_SHORT).show());

        binding.rvGoldPrices.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvGoldPrices.setAdapter(goldAdapter);
        binding.rvGoldPrices.setNestedScrollingEnabled(false);

        binding.rvVegPrices.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvVegPrices.setAdapter(vegAdapter);
        binding.rvVegPrices.setNestedScrollingEnabled(false);
    }

    // ── Firebase RTDB Listeners ──────────────────────────────────────────────

    private void attachRtdbListeners() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();

        goldRef = db.getReference(RTDB_ROOT).child(NODE_GOLD);
        vegRef  = db.getReference(RTDB_ROOT).child(NODE_VEGETABLES);

        // Each listener is built separately so it captures the correct adapter
        goldListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;
                List<LivePriceItem> items = parseItems(snapshot);
                goldAdapter.updateItems(items);
                goldLoaded = true;
                onSectionLoaded(items.isEmpty(), true);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Gold prices error: " + error.getMessage());
            }
        };

        vegListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;
                List<LivePriceItem> items = parseItems(snapshot);
                vegAdapter.updateItems(items);
                vegLoaded = true;
                onSectionLoaded(items.isEmpty(), false);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Vegetable prices error: " + error.getMessage());
            }
        };

        goldRef.addValueEventListener(goldListener);
        vegRef.addValueEventListener(vegListener);
    }

    /**
     * Deserializes all children of a DataSnapshot into LivePriceItem objects,
     * filtering out any entries missing name or price.
     */
    private List<LivePriceItem> parseItems(DataSnapshot snapshot) {
        List<LivePriceItem> items = new ArrayList<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            LivePriceItem item = child.getValue(LivePriceItem.class);
            if (item != null
                    && item.getName() != null && !item.getName().isEmpty()
                    && item.getPrice() != null && !item.getPrice().isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    /**
     * Called after each section loads. Once BOTH gold and veg have responded,
     * stops the shimmer and shows real content (or empty state).
     *
     * @param isEmpty   whether this section returned zero items
     * @param isGold    true = gold section, false = veg section
     */
    private void onSectionLoaded(boolean isEmpty, boolean isGold) {
        if (binding == null) return;

        // Show the section that just loaded regardless of whether the other is ready
        if (isGold) {
            binding.cardGoldHeader.setVisibility(View.VISIBLE);
            binding.rvGoldPrices.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        } else {
            binding.cardVegHeader.setVisibility(View.VISIBLE);
            binding.rvVegPrices.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }

        // Only kill the shimmer once both sections have answered
        if (goldLoaded && vegLoaded) {
            binding.shimmerLive.stopShimmer();
            binding.shimmerLive.setVisibility(View.GONE);

            // Show empty state only if BOTH sections are empty
            boolean bothEmpty = goldAdapter.getItemCount() == 0
                    && vegAdapter.getItemCount() == 0;
            binding.emptyStateLive.getRoot()
                    .setVisibility(bothEmpty ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        if (!isAdded() || getContext() == null) return;
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        // Stop shimmer on error so the user isn't left staring at skeletons
        if (binding != null) {
            binding.shimmerLive.stopShimmer();
            binding.shimmerLive.setVisibility(View.GONE);
            binding.emptyStateLive.getRoot().setVisibility(View.VISIBLE);
        }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (goldRef != null && goldListener != null) goldRef.removeEventListener(goldListener);
        if (vegRef  != null && vegListener  != null) vegRef.removeEventListener(vegListener);
        binding = null;
    }
}