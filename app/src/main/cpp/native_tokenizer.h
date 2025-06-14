#ifndef NATIVE_TOKENIZER_H
#define NATIVE_TOKENIZER_H

#include <string>
#include <vector>
#include <map>

class NativeTokenizer {
private:
    std::map<std::string, int> vocab;
    bool tokenizer_loaded = false;
    
    bool parseTokenizerJson(const std::string& json_content);
    std::vector<std::string> tokenizeText(const std::string& text);

public:
    bool loadTokenizer(const std::string& tokenizer_path);
    std::vector<int> tokenize(const std::string& text);
    bool isLoaded() const;
    size_t getVocabSize() const;
};

#endif // NATIVE_TOKENIZER_H 