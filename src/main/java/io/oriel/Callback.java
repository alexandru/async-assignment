package io.oriel;

@FunctionalInterface
public interface Callback<A> {
  void onSuccess(A value);

  default void onError(Throwable e) {
    e.printStackTrace();
  }
}
