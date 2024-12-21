package ai.humnod.genai.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;

public class DataManager {
    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_FIRST_LAUNCH = "isFirstLaunch";

    private Context context;

    public DataManager(Context context) {
        this.context = context;
    }

    // Check if this is the first launch
    public boolean isFirstLaunch() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    // Mark the app as launched
    public void setFirstLaunch(boolean isFirstLaunch) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch).apply();
    }

    // Save settings to a JSON file
    public void saveSettings(int maxLength, float lengthPenalty, String agentMode) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("maxLength", maxLength);
            jsonObject.put("lengthPenalty", lengthPenalty);
            jsonObject.put("agentMode", agentMode);

            File file = new File(context.getFilesDir(), SETTINGS_FILE_NAME);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonObject.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load settings from a JSON file
    public JSONObject loadSettings() {
        File file = new File(context.getFilesDir(), SETTINGS_FILE_NAME);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                char[] buffer = new char[(int) file.length()];
                reader.read(buffer);
                return new JSONObject(new String(buffer));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null; // Return null if the file doesn't exist
    }
}
