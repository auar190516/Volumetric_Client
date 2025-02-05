#include <jni.h>
#include <cinttypes>
#include <sstream>
#include <vector>

#include "draco/compression/decode.h"
#include "draco/io/ply_encoder.h"
#include "draco/io/parser_utils.h"
#include "draco/core/cycle_timer.h"

extern "C"
JNIEXPORT jbyteArray
Java_com_example_volumetric_DracoDecoder_decodeDraco(JNIEnv *env, jobject /* this */,
jbyteArray encodedData) {
// 获取 Java 字节数组的长度
jsize length = env->GetArrayLength(encodedData);
// 获取 Java 字节数组的元素指针
jbyte *byteArray = env->GetByteArrayElements(encodedData, JNI_FALSE);
// 将 Java 字节数组转换为 C++ 的 std::vector<uint8_t>
std::vector<uint8_t> inputData(byteArray, byteArray + length);

// 创建 Draco 解码器
draco::Decoder decoder;
// 创建解码缓冲区
draco::DecoderBuffer buffer;
buffer.Init(reinterpret_cast<const char*>(inputData.data()), inputData.size());

// 尝试解码为点云
std::unique_ptr<draco::PointCloud> pc;
auto statusor = decoder.DecodePointCloudFromBuffer(&buffer);
if (!statusor.ok()) {
// 解码失败，释放 Java 字节数组资源并返回空指针
env->ReleaseByteArrayElements(encodedData, byteArray, JNI_ABORT);
return nullptr;
}
pc = std::move(statusor).value();

if (pc == nullptr) {
// 若解码后的点云为空，释放 Java 字节数组资源并返回空指针
env->ReleaseByteArrayElements(encodedData, byteArray, JNI_ABORT);
return nullptr;
}

// 创建 PLY 编码器
draco::PlyEncoder ply_encoder;
draco::EncoderBuffer buffer_out;
// 使用 EncodeToBuffer 将点云数据编码为 PLY 格式到 EncoderBuffer
if (!ply_encoder.EncodeToBuffer(*pc, &buffer_out)) {
// 若编码失败，释放资源并返回空指针
env->ReleaseByteArrayElements(encodedData, byteArray, JNI_ABORT);
return nullptr;
}

// 获取编码后的数据
const char *encodedDataPtr = buffer_out.data();
size_t encodedDataSize = buffer_out.size();

// 创建 Java 字节数组，并将 C++ 数据复制到 Java 字节数组
jbyteArray result = env->NewByteArray(encodedDataSize);
env->SetByteArrayRegion(result, 0, encodedDataSize,
reinterpret_cast<const jbyte *>(encodedDataPtr));

// 释放 Java 字节数组资源
env->ReleaseByteArrayElements(encodedData, byteArray, JNI_ABORT);

return result;
}
