package name.cantanima.idealnim;

import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.TreeSet;

import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;

/**
 * Created by cantanima on 8/8/17.
 */

public class Game_Control {

  public Game_Control() {}

  public void new_game(Playfield p) {
    playfield = p;
    switch (level) {
      case 1: new_level_one_game();
      default: new_level_one_game();
    }
  }

  public void new_level_one_game() {

    Calendar cal = new GregorianCalendar();
    current_seed = cal.get(MINUTE) * 59 + cal.get(HOUR_OF_DAY) * 23 + cal.get(SECOND) * 43
        + cal.get(DAY_OF_YEAR) * 365 + cal.get(YEAR) * 9999 + cal.get(SECOND);
    random.setSeed(current_seed);

    Ideal I = new Ideal();
    int n = random.nextInt(5) + 1;
    for (int i = 0; i < n; ++i)
      I.add_generator_fast(
          random.nextInt(playfield.view_xmax - 1), random.nextInt(playfield.view_ymax - 1)
      );
    I.sort_ideal();
    playfield.set_to(I);
    playfield.invalidate();

  }

  public int game_value(boolean[][] field, int count, int max_x, int max_y) {

    int result;

    switch(count) {

      case 0: result = 0; break;
      case 1: result = 1; break;
      default:
        TreeSet<Integer> options = new TreeSet<>();
        // work our way up to largest x, y values
        for (int i = 0; i < max_x; ++i) {
          for (int j = 0; j < max_y; ++j) {
            // we will remove the point (i,j) and all those northeast of it to create a new field
            while (j < max_y && field[i][j] == false) ++j;
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
              options.add(game_value(new_field, new_count, new_max_x, new_max_y));
            }
          }
        }
        int mex = 0;
        while (options.contains(mex)) ++mex;
        result = mex;
        print_field(field, max_x, max_y);
        Log.d(tag, "count: " + String.valueOf(count));
        Log.d(tag, options.toString() + " : " + String.valueOf(mex));

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

  protected int level = 1, max_mex_degree = 7;
  protected long current_seed;
  protected Playfield playfield;
  protected Random random = new Random();

  private String tag = "Game_Control";

}
