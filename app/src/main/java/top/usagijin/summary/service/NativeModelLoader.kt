package top.usagijin.summary.service

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.File

/**
 * Native模型加载器 - 使用NDK在C++层加载1.8GB模型，避免Java堆限制
 */
class NativeModelLoader {
    
    companion object {
        private const val TAG = "NativeModelLoader"
        
        init {
            try {
                System.loadLibrary("nativemodel")
                Log.i(TAG, "Native库加载成功")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Native库加载失败: ${e.message}")
            }
        }
        
        @Volatile
        private var INSTANCE: NativeModelLoader? = null
        
        fun getInstance(): NativeModelLoader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NativeModelLoader().also { INSTANCE = it }
            }
        }
    }
    
    private var isModelInitialized = false
    private var isTokenizerInitialized = false
    
    /**
     * 完整初始化：模型 + 分词器
     */
    fun initialize(context: Context): Boolean {
        Log.i(TAG, "开始完整初始化（模型 + 分词器）")
        
        try {
            // 1. 准备模型和分词器文件路径
            val filePaths = prepareModelFiles(context)
            if (filePaths == null) {
                Log.e(TAG, "模型文件准备失败")
                return false
            }
            
            // 2. 加载模型
            if (!loadNativeModel(filePaths.first)) {
                Log.e(TAG, "模型加载失败")
                return false
            }
            
            // 3. 初始化分词器（使用AssetManager方式）
            Log.i(TAG, "开始初始化分词器...")
            if (!initTokenizer(context.assets)) {
                Log.e(TAG, "分词器初始化失败")
                return false
            }
            
            Log.i(TAG, "✓ 完整初始化成功！")
            Log.i(TAG, "词汇表大小: ${getVocabularySize()}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "初始化异常: ${e.message}", e)
            return false
        }
    }
    
    /**
     * 准备模型和分词器文件（从assets复制到内部存储）
     * @return Pair<modelPath, tokenizerPath> 或 null
     */
    private fun prepareModelFiles(context: Context): Pair<String, String>? {
        return try {
            val internalDir = File(context.filesDir, "models")
            if (!internalDir.exists()) {
                internalDir.mkdirs()
            }
            
            // 准备模型文件
            val modelPath = prepareModelFile(context, internalDir)
            if (modelPath == null) {
                Log.e(TAG, "模型文件准备失败")
                return null
            }
            
            // 准备分词器文件
            val tokenizerPath = prepareTokenizerFile(context, internalDir)
            if (tokenizerPath == null) {
                Log.e(TAG, "分词器文件准备失败")
                return null
            }
            
            Pair(modelPath, tokenizerPath)
            
        } catch (e: Exception) {
            Log.e(TAG, "准备文件失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 准备模型文件（从assets复制到内部存储）
     */
    private fun prepareModelFile(context: Context, internalDir: File): String? {
        return try {
            val modelFile = File(internalDir, "model.onnx")
            
            // 如果文件已存在，检查大小
            if (modelFile.exists()) {
                val expectedSize = getAssetFileSize(context.assets, "model/model.onnx")
                if (expectedSize > 0 && modelFile.length() == expectedSize) {
                    Log.i(TAG, "使用已存在的模型文件: ${modelFile.absolutePath}")
                    return modelFile.absolutePath
                } else {
                    Log.i(TAG, "模型文件大小不匹配，重新复制...")
                    modelFile.delete()
                }
            }
            
            // 复制模型文件
            Log.i(TAG, "开始复制模型文件...")
            context.assets.open("model/model.onnx").use { inputStream ->
                modelFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(8 * 1024 * 1024) // 8MB缓冲区
                    var totalBytes = 0L
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        
                        // 每复制100MB输出一次进度
                        if (totalBytes % (100 * 1024 * 1024) == 0L) {
                            Log.i(TAG, "已复制: ${totalBytes / (1024 * 1024)}MB")
                        }
                    }
                    
                    Log.i(TAG, "✓ 模型文件复制完成: ${totalBytes / (1024 * 1024)}MB")
                }
            }
            
            modelFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "准备模型文件失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 准备分词器文件（从assets复制到内部存储）
     */
    private fun prepareTokenizerFile(context: Context, internalDir: File): String? {
        return try {
            val tokenizerFile = File(internalDir, "tokenizer.json")
            
            // 如果文件已存在，检查大小
            if (tokenizerFile.exists()) {
                val expectedSize = getAssetFileSize(context.assets, "model/tokenizer.json")
                if (expectedSize > 0 && tokenizerFile.length() == expectedSize) {
                    Log.i(TAG, "使用已存在的分词器文件: ${tokenizerFile.absolutePath}")
                    return tokenizerFile.absolutePath
                } else {
                    Log.i(TAG, "分词器文件大小不匹配，重新复制...")
                    tokenizerFile.delete()
                }
            }
            
            // 复制分词器文件
            Log.i(TAG, "开始复制分词器文件...")
            context.assets.open("model/tokenizer.json").use { inputStream ->
                tokenizerFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(64 * 1024) // 64KB缓冲区
                    var totalBytes = 0L
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                    }
                    
                    Log.i(TAG, "✓ 分词器文件复制完成: ${totalBytes / 1024}KB")
                }
            }
            
            tokenizerFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "准备分词器文件失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 获取Asset文件大小
     */
    private fun getAssetFileSize(assetManager: AssetManager, assetPath: String): Long {
        return try {
            assetManager.openFd(assetPath).use { it.length }
        } catch (e: Exception) {
            // 如果无法获取文件描述符（压缩文件），返回-1
            -1L
        }
    }
    
    /**
     * 执行文本摘要推理
     */
    fun performSummarization(inputText: String): String? {
        return try {
            if (!isFullyInitialized()) {
                Log.w(TAG, "模型或分词器未完全初始化")
                return null
            }
            
            Log.d(TAG, "开始文本摘要推理，输入长度: ${inputText.length}")
            
            // 直接调用native推理方法
            val result = nativeRunInference(inputText)
            
            if (result.isNullOrEmpty()) {
                Log.w(TAG, "推理返回空结果")
                return null
            }
            
            Log.d(TAG, "推理完成，输出长度: ${result.length}")
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "推理异常: ${e.message}", e)
            null
        }
    }
    
    /**
     * 文本编码（分词）
     */
    fun encodeText(text: String): IntArray? {
        return try {
            if (!isTokenizerInitialized) {
                Log.w(TAG, "分词器未初始化")
                return null
            }
            
            nativeEncodeText(text)
        } catch (e: Exception) {
            Log.e(TAG, "文本编码异常: ${e.message}", e)
            null
        }
    }
    
    /**
     * Token解码
     */
    fun decodeTokens(tokens: IntArray): String? {
        return try {
            if (!isTokenizerInitialized) {
                Log.w(TAG, "分词器未初始化")
                return null
            }
            
            nativeDecodeTokens(tokens)
        } catch (e: Exception) {
            Log.e(TAG, "Token解码异常: ${e.message}", e)
            null
        }
    }
    
    /**
     * 检查是否完全初始化
     */
    fun isFullyInitialized(): Boolean {
        return try {
            nativeIsModelLoaded() && isTokenizerInitialized
        } catch (e: Exception) {
            Log.e(TAG, "检查初始化状态异常: ${e.message}")
            false
        }
    }
    
    /**
     * 获取词汇表大小
     */
    fun getVocabularySize(): Int {
        return try {
            if (!isTokenizerInitialized) {
                Log.w(TAG, "分词器未初始化")
                return 0
            }
            nativeGetVocabSize()
        } catch (e: Exception) {
            Log.e(TAG, "获取词汇表大小异常: ${e.message}")
            0
        }
    }
    
    /**
     * 清理所有资源
     */
    fun cleanup() {
        try {
            Log.i(TAG, "清理所有Native资源")
            
            if (isModelInitialized) {
                nativeCleanupInference()
                isModelInitialized = false
            }
            
            if (isTokenizerInitialized) {
                nativeCleanupTokenizer()
                isTokenizerInitialized = false
            }
            
            Log.i(TAG, "✓ 资源清理完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "资源清理异常: ${e.message}", e)
        }
    }
    
    // ========== Native方法声明 ==========
    
    // 分词器相关
    private external fun nativeInitializeTokenizer(assetManager: AssetManager): Boolean
    private external fun nativeEncodeText(text: String): IntArray
    private external fun nativeDecodeTokens(tokenIds: IntArray): String
    private external fun nativeGetVocabSize(): Int
    private external fun nativeCleanupTokenizer()
    
    // 模型推理相关
    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeRunInference(inputText: String): String?
    private external fun nativeIsModelLoaded(): Boolean
    private external fun nativeCleanupInference()
    
    // ========== 私有方法 ==========
    
    private fun initTokenizer(assetManager: AssetManager): Boolean {
        val result = nativeInitializeTokenizer(assetManager)
        isTokenizerInitialized = result
        return result
    }
    
    private fun loadNativeModel(modelPath: String): Boolean {
        val result = nativeLoadModel(modelPath)
        isModelInitialized = result
        return result
    }
} 