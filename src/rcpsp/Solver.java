package rcpsp;

import java.nio.file.Paths;
import java.util.ArrayList;
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
    ArrayList<Integer> pred = new ArrayList<>();
    for (int i = 0; i < instance.successors.length; i++) {
      for (int p = 0; p < instance.successors[i].length; p++) {
        if (instance.successors[i][p] == j) {
          pred.add(i);
        }
      }
    }
    //get the max start time of all predecessors of j
    for (int i = 0; i < pred.size(); i++) {
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
  private static int[][] createInitialPopulation(Instance instance, int numberOfJobs, int populationSize, long seed, int maxMakespan) {
    int[][] population = new int[populationSize][numberOfJobs];

    //create different start orders
    for (int i = 0; i < populationSize; i++) {
      ArrayList<Integer> startOrder = new ArrayList<>();
      for (int j = 0; j < numberOfJobs; j++) {
        startOrder.add(j);
      }
      Collections.shuffle(startOrder);

      //make sure the precedence constraints are satisfied
      boolean inOrder = false;
      while (!inOrder) {
        inOrder = true;
        for (int u = 0; u < numberOfJobs; u++) {
          for (int v = 0; v < instance.successors[u].length; v++) {
            if (startOrder.indexOf(u) > startOrder.indexOf(instance.successors[u][v])) {
              inOrder = false;
              startOrder.set(startOrder.indexOf(u), instance.successors[u][v]);
              startOrder.set(startOrder.indexOf(instance.successors[u][v]), u);
            }
          }
        }
      }

      population[i] = ess(startOrder, instance, maxMakespan);
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
  private static int[] ess(ArrayList<Integer> activityList, Instance instance, int maxMakespan) {
    int[] solution = new int[instance.n()];
    //initialize
    int[][] ressourcesForEachPeriod = new int[instance.r()][maxMakespan];
    for (int j = 0; j < instance.r(); j++) {
      for (int p = 0; p < maxMakespan; p++) {
        ressourcesForEachPeriod[j][p] = instance.resources[j];
      }
    }

    //ESS
    for (int j : activityList) {
      //schedule job j
      //get earliest start time if you only look at the predecessors
      solution[j] = getEarliestStartTime(j, instance, solution);

      //schedule j by looking at ressourcesForEachPeriod to satisfy resource constraints at each time
      boolean problem = true;
      while (problem) {
        problem = false;
        for (int k = 0; k < instance.r(); k++) {
          for (int t = solution[j]; t < solution[j] + instance.processingTime[j]; t++) {
            if (ressourcesForEachPeriod[k][t] < instance.demands[j][k]) {
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
          ressourcesForEachPeriod[k][t] = ressourcesForEachPeriod[k][t] - instance.demands[j][k];
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
  private static int[] transformSolutionIntoActivityList(int[] solution, int maxMakespan) {
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

  private static boolean searchElement(ArrayList<Integer> activityList, int element) {
    for (int i = 0; i < activityList.size(); i++) {
      if (activityList.get(i) == element) return true;
    }
    return false;

  }

  private static int[] doCrossover(int[][] population, Instance instance, int maxMakespan) {
    assert population.length >= 2;

    IntPair selection = TournamentSelection.getBest(population, instance);
    int[] father = population[selection.a];
    int[] mother = population[selection.b];
    return crossover(father, mother, instance, maxMakespan);
  }

  /**
   * Execute crossover operation using two parent solutions to get a different new solution
   *
   * @param father (solution)
   * @param mother (solution
   * @return child created by crossover
   */
  private static int[] crossover(int[] father, int[] mother, Instance instance, int maxMakespan) {
    int[] fatherActivityList = transformSolutionIntoActivityList(father, maxMakespan);
    int[] motherActivityList = transformSolutionIntoActivityList(mother, maxMakespan);

    Random rand = App.getRandom();
    int crossoverPoint = rand.nextInt(instance.n());
    var childActivityList = new ArrayList<Integer>(crossoverPoint);

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

  /**
   * Change some aspect of the solution using specified probability
   *
   * @param solution
   * @return changed solution
   */
  private static void mutation(int[][] population, Instance instance) {
    int popIndex = App.getRandom().nextInt(population.length);
    int[] replacement = RandomMutation.mutate(population[popIndex], instance);
    if (replacement == null) {
      return;
    }

    IntPair details = TournamentSelection.getWorst(population, instance);
    if (makespan(replacement, instance) < details.b) {
      int indexToReplace = details.a;
      population[indexToReplace] = replacement;
    }
  }

  /**
   * Remove a set of solutions inline.
   *
   * @param population
   * @return selection
   */
  private static void selection(int[][] population, Instance instance, int[] replacement) {
    // TODO: We should use a vector instead to replace multiple entries w/o creating a copy.
    int indexToReplace = TournamentSelection.getWorst(population, instance).a;
    population[indexToReplace] = replacement;
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
    int[][] ressourcesForEachPeriod = new int[instance.r()][solutionMakespan];

    for (int j = 0; j < instance.r(); j++) {
      for (int p = 0; p < solutionMakespan; p++) {
        ressourcesForEachPeriod[j][p] = instance.resources[j];
      }
    }
    //sub all demands in the solution and check resource constraints
    for (int i = 0; i < numberOfJobs; i++) {
      for (int j = solution[i]; j < solution[i] + instance.processingTime[i]; j++) {
        for (int k = 0; k < instance.r(); k++) {
          ressourcesForEachPeriod[k][j] = ressourcesForEachPeriod[k][j] - instance.demands[i][k];
          if (ressourcesForEachPeriod[k][j] < 0) return false;
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


  private static int[] pickBestSolution(int[][] population, Instance instance) {
    int[] bestSolution = population[0];
    int bestFitness = Fitness.get(bestSolution, instance);
    for (int i = 1; i < population.length; i++) {
      int currFitness = makespan(population[i], instance);
      if (currFitness < bestFitness) {
        bestSolution = population[i];
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
    long timeLimit = Long.parseLong(args[2]);
    long startTime = System.currentTimeMillis();
    App.init(seed);

    int maxMakespan = 0;
    for (int i = 0; i < instance.n(); i++) {
      maxMakespan += instance.processingTime[i];
    }

    int[][] population = createInitialPopulation(instance, instance.n(), 30, seed, maxMakespan);

    // execute as long as the time limit is not reached
    while ((System.currentTimeMillis() - startTime) / 1000 <= timeLimit) {
      // Crossover
      int[] output = doCrossover(population, instance, maxMakespan);

      // Mutate
      mutation(population, instance);

      // Elimination (Selection)
      selection(population, instance, output);
    }

    int[] bestSolution = pickBestSolution(population, instance);
    System.out.println("Valid: " + checkSolution(bestSolution, instance));
    System.out.println("Makespan: " + makespan(bestSolution, instance));

    Io.writeSolution(bestSolution, Paths.get(args[1]));
  }
}
