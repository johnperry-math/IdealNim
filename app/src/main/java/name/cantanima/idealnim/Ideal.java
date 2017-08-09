package name.cantanima.idealnim;

import android.util.Log;

import java.util.Iterator;
import java.util.LinkedList;

import static java.util.Collections.reverse;
import static java.util.Collections.sort;

/**
 * Created by cantanima on 8/8/17.
 */

/**
 * An Ideal class that allows one to iterate through the ideal's positions.
 * Includes a diagram that gives quick (array-like) way to test whether a position is in the ideal.
 */
public class Ideal implements Iterable<Position> {

  /**
   * Creates a new ideal, and initializes a 10x10 monomial diagram.
   */
  public Ideal() {
    T = new LinkedList<>();
    mon_diag = new boolean[10][10];
    for (int i = 0; i < 10; ++i)
      for (int j = 0; j < 10; ++j)
        mon_diag[i][j] = false;
  }

  /**
   * Creates a new ideal as a copy of the old one,
   * and initializes a monomial diagram of sufficient size.
   * @param F  ideal to copy
   */
  public Ideal(Ideal F) {

    T = new LinkedList<>();
    for (Position t : F.T)
      T.add(t);
    max_x = F.max_x;
    max_y = F.max_y;
    mon_diag = new boolean[max_x][max_y];
    for (int i = 0; i < max_x; ++i)
      for (int j = 0; j < max_y; ++j)
        mon_diag[i][j] = F.mon_diag[i][j];

  }

  /**
   * Returns an iterator over the Positions.
   *
   * @return an Iterator.
   */
  @Override
  public Iterator<Position> iterator() {
    return T.iterator();
  }

  /**
   * Adds a generator at the given position, and sorts the ideal if desired.
   * @param x x-value of new position
   * @param y y-value of new position
   * @param sort whether to sort the ideal; see Position for the precise technique
   * @see Position
   */
  public void add_generator(int x, int y, boolean sort) {

    boolean has_point = false;
    Iterator Ti = T.iterator();
    while (Ti.hasNext()) {
      Position t = (Position) Ti.next();
      if (t.is_generated_by(x, y))
        Ti.remove();
      else if (t.generates(x, y))
        has_point = true;
    }

    if (!has_point) {
      T.add(new Position(x, y));
      if (sort) sort_ideal();
      mark_diagram(x, y);
    }

  }

  /**
   * Adds a generator to the ideal. Does not sort. Use this for slightly higher efficiency.
   * Be sure to call {@code sort_ideal()} when done adding generators.
   * @param x x-value of new position
   * @param y y-value of new position
   */
  public void add_generator_fast(int x, int y) {
    add_generator(x, y, false);
  }

  /**
   * Determines whether the ideal generates the position (x,y).
   * @param x x-value of position
   * @param y y-value of position
   * @return {@literal true} if and only if the ideal generates (x,y).
   */
  private boolean generates(int x, int y) {

    boolean result = false;

    for (Position t : T)
      if (t.generates(x, y)) {
        result = true;
        break;
      }

    return result;

  }

  /**
   * Indicates whether the Ideal contains the position (x, y).
   * This is a fast check based on a cache.
   * @param x x-value of position to check
   * @param y y-value of position to check
   * @return {@literal true} if and only (x,y) is contained in the Ideal
   */
  public boolean contains(int x, int y) { return mon_diag[x][y]; }

  /**
   * Sorts the ideal. See Position for details on the ordering.
   * @see Position
   */
  public void sort_ideal() { sort(T); }

  /**
   * Marks the monomial diagram up to the indicated position.
   * @param x x-value of position to mark
   * @param y y-value of position to mark
   */
  private void mark_diagram(int x, int y) {

    int start_x = x, start_y = y;
    if (x > max_x || y > max_y) {
      max_x = (x > max_x) ? x + 5 : max_x;
      max_y = (y > max_y) ? y + 5 : max_y;
      mon_diag = new boolean[max_x][max_y];
      for (int i = 0; i < max_x; ++i)
        for (int j = 0; j < max_y; ++j)
          mon_diag[i][j] = false;
      start_x = start_y = 0;
    }
    for (int i = start_x; i < max_x; ++i)
      for (int j = start_y; j < max_y; ++j)
        if (mon_diag[i][j]) break;
        else {
          if (generates(i, j)) {
            for (int k = i; k < max_x; ++k)
              for (int l = j; l < max_y; ++l)
                if (mon_diag[k][l]) break;
                else mon_diag[k][l] = true;
          }
        }

  }

  public void log() {

    Log.d(tag, "----");
    for (Position t : T) {
      Log.d(tag, "(" + String.valueOf(t.get_x()) + ", " + String.valueOf(t.get_y()) + ")");
    }
    Log.d(tag, "----");

  }

  protected LinkedList<Position> T;
  protected boolean mon_diag[][];
  protected int max_x = 10, max_y = 10;

  private String tag = "Ideal";

}
