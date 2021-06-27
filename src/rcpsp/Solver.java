package rcpsp;

import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;

/**
 * Solver for the RCPSP. Solves it by using genetic algorithm
 **/
public class Solver {
  private static final Map<Integer, ArrayList<Integer>> predecessorCache = new HashMap<>();

  /**
   * Method calculates the earlist starttime of a job by
   * looking at the precedence constraints
   *
   * @param jobNr
   * @param instance
   * @param solution
   * @return earliest start time of job i
   */
  private static int getEarliestStartTime(int jobNr, Instance instance, Solution solution) {
    ArrayList<Integer> pred = predecessorCache.get(jobNr);
    if (pred == null) {
      // find predecessors of j
      pred = new ArrayList<>(4);
      for (int i = 0; i < instance.successors.length; ++i) {
        for (int p = 0; p < instance.successors[i].length; ++p) {
          if (instance.successors[i][p] == jobNr) {
            pred.add(i);
          }
        }
      }
      predecessorCache.put(jobNr, pred);
    }

    // get the max start time of all predecessors of j
    int max = 0;
    for (int i = 0; i < pred.size(); ++i) {
      int curr = solution.get(pred.get(i)) + instance.processingTime[pred.get(i)];
      if (curr > max) {
        max = curr;
      }
    }
    return max;
  }

  /**
   * Creates initial population by executing Earliest Start Schedule with different activity orders
   *
   * @param instance
   * @param numberOfJobs
   * @param populationSize
   * @param maxMakespan
   * @return set of solutions each represented as an array of start times
   */
  private static ArrayListEx<Solution> createInitialPopulation(Instance instance, int numberOfJobs, int populationSize, int maxMakespan) {
    var population = new ArrayListEx<Solution>(populationSize);

    // create different start orders
    for (int i = 0; i < populationSize; ++i) {
      var startOrder = new ArrayListEx<Integer>(numberOfJobs);
      for (int j = 0; j < numberOfJobs; ++j) {
        startOrder.add(j);
      }
      Collections.shuffle(startOrder, App.getRandom());

      // make sure the precedence constraints are satisfied
      boolean inOrder = false;
      while (!inOrder) {
        inOrder = true;
        for (int u = 0; u < numberOfJobs; ++u) {
          for (int v = 0; v < instance.successors[u].length; ++v) {
            int indexU = startOrder.indexOf(u);
            int indexSucc = startOrder.indexOf(instance.successors[u][v]);
            if (indexU > indexSucc) {
              inOrder = false;
              startOrder.set(indexU, instance.successors[u][v]);
              startOrder.set(indexSucc, u);
            }
          }
        }
      }

      population.add(ess(startOrder, instance, maxMakespan));
    }
    return population;
  }

  /**
   * Execute Earliest Start Schedule to get solution with start times
   * from an activity list
   *
   * @param activityList
   * @return
   */
  public static Solution ess(ArrayListEx<Integer> activityList, Instance instance, int maxMakespan) {
    Solution solution = new Solution(instance.n(), instance);

    // initialize
    int[][] resourcesForEachPeriod = new int[instance.r()][maxMakespan];
    for (int j = 0; j < instance.r(); ++j) {
      for (int p = 0; p < maxMakespan; ++p) {
        resourcesForEachPeriod[j][p] = instance.resources[j];
      }
    }

    // ESS
    for (int j : activityList) {
      // schedule job j
      // get earliest start time if you only look at the predecessors
      solution.set(j, getEarliestStartTime(j, instance, solution));

      // schedule j by looking at resourcesForEachPeriod to satisfy resource constraints at each time
      boolean problem = true;
      while (problem) {
        problem = false;
        for (int k = 0; k < instance.r(); ++k) {
          int limit = solution.get(j) + instance.processingTime[j];
          for (int t = solution.get(j); t < limit; ++t) {
            if (resourcesForEachPeriod[k][t] < instance.demands[j][k]) {
              problem = true;
              break;
            }
          }
          if (problem) {
            int val = solution.get(j);
            solution.set(j, ++val);
          }
        }
      }

      // update resources
      for (int k = 0; k < instance.r(); ++k) {
        int limit = solution.get(j) + instance.processingTime[j];
        for (int t = solution.get(j); t < limit; ++t) {
          resourcesForEachPeriod[k][t] -= instance.demands[j][k];
        }
      }
    }



    return solution;
  }

  /**
   * Transforms the solution, which holds the start times of each job into
   * an activity list to perform crossover operation
   *
   * @param solution
   * @return solution
   */
  public static int[] transformSolutionIntoActivityList(Solution solution, int maxMakespan) {
    int[] copy = new int[solution.size()];
    System.arraycopy(solution.getDataUnsafe(), 0, copy, 0, solution.size());

    int[] activityList = new int[solution.size()];
    for (int i = 0; i < copy.length; ++i) {
      int minIndex = 0;
      for (int j = 0; j < copy.length; ++j) {
        if (copy[j] < copy[minIndex]) {
          minIndex = j;
        }
      }
      activityList[i] = minIndex;
      copy[minIndex] = maxMakespan;
    }
    return activityList;
  }

