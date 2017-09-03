package name.cantanima.idealnim;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
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
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static name.cantanima.idealnim.Position.ORIGIN;

/**
 * Created by cantanima on 8/15/17.
 */

public class Computer_Opponent extends Opponent {

  public Computer_Opponent(
      Context context, Ideal I, Ideal J, int level
  ) {

    super(context, I, J, level);

    int view_xmax = base_playable.T.peekLast().get_x();
    int view_ymax = base_playable.T.peekFirst().get_y();
    if (J != null)
      base_played = new Ideal(J);
    else {
      base_played = new Ideal();
    }
    base_max_x = view_xmax + 5;
    base_max_y = view_ymax + 5;
    int initial_map_size = 3000; // based on experimental observation (examination of cache.size())
    cache = null;
    try {
      cache = new HashMap<>(initial_map_size);
    } catch (OutOfMemoryError e) {
      AlertDialog.Builder builder = new AlertDialog.Builder(overall_context)
          .setTitle(R.string.out_of_memory_title)
          .setMessage(R.string.out_of_memory_message)
          .setPositiveButton(R.string.understood, null);
    }

  }

  @Override
  public void update_with_position(int i, int j) {

    super.update_with_position(i,j);
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

  }

  Position hint_position() { return zero_position; }

  public void choose_a_position() {
    computer_move = true;
    game_value();
  }

  public int game_value() {

    int result = 0;
    boolean decided = false;
    Position winning_position = ORIGIN;

    if (game_level == 1 || game_level == 2) {

      // on levels 1, 2 it is easy to decide the winning move
      decided = true;
      zero_position = base_playable.T.getFirst();

    } else if (game_level == 3 || game_level == 4) {

      // easy to decide if we can create symmetry, in which case the winning move is that one
      decided = quickly_analyze_two_corner_game();

    } else if (game_level == 5 || game_level == 6) {

      // relatively easy to decide if we can create symmetry,
      // in which case the winning move is that one
      decided = quickly_analyze_three_corner_game();

    }

    if (decided && computer_move) {
      Playfield playfield = (Playfield) overall_context.findViewById(R.id.playfield);
      playfield.hint_position = zero_position;
      computer_move = false;
      playfield.get_computer_move();
    }

    if (!decided) {

      // harder cases

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
          // last position is a generator of playable that is not also a generator of played
          Iterator<Position> Ti = base_playable.T.iterator();
          Iterator<Position> Ui = base_played.T.iterator();
          Position position, invalid_position;
          do {
            position = Ti.next();
            invalid_position = Ui.next();
          } while (position.equals(invalid_position));
          zero_position = position;
          if (computer_move) {
            Playfield playfield = (Playfield) overall_context.findViewById(R.id.playfield);
            playfield.hint_position = zero_position;
            computer_move = false;
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
          computer_move = false;
          playfield.get_computer_move();
        }
      }

    }

