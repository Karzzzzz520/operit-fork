cmake_minimum_required(VERSION 3.22.1)

project("llama_jni")

include("${CMAKE_CURRENT_LIST_DIR}/../../cmake/operit_git_source.cmake")

set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
# armeabi-v7a 没有 armv8 FP16 向量指令集（__ARM_FP16_VECTOR_ARITHMETIC），
# ggml-cpu/llamafile 的 fp16 内联汇编在该 ABI 上无法编译，故强制关闭 fp16 快速路径。
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
    set(GGML_HAS_FP16 0 CACHE INT "" FORCE)
endif()

operit_prepare_git_source(
    OPERIT_LLAMA_CPP_SOURCE_DIR
    OPERIT_LLAMA_CPP_BINARY_DIR
    llama_cpp
    "https://github.com/ggml-org/llama.cpp.git"
    "720d7fa4097f76e5d0eade5a92c1df87c1faf9d9"
    # ggml 上游已知缺陷（sgemm.cpp 自带 FIXME 注释）：fp16 向量化内存加载块
    # 只判断了 __ARM_NEON，未判断 __ARM_FEATURE_FP16_VECTOR_ARITHMETIC。
    # 后者是 armv8+ 专属；armeabi-v7a 上 vld1q_f16 / vld1_f16 / float16x8_t
    # 全部不存在，导致 :llama:buildCMakeDebug[armeabi-v7a] 编译失败。
    # 该补丁在下游仓库就地修复上游 bug，不改动被缓存的第三方源。
    PATCH_COMMAND "${CMAKE_COMMAND}" -E env bash -c "
        p="${OPERIT_LLAMA_CPP_SOURCE_DIR}/ggml/src/ggml-cpu/llamafile/sgemm.cpp";        if [ -f "$p" ]; then            sed -i 's/^#if !defined(_MSC_VER)$/#if !defined(_MSC_VER) \&\& defined(__ARM_FEATURE_FP16_VECTOR_ARITHMETIC)/' "$p";            sed -i '318i #endif // __ARM_FEATURE_FP16_VECTOR_ARITHMETIC' "$p";            sed -i '309a #if defined(__ARM_FEATURE_FP16_VECTOR_ARITHMETIC)' "$p";        fi
    "
)

add_subdirectory("${OPERIT_LLAMA_CPP_SOURCE_DIR}" "${OPERIT_LLAMA_CPP_BINARY_DIR}" EXCLUDE_FROM_ALL)

add_library(
    LlamaWrapper
    SHARED
    src/main/cpp/llama_jni_stub.cpp
)

target_compile_definitions(LlamaWrapper PRIVATE OPERIT_HAS_LLAMA_CPP=1)
target_include_directories(LlamaWrapper PRIVATE "${OPERIT_LLAMA_CPP_SOURCE_DIR}")
target_include_directories(LlamaWrapper PRIVATE "${OPERIT_LLAMA_CPP_SOURCE_DIR}/common")
target_include_directories(LlamaWrapper PRIVATE "${OPERIT_LLAMA_CPP_SOURCE_DIR}/include")
target_include_directories(LlamaWrapper PRIVATE "${OPERIT_LLAMA_CPP_SOURCE_DIR}/ggml/include")
target_include_directories(LlamaWrapper PRIVATE "${OPERIT_LLAMA_CPP_SOURCE_DIR}/vendor")
target_link_libraries(LlamaWrapper llama llama-common)

target_link_libraries(
    LlamaWrapper
    android
    log
)

# 16KB page size support (Android 15+ requirement)
target_link_options(LlamaWrapper PRIVATE "-Wl,-z,max-page-size=16384")
