cmake_minimum_required(VERSION 3.22.1)

project("nativemodel")

# 设置C++标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 设置编译选项
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -fPIC -std=c++17")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -fPIC")

# 查找必要的Android库
find_library(log-lib log)
find_library(android-lib android)

# 添加源文件
add_library(
    nativemodel
    SHARED
    native_inference.cpp
    native_tokenizer.cpp
)

# 链接库
target_link_libraries(
    nativemodel
    ${log-lib}
    ${android-lib}
) 