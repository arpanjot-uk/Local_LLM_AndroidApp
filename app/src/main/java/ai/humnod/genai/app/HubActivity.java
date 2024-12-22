package ai.humnod.genai.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class HubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);

        DataManager dataManager = new DataManager(this);

        // Check for first launch
        if (dataManager.isFirstLaunch()) {
            // Launch the OnboardingActivity
            Intent intent = new Intent(HubActivity.this, OnboardingActivity.class);
            startActivity(intent);
            finish(); // Finish HubActivity to prevent returning to it after onboarding
        } else {
            // Proceed directly to MainActivity
            Intent intent = new Intent(HubActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Ensure HubActivity doesn't remain in the back stack
        }
    }
}
