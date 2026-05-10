cmake_minimum_required(VERSION 3.22.1)

project("cflang")

# Set C standard
set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Set compilation flags for performance
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra -O3 -ffast-math -fno-math-errno -fno-trapping-math -funroll-loops")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -DNDEBUG")

# Add 16KB page size compatibility for Android 15+
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# Include directories
include_directories(include)

# Source files (excluding main.c which is only for standalone)
set(CFLANG_SOURCES
    fast_board.c
    fast_bot.c
    evaluation.c
    fast_evaluation.c
    fast_move_generator.c
    fast_transposition.c
    fast_zobrist.c
    nnue.c
    bitboard_utils.c
    jni_flang.cpp
)

# Create shared library for Android
add_library(cflang SHARED ${CFLANG_SOURCES})

# Enable NNUE evaluation
target_compile_definitions(cflang PRIVATE USE_NNUE)

# Link math library
target_link_libraries(cflang m)

# Find required Android libraries
find_library(log-lib log)

# Link Android log library for JNI logging
target_link_libraries(cflang ${log-lib})