    return result;

  }

  public boolean quickly_analyze_three_corner_game() {

    // first find the optimal first position, which forces symmetric moves -- IF IT EXISTS
    Iterator<Position> Ti = base_playable.iterator();
    Ti.next();
    Position Q = Ti.next();
    // for symmetry
    boolean result = Q.get_x() == Q.get_y();
    if (result && base_played.T.size() != 0) {
      Iterator<Position> Ui = base_played.iterator();
      while (result && Ui.hasNext())
        result = Q.generates(Ui.next());
    }
    if (result) {
      zero_position = Q;
    }

    return result;

  }

  public boolean quickly_analyze_two_corner_game() {

    boolean result = true;

    // first find the optimal first position, which forces symmetric moves
    Position P = base_playable.T.peekFirst(), Q = base_playable.T.peekLast();
    int a = P.get_x(), b = P.get_y(), c = Q.get_x(), d = Q.get_y();
    int delta_x = c - a, delta_y = b - d, i, j, comp_lx, comp_ly, comp_rx, comp_ry;
    Position S;
    if (delta_x <= delta_y) {
      i = c;
      j = d + delta_x;
      comp_lx = c; comp_ly = b;
      comp_rx = i; comp_ry = j;
    } else {
      i = a + delta_y;
      j = b;
      comp_lx = i; comp_ly = j;
      comp_rx = c; comp_ry = b;
    }
    S = new Position(i, j);

    if (base_played.contains(S.get_x(), S.get_y())) {

      // if S has already been played, see if we can obtain symmetry

      int num_asym = 0;
      Iterator<Position> Ti = base_played.T.iterator();
      Position T, U = ORIGIN;
      while (Ti.hasNext() && num_asym < 2) {
        T = Ti.next();
        if (T.lies_left_of(S)) {
          if (!base_played.contains(comp_rx + T.get_y() - comp_ly, comp_ry + T.get_x() - comp_lx)) {
            ++num_asym;
            U = new Position(comp_rx + T.get_y() - comp_ly, comp_ry + T.get_x() - comp_lx);
          }
        } else if (!T.equals(S)){
          if (!base_played.contains(comp_lx + T.get_y() - comp_ry, comp_ly + T.get_x() - comp_rx)) {
            ++num_asym;
            U = new Position(comp_lx + T.get_y() - comp_ry, comp_ly + T.get_x() - comp_rx);
          }
        }
      }
      result = num_asym < 2;
      if (result)
        zero_position = U;

    } else if (base_played.T.size() != 0) {

      // if S has not been played, see if playing S would "swallow" the previous plays

      for (Position T : base_played.T) {
        result = S.generates(T);
        if (!result) break;
      }
      if (result) zero_position = S;

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
      /*Log.d(tag, "times: " + String.valueOf(end_time.getTime()) + ", " + start_time.getTime());
      Log.d(tag, "entries: " + String.valueOf(cache.size()));*/
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
          if (cache.size() == 0) seed_cache_with_known_values();
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
            // work our way up to largest x, y values, short-circuiting when a winning move is found
            for (int i = 0; i <= max_x && winning_position == ORIGIN; ++i) {
              for (int j = 0; j <= max_y && winning_position == ORIGIN; ++j) {
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

  public void seed_cache_with_known_values() {

    seed_with_dozier_configurations();
    seed_with_border_configurations();

  }

  private void seed_with_dozier_configurations() {

    Ideal I = base_playable;
    if (I.T.size() < 3) return;
    Iterator<Position> Ti = I.T.iterator();
    Position first = Ti.next();
    Position second = Ti.next();
    Position third = Ti.next();

    // Haley Dozier's configurations: vertical version
    if (second.get_x() == first.get_x() + 1) {
      ArrayList<Integer> gens = new ArrayList<>(I.T.size() * 2 - 2);
      Iterator<Position> Ui = I.T.iterator();
      Ui.next(); Ui.next();
      int iter_offset;
      if (third.get_x() == second.get_x() + 1) {
        gens.add(third.get_x());
        gens.add(third.get_y());
        Ui.next();
        iter_offset = 3;
      } else {
        gens.add(second.get_x() + 1);
        gens.add(second.get_y());
        iter_offset = 2;
      }
      while (Ui.hasNext()) {
        Position Q = Ui.next();
        gens.add(Q.get_x());
        gens.add(Q.get_y());
      }
      //Log.d(tag, "---- caching configuration ----");
      //Log.d(tag, String.valueOf(0));
      //print_configuration(gens, base_max_x, base_max_y);
      cache.put(gens, new Pair<>(0, ORIGIN));
      // easy zeros
      for (int i = 1; i < first.get_y() + base_max_y; ++i) {
        ArrayList<Integer> future_gens = new ArrayList<>(I.T.size() * 2 + 2);
        future_gens.add(first.get_x());
        future_gens.add(first.get_y() + i);
        future_gens.add(second.get_x());
        future_gens.add(second.get_y() + i);
        Ui = I.T.iterator();
        Ui.next(); Ui.next();
        if (third.get_x() != second.get_x() + 1) {
          future_gens.add(second.get_x() + 1);
          future_gens.add(second.get_y());
        }
        while (Ui.hasNext()) {
          Position Q = Ui.next();
          future_gens.add(Q.get_x());
          future_gens.add(Q.get_y());
        }
        //Log.d(tag, "---- caching configuration ----");
        //Log.d(tag, String.valueOf(0));
        //print_configuration(future_gens, base_max_x, base_max_y);
        cache.put(future_gens, new Pair<>(0, ORIGIN));
      }
    } else {
      ArrayList<Integer> gens = new ArrayList<>(I.T.size() * 2 - 2);
      Iterator<Position> Ui = I.T.iterator();
      Ui.next();
      if (second.get_x() == first.get_x() + 2) {
        gens.add(second.get_x());
        gens.add(second.get_y());
        Ui.next();
      } else {
        gens.add(first.get_x() + 2);
        gens.add(first.get_y());
      }
      while (Ui.hasNext()) {
        Position P = Ui.next();
        gens.add(P.get_x());
        gens.add(P.get_y());
      }
      //Log.d(tag, "---- caching configuration ----");
      //Log.d(tag, String.valueOf(1));
      //print_configuration(gens, base_max_x, base_max_y);
      cache.put(gens, new Pair<>(1, first));
      // easy 1's
      for (int i = 1; i < base_max_y; ++i) {
        ArrayList<Integer> future_gens = new ArrayList<>(I.T.size() * 2 + 2);
        future_gens.add(first.get_x());
        future_gens.add(first.get_y() + i);
        future_gens.add(first.get_x() + 1);
        future_gens.add(first.get_y() + i - 1);
        Ui = I.T.iterator();
        Ui.next();
        if (second.get_x() == first.get_x() + 2) {
          future_gens.add(second.get_x());
          future_gens.add(second.get_y());
          Ui.next();
        } else {
          future_gens.add(first.get_x() + 2);
          future_gens.add(first.get_y());
        }
        while (Ui.hasNext()) {
          Position P = Ui.next();
          future_gens.add(P.get_x());
          future_gens.add(P.get_y());
        }
        //Log.d(tag, "---- caching configuration ----");
        //Log.d(tag, String.valueOf(1));
        //print_configuration(future_gens, base_max_x, base_max_y);
        cache.put(future_gens, new Pair<>(1, first));
      }
      // easy non-1's
      for (int i = 1; i < base_max_y; ++i) {
        ArrayList<Integer> future_gens = new ArrayList<>(I.T.size() * 2 + 2);
        future_gens.add(first.get_x());
        future_gens.add(first.get_y() + i);
        Ui = I.T.iterator();
        Ui.next();
        if (second.get_x() == first.get_x() + 2) {
          future_gens.add(second.get_x());
          future_gens.add(second.get_y());
          Ui.next();
        } else {
          future_gens.add(first.get_x() + 2);
          future_gens.add(first.get_y());
        }
        while (Ui.hasNext()) {
          Position P = Ui.next();
          future_gens.add(P.get_x());
          future_gens.add(P.get_y());
        }
        int value = i + ((i + 1) / 2);
        //Log.d(tag, "---- caching configuration ----");
        //Log.d(tag, String.valueOf(value));
        //print_configuration(future_gens, base_max_x, base_max_y);
        cache.put(future_gens, new Pair<>(value, first));
      }

    }

    while (Ti.hasNext()) {
      first = second;
      second = third;
      third = Ti.next();
    }

    // Haley Dozier's configurations: horizontal version
    if (second.get_y() == third.get_y() + 1) {
      ArrayList<Integer> gens = new ArrayList<>(I.T.size() * 2 - 2);
      Iterator<Position> Ui = I.T.iterator();
      Position U = Ui.next();
      do {
        gens.add(U.get_x());
        gens.add(U.get_y());
        U = Ui.next();
      } while (U != second);
      if (first.get_y() != second.get_y() + 1) {
        gens.add(second.get_x());
        gens.add(second.get_y() + 1);
      }
      //Log.d(tag, "---- caching configuration ----");
      //Log.d(tag, String.valueOf(0));
      //print_configuration(gens, base_max_x, base_max_y);
      cache.put(gens, new Pair<>(0, ORIGIN));
      // easy zeros
      for (int i = 1; i < third.get_x() + base_max_x; ++i) {
        ArrayList<Integer> future_gens = new ArrayList<>(I.T.size() * 2 + 2);
        Ui = I.T.iterator();
        U = Ui.next();
        do {
          future_gens.add(U.get_x());
          future_gens.add(U.get_y());
          U = Ui.next();
        } while (U != second);
        if (first.get_y() != second.get_y() + 1) {
          future_gens.add(second.get_x());
          future_gens.add(second.get_y() + 1);
        }
        future_gens.add(second.get_x() + i);
        future_gens.add(second.get_y());
        future_gens.add(third.get_x() + i);
        future_gens.add(third.get_y());
        //Log.d(tag, "---- caching configuration ----");
        //Log.d(tag, String.valueOf(0));
        //print_configuration(future_gens, base_max_x, base_max_y);
        cache.put(future_gens, new Pair<>(0, ORIGIN));
      }
    } else {
      ArrayList<Integer> gens = new ArrayList<>(I.T.size() * 2 - 2);
      Iterator<Position> Ui = I.T.iterator();
      Position P = Ui.next();
      while (P != third) {
        gens.add(P.get_x());
        gens.add(P.get_y());
        P = Ui.next();
      }
      if (second.get_y() != third.get_y() + 2) {
        gens.add(third.get_x());
        gens.add(third.get_y() + 2);
      }
      //Log.d(tag, "---- caching configuration ----");
      //Log.d(tag, String.valueOf(1));
      //print_configuration(gens, base_max_x, base_max_y);
      cache.put(gens, new Pair<>(1, third));
      // easy 1's
      for (int i = 1; i < base_max_y; ++i) {
        ArrayList<Integer> future_gens = new ArrayList<>(I.T.size() * 2 + 2);
        Ui = I.T.iterator();
        P = Ui.next();
        while (P != third) {
          future_gens.add(P.get_x());
          future_gens.add(P.get_y());
          P = Ui.next();
        }
        if (second.get_y() != third.get_y() + 2) {
          future_gens.add(third.get_x());
          future_gens.add(third.get_y() + 2);
        }
        future_gens.add(third.get_x() + i);
        future_gens.add(third.get_y());
        future_gens.add(third.get_x() + i - 1);
        future_gens.add(third.get_y() + 1);
        //Log.d(tag, "---- caching configuration ----");
        //Log.d(tag, String.valueOf(1));
        //print_configuration(future_gens, base_max_x, base_max_y);
        cache.put(future_gens, new Pair<>(1, first));
      }
      // easy non-1's
      for (int i = 1; i < base_max_y; ++i) {
        ArrayList<Integer> future_gens = new ArrayList<>(I.T.size() * 2 + 2);
        Ui = I.T.iterator();
        P = Ui.next();
        while (P != third) {
          future_gens.add(P.get_x());
          future_gens.add(P.get_y());
          P = Ui.next();
        }
        if (second.get_y() != third.get_y() + 2) {
          future_gens.add(third.get_x());
          future_gens.add(third.get_y() + 2);
        }
        future_gens.add(third.get_x() + i);
        future_gens.add(third.get_y());
        int value = i + ((i + 1) / 2);
        //Log.d(tag, "---- caching configuration ----");
        //Log.d(tag, String.valueOf(value));
        //print_configuration(future_gens, base_max_x, base_max_y);
        cache.put(future_gens, new Pair<>(value, first));
      }

    }

  }

  private void seed_with_border_configurations() {

    if (base_playable.T.size() >= 2) {

      Iterator<Position> Ti = base_playable.T.iterator();
      Position P, Q = Ti.next();
      do {
        P = Q;
        Q = Ti.next();
        int a = Q.get_x() - P.get_x(), b = P.get_y() - Q.get_y();
        int value = border_configuration_value(a, b);
        Position winning_position;
        if (a == b)
          winning_position = new Position(Q.get_x(), P.get_y());
        else if (a > b)
          winning_position = new Position(P.get_x() + b, P.get_y());
        else
          winning_position = new Position(Q.get_x(), Q.get_y() + a);
        Iterator<Position> Ui = base_playable.T.iterator();
        ArrayList<Integer> gens = new ArrayList<>(base_playable.T.size() * 2);
        while (Ui.hasNext()) {
          Position R = Ui.next();
          if (R == P) {
            gens.add(R.get_x());
            gens.add(R.get_y() + 1);
          } else if (R == Q) {
            gens.add(R.get_x() + 1);
            gens.add(R.get_y());
          } else {
            gens.add(R.get_x());
            gens.add(R.get_y());
          }
        }
        //Log.d(tag, "---- caching configuration ----");
        //Log.d(tag, String.valueOf(value));
        //print_configuration(gens, base_max_x, base_max_y);
        cache.put(gens, new Pair<>(value, winning_position));
      } while (Ti.hasNext());

    }

  }

  private int border_configuration_value(int m, int n) {

    int result;

    if (m < n) {
      int temp = m;
      m = n;
      n = temp;
    }

    int i = 0, j = 0, k = 0;
    while (m != 0 && ((m % 2 != 0) || (n % 2 != 0))) {
      m /= 2;
      n /= 2;
      ++i;
    }
    j = k = i;
    result = 1 << i;
    while (n != 0) {
      if ((m % 2) != (n % 2)) {
        result += (2 << k);
        if (m % 2 == 1)
          j = k;
      }
      ++k;
      m /= 2;
      n /= 2;
    }
    if (j != i)
      while (j != k) {
        ++j;
        result -= (1 << j);
      }

    return result;

  }

  protected void print_configuration(ArrayList<Integer> config, int max_x, int max_y) {
    boolean [][] bconfig = new boolean[max_x][max_y];
    for (int i = 0; i < max_x; ++i)
      for (int j = 0; j < max_y; ++j)
        bconfig[i][j] = base_playable.contains(i, j);
    for (int i = 0; i < config.size(); i += 2)
      for (int k = config.get(i); k < max_x; ++k)
        for (int l = config.get(i + 1); l < max_y; ++l) {
          if (!bconfig[k][l]) break;
          else bconfig[k][l] = false;
        }
    print_configuration(bconfig, max_x, max_y);
  }

  protected void print_configuration(boolean[][] config, int max_x, int max_y) {
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

  protected int base_max_x, base_max_y;
  protected HashMap<ArrayList<Integer>, Pair<Integer, Position>> cache;

  protected Position zero_position = ORIGIN;
  protected int configuration_value;

  protected boolean computer_move;

  final private static String tag = "Computer_Opponent";

}
