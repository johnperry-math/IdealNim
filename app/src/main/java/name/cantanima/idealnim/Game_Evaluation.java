package name.cantanima.idealnim;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Vector;

import static java.lang.Math.abs;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

/**
 * Created by cantanima on 8/10/17.
 */

public class Game_Evaluation {

  public Game_Evaluation(
      Context context, Ideal I, Ideal J, int view_xmax, int view_ymax, int level
  ) {

    overall_context = (Activity) context;
    game_level = level;

    base_configuration = new boolean[view_xmax][view_ymax];
    base_count = 0;
    for (int i = 0; i < view_xmax; ++i)
      for (int j = 0; j < view_ymax; ++j)
        base_configuration[i][j] = false;
    for (Position t: I.T) {
      for (int i = t.get_x(); i < view_xmax; ++i)
        for (int j = t.get_y(); j < view_ymax; ++j)
          if (base_configuration[i][j])
            break;
          else {
            base_configuration[i][j] = true;
            ++base_count;
          }
    }
    if (J != null)
      for (Position t : J.T) {
        for (int i = t.get_x(); i < view_xmax; ++i)
          for (int j = t.get_y(); j < view_ymax; ++j) {
            if (!base_configuration[i][j])
              break;
            else {
              base_configuration[i][j] = false;
              --base_count;
            }
          }
      }
    base_max_x = view_xmax;
    base_max_y = view_ymax;
    cache = new Vector<>(base_count);
    zero_positions = new Vector<>(base_count);
    for (int i = 0; i < base_count; ++i) {
      cache.addElement(new Vector<Vector<LinkedList<Pair<boolean[][], Integer>>>>(base_max_x));
      zero_positions.addElement(new Vector<Vector<LinkedList<Position>>>(base_max_x));
      for (int j = 0; j < base_max_x; ++j) {
        cache.get(i).addElement(new Vector<LinkedList<Pair<boolean[][], Integer>>>(base_max_y));
        zero_positions.get(i).addElement(new Vector<LinkedList<Position>>(base_max_y));
        for (int k = 0; k < base_max_y; ++k) {
          cache.get(i).get(j).addElement(new LinkedList<Pair<boolean[][], Integer>>());
          zero_positions.get(i).get(j).addElement(new LinkedList<Position>());
        }
      }
    }

  }

