#include <jni.h>
#include <android/log.h>
#include <string>
#include <memory>
#include <cstring>
#include <cstdlib>
#include <ctype.h>

extern "C" {
#include "include/fast_board.h"
#include "include/fast_bot.h"
#include "include/fast_evaluation.h"
#include "include/bitboard_utils.h"
}

#define LOG_TAG "CFlangJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Static initialization flag for bitboard tables
static bool g_bitboard_initialized = false;
static bool g_init_lock = false;

// Initialize bitboard tables once
void ensure_bitboard_init() {
    if (!g_bitboard_initialized && !g_init_lock) {
        g_init_lock = true;
        bb_init_attack_tables();
        g_bitboard_initialized = true;
        LOGI("Bitboard attack tables initialized");
    }
}

// Character to piece type mapping (same as main.c)
FastType char_to_piece_type(char c) {
    switch (tolower(c)) {
        case 'p': return FAST_PAWN;
        case 'h': return FAST_HORSE;
        case 'r': return FAST_ROOK;
        case 'f': return FAST_FLANGER;
        case 'u': return FAST_UNI;
        case 'k': return FAST_KING;
        case ' ': return FAST_NONE;
        default: return FAST_NONE;
    }
}

// Parse FBN2 format string into Board (same as main.c)
bool parse_fbn2(const char* fbn2, FastBoard* board) {
    fast_board_init(board);
    
    if (strlen(fbn2) < 1) return false;
    
    // First character determines who moves
    FastColor at_move = FAST_WHITE;
    switch (fbn2[0]) {
        case '+': at_move = FAST_WHITE; break;
        case '-': at_move = FAST_BLACK; break;
        default:
            LOGE("FBN2 must start with + or -");
            return false;
    }
    board->at_move = at_move;
    
    // Parse board string
    char board_string[ARRAY_SIZE * 2 + 1] = {0};
    int pos = 0;
    int digit_count = 0;
    int empty_squares = 0;
    
    for (int i = 1; i < (int)strlen(fbn2); i++) {
        char c = fbn2[i];
        
        if (isdigit(c)) {
            empty_squares = empty_squares * 10 + (c - '0');
            digit_count++;
            if (digit_count > 2) {
                LOGE("Invalid number in FBN2");
                return false;
            }
        } else {
            // Add empty squares if we had digits
            if (digit_count > 0) {
                for (int j = 0; j < empty_squares; j++) {
                    if (pos >= ARRAY_SIZE * 2) break;
                    board_string[pos++] = ' ';
                    board_string[pos++] = '+';
                }
                empty_squares = 0;
                digit_count = 0;
            }
            
            // Add piece
            if (isalpha(c) && pos < ARRAY_SIZE * 2) {
                board_string[pos++] = c;
                board_string[pos++] = '+';
            }
            
            // Handle frozen pieces (-)
            if (c == '-' && pos > 0) {
                board_string[pos - 1] = '-';
            }
        }
    }
    
    // Handle remaining digits at end
    if (digit_count > 0) {
        for (int j = 0; j < empty_squares; j++) {
            if (pos >= ARRAY_SIZE * 2) break;
            board_string[pos++] = ' ';
            board_string[pos++] = '+';
        }
    }
    
    // Fill remaining spaces
    while (pos < ARRAY_SIZE * 2) {
        board_string[pos++] = ' ';
        board_string[pos++] = '+';
    }
    
    // Convert board string to Board
    for (int i = 0; i < ARRAY_SIZE && i * 2 + 1 < (int)strlen(board_string); i++) {
        char piece_char = board_string[i * 2];
        char state_char = board_string[i * 2 + 1];
        
        FastType type = char_to_piece_type(piece_char);
        FastColor color = isupper(piece_char) ? FAST_WHITE : FAST_BLACK;
        FastFrozenState frozen = (state_char == '-') ? FAST_FROZEN : FAST_NORMAL;
        
        if (type != FAST_NONE) {
            FastPieceState state = MAKE_PIECE_STATE(type, color, frozen);
            fast_board_set_at(board, i, state);
            
            if (frozen == FAST_FROZEN) {
                fast_board_set_frozen_piece_index(board, color, i);
            }
        }
    }

    fast_board_rebuild_hash_cache(board);
    
    return true;
}

// Format move as string for return to Java
void format_move_string(FastMove move, char* buffer) {
    int from = move.from;
    int to = move.to;
    sprintf(buffer, "%c%d%c%d", 'a' + get_x(from), get_y(from) + 1, 'a' + get_x(to), get_y(to) + 1);
}

