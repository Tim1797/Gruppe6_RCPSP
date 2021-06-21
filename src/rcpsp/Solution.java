package rcpsp;

import java.util.HashMap;
import java.util.Map;

public class Solution {
  public int[] data;
  private final Instance instance;
  private Map<Integer, Integer> fitnessCache;

  public Solution(int size, Instance instance) {
    data = new int[size];
    this.instance = instance;
    fitnessCache = new HashMap<>(size);
  }

  public int getMakespan(int index) {
    return fitnessCache.computeIfAbsent(index, k -> Fitness.get(data, instance));
  }
}
