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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    // Cached so applyWindowInsets() can reference the real measured height
    // after the BottomNavigationView has been laid out.
    private static final int BOTTOM_NAV_HEIGHT_DP = 80;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Must be called BEFORE setContentView so the window is transparent
        // behind system bars before any view is drawn.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
        setupBottomNavigation();

        // Only set the default tab on a fresh launch, not on config change.
        if (savedInstanceState == null) {
            binding.bottomNavigationView.setSelectedItemId(R.id.nav_market);
        }
    }

    /**
     * INSET STRATEGY
     * ─────────────────────────────────────────────────────────────────
     * ┌─ Status bar ───────────────────────────┐  ← insets.top
     * │  FragmentContainerView (paddingTop)    │
     * │                                        │
     * │  … fragment content …                  │
     * │                                        │
     * │  FragmentContainerView (paddingBottom) │  ← navBarHeight + bottomNavHeight
     * ├─ BottomNavigationView (80dp) ──────────┤
     * │  BottomNav (paddingBottom)             │  ← insets.bottom (gesture bar)
     * └────────────────────────────────────────┘
     *
     * Why this works:
     *   • The BottomNav sits flush at the bottom of the screen. Its own
     *     paddingBottom lifts its content (icons + labels) above the
     *     gesture/navigation bar.
     *   • The FragmentContainer fills the entire window. Its paddingBottom
     *     accounts for BOTH the gesture bar AND the full BottomNav so that
     *     scrollable content is never clipped behind the nav bar.
     *   • Left/right insets handle display cutouts on landscape devices.
     */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() |
                            WindowInsetsCompat.Type.displayCutout()
            );

            float density = getResources().getDisplayMetrics().density;
            int bottomNavHeightPx = Math.round(BOTTOM_NAV_HEIGHT_DP * density);

            // BottomNav: add bottom padding so icons sit above the gesture bar.
            binding.bottomNavigationView.setPadding(
                    0,
                    0,
                    0,
                    insets.bottom
            );

            // FragmentContainer: full edge-to-edge with reserved space at top and bottom.
            binding.fragmentContainer.setPadding(
                    insets.left,
                    insets.top,
                    insets.right,
                    insets.bottom + bottomNavHeightPx   // clears gesture bar + bottom nav
            );

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_market) {
                loadFragment(new MarketFragment());
                return true;
            } else if (id == R.id.nav_alerts) {
                loadFragment(new AlertsFragment());
                return true;
            } else if (id == R.id.nav_history) {
                loadFragment(new HistoryFragment());
                return true;
            }
            return false;
        });
    }

    /**
     * Replaces the fragment with a smooth cross-fade.
     * fade_in is used for both enter and exit so neither fragment
     * "slides" — it just dissolves cleanly.
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.fade_in,   // enter
                        R.anim.fade_out   // exit  ← use fade_out, not fade_in again
                )
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;   // prevent memory leaks after the Activity is gone
    }
}