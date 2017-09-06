package name.cantanima.idealnim;

import android.app.Activity;
import android.content.Context;

/**
 * Created by cantanima on 9/2/17.
 */

public abstract class Opponent {

  public Opponent(Context context, Ideal I, Ideal J, int level) {

    overall_context = (Activity) context;
    game_level = level;

    base_playable = new Ideal(I);
    base_playable.sort_ideal();

  }

  public abstract void choose_a_position();

  /**
   * Invoked when the other player (NOT {@code this}) has played the point (i,j).
   * The default behavior is to add (i,j) to the playfield, then check whether the game is over.
   * If so, game control is notified automatically.
   * @param i x-value of the point played
   * @param j y-value of the point played
   */
  public void update_with_position(int i, int j) {
    Playfield p = (Playfield) overall_context.findViewById(R.id.playfield);
    base_played.add_generator(i, j, true);
    p.invalidate();
    //if (base_playable.equals(base_played))
    //  p.game_control.notify_game_over();
  }

  public void set_playable(Ideal I) { base_playable = I; }

  protected Ideal base_playable, base_played;

  protected Activity overall_context;
  protected int game_level;

}
