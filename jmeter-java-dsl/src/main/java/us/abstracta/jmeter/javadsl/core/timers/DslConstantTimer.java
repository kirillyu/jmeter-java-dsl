package us.abstracta.jmeter.javadsl.core.timers;

import java.lang.reflect.Method;
import java.util.List;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.timers.ConstantTimer;
import org.apache.jmeter.timers.RandomTimer;
import org.apache.jmeter.timers.gui.ConstantTimerGui;
import us.abstracta.jmeter.javadsl.codegeneration.MethodCall;
import us.abstracta.jmeter.javadsl.codegeneration.MethodCallContext;
import us.abstracta.jmeter.javadsl.codegeneration.MethodParam;
import us.abstracta.jmeter.javadsl.codegeneration.SingleTestElementCallBuilder;
import us.abstracta.jmeter.javadsl.codegeneration.TestElementParamBuilder;
import us.abstracta.jmeter.javadsl.codegeneration.params.LongParam;

/**
 * Allows using JMeter Constant Timers which pause the thread for a given period.
 * <p>
 * The pause calculated by the timer will be applied after samplers pre-processors execution and
 * before actual sampling.
 * <p>
 * Take into consideration that timers applies to all samplers in their scope: if added at test plan
 * level, it will apply to all samplers in test plan; if added at thread group level, it will apply
 * only to samples in such thread group; if added as child of a sampler, it will only apply to that
 * sampler.
 *
 * @since 0.62
 */
public class DslConstantTimer extends BaseTimer {

  private final long durationMillis;

  public DslConstantTimer(long durationMillis) {
    super("Constant Timer", ConstantTimerGui.class);
    this.durationMillis = durationMillis;
  }

  @Override
  protected TestElement buildTestElement() {
    ConstantTimer ret = new ConstantTimer();
    ret.setDelay(String.valueOf(durationMillis));
    return ret;
  }

  public static class CodeBuilder extends SingleTestElementCallBuilder<ConstantTimer> {

    public CodeBuilder(List<Method> builderMethods) {
      super(ConstantTimer.class, builderMethods);
    }

    @Override
    protected MethodCall buildMethodCall(ConstantTimer testElement, MethodCallContext context) {
      TestElementParamBuilder paramBuilder = new TestElementParamBuilder(testElement);
      MethodParam delay = paramBuilder.longParam(RandomTimer.DELAY);
      if (!(delay instanceof LongParam)) {
        throw new UnsupportedOperationException("Using JMeter expressions in timer properties is "
            + "still not supported. Request it in the GitHub repository as an issue and we will "
            + "add support for it.");
      }
      return ((LongParam) delay).getValue() == 0 ? MethodCall.emptyCall() : buildMethodCall(delay);
    }

  }

}
