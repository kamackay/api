package com.keithmackay.api.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.keithmackay.api.utils.UtilsKt.millisToReadableTime;


/**
 * Debugging class to quickly log time that code execution takes
 */
public class BenchmarkTimer {

  private static BenchmarkTimer instance = null;
  private final Map<String, BenchmarkData> startTimes;
  private long limit = 0;
  private Logger logger = LoggerFactory.getLogger(BenchmarkTimer.class);

  private BenchmarkTimer() {
    startTimes = new HashMap<>();
  }

  /**
   * Get the existing instance of the timer object
   *
   * @return Singleton instance of this object
   */
  public static BenchmarkTimer timer() {
    if (instance == null)
      instance = new BenchmarkTimer();

    return instance;
  }

  /**
   * Start a timer with the default data
   *
   * @param name - the Name of the timer to start
   * @return - this object, for the builder pattern
   */
  public BenchmarkTimer start(final String name) {
    return start(BenchmarkData.builder().name(name).limit(limit).build());
  }

  /**
   * Start a timer
   *
   * @param data - The data of the timer to start
   * @return - this object, for the builder pattern
   */
  public BenchmarkTimer start(final BenchmarkData data) {
    startTimes.put(data.getName(), data);
    return this;
  }

  /**
   * End a benchmark, and if it was above the configured limit, log it's data
   *
   * @param benchmarkName - Name of the benchmark to end
   * @return - this object, for the builder pattern
   */
  public BenchmarkTimer end(final String benchmarkName) {
    final Optional<BenchmarkData> dataOptional = get(benchmarkName);
    dataOptional.ifPresentOrElse(data -> {
      final long time = System.currentTimeMillis() - data.getStartTime();
      if (time > data.getLimit() || data.isLogAbsolute()) {
        logger.warn("Benchmark '{}' completed in {}",
            benchmarkName,
            millisToReadableTime(time));
      }
    }, () -> logger.warn("Call to end Benchmark \"{}\" without initializing first",
        benchmarkName));
    return this;
  }

  /**
   * Set the limit for which anything longer should be logged
   *
   * @param limit - If process takes longer than the limit, it will be logged
   */
  public BenchmarkTimer setLogLimit(final long limit) {
    this.limit = limit;
    return this;
  }

  private Optional<BenchmarkData> get(final String name) {
    return Optional.ofNullable(startTimes.get(name));
  }
}
