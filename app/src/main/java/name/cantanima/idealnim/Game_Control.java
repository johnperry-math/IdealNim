package name.cantanima.idealnim;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import static android.R.id.message;
import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static java.lang.Math.max;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static name.cantanima.idealnim.Game_Control.Dialog_Id.NEW_GAME;
import static name.cantanima.idealnim.Game_Control.Dialog_Id.PLAY_AGAIN;
import static name.cantanima.idealnim.Game_Control.Player_Kind.COMPUTER;
import static name.cantanima.idealnim.Game_Control.Player_Kind.HUMAN;

/**
 * Created by cantanima on 8/8/17.
 */

public class Game_Control implements DialogInterface.OnClickListener {

  public Game_Control() {

    Calendar cal = new GregorianCalendar();
    current_seed = cal.get(MINUTE) * 59 + cal.get(HOUR_OF_DAY) * 23 + cal.get(SECOND) * 43
        + cal.get(DAY_OF_YEAR) * 365 + cal.get(YEAR) * 9999 + cal.get(SECOND);
    random.setSeed(current_seed);

  }

  public void new_game(Playfield p, int max_x, int max_y, int level) {
    playfield = p;
    main_activity = p.getContext();

    Ideal I;
    this.level = level;
    level = (level < 1) ? 1 : (level + 1) / 2;
    do {
      I = new Ideal();
      //Log.d(tag, "----");
      for (int n = 0; n < level; ++n) {
        int i = random.nextInt(3 * max_x / 4);
        int j = random.nextInt(3 * max_y / 4);
        //Log.d(tag, String.valueOf(i) + ", " + String.valueOf(j));
        I.add_generator_fast(i, j);
      }
      //Log.d(tag, String.valueOf(I.T.size()) + " = " + String.valueOf(level));

      //Log.d(tag, "----");
    } while (I.T.size() < level);
    I.sort_ideal();
    playfield.set_to(I);
    playfield.invalidate();

    AlertDialog.Builder first_builder = new AlertDialog.Builder(main_activity);
    first_builder.setTitle(main_activity.getString(R.string.new_game));
    first_builder.setMessage(main_activity.getString(R.string.who_first));
    first_builder.setPositiveButton(main_activity.getString(R.string.human), this);
    first_builder.setNegativeButton(main_activity.getString(R.string.computer), this);
    first_builder.show();
    last_dialog = NEW_GAME;


  }

  public void notify_game_over() {
    AlertDialog.Builder last_builder = new AlertDialog.Builder(main_activity);
    last_builder.setTitle(main_activity.getString(R.string.game_over));
    //String message = main_activity.getString(R.string.play_again);
    String message;
    if (last_player == COMPUTER) {
      String[] insults = main_activity.getResources().getStringArray(R.array.lose_insults);
      String insult;
      if (playfield.computer_sometimes_dumb && random.nextBoolean()) insult = insults[0];
      else insult = insults[random.nextInt(insults.length - 1) + 1];
      message = main_activity.getString(R.string.computer_won) + insult;
      last_builder.setNegativeButton(main_activity.getString(R.string.dont_play_again_lose), this);
    } else {
      String[] insults = main_activity.getResources().getStringArray(R.array.win_insults);
      String insult;
      if (playfield.computer_sometimes_dumb && random.nextBoolean()) insult = insults[0];
      else insult = insults[random.nextInt(insults.length - 1) + 1];
      message = main_activity.getString(R.string.player_won) + insult;
      last_builder.setNegativeButton(main_activity.getString(R.string.dont_play_again_win), this);
    }
    last_builder.setMessage(message);
    last_builder.setPositiveButton(main_activity.getString(R.string.another_game), this);
    last_builder.show();
    last_dialog = PLAY_AGAIN;
  }

  public void set_player_kind(Player_Kind kind) { last_player = kind; }

  public Player_Kind get_player_kind() { return last_player; }

  /**
   * This method will be invoked when a button in the dialog is clicked.
   *
   * @param dialog The dialog that received the click.
   * @param which  The button that was clicked (e.g.
   *               {@link DialogInterface#BUTTON1}) or the position
   */
  @Override
  public void onClick(DialogInterface dialog, int which) {

    if (last_dialog == NEW_GAME) {
      if (which == BUTTON_POSITIVE)
        last_player = HUMAN;
      else {
        last_player = COMPUTER;
        playfield.evaluator.choose_computer_move();
      }
    } else if (last_dialog == PLAY_AGAIN) {
      if (which == BUTTON_POSITIVE)
        new_game(playfield, playfield.view_xmax, playfield.view_ymax, level);
    }

  }

  protected int level = 1;
  protected long current_seed;
  protected Playfield playfield;
  protected Context main_activity;
  protected Random random = new Random();

  protected enum Player_Kind { COMPUTER, HUMAN };
  protected Player_Kind last_player;

  protected enum Dialog_Id { NEW_GAME, PLAY_AGAIN };
  protected Dialog_Id last_dialog;

  final private static String tag = "Game_Control";

}
