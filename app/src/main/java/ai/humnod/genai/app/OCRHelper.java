package ai.humnod.genai.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class OCRHelper {

    private final Context context;

    public OCRHelper(Context context) {
        this.context = context;
    }

    public void recognizeTextFromImage(Bitmap bitmap, OCRCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    StringBuilder sb = new StringBuilder();
                    for (Text.TextBlock block : text.getTextBlocks()) {
                        sb.append(block.getText()).append("\n");
                    }
                    callback.onSuccess(sb.toString());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to recognize text: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    callback.onFailure(e);
                });
    }

    public interface OCRCallback {
        void onSuccess(String text);
        void onFailure(Exception e);
    }
}

