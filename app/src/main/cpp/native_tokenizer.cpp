#include <jni.h>
#include <string>
#include <vector>
#include <unordered_map>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <sstream>
#include <algorithm>
#include <fstream>
#include <map>
#include "native_tokenizer.h"

#define LOG_TAG "NativeTokenizer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 旧的NativeTokenizer类用于JNI接口
class LegacyNativeTokenizer {
private:
    std::unordered_map<std::string, int> vocab;
    std::unordered_map<int, std::string> id_to_token;
    int vocab_size = 0;
    int pad_token_id = 0;
    int unk_token_id = 1;
    int bos_token_id = 2;
    int eos_token_id = 3;
    
public:
    bool loadFromAsset(AAssetManager* assetManager) {
        LOGI("开始加载tokenizer配置...");
        
        AAsset* asset = AAssetManager_open(assetManager, "model/tokenizer.json", AASSET_MODE_BUFFER);
        if (!asset) {
            LOGE("无法打开tokenizer.json文件");
            return false;
        }
        
        size_t length = AAsset_getLength(asset);
        const char* buffer = (const char*)AAsset_getBuffer(asset);
        
        if (!buffer) {
            LOGE("无法读取tokenizer.json内容");
            AAsset_close(asset);
            return false;
        }
        
        std::string json_content(buffer, length);
        AAsset_close(asset);
        
        // 简化的JSON解析 - 提取vocab信息
        if (!parseTokenizerJson(json_content)) {
            LOGE("解析tokenizer.json失败");
            return false;
        }
        
        LOGI("Tokenizer加载成功，词汇表大小: %d", vocab_size);
        return true;
    }
    
    std::vector<int> encode(const std::string& text) {
        std::vector<int> tokens;
        
        // 添加BOS token
        tokens.push_back(bos_token_id);
        
        // 简单的分词策略：按空格和标点符号分割
        std::vector<std::string> words = tokenizeText(text);
        
        for (const auto& word : words) {
            auto it = vocab.find(word);
            if (it != vocab.end()) {
                tokens.push_back(it->second);
            } else {
                // 未知词使用UNK token
                tokens.push_back(unk_token_id);
            }
        }
        
        // 添加EOS token
        tokens.push_back(eos_token_id);
        
        return tokens;
    }
    
    std::string decode(const std::vector<int>& token_ids) {
        std::string result;
        
        for (int token_id : token_ids) {
            if (token_id == bos_token_id || token_id == eos_token_id || token_id == pad_token_id) {
                continue; // 跳过特殊token
            }
            
            auto it = id_to_token.find(token_id);
            if (it != id_to_token.end()) {
                if (!result.empty()) result += " ";
                result += it->second;
            }
        }
        
        return result;
    }
    
    int getVocabSize() const { return vocab_size; }
    int getPadTokenId() const { return pad_token_id; }
    int getEosTokenId() const { return eos_token_id; }
    
private:
    bool parseTokenizerJson(const std::string& json_content) {
        LOGI("开始解析tokenizer.json文件...");
        
        // 查找vocab部分
        size_t vocab_start = json_content.find("\"vocab\"");
        if (vocab_start == std::string::npos) {
            LOGE("在tokenizer.json中找不到vocab部分");
            return false;
        }
        
        // 找到vocab对象的开始
        size_t brace_start = json_content.find("{", vocab_start);
        if (brace_start == std::string::npos) {
            LOGE("找不到vocab对象的开始");
            return false;
        }
        
        // 找到匹配的结束大括号
        int brace_count = 1;
        size_t pos = brace_start + 1;
        size_t brace_end = std::string::npos;
        
        while (pos < json_content.length() && brace_count > 0) {
            if (json_content[pos] == '{') {
                brace_count++;
            } else if (json_content[pos] == '}') {
                brace_count--;
                if (brace_count == 0) {
                    brace_end = pos;
                    break;
                }
            }
            pos++;
        }
        
        if (brace_end == std::string::npos) {
            LOGE("找不到vocab对象的结束");
            return false;
        }
        
        // 提取vocab内容
        std::string vocab_content = json_content.substr(brace_start + 1, brace_end - brace_start - 1);
        
        // 解析键值对 - 简单的正则表达式替代
        size_t search_pos = 0;
        int max_token_id = -1;
        
        while (search_pos < vocab_content.length()) {
            // 查找下一个键值对
            size_t quote1 = vocab_content.find("\"", search_pos);
            if (quote1 == std::string::npos) break;
            
            size_t quote2 = vocab_content.find("\"", quote1 + 1);
            if (quote2 == std::string::npos) break;
            
            size_t colon = vocab_content.find(":", quote2);
            if (colon == std::string::npos) break;
            
            // 查找数字
            size_t num_start = colon + 1;
            while (num_start < vocab_content.length() && 
                   (vocab_content[num_start] == ' ' || vocab_content[num_start] == '\t' || 
                    vocab_content[num_start] == '\n' || vocab_content[num_start] == '\r')) {
                num_start++;
            }
            
            size_t num_end = num_start;
            while (num_end < vocab_content.length() && 
                   std::isdigit(vocab_content[num_end])) {
                num_end++;
            }
            
            if (num_start < num_end) {
                std::string token = vocab_content.substr(quote1 + 1, quote2 - quote1 - 1);
                std::string num_str = vocab_content.substr(num_start, num_end - num_start);
                
                try {
                    int token_id = std::stoi(num_str);
                    vocab[token] = token_id;
                    id_to_token[token_id] = token;
                    max_token_id = std::max(max_token_id, token_id);
                } catch (const std::exception& e) {
                    LOGE("解析token ID失败: %s", e.what());
                }
            }
            
            search_pos = num_end;
        }
        
        vocab_size = max_token_id + 1;
        
        // 确保特殊token存在
        if (vocab.find("<pad>") != vocab.end()) {
            pad_token_id = vocab["<pad>"];
        }
        if (vocab.find("<unk>") != vocab.end()) {
            unk_token_id = vocab["<unk>"];
        }
        if (vocab.find("<s>") != vocab.end()) {
            bos_token_id = vocab["<s>"];
        } else if (vocab.find("<bos>") != vocab.end()) {
            bos_token_id = vocab["<bos>"];
        }
        if (vocab.find("</s>") != vocab.end()) {
            eos_token_id = vocab["</s>"];
        } else if (vocab.find("<eos>") != vocab.end()) {
            eos_token_id = vocab["<eos>"];
        }
        
        LOGI("成功解析tokenizer.json，词汇表大小: %d", vocab_size);
        LOGI("特殊tokens - PAD: %d, UNK: %d, BOS: %d, EOS: %d", 
             pad_token_id, unk_token_id, bos_token_id, eos_token_id);
        
        return vocab_size > 0;
    }
    
