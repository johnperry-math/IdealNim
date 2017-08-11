package name.cantanima.idealnim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.util.Pair;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.TreeSet;
import java.util.Vector;

import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

/**
 * Created by cantanima on 8/10/17.
 */

public class Game_Evaluation {

  public Game_Evaluation(Context context, int count, int max_x, int max_y) {

    overall_context = context;
    base_count = count;
    base_max_x = max_x;
    base_max_y = max_y;
    cache = new Vector<>(count);
    zero_positions = new Vector<>(count);
    for (int i = 0; i < count; ++i) {
      cache.addElement(new Vector<Vector<LinkedList<Pair<boolean[][], Integer>>>>(max_x));
      zero_positions.addElement(new Vector<Vector<LinkedList<Position>>>(max_x));
      for (int j = 0; j < max_x; ++j) {
        cache.get(i).addElement(new Vector<LinkedList<Pair<boolean[][], Integer>>>(max_y));
        zero_positions.get(i).addElement(new Vector<LinkedList<Position>>(max_y));
        for (int k = 0; k < max_y; ++k) {
          cache.get(i).get(j).addElement(new LinkedList<Pair<boolean[][], Integer>>());
          zero_positions.get(i).get(j).addElement(new LinkedList<Position>());
        }
      }
    }

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

  public int game_value(boolean[][] field, int count, int max_x, int max_y) {

    int result = 0;
    Position winning_position = ORIGIN;

    LinkedList<Pair<boolean[][], Integer>> cache_line
        = cache.get(count - 1).get(max_x - 1).get(max_y - 1);
    LinkedList<Position> position_line
        = zero_positions.get(count - 1).get(max_x - 1).get(max_y - 1);
    Iterator<Pair<boolean[][], Integer>> iter = cache_line.iterator();
    Iterator<Position> piter = position_line.iterator();
    boolean searching = true;
    Pair<boolean[][], Integer> p = null;
    while (searching && iter.hasNext()) {
      p = iter.next();
      Position temp_position = piter.next();
      if (same_field(field, p.first, count, max_x, max_y)) {
        searching = false;
        winning_position = temp_position;
      }
    }
    if (searching) {
      // result = compute_scores(field, count, max_x, max_y, 0);

      Compute_Task my_task = new Compute_Task(overall_context, count);
      my_task.execute(field, count, max_x, max_y);
      /*try {
        result = my_task.get();
      } catch (Exception e) {

      }*/
      //ComputeThread thread = new ComputeThread(field);
      //thread.start();
      /*ProgressDialog waiter = new ProgressDialog(overall_context);
      waiter.setTitle(overall_context.getString(R.string.thinking));
      waiter.setMessage(overall_context.getString(R.string.please_wait));
      waiter.show();*/
    } else {
      zero_position = winning_position;
      result =p.second;
    }

    return result;

  }

  private class Compute_Task extends AsyncTask<Object, Integer, Integer> {

    /**
     * Creates a new asynchronous task. This constructor must be invoked on the UI thread.
     */
    public Compute_Task(Context context, int count) {

      super();

      current_positions = 0;

      update_dialog = new ProgressDialog(context);
      update_dialog.setMax(count);
      update_dialog.setIndeterminate(false);
      update_dialog.setCancelable(false);
      update_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      Activity activity = (Activity) context;
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
      ((TextView) ((Activity) overall_context).findViewById(R.id.value_view)).setText(integer.toString());
      update_dialog.dismiss();
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
      Integer result = this.compute_scores(
          (boolean[][]) params[0], (Integer) params[1], (Integer) params[2], (Integer) params[3], 0
      );
      return result;
    }

    public int compute_scores(boolean[][] field, int count, int max_x, int max_y, int level) {

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
            zero_position = find_min_pos(field, max_x, max_y);
          break;
        default:
          //String header = "";
          //for (int i = 0; i < level; ++i) header += "=";
          //Log.d(tag, header);
          //Log.d(tag, "Searching for:");
          //print_field(field, max_x, max_y);
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
            if (same_field(field, p.first, count, max_x, max_y)) {
              searching = false;
              winning_position = temp_position;
            }
          }
          if (!searching) {
            result = p.second;
            zero_position = winning_position;
            //Log.d(tag, "found with value " + String.valueOf(result));
            //print_field(p.first, max_x, max_y);
            //Log.d(tag, "----");
          } else {
            //Log.d(tag, "not found, computing");
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
                  int value = compute_scores(new_field, new_count, new_max_x, new_max_y, level + 1);
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
            cache_line.add(new Pair<>(field, result));
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

  protected Position find_min_pos(boolean[][] field, int max_x, int max_y) {
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
    return new Position(i, j);
  }

  Position hint_position() { return zero_position; }

  protected int base_count, base_max_x, base_max_y;
  protected Vector<Vector<Vector<LinkedList<Pair<boolean[][], Integer>>>>> cache;
  protected Vector<Vector<Vector<LinkedList<Position>>>> zero_positions;

  protected static final Position ORIGIN = new Position(0,0);
  protected Position zero_position = ORIGIN;

  protected Context overall_context;

  final private static String tag = "Game_Evaluation";

}
