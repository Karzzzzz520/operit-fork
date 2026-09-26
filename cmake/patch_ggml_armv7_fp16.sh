#!/usr/bin/env bash
# 为 armeabi-v7a 就地修复 ggml 的 fp16 向量化内存加载门控。
# 用法: patch_ggml_armv7_fp16.sh <sgemm.cpp 路径>
# 由 llm/llama/CMakeLists.txt 的 FetchContent PATCH_COMMAND 在 configure 阶段调用。
set -eu
p="${1:-}"
if [ -z "$p" ] || [ ! -f "$p" ]; then
    echo "patch_ggml_armv7_fp16.sh: 目标文件不存在: $p" >&2
    exit 1
fi
fp16_macro="__ARM_FEATURE_FP16_VECTOR_ARITHMETIC"
if grep -q "$fp16_macro && !defined(_MSC_VER)" "$p"; then
    echo "[patch_ggml_armv7_fp16] 已打过补丁，跳过"
    exit 0
fi
# 关键行唯一匹配：只有 fp16 加载块那处是 "#if !defined(_MSC_VER)"，
# 它后面紧跟上游自己留下的 FIXME 注释。
sed -i "s|#if !defined(_MSC_VER)|#if $fp16_macro \&\& !defined(_MSC_VER)|" "$p"
sed -i "/FIXME: this should check for __ARM_FEATURE_FP16_VECTOR_ARITHMETIC/a #if defined($fp16_macro)" "$p"
echo "[patch_ggml_armv7_fp16] 已修复 $p"
