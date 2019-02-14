package org.alexn.async;

@FunctionalInterface
public interface Callback<A> {
  /** To be called when the async process ends in success. */
  void onSuccess(A value);

  /** To be called when the async process ends in error. */
  default void onError(Throwable e) {
    e.printStackTrace();
  }
}
