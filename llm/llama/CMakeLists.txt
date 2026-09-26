cmake_minimum_required(VERSION 3.22.1)

project("llama_jni")

include("${CMAKE_CURRENT_LIST_DIR}/../../cmake/operit_git_source.cmake")

set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)

if(CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
    # ggml 上游已知缺陷：ggml-cpu/llamafile/sgemm.cpp 的 fp16 向量化内存加载块
    # 只判断了 __ARM_NEON，未判断 __ARM_FEATURE_FP16_VECTOR_ARITHMETIC（armv8+ 专属）。
    # armeabi-v7a 上 vld1q_f16 / vld1_f16 / float16x8_t 全部不存在，编译直接失败。
    # 该脚本在 configure 阶段就地补上正确的门控，幂等可重跑。
    set(OPERIT_LLAMA_PATCH_COMMAND
        "${CMAKE_COMMAND}"
        -E
        env
        bash
        "${CMAKE_CURRENT_LIST_DIR}/../../cmake/patch_ggml_armv7_fp16.sh"
    )
else()
    set(OPERIT_LLAMA_PATCH_COMMAND "${CMAKE_COMMAND}" -E true)
endif()

operit_prepare_git_source(
    OPERIT_LLAMA_CPP_SOURCE_DIR
    OPERIT_LLAMA_CPP_BINARY_DIR
    llama_cpp
    "https://github.com/ggml-org/llama.cpp.git"
    "720d7fa4097f76e5d0eade5a92c1df87c1faf9d9"
    PATCH_COMMAND "${CMAKE_COMMAND}" -E env bash "${CMAKE_CURRENT_LIST_DIR}/../../cmake/patch_ggml_armv7_fp16.sh" "${OPERIT_LLAMA_CPP_SOURCE_DIR}/ggml/src/ggml-cpu/llamafile/sgemm.cpp"
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
