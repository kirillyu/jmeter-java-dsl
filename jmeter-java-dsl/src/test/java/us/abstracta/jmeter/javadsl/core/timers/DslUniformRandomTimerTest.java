package us.abstracta.jmeter.javadsl.core.timers;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.uniformRandomTimer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import us.abstracta.jmeter.javadsl.JmeterDslTest;
import us.abstracta.jmeter.javadsl.codegeneration.MethodCallBuilderTest;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

public class DslUniformRandomTimerTest extends JmeterDslTest {

  @Test
  public void shouldLastAtLeastMinimumTimeWhenUsingRandomUniformTimer() throws Exception {
    long minimumMillis = 5000;
    TestPlanStats stats = testPlan(
        threadGroup(1, 1,
            uniformRandomTimer(minimumMillis, 7000),
            httpSampler(wiremockUri)
        )
    ).run();
    assertThat(stats.duration().toMillis()).isGreaterThan(minimumMillis);
  }

  @Nested
  public class CodeBuilderTest extends MethodCallBuilderTest {

    public DslTestPlan testPlanWithUniformRandomTimer() {
      return testPlan(
          threadGroup(1, 1,
              uniformRandomTimer(1000, 5000),
              httpSampler("http://localhost")
          )
      );
    }

  }

}
