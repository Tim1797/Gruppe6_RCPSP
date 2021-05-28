package rcpsp;

public class RandomSelection {
  public static IntPair selection(int[][] population, Instance instance) {
    int i = App.getRandom().nextInt(population.length);
    int j = App.getRandom().nextInt(population.length);
    if (i == j) {
      if (i == 0) {
        j = population.length - 1;
      } else {
        j = 0;
      }
    }
    return new IntPair(i, j);
  }
}
