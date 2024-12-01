package ai.humnod.genai.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import ai.onnxruntime.genai.GenAIException;
import ai.onnxruntime.genai.Generator;
import ai.onnxruntime.genai.GeneratorParams;
import ai.onnxruntime.genai.Sequences;
import ai.onnxruntime.genai.TokenizerStream;
import ai.humnod.genai.app.databinding.ActivityMainBinding;
import ai.onnxruntime.genai.Model;
import ai.onnxruntime.genai.Tokenizer;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.core.MarkwonTheme;

public class MainActivity extends AppCompatActivity implements Consumer<String> {

    private ActivityMainBinding binding;
    private Model model;
    private Tokenizer tokenizer;
    private boolean isGenerating;
    private static final String TAG = "genai.app.MainActivity";


    private Markwon markwon;


    private EditText userMsgEdt;
    private ImageButton sendMsgIB;
    private ImageButton attachFileIB;
    private TextView generatedTV;
    private TextView promptTV;
    private TextView progressText;
    private ProgressBar progressBar;
    private ImageButton settingsButton;
    private ImageButton adsButton;
    private ScrollView chatScrollView;
    private ImageButton scrollToBottomButton;


    private int maxLength = 2000;
    private float lengthPenalty = 1.0f;
    private String agentMode = "Assistant";


    private boolean isCharLimitOn;


    private static final int PERMISSION_REQUEST_CODE = 100;
    private boolean hasAllPermissions = false;


