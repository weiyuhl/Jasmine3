//
//  llm.hpp
//
//  Created by MNN on 2023/08/25.
//  ZhaodeWang
//

#ifndef LLM_hpp
#define LLM_hpp

#include <vector>
#include <memory>
#include <string>
#include <iosfwd>

#include <MNN/expr/Expr.hpp>
namespace MNN { namespace Express { class Module; namespace Executor { class RuntimeManager; } } }

namespace MNN {
namespace Transformer {
class Tokenizer;
class LlmConfig;
class DiskEmbedding;
class Sampler;
class Prompt;
class Generation;
 

using ChatMessage = std::pair<std::string, std::string>; // <role, content>
using ChatMessages = std::vector<ChatMessage>;

struct PromptImagePart;
struct MultimodalPrompt;

 

struct KVMeta;
struct LlmContext {
    // forward
    int prompt_len = 0;
    int gen_seq_len = 0;
    int all_seq_len = 0;
    std::ostream* os = nullptr;
    std::string end_with;
    // perf
    int64_t load_us = 0;
    int64_t vision_us = 0;
    int64_t prefill_us = 0;
    int64_t decode_us = 0;
    int64_t sample_us = 0;
    float prefill_mb = 0;
    float decode_mb = 0;
    // tokens
    int current_token;
    std::vector<int> history_tokens;
    std::vector<int> output_tokens;
    std::string generate_str;
};
struct GenerationParams;
class MNN_PUBLIC Llm {
public:
    enum Stage {
        Prefill,
        Decode
    };
    static Llm* createLLM(const std::string& config_path);
    static void destroy(Llm* llm);// For Windows RT mode should use destroy
    Llm(std::shared_ptr<LlmConfig> config);
    virtual ~Llm();
    virtual void load();
    virtual Express::VARP gen_attention_mask(int seq_len);
    virtual Express::VARP gen_position_ids(int seq_len);
    virtual Express::VARP embedding(const std::vector<int>& input_ids);
    virtual int sample(Express::VARP logits, int offset = 0, int size = 0);
    
    virtual std::vector<Express::VARP> forwardRaw(Express::VARP hiddenState, Express::VARP mask, Express::VARP inputPos);
    
    virtual void response(const std::vector<int>& input_ids, std::ostream* os = &std::cout, const char* end_with = nullptr, int max_new_tokens = -1);
    void response(const ChatMessages& chat_prompts, std::ostream* os = &std::cout, const char* end_with = nullptr, int max_new_tokens = -1);
    virtual void generate_init(std::ostream* os = nullptr, const char* end_with = nullptr);
    void generate(int max_token);
    
    // config function
    std::string dump_config();
    bool set_config(const std::string& content);
    
    virtual std::vector<int> tokenizer_encode(const std::string& query);
    
    
    virtual std::vector<int> tokenizer_encode(const MultimodalPrompt& multimodal_input);
    void response(const MultimodalPrompt& multimodal_input, 
                  std::ostream* os = &std::cout, 
                  const char* end_with = nullptr, 
                  int max_new_tokens = -1);
    const LlmContext* getContext() const {
        return mContext.get();
    }
protected:
    std::shared_ptr<LlmContext> mContext;
    std::shared_ptr<KVMeta> mMeta;
    std::shared_ptr<LlmConfig> mConfig;
    std::shared_ptr<Prompt> mPrompt;
    std::shared_ptr<Tokenizer> mTokenizer;
    std::shared_ptr<DiskEmbedding> mDiskEmbedding;
    std::shared_ptr<Sampler> mSampler;
    std::shared_ptr<Express::Executor::RuntimeManager> mRuntimeManager, mProcessorRuntimeManager;
    std::vector<std::shared_ptr<Express::Module>> mModules;
    /**
     key: <seq_len, all_logists>
     value : module
     note: prefill share one module, seq_len = 100 for example
     */
    const int mPrefillKey = 100;
    std::map<std::pair<int, bool>, std::shared_ptr<Express::Module>> mModulePool;
    const Express::Module* mBaseModule = nullptr;
    Express::VARP inputsEmbeds, attentionMask, positionIds;
    std::vector<Express::VARP> mAttentionMaskVarVec, mPositionIdsVarVec;
    Express::VARP logitsAllIdx, logitsLastIdx;
    int mSeqLenIndex = 0;
protected:
    
private:
    std::shared_ptr<Generation> mGenerationStrategy;
    

private:
    bool mInSpec = false;
    int mDraftLength = 4;
    std::shared_ptr<GenerationParams> mGenerateParam;
    bool mAsync = true;
};

 
}
}

#endif // LLM_hpp
