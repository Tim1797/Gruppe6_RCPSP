package rcpsp;

import java.util.Random;

public class App {
  private static Random rng = null;

  public static void init(long seed) {
    rng = new Random(seed);
  }

  public static Random getRandom() {
    if (rng == null) {
      throw new RuntimeException("Invalid state.");
    }
    return rng;
  }
}
