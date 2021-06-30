package rcpsp;

public class Solution {
  private final int[] data;
  private final Instance instance;
  private int fitnessCache;

  public Solution(int size, Instance instance) {
    this.data = new int[size];
    this.instance = instance;
    this.fitnessCache = -1;
  }

  public int get(int index) {
    return data[index];
  }

  public void set(int index, int value) {
    data[index] = value;
    // Invalidate cache
    fitnessCache = -1;
  }

  // NOTE: Do not modify the underlying data!
  public int[] getDataUnsafe() {
    return data;
  }

  public int getMakespan() {
    if (fitnessCache == -1) {
      fitnessCache = Fitness.get(data, instance);
    }
    return fitnessCache;
  }

  public int size() {
    return data.length;
  }
}
