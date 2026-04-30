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
import com.nighttech.dailybazar.ui.fragment.MarketFragment;
import com.nighttech.dailybazar.ui.fragment.AlertsFragment;
import com.nighttech.dailybazar.ui.fragment.HistoryFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
        setupBottomNavigation();

        if (savedInstanceState == null) {
            loadFragment(new MarketFragment());
            binding.bottomNavigationView.setSelectedItemId(R.id.nav_market);
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() |
                            WindowInsetsCompat.Type.displayCutout()
            );

            // BottomNav: pad bottom by system nav bar height so it's above gesture bar
            binding.bottomNavigationView.setPadding(
                    0, 0, 0, insets.bottom
            );

            // Fragment container: pad top for status bar, bottom for nav bar
            binding.fragmentContainer.setPadding(
                    insets.left,
                    insets.top,
                    insets.right,
                    insets.bottom
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

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_in)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}