package com.nighttech.dailybazar.ui.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ActivityMainBinding;
import com.nighttech.dailybazar.ui.bottomsheet.AddMarketItemBottomSheet;
import com.nighttech.dailybazar.ui.fragment.AlertsFragment;
import com.nighttech.dailybazar.ui.fragment.HistoryFragment;
import com.nighttech.dailybazar.ui.fragment.MarketFragment;
import com.nighttech.dailybazar.ui.fragment.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private static final String KEY_SELECTED_NAV = "selected_nav_item";
    private int currentNavId = R.id.nav_market;
    private boolean isRestoringNavState = false;

    // FIX: Keep a single Firestore listener reference so we can detach it in onDestroy
    // Previously two separate listeners (syncProfileData + updateToolbarProfileIcon) were
    // reading the same document, causing duplicate reads and a potential memory leak.
    private ListenerRegistration profileListener;

    // FIX: Cache the user role so we know whether to show the "Add Item" FAB
    private String userRole = "user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        applyWindowInsets();
        setupBottomNavigation(savedInstanceState);
        setupNavigationDrawer();
        setupProfileAvatar();
        startProfileListener();   // Single consolidated listener replaces two
        setupAddItemFab();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_NAV, currentNavId);
    }

    // ── Insets ────────────────────────────────────────────────────────────────

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0);
            binding.bottomNavigation.setPadding(
                    0, 0, 0, systemBars.bottom);
            binding.bottomNavigation.post(() -> {
                int bottomNavHeight = binding.bottomNavigation.getHeight();
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) binding.fragmentContainer.getLayoutParams();
                params.bottomMargin = bottomNavHeight;
                binding.fragmentContainer.setLayoutParams(params);
            });
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // ── Bottom Navigation ────────────────────────────────────────────────────

    private void setupBottomNavigation(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentNavId = savedInstanceState.getInt(KEY_SELECTED_NAV, R.id.nav_market);
            isRestoringNavState = true;
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (isRestoringNavState) {
                isRestoringNavState = false;
                return true;
            }
            currentNavId = item.getItemId();
            loadFragment(buildFragmentFor(currentNavId));
            // Show FAB only on Market tab when user is admin
            updateFabVisibility();
            return true;
        });

        if (savedInstanceState == null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_market);
        } else {
            binding.bottomNavigation.setSelectedItemId(currentNavId);
        }
    }

    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_settings) {
                // TODO: Open Settings Activity
            } else if (id == R.id.nav_about) {
                // TODO: Open About Activity
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    // ── Single consolidated Firestore profile listener ───────────────────────
    //
    // FIX: The original code had TWO addSnapshotListeners on the same document
    // (syncProfileData + updateToolbarProfileIcon), doubling Firestore reads and
    // creating two listener leaks. Merged into one listener that updates both the
    // toolbar avatar and the navigation drawer header simultaneously.
    //
    // FIX: Reads "name" field first and falls back to "fullName" to stay
    // compatible with accounts created before the field rename.

    private void startProfileListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        android.view.View headerView = binding.navigationView.getHeaderView(0);
        com.google.android.material.imageview.ShapeableImageView navAvatar =
                headerView.findViewById(R.id.nav_header_avatar);
        android.widget.TextView navName  = headerView.findViewById(R.id.nav_header_name);
        android.widget.TextView navEmail = headerView.findViewById(R.id.nav_header_email);
        android.view.View navStatusDot   = headerView.findViewById(R.id.nav_header_status_dot);

        profileListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (value == null || !value.exists()) return;

                    // FIX: Read "name" first, fallback to "fullName" for old accounts
                    String name = value.getString("name");
                    if (name == null || name.isEmpty()) name = value.getString("fullName");

                    String email           = value.getString("email");
                    String profileImageUrl = value.getString("profileImageUrl");
                    String status          = value.getString("status");

                    // FIX: Cache the role so the FAB can be shown/hidden correctly
                    String role = value.getString("role");
                    if (role != null) {
                        userRole = role;
                        updateFabVisibility();
                    }

                    // Update drawer header text
                    if (name  != null) navName.setText(name);
                    if (email != null) navEmail.setText(email);

                    // Update both toolbar avatar and drawer avatar from the same listener
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_profile)
                                .circleCrop()
                                .into(binding.ivProfileAvatar);
                        Glide.with(this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_profile)
                                .circleCrop()
                                .into(navAvatar);
                    }

                    // Status dot and avatar ring colour
                    if ("active".equals(status)) {
                        binding.ivProfileAvatar.setStrokeColor(
                                ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                        navStatusDot.setVisibility(View.VISIBLE);
                    } else {
                        binding.ivProfileAvatar.setStrokeColor(
                                ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                        navStatusDot.setVisibility(View.GONE);
                    }
                });
    }

    // ── Profile Avatar ───────────────────────────────────────────────────────

    private void setupProfileAvatar() {
        binding.ivProfileAvatar.setOnClickListener(v -> {
            if (currentNavId != R.id.nav_profile) {
                currentNavId = R.id.nav_profile;
                binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
            }
        });
    }

    // ── Add Market Item FAB ──────────────────────────────────────────────────
    //
    // Visible only when:
    //   • The user's role == "admin"  (checked from Firestore)
    //   • The currently visible tab is Market (nav_market)
    //
    // Tapping opens AddMarketItemBottomSheet which writes to the "marketItems"
    // collection. MarketFragment's existing real-time listener picks up the new
    // document automatically — no manual refresh needed.

    private void setupAddItemFab() {
        binding.fabAddItem.setOnClickListener(v -> {
            AddMarketItemBottomSheet sheet = new AddMarketItemBottomSheet();
            sheet.show(getSupportFragmentManager(), AddMarketItemBottomSheet.TAG);
        });
        updateFabVisibility();
    }

    private void updateFabVisibility() {
        boolean isAdmin   = "admin".equals(userRole);
        boolean isMarket  = (currentNavId == R.id.nav_market);
        binding.fabAddItem.setVisibility((isAdmin && isMarket) ? View.VISIBLE : View.GONE);
    }

    // ── Fragment Loading ─────────────────────────────────────────────────────

    private void loadFragment(Fragment fragment) {
        MaterialFadeThrough fadeThrough = new MaterialFadeThrough();
        fragment.setEnterTransition(fadeThrough);
        fragment.setExitTransition(new MaterialFadeThrough());

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private Fragment buildFragmentFor(int navId) {
        if (navId == R.id.nav_alerts)  return new AlertsFragment();
        if (navId == R.id.nav_history) return new HistoryFragment();
        if (navId == R.id.nav_profile) return new ProfileFragment();
        return new MarketFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // FIX: Detach the single profile listener to prevent memory leaks
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
        binding = null;
    }
}