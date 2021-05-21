
import java.nio.file.Paths;

public class Example {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("usage: java Example <instance-path>");
            return;
        }
        
        final String path = args[0];

        final Instance instance = Io.readInstance(Paths.get(path));

        for (int i = 0; i < instance.r(); i++) {
            System.out.printf("res %d has %d available units\n", i, instance.resources[i]);
        }

        System.out.println();

        for (int i = 0; i < instance.n(); i++) {

            System.out.printf("i = %d\n", i);
            System.out.printf("  pi = %d\n", instance.processingTime[i]);

            System.out.print("  R[i,k] = ");

            for (int d : instance.demands[i]) {
                System.out.printf("%d ", d);
            }

            System.out.print("\n  SUC[i] = ");

            for (int s : instance.successors[i]) {
                System.out.printf("%d ", s);
            }

            System.out.println();
        }

        // Assume a solution...
        int[] solution = new int[instance.n()];

        for (int i = 0; i < solution.length; i++) {
            solution[i] = 100 + i;
        }

        Io.writeSolution(solution, Paths.get("sol.txt"));

        System.out.println("ok");
    }
}