  public boolean same_configuration(boolean[][] F, boolean[][] G, int count, int max_x, int max_y) {

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

  public void play_point(int i, int j) {
    
    // update base_configuration
    for (int k = i; k < base_max_x; ++k)
      for (int l = j; l < base_max_y; ++l)
        if (base_configuration[k][l]) {
          base_configuration[k][l] = false;
          --base_count;
        }
    // adjust base_max_y
    boolean checking_y = true;
    int old_base_max_y = base_max_y;
    while (base_max_y > 0 && checking_y) {
      boolean found_position = false;
      for (int k = 0; !found_position && k < base_max_x; ++k)
        found_position |= base_configuration[k][base_max_y - 1];
      if (found_position) checking_y = false;
      else --base_max_y;
    }
    // adjust base_max_x
    if (j == 0) base_max_x = i;
    boolean checking_x = true;
    while (base_max_x > 0 && checking_x) {
      boolean found_position = false;
      for (int l = 0; !found_position && l < old_base_max_y; ++l)
        found_position |= base_configuration[base_max_x - 1][l];
      if (found_position) checking_x = false;
      else --base_max_x;
    }

    Log.d(tag,
        "count: " + String.valueOf(base_count) +
            " max_x: " + String.valueOf(base_max_x) +
            " max_y: " + String.valueOf(base_max_y)
    );

    if (base_count == 0)
      ((Playfield) overall_context.findViewById(R.id.playfield)).game_control.notify_game_over();

  }

  Position hint_position() { return zero_position; }

  public void choose_computer_move() {
    computer_move = true;
    game_value();
  }

  public int game_value() {

    int result = 0;
    Position winning_position = ORIGIN;

    if (base_count != 0) {

      if (game_level == 1 || game_level == 2) {

        zero_position = find_min_pos(base_configuration, base_max_x, base_max_y);
        if (computer_move) {
          Playfield playfield = (Playfield) overall_context.findViewById(R.id.playfield);
          playfield.hint_position = zero_position;
          playfield.get_computer_move();
        }

      } else {

        LinkedList<Pair<boolean[][], Integer>> cache_line
            = cache.get(base_count - 1).get(base_max_x - 1).get(base_max_y - 1);
        LinkedList<Position> position_line
            = zero_positions.get(base_count - 1).get(base_max_x - 1).get(base_max_y - 1);
        Iterator<Pair<boolean[][], Integer>> iter = cache_line.iterator();
        Iterator<Position> piter = position_line.iterator();

        boolean searching = true;
        Pair<boolean[][], Integer> p = null;
        while (searching && iter.hasNext()) {
          p = iter.next();
          Position temp_position = piter.next();
          if (same_configuration(base_configuration, p.first, base_count, base_max_x, base_max_y)) {
            searching = false;
            winning_position = temp_position;
          }
        }

        if (searching) {
          Compute_Task my_task = new Compute_Task(overall_context, base_count);
          my_task.execute(base_configuration, base_count, base_max_x, base_max_y);
        } else {
          zero_position = winning_position;
          result = configuration_value = p.second;
          result = p.second;
          if (computer_move) {
            Playfield playfield = (Playfield) overall_context.findViewById(R.id.playfield);
            playfield.hint_position = zero_position;
            playfield.get_computer_move();
          }
        }

      }

    }

    return result;

  }

  private class Compute_Task extends AsyncTask<Object, Integer, Integer> {

    /**
     * Creates a new asynchronous task. This constructor must be invoked on the UI thread.
     */
    public Compute_Task(Activity activity, int count) {

      super();

      current_positions = 0;

      update_dialog = new ProgressDialog(activity);
      update_dialog.setMax(count);
      update_dialog.setIndeterminate(false);
      update_dialog.setCancelable(false);
      update_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      update_dialog.setTitle(activity.getString(R.string.thinking));
      update_dialog.setMessage(activity.getString(R.string.please_wait));

    }

    /**
     * Runs on the UI thread before {@link #doInBackground}.
     *
     * @see #onPostExecute
     * @see #doInBackground
     */
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      update_dialog.show();

    }

    /**
     * <p>Runs on the UI thread after {@link #doInBackground}. The
     * specified result is the value returned by {@link #doInBackground}.</p>
     * <p>
     * <p>This method won't be invoked if the task was cancelled.</p>
     *
     * @param integer The result of the operation computed by {@link #doInBackground}.
     * @see #onPreExecute
     * @see #doInBackground
     * @see #onCancelled(Object)
     */
    @Override
    protected void onPostExecute(Integer integer) {
      super.onPostExecute(integer);
      ((TextView) overall_context.findViewById(R.id.value_view)).setText(integer.toString());
      Playfield p = ((Playfield) overall_context.findViewById(R.id.playfield));
      end_time = new GregorianCalendar();
      if (
          start_time.get(HOUR) != end_time.get(HOUR) ||
              start_time.get(MINUTE) != end_time.get(MINUTE) ||
              abs(start_time.get(SECOND) - end_time.get(SECOND)) >= 30 ||
              start_time.get(DAY_OF_MONTH) != end_time.get(DAY_OF_MONTH) // gracious me, that's patient!
          ) {
        p.game_control.notify_large_board();
      }
      p.hint_position = zero_position;
      p.invalidate();
      TextView tv = (TextView) overall_context.findViewById(R.id.value_view);
      tv.setText(String.valueOf(configuration_value));
      update_dialog.dismiss();
      if (computer_move) {
        computer_move = false;
        p.get_computer_move();
      }
    }

    /**
     * Runs on the UI thread after {@link #publishProgress} is invoked.
     * The specified values are the values passed to {@link #publishProgress}.
     *
     * @param values The values indicating progress.
     * @see #publishProgress
     * @see #doInBackground
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
      super.onProgressUpdate(values);
      update_dialog.incrementProgressBy(1);
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     * <p>
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @Override
    protected Integer doInBackground(Object... params) {
      start_time = new GregorianCalendar();
      Integer result = this.compute_scores(
          (boolean[][]) params[0], (Integer) params[1], (Integer) params[2], (Integer) params[3], 0
      );
      return result;
    }

    public int compute_scores(boolean[][] configuration, int count, int max_x, int max_y, int level) {

      int result, completed = 0;

      switch(count) {

        case 0:
          result = 0;
          if (level == 0)
            zero_position = ORIGIN;
          break;
        case 1:
          result = 1;
          if (level == 0)
            zero_position = find_min_pos(configuration, max_x, max_y);
          break;
        default:
          //String header = "";
          //for (int i = 0; i < level; ++i) header += "=";
          //Log.d(tag, header);
          //Log.d(tag, "Searching for:");
          //print_configuration(configuration, max_x, max_y);
          //Log.d(tag, "----");
          LinkedList<Pair<boolean[][], Integer>> cache_line
              = cache.get(count - 1).get(max_x - 1).get(max_y - 1);
          LinkedList<Position> position_line
              = zero_positions.get(count - 1).get(max_x - 1).get(max_y - 1);
          Iterator<Pair<boolean[][], Integer>> iter = cache_line.iterator();
          Iterator<Position> piter = position_line.iterator();
          boolean searching = true;
          Pair<boolean[][], Integer> p = null;
          Position winning_position = ORIGIN;
          while (searching && iter.hasNext()) {
            p = iter.next();
            Position temp_position = piter.next();
            if (same_configuration(configuration, p.first, count, max_x, max_y)) {
              searching = false;
              winning_position = temp_position;
            }
          }
          if (!searching) {
            result = p.second;
            if (level == 0) zero_position = winning_position;
            //Log.d(tag, "found with value " + String.valueOf(result));
            //print_configuration(p.first, max_x, max_y);
            //Log.d(tag, "----");
          } else {
            //Log.d(tag, "not found, computing");
            TreeSet<Integer> options = new TreeSet<>();
            // work our way up to largest x, y values
            for (int i = 0; i < max_x; ++i) {
              for (int j = 0; j < max_y; ++j) {
                // we will remove the point (i,j) and all those northeast of it
                // to create a new configuration
                while (j < max_y && !configuration[i][j]) ++j;
                // no point continuing if there are no points in this row
                if (j < max_y && configuration[i][j]) {
                  // make the new configuration
                  boolean[][] new_configuration = new boolean[max_x][max_y];
                  int new_count = count;
                  for (int k = 0; k < max_x; ++k) {
                    for (int l = 0; l < max_y; ++l)
                      if (k < i || l < j)
                        new_configuration[k][l] = configuration[k][l];
                      else {
                        new_configuration[k][l] = false;
                        if (configuration[k][l]) --new_count;
                      }
                  }
                  // adjust max_x, max_y if necessary
                  int new_max_x = max_x, new_max_y = max_y;
                  if (new_count > 1) {
                    boolean checking_y = true;
                    while (new_max_y > 0 && checking_y) {
                      boolean found_position = false;
                      for (int k = 0; !found_position && k < max_x; ++k)
                        found_position |= new_configuration[k][new_max_y - 1];
                      if (found_position) checking_y = false;
                      else --new_max_y;
                    }
                    boolean checking_x = true;
                    while (new_max_x > 0 && checking_x) {
                      boolean found_position = false;
                      for (int l = 0; !found_position && l < max_y; ++l)
                        found_position |= new_configuration[new_max_x - 1][l];
                      if (found_position) checking_x = false;
                      else --new_max_x;
                    }
                  }
                  int value = compute_scores(new_configuration, new_count, new_max_x, new_max_y, level + 1);
                  options.add(value);
                  if (value == 0) {
                    winning_position = new Position(i, j);
                    if (level == 0)
                      zero_position = winning_position;
                  }
                }
                if (level == 0)
                  publishProgress(++completed);
              }
            }
            int mex = 0;
            while (options.contains(mex)) ++mex;
            result = mex;
            cache_line.add(new Pair<>(configuration, result));
            position_line.add(winning_position);
            if (result == 0 && level == 0)
              zero_position = ORIGIN;
            //Log.d(tag, "final value " + String.valueOf(result));
            //Log.d(tag, "----");
          }

      }

      return result;

    }

    ProgressDialog update_dialog;
    int current_positions;
    Calendar start_time, end_time;

  }

  public void print_configuration(boolean[][] config, int max_x, int max_y) {
    Log.d(tag, "----");
    for (int i = max_y - 1; i > -1; --i) {
      String output = "";
      for (int j = 0; j < max_x; ++j)
        output += (config[j][i]) ? "X" : ".";
      Log.d(tag, output);
    }
    Log.d(tag, "----");
  }

  public void print_cache(int line) {
    if (line < 0 || line > cache.size())
      return;
    Vector<Vector<LinkedList<Pair<boolean[][], Integer>>>> cache_line = cache.get(line);
    for (int i = 0; i < cache_line.size(); ++i)
      for (int j = 0; j < cache_line.get(i).size(); ++i)
        for (Pair<boolean[][], Integer> p : cache_line.get(i).get(j)) {
          Log.d(tag, "==== " + String.valueOf(i) + ", " + String.valueOf(j) + " ====");
          print_configuration(p.first, i, j);
          Log.d(tag, "==============");
        }
  }

  protected Position find_min_pos(boolean[][] config, int max_x, int max_y) {
    boolean searching = true;
    int i = 0, j = 0;
    while (searching && i < max_x) {
      j = 0;
      while (searching && j < max_y) {
        searching = !config[i][j];
        if (searching) ++j;
      }
      if (searching) ++i;
    }
    return new Position(i, j);
  }

  protected boolean[][] base_configuration;
  protected int base_count, base_max_x, base_max_y;
  protected Vector<Vector<Vector<LinkedList<Pair<boolean[][], Integer>>>>> cache;
  protected Vector<Vector<Vector<LinkedList<Position>>>> zero_positions;

  protected static final Position ORIGIN = new Position(0,0);
  protected Position zero_position = ORIGIN;
  protected int configuration_value;

  protected Activity overall_context;
  protected int game_level;

  protected boolean computer_move;

  final private static String tag = "Game_Evaluation";

}