    private String attachmentContent;
    private static final String[] SUPPORTED_MIME_TYPES = {
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/*"
    };
    private static final int PICK_DOCUMENT_REQUEST_CODE = 102;
    private OCRHelper ocrHelper;







    /***
     *  Handling the users selected file
     * @param uri
     */
    private void handleSelectedFile(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        if (mimeType != null) {

            if (mimeType.startsWith("image/")) {
                attachmentContent += "\nUser has uploaded an image and the extracted information is provided below. Craft a response based on the data and user query without mentioning OCR:\n";
                // Handle image file
                processImage(uri);

            } else if (mimeType.equals("application/pdf")) {
                attachmentContent += "\nUser has uploaded a PDF. Extracted content is provided below. Craft a response based on the data and user query:\n";
                //processPdf(uri);

            } else if (mimeType.equals("text/plain")) {
                attachmentContent += "\nUser has uploaded a text file. Extracted content is provided below. Craft a response based on the data and user query:\n";
                processTextFile(uri);

            } else if (mimeType.equals("application/msword") ||
                    mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                attachmentContent += "\nUser has uploaded a Word document. Extracted content is provided below. Craft a response based on the data and user query:\n";
                //processWordFile(uri);

            } else {
                attachmentContent += "\nRespond with the following error message and nothing else: The user has uploaded an unsupported file, and no file content has been provided to HumNod Lite";
                Toast.makeText(this, "Unsupported file type.", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, SUPPORTED_MIME_TYPES);
        startActivityForResult(intent, PICK_DOCUMENT_REQUEST_CODE);
    }

    private void processImage(Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }


            ocrHelper.recognizeTextFromImage(bitmap, new OCRHelper.OCRCallback() {
                @Override
                public void onSuccess(String text) {
                    // Split the extracted text into lines
                    String[] lines = text.split("\\r?\\n");

                    // Filter out empty lines, trim spaces, and convert to lowercase
                    List<String> filteredLines = new ArrayList<>();
                    for (String line : lines) {
                        line = line.trim().toLowerCase(); // Convert to lowercase
                        if (!line.isEmpty()) {
                            filteredLines.add(line);
                        }
                    }

                    // Join the filtered lines with commas
                    String commaSeparatedText = String.join(", ", filteredLines);

                    // Check if the result is not empty before saving
                    if (!commaSeparatedText.isEmpty()) {
                        // Changing button icon to attached
                        attachFileIB.setImageResource(R.drawable.attached);

                        // Append the result to attachmentContent
                        attachmentContent += DocumentProcessor.processAndSummarizeContent(commaSeparatedText, 50);
                    } else {
                        attachmentContent += "Respond with the following error message and nothing else: Please upload a clear image containing text, as no file content has been provided to HumNod Lite";
                        Toast.makeText(MainActivity.this, "Please upload a clear image that includes text", Toast.LENGTH_SHORT).show();
                    }

                    // Recycle the bitmap after processing
                    bitmap.recycle();
                }

                @Override
                public void onFailure(Exception e) {
                    attachmentContent += "Respond with the following error message and nothing else: OCR processing failed, as no file content has been provided to HumNod Lite";
                    Toast.makeText(MainActivity.this, "OCR failed", Toast.LENGTH_SHORT).show();

                    // Recycle the bitmap after processing
                    bitmap.recycle();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            attachmentContent += "Respond with the following error message and nothing else: Failed to load image, as no file content has been provided to HumNod Lite";
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }


    private void processTextFile(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            // File content as a single string
            String fileContent = stringBuilder.toString();

            // Process and summarize the content using DocumentProcessor
            String summaryDocument = DocumentProcessor.processAndSummarizeContent(fileContent, 50);

            // Handle the result
            if (summaryDocument.equals("Not ASCII")) {
                attachmentContent += "Respond with the following error message and nothing else: The uploaded file contains invalid or non-ASCII content, as no file content has been provided to HumNod Lite";
                Toast.makeText(this, "The file contains invalid or non-ASCII content", Toast.LENGTH_SHORT).show();
            } else if (summaryDocument.equals("Limit Hit")) {
                attachmentContent += "Respond with the following error message and nothing else: The file is too large. Max word length is 600, as no file content has been provided to HumNod Lite";
                Toast.makeText(this, "The file is too large. Max word length is 600", Toast.LENGTH_SHORT).show();
            } else {
                attachFileIB.setImageResource(R.drawable.attached);
                attachmentContent += summaryDocument;
            }

        } catch (IOException e) {
            e.printStackTrace();
            attachmentContent += "Respond with the following error message and nothing else: Failed to read text file, as no file content has been provided to HumNod Lite";
            Toast.makeText(this, "Failed to read text file", Toast.LENGTH_SHORT).show();
        }
    }



    private static boolean fileExists(Context context, String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        return file.exists();
    }







    /***
     * Used for auto scaling the text size based on the device screen size for the code box
     * @param sp
     * @return
     */
    private int getScaledTextSize(float sp) {
        return Math.round(sp * getResources().getDisplayMetrics().scaledDensity);
    }

    // Method to check if the device has at least a certain amount of RAM (in GB)
    private boolean hasMinimumRam(int minRamInGb) {
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        long totalRamInBytes = memoryInfo.totalMem;
        long minRamInBytes = minRamInGb * 1024L * 1024L * 1024L; // Convert GB to Bytes

        return totalRamInBytes >= minRamInBytes;
    }







    /***
     *  Show a warning dialog and exit the app
     */
    private void showRamWarningAndExit() {
        // Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.warning_dialog_layout, null);

        // Create the AlertDialog with the custom view
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        // Find the button and set the click listener to exit the app
        ImageButton exitButton = dialogView.findViewById(R.id.applySettingsButton);
        exitButton.setOnClickListener(v -> {
            finish(); // Close the app
        });

        // Show the dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false); // Prevent closing the dialog without action
        alertDialog.show();
    }







    /***
     *  Check if the app has all the necessary permissions
     */
    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            // For Android versions below 13
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            hasAllPermissions = true;
            // Proceed with your app logic that requires permissions
        }
    }









    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check if the device has the required RAM (e.g., 6 GB)
        if (!hasMinimumRam(5)) {
            showRamWarningAndExit();
            return;
        }

        // Check and request necessary permissions
        checkAndRequestPermissions();

        sendMsgIB = findViewById(R.id.idIBSend);
        attachFileIB = findViewById(R.id.idIBAttach);
        attachFileIB.setEnabled(false);
        userMsgEdt = findViewById(R.id.idEdtMessage);
        generatedTV = findViewById(R.id.sample_text);
        promptTV = findViewById(R.id.user_text);
        progressText = findViewById(R.id.progress_text);
        progressBar = findViewById(R.id.progress_bar);
        chatScrollView = findViewById(R.id.chatScrollView);
        settingsButton = findViewById(R.id.idIBSettings);
        adsButton = findViewById(R.id.idIBAds);
        scrollToBottomButton = findViewById(R.id.scrollToBottomButton);
        isGenerating = false;
        isCharLimitOn = false;
        markwon = Markwon.builder(this)
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
                        builder
                                .codeTextColor(Color.parseColor("#5A5A5A")) // Inline code text color
                                .codeBackgroundColor(Color.parseColor("#F0F0F0")) // Inline code background color
                                .codeBlockTextColor(Color.parseColor("#5A5A5A")) // Code block text color
                                .codeBlockBackgroundColor(Color.parseColor("#F0F0F0")) // Code block background color
                                .codeTypeface(Typeface.MONOSPACE) // Typeface for code
                                .codeBlockTextSize(getScaledTextSize(12)) // Set code block text size (in SP, 12sp in this example)
                                .build();
                    }
                })
                .build();
        attachmentContent = "";
        ocrHelper = new OCRHelper(this);


        // Trigger the download operation when the application is created
        try {
            downloadModels(
                    getApplicationContext());
        } catch (GenAIException e) {
            throw new RuntimeException(e);
        }

        adsButton.setOnClickListener(v -> {
            AdsBottomSheet adsBottomSheet = new AdsBottomSheet();
            adsBottomSheet.show(getSupportFragmentManager(), "AdsBottomSheet");
        });

        settingsButton.setOnClickListener(v -> {
            // Pass the updated maxLength, lengthPenalty, and agentMode from MainActivity to BottomSheet
            BottomSheet bottomSheet = BottomSheet.newInstance(maxLength, lengthPenalty, agentMode);

            bottomSheet.setSettingsListener(new BottomSheet.SettingsListener() {
                @Override
                public void onSettingsApplied(int maxLength, float lengthPenalty, String agentMode) {
                    // Update MainActivity's fields when new values are applied
                    MainActivity.this.maxLength = maxLength;
                    MainActivity.this.lengthPenalty = lengthPenalty;
                    MainActivity.this.agentMode = agentMode; // Update agentMode as well

                    Log.i(TAG, "Setting max response length to: " + maxLength);
                    Log.i(TAG, "Setting length penalty to: " + lengthPenalty);
                    Log.i(TAG, "Setting agent mode to: " + agentMode);
                }
            });

            // Show the BottomSheet
            bottomSheet.show(getSupportFragmentManager(), "BottomSheet");
        });


        // Set up scroll listener for chatScrollView
        chatScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            View view = chatScrollView.getChildAt(chatScrollView.getChildCount() - 1);
            int diff = (view.getBottom() - (chatScrollView.getHeight() + chatScrollView.getScrollY()));

            // If diff > 500, it means that the ScrollView is not at the bottom
            if (diff > 500) {
                scrollToBottomButton.setVisibility(View.VISIBLE);
            } else {
                scrollToBottomButton.setVisibility(View.GONE);
            }
        });

        // Set up button click listener to scroll to bottom
        scrollToBottomButton.setOnClickListener(v -> {
            chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
        });

        //Checking if the search filed is empty or not.
        userMsgEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // No need to implement
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Enable or disable send button based on text presence, only when not generating
                // Enable or disable the send button based on text length
                if (!isGenerating) {
                    if (charSequence.toString().trim().length() < 12) {
                        sendMsgIB.setEnabled(false);
                        sendMsgIB.setAlpha(0.5f);

                        if (!isCharLimitOn) {
                            isCharLimitOn = true;
                            Toast.makeText(MainActivity.this, "Please enter at least 12 characters.", Toast.LENGTH_SHORT).show();
                        }

                        attachFileIB.setEnabled(false); // Disable input field to prevent editing
                        attachFileIB.setAlpha(0.5f);  // Make it visually lighter to show it is disabled
                    } else {
                        sendMsgIB.setEnabled(true);
                        sendMsgIB.setAlpha(1.0f);

                        if(hasAllPermissions){
                            attachFileIB.setEnabled(true); // Disable input field to prevent editing
                            attachFileIB.setAlpha(1.0f);  // Make it visually lighter to show it is disabled
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No need to implement
            }
        });


        // Set up attach button
        attachFileIB.setOnClickListener(v -> {
            // Check if the app has right agent mode
            if(agentMode.equals("Academic")){
                Toast.makeText(this, "File attachment only available with Assistant mode", Toast.LENGTH_SHORT).show();
                return;
            }
            // Check if the app has all the necessary permissions
            if (hasAllPermissions) {
                attachmentContent = "";
                openDocumentPicker();
            } else {
                Toast.makeText(this, "Permissions are required to access files.", Toast.LENGTH_SHORT).show();
                checkAndRequestPermissions();
            }
        });


        Consumer<String> tokenListener = this;

        //enable scrolling and resizing of text boxes
        generatedTV.setMovementMethod(new ScrollingMovementMethod());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // adding on click listener for send message button.
        sendMsgIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isGenerating) {
                    isGenerating = false;
                    return;
                }


                if (tokenizer == null) {
                    // if user tries to submit prompt while model is still downloading, display a toast message.
                    Toast.makeText(MainActivity.this, "Model not loaded yet, please wait...", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Checking if the message entered
                // by user is empty or not.
                if (userMsgEdt.getText().toString().isEmpty()) {
                    // if the edit text is empty display a toast message.
                    Toast.makeText(MainActivity.this, "Please enter your message...", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ** Clear accumulated text before starting a new query **
                accumulatedText.setLength(0);

                // Reset the TextView to clear the generated response
                generatedTV.setText("");


                String promptQuestionTag = "User Message is provided bellow: \n";

                String promptQuestion = userMsgEdt.getText().toString();

                String systemPrompt = "";
                if(agentMode.equals("Assistant")){
                    systemPrompt = "You are HumNod Lite, an AI assistant developed by UK-based HumNod LTD, led by CEO Arpanjot Singh and CTO Farhan Memon. Your primary objective is to assist users by addressing their queries with clarity, precision, and a friendly tone. Focus exclusively on the user's query, avoiding unnecessary details. Ensure responses are well-organized, divided into clear paragraphs to enhance readability.";
                }else if(agentMode.equals("Academic")){
                    systemPrompt = "You are HumNod Lite, an AI assistant developed by UK-based HumNod LTD, led by CEO Arpanjot Singh and CTO Farhan Memon. Your primary role is to assist users as a learning-oriented search engine, providing accurate, concise, and informative responses similar to resources like Google, Wikipedia, and educational sites. Your responses should be direct, factual, and easy to understand, especially when dealing with subjects like math, science, and general knowledge. Format the information as nicely as possible using Markdown, ensuring that content is well-structured and easy to read. Use headings, bullet points, code blocks, and other Markdown elements to make the presentation clear and engaging. Aim to provide the user with the most relevant and educational information, while maintaining a friendly and supportive tone.";
                }
                String promptQuestion_formatted = "<|system|>" + systemPrompt + "<|end|>\n<|user|> "+promptQuestionTag+promptQuestion+attachmentContent+"<|end|>\n"+"<|assistant|> ";
                Log.i("GenAI: prompt question", promptQuestion_formatted);
                setVisibility();

                // Disable send button while responding to prompt.
                sendMsgIB.setImageResource(R.drawable.stop_button); // Change icon to indicate "Stop"
                isGenerating = true;

                attachFileIB.setEnabled(false); // Disable input field to prevent editing
                attachFileIB.setAlpha(0.5f);  // Make it visually lighter to show it is disabled

                userMsgEdt.setEnabled(false); // Disable input field to prevent editing
                userMsgEdt.setAlpha(0.5f);  // Make it visually lighter to show it is disabled

                promptTV.setText(promptQuestion);
                // Clear Edit Text or prompt question.
                userMsgEdt.setText("");
                generatedTV.setText("");
                attachmentContent ="";

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TokenizerStream stream = null;
                        GeneratorParams generatorParams = null;
                        Generator generator = null;
                        Sequences encodedPrompt = null;
                        try {
                            stream = tokenizer.createStream();

                            generatorParams = model.createGeneratorParams();
                            //examples for optional parameters to format AI response
                            // https://onnxruntime.ai/docs/genai/reference/config.html
                            generatorParams.setSearchOption("length_penalty", lengthPenalty);
                            generatorParams.setSearchOption("max_length", maxLength);

                            encodedPrompt = tokenizer.encode(promptQuestion_formatted);
                            generatorParams.setInput(encodedPrompt);

                            generator = new Generator(model, generatorParams);

                            // Initialize variables for timing
                            long[] tokenTimes = new long[5];
                            int tokenTimeIndex = 0;
                            boolean hasFiveTokenTimes = false;
                            long startTime = System.currentTimeMillis();
                            long firstTokenTime = startTime;
                            long currentTime = startTime;
                            int numTokens = 0;

                            while (!generator.isDone() && isGenerating) {
                                long tokenStartTime = System.currentTimeMillis();

                                generator.computeLogits();
                                generator.generateNextToken();

                                int token = generator.getLastTokenInSequence(0);

                                String tokenString = stream.decode(token);
                                tokenListener.accept(tokenString);

                                long tokenEndTime = System.currentTimeMillis();
                                long tokenTime = tokenEndTime - tokenStartTime;

                                numTokens++;

                                if (numTokens == 1) {
                                    // Ignore the first token's time
                                    firstTokenTime = tokenEndTime;
                                } else {
                                    // Store the token time, excluding the first token
                                    tokenTimes[tokenTimeIndex] = tokenTime;
                                    tokenTimeIndex++;

                                    if (tokenTimeIndex >= 5) {
                                        tokenTimeIndex = 0;
                                        hasFiveTokenTimes = true;
                                    }

                                    if (hasFiveTokenTimes) {
                                        // Compute the average time over the last 5 tokens
                                        long totalTokenTime = 0;
                                        for (int i = 0; i < 5; i++) {
                                            totalTokenTime += tokenTimes[i];
                                        }
                                        double averageTokenTime = totalTokenTime / 5.0;

                                        Log.i(TAG, "Average time over last 5 tokens (excluding first token): " + (averageTokenTime / 1000.0) + " seconds");

                                        if (averageTokenTime > 1000) { // 1000 milliseconds = 1 seconds
                                            runOnUiThread(() -> {
                                                Toast.makeText(MainActivity.this, "Processing is taking too long due to large input. Please provide a smaller input.", Toast.LENGTH_LONG).show();
                                            });
                                            isGenerating = false;
                                            break;
                                        }
                                    }
                                }

                                Log.i(TAG, "Generated token: " + token + ": " + tokenString);
                                Log.i(TAG, "Time taken to generate token: " + (tokenTime / 1000.0) + " seconds");

                                currentTime = tokenEndTime;
                            }

                        }
                        catch (GenAIException e) {
                            Log.e(TAG, "Exception occurred during model query: " + e.getMessage());
                        }
                        finally {
                            // Clean up resources
                            if (generator != null) {
                                generator.close();
                                generator = null;
                            }
                            if (encodedPrompt != null) {
                                encodedPrompt.close();
                                encodedPrompt = null;
                            }
                            if (stream != null) {
                                stream.close();
                                stream = null;
                            }
                            if (generatorParams != null) {
                                generatorParams.close();
                                generatorParams = null;
                            }
                        }

                        runOnUiThread(() -> {
                            isGenerating = false;

                            // Reset button and state after generation is done or stopped
                            sendMsgIB.setEnabled(false);
                            sendMsgIB.setAlpha(0.5f);
                            sendMsgIB.setImageResource(R.drawable.humnod_send); // Change icon back to "Send"

                            attachFileIB.setEnabled(false); // Disable input field to prevent editing
                            attachFileIB.setAlpha(0.5f);  // Make it visually lighter to show it is disabled
                            attachFileIB.setImageResource(R.drawable.attach); // Change icon back to "attach"

                            // Enable EditText and restore appearance
                            userMsgEdt.setEnabled(true);
                            userMsgEdt.setAlpha(1.0f);  // Make it opaque again to show it is active
                        });
                    }
                }).start();
            }
        });
    }








    @Override
    protected void onDestroy() {
        if (tokenizer != null) {
            tokenizer.close();
            tokenizer = null;
        }
        if (model != null) {
            model.close();
            model = null;
        }
        super.onDestroy();
    }








    private void downloadModels(Context context) throws GenAIException {

        final String baseUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/";
        List<String> files = Arrays.asList(
                "added_tokens.json",
                "config.json",
                "configuration_phi3.py",
                "genai_config.json",
                "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx",
                "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx.data",
                "special_tokens_map.json",
                "tokenizer.json",
                "tokenizer.model",
                "tokenizer_config.json");

        List<Pair<String, String>> urlFilePairs = new ArrayList<>();
        for (String file : files) {
            if (!fileExists(context, file)) {
                urlFilePairs.add(new Pair<>(
                        baseUrl + file,
                        file));
            }
        }
        if (urlFilePairs.isEmpty()) {
            // Display a message using Toast
            Log.d(TAG, "All files already exist. Skipping download.");
            model = new Model(getFilesDir().getPath());
            tokenizer = model.createTokenizer();
            return;
        }

        // Set ProgressBar and ProgressText visibility to visible and reset progress
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            progressText.setVisibility(View.VISIBLE);
            progressText.setText("Downloading HumNod Lite Model...");
        });

        Toast.makeText(this,
                "Downloading model for the app... Model Size greater than 2GB, please allow a few minutes to download.",
                Toast.LENGTH_SHORT).show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            ModelDownloader.downloadModel(context, urlFilePairs, new ModelDownloader.DownloadCallback() {
                @Override
                public void onProgress(long lastBytesRead, long bytesRead, long bytesTotal) {
                    long lastPctDone = 100 * lastBytesRead / bytesTotal;
                    long pctDone = 100 * bytesRead / bytesTotal;
                    if (pctDone > lastPctDone) {
                        Log.d(TAG, "Downloading HumNod Lite: " + pctDone + "%");
                        runOnUiThread(() -> {
                            progressBar.setProgress((int) pctDone);
                            progressText.setText("Downloading HumNod Lite: " + pctDone + "%");
                        });
                    }
                }
                @Override
                public void onDownloadComplete() {
                    Log.d(TAG, "HumNod Lite download completed");

                    // Last download completed, create SimpleGenAI
                    try {
                        model = new Model(getFilesDir().getPath());
                        tokenizer = model.createTokenizer();
                        runOnUiThread(() -> {
                            Toast.makeText(context, "HumNod Lite download completed", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.INVISIBLE); // Hide the progress bar when done
                            progressText.setVisibility(View.INVISIBLE); // Hide the progress text when done
                        });
                    } catch (GenAIException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }

                }
            });
        });
        executor.shutdown();
    }






    // Member variable to accumulate the text as it's generated.
    private final StringBuilder accumulatedText = new StringBuilder();

    @Override
    public void accept(String token) {
        runOnUiThread(() -> {
            try {
                // Check if the token is null or empty and skip if it is
                if (token == null || token.isEmpty()) {
                    Log.w(TAG, "Received null or empty token, skipping update.");
                    return;
                }

                // Append the new token to the accumulated text
                accumulatedText.append(token);

                // Render the Markdown content in the TextView
                markwon.setMarkdown(generatedTV, accumulatedText.toString());

                // Scroll the TextView if necessary (this part ensures that the generatedTV moves correctly)
                generatedTV.post(() -> {
                    if (generatedTV.getLayout() != null) {
                        final int scrollAmount = generatedTV.getLayout().getLineTop(generatedTV.getLineCount()) - generatedTV.getHeight();
                        generatedTV.scrollTo(0, Math.max(scrollAmount, 0));
                    } else {
                        Log.w(TAG, "TextView layout not ready for scrolling");
                    }
                });

                // Scroll the ScrollView to the bottom to keep it in sync with the updated generatedTV
                chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));

            } catch (Exception e) {
                // Log the exception to help debug the issue
                Log.e(TAG, "Error in accept method", e);
                e.printStackTrace();
            }
        });
    }






    public void setVisibility() {
        TextView view = (TextView) findViewById(R.id.user_text);
        view.setVisibility(View.VISIBLE);
        TextView botView = (TextView) findViewById(R.id.sample_text);
        botView.setVisibility(View.VISIBLE);
    }






    // Handle the user's response to the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                allGranted &= (result == PackageManager.PERMISSION_GRANTED);
            }
            if (allGranted) {
                hasAllPermissions = true;
                // Proceed with accessing files
            } else {
                attachFileIB.setEnabled(false); // Disable input field to prevent editing
                attachFileIB.setAlpha(0.5f);  // Make it visually lighter to show it is disabled
                Toast.makeText(this, "Permissions are required to access media files.", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }






    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_DOCUMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                // Handle the selected file
                handleSelectedFile(uri);
            }
        }
    }


}
