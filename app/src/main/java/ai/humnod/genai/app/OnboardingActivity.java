package ai.humnod.genai.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private LinearLayout dotsIndicator;
    private ImageView[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        ViewPager2 viewPager = findViewById(R.id.onboardingViewPager);
        dotsIndicator = findViewById(R.id.dotsIndicator);
        ImageView skipButton = findViewById(R.id.idIBSkip);
        ImageView nextButton = findViewById(R.id.idIBNext);

        // List of onboarding layouts
        List<Integer> layouts = Arrays.asList(
                R.layout.onboarding_screen1,
                R.layout.onboarding_screen2,
                R.layout.onboarding_screen3,
                R.layout.onboarding_screen4,
                R.layout.onboarding_screen5,
                R.layout.onboarding_screen6
        );

        OnboardingAdapter adapter = new OnboardingAdapter(this, layouts);
        viewPager.setAdapter(adapter);

        // Initialize dots indicator
        addDotsIndicator(layouts.size(), 0);

        // Handle page change
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDotsIndicator(position);

                // Check if this is the last screen
                if (position == layouts.size() - 1) { // Last page
                    nextButton.setImageResource(R.drawable.ic_end); // Change to "End" icon
                } else {
                    nextButton.setImageResource(R.drawable.ic_next); // Reset to "Next" icon
                }
            }
        });

        // Skip button: Redirect to MainActivity
        skipButton.setOnClickListener(v -> navigateToMainActivity());

        // Next button: Navigate through screens or finish onboarding
        nextButton.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < layouts.size() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1); // Go to the next screen
            } else {
                navigateToMainActivity(); // If on the last screen, go to MainActivity
            }
        });
    }

    private void navigateToMainActivity() {
        DataManager dataManager = new DataManager(this);
        dataManager.setFirstLaunch(false); // Mark the first launch as completed

        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Ensure OnboardingActivity doesn't remain in the back stack
    }

    // Initialize dots
    private void addDotsIndicator(int count, int currentPosition) {
        dotsIndicator.removeAllViews();
        dots = new ImageView[count];

        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_dot_inactive));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dotsIndicator.addView(dots[i], params);
        }

        if (dots.length > 0) {
            dots[currentPosition].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_dot_active));
        }
    }

    // Update dots
    private void updateDotsIndicator(int position) {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_dot_inactive));
        }
        dots[position].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_dot_active));
    }
}
