package ai.onnxruntime.genai.demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
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
import ai.onnxruntime.genai.demo.databinding.ActivityMainBinding;
import ai.onnxruntime.genai.Model;
import ai.onnxruntime.genai.Tokenizer;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.core.MarkwonTheme;

public class MainActivity extends AppCompatActivity implements Consumer<String> {

    private ActivityMainBinding binding;
    private EditText userMsgEdt;
    private Model model;
    private Tokenizer tokenizer;

    private Markwon markwon;

    private ImageButton sendMsgIB;
    private TextView generatedTV;
    private TextView promptTV;
    private TextView progressText;
    private ProgressBar progressBar;
    private ImageButton settingsButton;
    private ImageButton adsButton;
    private ScrollView chatScrollView;
    private ImageButton scrollToBottomButton;

    private boolean isGenerating;

    private static final String TAG = "genai.demo.MainActivity";
    private int maxLength = 1000;
    private float lengthPenalty = 1.0f;
    private String agentMode = "Fast Reasoning";

    private static boolean fileExists(Context context, String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        return file.exists();
    }

    private int getScaledTextSize(float sp) {
        return Math.round(sp * getResources().getDisplayMetrics().scaledDensity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sendMsgIB = findViewById(R.id.idIBSend);
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
                if (!isGenerating) {
                    if (charSequence.toString().trim().isEmpty()) {
                        sendMsgIB.setEnabled(false);
                        sendMsgIB.setAlpha(0.5f);
                    } else {
                        sendMsgIB.setEnabled(true);
                        sendMsgIB.setAlpha(1.0f);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No need to implement
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
                    // If generation is in progress, set the flag to stop it
                    isGenerating = false;

                    // Reset button to original state after stopping
                    runOnUiThread(() -> {
                        sendMsgIB.setEnabled(true);
                        sendMsgIB.setAlpha(1.0f);
                        sendMsgIB.setImageResource(R.drawable.humnod_send); // Change icon back to "Send"

                        // Enable EditText and restore appearance
                        userMsgEdt.setEnabled(true);
                        userMsgEdt.setAlpha(1.0f);  // Make it opaque again to show it is active
                    });

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
                    Toast.makeText(MainActivity.this, "Please enter your message..", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ** Clear accumulated text before starting a new query **
                accumulatedText.setLength(0);

                // Reset the TextView to clear the generated response
                generatedTV.setText("");


                String promptQuestion = userMsgEdt.getText().toString();

                String systemPrompt = "";
                if(agentMode.equals("Fast Reasoning")){
                 systemPrompt = "You are HumNod Lite, a helpful AI assistant developed by the HumNod LTD team in the UK. The HumNod team is led by CEO Arpanjot Singh and CTO Farhan Memon. Answer in two paragraphs or less";
                }else if(agentMode.equals("Intense Reasoning")){
                    systemPrompt = "You are HumNod Lite, a helpful AI assistant developed by the HumNod LTD team in the UK. The HumNod team is led by CEO Arpanjot Singh and CTO Farhan Memon. Your primary role is to assist users as a learning-oriented search engine, providing accurate, concise, and informative responses similar to resources like Google, Wikipedia, and educational sites. Your responses should be direct, factual, and easy to understand, especially when dealing with subjects like math, science, and general knowledge. Format the information as nicely as possible using Markdown, ensuring that content is well-structured and easy to read. Use headings, bullet points, code blocks, and other Markdown elements to make the presentation clear and engaging. For visualizations, ensure they are small enough to fit comfortably on an average smartphone screen size of 6 inches, making them easy to view and interact with on mobile devices. Aim to provide the user with the most relevant and educational information, while maintaining a friendly and supportive tone.";
                }
                String promptQuestion_formatted = "<system> "+systemPrompt+" <|end|> <|user|> "+promptQuestion+" <|end|>\n<assistant|>";
                Log.i("GenAI: prompt question", promptQuestion_formatted);
                setVisibility();

                // Disable send button while responding to prompt.
                sendMsgIB.setImageResource(R.drawable.stop_button); // Change icon to indicate "Stop"
                isGenerating = true;

                userMsgEdt.setEnabled(false); // Disable input field to prevent editing
                userMsgEdt.setAlpha(0.5f);  // Make it visually lighter to show it is disabled

                promptTV.setText(promptQuestion);
                // Clear Edit Text or prompt question.
                userMsgEdt.setText("");
                generatedTV.setText("");

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

                            // try to measure average time taken to generate each token.
                            long startTime = System.currentTimeMillis();
                            long firstTokenTime = startTime;
                            long currentTime = startTime;
                            int numTokens = 0;
                            while (!generator.isDone() && isGenerating) {
                                generator.computeLogits();
                                generator.generateNextToken();
                 
                                int token = generator.getLastTokenInSequence(0);

                                if (numTokens == 0) { //first token
                                    firstTokenTime = System.currentTimeMillis();
                                }

                                tokenListener.accept(stream.decode(token));


                                Log.i(TAG, "Generated token: " + token + ": " +  stream.decode(token));
                                Log.i(TAG, "Time taken to generate token: " + (System.currentTimeMillis() - currentTime)/ 1000.0 + " seconds");
                                currentTime = System.currentTimeMillis();
                                numTokens++;
                            }
                            long totalTime = System.currentTimeMillis() - firstTokenTime;

                            float promptProcessingTime = (firstTokenTime - startTime)/ 1000.0f;
                            float tokensPerSecond = (1000 * (numTokens -1)) / totalTime;

                        //THIS WAS REMOVED AS WAS USED TO READABLE THE BUTTON AFTER GENERATION
                           // runOnUiThread(() -> {
                                //sendMsgIB.setEnabled(true);
                                //sendMsgIB.setAlpha(1.0f);

                                // Display the token generation rate in a dialog popup
                                //showTokenPopup(promptProcessingTime, tokensPerSecond);
                           // });

                            Log.i(TAG, "Prompt processing time (first token): " + promptProcessingTime + " seconds");
                            Log.i(TAG, "Tokens generated per second (excluding prompt processing): " + tokensPerSecond);
                        }
                        catch (GenAIException e) {
                            Log.e(TAG, "Exception occurred during model query: " + e.getMessage());
                        }
                        finally {
                            if (generator != null) generator.close();
                            if (encodedPrompt != null) encodedPrompt.close();
                            if (stream != null) stream.close();
                            if (generatorParams != null) generatorParams.close();
                        }

                        runOnUiThread(() -> {
                            // Reset button and state after generation is done or stopped
                            sendMsgIB.setEnabled(false);
                            sendMsgIB.setAlpha(0.5f);
                            sendMsgIB.setImageResource(R.drawable.humnod_send); // Change icon back to "Send"
                            isGenerating = false;

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
        tokenizer.close();
        tokenizer = null;
        model.close();
        model = null;
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
            progressText.setText("Downloading...");
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
                        Log.d(TAG, "Downloading files: " + pctDone + "%");
                        runOnUiThread(() -> {
                            progressBar.setProgress((int) pctDone);
                            progressText.setText("Downloading: " + pctDone + "%");
                        });
                    }
                }
                @Override
                public void onDownloadComplete() {
                    Log.d(TAG, "All downloads completed.");

                    // Last download completed, create SimpleGenAI
                    try {
                        model = new Model(getFilesDir().getPath());
                        tokenizer = model.createTokenizer();
                        runOnUiThread(() -> {
                            Toast.makeText(context, "All downloads completed", Toast.LENGTH_SHORT).show();
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

    private void showTokenPopup(float promptProcessingTime, float tokenRate) {

        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.info_popup);

        TextView promptProcessingTimeTv = dialog.findViewById(R.id.prompt_processing_time_tv);
        TextView tokensPerSecondTv = dialog.findViewById(R.id.tokens_per_second_tv);
        Button closeBtn = dialog.findViewById(R.id.close_btn);

        promptProcessingTimeTv.setText(String.format("Prompt processing time: %.2f seconds", promptProcessingTime));
        tokensPerSecondTv.setText(String.format("Tokens per second: %.2f", tokenRate));

        closeBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


}
