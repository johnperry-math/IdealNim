package name.cantanima.idealnim;

import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

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

  protected int level = 1;
  protected long current_seed;
  protected Playfield playfield;
  protected Random random = new Random();

  private String tag = "Game_Control";

}