  private static Solution doCrossover(ArrayListEx<Solution> population, Instance instance, int maxMakespan, int crossoverChoice) {
    assert population.size() >= 2;
    IntPair selection = TournamentSelection.getBest(population, instance);
    Solution father = population.get(selection.a);
    Solution mother = population.get(selection.b);

    // final int crossoverChoice = 1;

    if (crossoverChoice == 0) {
      return onePointCO(father, mother, instance, maxMakespan);
    } else if (crossoverChoice == 1) {
      return twoPointCO(father, mother, instance, maxMakespan);
    } else {
      return uniformCO(father, mother, instance, maxMakespan);
    }
  }

  /**
   * Execute crossover operation using two parent solutions to get a different new solution
   *
   * @param father (solution)
   * @param mother (solution)
   * @return child created by crossover
   */
  private static Solution onePointCO(Solution father, Solution mother, Instance instance, int maxMakespan) {
    int[] fatherActivityList = transformSolutionIntoActivityList(father, maxMakespan);
    int[] motherActivityList = transformSolutionIntoActivityList(mother, maxMakespan);

    Random rand = App.getRandom();
    int crossoverPoint = rand.nextInt(instance.n());
    var child = new ArrayListEx<Integer>(instance.n());
    var childCache = new HashSet<Integer>(crossoverPoint);

    for (int i = 0; i < crossoverPoint; ++i) {
      int value = fatherActivityList[i];
      child.add(value);
      childCache.add(value);
    }
    for (int i = 0; i < instance.n(); ++i) {
      if (!childCache.contains(motherActivityList[i])) {
        int value = motherActivityList[i];
        child.add(value);
        childCache.add(value);
      }
    }
    return ess(child, instance, maxMakespan);
  }

  private static Solution twoPointCO(Solution father, Solution mother, Instance instance, int maxMakespan) {
    int[] fatherActivityList = transformSolutionIntoActivityList(father, maxMakespan);
    int[] motherActivityList = transformSolutionIntoActivityList(mother, maxMakespan);

    Random rand = App.getRandom();
    int firstPoint = rand.nextInt(instance.n() - 1);
    int secondPoint = rand.nextInt(instance.n() - firstPoint + 1) + firstPoint;
    var child = new ArrayListEx<Integer>(instance.n());
    var childCache = new HashSet<Integer>(instance.n());
    int magicNumber = instance.n() + 10;

    for (int i = 0; i < firstPoint; ++i) {
      child.add(motherActivityList[i]);
      childCache.add(motherActivityList[i]);
    }
    for (int i = firstPoint; i < secondPoint; ++i) {
      // fill with not used job number
      child.add(magicNumber);
    }
    for (int i = secondPoint; i < instance.n(); ++i) {
      child.add(motherActivityList[i]);
      childCache.add(motherActivityList[i]);
    }

    for (int i = firstPoint; i < secondPoint; ++i) {
      for (int j = 0; j < instance.n(); ++j) {
        if (!childCache.contains(fatherActivityList[j])) {
          childCache.remove(child.get(i));

          int value = fatherActivityList[j];
          child.set(i, value);
          childCache.add(value);
        }
      }
    }

    // make sure the precedence constraints are met
    boolean inOrder = false;
    while (!inOrder) {
      inOrder = true;
      for (int u = 0; u < instance.n(); ++u) {
        int len = instance.successors[u].length;
        for (int v = 0; v < len; ++v) {
          // NB: Manually search both indices.
          int indexU = -1;
          int indexSucc = -1;
          int succVal = instance.successors[u][v];
          for (int i = 0; i < child.size(); ++i) {
            int val = child.get(i);
            if (val == u) {
              indexU = i;
              break;
            } else if (val == succVal) {
              indexSucc = i;
            }
          }

          if (indexSucc != -1) {
            assert indexU != -1 && indexU > indexSucc;
            inOrder = false;
            child.set(indexU, succVal);
            child.set(indexSucc, u);
          }
//          // Original code:
//          int indexU = child.indexOf(u);
//          int indexSucc = child.indexOf(instance.successors[u][v]);
//          if (indexU > indexSucc) {
//            inOrder = false;
//            child.set(indexU, instance.successors[u][v]);
//            child.set(indexSucc, u);
//          }
        }
      }
    }
    return ess(child, instance, maxMakespan);
  }

  private static Solution uniformCO(Solution father, Solution mother, Instance instance, int maxMakespan) {
    int[] fatherActivityList = transformSolutionIntoActivityList(father, maxMakespan);
    int[] motherActivityList = transformSolutionIntoActivityList(mother, maxMakespan);

    Random rand = App.getRandom();
    var child = new ArrayListEx<Integer>(instance.n());
    var childCache = new HashSet<Integer>(instance.n());

    for (int i = 0; i < instance.n(); ++i) {
      if (rand.nextBoolean()) {
        for (int j = 0; j < instance.n(); ++j) {
          if (!childCache.contains(fatherActivityList[j])) {
            int value = fatherActivityList[j];
            child.add(value);
            childCache.add(value);
          }
        }
      } else {
        for (int j = 0; j < instance.n(); ++j) {
          if (!childCache.contains(motherActivityList[j])) {
            int value = motherActivityList[j];
            child.add(value);
            childCache.add(value);
          }
        }
      }
    }
    return ess(child, instance, maxMakespan);
  }

