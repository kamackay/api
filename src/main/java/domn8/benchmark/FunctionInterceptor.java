package domn8.benchmark;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import static domn8.benchmark.BenchmarkData.data;
import static domn8.benchmark.BenchmarkTimer.timer;


public class FunctionInterceptor implements MethodInterceptor {

  public Object invoke(MethodInvocation invocation) throws Throwable {
    Benchmark annotation = invocation.getMethod().getAnnotation(Benchmark.class);
    final StringBuilder nameBuilder = new StringBuilder(String.format("%s.%s",
        invocation.getThis().getClass().getSuperclass().getSimpleName(),
        invocation.getMethod().getName()));
    try {
      if (annotation.paramName()) {
        nameBuilder.append("(\"").append(invocation.getArguments()[0].toString()).append("\")");
      }
    } catch (Exception e) {
      // Something went wrong getting the first string parameter, move on
    }
    final String name = nameBuilder.toString();
    timer().start(data(name, annotation.limit()));
    Object result = invocation.proceed();
    timer().end(name);
    return result;
  }
}
