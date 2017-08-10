package name.cantanima.idealnim;

import android.util.Log;
import android.util.Pair;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Vector;

import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

/**
 * Created by cantanima on 8/10/17.
 */

public class Game_Evaluation {

  public Game_Evaluation(int count, int max_x, int max_y) {

    Calendar timestamp = new GregorianCalendar();
    Log.d(tag, timestamp.get(MINUTE) + ":" + timestamp.get(SECOND) + "." + timestamp.get(MILLISECOND));
    base_count = count;
    base_max_x = max_x;
    base_max_y = max_y;
    cache = new Vector<>(count);
    for (int i = 0; i < count; ++i) {
      cache.addElement(new Vector<Vector<LinkedList<Pair<boolean[][], Integer>>>>(max_x));
      for (int j = 0; j < max_x; ++j) {
        cache.get(i).addElement(new Vector<LinkedList<Pair<boolean[][], Integer>>>(max_y));
        for (int k = 0; k < max_y; ++k) {
          cache.get(i).get(j).addElement(new LinkedList<Pair<boolean[][], Integer>>());
        }
      }
    }
    timestamp = new GregorianCalendar();
    Log.d(tag, timestamp.get(MINUTE) + ":" + timestamp.get(SECOND) + "." + timestamp.get(MILLISECOND));

  }

  public boolean same_field(boolean[][] F, boolean[][] G, int count, int max_x, int max_y) {

    int counted = 0;
    boolean so_far_so_good = true;
    for (int i = 0; so_far_so_good && counted < count && i < max_x; ++i) {
      for (int j = 0; so_far_so_good && counted < count && j < max_y; ++j) {
        so_far_so_good = (F[i][j] == G[i][j]);
        if (F[i][j]) ++counted;
      }
    }
    return so_far_so_good;

  }

  public int game_value(boolean[][] field, int count, int max_x, int max_y, int level) {

    int result;

    switch(count) {

      case 0: result = 0; break;
      case 1: result = 1; break;
      default:
        String header = "";
        for (int i = 0; i < level; ++i) header += "=";
        Log.d(tag, header);
        Log.d(tag, "Searching for:");
        print_field(field, max_x, max_y);
        Log.d(tag, "----");
        LinkedList<Pair<boolean[][], Integer>> cache_line
            = cache.get(count - 1).get(max_x - 1).get(max_y - 1);
        Iterator<Pair<boolean[][], Integer>> iter = cache_line.iterator();
        boolean searching = true;
        Pair<boolean[][], Integer> p = null;
        while (searching && iter.hasNext()) {
          p = iter.next();
          if (same_field(field, p.first, count, max_x, max_y))
            searching = false;
        }
        if (!searching) {
          result = p.second;
          Log.d(tag, "found with value " + String.valueOf(result));
          print_field(p.first, max_x, max_y);
          Log.d(tag, "----");
        } else {
          Log.d(tag, "not found, computing");
          TreeSet<Integer> options = new TreeSet<>();
          // work our way up to largest x, y values
          for (int i = 0; i < max_x; ++i) {
            for (int j = 0; j < max_y; ++j) {
              // we will remove the point (i,j) and all those northeast of it to create a new field
              while (j < max_y && !field[i][j]) ++j;
              if (j < max_y && field[i][j]) { // no point continuing if there are no points in this row
                // make the new field
                boolean[][] new_field = new boolean[max_x][max_y];
                int new_count = count;
                for (int k = 0; k < max_x; ++k) {
                  for (int l = 0; l < max_y; ++l)
                    if (k < i || l < j)
                      new_field[k][l] = field[k][l];
                    else {
                      new_field[k][l] = false;
                      if (field[k][l]) --new_count;
                    }
                }
                // adjust max_x, max_y if necessary
                int new_max_x = max_x, new_max_y = max_y;
                if (new_count > 1) {
                  boolean checking_y = true;
                  while (new_max_y > 0 && checking_y) {
                    boolean found_position = false;
                    for (int k = 0; !found_position && k < max_x; ++k)
                      found_position |= new_field[k][new_max_y - 1];
                    if (found_position) checking_y = false;
                    else --new_max_y;
                  }
                  boolean checking_x = true;
                  while (new_max_x > 0 && checking_x) {
                    boolean found_position = false;
                    for (int l = 0; !found_position && l < max_y; ++l)
                      found_position |= new_field[new_max_x - 1][l];
                    if (found_position) checking_x = false;
                    else --new_max_x;
                  }
                }
                int value = game_value(new_field, new_count, new_max_x, new_max_y, level + 1);
                options.add(value);
              }
            }
          }
          int mex = 0;
          while (options.contains(mex)) ++mex;
          result = mex;
          cache_line.add(new Pair<>(field, result));
          Log.d(tag, "final value " + String.valueOf(result));
          Log.d(tag, "----");
          //print_field(field, max_x, max_y);
          //Log.d(tag, "count: " + String.valueOf(count));
          //Log.d(tag, options.toString() + " : " + String.valueOf(mex));
        }

    }

    return result;

  }

  public void print_field(boolean[][] field, int max_x, int max_y) {
    Log.d(tag, "----");
    for (int i = max_y - 1; i > -1; --i) {
      String output = "";
      for (int j = 0; j < max_x; ++j)
        output += (field[j][i]) ? "X" : ".";
      Log.d(tag, output);
    }
    Log.d(tag, "----");
  }

  Position find_min_pos(boolean[][] field, int max_x, int max_y) {
    boolean searching = true;
    int i = 0, j = 0;
    while (searching && i < max_x) {
      j = 0;
      while (searching && j < max_y) {
        searching = !field[i][j];
        if (searching) ++j;
      }
      if (searching) ++i;
    }
    Log.d(tag, "min pos at " + String.valueOf(i) + ", " + String.valueOf(j));
    return new Position(i, j);
  }

  protected int base_count, base_max_x, base_max_y;
  protected Vector<Vector<Vector<LinkedList<Pair<boolean[][], Integer>>>>> cache;

  private String tag = "Game_Evaluation";

}
