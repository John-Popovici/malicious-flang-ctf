package de.tadris.flang_lib.bot.fast

import de.tadris.flang_lib.Move

/**
 * Transposition table for storing and retrieving previously computed position evaluations.
 * Uses Zobrist hashing for position identification and replacement schemes for memory management.
 */
class TranspositionTable(private val sizeMB: Int = 64) {
    
    /**
     * Entry in the transposition table
     */
    data class TTEntry(
        var zobristHash: Long,
        var depth: Int,
        var value: Double,
        var nodeType: NodeType,
        var bestMove: Move? = null,
        var age: Int = 0 // For replacement scheme
    )
    
    /**
     * Type of node/bound stored in the table
     */
    enum class NodeType {
        EXACT,      // Exact value (PV node)
        LOWER_BOUND, // Beta cutoff (value >= beta)
        UPPER_BOUND  // Alpha cutoff (value <= alpha)
    }
    
    /**
     * Result of a table probe
     */
    data class TTResult(
        val hit: Boolean,
        val value: Double = 0.0,
        val bestMove: Move? = null,
        val nodeType: NodeType = NodeType.EXACT
    )
    
    // Calculate table size (number of entries)
    private val entrySize = 64 // Approximate size of TTEntry in bytes
    private val tableSize = (sizeMB * 1024 / entrySize * 1024)
    private val table = Array<TTEntry?>(tableSize) { null }
    
    // Age counter for replacement scheme
    private var currentAge = 0
    
    // Statistics
    var hits = 0L
        private set
    var misses = 0L
        private set
    var collisions = 0L
        private set
    
    /**
     * Store an entry in the transposition table
     */
    fun store(
        zobristHash: Long,
        depth: Int,
        value: Double,
        nodeType: NodeType,
        bestMove: Move? = null
    ) {
        if(tableSize == 0) return
        val index = (zobristHash and 0x7FFFFFFFFFFFFFFFL) % tableSize
        val existing = table[index.toInt()]
        
        // Replacement scheme: replace if
        // 1. Slot is empty
        // 2. Same position (hash match)
        // 3. New entry has greater depth
        // 4. Existing entry is from older search (age-based replacement)
        val shouldReplace = existing == null ||
                existing.zobristHash == zobristHash ||
                depth >= existing.depth ||
                (currentAge - existing.age) > 2
        
        if (shouldReplace) {
            if(existing == null){
                table[index.toInt()] = TTEntry(
                    zobristHash = zobristHash,
                    depth = depth,
                    value = value,
                    nodeType = nodeType,
                    bestMove = bestMove,
                    age = currentAge
                )
            }else{
                table[index.toInt()].apply {
                    this!!
                    this.zobristHash = zobristHash
                    this.depth = depth
                    this.value = value
                    this.nodeType = nodeType
                    this.bestMove = bestMove
                    this.age = currentAge
                }
            }

        }
    }
    
    /**
     * Probe the transposition table for a position
     */
    fun probe(zobristHash: Long, depth: Int, alpha: Double, beta: Double): TTResult? {
        if(tableSize == 0){
            return null
        }
        val index = (zobristHash and 0x7FFFFFFFFFFFFFFFL) % tableSize
        val entry = table[index.toInt()]
        
        // Check for hash collision or empty slot
        if (entry == null || entry.zobristHash != zobristHash) {
            misses++
            return null
        }
        
        // Found matching position
        hits++
        
        // Check if stored depth is sufficient
        if (entry.depth < depth) {
            // Insufficient depth for value, but we can still use the best move for ordering
            return TTResult(
                hit = false, // Cannot use the value!
                value = entry.value,
                bestMove = entry.bestMove,
                nodeType = entry.nodeType
            )
        }
        
        // Check if we can use the stored value based on node type
        val canUseValue = when (entry.nodeType) {
            NodeType.EXACT -> true
            NodeType.LOWER_BOUND -> entry.value >= beta
            NodeType.UPPER_BOUND -> entry.value <= alpha
        }
        
        return TTResult(
            hit = canUseValue,
            value = entry.value,
            bestMove = entry.bestMove,
            nodeType = entry.nodeType
        )
    }
    
    /**
     * Get the best move from the table if available
     */
    fun getBestMove(zobristHash: Long): Move? {
        val index = (zobristHash and 0x7FFFFFFFFFFFFFFFL) % tableSize
        val entry = table[index.toInt()]
        
        return if (entry != null && entry.zobristHash == zobristHash) {
            entry.bestMove
        } else {
            null
        }
    }
    
    /**
     * Increment age for new search (called at the start of each search)
     */
    fun newSearch() {
        currentAge++
        hits = 0L
        misses = 0L
        collisions = 0L
    }
    
    /**
     * Clear the entire table
     */
    fun clear() {
        for (i in table.indices) {
            table[i] = null
        }
        hits = 0L
        misses = 0L
        collisions = 0L
        currentAge = 0
    }
    
    /**
     * Get hit rate percentage
     */
    fun getHitRate(): Double {
        val total = hits + misses
        return if (total > 0) (hits.toDouble() / total.toDouble()) * 100.0 else 0.0
    }
    
    /**
     * Get table usage statistics
     */
    fun getUsageStats(): String {
        val million = 1_000_000L
        val used = table.count { it != null }
        val usagePercent = (used.toDouble() / tableSize.toDouble()) * 100.0
        return "TT Usage: ${used/million}M/${tableSize/million}M (${String.format("%.1f", usagePercent)}%), " +
                "Hit rate: ${String.format("%.1f", getHitRate())}%, " +
                "Hits: ${hits/1000}k, Misses: ${misses/1000}k"
    }
}