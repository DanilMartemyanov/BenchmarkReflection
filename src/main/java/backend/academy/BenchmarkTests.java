package backend.academy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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

@State(Scope.Thread)
@SuppressWarnings({"UncommentedMain", "MagicNumber", "MultipleStringLiterals"})
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
            .measurementTime(TimeValue.minutes(2))
            .build();

        new Runner(options).run();
    }



    private Student student;
    private String name;
    private Method method;
    private MethodHandle methodHandleGetName;
    private Function<Student, String> getName;

    @SneakyThrows
    @Setup
    @SuppressFBWarnings("RFI_SET_ACCESSIBLE")
    public void setup() {
        student = new Student("Danil", "Martemyanov");

        method = Student.class.getDeclaredMethod("name");
        method.setAccessible(true);

//      Фабрика для создания method handles
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        MethodType mType = MethodType.methodType(String.class);
        methodHandleGetName = lookup.findVirtual(Student.class, "name", mType);

        getName =
            (Function<Student, String>) createGetter(lookup, methodHandleGetName);

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
        name = getName.apply(student);
        bh.consume(name);
    }

    private static Function createGetter(final MethodHandles.Lookup lookup, final MethodHandle getter)
        throws Exception {
        final CallSite callSite = LambdaMetafactory.metafactory(lookup, "apply",
            MethodType.methodType(Function.class),
            MethodType.methodType(Object.class, Object.class),
            getter,
            getter.type()
        );

        try {
            return (Function) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    record Student(String name, String surname) {
    }

}
