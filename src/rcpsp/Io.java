package rcpsp;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Instance and solution I/O.
 */
public class Io {

    /**
     * Io Error
     */
    public static class Error extends RuntimeException {
        public Error(String msg) {
            super(msg);
        }

        public Error(Exception ex) {
            super(ex);
        }
    }

    /**
     * Read an instance from the given path.
     *
     * @param path path to file / source
     * @return Instance on success.
     * @throws Error in case of any error
     */
    public static Instance readInstance(Path path) {

        if (!Files.isRegularFile(path)) {
            throw new Error(String.format("Path %s does not exist", path.toString()));
        }

        try {

            final BufferedReader reader = Files.newBufferedReader(path, Charset.defaultCharset());

            final String[] nrstr = reader.readLine().split("\\s+");

            final int n = Integer.parseInt(nrstr[0]);
            final int r = Integer.parseInt(nrstr[1]);

            final Instance instance = new Instance(n, r);

            final String[] nrres = reader.readLine().split("\\s+");

            for (int i = 0; i < instance.r(); i++) {
                instance.resources[i] = Integer.parseInt(nrres[i]);
            }

            for (int i = 0; i < instance.n(); i++) {
                final String[] toks = reader.readLine().split("\\s+");

                instance.processingTime[i] = Integer.parseInt(toks[0]);

                for (int j = 0; j < instance.r(); j++) {
                    instance.demands[i][j] = Integer.parseInt(toks[j + 1]);
                }

                final int nSucc = Integer.parseInt(toks[instance.r() + 1]);

                instance.successors[i] = new int[nSucc];

                for (int j = 0; j < nSucc; j++) {
                //changed!!!!!!! the -1
                    instance.successors[i][j] = Integer.parseInt(toks[instance.r() + j + 2])-1;
                }
            }

            return instance;

        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Write a solution to the given path.
     *
     * @param startTimes Job ordered sequence of starting times
     * @param path       Path to file being written
     */
    public static void writeSolution(int[] startTimes, Path path) {
        try (final BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset());
             final PrintWriter printer = new PrintWriter(writer)) {

            printer.format("%d\n", startTimes.length);

            for (int i = 0; i < startTimes.length; i++) {
                printer.format("%d %d\n", i + 1, startTimes[i]);
            }

        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
