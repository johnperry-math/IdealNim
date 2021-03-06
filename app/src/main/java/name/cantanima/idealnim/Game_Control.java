package name.cantanima.idealnim;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import static android.content.DialogInterface.BUTTON_POSITIVE;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static name.cantanima.idealnim.Game_Control.Dialog_Id.NEW_GAME;
import static name.cantanima.idealnim.Game_Control.Dialog_Id.PLAY_AGAIN;
import static name.cantanima.idealnim.Game_Control.Player_Kind.COMPUTER;
import static name.cantanima.idealnim.Game_Control.Player_Kind.HUMAN;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.APPRENTICE;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.CHANGED_BOARD_SIZE;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.CRAFTSMAN;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.DOCTOR_OF_IDEAL_NIM;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.EVERYONE_GETS_A_TROPHY;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.FAIR_PLAY;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.HONORABLE_MENTION;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.JOURNEYMAN;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.MASTER_CRAFTSMAN;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.ONE_HAND_BEHIND_MY_BACK;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.PATIENCE_A_VIRTUE;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.WON_LEVEL_5;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.WON_LEVEL_7;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.WON_LEVEL_9;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.GOOD_SPORT;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.ONLY_HUMAN;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.PENTAWIN;
import static name.cantanima.idealnim.MainActivity.Achievements_to_unlock.LEGEND_AMONG_KIND;

/**
 * Controls a game.
 */

class Game_Control implements DialogInterface.OnClickListener {

  Game_Control() {

    Calendar cal = new GregorianCalendar();
    current_seed = cal.get(MINUTE) * 59 + cal.get(HOUR_OF_DAY) * 23 + cal.get(SECOND) * 43
        + cal.get(DAY_OF_YEAR) * 365 + cal.get(YEAR) * 9999 + cal.get(SECOND);
    random.setSeed(current_seed);

  }

  void new_game(Playfield p, int max_x, int max_y, int level, boolean generate_ideals) {
    playfield = p;
    playfield.reset_view();
    playfield.reset_played();
    main_activity = (MainActivity) p.getContext();

    if ((playfield.opponent_is_computer() && generate_ideals) ||
        (!playfield.opponent_is_computer() && main_activity.i_host_the_game())
    ) {

      max_x = (max_x > 10) ? 10 : max_x;
      max_y = (max_y > 10) ? 10 : max_y;

      Ideal I;
      this.level = level;
      level = (level < 1) ? 1 : (level + 1) / 2;
      if (level != 5) {
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
      } else {
        int last_i, last_j;
        do {
          last_i = 0;
          last_j = max_y;
          I = new Ideal();
          //Log.d(tag, "----");
          for (int n = 0; n < 5; ++n) {
            int i = last_i + (random.nextInt(2) + 1);
            int j = last_j - (random.nextInt(2) + 1);
            if (j < 0) break;
            last_i = i;
            last_j = j;
            I.add_generator_fast(i, j);
            //Log.d(tag, String.valueOf(i) + ", " + String.valueOf(j));
          }
          //Log.d(tag, "----");
        } while (last_i >= max_x - 1 || I.T.size() < 5);
      }
      I.sort_ideal();
      int x_offset = (level < 3) ? 0 : I.T.peekFirst().get_x();
      int y_offset = (level < 3) ? 0 : I.T.peekLast().get_y();
      Ideal J = new Ideal();
      for (Position P : I.T)
        J.add_generator_fast(P.get_x() - x_offset, P.get_y() - y_offset);
      playfield.set_to(J);

      if (!playfield.opponent_is_computer())
        playfield.opponent.choose_a_position();
      else {
        AlertDialog.Builder first_builder = new AlertDialog.Builder(main_activity);
        first_builder.setTitle(main_activity.getString(R.string.new_game));
        first_builder.setMessage(main_activity.getString(R.string.who_first));
        first_builder.setPositiveButton(main_activity.getString(R.string.human), this);
        first_builder.setNegativeButton(main_activity.getString(R.string.computer), this);
        AlertDialog dialog = first_builder.create();
        Window dialog_window = dialog.getWindow();
        WindowManager.LayoutParams win_attr = dialog_window.getAttributes();
        //win_attr.gravity = Gravity.BOTTOM;
        win_attr.alpha = 0.85f;
        dialog_window.setAttributes(win_attr);
        dialog_window.setDimAmount(0.25f);
        dialog.show();
        last_dialog = NEW_GAME;
      }


    }

  }

