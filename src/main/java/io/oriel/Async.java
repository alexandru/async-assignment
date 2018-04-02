package io.oriel;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Async<A> {
  /** Execution context. */
  public final Executor context;

  public Async(Executor ec) {
    this.context = ec;
  }

  /**
   * Characteristic function; every function that needs to be implemented
   * below should be based on calls to `run`.
   */
  abstract void run(Callback<A> cb);

  /**
   * Converts this `Async` to a Java `CompletableFuture`, triggering
   * the computation in the process.
   */
  public CompletableFuture<A> toFuture() {
    throw new NotImplementedException();
  }

  /**
   * Given a mapping function, returns a new `Async` value with the
   * result of the source transformed with it.
   *
   * <pre>
   * {@code
   * Async<Integer> fa = Async.eval(() -> 1 + 1)
   *
   * Async<Integer> fb = fa.map(a -> a * 2)
   *
   * Async<String> fc = fb.map(x -> x.toString())
   * }
   * </pre>
   *
   * As a piece of trivia that you don't need to know for this
   * assignment, this function describes a Functor, see:
   * <a href="https://en.wikipedia.org/wiki/Functor">Functor</a>.
   */
  public <B> Async<B> map(Function<A, B> f) {
    throw new NotImplementedException();
  }

  /**
   * Given a mapping function that returns another async result,
   * returns a new `Async` value with the result of the source transformed.
   *
   * <pre>
   * {@code
   * Async<Integer> fa = Async.eval(() -> 1 + 1)
   *
   * Async<Integer> fb = fa.flatMap(a -> Async.eval(() -> a * 2))
   *
   * Async<String> fc = fb.flatMap(x -> Async.eval(() -> x.toString()))
   * }
   * </pre>
   *
   * As a piece of trivia that you don't need to know for this
   * assignment, this is the "monadic bind", see:
   * <a href="https://en.wikipedia.org/wiki/Monad_(functional_programming)">Monad</a>.
   */
  public <B> Async<B> flatMap(Function<A, Async<B>> f) {
    throw new NotImplementedException();
  }

  /**
   * Executes the two `Async` values in parallel, executing the given function for
   * producing a final result.
   */
  public static <A, B, C> Async<C> parMap2(Async<A> fa, Async<B> fb, BiFunction<A, B, C> f) {
    throw new NotImplementedException();
  }

  /**
   * Describes an async computation that executes the given `thunk`
   * on the provided `Executor`.
   */
  public static <A> Async<A> eval(Executor ec, Supplier<A> thunk) {
    return new Async<A>(ec) {
      @Override
      void run(Callback<A> cb) {
        ec.execute(() -> {
          boolean streamError = true;
          try {
            A value = thunk.get();
            streamError = false;
            cb.onSuccess(value);
          } catch (Exception e) {
            if (streamError) cb.onError(e);
            else throw e;
          }
        });
      }
    };
  }
}
