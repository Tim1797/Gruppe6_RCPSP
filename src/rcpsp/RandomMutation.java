package rcpsp;

import java.util.Random;

public class RandomMutation {
  private static final double Probability = 0.40;

  public static Solution mutate(Solution solution, Instance instance, int maxMakespan) {
    Random rng = App.getRandom();
    if (rng.nextDouble() > Probability) {
      return solution;
    }

    int[] solActivityList = Solver.transformSolutionIntoActivityList(solution, maxMakespan);
    var copy = new ArrayListEx<Integer>(solActivityList.length);
    for (int i = 0; i < solActivityList.length; ++i) {
      copy.add(solActivityList[i]);
    }

    boolean feasible = false;
    int counter = 0;
    while (!feasible && counter < 100) {
      int swapPoint1 = rng.nextInt(instance.n());
      int swapPoint2 = rng.nextInt(instance.n());
      int temp1 = copy.get(swapPoint1);
      int temp2 = copy.get(swapPoint2);
      copy.set(swapPoint1, temp2);
      copy.set(swapPoint2, temp1);

      feasible = Solver.checkSolution(Solver.ess(copy, instance, maxMakespan), instance);
      if (!feasible) {
        // swap back
        copy.set(swapPoint1, temp1);
        copy.set(swapPoint2, temp2);
      }
      ++counter;
    }
    return Solver.ess(copy, instance, maxMakespan);
  }
}