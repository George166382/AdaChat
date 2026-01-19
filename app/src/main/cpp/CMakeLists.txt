cmake_minimum_required(VERSION 3.18.1)
project(whisper)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

add_library(
        whisper
        SHARED

        whisper_jni.cpp
        whisper.cpp

        ggml.c
        ggml-alloc.c
        ggml-backend.c
        ggml-quants.c
)

find_library(
        log-lib
        log
)

target_link_libraries(
        whisper
        ${log-lib}
)
