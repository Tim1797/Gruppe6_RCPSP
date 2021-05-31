package rcpsp;

public class Fitness {
  /// Returns the makespan.
  public static int get(int[] solution, Instance instance) {
    int max = 0;
    for (int i = 0; i < solution.length; i++) {
      int currEnd = solution[i] + instance.processingTime[i];
      if (currEnd > max) {
        max = currEnd;
      }
    }
    return max;
  }
}
