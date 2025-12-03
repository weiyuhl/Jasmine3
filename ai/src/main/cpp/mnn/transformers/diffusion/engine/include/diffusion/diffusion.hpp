//
//  diffusion.hpp
//
//  Created by MNN on 2025/01/12.
//  MNN
//
#ifndef MNN_DIFFUSION_HPP
#define MNN_DIFFUSION_HPP

#include <vector>
#include <functional>
#include <string>
#include <memory>
#include <MNN/Interpreter.hpp>
#include <MNN/expr/Expr.hpp>
namespace MNN { namespace Express { class Module; namespace Executor { class RuntimeManager; } } }

 

namespace MNN {
namespace DIFFUSION {

class Tokenizer;
typedef enum {
    STABLE_DIFFUSION_1_5 = 0,
    STABLE_DIFFUSION_TAIYI_CHINESE = 1,
    DIFFUSION_MODEL_USER
} DiffusionModelType;

class MNN_PUBLIC Diffusion {
public:
    Diffusion(std::string modelPath, DiffusionModelType modelType, MNN::MNNForwardType backendType, int memoryMode);
    virtual ~Diffusion();
    static Diffusion* createDiffusion(std::string modelPath, DiffusionModelType modelType, MNN::MNNForwardType backendType, int memoryMode);

    bool run(const std::string prompt, const std::string imagePath, int iterNum, int randomSeed, std::function<void(int)> progressCallback);
    bool load();
private:
    
private:
    std::shared_ptr<MNN::Express::Executor::RuntimeManager> runtime_manager_;
    std::vector<std::shared_ptr<MNN::Express::Module>> mModules;
    // step_plms
    std::vector<int> mTimeSteps;
    std::vector<float> mAlphas;
    std::vector<MNN::Express::VARP> mEts;
    MNN::Express::VARP mSample;
    MNN::Express::VARP mLatentVar, mPromptVar, mTimestepVar, mSampleVar;
    std::vector<float> mInitNoise;
    
private:
    std::string mModelPath;
    DiffusionModelType mModelType;
    int mMaxTextLen = 77;
    /* 0 -> memory saving mode, for memory stictly limited application
        1 -> memory enough mode, for better image generation speed
        2 -> balance mode for memory and generation speed.
     */
    int mMemoryMode;
    MNN::MNNForwardType mBackendType;
    std::unique_ptr<Tokenizer> mTokenizer;
};

}
}
#endif