    std::vector<std::string> tokenizeText(const std::string& text) {
        std::vector<std::string> tokens;
        std::string current_token;
        
        for (char c : text) {
            if (std::isspace(c) || std::ispunct(c)) {
                if (!current_token.empty()) {
                    tokens.push_back(current_token);
                    current_token.clear();
                }
                if (!std::isspace(c)) {
                    tokens.push_back(std::string(1, c));
                }
            } else {
                current_token += c;
            }
        }
        
        if (!current_token.empty()) {
            tokens.push_back(current_token);
        }
        
        return tokens;
    }
};

// 全局tokenizer实例
static LegacyNativeTokenizer* g_tokenizer = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_nativeInitializeTokenizer(JNIEnv *env, jobject thiz, jobject asset_manager) {
    if (g_tokenizer) {
        delete g_tokenizer;
    }
    
    g_tokenizer = new LegacyNativeTokenizer();
    AAssetManager* mgr = AAssetManager_fromJava(env, asset_manager);
    
    if (!g_tokenizer->loadFromAsset(mgr)) {
        delete g_tokenizer;
        g_tokenizer = nullptr;
        return JNI_FALSE;
    }
    
    return JNI_TRUE;
}

JNIEXPORT jintArray JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_nativeEncodeText(JNIEnv *env, jobject thiz, jstring text) {
    if (!g_tokenizer) {
        LOGE("Tokenizer未初始化");
        return nullptr;
    }
    
    const char* text_chars = env->GetStringUTFChars(text, nullptr);
    std::string text_str(text_chars);
    env->ReleaseStringUTFChars(text, text_chars);
    
    std::vector<int> tokens = g_tokenizer->encode(text_str);
    
    jintArray result = env->NewIntArray(tokens.size());
    env->SetIntArrayRegion(result, 0, tokens.size(), tokens.data());
    
    return result;
}

JNIEXPORT jstring JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_nativeDecodeTokens(JNIEnv *env, jobject thiz, jintArray token_ids) {
    if (!g_tokenizer) {
        LOGE("Tokenizer未初始化");
        return nullptr;
    }
    
    jsize length = env->GetArrayLength(token_ids);
    jint* tokens = env->GetIntArrayElements(token_ids, nullptr);
    
    std::vector<int> token_vector(tokens, tokens + length);
    env->ReleaseIntArrayElements(token_ids, tokens, JNI_ABORT);
    
    std::string decoded = g_tokenizer->decode(token_vector);
    return env->NewStringUTF(decoded.c_str());
}

JNIEXPORT jint JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_nativeGetVocabSize(JNIEnv *env, jobject thiz) {
    if (!g_tokenizer) {
        return 0;
    }
    return g_tokenizer->getVocabSize();
}

JNIEXPORT void JNICALL
Java_top_usagijin_summary_service_NativeModelLoader_nativeCleanupTokenizer(JNIEnv *env, jobject thiz) {
    if (g_tokenizer) {
        delete g_tokenizer;
        g_tokenizer = nullptr;
        LOGI("Tokenizer资源已清理");
    }
}

}

