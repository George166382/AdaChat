#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "whisper.h"

#define TAG "JNI_Whisper"

static struct whisper_context *g_ctx = nullptr;

extern "C" {

// 1. INIT
JNIEXPORT jboolean JNICALL
Java_com_example_andopsi_WhisperBridge_init(JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);

    g_ctx = whisper_init_from_file(path);

    env->ReleaseStringUTFChars(model_path, path);

    if (g_ctx != nullptr) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Model loaded: %s", path);
        return JNI_TRUE;
    } else {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to load model");
        return JNI_FALSE;
    }
}


// 2. PROCESS AUDIO (With Float Conversion Fix)
JNIEXPORT jstring JNICALL
Java_com_example_andopsi_WhisperBridge_processAudio(JNIEnv *env, jobject thiz, jshortArray pcm, jint length) {
    if (g_ctx == nullptr) return env->NewStringUTF("");

    jshort *pcm_shorts = env->GetShortArrayElements(pcm, nullptr);

    // CRITICAL FIX: Convert Int16 -> Float32
    std::vector<float> pcmf(length);
    for (int i = 0; i < length; i++) {
        pcmf[i] = static_cast<float>(pcm_shorts[i]) / 32768.0f;
    }

    env->ReleaseShortArrayElements(pcm, pcm_shorts, 0);

    // Setup Params
    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_progress = false;
    wparams.print_special = false;
    wparams.print_realtime = false;
    wparams.print_timestamps = false;
    wparams.language = "en";

    // Run Inference
    if (whisper_full(g_ctx, wparams, pcmf.data(), pcmf.size()) != 0) {
        return env->NewStringUTF("");
    }

    // Get Text Segments
    std::string result;
    const int n_segments = whisper_full_n_segments(g_ctx);
    for (int i = 0; i < n_segments; ++i) {
        const char *text = whisper_full_get_segment_text(g_ctx, i);
        result += text;
        result += " ";
    }

    return env->NewStringUTF(result.c_str());
}

// 3. DESTROY
JNIEXPORT void JNICALL
Java_com_example_andopsi_WhisperBridge_destroy(JNIEnv *env, jobject thiz) {
    if (g_ctx != nullptr) {
        whisper_free(g_ctx);
        g_ctx = nullptr;
    }
}

} // extern "C"