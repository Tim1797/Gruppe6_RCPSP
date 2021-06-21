package rcpsp;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Random;

/**
 * Solver for the RCPSP. Solves it by using genetic algorithm
 **/
public class Solver {

  /**
   * Method calculates the earlist starttime of a job by
   * looking at the precedence constraints
   *
   * @param j
   * @param instance
   * @param solution
   * @return earliest start time of job i
   */
  private static int getEarliestStartTime(int j, Instance instance, int[] solution) {
    int max = 0;
    //find predecessors of j
    ArrayListEx<Integer> pred = new ArrayListEx<>(3);
    for (int i = 0; i < instance.successors.length; ++i) {
      for (int p = 0; p < instance.successors[i].length; ++p) {
        if (instance.successors[i][p] == j) {
          pred.add(i);
        }
      }
    }
    //get the max start time of all predecessors of j
    for (int i = 0; i < pred.size(); ++i) {
      if (solution[pred.get(i)] + instance.processingTime[pred.get(i)] > max) {
        max = solution[pred.get(i)] + instance.processingTime[pred.get(i)];
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
   * @param seed
   * @param maxMakespan
   * @return set of solutions each represented as an array of start times
   */
  private static ArrayListEx<int[]> createInitialPopulation(Instance instance, int numberOfJobs, int populationSize, long seed, int maxMakespan) {
    ArrayListEx<int[]> population = new ArrayListEx<>(populationSize);

    //create different start orders
    for (int i = 0; i < populationSize; ++i) {
      ArrayListEx<Integer> startOrder = new ArrayListEx<>(numberOfJobs);
      for (int j = 0; j < numberOfJobs; ++j) {
        startOrder.add(j);
      }
      Collections.shuffle(startOrder, App.getRandom());

      //make sure the precedence constraints are satisfied
      boolean inOrder = false;
      while (!inOrder) {
        inOrder = true;
        for (int u = 0; u < numberOfJobs; ++u) {
          for (int v = 0; v < instance.successors[u].length; ++v) {
            if (startOrder.indexOf(u) > startOrder.indexOf(instance.successors[u][v])) {
              inOrder = false;
              startOrder.set(startOrder.indexOf(u), instance.successors[u][v]);
              startOrder.set(startOrder.indexOf(instance.successors[u][v]), u);
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
  public static int[] ess(ArrayListEx<Integer> activityList, Instance instance, int maxMakespan) {
    int[] solution = new int[instance.n()];
    //initialize
    int[][] resourcesForEachPeriod = new int[instance.r()][maxMakespan];
    for (int j = 0; j < instance.r(); j++) {
      for (int p = 0; p < maxMakespan; p++) {
        resourcesForEachPeriod[j][p] = instance.resources[j];
      }
    }

    //ESS
    for (int j : activityList) {
      //schedule job j
      //get earliest start time if you only look at the predecessors
      solution[j] = getEarliestStartTime(j, instance, solution);

      //schedule j by looking at resourcesForEachPeriod to satisfy resource constraints at each time
      boolean problem = true;
      while (problem) {
        problem = false;
        for (int k = 0; k < instance.r(); k++) {
          for (int t = solution[j]; t < solution[j] + instance.processingTime[j]; t++) {
            if (resourcesForEachPeriod[k][t] < instance.demands[j][k]) {
              problem = true;
              break;
            }
          }
          if (problem) {
            solution[j]++;
          }
        }
      }

      //update resources
      for (int k = 0; k < instance.r(); k++) {
        for (int t = solution[j]; t < solution[j] + instance.processingTime[j]; t++) {
          resourcesForEachPeriod[k][t] = resourcesForEachPeriod[k][t] - instance.demands[j][k];
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
  public static int[] transformSolutionIntoActivityList(int[] solution, int maxMakespan) {
    int[] activityList = new int[solution.length];
    int[] copy = new int[solution.length];
    System.arraycopy(solution, 0, copy, 0, solution.length);

    for (int i = 0; i < copy.length; i++) {
      int minIndex = 0;
      for (int j = 0; j < copy.length; j++) {
        if (copy[j] < copy[minIndex]) {
          minIndex = j;
        }
      }
      activityList[i] = minIndex;
      copy[minIndex] = maxMakespan;
    }
    return activityList;
  }

  private static boolean searchElement(ArrayListEx<Integer> activityList, int element) {
    for (int i = 0; i < activityList.size(); i++) {
      if (activityList.get(i) == element) return true;
    }
    return false;

  }

  private static int[] doCrossover(ArrayListEx<int[]> population, Instance instance, int maxMakespan) {
    assert population.size() >= 2;
    Random rand = App.getRandom();
    IntPair selection = TournamentSelection.getBest(population, instance);
    int[] father = population.get(selection.a);
    int[] mother = population.get(selection.b);

    final int crossoverChoice = 1;

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
  private static int[] onePointCO(int[] father, int[] mother, Instance instance, int maxMakespan) {
    int[] fatherActivityList = transformSolutionIntoActivityList(father, maxMakespan);
    int[] motherActivityList = transformSolutionIntoActivityList(mother, maxMakespan);

    Random rand = App.getRandom();
    int crossoverPoint = rand.nextInt(instance.n());
    var childActivityList = new ArrayListEx<Integer>(instance.n());

    for (int i = 0; i < crossoverPoint; i++) {
      childActivityList.add(fatherActivityList[i]);
    }
    for (int i = 0; i < instance.n(); i++) {
      if (!searchElement(childActivityList, motherActivityList[i])) {
        childActivityList.add(motherActivityList[i]);
      }
    }
    return ess(childActivityList, instance, maxMakespan);
  }

  private static int[] twoPointCO(int[] father, int[] mother, Instance instance, int maxMakespan) {
    int[] fatherActivityList = transformSolutionIntoActivityList(father, maxMakespan);
    int[] motherActivityList = transformSolutionIntoActivityList(mother, maxMakespan);

    Random rand = App.getRandom();
    int firstPoint = rand.nextInt(instance.n() - 1);
    int secondPoint = rand.nextInt(instance.n() - firstPoint + 1) + firstPoint;
    ArrayListEx<Integer> child = new ArrayListEx<>(instance.n());

    for (int i = 0; i < firstPoint; i++) {
      child.add(motherActivityList[i]);
    }
    for (int i = firstPoint; i < secondPoint; i++) {
      //fill with not used job number
      child.add(instance.n() + 10);
    }
    for (int i = secondPoint; i < instance.n(); i++) {
      child.add(motherActivityList[i]);
    }

    for (int i = firstPoint; i < secondPoint; i++) {
      for (int j = 0; j < instance.n(); j++) {
        if (!searchElement(child, fatherActivityList[j])) {
          child.set(i, fatherActivityList[j]);
        }
      }

    }

    //make sure the precedence constraints are met
    boolean inOrder = false;
    while (!inOrder) {
      inOrder = true;
      for (int u = 0; u < instance.n(); u++) {
        for (int v = 0; v < instance.successors[u].length; v++) {
          if (child.indexOf(u) > child.indexOf(instance.successors[u][v])) {
            inOrder = false;
            child.set(child.indexOf(u), instance.successors[u][v]);
            child.set(child.indexOf(instance.successors[u][v]), u);
          }
        }
      }
    }
    return ess(child, instance, maxMakespan);
  }

  private static int[] uniformCO(int[] father, int[] mother, Instance instance, int maxMakespan) {
    int[] fatherActivityList = transformSolutionIntoActivityList(father, maxMakespan);
    int[] motherActivityList = transformSolutionIntoActivityList(mother, maxMakespan);

    Random rand = App.getRandom();
    ArrayListEx<Integer> child = new ArrayListEx<>(instance.n());

    for (int i = 0; i < instance.n(); i++) {
      if (rand.nextInt(2) == 0) {
        for (int j = 0; j < instance.n(); j++) {
          if (!searchElement(child, fatherActivityList[j])) {
            child.add(fatherActivityList[j]);
          }
        }
      } else {
        for (int j = 0; j < instance.n(); j++) {
          if (!searchElement(child, motherActivityList[j])) {
            child.add(motherActivityList[j]);
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
  private static void selection(ArrayListEx<int[]> population, Instance instance) {
    population.sort((sol1, sol2) -> {
      int makespanSol1 = makespan(sol1, instance);
      int makespanSol2 = makespan(sol2, instance);
      return makespanSol2 - makespanSol1;
    });

    int upperBound = population.size() / 2;
    population.removeRange(0, upperBound);
  }

  /**
   * Method calculates the makespan of the solution by searching for the
   * maximum end time of a job
   *
   * @param solution
   * @param instance
   * @return makespan
   */
  private static int makespan(int[] solution, Instance instance) {
    return Fitness.get(solution, instance);
  }


  /**
   * Method checks of a created solution is valid by checking the precedence constraints
   * and the resource constraints
   *
   * @param solution
   * @param instance
   * @return
   */
  public static boolean checkSolution(int[] solution, Instance instance) {
    int numberOfJobs = instance.n();
    int solutionMakespan = makespan(solution, instance);

    //initialize
    int[][] resourcesForEachPeriod = new int[instance.r()][solutionMakespan];

    for (int j = 0; j < instance.r(); j++) {
      for (int p = 0; p < solutionMakespan; p++) {
        resourcesForEachPeriod[j][p] = instance.resources[j];
      }
    }
    //sub all demands in the solution and check resource constraints
    for (int i = 0; i < numberOfJobs; i++) {
      for (int j = solution[i]; j < solution[i] + instance.processingTime[i]; j++) {
        for (int k = 0; k < instance.r(); k++) {
          resourcesForEachPeriod[k][j] = resourcesForEachPeriod[k][j] - instance.demands[i][k];
          if (resourcesForEachPeriod[k][j] < 0) return false;
        }
      }
    }
    //check successor constraints
    for (int i = 0; i < numberOfJobs; i++) {
      for (int j = 0; j < instance.successors[i].length; j++) {
        if (solution[i] + instance.processingTime[i] > solution[instance.successors[i][j]]) {
          return false;
        }
      }
    }
    return true;
  }


  private static int[] pickBestSolution(ArrayListEx<int[]> population, Instance instance) {
    int[] bestSolution = population.get(0);
    int bestFitness = Fitness.get(bestSolution, instance);
    for (int i = 1; i < population.size(); i++) {
      int currFitness = makespan(population.get(i), instance);
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

    long seed = Long.parseLong(args[3]);
    int sizeOfInitialPop = 30;
    long timeLimit = Long.parseLong(args[2]);
    long startTime = System.currentTimeMillis();
    App.init(seed);

    int maxMakespan = 0;
    for (int i = 0; i < instance.n(); i++) {
      maxMakespan += instance.processingTime[i];
    }

    int debugIterations = 0; // #DEBUG

    ArrayListEx<int[]> population = createInitialPopulation(instance, instance.n(), sizeOfInitialPop, seed, maxMakespan);
    // execute as long as the time limit is not reached
    while ((System.currentTimeMillis() - startTime) / 1000 <= timeLimit) {
      // Crossover
      int[] child = doCrossover(population, instance, maxMakespan);

      // Mutate
      child = RandomMutation.mutate(child, instance, maxMakespan);
      population.add(child);

      // Elimination (Selection)
      if (population.size() > 4 * sizeOfInitialPop) {
        selection(population, instance);
      }

      ++debugIterations; // #DEBUG
    }

    int[] bestSolution = pickBestSolution(population, instance);
    System.out.println("Valid: " + checkSolution(bestSolution, instance));
    System.out.println("Makespan: " + makespan(bestSolution, instance));

    System.out.println("\nIterations: " + debugIterations); // #DEBUG

    Io.writeSolution(bestSolution, Paths.get(args[1]));
  }
}
