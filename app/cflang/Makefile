# CFlang Makefile
CC = gcc
CFLAGS = -Wall -Wextra -O3 -std=c99 -march=native -flto -ffast-math -fno-math-errno -fno-trapping-math -funroll-loops
DEBUG_CFLAGS = -Wall -Wextra -O0 -g -std=c99 -DDEBUG
LIBS = -lm

# Directories
SRC_DIR = ../app/src/main/cpp
INCLUDE_DIR = ../app/src/main/cpp/include
BUILD_DIR = build
TEST_DIR = tests

# Source files
SOURCES = $(SRC_DIR)/fast_board.c $(SRC_DIR)/fast_move_generator.c $(SRC_DIR)/fast_evaluation.c $(SRC_DIR)/evaluation.c $(SRC_DIR)/fast_bot.c $(SRC_DIR)/fast_zobrist.c $(SRC_DIR)/fast_transposition.c $(SRC_DIR)/bitboard_utils.c $(SRC_DIR)/nnue.c
MAIN_SOURCE = $(SRC_DIR)/main.c
TEST_NNUE_SOURCE = $(SRC_DIR)/test_nnue.c
OBJECTS = $(SOURCES:$(SRC_DIR)/%.c=$(BUILD_DIR)/%.o)
OBJECTS_NNUE = $(SOURCES:$(SRC_DIR)/%.c=$(BUILD_DIR)/%.nnue.o)
MAIN_OBJECT = $(BUILD_DIR)/main.o
MAIN_OBJECT_NNUE = $(BUILD_DIR)/main.nnue.o
TEST_NNUE_OBJECT = $(BUILD_DIR)/test_nnue.o

# Targets
TARGET = cflang
TARGET_NNUE = cflang_nnue
DEBUG_TARGET = cflang_debug
DEBUG_TARGET_NNUE = cflang_nnue_debug
TEST_NNUE_TARGET = test_nnue

.PHONY: all clean debug test test-nnue install nnue nnue-debug

# Default target
all: $(TARGET)

# Release build (HCE)
$(TARGET): $(BUILD_DIR) $(OBJECTS) $(MAIN_OBJECT)
	$(CC) $(CFLAGS) $(OBJECTS) $(MAIN_OBJECT) -o $(TARGET) $(LIBS)
	@echo "Built $(TARGET) with HCE successfully!"

# Release build (NNUE)
nnue: $(TARGET_NNUE)

$(TARGET_NNUE): $(BUILD_DIR) $(OBJECTS_NNUE) $(MAIN_OBJECT_NNUE)
	$(CC) $(CFLAGS) -DUSE_NNUE $(OBJECTS_NNUE) $(MAIN_OBJECT_NNUE) -o $(TARGET_NNUE) $(LIBS)
	@echo "Built $(TARGET_NNUE) with NNUE successfully!"

# Debug build (HCE)
debug: CFLAGS = $(DEBUG_CFLAGS)
debug: $(DEBUG_TARGET)

$(DEBUG_TARGET): $(BUILD_DIR) $(OBJECTS) $(MAIN_OBJECT)
	$(CC) $(DEBUG_CFLAGS) $(OBJECTS) $(MAIN_OBJECT) -o $(DEBUG_TARGET) $(LIBS)
	@echo "Built $(DEBUG_TARGET) with HCE successfully!"

# Debug build (NNUE)
nnue-debug: $(DEBUG_TARGET_NNUE)

$(DEBUG_TARGET_NNUE): $(BUILD_DIR) $(OBJECTS_NNUE) $(MAIN_OBJECT_NNUE)
	$(CC) $(DEBUG_CFLAGS) -DUSE_NNUE $(OBJECTS_NNUE) $(MAIN_OBJECT_NNUE) -o $(DEBUG_TARGET_NNUE) $(LIBS)
	@echo "Built $(DEBUG_TARGET_NNUE) with NNUE successfully!"

# Create build directory
$(BUILD_DIR):
	mkdir -p $(BUILD_DIR)

# Compile source files (HCE)
$(BUILD_DIR)/%.o: $(SRC_DIR)/%.c
	$(CC) $(CFLAGS) -I$(INCLUDE_DIR) -c $< -o $@

# Compile source files (NNUE)
$(BUILD_DIR)/%.nnue.o: $(SRC_DIR)/%.c
	$(CC) $(CFLAGS) -DUSE_NNUE -I$(INCLUDE_DIR) -c $< -o $@

# Clean build artifacts
clean:
	rm -rf $(BUILD_DIR)
	rm -f $(TARGET) $(TARGET_NNUE) $(DEBUG_TARGET) $(DEBUG_TARGET_NNUE) $(TEST_NNUE_TARGET)
	@echo "Cleaned build artifacts"

# Run the program
run: $(TARGET)
	./$(TARGET)

# Run debug version
run-debug: debug
	./$(DEBUG_TARGET)

# Test NNUE integration
test-nnue: $(BUILD_DIR) $(OBJECTS) $(TEST_NNUE_OBJECT)
	$(CC) $(CFLAGS) $(OBJECTS) $(TEST_NNUE_OBJECT) -o $(TEST_NNUE_TARGET) $(LIBS)
	@echo "Built $(TEST_NNUE_TARGET) successfully!"
	./$(TEST_NNUE_TARGET)

# Install (copy to /usr/local/bin)
install: $(TARGET)
	sudo cp $(TARGET) /usr/local/bin/
	@echo "Installed $(TARGET) to /usr/local/bin/"

# Uninstall
uninstall:
	sudo rm -f /usr/local/bin/$(TARGET)
	@echo "Uninstalled $(TARGET)"

# Performance test
perf: $(TARGET)
	time ./$(TARGET)

# Memory check (requires valgrind)
memcheck: debug
	valgrind --leak-check=full --show-leak-kinds=all ./$(DEBUG_TARGET)

# Static analysis (requires cppcheck)
analyze:
	cppcheck --enable=all --inconclusive --std=c99 -I$(INCLUDE_DIR) $(SRC_DIR)/*.c

# Show help
help:
	@echo "CFlang Build System"
	@echo "==================="
	@echo ""
	@echo "Available targets:"
	@echo "  all          - Build release version with HCE (default)"
	@echo "  nnue         - Build release version with NNUE"
	@echo "  debug        - Build debug version with HCE"
	@echo "  nnue-debug   - Build debug version with NNUE"
	@echo "  clean        - Remove build artifacts"
	@echo "  run          - Build and run release version (HCE)"
	@echo "  run-debug    - Build and run debug version (HCE)"
	@echo "  test-nnue    - Build and run NNUE integration test"
	@echo "  install      - Install to /usr/local/bin"
	@echo "  uninstall    - Remove from /usr/local/bin"
	@echo "  perf         - Run performance test"
	@echo "  memcheck     - Run memory leak check (requires valgrind)"
	@echo "  analyze      - Run static analysis (requires cppcheck)"
	@echo "  help         - Show this help message"
	@echo ""
	@echo "Build options:"
	@echo "  CC         - C compiler (default: gcc)"
	@echo "  CFLAGS     - Compiler flags"
	@echo "  LIBS       - Libraries to link"
	@echo ""
	@echo "Examples:"
	@echo "  make                    # Build release version"
	@echo "  make debug              # Build debug version"
	@echo "  make CC=clang           # Build with clang"
	@echo "  make CFLAGS='-O2 -g'    # Custom compiler flags"