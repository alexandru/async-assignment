package io.oriel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The `Async` data type is a lazy `Future`, i.e. a way to describe
 * asynchronous computations.
 *
 * It is described by {@link Async#run(Callback)}, it's characteristic
 * function. See {@link Async#eval(Executor, Supplier)} for how `Async`
 * values can be built.
 *
 * The assignment, should you wish to accept it, is to fill in the implementation
 * for all functions that are marked with `throw UnsupportedOperationException`.
 */
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
    // TODO
    throw new UnsupportedOperationException("Please implement!");
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
    // TODO
    throw new UnsupportedOperationException("Please implement!");
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
    // TODO
    throw new UnsupportedOperationException("Please implement!");
  }

  /**
   * Executes the two `Async` values in parallel, executing the given function for
   * producing a final result.
   *
   * <pre>
   * {@code
   * Async<Integer> fa = Async.eval(() -> 1 + 1)
   *
   * Async<Integer> fb = Async.eval(() -> 2 + 2)
   *
   * // Should yield 6
   * Async<Integer> fc = Async.parMap2(fa, fb, (a, b) -> a + b)
   * }
   * </pre>
   */
  public static <A, B, C> Async<C> parMap2(Async<A> fa, Async<B> fb, BiFunction<A, B, C> f) {
    // TODO
    throw new UnsupportedOperationException("Please implement!");
  }

  /**
   * Given a list of `Async` values, processes all of them and returns the
   * final result as a list.
   *
   * Execution of the given list should be sequential (not parallel).
   *
   * SHOULD implement in terms of `flatMap` ;-)
   */
  public static <A> Async<A[]> sequence(Executor ec, Async<A>[] list) {
    // TODO
    throw new UnsupportedOperationException("Please implement!");
  }

  /**
   * Given a list of `Async` values, processes all of them in parallel and
   * returns the final result as a list.
   *
   * Execution of the given list should be parallel.
   *
   * SHOULD implement in terms of `parMap2` ;-)
   */
  public static <A> Async<A[]> parallel(Executor ec, Async<A>[] list) {
    // TODO
    throw new UnsupportedOperationException("Please implement!");
  }

  /**
   * Describes an async computation that executes the given `thunk`
   * on the provided `Executor`.
   *
   * <pre>
   * {@code
   * Async<Integer> fa = Async.eval(() -> 1 + 1)
   * }
   * </pre>
   */
  public static <A> Async<A> eval(Executor ec, Supplier<A> thunk) {
    return new Async<A>(ec) {
      @Override
      void run(Callback<A> cb) {
        // Asynchronous boundary
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