  void notify_game_over() {

    handle_achievements();

    boolean two_player_game = main_activity.is_two_player_game();
    if (two_player_game) {
      main_activity.two_player_game_ended();
      playfield.set_opponent_to_computer();
    }

    AlertDialog.Builder last_builder = new AlertDialog.Builder(main_activity);
    last_builder.setTitle(main_activity.getString(R.string.game_over));
    String message, insult;
    if (last_player == COMPUTER) {
      if (two_player_game) {
        message = main_activity.getString(R.string.two_player_lost) + " " +
            main_activity.getString(R.string.two_player_won_lost);
      } else {
        String[] insults = main_activity.getResources().getStringArray(R.array.lose_insults);
        if (playfield.computer_sometimes_dumb && random.nextBoolean()) insult = insults[0];
        else insult = insults[random.nextInt(insults.length - 1) + 1];
        message = main_activity.getString(R.string.computer_won) + insult;
      }
      last_builder.setNegativeButton(main_activity.getString(R.string.dont_play_again_lose), this);
    } else {
      if (two_player_game) {
        message = main_activity.getString(R.string.two_player_won) + " " +
            main_activity.getString(R.string.two_player_won_lost);
      } else {
        String[] insults = main_activity.getResources().getStringArray(R.array.win_insults);
        if (playfield.computer_sometimes_dumb && random.nextBoolean()) insult = insults[0];
        else insult = insults[random.nextInt(insults.length - 1) + 1];
        message = main_activity.getString(R.string.player_won) + insult;
      }
      last_builder.setNegativeButton(main_activity.getString(R.string.dont_play_again_win), this);
    }
    last_builder.setMessage(message);
    last_builder.setPositiveButton(main_activity.getString(R.string.another_game), this);
    last_builder.show();
    last_dialog = PLAY_AGAIN;
  }

