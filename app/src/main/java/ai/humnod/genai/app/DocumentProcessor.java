package ai.humnod.genai.app;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class DocumentProcessor {

    private static final String TAG = "DocumentProcessor";

    public static String processAndSummarizeContent(String content, int summarizationPercent) {
        // Filter and validate ASCII content
        ASCIIValidationResult validationResult = filterAndValidateASCII(content);

        if (!validationResult.isValid) {
            Log.d(TAG, "Content contains too many non-ASCII characters.");
            return "Not ASCII";
        }

        // Use cleaned ASCII content
        content = validationResult.cleanedContent;

        StringBuilder finalOutput = new StringBuilder();

        // Normalize input
        content = content.replaceAll("\\s+", " ").trim(); // Normalize spaces
        content = content.replaceAll("\\. ", ".\n");     // Add line breaks after periods

        Log.d(TAG, "Normalized Input Content: " + content);

        // Count words in the provided content
        int wordCount = countWords(content);
        Log.d(TAG, "Total word count: " + wordCount);

        // Skip processing if fewer than 100 words
        if (wordCount < 50) {
            Log.d(TAG, "Word count less than 50, returning content as-is.");
            return content; // Return the content as-is
        }

        // Process content in chunks
        BufferedReader chunkReader = new BufferedReader(new StringReader(content));
        List<String> chunk = new ArrayList<>();
        String line;

        try {
            while ((line = chunkReader.readLine()) != null) {
                chunk.add(line);

                // Process the chunk when it reaches the desired size
                if (chunk.size() >= 5) {
                    Log.d(TAG, "Processing chunk of size " + chunk.size() + ":");
                    for (String chunkLine : chunk) {
                        Log.d(TAG, chunkLine);
                    }
                    finalOutput.append(processChunk(chunk, summarizationPercent));
                    chunk.clear(); // Reset the chunk for the next set of lines
                }
            }

            // Process any remaining lines
            if (!chunk.isEmpty()) {
                Log.d(TAG, "Processing remaining chunk of size " + chunk.size() + ":");
                for (String chunkLine : chunk) {
                    Log.d(TAG, chunkLine);
                }
                finalOutput.append(processChunk(chunk, summarizationPercent));
            }

            chunkReader.close();

        } catch (IOException e) {
            Log.e(TAG, "Error processing content: " + e.getMessage());
            return "Error processing content: " + e.getMessage();
        }

        // Check summarized word count limit
        int summarizedWordCount = countWords(finalOutput.toString());
        Log.d(TAG, "Summarized Word Count: " + summarizedWordCount);

        if (summarizedWordCount > 300) {
            Log.d(TAG, "Summarized content exceeds the 300-word limit.");
            return "Limit Hit";
        }

        return finalOutput.toString();
    }

    private static class ASCIIValidationResult {
        String cleanedContent;
        boolean isValid;

        public ASCIIValidationResult(String cleanedContent, boolean isValid) {
            this.cleanedContent = cleanedContent;
            this.isValid = isValid;
        }
    }

    private static ASCIIValidationResult filterAndValidateASCII(String content) {
        StringBuilder cleanedContent = new StringBuilder();
        int nonASCIICount = 0;

        for (char c : content.toCharArray()) {
            if ((c >= 32 && c <= 126) || c == 10 || c == 13 || c == 9) {
                cleanedContent.append(c); // Keep valid ASCII characters
            } else {
                nonASCIICount++; // Count invalid ASCII characters
            }

            if (nonASCIICount > 10) {
                // Too many non-ASCII characters
                return new ASCIIValidationResult("", false);
            }
        }

        return new ASCIIValidationResult(cleanedContent.toString(), true);
    }

    private static int countWords(String text) {
        // Split the text into words and count
        String[] words = text.split("\\s+");
        return words.length;
    }

    private static String processChunk(List<String> chunk, int summarizationPercent) {
        StringBuilder finalChunkOutput = new StringBuilder();
        List<String> currentSubChunk = new ArrayList<>();
        boolean currentIsCode = isCodeChunk(Collections.singletonList(chunk.get(0))); // Check the first line

        for (String line : chunk) {
            boolean lineIsCode = isCodeChunk(Collections.singletonList(line));

            if (lineIsCode == currentIsCode) {
                // Add to the current sub-chunk
                currentSubChunk.add(line);
            } else {
                // Process the previous sub-chunk
                finalChunkOutput.append(processSubChunk(currentSubChunk, currentIsCode, summarizationPercent));
                // Start a new sub-chunk
                currentSubChunk.clear();
                currentSubChunk.add(line);
                currentIsCode = lineIsCode;
            }
        }

        // Process the remaining sub-chunk
        if (!currentSubChunk.isEmpty()) {
            finalChunkOutput.append(processSubChunk(currentSubChunk, currentIsCode, summarizationPercent));
        }

        return finalChunkOutput.toString();
    }

    private static String processSubChunk(List<String> subChunk, boolean isCode, int summarizationPercent) {
        if (isCode) {
            Log.d(TAG, "SubChunk classified as: Code");
            for (String line : subChunk) {
                Log.d(TAG, "CodeSubChunk: " + line);
            }
            return String.join("\n", subChunk) + "\n";
        } else {
            Log.d(TAG, "SubChunk classified as: Text");
            for (String line : subChunk) {
                Log.d(TAG, "TextSubChunk: " + line);
            }
            StringBuilder textBuffer = new StringBuilder();
            for (String line : subChunk) {
                textBuffer.append(line).append(" ");
            }
            String summary = summarizeTextContent(textBuffer.toString(), summarizationPercent); // Pass percentage here
            Log.d(TAG, "Generated Summary for SubChunk: " + summary);
            return summary + "\n";
        }
    }

    private static boolean isCodeChunk(List<String> chunk) {
        int wordCount = 0;
        int symbolCount = 0;
        int keywordCount = 0;

        for (String line : chunk) {
            String trimmedLine = line.trim();

            // Count words
            wordCount += trimmedLine.split("\\s+").length;

            // Count symbols (non-alphanumeric, non-space characters)
            symbolCount += trimmedLine.replaceAll("[A-Za-z0-9_\\s]", "").length();

            // Check for programming keywords and patterns
            if (trimmedLine.matches(".*[{}();=<>\\[\\]].*") || // Common programming symbols
                    trimmedLine.matches(".*\\b(class|def|if|else|for|while|return|public|private|import|export|function|lambda|SELECT|INSERT|DELETE|UPDATE|FROM|BEGIN|END|try|catch|finally|async|await)\\b.*") || // Keywords
                    trimmedLine.matches(".*<[^>]+>.*") || // HTML/XML tags
                    trimmedLine.startsWith("@Override") || // Annotations
                    trimmedLine.matches("^\\s*[A-Za-z0-9_]+\\s*\\(.*\\).*;?") || // Function calls
                    trimmedLine.startsWith("//") || trimmedLine.startsWith("#") || trimmedLine.startsWith("/*") || trimmedLine.startsWith("*") || trimmedLine.startsWith("--")) { // Comments
                keywordCount++;
            }
        }

        // Adjust thresholds: Require higher symbol and keyword counts for code classification
        boolean isCode = (symbolCount > wordCount * 0.6) || (keywordCount >= 3);
        Log.d(TAG, "Chunk Metrics - Word Count: " + wordCount +
                ", Symbol Count: " + symbolCount +
                ", Keyword Count: " + keywordCount +
                ", Is Code: " + isCode);
        return isCode;
    }

    private static String summarizeTextContent(String text, int summarizationPercent) {
        String[] sentences = text.split("\\.\\s+");
        int totalSentences = sentences.length;
        int numSentences = Math.max(1, (int) Math.ceil((summarizationPercent / 100.0) * totalSentences));
        Log.d(TAG, "Total Sentences: " + totalSentences + ", Retaining: " + numSentences);

        double[][] similarityMatrix = new double[totalSentences][totalSentences];
        for (int i = 0; i < totalSentences; i++) {
            for (int j = 0; j < totalSentences; j++) {
                if (i != j) {
                    similarityMatrix[i][j] = sentenceSimilarity(sentences[i], sentences[j]);
                }
            }
        }

        double[] scores = textRank(similarityMatrix);
        Map<Integer, Double> sentenceScores = new HashMap<>();
        for (int i = 0; i < totalSentences; i++) {
            sentenceScores.put(i, scores[i]);
        }
        List<Map.Entry<Integer, Double>> rankedSentences = new ArrayList<>(sentenceScores.entrySet());
        rankedSentences.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        Set<Integer> selectedIndices = new HashSet<>();
        for (int i = 0; i < Math.min(numSentences, rankedSentences.size()); i++) {
            selectedIndices.add(rankedSentences.get(i).getKey());
        }

        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < totalSentences; i++) {
            if (selectedIndices.contains(i)) {
                summary.append(sentences[i]).append(". ");
            }
        }

        return summary.toString().trim();
    }

    private static double sentenceSimilarity(String sentence1, String sentence2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(sentence1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(sentence2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        double numerator = intersection.size();
        double denominator = Math.sqrt(words1.size() * words2.size());

        return (denominator == 0) ? 0 : numerator / denominator;
    }

    private static double[] textRank(double[][] similarityMatrix) {
        int n = similarityMatrix.length;
        double[] scores = new double[n];
        Arrays.fill(scores, 1.0);

        double d = 0.85; // Damping factor
        int maxIter = 100;
        double minDiff = 0.001;

        for (int iter = 0; iter < maxIter; iter++) {
            double[] newScores = new double[n];
            double maxChange = 0;

            for (int i = 0; i < n; i++) {
                newScores[i] = 1 - d;
                for (int j = 0; j < n; j++) {
                    if (i != j && similarityMatrix[j][i] > 0) {
                        newScores[i] += d * similarityMatrix[j][i] * scores[j] / rowSum(similarityMatrix[j]);
                    }
                }
                maxChange = Math.max(maxChange, Math.abs(newScores[i] - scores[i]));
            }

            scores = newScores;
            if (maxChange < minDiff) {
                break;
            }
        }

        return scores;
    }

    private static double rowSum(double[] row) {
        double sum = 0;
        for (double val : row) {
            sum += val;
        }
        return sum;
    }
}
