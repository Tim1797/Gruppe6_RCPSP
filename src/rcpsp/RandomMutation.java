package rcpsp;

import java.util.Random;

public class RandomMutation {
  private static final double Probability = 0.25;

  public static int[] mutate(int[] solution, Instance instance) {
    int len = solution.length;
    Random rng = App.getRandom();
    if (rng.nextDouble() > Probability) {
      return null;
    }

    int i = rng.nextInt(len);

    int startTime = solution[i];
    // TODO: Find earliest starting time.

    int endTime = solution[i];
    int[] succ = instance.successors[i];
    for (int index : succ) {
      int time = solution[index];
      if (time > endTime) {
        endTime = time;
      }
    }

    if (startTime != endTime) {
      int[] buf = new int[solution.length];
      System.arraycopy(solution, 0, buf, 0, solution.length);

      for (int j = 0; j < 3; ++j) {
        buf[i] = startTime + rng.nextInt(endTime - startTime);
        if (Solver.checkSolution(buf, instance)) {
          return buf;
        }
      }
    }
    return null;
  }
}
