#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <errno.h>
#include <sys/time.h>
#include "fast_board.h"
#include "fast_bot.h"
#include "fast_evaluation.h"
#include "evaluation.h"

// Global variable to store side to move for sorting
static FastColor g_side_to_move_for_sorting;

// Comparison function for sorting move evaluations (descending for white, ascending for black)
int compare_move_evaluations(const void* a, const void* b) {
    const MoveEvaluation* eval_a = (const MoveEvaluation*)a;
    const MoveEvaluation* eval_b = (const MoveEvaluation*)b;
    
    int diff = (int)eval_a->evaluation - (int)eval_b->evaluation;

    if (g_side_to_move_for_sorting == FAST_WHITE) {
        return (diff < 0) ? 1 : ((diff > 0) ? -1 : 0);
    } else {
        return (diff < 0) ? -1 : ((diff > 0) ? 1 : 0);
    }
}

// Forward declarations
int run_socket_server(int threads, int ttsize_mb, bool use_lme, int lme_max_extension, bool only_best_move, bool use_nnue, int port);

// Character to piece type mapping
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

// Parse FBN2 format string into Board
bool parse_fbn2(const char* fbn2, FastBoard* board) {
    fast_board_init(board);
    
    if (strlen(fbn2) < 1) return false;
    
    // First character determines who moves
    FastColor at_move = FAST_WHITE;
    switch (fbn2[0]) {
        case '+': at_move = FAST_WHITE; break;
        case '-': at_move = FAST_BLACK; break;
        default:
            printf("Error: FBN2 must start with + or -\n");
            return false;
    }
    board->at_move = at_move;
    
    // Parse board string
    char board_string[ARRAY_SIZE * 2 + 1] = {0}; // Space for piece + state chars
    int pos = 0;
    int digit_count = 0;
    int empty_squares = 0;
    
    for (int i = 1; i < (int)strlen(fbn2); i++) {
        char c = fbn2[i];
        
        if (isdigit(c)) {
            empty_squares = empty_squares * 10 + (c - '0');
            digit_count++;
            if (digit_count > 2) {
                printf("Error: Invalid number in FBN2\n");
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
                board_string[pos++] = '+'; // Normal state by default
            }
            
            // Handle frozen pieces (-)
            if (c == '-' && pos > 0) {
                board_string[pos - 1] = '-'; // Change last piece to frozen
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
            
            // Track frozen pieces
            if (frozen == FAST_FROZEN) {
                fast_board_set_frozen_piece_index(board, color, i);
            }
        }
    }
    
    // Rebuild hash cache after loading the board
    fast_board_rebuild_hash_cache(board);
    
    return true;
}

// Function to set up an initial board position (simplified)
void setup_initial_position(FastBoard* board) {
    fast_board_init(board);
    
    // Set up a simple test position
    // White pieces on rank 1 (y=0)
    fast_board_set_at(board, index_of(2, 0), MAKE_PIECE_STATE(FAST_PAWN, FAST_WHITE, FAST_NORMAL));
    fast_board_set_at(board, index_of(3, 0), MAKE_PIECE_STATE(FAST_ROOK, FAST_WHITE, FAST_NORMAL));
    fast_board_set_at(board, index_of(4, 0), MAKE_PIECE_STATE(FAST_HORSE, FAST_WHITE, FAST_NORMAL));
    fast_board_set_at(board, index_of(5, 0), MAKE_PIECE_STATE(FAST_FLANGER, FAST_WHITE, FAST_NORMAL));
    fast_board_set_at(board, index_of(6, 0), MAKE_PIECE_STATE(FAST_UNI, FAST_WHITE, FAST_NORMAL));
    fast_board_set_at(board, index_of(7, 0), MAKE_PIECE_STATE(FAST_KING, FAST_WHITE, FAST_NORMAL));

    // White pawns on rank 2 (y=1)
    for (int x = 2; x < BOARD_SIZE; x++) {
        fast_board_set_at(board, index_of(x, 1), MAKE_PIECE_STATE(FAST_PAWN, FAST_WHITE, FAST_NORMAL));
    }
    
    // Black pieces on rank 8 (y=7)
    fast_board_set_at(board, index_of(0, 7), MAKE_PIECE_STATE(FAST_KING, FAST_BLACK, FAST_NORMAL));
    fast_board_set_at(board, index_of(1, 7), MAKE_PIECE_STATE(FAST_UNI, FAST_BLACK, FAST_NORMAL));
    fast_board_set_at(board, index_of(2, 7), MAKE_PIECE_STATE(FAST_FLANGER, FAST_BLACK, FAST_NORMAL));
    fast_board_set_at(board, index_of(3, 7), MAKE_PIECE_STATE(FAST_HORSE, FAST_BLACK, FAST_NORMAL));
    fast_board_set_at(board, index_of(4, 7), MAKE_PIECE_STATE(FAST_ROOK, FAST_BLACK, FAST_NORMAL));
    fast_board_set_at(board, index_of(5, 7), MAKE_PIECE_STATE(FAST_PAWN, FAST_BLACK, FAST_NORMAL));

    // Black pawns on rank 7 (y=6)
    for (int x = 0; x < 6; x++) {
        fast_board_set_at(board, index_of(x, 6), MAKE_PIECE_STATE(FAST_PAWN, FAST_BLACK, FAST_NORMAL));
    }
    
    // Rebuild hash cache after setting up the initial position
    fast_board_rebuild_hash_cache(board);
}

// Function to print the board
void print_board(const FastBoard* board) {
    printf("  a b c d e f g h\n");
    for (int y = 7; y >= 0; y--) {
        printf("%d ", y + 1);
        for (int x = 0; x < BOARD_SIZE; x++) {
            FastPieceState piece = fast_board_get_at_xy(board, x, y);
            FastType type = piece.type;
            FastColor color = piece.color;
            
            char c = ' ';
            switch (type) {
                case FAST_PAWN: c = 'P'; break;
                case FAST_HORSE: c = 'H'; break;
                case FAST_ROOK: c = 'R'; break;
                case FAST_FLANGER: c = 'F'; break;
                case FAST_UNI: c = 'U'; break;
                case FAST_KING: c = 'K'; break;
                default: c = '.'; break;
            }
            
            if (type != FAST_NONE && color == FAST_BLACK) {
                c = c + ('a' - 'A'); // Make lowercase for black pieces
            }
            
            printf("%c ", c);
        }
        printf("\n");
    }
    printf("\nTo move: %s\n", board->at_move ? "White" : "Black");
}

// Function to print move in algebraic notation (simplified)
void print_move(FastMove move) {
    int from = move.from;
    int to = move.to;
    char from_square[3] = {'a' + get_x(from), '1' + get_y(from), '\0'};
    char to_square[3] = {'a' + get_x(to), '1' + get_y(to), '\0'};
    printf("%s%s", from_square, to_square);
}

// Function to format move as string
void format_move_string(FastMove move, char* buffer) {
    int from = move.from;
    int to = move.to;
    sprintf(buffer, "%c%d%c%d", 'a' + get_x(from), get_y(from) + 1, 'a' + get_x(to), get_y(to) + 1);
}

// Socket server mode
int run_socket_server(int threads, int ttsize_mb, bool use_lme, int lme_max_extension, bool only_best_move, bool use_nnue, int port) {
    printf("Starting socket server on port %d...\n", port);

    // Initialize precomputed bitboard tables
    bb_init_attack_tables();

    // Initialize bot once for persistent TT
    FastFlangBot bot;
    fast_bot_init(&bot, 1, 20, threads, ttsize_mb, use_lme, lme_max_extension, only_best_move, use_nnue);
    
    // Create socket
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd == -1) {
        perror("socket creation failed");
        return 1;
    }
    
    // Enable SO_REUSEADDR
    int opt = 1;
    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
        perror("setsockopt failed");
        close(server_fd);
        return 1;
    }
    
    // Setup server address
    struct sockaddr_in server_addr;
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(port);
    
    // Bind socket
    if (bind(server_fd, (struct sockaddr*)&server_addr, sizeof(server_addr)) < 0) {
        perror("bind failed");
        close(server_fd);
        return 1;
    }
    
    // Listen for connections
    if (listen(server_fd, 5) < 0) {
        perror("listen failed");
        close(server_fd);
        return 1;
    }
    
    printf("Socket server listening on port %d\n", port);
    
    // Accept connections and handle requests
    while (1) {
        struct sockaddr_in client_addr;
        socklen_t client_len = sizeof(client_addr);
        int client_fd = accept(server_fd, (struct sockaddr*)&client_addr, &client_len);
        
        if (client_fd < 0) {
            perror("accept failed");
            continue;
        }
        
        printf("Client connected\n");
        
        // Set socket timeout (30 seconds)
        struct timeval timeout;
        timeout.tv_sec = 30;
        timeout.tv_usec = 0;
        if (setsockopt(client_fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)) < 0) {
            perror("setsockopt timeout failed");
        }
        
        // Handle client requests
        char buffer[4096];
        while (1) {
            ssize_t bytes_read = recv(client_fd, buffer, sizeof(buffer) - 1, 0);
            if (bytes_read <= 0) {
                if (bytes_read == 0) {
                    printf("Client disconnected cleanly\n");
                } else if (errno == EAGAIN || errno == EWOULDBLOCK) {
                    printf("Socket timeout - client appears disconnected\n");
                } else {
                    perror("recv error");
                }
                break; // Client disconnected, timeout, or error
            }
            
            buffer[bytes_read] = '\0';
            printf("Received: %s\n", buffer);
            
            // Parse and handle request
            if (strncmp(buffer, "DISCONNECT", 10) == 0) {
                printf("Disconnect command received, closing client connection\n");
                const char* ack_response = "DISCONNECTED\n";
                send(client_fd, ack_response, strlen(ack_response), 0);
                break; // Exit client loop, keep server running
            } else if (strncmp(buffer, "CLOSE", 5) == 0) {
                printf("Close command received, shutting down server\n");
                close(client_fd);
                close(server_fd);
                fast_bot_destroy(&bot);
                return 0;
            } else if (strncmp(buffer, "SEARCH ", 7) == 0) {
                // Parse: SEARCH <fbn2> <min_depth> <max_depth> <max_time_ms>
                char fbn2_str[512];
                int min_depth, max_depth;
                long max_time_ms;
                
                if (sscanf(buffer + 7, "%511s %d %d %ld", fbn2_str, &min_depth, &max_depth, &max_time_ms) == 4) {
                    // Parse FBN2 board
                    FastBoard board;
                    if (parse_fbn2(fbn2_str, &board)) {
                        // Update bot depths for this request
                        bot.min_depth = min_depth;
                        bot.max_depth = max_depth;
                        
                        // Perform search
                        BotResult result;
                        if (max_time_ms <= 0) {
                            result = fast_bot_find_best_move(&bot, &board, false);
                        } else {
                            result = fast_bot_find_best_move_iterative(&bot, &board, false, max_time_ms);
                        }
                        
                        // Format response
                        char response[8192];
                        char best_move_str[8];
                        format_move_string(result.best_move.move, best_move_str);
                        
                        int response_len = snprintf(response, sizeof(response),
                            "BEST %s %.6f %d\nMOVES %d\n",
                            best_move_str, score_to_cp(result.best_move.evaluation), result.best_move.depth, result.evaluation_count);

                        // Add all move evaluations
                        for (int i = 0; i < result.evaluation_count && (size_t)response_len < sizeof(response) - 100; i++) {
                            char move_str[8];
                            format_move_string(result.all_evaluations[i].move, move_str);
                            response_len += snprintf(response + response_len, sizeof(response) - response_len,
                                "MOVE %s %.6f %d\n", move_str,
                                score_to_cp(result.all_evaluations[i].evaluation), result.all_evaluations[i].depth);
                        }
                        
                        response_len += snprintf(response + response_len, sizeof(response) - response_len,
                            "EVALUATIONS %ld\n", result.total_evaluations);
                        
                        // Send response
                        send(client_fd, response, response_len, 0);
                        
                        // Free result
                        if (result.all_evaluations) {
                            free(result.all_evaluations);
                        }
                    } else {
                        const char* error_response = "ERROR Invalid FBN2 format\n";
                        send(client_fd, error_response, strlen(error_response), 0);
                    }
                } else {
                    const char* error_response = "ERROR Invalid SEARCH command format\n";
                    send(client_fd, error_response, strlen(error_response), 0);
                }
            } else {
                const char* error_response = "ERROR Unknown command\n";
                send(client_fd, error_response, strlen(error_response), 0);
            }
        }
        
        printf("Client disconnected\n");
        close(client_fd);
    }
    
    close(server_fd);
    fast_bot_destroy(&bot);
    return 0;
}

