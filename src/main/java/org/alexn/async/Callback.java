package org.alexn.async;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public interface Callback<A> {
  /** To be called when the async process ends in success. */
  void onSuccess(A value);

  /** To be called when the async process ends in error. */
  void onError(Throwable e);

  /**
   * Wraps the given callback implementation into one that
   * can be safely called multiple times, ensuring "idempotence".
   */
  static <A> Callback<A> safe(Callback<A> underlying) {
    return new Callback<A>() {
      private final AtomicBoolean wasCalled =
        new AtomicBoolean(false);

      @Override
      public void onSuccess(A value) {
        if (wasCalled.compareAndSet(false, true))
          underlying.onSuccess(value);
      }

      @Override
      public void onError(Throwable e) {
        if (wasCalled.compareAndSet(false, true))
          underlying.onError(e);
        else
          e.printStackTrace();
      }
    };
  }

  /** 
   * Converts a classic JavaScript-style callback.
   */
  static <A> Callback<A> fromClassicCallback(BiConsumer<Throwable, A> cb) {
    return new Callback<A>() {
      @Override
      public void onSuccess(A value) { cb.accept(null, value); }

      @Override
      public void onError(Throwable e) { cb.accept(e, null); }
    };
  }
}
