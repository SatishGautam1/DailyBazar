package com.nighttech.dailybazar.ui.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ActivityMainBinding;
import com.nighttech.dailybazar.ui.fragment.AlertsFragment;
import com.nighttech.dailybazar.ui.fragment.HistoryFragment;
import com.nighttech.dailybazar.ui.fragment.MarketFragment;
import com.nighttech.dailybazar.ui.fragment.ProfileFragment;

/**
 * MainActivity — single-activity host for DailyBazar.
 *
 * Key responsibilities:
 *  • Edge-to-edge window rendering via WindowCompat
 *  • Precise inset distribution (status bar → toolbar, nav bar → bottom nav)
 *  • Fragment switching via FragmentManager (no NavComponent dependency)
 *  • State preservation across configuration changes (rotation)
 *  • Secondary profile entry via the Toolbar avatar
 */
public class MainActivity extends AppCompatActivity {

    // ── ViewBinding ──────────────────────────────────────────────────────────
    private ActivityMainBinding binding;

    // ── State ────────────────────────────────────────────────────────────────
    /** Persisted across rotation so the correct nav item is re-selected. */
    private static final String KEY_SELECTED_NAV = "selected_nav_item";
    private int currentNavId = R.id.nav_market;

    /**
     * Guards against re-loading the fragment that the FragmentManager already
     * restored automatically after a configuration change.
     */
    private boolean isRestoringNavState = false;

    // ════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ① Edge-to-edge — must be called BEFORE super.onCreate so the
        //   decor is configured before the window is attached.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Use MaterialToolbar as the ActionBar (required for proper M3 styling).
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            // Title is already set in XML; suppress the default ActionBar title
            // so the Toolbar's own title takes precedence.
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        applyWindowInsets();
        setupBottomNavigation(savedInstanceState);
        setupProfileAvatar();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_NAV, currentNavId);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Window Insets
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Distributes system-bar insets to the correct UI surfaces:
     *   • Status-bar height  → top padding of the AppBarLayout
     *   • Nav-bar / gesture-bar height → bottom padding of BottomNavigationView
     *
     * The fragment container itself does NOT need top padding because it sits
     * below the AppBarLayout via appbar_scrolling_view_behavior.
     */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Push the AppBar below the status bar.
            binding.appBarLayout.setPadding(
                    0, systemBars.top, 0, 0
            );

            // Push the Bottom Nav above the gesture / navigation bar.
            binding.bottomNavigation.setPadding(
                    binding.bottomNavigation.getPaddingLeft(),
                    binding.bottomNavigation.getPaddingTop(),
                    binding.bottomNavigation.getPaddingRight(),
                    systemBars.bottom
            );

            // Return CONSUMED so child views don't re-process the same insets.
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Bottom Navigation
    // ════════════════════════════════════════════════════════════════════════

    private void setupBottomNavigation(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            // Configuration change: restore the previously selected tab ID.
            // The FragmentManager has already restored the fragment itself —
            // we only need to restore the visual selection in the nav bar
            // WITHOUT triggering the listener (which would replace the fragment).
            currentNavId = savedInstanceState.getInt(KEY_SELECTED_NAV, R.id.nav_market);
            isRestoringNavState = true;
        }

        // Register listener BEFORE calling setSelectedItemId so the pill
        // indicator animates on first launch.
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (isRestoringNavState) {
                // Visual state restored — do NOT replace the FM-managed fragment.
                isRestoringNavState = false;
                return true;
            }
            currentNavId = item.getItemId();
            loadFragment(buildFragmentFor(currentNavId));
            return true;
        });

        if (savedInstanceState == null) {
            // Fresh launch: trigger the default selection via the listener.
            binding.bottomNavigation.setSelectedItemId(R.id.nav_market);
        } else {
            // Rotation: restore visual selection only (listener skips due to flag).
            binding.bottomNavigation.setSelectedItemId(currentNavId);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Profile Avatar (secondary entry point)
    // ════════════════════════════════════════════════════════════════════════

    private void setupProfileAvatar() {
        binding.ivProfileAvatar.setOnClickListener(v -> {
            // Sync the bottom nav indicator with the programmatic navigation.
            if (currentNavId != R.id.nav_profile) {
                currentNavId = R.id.nav_profile;
                binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
                // setSelectedItemId fires the listener which calls loadFragment.
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Fragment Helpers
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Replaces the fragment container contents.
     * Uses replace() so only one fragment is ever active at a time, keeping
     * memory usage low for an informational app.
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)          // Required for predictive back
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /** Maps a nav menu item ID to the corresponding Fragment instance. */
    private Fragment buildFragmentFor(int navId) {
        if (navId == R.id.nav_alerts)  return new AlertsFragment();
        if (navId == R.id.nav_history) return new HistoryFragment();
        if (navId == R.id.nav_profile) return new ProfileFragment();
        return new MarketFragment(); // default / nav_market
    }
}