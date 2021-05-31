package rcpsp;

import java.util.Random;
import java.util.Arrays;


public class RandomMutation {
  private static final double Probability = 0.90;

  public static int[] mutate(int[] solution, Instance instance, int maxMakespan) {
   
    Random rng = App.getRandom();
    if (rng.nextDouble() > Probability) {
      return null;
    }
    
    int[] tempSolution = Arrays.copyOf(solution, solution.length);
    int randInt = rng.nextInt(100) + 1; //random number between 1 and 100
    
    boolean feasible = false;
    while (!feasible) {
        tempSolution = Arrays.copyOf(solution, solution.length);
        //mutation point between 1 and n-1
        int mutationPoint = rng.nextInt(instance.n() - 2) + 1;
        int temp = tempSolution[mutationPoint];
        tempSolution[mutationPoint] = tempSolution[mutationPoint + 1];
        tempSolution[mutationPoint + 1] = temp;
        feasible = Solver.checkSolution(tempSolution, instance);
        
    }

    return tempSolution;        

  
  }
}