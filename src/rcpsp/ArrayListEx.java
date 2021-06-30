package rcpsp;

import java.util.ArrayList;

public class ArrayListEx<T> extends ArrayList<T> {
  public ArrayListEx() {
    super();
  }

  public ArrayListEx(int initialCapacity) {
    super(initialCapacity);
  }

  public void removeRange(int fromIndex, int toIndex) {
    super.removeRange(fromIndex, toIndex);
  }
}
