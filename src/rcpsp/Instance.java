package rcpsp;

/**
 * Single-mode RCPSP instance.
 *
 * @author bg (begraf@uos.de)
 */
public class Instance {

    /** Available units for each resource. */
    public int[] resources;

    /** Processing time for each job. */
    public int[] processingTime;

    /** Resource demands for each job. */
    public int[][] demands;

    /** Successors for each job. */
    public int[][] successors;

    /**
     * Construct.
     * @param n  number of jobs
     * @param r  number of resources
     */
    public Instance(int n, int r) {
        this.resources = new int[r];
        this.processingTime = new int[n];
        this.demands = new int[n][r];
        this.successors = new int[n][];
    }

    /**
     * Number of jobs.
     * @return  number of jobs
     */
    public int n() {
        return processingTime.length;
    }

    /**
     * Number of resources.
     * @return  number of resources.
     */
    public int r() {
        return resources.length;
    }
}
