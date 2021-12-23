package domn8.benchmark;


import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static domn8.util.Utils.millisToReadableTime;

/**
 * Debugging class to quickly log time that code execution takes
 */
@Slf4j
public class BenchmarkTimer {

  private static BenchmarkTimer instance = null;
  private final Map<String, BenchmarkData> startTimes;
  private long limit = 0;

  private BenchmarkTimer() {
    startTimes = new HashMap<>();
  }

  /**
   * Get the existing instance of the timer object
   *
   * @return Singleton instance of this object
   */
  public static BenchmarkTimer timer() {
    if (instance == null) {
      instance = new BenchmarkTimer();
    }

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
        log.warn("Benchmark '{}' completed in {}",
            benchmarkName,
            millisToReadableTime(time));
      }
    }, () -> log.warn("Call to end Benchmark \"{}\" without initializing first",
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
