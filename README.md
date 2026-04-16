# HumNod - Local LLM Android Assistant & Framework

Welcome to the official repository for **HumNod**, a cutting-edge, privacy-centric Android application that democratizes access to artificial intelligence by running advanced Large Language Models (LLMs) entirely on your local mobile hardware. 

In an era where cloud computing raises severe data privacy concerns and requires constant internet connectivity, HumNod provides a robust alternative. By leveraging the Microsoft ONNX Runtime Generative AI API and native Android frameworks, this application transforms your smartphone into an offline powerhouse capable of context-aware chatting, physical document analysis (Local RAG), and real-time image text extraction, all without a single byte of your personal data ever leaving your device.

Whether you are an end-user looking for a secure AI companion or an Android developer seeking a highly structured foundational framework to build your own local AI apps, this repository provides everything you need.

[![HumNod Demo Video](https://img.youtube.com/vi/yTYgDmDwdxM/0.jpg)](https://www.youtube.com/watch?v=yTYgDmDwdxM "HumNod - Offline AI Assistant Demo")

*(Click the image above to watch the HumNod demo on YouTube)*

---

## 🌟 Comprehensive Feature Set

### 🔒 Zero-Trust Privacy & 100% Offline Inference
HumNod does not rely on API keys, cloud servers, or external internet connections for text generation. Once the initial LLM weights are downloaded, your device's CPU/NPU handles all the computational heavy lifting locally.

### 🧠 On-Device Model Hub
The app features a built-in model management system (`HubActivity`). Users can easily browse, download, and seamlessly switch between highly optimized, quantized state-of-the-art open-source models (such as Microsoft's Phi-3 or Meta's Llama 3) directly to their device storage. 

### 📄 Local Retrieval-Augmented Generation (RAG)
Chatting with your documents has never been more secure. Using the custom `DocumentProcessor.java` module, users can upload `.txt`, `.pdf`, and `.md` files directly into the chat interface. HumNod automatically extracts the text, cleans the formatting, and injects it into the LLM's context window, allowing you to ask questions about your specific offline data.

### 📸 Seamless Image OCR Integration
Need to analyze a screenshot or a photo of a physical document? Attach images directly to your prompt. HumNod utilizes Google ML Kit's on-device Vision libraries via `OCRHelper.java` to read the text inside the image and feed it to the AI for instant analysis and summarization.

### ⚙️ Granular Generation Controls
Take complete control over how the AI responds using the intuitive `BottomSheet` UI. You can tweak generation parameters in real-time without interrupting your workflow:
* **Max Response Length:** Control token output limits.
* **Length Penalty:** Force the model to be concise or highly descriptive.
* **Agent Type:** Alter the system prompt to change the persona of the AI (e.g., Academic, Casual, Coding Assistant).

---

## 🏗️ Technical Architecture & Codebase Deep Dive

HumNod is built entirely in Java, prioritizing performance, stability, and clean architectural patterns.

* **`MainActivity.java`**: The nerve center of the application. It acts as the bridge between the Android UI and the underlying C++ inference engine. It manages the active chat session, handles token streaming UI updates, and orchestrates user inputs.
* **ONNX Runtime Engine (`onnxruntime-genai-android-0.4.0-dev.aar`)**: Located in `app/libs/`, this pre-compiled AAR library provides the highly optimized execution environment for `.onnx` models, utilizing JNI to interface with the device's hardware.
* **`ModelDownloader.java`**: A robust background service responsible for safely fetching heavy model artifacts (`.onnx` weights, `.bin` configurations, tokenizers) from remote servers to local storage, complete with interrupt-resume capabilities.
* **Dynamic UI Rendering**: Found in `app/src/main/res/`, the UI uses optimized XML layouts and dynamic custom drawables (like `ic_animation_bear.gif` and dynamic corner radii) to provide a fluid, modern user experience while the model is actively streaming tokens.

---

## 🚀 Getting Started

### System Requirements
Running local LLMs requires modern mobile hardware. To ensure smooth generation speeds (Tokens Per Second):
* **RAM:** Minimum 4GB (6GB+ highly recommended for 3B+ parameter models).
* **Storage:** At least 2GB to 5GB of free space depending on the quantized model downloaded.
* **OS:** Android 10 (API Level 29) or higher.

### Developer Setup & Build Instructions

1.  **Clone the Source Code:**
    Open your terminal and clone the repository:
    ```bash
    git clone [https://github.com/yourusername/Local_LLM_AndroidApp.git](https://github.com/yourusername/Local_LLM_AndroidApp.git)
    cd Local_LLM_AndroidApp
    ```

2.  **Open in Android Studio:**
    Launch Android Studio (latest stable release recommended). Select **File > Open** and navigate to your cloned directory.

3.  **Verify Dependencies:**
    Allow Android Studio to sync the Gradle build files. **Crucial step:** Ensure that the local library `app/libs/onnxruntime-genai-android-0.4.0-dev.aar` is properly recognized, as the project will not compile without the ONNX GenAI bindings.

4.  **Compile and Run:**
    Connect a physical Android device with USB Debugging enabled. Emulators are generally too slow for local LLM inference testing unless hardware acceleration is heavily optimized. Click **Run (Shift + F10)**.

> **Note:** On your very first launch, you will need an active Wi-Fi connection to download your preferred LLM package via the Hub dashboard. Once the download is verified, you can safely turn on Airplane Mode and use the app indefinitely.

---

## 🤝 Contributing
We welcome contributions from the Android and Open-Source AI community! Whether you want to add support for new file types in the `DocumentProcessor`, optimize the UI with Jetpack Compose, or add support for newer ONNX execution providers, please feel free to fork the repository and submit a pull request.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

---

## 🛡️ License
This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details. This open-source framework is intended to help developers build secure, local-first applications.
   ```c
   Key: humnod
   Pass Both: 245632
   ```
