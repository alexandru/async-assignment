package org.alexn.async;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncTests {
  private final int count = 10000;
  private ExecutorService ec;

  @Before
  public void setup() {
    ec = Executors.newFixedThreadPool(4);
  }

  @After
  public void tearDown() {
    ec.shutdown();
  }

  @Test public void eval() {
    Async<Integer> fa = Async.eval(() -> 1 + 1);
    assertEquals(await(fa, ec).intValue(), 2);
    // Repeating the evaluation should yield the same value
    assertEquals(await(fa, ec).intValue(), 2);
  }

  /**
   * TIP: If the `times` equality test fails, it means the returned
   * instance has shared mutable state, possibly caching the result.
   *
   * It shouldn't. `Async` is similar with Java's `Future`, but it
   * needs to behave like a function that doesn't do any caching.
   */
  @Test public void evalIsNotMemoized() {
    final AtomicInteger times = new AtomicInteger(0);
    Async<Integer> fa = Async.eval(() -> {
      times.incrementAndGet();
      return 1 + 1;
    });

    assertEquals(await(fa, ec).intValue(), 2);
    assertEquals(await(fa, ec).intValue(), 2);
    assertEquals(times.get(), 2);
  }

  @Test public void toFuture() throws Exception {
    final AtomicInteger times = new AtomicInteger(0);
    Async<Integer> task = Async.eval(() -> {
      times.incrementAndGet();
      return 1 + 1;
    });

    assertEquals(task.toFuture(ec).get().intValue(), 2);
    assertEquals(task.toFuture(ec).get().intValue(), 2);
    // Should not do memoization
    assertEquals(times.get(), 2);
  }

  @Test public void fromFuture() {
    final AtomicInteger times = new AtomicInteger(0);
    final Async<Integer> f =
      Async.fromFuture(() ->
        CompletableFuture.supplyAsync(times::incrementAndGet, ec)
      );

    assertEquals(await(f, ec).intValue(), 1);
    assertEquals(await(f, ec).intValue(), 2);
    assertEquals(await(f, ec).intValue(), 3);
  }

  @Test public void mapIdentity() {
    Async<Integer> task = Async
      .eval(() -> 1 + 1)
      .map(x -> x);

    assertEquals(await(task, ec).intValue(), 2);
  }

  @Test public void mapAssociativity() {
    Async<Integer> lh = Async
      .eval(() -> 1 + 1)
      .map(x -> x * 2)
      .map(x -> x + 2);

    Async<Integer> rh = Async
      .eval(() -> 1 + 1)
      .map(x -> x * 2 + 2);

    assertEquals(await(lh, ec).intValue(), await(rh, ec).intValue());
  }

  /**
   * TIP: if the above tests don't fail, but this one does, it means
   * that you haven't used the `executor` for scheduling the transformation
   * of the result. Use the `executor` to make `map` stack safe.
   */
  @Test public void mapIsStackSafe() {
    final int count = 10000;

    Async<Integer> ref = Async.eval(() -> 0);
    for (int i = 0; i < count; i++) {
      ref = ref.map(x -> x + 1);
    }

    assertEquals(await(ref, ec).intValue(), count);
  }

  @Test public void flatMapRightIdentity() {
    Async<Integer> lh = Async
      .eval(() -> 1 + 1)
      .flatMap(x -> Async.eval(() -> x));

    assertEquals(await(lh, ec).intValue(), 2);
  }

  @Test public void flatMapAssociativity() {
    Async<Integer> lh = Async
      .eval(() -> 1 + 1)
      .flatMap(x -> Async.eval(() -> x * 2))
      .flatMap(x -> Async.eval(() -> x + 2));

    Async<Integer> rh = Async
      .eval(() -> 1 + 1)
      .flatMap(x -> Async
        .eval(() -> x * 2)
        .flatMap(y -> Async.eval(() -> y + 2)));

    assertEquals(await(lh, ec).intValue(), await(rh, ec).intValue());
  }

  /**
   * TIP: if the above tests don't fail, but this one does, it means
   * that you haven't used the `executor` for scheduling the transformation
   * of the result. Use the `executor` to make `flatMap` stack safe.
   */
  @Test public void flatMapIsStackSafe() {
    final int count = 10000;

    Async<Integer> ref = Async.eval(() -> 0);
    for (int i = 0; i < count; i++) {
      ref = ref.flatMap(x -> Async.eval(() -> x + 1));
    }

    assertEquals(await(ref, ec).intValue(), count);
  }

  @Test
  public void sequence() {
    final ArrayList<Async<Integer>> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      list.add(Async.eval(() -> 1 + 1));
    }

    Async<Integer> sum = Async.sequence(ec, list)
      .map(l -> l.stream().reduce(0, Integer::sum));

    assertEquals(await(sum, ec).intValue(), count * 2);
  }

  @Test
  public void parMap2() throws InterruptedException, ExecutionException {
    final CountDownLatch workersStarted = new CountDownLatch(2);
    final CountDownLatch release = new CountDownLatch(1);

    final Async<Integer> task = Async.eval(() -> {
      workersStarted.countDown();
      try {
        assertTrue(release.await(10L, TimeUnit.SECONDS));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return 10;
    });

    final CompletableFuture<Integer> result =
      Async
        .parMap2(ec, task, task, Integer::sum)
        .toFuture(ec);

    assertTrue(workersStarted.await(10L, TimeUnit.SECONDS));
    release.countDown();
    assertEquals(result.get().intValue(), 20);
  }

  @Test
  public void parallel() {
    final ArrayList<Async<Integer>> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      list.add(Async.eval(() -> 1 + 1));
    }

    Async<Integer> sum = Async.parallel(ec, list)
      .map(l -> l.stream().reduce(0, Integer::sum));

    assertEquals(await(sum, ec).intValue(), count * 2);
  }

  public static <A> A await(Async<A> fa, Executor e) {
    BlockingCallback<A> cb = new BlockingCallback<>();
    fa.run(e, cb);
    return cb.get();
  }

  static class BlockingCallback<A> implements Callback<A> {
    private A value = null;
    private Throwable e = null;
    private final CountDownLatch l;

    public BlockingCallback() {
      this.l = new CountDownLatch(1);
    }

    @Override
    public void onSuccess(A value) {
      this.value = value;
      l.countDown();
    }

    @Override
    public void onError(Throwable e) {
      this.e = e;
      l.countDown();
    }

    public A get() {
      try { l.await(3, TimeUnit.SECONDS); }
      catch (InterruptedException ignored) {}
      if (e != null) throw new RuntimeException(e);
      return this.value;
    }
  }
}
