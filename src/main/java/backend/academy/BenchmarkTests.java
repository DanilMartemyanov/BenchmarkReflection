package backend.academy;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@State(Scope.Thread)
public class BenchmarkTests {
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(BenchmarkTests.class.getSimpleName())
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.NANOSECONDS)
            .forks(1)
            .warmupForks(1)
            .warmupIterations(1)
            .warmupTime(TimeValue.seconds(5))
            .measurementIterations(1)
            .measurementTime(TimeValue.seconds(5))
            .build();

        new Runner(options).run();
    }

    record Student(String name, String surname) {
    }

    private Student student;
    private String name;
    private Method method;
    private MethodHandle methodHandleGetName;



    @SneakyThrows
    @Setup
    public void setup() {
        student = new Student("Danil", "Martemyanov");

        method = Student.class.getDeclaredMethod("name");
        method.setAccessible(true);

//      Фабрика для создания method handles
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        MethodType mType = MethodType.methodType(String.class);
        methodHandleGetName = lookup.findVirtual(Student.class, "name", mType);

    }

    @Benchmark
    public void directAccess(Blackhole bh) {
        name = student.name();
        bh.consume(name);

    }

    @SneakyThrows
    @Benchmark
    public void reflection(Blackhole bh) {
        name = (String) method.invoke(student);
        bh.consume(name);
    }

    @SneakyThrows
    @Benchmark
    public void methodHandle(Blackhole bh) {
        name = (String) methodHandleGetName.invoke(student);
        bh.consume(name);
    }

    @Benchmark
    public void lambdaMetafactory(Blackhole bh) {

    }




}
