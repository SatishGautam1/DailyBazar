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

/**
 * LivePricesFragment — shows real-time Gold and Vegetable prices from
 * Firebase Realtime Database.
 *
 * RTDB structure expected:
 *   /livePrices
 *     /Gold
 *       /{itemId}
 *         name:      "Gold (24k)"
 *         price:     "Rs. 1,45,000/tola"
 *         imageUrl:  "https://res.cloudinary.com/..."   (optional)
 *         isTrendUp: true
 *         category:  "Gold"
 *     /Vegetables
 *       /{itemId}
 *         name:      "Tomato"
 *         price:     "Rs. 80/kg"
 *         isTrendUp: false
 *         category:  "Vegetable"
 *
 * Uses the existing FragmentAlertsBinding layout — swap for a dedicated
 * fragment_live_prices.xml if you want a different design.
 *
 * Gradle dependency required:
 *   implementation 'com.google.firebase:firebase-database'
 */
public class LivePricesFragment extends Fragment {

    // ── RTDB path constants ───────────────────────────────────────────────────
    private static final String RTDB_ROOT      = "livePrices";
    private static final String NODE_GOLD      = "Gold";
    private static final String NODE_VEGETABLES = "Vegetables";

    private FragmentAlertsBinding binding;
    private LivePriceAdapter goldAdapter;
    private LivePriceAdapter vegAdapter;

    // Keep references so we can detach in onDestroyView — critical to prevent
    // ghost callbacks after the fragment is destroyed.
    private DatabaseReference goldRef;
    private DatabaseReference vegRef;
    private ValueEventListener goldListener;
    private ValueEventListener vegListener;

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

        setupRecyclerViews();
        attachRtdbListeners();
    }

    // ── RecyclerViews ────────────────────────────────────────────────────────

    private void setupRecyclerViews() {
        goldAdapter = new LivePriceAdapter();
        goldAdapter.setOnItemClickListener(item ->
                Toast.makeText(getContext(), item.getName() + ": " + item.getPrice(),
                        Toast.LENGTH_SHORT).show());

        vegAdapter = new LivePriceAdapter();
        vegAdapter.setOnItemClickListener(item ->
                Toast.makeText(getContext(), item.getName() + ": " + item.getPrice(),
                        Toast.LENGTH_SHORT).show());

        // Reusing the two RecyclerViews declared in fragment_alerts.xml.
        // Adjust IDs to match your actual layout.
        if (binding.rvGoldPrices != null) {
            binding.rvGoldPrices.setLayoutManager(new GridLayoutManager(getContext(), 2));
            binding.rvGoldPrices.setAdapter(goldAdapter);
            binding.rvGoldPrices.setNestedScrollingEnabled(false);
        }
        if (binding.rvVegPrices != null) {
            binding.rvVegPrices.setLayoutManager(new GridLayoutManager(getContext(), 2));
            binding.rvVegPrices.setAdapter(vegAdapter);
            binding.rvVegPrices.setNestedScrollingEnabled(false);
        }
    }

    // ── Firebase RTDB listeners ──────────────────────────────────────────────

    private void attachRtdbListeners() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();

        goldRef = db.getReference(RTDB_ROOT).child(NODE_GOLD);
        vegRef  = db.getReference(RTDB_ROOT).child(NODE_VEGETABLES);

        goldListener = buildListener(goldAdapter, "Gold prices");
        vegListener  = buildListener(vegAdapter,  "Vegetable prices");

        goldRef.addValueEventListener(goldListener);
        vegRef.addValueEventListener(vegListener);
    }

    /**
     * Builds a ValueEventListener that deserializes each child node as a
     * LivePriceItem and pushes the full list to the given adapter.
     *
     * ValueEventListener fires:
     *   • Immediately with cached data (offline-first)
     *   • Every time any child node under the reference changes in RTDB
     */
    private ValueEventListener buildListener(LivePriceAdapter adapter, String label) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;

                List<LivePriceItem> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    LivePriceItem item = child.getValue(LivePriceItem.class);
                    if (item != null && !item.getName().isEmpty() && !item.getPrice().isEmpty()) {
                        items.add(item);
                    }
                }
                adapter.updateItems(items);

                // Show/hide empty state if your layout has one
                boolean isEmpty = items.isEmpty();
                if (binding.emptyStateLive != null) {
                    binding.emptyStateLive.getRoot()
                            .setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(),
                        label + " error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };
    }

    private void detachListeners() {
        if (goldRef != null && goldListener != null) {
            goldRef.removeEventListener(goldListener);
        }
        if (vegRef != null && vegListener != null) {
            vegRef.removeEventListener(vegListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detachListeners(); // Prevents memory leaks and ghost callbacks
        binding = null;
    }
}