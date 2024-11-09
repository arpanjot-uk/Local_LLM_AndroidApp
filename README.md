# Local Chatbot on Android with Phi-3, ONNX Runtime Mobile and ONNX Runtime Generate() API

## Overview

This is a basic [Phi-3](https://huggingface.co/microsoft/Phi-3-mini-4k-instruct) Android example application with [ONNX Runtime mobile](https://onnxruntime.ai/docs/tutorials/mobile/) and [ONNX Runtime Generate() API](https://github.com/microsoft/onnxruntime-genai) with support for efficiently running generative AI models. This app demonstrates the usage of phi-3 model in a simple question answering chatbot mode.

### Model
The model used here is [ONNX Phi-3 model on HuggingFace](https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/tree/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4) with INT4 quantization and optimizations for mobile usage.

You can also optimize your fine-tuned PyTorch Phi-3 model for mobile usage following this example [Phi3 optimization with Olive](https://github.com/microsoft/Olive/tree/main/examples/phi3). 

### Requirements
- Android Studio Giraffe | 2022.3.1 or later (installed on Mac/Windows/Linux)
- Android SDK 29+
- Android NDK r22+
- An Android device or an Android Emulator
