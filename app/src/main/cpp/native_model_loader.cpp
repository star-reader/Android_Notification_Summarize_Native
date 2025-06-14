#include <jni.h>
#include <string>
#include <vector>
#include <memory>
#include <fstream>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#define TAG "NativeModelLoader"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

class NativeModelData {
public:
    std::vector<uint8_t> modelData;
    std::string tokenizerData;
    bool isLoaded = false;
    
    void clear() {
        modelData.clear();
        tokenizerData.clear();
        isLoaded = false;
    }
};

// 全局模型数据实例
static std::unique_ptr<NativeModelData> g_modelData = nullptr;

// 函数声明
std::vector<int> simulateInferenceInNative(const jint* inputTokens, jsize inputLength);

extern "C" JNIEXPORT jboolean JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_initializeModel(
    JNIEnv *env,
    jobject /* this */,
    jobject assetManager
) {
    LOGI("开始初始化模型");
    
    try {
        // 初始化全局模型数据
        if (!g_modelData) {
            g_modelData = std::make_unique<NativeModelData>();
        }
        
        // 清理之前的数据
        g_modelData->clear();
        
        // 获取AssetManager
        AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
        if (!mgr) {
            LOGE("无法获取AssetManager");
            return JNI_FALSE;
        }
        
        // 加载模型文件
        LOGI("开始加载模型文件（1.8GB）");
        AAsset* modelAsset = AAssetManager_open(mgr, "model/model.onnx", AASSET_MODE_STREAMING);
        if (!modelAsset) {
            LOGE("无法打开模型文件");
            return JNI_FALSE;
        }
        
        // 获取模型文件大小
        off_t modelSize = AAsset_getLength(modelAsset);
        LOGI("模型文件大小: %ld bytes (%.2f MB)", modelSize, modelSize / (1024.0 * 1024.0));
        
        // 分配内存并读取模型数据
        g_modelData->modelData.resize(modelSize);
        
        // 分块读取，避免一次性读取过大内存
        const size_t chunkSize = 8 * 1024 * 1024; // 8MB块
        size_t totalRead = 0;
        size_t remainingBytes = modelSize;
        
        while (remainingBytes > 0) {
            size_t bytesToRead = std::min(chunkSize, remainingBytes);
            int bytesRead = AAsset_read(modelAsset, 
                g_modelData->modelData.data() + totalRead, bytesToRead);
            
            if (bytesRead <= 0) {
                LOGE("读取模型文件失败，已读取: %zu bytes", totalRead);
                AAsset_close(modelAsset);
                return JNI_FALSE;
            }
            
            totalRead += bytesRead;
            remainingBytes -= bytesRead;
            
            // 每读取100MB打印一次进度
            if (totalRead % (100 * 1024 * 1024) == 0 || remainingBytes == 0) {
                LOGI("模型加载进度: %.1f%% (%zu/%ld bytes)", 
                    (totalRead * 100.0) / modelSize, totalRead, modelSize);
            }
        }
        
        AAsset_close(modelAsset);
        LOGI("模型文件加载完成");
        
        // 加载tokenizer文件
        LOGI("开始加载tokenizer文件");
        AAsset* tokenizerAsset = AAssetManager_open(mgr, "model/tokenizer.json", AASSET_MODE_BUFFER);
        if (!tokenizerAsset) {
            LOGE("无法打开tokenizer文件");
            return JNI_FALSE;
        }
        
        off_t tokenizerSize = AAsset_getLength(tokenizerAsset);
        LOGI("Tokenizer文件大小: %ld bytes", tokenizerSize);
        
        g_modelData->tokenizerData.resize(tokenizerSize);
        int tokenizerBytesRead = AAsset_read(tokenizerAsset, 
            const_cast<char*>(g_modelData->tokenizerData.data()), tokenizerSize);
        
        AAsset_close(tokenizerAsset);
        
        if (tokenizerBytesRead != tokenizerSize) {
            LOGE("读取tokenizer文件失败");
            return JNI_FALSE;
        }
        
        LOGI("Tokenizer文件加载完成");
        
        // 标记为已加载
        g_modelData->isLoaded = true;
        
        LOGI("模型初始化完成！");
        return JNI_TRUE;
        
    } catch (const std::exception& e) {
        LOGE("模型初始化异常: %s", e.what());
        return JNI_FALSE;
    } catch (...) {
        LOGE("模型初始化发生未知异常");
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_nativeIsModelLoaded(
    JNIEnv *env,
    jobject /* this */
) {
    return g_modelData && g_modelData->isLoaded ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_nativeGetModelSize(
    JNIEnv *env,
    jobject /* this */
) {
    if (!g_modelData || !g_modelData->isLoaded) {
        return 0;
    }
    return static_cast<jlong>(g_modelData->modelData.size());
}

extern "C" JNIEXPORT jstring JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_nativeGetTokenizerData(
    JNIEnv *env,
    jobject /* this */
) {
    if (!g_modelData || !g_modelData->isLoaded) {
        return nullptr;
    }
    return env->NewStringUTF(g_modelData->tokenizerData.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_nativeReleaseModel(
    JNIEnv *env,
    jobject /* this */
) {
    LOGI("释放模型资源");
    if (g_modelData) {
        g_modelData->clear();
        g_modelData.reset();
    }
    LOGI("模型资源已释放");
}

// 获取模型数据指针（用于ONNX Runtime）
extern "C" JNIEXPORT jlong JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_nativeGetModelDataPointer(
    JNIEnv *env,
    jobject /* this */
) {
    if (!g_modelData || !g_modelData->isLoaded || g_modelData->modelData.empty()) {
        return 0;
    }
    return reinterpret_cast<jlong>(g_modelData->modelData.data());
}

// 执行ONNX模型推理
extern "C" JNIEXPORT jintArray JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_nativePerformInference(
    JNIEnv *env,
    jobject /* this */,
    jintArray inputTokens
) {
    LOGI("开始Native ONNX推理");
    
    try {
        // 检查模型是否已加载
        if (!g_modelData || !g_modelData->isLoaded) {
            LOGE("模型未加载，无法执行推理");
            return env->NewIntArray(0);
        }
        
        // 获取输入tokens
        jsize inputLength = env->GetArrayLength(inputTokens);
        jint* inputData = env->GetIntArrayElements(inputTokens, nullptr);
        
        if (!inputData || inputLength == 0) {
            LOGE("输入tokens无效");
            return env->NewIntArray(0);
        }
        
        LOGI("输入token数量: %d", inputLength);
        
        // 这里应该实现真正的ONNX推理
        // 由于需要ONNX Runtime C++ API，目前先实现一个简化版本
        
        // 模拟推理过程：根据输入生成一个合理的JSON响应
        std::vector<int> outputTokens = simulateInferenceInNative(inputData, inputLength);
        
        // 释放输入数组
        env->ReleaseIntArrayElements(inputTokens, inputData, JNI_ABORT);
        
        // 创建输出数组
        jintArray result = env->NewIntArray(outputTokens.size());
        if (result == nullptr) {
            LOGE("无法创建输出数组");
            return env->NewIntArray(0);
        }
        
        env->SetIntArrayRegion(result, 0, outputTokens.size(), outputTokens.data());
        
        LOGI("Native推理完成，输出token数量: %zu", outputTokens.size());
        return result;
        
    } catch (const std::exception& e) {
        LOGE("Native推理异常: %s", e.what());
        return env->NewIntArray(0);
    } catch (...) {
        LOGE("Native推理发生未知异常");
        return env->NewIntArray(0);
    }
}

// 模拟推理（临时实现，直到真正的ONNX Runtime集成完成）
std::vector<int> simulateInferenceInNative(const jint* inputTokens, jsize inputLength) {
    LOGI("执行模拟推理（Native层）");
    
    // 生成一个固定的JSON响应的token序列
    // 这里模拟生成：{"title": "通知摘要", "summary": "收到新消息", "importanceLevel": 3}
    
    std::string jsonResponse = R"({"title": "AI摘要", "summary": "通过ONNX模型生成的智能摘要", "importanceLevel": 3})";
    
    // 将字符串转换为token（简单的字符级编码）
    std::vector<int> outputTokens;
    for (char c : jsonResponse) {
        outputTokens.push_back(static_cast<int>(c));
    }
    
    LOGI("模拟推理生成响应: %s", jsonResponse.c_str());
    return outputTokens;
} 