// 实现头文件中声明的NativeTokenizer类方法
bool NativeTokenizer::loadTokenizer(const std::string& tokenizer_path) {
    LOGI("开始加载tokenizer文件: %s", tokenizer_path.c_str());
    
    std::ifstream file(tokenizer_path);
    if (!file.is_open()) {
        LOGE("无法打开tokenizer文件: %s", tokenizer_path.c_str());
        return false;
    }
    
    // 读取整个文件内容
    std::string json_content((std::istreambuf_iterator<char>(file)),
                             std::istreambuf_iterator<char>());
    file.close();
    
    if (json_content.empty()) {
        LOGE("tokenizer文件为空");
        return false;
    }
    
    // 解析JSON内容
    if (!parseTokenizerJson(json_content)) {
        LOGE("解析tokenizer JSON失败");
        return false;
    }
    
    tokenizer_loaded = true;
    LOGI("✓ Tokenizer加载成功，词汇表大小: %zu", vocab.size());
    return true;
}

std::vector<int> NativeTokenizer::tokenize(const std::string& text) {
    if (!tokenizer_loaded) {
        LOGE("Tokenizer未加载");
        return {};
    }
    
    std::vector<int> tokens;
    
    // 简单的分词策略：按空格和标点符号分割
    std::vector<std::string> words = tokenizeText(text);
    
    for (const auto& word : words) {
        auto it = vocab.find(word);
        if (it != vocab.end()) {
            tokens.push_back(it->second);
        } else {
            // 未知词使用UNK token (假设ID为1)
            tokens.push_back(1);
        }
    }
    
    return tokens;
}

bool NativeTokenizer::isLoaded() const {
    return tokenizer_loaded;
}

size_t NativeTokenizer::getVocabSize() const {
    return vocab.size();
}

bool NativeTokenizer::parseTokenizerJson(const std::string& json_content) {
    LOGI("开始解析tokenizer.json文件...");
    
    // 查找vocab部分
    size_t vocab_start = json_content.find("\"vocab\"");
    if (vocab_start == std::string::npos) {
        LOGE("在tokenizer.json中找不到vocab部分");
        return false;
    }
    
    // 找到vocab对象的开始
    size_t brace_start = json_content.find("{", vocab_start);
    if (brace_start == std::string::npos) {
        LOGE("找不到vocab对象的开始");
        return false;
    }
    
    // 找到匹配的结束大括号
    int brace_count = 1;
    size_t pos = brace_start + 1;
    size_t brace_end = std::string::npos;
    
    while (pos < json_content.length() && brace_count > 0) {
        if (json_content[pos] == '{') {
            brace_count++;
        } else if (json_content[pos] == '}') {
            brace_count--;
            if (brace_count == 0) {
                brace_end = pos;
                break;
            }
        }
        pos++;
    }
    
    if (brace_end == std::string::npos) {
        LOGE("找不到vocab对象的结束");
        return false;
    }
    
    // 提取vocab内容
    std::string vocab_content = json_content.substr(brace_start + 1, brace_end - brace_start - 1);
    
    // 解析键值对
    size_t search_pos = 0;
    
    while (search_pos < vocab_content.length()) {
        // 查找下一个键值对
        size_t quote1 = vocab_content.find("\"", search_pos);
        if (quote1 == std::string::npos) break;
        
        size_t quote2 = vocab_content.find("\"", quote1 + 1);
        if (quote2 == std::string::npos) break;
        
        size_t colon = vocab_content.find(":", quote2);
        if (colon == std::string::npos) break;
        
        // 查找数字
        size_t num_start = colon + 1;
        while (num_start < vocab_content.length() && 
               (vocab_content[num_start] == ' ' || vocab_content[num_start] == '\t' || 
                vocab_content[num_start] == '\n' || vocab_content[num_start] == '\r')) {
            num_start++;
        }
        
        size_t num_end = num_start;
        while (num_end < vocab_content.length() && 
               std::isdigit(vocab_content[num_end])) {
            num_end++;
        }
        
        if (num_start < num_end) {
            std::string token = vocab_content.substr(quote1 + 1, quote2 - quote1 - 1);
            std::string num_str = vocab_content.substr(num_start, num_end - num_start);
            
            try {
                int token_id = std::stoi(num_str);
                vocab[token] = token_id;
            } catch (const std::exception& e) {
                LOGE("解析token ID失败: %s", e.what());
            }
        }
        
        search_pos = num_end;
    }
    
    LOGI("成功解析tokenizer.json，词汇表大小: %zu", vocab.size());
    return vocab.size() > 0;
}

std::vector<std::string> NativeTokenizer::tokenizeText(const std::string& text) {
    std::vector<std::string> tokens;
    std::string current_token;
    
    for (char c : text) {
        if (std::isspace(c) || std::ispunct(c)) {
            if (!current_token.empty()) {
                tokens.push_back(current_token);
                current_token.clear();
            }
            if (!std::isspace(c)) {
                tokens.push_back(std::string(1, c));
            }
        } else {
            current_token += c;
        }
    }
    
    if (!current_token.empty()) {
        tokens.push_back(current_token);
    }
    
    return tokens;
} 