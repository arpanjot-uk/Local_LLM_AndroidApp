package ai.humnod.genai.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheet extends BottomSheetDialogFragment {
    private EditText maxLengthEditText;
    private EditText lengthPenaltyEditText;
    private SettingsListener settingsListener;
    private Spinner responseModeSpinner;

    private static final String ARG_MAX_LENGTH = "max_length";
    private static final String ARG_LENGTH_PENALTY = "length_penalty";

    public interface SettingsListener {
        void onSettingsApplied(int maxLength, float lengthPenalty, String agentMode);
    }

    public void setSettingsListener(SettingsListener listener) {
        this.settingsListener = listener;
    }

    public static BottomSheet newInstance(int maxLength, float lengthPenalty, String agentMode) {
        BottomSheet bottomSheet = new BottomSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_MAX_LENGTH, maxLength);
        args.putFloat(ARG_LENGTH_PENALTY, lengthPenalty);
        args.putString("agentMode", agentMode);
        bottomSheet.setArguments(args);
        return bottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet, container, false);

        maxLengthEditText = view.findViewById(R.id.idEdtMaxLength);
        lengthPenaltyEditText = view.findViewById(R.id.idEdtLengthPenalty);
        ImageButton applyButton = view.findViewById(R.id.applySettingsButton);
        responseModeSpinner = view.findViewById(R.id.idSpinnerResponseMode);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.response_modes, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        responseModeSpinner.setAdapter(adapter);

        // Get arguments and pre-fill the EditTexts and Spinner
        if (getArguments() != null) {
            int maxLength = getArguments().getInt(ARG_MAX_LENGTH, 100); // Default is 100 if no argument
            float lengthPenalty = getArguments().getFloat(ARG_LENGTH_PENALTY, 1.0f); // Default is 1.0f if no argument
            String agentMode = getArguments().getString("agentMode", "Fast Reasoning"); // Default is "Fast Reasoning"

            maxLengthEditText.setText(String.valueOf(maxLength));
            lengthPenaltyEditText.setText(String.valueOf(lengthPenalty));

            // Set the Spinner selection based on agentMode value
            int spinnerPosition = adapter.getPosition(agentMode);
            if (spinnerPosition >= 0) {
                responseModeSpinner.setSelection(spinnerPosition);
            }
        }

        // Handle apply button click
        applyButton.setOnClickListener(v -> {
            String maxLengthStr = maxLengthEditText.getText().toString();
            String lengthPenaltyStr = lengthPenaltyEditText.getText().toString();

            if (maxLengthStr.isEmpty() || lengthPenaltyStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int maxLength = Integer.parseInt(maxLengthStr);
            float lengthPenalty = Float.parseFloat(lengthPenaltyStr);

            // Retrieve the selected item from the Spinner
            String agentMode = responseModeSpinner.getSelectedItem().toString();

            // Validation logic
            if (maxLength > 5000) {
                Toast.makeText(getContext(), "Max response length cannot exceed 5000", Toast.LENGTH_SHORT).show();
                return;
            }

            if (maxLength < 300) {
                Toast.makeText(getContext(), "Max response length cannot be less than 300", Toast.LENGTH_SHORT).show();
                return;
            }

            if (lengthPenalty > 5.0f) {
                Toast.makeText(getContext(), "Length penalty cannot exceed 5.0", Toast.LENGTH_SHORT).show();
                return;
            }

            // If validation passes, apply the settings
            if (settingsListener != null) {
                settingsListener.onSettingsApplied(maxLength, lengthPenalty, agentMode);
                dismiss();
            }
        });

        return view;
    }

}