int main(int argc, char* argv[]) {
    // Default values
    int min_depth = 1;
    int max_depth = 6;
    long max_time_ms = -1; // -1 means endless
    int threads = 1;
    int ttsize_mb = 64;
    bool evaluation_only = false;
    bool machine_readable = false;
    bool use_lme = false;
    int lme_max_extension = 1;
    bool socket_mode = false;
    bool batch_mode = false;
    bool only_best_move = false;  // Default: search all moves
#ifdef USE_NNUE
    bool use_nnue = true;
#else
    bool use_nnue = false;
#endif
    int socket_port = 8080;
    const char* fbn2 = NULL;
    const char* batch_file = NULL;

    if (argc < 2) {
        printf("Usage: %s <fbn2_string> [options]\n", argv[0]);
        printf("       %s --socket [options]    (socket server mode)\n", argv[0]);
        printf("       %s --batch-eval <file>   (batch evaluation from file)\n", argv[0]);
        printf("Example: %s \"+RHFUKFHR8pppp1p1p16pppp1P1P8rhfukfhr\"\n", argv[0]);
        printf("        %s \"+RHFUKFHR8pppp1p1p16pppp1P1P8rhfukfhr\" --max-depth 8\n", argv[0]);
        printf("        %s \"+RHFUKFHR8pppp1p1p16pppp1P1P8rhfukfhr\" --evaluation-only\n", argv[0]);
        printf("        %s --socket --port 9090\n", argv[0]);
        printf("        %s --batch-eval positions.txt\n", argv[0]);
        printf("\nOptions:\n");
        printf("  --min-depth <n>    : Minimum search depth (default: 1, range: 1-20)\n");
        printf("  --max-depth <n>    : Maximum search depth (default: 6, range: 1-20)\n");
        printf("  --max-time <ms>    : Maximum search time in milliseconds (-1 for endless, default: -1)\n");
        printf("  --threads <n>      : Number of threads to use (default: 1, range: 1-64)\n");
        printf("  --ttsize <mb>      : Transposition table size in MB (default: 64, range: 1-65536)\n");
        printf("  --evaluation-only  : Only evaluate position (no search)\n");
        printf("  --machine-readable : Output in machine-readable format\n");
        printf("  --only-best        : Only search for best move (skip all move evaluations)\n");
        printf("  --use-lme          : Enable Late Move Extensions\n");
        printf("  --hce              : Use hand-crafted evaluation instead of NNUE (NNUE builds only)\n");
        printf("  --lme-max-ext <n>  : Maximum LME extension depth (default: 1, range: 1-10)\n");
        printf("  --socket           : Run in socket server mode\n");
        printf("  --port <n>         : Socket server port (default: 8080, range: 1024-65535)\n");
        printf("  --batch-eval <file>: Batch evaluation mode (reads FBN from file, one per line)\n");
        printf("\nFBN2 format:\n");
        printf("  +/- : White/Black to move\n");
        printf("  Letters: Pieces (P=pawn, H=horse, R=rook, F=flanger, U=uni, K=king)\n");
        printf("  Numbers: Empty squares\n");
        printf("  - after piece: Frozen piece\n");
        return 1;
    }

    // Check for special modes first
    if (argc >= 2 && strcmp(argv[1], "--socket") == 0) {
        socket_mode = true;
        fbn2 = NULL; // No FBN2 needed in socket mode
    } else if (argc >= 2 && strcmp(argv[1], "--batch-eval") == 0) {
        batch_mode = true;
        fbn2 = NULL; // No FBN2 needed in batch mode
        if (argc >= 3) {
            batch_file = argv[2];
        } else {
            printf("Error: --batch-eval requires a filename\n");
            return 1;
        }
    } else {
        fbn2 = argv[1];
    }
    
    // Parse optional arguments
    // Start at position 3 if batch_mode (skip filename), otherwise position 2
    int start_pos = (batch_mode || socket_mode) ? 3 : 2;
    for (int i = start_pos; i < argc; i++) {
        if (strcmp(argv[i], "--min-depth") == 0 && i + 1 < argc) {
            min_depth = atoi(argv[i + 1]);
            i++;
        } else if (strcmp(argv[i], "--max-depth") == 0 && i + 1 < argc) {
            max_depth = atoi(argv[i + 1]);
            i++;
        } else if (strcmp(argv[i], "--max-time") == 0 && i + 1 < argc) {
            max_time_ms = atol(argv[i + 1]);
            i++;
        } else if (strcmp(argv[i], "--threads") == 0 && i + 1 < argc) {
            threads = atoi(argv[i + 1]);
            i++;
        } else if (strcmp(argv[i], "--ttsize") == 0 && i + 1 < argc) {
            ttsize_mb = atoi(argv[i + 1]);
            i++;
        } else if (strcmp(argv[i], "--evaluation-only") == 0) {
            evaluation_only = true;
        } else if (strcmp(argv[i], "--machine-readable") == 0) {
            machine_readable = true;
        } else if (strcmp(argv[i], "--only-best") == 0) {
            only_best_move = true;
        } else if (strcmp(argv[i], "--use-lme") == 0) {
            use_lme = true;
        } else if (strcmp(argv[i], "--hce") == 0) {
            use_nnue = false;
        } else if (strcmp(argv[i], "--lme-max-ext") == 0 && i + 1 < argc) {
            lme_max_extension = atoi(argv[i + 1]);
            i++;
        } else if (strcmp(argv[i], "--socket") == 0) {
            socket_mode = true;
        } else if (strcmp(argv[i], "--port") == 0 && i + 1 < argc) {
            socket_port = atoi(argv[i + 1]);
            i++;
        } else if (strcmp(argv[i], "--batch-eval") == 0 && i + 1 < argc) {
            batch_mode = true;
            batch_file = argv[i + 1];
            i++;
        } else {
            printf("Unknown option: %s\n", argv[i]);
            return 1;
        }
    }
    
    // Validate parameters
    if (min_depth < 1 || min_depth > 20) {
        printf("Error: Min depth must be between 1 and 20\n");
        return 1;
    }
    if (max_depth < 1 || max_depth > 20) {
        printf("Error: Max depth must be between 1 and 20\n");
        return 1;
    }
    if (min_depth > max_depth) {
        printf("Error: Min depth cannot be greater than max depth\n");
        return 1;
    }
    if (threads < 1 || threads > 64) {
        printf("Error: Threads must be between 1 and 64\n");
        return 1;
    }
    if (ttsize_mb < 1 || ttsize_mb > 65536) {
        printf("Error: TT size must be between 1 and 65536 MB\n");
        return 1;
    }
    if (lme_max_extension < 1 || lme_max_extension > 10) {
        printf("Error: LME max extension must be between 1 and 10\n");
        return 1;
    }
    if (socket_port < 1024 || socket_port > 65535) {
        printf("Error: Port must be between 1024 and 65535\n");
        return 1;
    }
    
    // Initialize precomputed bitboard tables
    bb_init_attack_tables();
    
    if (!machine_readable) {
        printf("CFlang - C Implementation of FastFlangBot\n");
        printf("=========================================\n");
        printf("Configuration:\n");
        printf("  Min depth: %d\n", min_depth);
        printf("  Max depth: %d\n", max_depth);
        printf("  Max time: %ld ms (%s)\n", max_time_ms, max_time_ms == -1 ? "endless" : "limited");
        printf("  Threads: %d\n", threads);
        printf("  TT size: %d MB\n", ttsize_mb);
        printf("  LME: %s\n", use_lme ? "enabled" : "disabled");
        if (use_lme) {
            printf("  LME max ext: %d\n", lme_max_extension);
        }
        if (socket_mode) {
            printf("  Socket mode: enabled\n");
            printf("  Port: %d\n", socket_port);
        }
        printf("  Mode: %s\n", socket_mode ? "socket server" : (evaluation_only ? "evaluation only" : "search"));
        printf("\n");
    }
    
    if (socket_mode) {
        return run_socket_server(threads, ttsize_mb, use_lme, lme_max_extension, only_best_move, use_nnue, socket_port);
    }

    // Batch evaluation mode - read FBNs from file
    if (batch_mode) {
        FastBoardEvaluation evaluator;
        FastBoard board;
        char line[1024];

        // Open input file
        FILE* input_file = fopen(batch_file, "r");
        if (!input_file) {
            fprintf(stderr, "Error: Cannot open file '%s': %s\n", batch_file, strerror(errno));
            return 1;
        }

        // Initialize evaluator once
        fast_board_init(&board);
        fast_evaluation_init(&evaluator, &board);

        // Read FBNs from file, one per line
        while (fgets(line, sizeof(line), input_file) != NULL) {
            // Remove trailing newline
            size_t len = strlen(line);
            if (len > 0 && line[len-1] == '\n') {
                line[len-1] = '\0';
            }

            // Skip empty lines
            if (strlen(line) == 0) {
                continue;
            }

            // Parse and evaluate
            if (parse_fbn2(line, &board)) {
                evaluator.board = &board;
                evaluator.move_generator.board = &board;
                int16_t eval = fast_evaluation_evaluate(&evaluator);
                printf("%.6f\n", score_to_cp(eval));
                fflush(stdout);  // Important: flush after each result
            } else {
                fprintf(stderr, "ERROR: Failed to parse FBN: %s\n", line);
                printf("0\n");  // Output 0 for invalid positions
                fflush(stdout);
            }
        }

        fclose(input_file);
        return 0;
    }

    // Parse FBN2 string into board
    FastBoard board;
    if (!parse_fbn2(fbn2, &board)) {
        printf("Error: Failed to parse FBN2 string: %s\n", fbn2);
        return 1;
    }
    
    if (!machine_readable) {
        printf("Position from FBN2: %s\n", fbn2);
        print_board(&board);
        
        // Debug: Check what piece is at g1 (index 6*8 + 0 = 6)
        FastPieceState piece_g1 = fast_board_get_at_xy(&board, 6, 0);  // g1
        FastType type_g1 = piece_g1.type;
        FastColor color_g1 = piece_g1.color;
        printf("Debug: Piece at g1 - Type: %d, Color: %d\n", (int)type_g1, (int)color_g1);
        
        printf("\n");
    }
    
    if (evaluation_only) {
        // Just evaluate the current position
        if (!machine_readable) {
            printf("Evaluating current position...\n");
        }
        
        Evaluator evaluator;
        evaluator_init(&evaluator, &board);
        int16_t eval = evaluator_evaluate(&evaluator, &board);

        if (machine_readable) {
            printf("EVAL %.6f\n", score_to_cp(eval));
        } else {
            printf("\nPosition evaluation: %.2f\n", score_to_cp(eval));
        }
    } else {
        // Initialize bot and search
        FastFlangBot bot;

        fast_bot_init(&bot, min_depth, max_depth, threads, ttsize_mb, use_lme, lme_max_extension, only_best_move, use_nnue);

        if (!machine_readable) {
            printf("Searching (min: %d, max: %d, threads: %d)...\n", min_depth, max_depth, threads);
        }
        
        BotResult result;
        bool print_progress = !machine_readable;
        if (max_time_ms == -1) {
            // No time limit - use regular search
            result = fast_bot_find_best_move(&bot, &board, print_progress);
        } else {
            // Use time-limited search
            result = fast_bot_find_best_move_iterative(&bot, &board, print_progress, max_time_ms);
        }
        
        if (machine_readable) {
            printf("BEST ");
            print_move(result.best_move.move);
            printf(" %.6f %d\n", score_to_cp(result.best_move.evaluation), result.best_move.depth);

            // Output all move evaluations
            printf("MOVES %d\n", result.evaluation_count);
            for (int i = 0; i < result.evaluation_count; i++) {
                printf("MOVE ");
                print_move(result.all_evaluations[i].move);
                printf(" %.6f %d\n", score_to_cp(result.all_evaluations[i].evaluation), result.all_evaluations[i].depth);
            }
            printf("EVALUATIONS %ld\n", result.total_evaluations);
        } else {
            printf("\nBest move: ");
            print_move(result.best_move.move);
            printf(" (eval: %.2f, depth: %d)\n", score_to_cp(result.best_move.evaluation), result.best_move.depth);
            
            // Show all move evaluations
            if (result.evaluation_count > 0) {
                // Sort move evaluations by score (best moves first)
                g_side_to_move_for_sorting = board.at_move;
                qsort(result.all_evaluations, result.evaluation_count, sizeof(MoveEvaluation), compare_move_evaluations);
                
                printf("\nAll move evaluations (sorted by score):\n");
                for (int i = 0; i < result.evaluation_count; i++) {
                    printf("  ");
                    print_move(result.all_evaluations[i].move);
                    printf(" (eval: %.2f, depth: %d)\n", score_to_cp(result.all_evaluations[i].evaluation), result.all_evaluations[i].depth);
                }
                printf("Total evaluations: %ld\n", result.total_evaluations);
            }
        }
        
        // Cleanup
        if (result.all_evaluations) {
            free(result.all_evaluations);
        }
        fast_bot_destroy(&bot);
    }
    
    return 0;
}