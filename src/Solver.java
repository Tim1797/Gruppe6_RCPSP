import java.nio.file.Paths;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


/**
 Solver for the RCPSP. Solves it by using genetic algorithm
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
    private static int getEarliestStartTime(int j, Instance instance, int[] solution){
        int max = 0;
        //find predecessors of j
        ArrayList<Integer> pred = new ArrayList<>();
        for(int i=0; i<instance.successors.length; i++){
            for(int p=0; p<instance.successors[i].length; p++){
                if(instance.successors[i][p] == j){
                    pred.add(i);
                }
            }
        }
        //get the max start time of all predecessors of j
        for(int i=0; i<pred.size(); i++){

            if(solution[pred.get(i)]+instance.processingTime[pred.get(i)] > max){
                max = solution[pred.get(i)]+instance.processingTime[pred.get(i)];
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
     * @return activityList
     */
    private static int[][] createInitialPopulation(Instance instance, int numberOfJobs, int populationSize){

        int[][] population = new int [populationSize][numberOfJobs];

        //create different start orders
        for(int i=0; i<populationSize; i++){
            ArrayList<Integer> startOrder = new ArrayList<>();

            for(int j=0; j<numberOfJobs; j++){
                startOrder.add(j);
            }
            Collections.shuffle(startOrder);

            //make sure the precedence constraints are satisfied 
            boolean inOrder = false;
            while(inOrder == false){
                inOrder = true;
                for(int u=0; u<numberOfJobs; u++){
                    for(int v=0; v<instance.successors[u].length;v++){
                        if(startOrder.indexOf(u) > startOrder.indexOf(instance.successors[u][v])){
                            inOrder = false;
                            startOrder.set(startOrder.indexOf(u), instance.successors[u][v]);
                            startOrder.set(startOrder.indexOf(instance.successors[u][v]), u);
                        }
                    }
                }
            }

            //convert ArrayList<Integer> to int[]
            int[] startOrderArray = new int[startOrder.size()];
            for(int k = 0; k < startOrder.size(); k++) {
                startOrderArray[k] = startOrder.get(k).intValue();
            }

            //save activity list
            population[i] = startOrderArray;
        }


        //return population as activity list
        return population;

    }

    /**
     * Execute Earliest Start Schedule to get solution with start times
     * from an activity list
     *
     * @param activityList
     * @param instance
     * @param maxMakespan
     * @return solution
     */
    private static int[] ess(int[] activityList, Instance instance, int maxMakespan) {
        int[] solution = new int[instance.n()];
        //initialize  
        int[][] ressourcesForEachPeriod = new int[instance.r()][maxMakespan];
        for(int j=0; j<instance.r(); j++){
            for(int p=0; p<maxMakespan; p++){
                ressourcesForEachPeriod[j][p] = instance.resources[j];
            }
        }

        //ESS              
        for(int j : activityList){
            //schedule job j
            //get earliest start time if you only look at the predecessors             
            solution[j] = getEarliestStartTime(j, instance, solution);

            //schedule j by looking at ressourcesForEachPeriod to satisfy resource constraints at each time
            boolean problem = true;
            while(problem) {
                problem = false;
                for(int k=0; k<instance.r(); k++){
                    for(int t=solution[j]; t<solution[j]+instance.processingTime[j]; t++){
                        if(ressourcesForEachPeriod[k][t]<instance.demands[j][k]) {
                            problem = true;
                            break;
                        }
                    }

                    if(problem){
                        solution[j]++;
                    }

                }
            }

            //update resources
            for(int k=0; k<instance.r(); k++){
                for(int t=solution[j]; t<solution[j]+instance.processingTime[j]; t++){
                    ressourcesForEachPeriod[k][t] = ressourcesForEachPeriod[k][t] - instance.demands[j][k];
                }
            }
        }
        return solution;
    }

    /**
     * Check if activitiyList contains the searched element
     *
     * @param activityList
     * @param element
     * @return
     */
    private static boolean searchElement(int[] activityList, int element) {
        for(int i=0; i<activityList.length; i++) {
            if(activityList[i] == element) return true;
        }
        return false;

    }

    /**
     * Execute crossover operation using two parent solutions to get a different new solution
     *
     * @param fatherActivityList
     * @param motherActivityList
     * @param instance
     * @param rand
     * @return child created by crossover
     */
    private static int[] crossover(int[] fatherActivityList, int[] motherActivityList, Instance instance, Random rand) {
        int crossoverPoint = rand.nextInt(instance.n() - 2) + 1;
        int[] childActivityList = new int[fatherActivityList.length];

        for(int i=0; i<crossoverPoint; i++) {
            childActivityList[i] = fatherActivityList[i];
        }
        int offs = crossoverPoint;
        for(int j=1; j<instance.n(); j++) {
            if(!searchElement(childActivityList, motherActivityList[j])) {
                childActivityList[offs] = motherActivityList[j];
                offs++;
            }
        }

        return childActivityList;

    }

    /**
     * Mutate given solution to an Napi neighbor
     *
     * @param solution
     * @param instance
     * @param rand
     * @param maxMakespan
     * @return changed solution
     */
    private static int[] mutation(int[] solution, Instance instance, Random rand, int maxMakespan, double probability){
        int[] tempSolution = Arrays.copyOf(solution, solution.length);
        int randInt = rand.nextInt(100) + 1; //random number between 1 and 100

        if (randInt <= probability) {
            boolean feasible = false;
            while (!feasible) {
                tempSolution = Arrays.copyOf(solution, solution.length);
                //mutation point between 1 and n-1
                int mutationPoint = rand.nextInt(instance.n() - 2) + 1;
                int temp = tempSolution[mutationPoint];
                tempSolution[mutationPoint] = tempSolution[mutationPoint + 1];
                tempSolution[mutationPoint + 1] = temp;
                feasible = checkSolution(tempSolution, instance, maxMakespan);
            }
        }

        return tempSolution;
    }

    /**
     * Select an index of an activityList for roulette wheel selection
     *
     * @param population
     * @param instance
     * @param rand
     * @param maxMakespan
     * @param fitnessSum
     * @param highestMakespan
     * @return index of selection
     */
    private static int selectRandom(int[][] population, Instance instance, Random rand, int maxMakespan, int fitnessSum, int highestMakespan) {
        int randNumber = rand.nextInt(fitnessSum) + 1;

        int cumulativeMakespan = 0;
        int j = 0;
        while (cumulativeMakespan < randNumber) {
            int mksp = makespan(population[j], instance, maxMakespan);
            cumulativeMakespan += (highestMakespan + 1) - mksp;
            j++;
        }
        j--;
        return j;
    }

    /**
     * Roulette wheel selection of crossover parents
     *
     * @param population
     * @param instance
     * @param rand
     * @param maxMakespan
     * @return selections
     */
    private static int[][] selection(int[][] population, Instance instance, Random rand, int maxMakespan){
        //compute highest makespan
        int highestMakespan = makespan(population[0], instance, maxMakespan);
        for (int i = 0; i < population.length; i++) {
            int mksp = makespan(population[i], instance, maxMakespan);
            if (highestMakespan < mksp) {
                highestMakespan = mksp;
            }
        }

        //compute sum of fitness (highestMakespan + 1 - mksp) values
        int fitnessSum = 0;
        for (int i = 0; i < population.length; i++) {
            int mksp = makespan(population[i], instance, maxMakespan);
            fitnessSum += (highestMakespan + 1) - mksp;
        }

        //select two parent indices
        int firstParentIndex = selectRandom(population, instance, rand, maxMakespan, fitnessSum, highestMakespan);
        int secondParentIndex = selectRandom(population, instance, rand, maxMakespan, fitnessSum, highestMakespan);
        while (secondParentIndex == firstParentIndex) {
            secondParentIndex = selectRandom(population, instance, rand, maxMakespan, fitnessSum, highestMakespan);
        }

        int[][] selection = new int[2][population[0].length];
        selection[0] = population[firstParentIndex];
        selection[1] = population[secondParentIndex];

        return selection;
    }

    /**
     * Method calculates the makespan of the activityList by searching for the
     * maximum end time of a job
     *
     * @param activityList
     * @param instance
     * @param maxMakespan
     * @return makespan
     */
    private static int makespan(int[] activityList, Instance instance, int maxMakespan){
        int max = 0;
        int[] solution = ess(activityList, instance, maxMakespan);
        for(int i=0; i<solution.length; i++){
            if(solution[i] + instance.processingTime[i] > max){
                max = solution[i] + instance.processingTime[i];
            }
        }
        return max;
    }


    /**
     * Method checks if an activityList is valid by checking the precedence constraints
     * and the resource constraints
     *
     * @param activityList
     * @param instance
     * @param maxMakespan
     * @return valid status
     */
    private static boolean checkSolution(int[] activityList, Instance instance, int maxMakespan){
        int numberOfJobs = instance.n();
        int solutionMakespan = makespan(activityList, instance, maxMakespan);

        int[] solution = ess(activityList, instance, maxMakespan);

        //initialize              
        int[][] ressourcesForEachPeriod = new int[instance.r()][solutionMakespan];

        for(int j=0; j<instance.r(); j++){
            for(int p=0; p<solutionMakespan; p++){
                ressourcesForEachPeriod[j][p] = instance.resources[j];
            }
        }
        //sub all demands in the solution and check resource constraints
        for(int i=0; i<numberOfJobs; i++){
            for(int j=solution[i]; j<solution[i] + instance.processingTime[i]; j++){
                for(int k=0; k<instance.r(); k++){
                    ressourcesForEachPeriod[k][j] = ressourcesForEachPeriod[k][j] - instance.demands[i][k];
                    if(ressourcesForEachPeriod[k][j] < 0) return false;
                }
            }
        }
        //check successor constraints              
        for(int i=0; i<numberOfJobs; i++){
            for(int j=0; j<instance.successors[i].length; j++){
                if(solution[i]+instance.processingTime[i] > solution[instance.successors[i][j]]){
                    return false;
                }
            }
        }
        return true;
    }


    public static void main(String[] args) {

        if (args.length != 4) {
            System.out.println("usage: java Solver <instance-path> <solution-path> <time-limit> <seed>");
            return;
        }

        final int populationSize = 100;
        //mutation probability in percent
        final double mutationProbability = 5.0;

        final String path = args[0];
        final Instance instance = Io.readInstance(Paths.get(path));

        long seed = Long.parseLong(args[3]);
        Random rand = new Random(seed);;
        long timeLimit = Long.parseLong(args[2]);
        long startTime = System.currentTimeMillis();

        int maxMakespan = 0;
        for(int i=0; i<instance.n(); i++){
            maxMakespan = maxMakespan + instance.processingTime[i];
        }

        //1. create initial population
        int[][] population = createInitialPopulation(instance, instance.n(), populationSize);

        System.out.println("Check initial population: ");
        boolean found_illegal = false;
        for (int i = 0; i < population.length; i++) {
            if(!checkSolution(population[i], instance, maxMakespan)) {
                found_illegal = true;
            }
        }
        System.out.println("Found illegal initial population? : " + found_illegal);

        //execute as long as the time limit is not reached
        while((System.currentTimeMillis() - startTime)/1000 <= timeLimit) {
            //2. selection
            int[][] selecResult = selection(population, instance, rand, maxMakespan);

            //3. crossover
            int[] indiv1 = selecResult[0];
            int[] indiv2 = selecResult[1];
            int[] child = crossover(indiv1, indiv2, instance, rand);

            //4. mutation
            int[] mutChild = mutation(child, instance, rand, maxMakespan, mutationProbability);

            //5. replace unfittest solution with child
            int highestMakespan = 0;
            int hmIndex = -1;
            for (int i = 0; i < population.length; i++) {
                int mksp = makespan(population[i], instance, maxMakespan);
                if (highestMakespan < mksp) {
                    highestMakespan = mksp;
                    hmIndex = i;
                }
            }
            population[hmIndex] = mutChild;

        }

        //stores the best solution inside the population
        int[] bestSolution = null;

        // compute fitness function
        bestSolution = population[0];
        for (int i = 0; i < population.length; i++) {
            if (makespan(population[i], instance, maxMakespan) < makespan(bestSolution, instance, maxMakespan)) {
                bestSolution = population[i];
            }
        }

        System.out.print("Best Solution: ");
        for(int l = 0; l < bestSolution.length; l++){
            System.out.print(bestSolution[l] + ",");
        }
        System.out.print(" => " + makespan(bestSolution, instance, maxMakespan));
        System.out.print(" (" + checkSolution(bestSolution,instance, maxMakespan) + ")");
        System.out.println();

        Io.writeSolution(bestSolution, Paths.get(args[1]));
    }
}
