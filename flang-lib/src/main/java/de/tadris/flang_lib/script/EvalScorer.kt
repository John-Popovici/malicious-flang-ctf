package de.tadris.flang_lib.script

import java.io.File
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Data class containing all evaluation metrics.
 */
data class EvalMetrics(
    val mae: Double,
    val rmse: Double,
    val medianError: Double,
    val p90Error: Double,
    val p95Error: Double,
    val signAccuracy: Double,
    val correlation: Double,
    val totalPositions: Int,
    val largestErrors: List<Triple<String, Double, Double>>
)

/**
 * Calculates evaluation metrics for the cflang engine against a labeled dataset.
 *
 * @param datasetFile Path to the dataset file (FBN|score format)
 * @param cflangPath Path to the cflang executable
 * @return EvalMetrics object with all calculated metrics, or null on error
 */
fun calculateMetrics(
    datasetFile: String = "doc/eval_dataset_4.txt",
    cflangPath: String = "./cflang/cflang"
): EvalMetrics? {
    val dataset = File(datasetFile)
        .readLines()
        .filter { it.isNotBlank() }
        .map { line ->
            val parts = line.split("|")
            val fbn = parts[0]
            val trueScore = parts[1].toDouble()
            fbn to trueScore
        }
        .filter { it.second.absoluteValue < 5000 } // dont use mate scores as evals cannot predict them

    if (dataset.isEmpty()) {
        println("ERROR: Dataset is empty. Run EvalDatasetGenerator first!")
        return null
    }

    // Create temporary file with all FBNs
    val tempFile = File.createTempFile("eval_batch_", ".txt")
    try {
        // Write all FBNs to temp file
        tempFile.bufferedWriter().use { writer ->
            dataset.forEach { (fbn, _) ->
                writer.write(fbn)
                writer.newLine()
            }
        }

        // Start cflang in batch mode with file input
        val process = ProcessBuilder(cflangPath, "--batch-eval", tempFile.absolutePath)
            .redirectErrorStream(true)
            .start()

        val reader = process.inputStream.bufferedReader()

        // Read all evaluations
        val evalScores = mutableListOf<Double>()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val score = line?.toDoubleOrNull()
            if (score != null) {
                evalScores.add(score)
            }
        }

        process.waitFor()

        if (evalScores.size != dataset.size) {
            println("ERROR: Got ${evalScores.size} evaluations but expected ${dataset.size}")
            println("Temp file: ${tempFile.absolutePath}")
            println("First few lines of temp file:")
            tempFile.readLines().take(5).forEach { println("  $it") }
            return null
        }

        // Calculate metrics
        var totalAbsError = 0.0
        var totalSquaredError = 0.0
        var signAgreements = 0
        var sumTrueScores = 0.0
        var sumEvalScores = 0.0
        var sumTrueSquared = 0.0
        var sumEvalSquared = 0.0
        var sumProduct = 0.0

        val errors = mutableListOf<Double>()
        val largestErrors = mutableListOf<Triple<String, Double, Double>>()  // FBN, trueScore, evalScore

        dataset.forEachIndexed { index, (fbn, trueScore) ->
            val evalScore = evalScores[index]

            // Calculate metrics
            val error = evalScore - trueScore
            val absError = abs(error)

            totalAbsError += absError
            totalSquaredError += error * error
            errors.add(absError)

            // Sign agreement (do we agree on who's winning?)
            if ((trueScore > 0 && evalScore > 0) ||
                (trueScore < 0 && evalScore < 0) ||
                (abs(trueScore) < 10 && abs(evalScore) < 10)) {  // Both near zero
                signAgreements++
            }

            // Correlation calculation
            sumTrueScores += trueScore
            sumEvalScores += evalScore
            sumTrueSquared += trueScore * trueScore
            sumEvalSquared += evalScore * evalScore
            sumProduct += trueScore * evalScore

            // Track largest errors
            if (largestErrors.size < 10) {
                largestErrors.add(Triple(fbn, trueScore, evalScore))
                largestErrors.sortByDescending { abs(it.second - it.third) }
            } else if (absError > abs(largestErrors.last().second - largestErrors.last().third)) {
                largestErrors[largestErrors.lastIndex] = Triple(fbn, trueScore, evalScore)
                largestErrors.sortByDescending { abs(it.second - it.third) }
            }

            if ((index + 1) % 100 == 0) {
                print("\rEvaluated ${index + 1}/${dataset.size} positions...")
            }
        }

        println("\r" + " ".repeat(50))  // Clear progress line

        // Calculate final metrics
        val n = dataset.size
        val mae = totalAbsError / n
        val rmse = sqrt(totalSquaredError / n)
        val signAccuracy = (signAgreements.toDouble() / n) * 100.0

        // Pearson correlation
        val numerator = n * sumProduct - sumTrueScores * sumEvalScores
        val denominator = sqrt((n * sumTrueSquared - sumTrueScores.pow(2)) *
                              (n * sumEvalSquared - sumEvalScores.pow(2)))
        val correlation = if (denominator != 0.0) numerator / denominator else 0.0

        // Percentiles
        errors.sort()
        val p50 = errors[errors.size / 2]
        val p90 = errors[(errors.size * 0.9).toInt()]
        val p95 = errors[(errors.size * 0.95).toInt()]

        // Return metrics object
        return EvalMetrics(
            mae = mae,
            rmse = rmse,
            medianError = p50,
            p90Error = p90,
            p95Error = p95,
            signAccuracy = signAccuracy,
            correlation = correlation,
            totalPositions = n,
            largestErrors = largestErrors
        )

    } finally {
        // Clean up temp file
        tempFile.delete()
    }
}

/**
 * Scores an evaluation function against a labeled dataset.
 *
 * Metrics:
 * - Mean Absolute Error (MAE)
 * - Root Mean Square Error (RMSE)
 * - Correlation
 * - Sign agreement (does eval agree on who's winning?)
 */
fun main() {
    println("=== Evaluation Function Scorer ===\n")

    val datasetFile = "doc/eval_dataset_5.txt"
    val cflangPath = "./cflang/cflang"

    val metrics = calculateMetrics(datasetFile, cflangPath)

    if (metrics == null) {
        println("ERROR: Failed to calculate metrics")
        return
    }

    // Print results
    println("TOP 10 LARGEST ERRORS")
    println("=".repeat(80))
    metrics.largestErrors.forEachIndexed { index, (fmn, trueScore, evalScore) ->
        val error = evalScore - trueScore
        println("\n${index + 1}. Error: %+.1f (True: %.1f, Eval: %.1f)".format(error, trueScore, evalScore))
        println("   FMN: $fmn")
    }
    println("=".repeat(80))
    println("EVALUATION SCORING RESULTS")
    println("=".repeat(80))
    println()
    println("Dataset: $datasetFile")
    println("Positions: ${metrics.totalPositions}")
    println()
    println("--- Error Metrics ---")
    println("Mean Absolute Error (MAE):     %.2f".format(metrics.mae))
    println("Root Mean Square Error (RMSE): %.2f".format(metrics.rmse))
    println()
    println("--- Error Distribution ---")
    println("Median error:  %.2f".format(metrics.medianError))
    println("90th percentile: %.2f".format(metrics.p90Error))
    println("95th percentile: %.2f".format(metrics.p95Error))
    println()
    println("--- Accuracy Metrics ---")
    println("Sign accuracy: %.1f%% (agrees on who's winning)".format(metrics.signAccuracy))
    println("Correlation:   %.3f (1.0 = perfect correlation)".format(metrics.correlation))
    println()
    println("=".repeat(80))
}