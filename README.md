# HumNod - Offline AI Assistant for Android

**HumNod** is a privacy-first, fully offline Android application that brings the power of Large Language Models (LLMs) directly to your mobile device. Built using Java and the Microsoft ONNX Runtime Generative AI API, HumNod allows you to chat, analyze documents, and extract text from images without ever needing an internet connection for inference.

[![HumNod Demo Video](https://img.youtube.com/vi/yTYgDmDwdxM/0.jpg)](https://www.youtube.com/watch?v=yTYgDmDwdxM "HumNod - Offline AI Assistant")

*(Click the image above to watch the HumNod demo on YouTube)*

## 🌟 Key Features

* 🔒 **100% Offline & Private**: All AI inference happens locally on your device's CPU/NPU. Your prompts, documents, and images never leave your phone.

* 🧠 **On-Device Model Hub**: Download, manage, and switch between quantized LLMs (like Phi-3 or Llama 3) directly from the app's `HubActivity`.

* 📄 **Chat with Documents (Local RAG)**: Upload `.txt`, `.pdf`, or `.md` files. HumNod's built-in `DocumentProcessor` extracts the text and injects it into the LLM's context window.

* 📸 **Image OCR Integration**: Attach images (photos or screenshots) to the chat. The `OCRHelper` utilises on-device ML Vision to extract text for the LLM to analyze.

* ⚙️ **Advanced Generation Settings**: Fine-tune your AI's responses on the fly using the built-in Settings menu. Adjust Max Response Length, Length Penalty, and Agent Types.

* ✨ **Rich & Intuitive UI**: Enjoy a modern chat interface with dynamic visual feedback (GIF animations), custom typography (Karla font), and light/dark mode support.

## 🏗️ Architecture & Codebase

HumNod is built natively for Android using Java. Here is a quick overview of the core components:

* **`MainActivity.java`**: The core chat interface. It handles user inputs, manages the chat session, and interfaces directly with the ONNX Runtime engine.

* **`HubActivity.java` & `ModelDownloader.java`**: Acts as the central dashboard for fetching LLM artifacts (e.g., `.onnx`, `.bin` weights) from remote servers to local storage and managing the active model.

* **`DocumentProcessor.java` & `OCRHelper.java`**: The utility classes are responsible for parsing text from physical files and images to provide contextual data to the LLM.

* **ONNX Runtime (`onnxruntime-genai-android`)**: The heavy lifting is done via a pre-compiled AAR library located in `app/libs/`, providing highly optimized C++ inference engines with JNI bindings for mobile processors.

## 🚀 Getting Started

### Prerequisites

* [Android Studio](https://developer.android.com/studio) (Latest version recommended)

* An Android device or emulator with sufficient RAM and storage to hold and run quantized LLM models (4GB+ RAM recommended, depending on the model size).

### Build Instructions

1. **Clone the repository:**

   ```bash
   git clone [https://github.com/yourusername/Local_LLM_AndroidApp.git](https://github.com/yourusername/Local_LLM_AndroidApp.git)
   cd Local_LLM_AndroidApp

```c
Key: humnod
Pass Both: 245632
