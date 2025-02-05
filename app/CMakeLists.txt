# CMake 最小版本要求
cmake_minimum_required(VERSION 3.10)

# 项目名称
project("draco_example")

# 设置 C++ 标准为 C++11
set(CMAKE_CXX_STANDARD 11)

# Note "/path/to/draco" must be changed to the path where you have cloned
# the Draco sources.

add_subdirectory(D:/Backup/Documents/Downloads/draco-1.5.7
        ${CMAKE_BINARY_DIR}/draco_build)
include_directories("${CMAKE_BINARY_DIR}" D:/Backup/Documents/Downloads/draco-1.5.7/src)

# 创建本地库，假设您的 JNI 代码在 native-lib.cpp 中
add_library(native-lib SHARED src/main/cpp/native-lib.cpp)

# 查找 Android 日志库
find_library(log-lib log)

# 将 Draco 库和日志库链接到本地库
target_link_libraries(native-lib
        draco_static
        ${log-lib})