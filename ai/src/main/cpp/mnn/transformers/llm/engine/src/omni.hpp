//
//  omni.hpp
//
//  Created by MNN on 2025/04/08.
//  Copyright © 2018, Alibaba Group Holding Limited
//

#ifndef OMNI_hpp
#define OMNI_hpp

#include "llm/llm.hpp"

namespace MNN {
using namespace Express;
namespace Transformer {

class MropeInfo {
public:
    MropeInfo() {}
    MropeInfo(const MropeInfo& info) {
        mT = info.mT;
        mH = info.mH;
        mW = info.mW;
    }
    int back() {
        if (mW.empty()) {
            return 0;
        }
        return mW.back();
    }
    int currentIdx() {
        if (mW.empty()) {
            return 0;
        }
        return back() + 1;
    }
    void push_back(int t, int h, int w) {
        mT.push_back(t);
        mH.push_back(h);
        mW.push_back(w);
    }
    void push_back(int t) {
        push_back(t, t, t);
    }
    void push_back() {
        int cur_idx = currentIdx();
        push_back(cur_idx, cur_idx, cur_idx);
    }
    void clear() {
        mT.clear();
        mH.clear();
        mW.clear();
    }
    std::vector<int> mT, mH, mW;
};

class Omni : public Llm {
public:
    Omni(std::shared_ptr<LlmConfig> config);
    ~Omni() {
        mVisionModule.reset();
    }
    virtual void load() override;
    virtual std::vector<Express::VARP> forwardRaw(Express::VARP hiddenState, Express::VARP mask, Express::VARP inputPos) override;
    virtual std::vector<int> tokenizer_encode(const std::string& query) override;
    virtual std::vector<int> tokenizer_encode(const MultimodalPrompt& multimodal_input) override;
    virtual Express::VARP embedding(const std::vector<int>& input_ids) override;
    virtual Express::VARP gen_position_ids(int seq_len) override;
    virtual void response(const std::vector<int>& input_ids, std::ostream* os = &std::cout, const char* end_with = nullptr, int max_new_tokens = -1) override;
    // some models preprocess function
    std::vector<int> visionProcess(VARP image);
    std::vector<int> defaultVisionProcess(VARP image);
    std::vector<int> qwen2VisionProcess(VARP image);
    std::vector<int> smolvlmVisionProcess(VARP image);
    std::vector<int> minicpmVisionProcess(VARP image);
private:
    int mVisionHeight = 448, mVisionWidth = 448, mVisionStart = 151857,
        mVisionEnd = 151858, mVisionPad = 151859;
    int mVisionGlobal = 49152;
    int mVisionSizeUnit = 1, mVisionMaxSize = 2048;
    int mVisionNum = 0;
    std::vector<float> mVisionMean{122.7709383, 116.7460125, 104.09373615};
    std::vector<float> mVisionNorm{0.01459843, 0.01500777, 0.01422007};
    std::vector<int> multimodeProcess(const std::string& mode, std::string info);
    std::vector<int> visionProcess(const std::string& file);
    std::vector<int> processImageContent(const std::string& content, const std::map<std::string, PromptImagePart>& images);
    std::shared_ptr<Module> mVisionModule;
    std::vector<VARP> mVisionEmbeddings;
    // m_rope position ids
    void addPositionIds(int t, int h = -1, int w = -1);
    MropeInfo mPositionIds;
};

}
}
#endif // OMNI_hpp
