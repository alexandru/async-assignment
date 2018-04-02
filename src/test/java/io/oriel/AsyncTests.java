package io.oriel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.*;

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

  @Test
  public void eval() {
    Async<Integer> fa = Async.eval(ec, () -> 1 + 1);
    assertEquals(await(fa).intValue(), 2);
  }

  @Test
  public void toFuture() throws Exception {
    Async<Integer> task = Async.eval(ec, () -> 1 + 1);
    assertEquals(task.toFuture().get().intValue(), 2);
  }

  @Test
  public void mapIdentity() {
    Async<Integer> task = Async
      .eval(ec, () -> 1 + 1)
      .map(x -> x);

    assertEquals(await(task).intValue(), 2);
  }

  @Test
  public void mapAssociativity() {
    Async<Integer> lh = Async
      .eval(ec, () -> 1 + 1)
      .map(x -> x * 2)
      .map(x -> x + 2);

    Async<Integer> rh = Async
      .eval(ec, () -> 1 + 1)
      .map(x -> x * 2 + 2);

    assertEquals(await(lh).intValue(), await(rh).intValue());
  }

  @Test
  public void flatMapRightIdentity() {
    Async<Integer> lh = Async
      .eval(ec, () -> 1 + 1)
      .flatMap(x -> Async.eval(ec, () -> x));

    assertEquals(await(lh).intValue(), 2);
  }

  @Test
  public void flatMapAssociativity() {
    Async<Integer> lh = Async
      .eval(ec, () -> 1 + 1)
      .flatMap(x -> Async.eval(ec, () -> x * 2))
      .flatMap(x -> Async.eval(ec, () -> x + 2));

    Async<Integer> rh = Async
      .eval(ec, () -> 1 + 1)
      .flatMap(x -> Async
        .eval(ec, () -> x * 2)
        .flatMap(y -> Async.eval(ec, () -> y + 2)));

    assertEquals(await(lh).intValue(), await(rh).intValue());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void sequence() {
    final ArrayList<Async<Integer>> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      list.add(Async.eval(ec, () -> 1 + 1));
    }

    Async<Integer> sum = Async.sequence(ec, list)
      .map(l -> l.stream().reduce(0, (x, y) -> x + y));

    assertEquals(await(sum).intValue(), count * 2);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void parallel() {
    final ArrayList<Async<Integer>> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      list.add(Async.eval(ec, () -> 1 + 1));
    }

    Async<Integer> sum = Async.parallel(ec, list)
      .map(l -> l.stream().reduce(0, (x, y) -> x + y));

    assertEquals(await(sum).intValue(), count * 2);
  }

  public static <A> A await(Async<A> fa) {
    BlockingCallback<A> cb = new BlockingCallback<>();
    fa.run(cb);
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
