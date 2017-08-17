package name.cantanima.idealnim;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
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
 * Created by cantanima on 8/15/17.
 */

public class Game_Evaluation_Hashmap {

  public Game_Evaluation_Hashmap(
      Context context, Ideal I, Ideal J, int view_xmax, int view_ymax, int level
  ) {

    overall_context = (Activity) context;
    game_level = level;

    base_playable = new Ideal(I);
    base_playable.sort_ideal();
    if (J != null)
      base_played = new Ideal(J);
    else {
      base_played = new Ideal();
      base_played.add_generator_fast(view_xmax, view_ymax);
    }
    base_max_x = view_xmax;
    base_max_y = view_ymax;
    int initial_map_size = 10000000; // view_xmax*view_xmax*view_xmax*view_xmax*view_xmax;
    cache = new HashMap<>(initial_map_size);

  }

  public boolean same_configuration(Ideal I, Ideal J) { return I.T.equals(J.T);  }

  public void play_point(int i, int j) {

    base_played.add_generator(i, j, true);
    // adjust base_max_y
    boolean checking_y = true;
    int old_base_max_y = base_max_y;
    while (base_max_y > 0 && checking_y) {
      boolean found_position = false;
      for (int k = 0; !found_position && k < base_max_x; ++k)
        found_position |= base_played.contains(k, base_max_y - 1);
      if (found_position) checking_y = false;
      else --base_max_y;
    }
    // adjust base_max_x
    if (j == 0) base_max_x = i;
    boolean checking_x = true;
    while (base_max_x > 0 && checking_x) {
      boolean found_position = false;
      for (int l = 0; !found_position && l < old_base_max_y; ++l)
        found_position |= base_played.contains(base_max_x - 1, l);
      if (found_position) checking_x = false;
      else --base_max_x;
    }

    if (base_playable.equals(base_played))
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

    if (game_level == 1 || game_level == 2) {

      zero_position = base_playable.T.getFirst();
      if (computer_move) {
        Playfield playfield = (Playfield) overall_context.findViewById(R.id.playfield);
        playfield.hint_position = zero_position;
        playfield.get_computer_move();
      }

    } else {

      LinkedList<Position> L = base_played.T;
      ArrayList<Integer> ideal = new ArrayList<>(L.size() * 2);
      for (Position P : L) {
        ideal.add(P.get_x());
        ideal.add(P.get_y());
      }
      if (!cache.containsKey(ideal)) {
        int count = 0;
        for (int i = 0; i < base_max_x; ++i) {
          for (int j = 0; j < base_max_y; ++j) {
            if (!base_played.contains(i, j) && base_playable.contains(i, j))
              ++count;
          }
        }
        if (count != 1) { // we don't cache configurations of size 1, so we have to check
          Compute_Task my_task = new Compute_Task(overall_context, count);
          my_task.execute(ideal, count, base_max_x, base_max_y);
        } else {
          zero_position = base_playable.T.getFirst();
          if (computer_move) {
            Playfield playfield = (Playfield) overall_context.findViewById(R.id.playfield);
            playfield.hint_position = zero_position;
            playfield.get_computer_move();
          }
        }
      } else {
        Pair<Integer, Position> p = cache.get(ideal);
        zero_position = p.second;
        result = configuration_value = p.first;
        if (computer_move) {
          Playfield playfield = (Playfield) overall_context.findViewById(R.id.playfield);
          playfield.hint_position = zero_position;
          playfield.get_computer_move();
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
      Calendar end_calendar = new GregorianCalendar();
      Date end_time = end_calendar.getTime();
      super.onPostExecute(integer);
      ((TextView) overall_context.findViewById(R.id.value_view)).setText(integer.toString());
      Playfield p = ((Playfield) overall_context.findViewById(R.id.playfield));
      Log.d(tag, "times: " + String.valueOf(end_time.getTime()) + ", " + start_time.getTime());
      if (end_time.getTime() - start_time.getTime() > 60000) {
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
      calendar = new GregorianCalendar();
      start_time = calendar.getTime();
      Integer result = this.compute_scores(
          base_played, (Integer) params[1], (Integer) params[2] - 1, (Integer) params[3] - 1, 0
      );
      return result;
    }

    public int compute_scores(Ideal just_played, int count, int max_x, int max_y, int level) {

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
            zero_position = just_played.T.getFirst();
          break;
        default:
          ArrayList<Integer> gens = new ArrayList<>(just_played.T.size() * 2);
          for (Position P : just_played.T) {
            gens.add(P.get_x());
            gens.add(P.get_y());
          }
          if (cache.containsKey(gens)) {
            Pair<Integer, Position> p = cache.get(gens);
            result = p.first;
            if (level == 0) zero_position = p.second;
            //Log.d(tag, "found with value " + String.valueOf(result));
            //print_configuration(p.first, max_x, max_y);
            //Log.d(tag, "----");
          } else {
            Position winning_position = ORIGIN;
            //Log.d(tag, "not found, computing");
            TreeSet<Integer> options = new TreeSet<>();
            // work our way up to largest x, y values
            for (int i = 0; i <= max_x; ++i) {
              for (int j = 0; j <= max_y; ++j) {
                // we will remove the point (i,j) and all those northeast of it
                // to create a new configuration
                // no point continuing if there are no points in this row
                if (just_played.contains(i, j)) break;
                if (base_playable.contains(i, j)) {
                  Ideal next_played = new Ideal(just_played);
                  // make the new configuration
                  next_played.add_generator(i, j, true);
                  // adjust max_x, max_y if necessary
                  int new_count = 0, new_max_x = 0, new_max_y = 0;
                  for (int k = 0; k <= max_x; ++k)
                    for (int l = 0; l <= max_y; ++l) {
                      if (next_played.contains(k, l)) break;
                      if (base_playable.contains(k, l) && !next_played.contains(k, l)) {
                        ++new_count;
                        new_max_x = (k > new_max_x) ? k : new_max_x;
                        new_max_y = (l > new_max_y) ? l : new_max_y;
                      }
                    }
                  int value = compute_scores(next_played, new_count, new_max_x, new_max_y, level + 1);
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
            cache.put(gens, new Pair<Integer, Position>(result, winning_position));
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
    Calendar calendar;
    Date start_time;

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

  protected Ideal base_playable, base_played;
  protected int base_max_x, base_max_y;
  protected HashMap<ArrayList<Integer>, Pair<Integer, Position>> cache;

  protected static final Position ORIGIN = new Position(0,0);
  protected Position zero_position = ORIGIN;
  protected int configuration_value;

  protected Activity overall_context;
  protected int game_level;

  protected boolean computer_move;

  final private static String tag = "Game_Evaluation";

}
