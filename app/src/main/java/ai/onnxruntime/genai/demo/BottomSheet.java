package ai.onnxruntime.genai.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheet extends BottomSheetDialogFragment {
    private EditText maxLengthEditText;
    private EditText lengthPenaltyEditText;
    private SettingsListener settingsListener;

    private static final String ARG_MAX_LENGTH = "max_length";
    private static final String ARG_LENGTH_PENALTY = "length_penalty";

    public interface SettingsListener {
        void onSettingsApplied(int maxLength, float lengthPenalty);
    }

    public void setSettingsListener(SettingsListener listener) {
        this.settingsListener = listener;
    }

    public static BottomSheet newInstance(int maxLength, float lengthPenalty) {
        BottomSheet bottomSheet = new BottomSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_MAX_LENGTH, maxLength);
        args.putFloat(ARG_LENGTH_PENALTY, lengthPenalty);
        bottomSheet.setArguments(args);
        return bottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet, container, false);

        maxLengthEditText = view.findViewById(R.id.idEdtMaxLength);
        lengthPenaltyEditText = view.findViewById(R.id.idEdtLengthPenalty);
        Button applyButton = view.findViewById(R.id.applySettingsButton);

        // Get arguments and pre-fill the EditTexts
        if (getArguments() != null) {
            int maxLength = getArguments().getInt(ARG_MAX_LENGTH, 100); // Default is 100 if no argument
            float lengthPenalty = getArguments().getFloat(ARG_LENGTH_PENALTY, 1.0f); // Default is 1.0f if no argument

            maxLengthEditText.setText(String.valueOf(maxLength));
            lengthPenaltyEditText.setText(String.valueOf(lengthPenalty));
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

            // Validation logic
            if (maxLength > 5000) {
                Toast.makeText(getContext(), "Max response length cannot exceed 5000", Toast.LENGTH_SHORT).show();
                return;
            }

            if (maxLength < 50) {
                Toast.makeText(getContext(), "Max response length cannot be less than 50", Toast.LENGTH_SHORT).show();
                return;
            }

            if (lengthPenalty > 5.0f) {
                Toast.makeText(getContext(), "Length penalty cannot exceed 5.0", Toast.LENGTH_SHORT).show();
                return;
            }

            // If validation passes, apply the settings
            if (settingsListener != null) {
                settingsListener.onSettingsApplied(maxLength, lengthPenalty);
                dismiss();
            }
        });

        return view;
    }
}
