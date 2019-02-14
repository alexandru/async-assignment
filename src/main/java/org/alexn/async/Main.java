package org.alexn.async;

import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
  public static Async<Void> run() {
    return print("Your name: ").flatMap(v ->
      readLine().flatMap(name ->
        printLn("Hello, " + name + "!")
      )
    );
  }

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    final ExecutorService ec = Executors.newCachedThreadPool();
    try {
      run().toFuture(ec).get();
    } finally {
      ec.shutdown();
    }
  }

  static Async<String> readLine() {
    return Async.eval(() -> {
      try (Scanner in = new Scanner(System.in)) {
        return in.next();
      }
    });
  }

  static Async<Void> print(String str) {
    return Async.eval(() -> {
      System.out.print(str);
      System.out.flush();
      return null;
    });
  }

  static Async<Void> printLn(String str) {
    return print(str + "\n");
  }
}
