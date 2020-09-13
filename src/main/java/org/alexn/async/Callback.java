package org.alexn.async;

import java.util.concurrent.atomic.AtomicBoolean;

@FunctionalInterface
public interface Callback<A> {
  /** To be called when the async process ends in success. */
  void onSuccess(A value);

  /** To be called when the async process ends in error. */
  default void onError(Throwable e) {
    e.printStackTrace();
  }

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
}