  private void handle_achievements() {

    if (main_activity.can_handle_achievements()) {

      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(main_activity);
      SharedPreferences.Editor edit = pref.edit();

      if (last_player != HUMAN) {

        playfield.consecutive_wins = 0;

        // only losers receive this achievement

        if (playfield.opponent_is_computer()) {

          if (!pref.contains(main_activity.getString(R.string.everyone_get_a_trophy))) {
            main_activity.unlock_achievement(EVERYONE_GETS_A_TROPHY);
            edit.putBoolean(main_activity.getString(R.string.everyone_get_a_trophy), true);
          }

        } else {

          if (!pref.contains(main_activity.getString(R.string.good_sport)) ||
              pref.getInt(main_activity.getString(R.string.good_sport), 0) < 5)
          {
            main_activity.increment_achievement(GOOD_SPORT);
            int losses = pref.getInt(main_activity.getString(R.string.good_sport), 0);
            edit.putInt(main_activity.getString(R.string.good_sport), ++losses);
          }

          if (pref.contains(main_activity.getString(R.string.legend_among_kind))) {
            edit.putInt(main_activity.getString(R.string.legend_among_kind), 0);
          }

        }

      } else {

        // the following achievements require human victory (sometimes multiple victories)

        if (!playfield.opponent_is_computer()) {

          if (!pref.contains(main_activity.getString(R.string.only_human))) {
            main_activity.unlock_achievement(ONLY_HUMAN);
            edit.putBoolean(main_activity.getString(R.string.only_human), true);
          }

          if (!pref.contains(main_activity.getString(R.string.pentawin)) ||
              pref.getInt(main_activity.getString(R.string.pentawin), 0) < 5)
          {
            main_activity.increment_achievement(PENTAWIN);
            int wins = pref.getInt(main_activity.getString(R.string.pentawin), 0);
            edit.putInt(main_activity.getString(R.string.pentawin), ++wins);
          }

          if (!pref.contains(main_activity.getString(R.string.legend_among_kind)) ||
              pref.getInt(main_activity.getString(R.string.legend_among_kind), 0) < 10)
          {
            int wins = pref.getInt(main_activity.getString(R.string.legend_among_kind), 0);
            edit.putInt(main_activity.getString(R.string.legend_among_kind), ++wins);
            if (wins == 10) {
              main_activity.unlock_achievement(LEGEND_AMONG_KIND);
            }
          }

        } else {

          if (!pref.contains(main_activity.getString(R.string.changed_board_size))) {
            main_activity.unlock_achievement(CHANGED_BOARD_SIZE);
            edit.putBoolean(main_activity.getString(R.string.changed_board_size), true);
          }

          if (!pref.contains(main_activity.getString(R.string.honorable_mention))) {
            main_activity.unlock_achievement(HONORABLE_MENTION);
            edit.putBoolean(main_activity.getString(R.string.honorable_mention), true);
          }

          if (
              !pref.contains(main_activity.getString(R.string.one_hand_behind_my_back)) &&
                  used_one_hand_behind_back
              ) {
            main_activity.unlock_achievement(ONE_HAND_BEHIND_MY_BACK);
            edit.putBoolean(main_activity.getString(R.string.one_hand_behind_my_back), true);
          }

          if (
              !pref.contains(main_activity.getString(R.string.won_level_5)) &&
                  playfield.game_level == 5
              ) {
            main_activity.unlock_achievement(WON_LEVEL_5);
            edit.putBoolean(main_activity.getString(R.string.won_level_5), true);
          }

          if (
              !pref.contains(main_activity.getString(R.string.won_level_7)) &&
                  playfield.game_level == 7
              ) {
            main_activity.unlock_achievement(WON_LEVEL_7);
            edit.putBoolean(main_activity.getString(R.string.won_level_7), true);
          }

          if (
              !pref.contains(main_activity.getString(R.string.won_level_9)) &&
                  playfield.game_level == 9
              ) {
            main_activity.unlock_achievement(WON_LEVEL_9);
            edit.putBoolean(main_activity.getString(R.string.won_level_9), true);
          }

          boolean cheated = used_hint || used_one_hand_behind_back;

          if (!pref.contains(main_activity.getString(R.string.fair_play)) && !cheated) {
            main_activity.unlock_achievement(FAIR_PLAY);
            edit.putBoolean(main_activity.getString(R.string.fair_play), true);
          }

          if (
              !pref.contains(main_activity.getString(R.string.patience_a_virtue)) &&
                  computed_large_board
              ) {
            main_activity.unlock_achievement(PATIENCE_A_VIRTUE);
            edit.putBoolean(main_activity.getString(R.string.patience_a_virtue), true);
          }

          // the final achievements all require fair play

          if (!cheated) {

            int level = playfield.game_level;

            if (
                level == 2 &&
                    (!pref.contains(main_activity.getString(R.string.apprentice)) ||
                        pref.getInt(main_activity.getString(R.string.apprentice), 0) < 1)
                ) {
              main_activity.increment_achievement(APPRENTICE);
              edit.putInt(main_activity.getString(R.string.apprentice), 1);
            }
            if (
                level == 4 &&
                    (!pref.contains(main_activity.getString(R.string.journeyman)) ||
                        pref.getInt(main_activity.getString(R.string.journeyman), 0) < 2)
                ) {
              main_activity.increment_achievement(JOURNEYMAN);
              int wins = pref.getInt(main_activity.getString(R.string.journeyman), 0);
              edit.putInt(main_activity.getString(R.string.journeyman), ++wins);
            }
            if (
                level == 6 &&
                    (!pref.contains(main_activity.getString(R.string.craftsman)) ||
                        pref.getInt(main_activity.getString(R.string.craftsman), 0) < 3)
                ) {
              main_activity.increment_achievement(CRAFTSMAN);
              int wins = pref.getInt(main_activity.getString(R.string.craftsman), 0);
              edit.putInt(main_activity.getString(R.string.craftsman), ++wins);
            }
            if (
                level == 8 &&
                    (!pref.contains(main_activity.getString(R.string.master_craftsman)) ||
                        pref.getInt(main_activity.getString(R.string.master_craftsman), 0) < 4)
                ) {
              main_activity.increment_achievement(MASTER_CRAFTSMAN);
              int wins = pref.getInt(main_activity.getString(R.string.master_craftsman), 0);
              edit.putInt(main_activity.getString(R.string.master_craftsman), ++wins);
            }
            if (level != 8 && level != 10)
              playfield.consecutive_wins = 0;
            else if (level % 2 == 0) {
              ++playfield.consecutive_wins;
              if (
                  playfield.consecutive_wins >= 5 &&
                      !pref.contains(main_activity.getString(R.string.doctor_ideal_nim))
                  ) {
                main_activity.unlock_achievement(DOCTOR_OF_IDEAL_NIM);
                edit.putBoolean(main_activity.getString(R.string.doctor_ideal_nim), true);
              }
            }

          }

        }

      }

    edit.commit();

    }

  }

  void set_player_kind(Player_Kind kind) { last_player = kind; }

  Player_Kind get_player_kind() { return last_player; }

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
        playfield.opponent.choose_a_position();
      }
    } else if (last_dialog == PLAY_AGAIN) {
      if (which == BUTTON_POSITIVE) {
        new_game(playfield, playfield.view_xmax, playfield.view_ymax, level, true);
      }
    }

  }

  void notify_requested_a_hint() { used_hint = true; }

  public void notify_changed_board_size() { changed_size = true;}

  void notify_computer_sometimes_dumb() { used_one_hand_behind_back = true; }

  void notify_large_board() { computed_large_board = true; }

  private int level = 1;
  private long current_seed;
  private Playfield playfield;
  private MainActivity main_activity;
  Random random = new Random();

  enum Player_Kind { COMPUTER, HUMAN };
  private Player_Kind last_player;

  enum Dialog_Id { NEW_GAME, PLAY_AGAIN };
  private Dialog_Id last_dialog;

  // for achievements
  private boolean
      changed_size = false, used_hint = false, used_one_hand_behind_back = false,
      computed_large_board = false
  ;

  final private static String tag = "Game_Control";

}
