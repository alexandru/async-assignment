# Async Assignment (in Java)

The assignment is about finishing the implementation of the described `Async`
data type — for educational purposes, or for interviewing. 

`Async` is a data-type similar to [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html), except that the result isn't memoized, being a more "pure" abstraction.

This assignment has been used (with mixed results) for spotting Java developers that 
could make the transition to Scala and Functional Programming 😎

1. see [Async](./src/main/java/org/alexn/async/Async.java) and [Main](./src/main/java/org/alexn/async/Main.java)
2. read the source code already in place
3. implement the functions marked with `throw UnsupportedOperationException`
4. make sure the tests are passing, see [AsyncTest](./src/test/java/org/alexn/async/AsyncTest.java)

To run the provided test suite:

```
$ mvn test
```

NOTE: the build tool used is [Apache Maven](https://maven.apache.org/).

## Details

The described `Async` data type resembles Java's
[CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html),
except that it behaves like a function instead of a variable.

Quick usage sample:

```java
import org.alexn.async.Async;
import java.util.Random;
import java.util.concurrent.*;

Async<Integer> number = Async.eval(
  () -> {
    Random rnd = new Random(System.currentTimeMillis());
    return rnd.nextInt();
  });

// Needed for executing tasks
Executor ec = Executors.newCachedThreadPool();

// Actual execution, happens on another thread ;-)
number.run(ec, value -> {
  System.out.println("Generated random number: " + value);
});
```
