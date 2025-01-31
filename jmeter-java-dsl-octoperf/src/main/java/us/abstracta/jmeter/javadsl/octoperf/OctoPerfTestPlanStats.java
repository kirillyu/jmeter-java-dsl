package us.abstracta.jmeter.javadsl.octoperf;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.jmeter.samplers.SampleResult;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.stats.CountMetricSummary;
import us.abstracta.jmeter.javadsl.core.stats.StatsSummary;
import us.abstracta.jmeter.javadsl.core.stats.TimeMetricSummary;
import us.abstracta.jmeter.javadsl.octoperf.api.BenchResult;
import us.abstracta.jmeter.javadsl.octoperf.api.TableEntry;
import us.abstracta.jmeter.javadsl.octoperf.api.TableEntry.TableValue;
import us.abstracta.jmeter.javadsl.octoperf.api.VirtualUser;
import us.abstracta.jmeter.javadsl.octoperf.api.VirtualUser.Action;

public class OctoPerfTestPlanStats extends TestPlanStats {

  public OctoPerfTestPlanStats(double[] summaryStats, List<TableEntry> tableStats,
      List<VirtualUser> vus, BenchResult result) {
    super(() -> null);
    setStart(result.getCreated());
    setEnd(result.getLastModified());
    overallStats = new OctoPerfStatsSummary(result, summaryStats);
    Map<String, String> actionsLabels = vus.stream()
        .flatMap(vu -> vu.getChildren().stream())
        .collect(Collectors.toMap(Action::getId, Action::getName));
    labeledStats.putAll(tableStats.stream()
        .collect(Collectors.toMap(s -> actionsLabels.get(s.getActionId()),
            s -> new OctoPerfStatsSummary(result, s),
            OctoPerfStatsSummary::new)));
  }

  public static class OctoPerfStatsSummary implements StatsSummary {

    private final Instant startTime;
    private final Instant endTime;
    private final OctoPerfCount samples;
    private final OctoPerfCount errors;
    private final OctoPerfTime sampleTime;
    private final OctoPerfCount receivedBytes;
    private final OctoPerfCount sentBytes;

    public OctoPerfStatsSummary(BenchResult result, double[] summaryStats) {
      startTime = result.getCreated();
      endTime = result.getLastModified();
      int statIndex = 0;
      samples = new OctoPerfCount(summaryStats[statIndex++], summaryStats[statIndex++]);
      errors = new OctoPerfCount(summaryStats[statIndex++], summaryStats[statIndex++]);
      sampleTime = new OctoPerfTime(summaryStats[statIndex++], summaryStats[statIndex++],
          summaryStats[statIndex++], summaryStats[statIndex++], summaryStats[statIndex++],
          summaryStats[statIndex++], summaryStats[statIndex++]);
      receivedBytes = new OctoPerfCount(summaryStats[statIndex++], summaryStats[statIndex++]);
      sentBytes = new OctoPerfCount(summaryStats[statIndex++], summaryStats[statIndex]);
    }

    public OctoPerfStatsSummary(BenchResult result, TableEntry s) {
      this(result, s.getValues().stream()
          .mapToDouble(TableValue::getValue)
          .toArray());
    }

    public OctoPerfStatsSummary(OctoPerfStatsSummary s1, OctoPerfStatsSummary s2) {
      startTime = s1.startTime.isBefore(s2.startTime) ? s1.startTime : s2.startTime;
      endTime = s1.endTime.isAfter(s2.endTime) ? s1.endTime : s2.endTime;
      samples = new OctoPerfCount(s1.samples, s2.samples);
      errors = new OctoPerfCount(s1.errors, s2.errors);
      sampleTime = new OctoPerfTime(s1.sampleTime, s2.sampleTime, s1.samples.total(),
          s2.samples.total());
      receivedBytes = new OctoPerfCount(s1.receivedBytes, s2.receivedBytes);
      sentBytes = new OctoPerfCount(s1.sentBytes, s2.sentBytes);
    }

    @Override
    public void add(SampleResult result) {
    }

    @Override
    public Instant firstTime() {
      return startTime;
    }

    @Override
    public Instant endTime() {
      return endTime;
    }

    @Override
    public CountMetricSummary samples() {
      return samples;
    }

    @Override
    public CountMetricSummary errors() {
      return errors;
    }

    @Override
    public TimeMetricSummary sampleTime() {
      return sampleTime;
    }

    @Override
    public CountMetricSummary receivedBytes() {
      return receivedBytes;
    }

    @Override
    public CountMetricSummary sentBytes() {
      return sentBytes;
    }

  }

  public static class OctoPerfCount extends CountMetricSummary {

    private final long total;
    private final double rate;

    private OctoPerfCount(double total, double rate) {
      this.total = Math.round(total);
      this.rate = rate;
    }

    public OctoPerfCount(OctoPerfCount s1, OctoPerfCount s2) {
      total = s1.total + s2.total;
      rate = s1.rate + s2.rate;
    }

    @Override
    public long total() {
      return total;
    }

    @Override
    public double perSecond() {
      return rate;
    }

  }

  public static class OctoPerfTime implements TimeMetricSummary {

    private final Duration mean;
    private final Duration min;
    private final Duration max;
    private final Duration median;
    private final Duration perc90;
    private final Duration perc95;
    private final Duration perc99;

    public OctoPerfTime(double mean, double min, double max, double median, double perc90,
        double perc95, double perc99) {
      this.mean = durationFromSecs(mean);
      this.min = durationFromSecs(min);
      this.max = durationFromSecs(max);
      this.median = durationFromSecs(median);
      this.perc90 = durationFromSecs(perc90);
      this.perc95 = durationFromSecs(perc95);
      this.perc99 = durationFromSecs(perc99);
    }

    public OctoPerfTime(OctoPerfTime time1, OctoPerfTime time2, long count1, long count2) {
      mean = weightedDuration(time1.mean, time2.mean, count1, count2);
      min = time1.min.compareTo(time2.min) < 0 ? time1.min : time2.min;
      max = time1.max.compareTo(time2.max) > 0 ? time1.max : time2.max;
      /*
       since OctoPerf does not provide these metrics per label, we approximate them with weighted
       values
       */
      median = weightedDuration(time1.median, time2.median, count1, count2);
      perc90 = weightedDuration(time1.perc90, time2.perc90, count1, count2);
      perc95 = weightedDuration(time1.perc95, time2.perc95, count1, count2);
      perc99 = weightedDuration(time1.perc99, time2.perc99, count1, count2);
    }

    private Duration durationFromSecs(double secs) {
      return Duration.ofMillis(Math.round(secs * 1000));
    }

    private Duration weightedDuration(Duration d1, Duration d2, long w1, long w2) {
      return Duration.ofMillis(
          Math.round(((double) d1.toMillis() * w1) + ((double) d2.toMillis() * w2)) / (w1 + w2));
    }

    @Override
    public Duration mean() {
      return mean;
    }

    @Override
    public Duration min() {
      return min;
    }

    @Override
    public Duration max() {
      return max;
    }

    @Override
    public Duration median() {
      return median;
    }

    @Override
    public Duration perc90() {
      return perc90;
    }

    @Override
    public Duration perc95() {
      return perc95;
    }

    @Override
    public Duration perc99() {
      return perc99;
    }

  }

}
