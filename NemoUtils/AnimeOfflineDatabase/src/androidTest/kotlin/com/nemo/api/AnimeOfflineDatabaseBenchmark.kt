package com.nemo.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.lagradost.api.Log
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

@LargeTest
@RunWith(AndroidJUnit4::class)
class AnimeOfflineDatabaseBenchmark {

    companion object {
        @JvmStatic
        @AfterClass
        fun reset() {
            AnimeOfflineDatabase.reset()
        }
    }

    @Test
    fun benchmarkSearchPerformance() = runTest {
        Log.i("AODBenchmark", "Initializing AnimeOfflineDatabase benchmark...")

        AnimeOfflineDatabase.init()

        val query = "Haganai"
        val iterations = 5

        Log.i("AODBenchmark", "Warming up...")
        repeat(2) { AnimeOfflineDatabase.searchTitlesAsync(query) }

        Log.i("AODBenchmark", "Starting benchmark on search query: $query with $iterations iterations...")

        val times = mutableListOf<Long>()

        repeat(iterations) { i ->
            val time = measureTimeMillis {
                val results = AnimeOfflineDatabase.searchTitlesAsync(query)
                if (i == 0) {
                    Log.i("AODBenchmark", "First run results (${results.size} items):")
                    results.take(3).forEach { (match, score) ->
                        Log.i("AODBenchmark", "  - ${match.title} (Score: $score)")
                    }
                    if (results.size > 3) {
                        Log.i("AODBenchmark", "  ... and ${results.size - 3} more")
                    }
                }
            }
            times.add(time)
            Log.i("AODBenchmark", "Run ${i + 1}/$iterations: ${time}ms")
        }

        fun List<Long>.stats(): Triple<Long, Double, Double> {
            val avg = average()
            val stdDev = sqrt(map { (it - avg).pow(2) }.average())
            return Triple(minOrNull()!!, avg, stdDev)
        }

        val (min, avg, stdDev) = times.stats()

        Log.i("AODBenchmark", "=== Results ===")
        Log.i("AODBenchmark", "searchTitlesAsync (ms):")
        Log.i("AODBenchmark", "  Min: $min, Avg: ${"%.2f".format(avg)}, StdDev: ${"%.2f".format(stdDev)}")
    }
}
