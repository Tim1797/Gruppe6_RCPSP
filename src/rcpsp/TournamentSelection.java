package rcpsp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TournamentSelection {
  private static final int TournamentBestSize = 4;
  private static final int TournamentWorstSize = 3;

  /// Select n random indices (Floyd's random sampling algorithm).
  private static Set<Integer> randomSample(int totalSize, int n) {
    Random rng = App.getRandom();
    var sample = new HashSet<Integer>(n);
    for (int i = totalSize - n; i < totalSize; ++i) {
      int index = rng.nextInt(i);
      if (sample.contains(index)) {
        sample.add(i);
      } else {
        sample.add(index);
      }
    }
    return sample;
  }

  /// Returns the best and second best solution based on a fixed tournament.
  public static IntPair getBest(ArrayList<Solution> population, Instance instance) {
    int bestFitness = Integer.MAX_VALUE;
    int bestIndex = -1;
    int secondBestFitness = Integer.MAX_VALUE;
    int secondBestIndex = -1;
    Set<Integer> indices = randomSample(population.size(), TournamentBestSize);
    for (int index : indices) {
      int fitness = population.get(index).getMakespan();
      if (fitness < bestFitness) {
        secondBestFitness = bestFitness;
        secondBestIndex = bestIndex;
        bestFitness = fitness;
        bestIndex = index;
      } else if (fitness < secondBestFitness) {
        secondBestFitness = fitness;
        secondBestIndex = index;
      }
    }
    return new IntPair(bestIndex, secondBestIndex);
  }

  /// Returns the worst solution based on a fixed tournament.
  public static IntPair getWorst(ArrayList<Solution> population, Instance instance) {
    int worstFitness = Integer.MIN_VALUE;
    int worstIndex = -1;
    Set<Integer> indices = randomSample(population.size(), TournamentWorstSize);
    for (int index : indices) {
      int fitness =  population.get(index).getMakespan();
      if (fitness > worstFitness) {
        worstFitness = fitness;
        worstIndex = index;
      }
    }
    return new IntPair(worstIndex, worstFitness);
  }
}