extern "C" JNIEXPORT jlong JNICALL
Java_de_tadris_flang_bot_NativeCFlangEngine_initBot(JNIEnv *env, jobject /* this */,
                                                   jint minDepth, jint maxDepth,
                                                   jint threads, jint ttSizeMB,
                                                   jboolean useLME, jint lmeMaxExtension,
                                                   jboolean onlyBest, jboolean useNnue) {
    ensure_bitboard_init();

    FastFlangBot* bot = new FastFlangBot();
    if (!bot) {
        LOGE("Failed to allocate memory for FastFlangBot");
        return 0;
    }

    fast_bot_init(bot, minDepth, maxDepth, threads, ttSizeMB, useLME, lmeMaxExtension, onlyBest, useNnue);

    LOGI("Initialized FastFlangBot: depth %d-%d, threads %d, TT %d MB, NNUE %s",
         minDepth, maxDepth, threads, ttSizeMB, useNnue ? "on" : "off");

    return reinterpret_cast<jlong>(bot);
}

extern "C" JNIEXPORT void JNICALL
Java_de_tadris_flang_bot_NativeCFlangEngine_destroyBot(JNIEnv *env, jobject /* this */, jlong botPtr) {
    FastFlangBot* bot = reinterpret_cast<FastFlangBot*>(botPtr);
    if (bot) {
        fast_bot_destroy(bot);
        delete bot;
        LOGI("Destroyed FastFlangBot");
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_de_tadris_flang_bot_NativeCFlangEngine_findBestMove(JNIEnv *env, jobject /* this */, 
                                                        jlong botPtr, jstring fbn2) {
    FastFlangBot* bot = reinterpret_cast<FastFlangBot*>(botPtr);
    if (!bot) {
        LOGE("Invalid bot pointer");
        return nullptr;
    }
    
    // Convert Java string to C string
    const char* fbn2_chars = env->GetStringUTFChars(fbn2, nullptr);
    if (!fbn2_chars) {
        LOGE("Failed to get FBN2 string");
        return nullptr;
    }
    
    // Parse FBN2 into board
    FastBoard board;
    if (!parse_fbn2(fbn2_chars, &board)) {
        LOGE("Failed to parse FBN2: %s", fbn2_chars);
        env->ReleaseStringUTFChars(fbn2, fbn2_chars);
        return nullptr;
    }
    
    env->ReleaseStringUTFChars(fbn2, fbn2_chars);
    
    // Find best move
    BotResult result = fast_bot_find_best_move(bot, &board, false);
    
    // Format move as string
    char move_str[8];
    format_move_string(result.best_move.move, move_str);
    
    // Find NativeBotResult class and constructor
    jclass resultClass = env->FindClass("de/tadris/flang/bot/NativeBotResult");
    if (!resultClass) {
        LOGE("Failed to find NativeBotResult class");
        if (result.all_evaluations) free(result.all_evaluations);
        return nullptr;
    }
    
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/String;DIJ[Ljava/lang/String;[D[I)V");
    if (!constructor) {
        LOGE("Failed to find NativeBotResult constructor");
        if (result.all_evaluations) free(result.all_evaluations);
        return nullptr;
    }
    
    // Create Java string for best move
    jstring jMoveStr = env->NewStringUTF(move_str);
    
    // Create arrays for all move evaluations
    jobjectArray jMoveArray = env->NewObjectArray(result.evaluation_count, env->FindClass("java/lang/String"), nullptr);
    jdoubleArray jEvalArray = env->NewDoubleArray(result.evaluation_count);
    jintArray jDepthArray = env->NewIntArray(result.evaluation_count);
    
    if (jMoveArray && jEvalArray && jDepthArray) {
        // Fill the arrays
        jdouble* evalElements = env->GetDoubleArrayElements(jEvalArray, nullptr);
        jint* depthElements = env->GetIntArrayElements(jDepthArray, nullptr);
        
        for (int i = 0; i < result.evaluation_count; i++) {
            // Format move string
            char all_move_str[8];
            format_move_string(result.all_evaluations[i].move, all_move_str);
            jstring jAllMoveStr = env->NewStringUTF(all_move_str);
            env->SetObjectArrayElement(jMoveArray, i, jAllMoveStr);
            env->DeleteLocalRef(jAllMoveStr);
            
            // Set evaluation and depth
            evalElements[i] = score_to_cp(result.all_evaluations[i].evaluation);
            depthElements[i] = result.all_evaluations[i].depth;
        }
        
        env->ReleaseDoubleArrayElements(jEvalArray, evalElements, 0);
        env->ReleaseIntArrayElements(jDepthArray, depthElements, 0);
    }
    
    // Create result object
    jobject jResult = env->NewObject(resultClass, constructor, 
                                   jMoveStr,
                                   score_to_cp(result.best_move.evaluation),
                                   result.best_move.depth,
                                   (jlong)result.total_evaluations,
                                   jMoveArray,
                                   jEvalArray,
                                   jDepthArray);
    
    // Cleanup
    if (result.all_evaluations) {
        free(result.all_evaluations);
    }
    
    return jResult;
}

extern "C" JNIEXPORT jobject JNICALL
Java_de_tadris_flang_bot_NativeCFlangEngine_findBestMoveIterative(JNIEnv *env, jobject /* this */, 
                                                                 jlong botPtr, jstring fbn2, jlong maxTimeMs) {
    FastFlangBot* bot = reinterpret_cast<FastFlangBot*>(botPtr);
    if (!bot) {
        LOGE("Invalid bot pointer");
        return nullptr;
    }
    
    // Convert Java string to C string
    const char* fbn2_chars = env->GetStringUTFChars(fbn2, nullptr);
    if (!fbn2_chars) {
        LOGE("Failed to get FBN2 string");
        return nullptr;
    }
    
    // Parse FBN2 into board
    FastBoard board;
    if (!parse_fbn2(fbn2_chars, &board)) {
        LOGE("Failed to parse FBN2: %s", fbn2_chars);
        env->ReleaseStringUTFChars(fbn2, fbn2_chars);
        return nullptr;
    }
    
    env->ReleaseStringUTFChars(fbn2, fbn2_chars);
    
    // Find best move with time limit
    BotResult result = fast_bot_find_best_move_iterative(bot, &board, false, maxTimeMs);
    
    // Format move as string
    char move_str[8];
    format_move_string(result.best_move.move, move_str);
    
    // Find NativeBotResult class and constructor
    jclass resultClass = env->FindClass("de/tadris/flang/bot/NativeBotResult");
    if (!resultClass) {
        LOGE("Failed to find NativeBotResult class");
        if (result.all_evaluations) free(result.all_evaluations);
        return nullptr;
    }
    
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/String;DIJ[Ljava/lang/String;[D[I)V");
    if (!constructor) {
        LOGE("Failed to find NativeBotResult constructor");
        if (result.all_evaluations) free(result.all_evaluations);
        return nullptr;
    }
    
    // Create Java string for best move
    jstring jMoveStr = env->NewStringUTF(move_str);
    
    // Create arrays for all move evaluations
    jobjectArray jMoveArray = env->NewObjectArray(result.evaluation_count, env->FindClass("java/lang/String"), nullptr);
    jdoubleArray jEvalArray = env->NewDoubleArray(result.evaluation_count);
    jintArray jDepthArray = env->NewIntArray(result.evaluation_count);
    
    if (jMoveArray && jEvalArray && jDepthArray) {
        // Fill the arrays
        jdouble* evalElements = env->GetDoubleArrayElements(jEvalArray, nullptr);
        jint* depthElements = env->GetIntArrayElements(jDepthArray, nullptr);
        
        for (int i = 0; i < result.evaluation_count; i++) {
            // Format move string
            char all_move_str[8];
            format_move_string(result.all_evaluations[i].move, all_move_str);
            jstring jAllMoveStr = env->NewStringUTF(all_move_str);
            env->SetObjectArrayElement(jMoveArray, i, jAllMoveStr);
            env->DeleteLocalRef(jAllMoveStr);
            
            // Set evaluation and depth
            evalElements[i] = score_to_cp(result.all_evaluations[i].evaluation);
            depthElements[i] = result.all_evaluations[i].depth;
        }
        
        env->ReleaseDoubleArrayElements(jEvalArray, evalElements, 0);
        env->ReleaseIntArrayElements(jDepthArray, depthElements, 0);
    }
    
    // Create result object
    jobject jResult = env->NewObject(resultClass, constructor, 
                                   jMoveStr,
                                   score_to_cp(result.best_move.evaluation),
                                   result.best_move.depth,
                                   (jlong)result.total_evaluations,
                                   jMoveArray,
                                   jEvalArray,
                                   jDepthArray);
    
    // Cleanup
    if (result.all_evaluations) {
        free(result.all_evaluations);
    }
    
    return jResult;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_de_tadris_flang_bot_NativeCFlangEngine_evaluatePosition(JNIEnv *env, jobject /* this */, jstring fbn2) {
    ensure_bitboard_init();
    
    // Convert Java string to C string
    const char* fbn2_chars = env->GetStringUTFChars(fbn2, nullptr);
    if (!fbn2_chars) {
        LOGE("Failed to get FBN2 string");
        return 0.0;
    }
    
    // Parse FBN2 into board
    FastBoard board;
    if (!parse_fbn2(fbn2_chars, &board)) {
        LOGE("Failed to parse FBN2: %s", fbn2_chars);
        env->ReleaseStringUTFChars(fbn2, fbn2_chars);
        return 0.0;
    }
    
    env->ReleaseStringUTFChars(fbn2, fbn2_chars);
    
    // Evaluate position
    FastBoardEvaluation evaluator;
    fast_evaluation_init(&evaluator, &board);
    return score_to_cp(fast_evaluation_evaluate(&evaluator));
}