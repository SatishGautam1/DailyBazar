package com.nighttech.dailybazar.ui.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ActivityMainBinding;
import com.nighttech.dailybazar.ui.fragment.AlertsFragment;
import com.nighttech.dailybazar.ui.fragment.HistoryFragment;
import com.nighttech.dailybazar.ui.fragment.MarketFragment;
import com.nighttech.dailybazar.ui.fragment.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private static final String KEY_SELECTED_NAV = "selected_nav_item";
    private int currentNavId = R.id.nav_market;
    private boolean isRestoringNavState = false;

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
        updateToolbarProfileIcon();
        syncProfileData();
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

            // Status bar → AppBarLayout top padding
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0);

            // Nav bar → BottomNav bottom padding
            binding.bottomNavigation.setPadding(
                    binding.bottomNavigation.getPaddingLeft(),
                    binding.bottomNavigation.getPaddingTop(),
                    binding.bottomNavigation.getPaddingRight(),
                    systemBars.bottom
            );

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
            return true;
        });

        if (savedInstanceState == null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_market);
        } else {
            binding.bottomNavigation.setSelectedItemId(currentNavId);
        }
    }

    private void setupNavigationDrawer() {
        // Link Toolbar menu icon to Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Handle Drawer Item Clicks
        binding.navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_settings) {
                // Open Settings Activity
            } else if (id == R.id.nav_about) {
                // Open About Activity
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void syncProfileData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            // Get references to the Side Drawer Header views
            android.view.View headerView = binding.navigationView.getHeaderView(0);
            com.google.android.material.imageview.ShapeableImageView navAvatar = headerView.findViewById(R.id.nav_header_avatar);
            android.widget.TextView navName = headerView.findViewById(R.id.nav_header_name);
            android.widget.TextView navEmail = headerView.findViewById(R.id.nav_header_email);
            android.view.View navStatusDot = headerView.findViewById(R.id.nav_header_status_dot);

            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .addSnapshotListener((value, error) -> {
                        if (value != null && value.exists()) {
                            String profileImageUrl = value.getString("profileImageUrl");
                            String name = value.getString("name");
                            String email = value.getString("email");
                            String status = value.getString("status");

                            // 1. Update text in Drawer
                            if (name != null) navName.setText(name);
                            if (email != null) navEmail.setText(email);

                            // 2. Load Image into BOTH avatars
                            if (profileImageUrl != null) {
                                Glide.with(this).load(profileImageUrl).placeholder(R.drawable.ic_profile).circleCrop().into(binding.ivProfileAvatar);
                                Glide.with(this).load(profileImageUrl).placeholder(R.drawable.ic_profile).circleCrop().into(navAvatar);
                            }

                            // 3. Handle Active Status
                            if ("active".equals(status)) {
                                binding.ivProfileAvatar.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                                navStatusDot.setVisibility(android.view.View.VISIBLE);
                            } else {
                                binding.ivProfileAvatar.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#9E9E9E"))); // Grey if offline
                                navStatusDot.setVisibility(android.view.View.GONE);
                            }
                        }
                    });
        }
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

    private void updateToolbarProfileIcon() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .addSnapshotListener((value, error) -> {
                        if (value != null && value.exists()) {
                            String profileImageUrl = value.getString("profileImageUrl");

                            // Use Glide to load the image into the toolbar icon
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_profile) // Fallback
                                    .circleCrop()
                                    .into(binding.ivProfileAvatar);

                            // Optional: Change stroke color based on a 'status' field in Firebase
                            String status = value.getString("status");
                            if ("active".equals(status)) {
                                binding.ivProfileAvatar.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                            }
                        }
                    });
        }
    }

    // ── Fragment Loading with MaterialFadeThrough transition ─────────────────

    /**
     * MaterialFadeThrough: the outgoing fragment fades + scales down,
     * the incoming fragment fades + scales up. This is the M3-standard
     * transition for unrelated destinations (e.g. Market → Profile).
     */
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
        binding = null;
    }
}