  /**
   * Remove a set of solutions inline.
   *
   * @param population
   * @return selection
   */
  private static void selection(ArrayListEx<Solution> population) {
    population.sort((sol1, sol2) -> {
      int makespanSol1 = sol1.getMakespan();
      int makespanSol2 = sol2.getMakespan();
      return makespanSol2 - makespanSol1;
    });

    // Keep elitist solutions
    int upperBound = population.size() / 2;
    population.removeRange(0, upperBound);

    // Remove duplicates
    Random rng = App.getRandom();
    var cache = new HashSet<Integer>();
    for (int i = 0; i < population.size(); ++i) {
      int h  = Arrays.hashCode(population.get(i).getDataUnsafe());
      if (cache.contains(h)) {
        if (population.size() > 2 && rng.nextDouble() > 0.2) {
          population.remove(i);
          --i;
        }
      } else {
        cache.add(h);
      }
    }

    // Redistribute
    Collections.shuffle(population, rng);
  }

  /**
   * Method checks of a created solution is valid by checking the precedence constraints
   * and the resource constraints
   *
   * @param solution
   * @param instance
   * @return
   */
  public static boolean checkSolution(Solution solution, Instance instance) {
    int numberOfJobs = instance.n();
    int solutionMakespan = solution.getMakespan();

    // initialize
    int[][] resourcesForEachPeriod = new int[instance.r()][solutionMakespan];
    for (int j = 0; j < instance.r(); ++j) {
      for (int p = 0; p < solutionMakespan; ++p) {
        resourcesForEachPeriod[j][p] = instance.resources[j];
      }
    }

    // sub all demands in the solution and check resource constraints
    for (int i = 0; i < numberOfJobs; ++i) {
      int limit = solution.get(i) + instance.processingTime[i];
      for (int j = solution.get(i); j < limit; ++j) {
        for (int k = 0; k < instance.r(); ++k) {
          resourcesForEachPeriod[k][j] -= instance.demands[i][k];
          if (resourcesForEachPeriod[k][j] < 0) {
            return false;
          }
        }
      }
    }

    // check successor constraints
    for (int i = 0; i < numberOfJobs; ++i) {
      for (int j = 0; j < instance.successors[i].length; ++j) {
        if (solution.get(i) + instance.processingTime[i] > solution.get(instance.successors[i][j])) {
          return false;
        }
      }
    }
    return true;
  }

  private static Solution pickBestSolution(ArrayListEx<Solution> population) {
    Solution bestSolution = population.get(0);
    int bestFitness = bestSolution.getMakespan();
    for (int i = 1; i < population.size(); ++i) {
      int currFitness = population.get(i).getMakespan();
      if (currFitness < bestFitness) {
        bestSolution = population.get(i);
        bestFitness = currFitness;
      }
    }
    return bestSolution;
  }

  public static void main(String[] args) {
    if (args.length != 4) {
      System.out.println("usage: java Solver <instance-path> <solution-path> <time-limit> <seed>");
      return;
    }

    final String path = args[0];
    final Instance instance = Io.readInstance(Paths.get(path));

    final long seed = Long.parseLong(args[3]);
    final int sizeOfInitialPop = 30;
    final int eliminationThreshold = 10 * sizeOfInitialPop;
    final long timeLimit = Long.parseLong(args[2]) * 1000;
    final long startTime = System.currentTimeMillis();
    App.init(seed);

    int maxMakespan = 0;
    for (int i = 0; i < instance.n(); ++i) {
      maxMakespan += instance.processingTime[i];
    }

    // int debugIterations = 0; // #DEBUG

    Random rng = App.getRandom();
    ArrayListEx<Solution> population = createInitialPopulation(instance, instance.n(), sizeOfInitialPop, maxMakespan);

    // execute as long as the time limit is not reached
    while ((System.currentTimeMillis() - startTime) <= timeLimit) {
      // Crossover
      Solution child = doCrossover(population, instance, maxMakespan, 1);

      if (rng.nextDouble() > 0.9) {
        Solution child2 = doCrossover(population, instance, maxMakespan, 2);
        population.add(child2);
      }

      // Mutate
      child = RandomMutation.mutate(child, instance, maxMakespan);
      population.add(child);

      // Elimination (Selection)
      if (population.size() > eliminationThreshold) {
        selection(population);
      }

      // ++debugIterations; // #DEBUG
    }

    Solution bestSolution = pickBestSolution(population);
    System.out.println("Valid: " + checkSolution(bestSolution, instance));
    System.out.println("Makespan: " + bestSolution.getMakespan());

    // System.out.println("\nIterations: " + debugIterations); // #DEBUG

    Io.writeSolution(bestSolution.getDataUnsafe(), Paths.get(args[1]));
  }
}
