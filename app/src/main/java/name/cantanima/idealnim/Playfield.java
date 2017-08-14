package name.cantanima.idealnim;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

import java.util.Iterator;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.BLUE;
import static android.graphics.Color.GRAY;
import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.graphics.Color.YELLOW;
import static android.graphics.Paint.Style.FILL;
import static android.graphics.Paint.Style.STROKE;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static name.cantanima.idealnim.Game_Control.Player_Kind.COMPUTER;
import static name.cantanima.idealnim.Game_Control.Player_Kind.HUMAN;
import static name.cantanima.idealnim.Game_Evaluation.ORIGIN;

/**
 * Created by cantanima on 8/8/17.
 */

public class Playfield
    extends View
    implements OnTouchListener, OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener
{

  public Playfield(Context context, AttributeSet attrs) {

    super(context, attrs);
    playable = new Ideal();
    played = new Ideal();
    if (isInEditMode()) {
      played.add_generator_fast(3, 8);
      played.add_generator_fast(8, 2);
      played.add_generator_fast(2, 9);
      played.sort_ideal();
      playable.add_generator_fast(0, 5);
      playable.add_generator_fast(3, 2);
      playable.add_generator_fast(5, 1);
      playable.sort_ideal();
    } else {
      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
      pref.registerOnSharedPreferenceChangeListener(this);
      if (pref.contains(context.getString(R.string.level_pref)))
        game_level = pref.getInt(context.getString(R.string.level_pref), 1);
      if (pref.contains(context.getString(R.string.max_pref_key)))
        view_xmax = view_ymax = pref.getInt(context.getString(R.string.max_pref_key), 7);
      if (pref.contains(context.getString(R.string.stupid_pref_key)))
        computer_sometimes_dumb = pref.getBoolean(context.getString(R.string.stupid_pref_key), true);
      if (pref.contains(context.getString(R.string.bg_color_key)))
        background_color = pref.getInt(context.getString(R.string.bg_color_key), background_color);
      if (pref.contains(context.getString(R.string.playable_color_key)))
        playable_color = pref.getInt(context.getString(R.string.playable_color_key), playable_color);
      if (pref.contains(context.getString(R.string.played_color_key)))
        played_color = pref.getInt(context.getString(R.string.played_color_key), played_color);
      if (pref.contains(context.getString(R.string.highlight_color_key)))
        highlight_color = pref.getInt(context.getString(R.string.highlight_color_key), highlight_color);
      if (pref.contains(context.getString(R.string.hint_color_key)))
        hint_color = pref.getInt(context.getString(R.string.hint_color_key), hint_color);
      if (pref.contains(context.getString(R.string.invalid_color_key)))
        invalid_color = pref.getInt(context.getString(R.string.invalid_color_key), invalid_color);
      game_control = new Game_Control();
      game_control.new_game(this, view_xmax, view_ymax, game_level);
      SharedPreferences.Editor editor = pref.edit();
      editor.putBoolean(context.getString(R.string.stupid_pref_key), computer_sometimes_dumb);
      editor.putInt(context.getString(R.string.level_pref), game_level);
      editor.putInt(context.getString(R.string.max_pref_key), view_xmax);
      editor.putInt(context.getString(R.string.bg_color_key), background_color);
      editor.putInt(context.getString(R.string.playable_color_key), playable_color);
      editor.putInt(context.getString(R.string.played_color_key), played_color);
      editor.putInt(context.getString(R.string.highlight_color_key), highlight_color);
      editor.putInt(context.getString(R.string.hint_color_key), hint_color);
      editor.putInt(context.getString(R.string.invalid_color_key), invalid_color);
      editor.apply();
    }
    setOnTouchListener(this);

  }

  /**
   * Determines the height and width of a Playfield: this forces it to be square, and fits it
   * into the allowed space given by widthSpec and heightSpec.
   * @param widthSpec allowed width
   * @param heightSpec allowed height
   */
  @Override
  public void onMeasure(int widthSpec, int heightSpec) {

    super.onMeasure(widthSpec, heightSpec);

    int w = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
    int h = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
    if (w < h) h = w;
    else if (h < w) w = h;

    setMeasuredDimension(
        w + getPaddingLeft() + getPaddingRight(), h + getPaddingTop() + getPaddingBottom()
    );

    bg_paint.setShader(
        new LinearGradient(0, h, w, 0, background_color, disappear_color, Shader.TileMode.CLAMP)
    );
    played_paint.setShader(
        new LinearGradient(0, h, w, 0, played_color, disappear_color, Shader.TileMode.CLAMP)
    );

  }

  /**
   * Sets the playable ideal to {@code F} and clears the played ideal.
   * The Ideal {@code F} is copied.
   * @param F ideal of playable positions
   */
  public void set_to(Ideal F) {

    playable = new Ideal(F);
    played = new Ideal();
    hinting = false;
    if (value_text != null)
      value_text.setText(getContext().getString(R.string.unknown_game_value));

    evaluator = new Game_Evaluation(getContext(), playable, null, view_xmax, view_ymax, game_level);

  }

  public void set_view_xmax(int x) { view_xmax = x; }

  public void set_view_ymax(int y) { view_ymax = y; }

  public void set_background_color(int color) { background_color = color; }

  public void set_playable_color(int color) { playable_color = color; }

  public void set_played_color(int color) { played_color = color; }

  /**
   * This is called during layout when the size of this view has changed. If
   * you were just added to the view hierarchy, you're called with the old
   * values of 0.
   *
   * @param w    Current width of this view.
   * @param h    Current height of this view.
   * @param oldw Old width of this view.
   * @param oldh Old height of this view.
   */
  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    step_x = ((float) w) / view_xmax;
    step_y = ((float) h) / view_ymax;

  }

  /**
   * Implement this to do your drawing.
   *
   * @param canvas the canvas on which the background will be drawn
   */
  @Override
  protected void onDraw(Canvas canvas) {

    super.onDraw(canvas);

    bg_paint.setColor(background_color);
    bg_paint.setStyle(FILL);
    line_paint.setColor(BLACK);
    line_paint.setStrokeWidth(1);
    line_paint.setStyle(STROKE);
    playable_paint.setColor(playable_color);
    playable_paint.setStyle(FILL);
    played_paint.setColor(played_color);
    played_paint.setStyle(FILL);
    invalid_paint.setColor(invalid_color);
    invalid_paint.setStyle(FILL);
    highlight_paint.setColor(highlight_color);
    highlight_paint.setStyle(FILL);
    hint_paint.setColor(hint_color);
    hint_paint.setStyle(FILL);

    float h = getHeight(), w = getWidth();
    canvas.drawRect(0, 0, w, h, bg_paint);

    if (playable.T.size() != 0) {
      playable_path.rewind();
      playable_path.moveTo(w, 0);
      Iterator<Position> Ti = playable.iterator();
      Position P = Ti.next();
      playable_path.lineTo(P.get_x() * step_x, 0);
      if (P.get_y() < view_ymax)
        playable_path.lineTo(P.get_x() * step_x, h - P.get_y() * step_y);
      while (Ti.hasNext()) {
        Position Q = Ti.next();
        if (Q.get_y() < view_ymax) {
          playable_path.lineTo(Q.get_x() * step_x, h - P.get_y() * step_y);
          playable_path.lineTo(Q.get_x() * step_x, h - Q.get_y() * step_y);
        }
        P = Q;
      }
      if (P.get_x() < view_xmax)
        playable_path.lineTo(w, h - P.get_y() * step_y);
      playable_path.close();
      canvas.drawPath(playable_path, playable_paint);
    }

    if (played.T.size() != 0) {
      played_path.rewind();
      played_path.moveTo(w, 0);
      Iterator<Position> Ti = played.iterator();
      Position P = Ti.next();
      if (P.get_x() == 0) {
        played_path.lineTo(0, 0);
        played_path.lineTo(0, h - P.get_y() * step_y);
      } else {
        played_path.lineTo(P.get_x() * step_x, 0);
        played_path.lineTo(P.get_x() * step_x, h - P.get_y() * step_y);
      }
      while (Ti.hasNext()) {
        Position Q = Ti.next();
        played_path.lineTo(Q.get_x() * step_x, h - P.get_y() * step_y);
        played_path.lineTo(Q.get_x() * step_x, h - Q.get_y() * step_y);
        P = Q;
      }
      if (P.get_x() != w) {
        played_path.lineTo(w, h - P.get_y() * step_y);
        played_path.lineTo(w, 0);
      }
      played_path.close();
      canvas.drawPath(played_path, played_paint);
    }

    if (highlighting) {
      highlight_paint.setColor(highlight_color);
      highlight_paint.setStyle(FILL);
      canvas.drawRect(
          highlight_x * step_x, h - (highlight_y + 1) * step_y,
          (highlight_x + 1) * step_x, h - highlight_y * step_y,
          highlight_paint
      );
    }

    if (hinting) {
      hint_paint.setColor(hint_color);
      hint_paint.setStyle(FILL);
      int x = hint_position.get_x(), y = hint_position.get_y();
      canvas.drawRect(
          x * step_x, h - (y + 1) * step_y,
          (x + 1) * step_x, h - y * step_y,
          hint_paint
      );
    }

    for (int i = 0; i < view_xmax; ++i)
      canvas.drawLine(i * step_x, 0, i * step_x, h, line_paint);
    for (int i = 0; i < view_xmax; ++i)
      canvas.drawLine(0, h - i * step_y, w, h - i * step_y, line_paint);
  }

  public void get_computer_move() {

    boolean stupid_turn = computer_sometimes_dumb && game_control.random.nextBoolean();

    if (hint_position == null)
      evaluator.choose_computer_move();
    else{
      int i, j;
      if (hint_position == ORIGIN || stupid_turn) {
        do {
          i = game_control.random.nextInt(view_xmax);
          j = game_control.random.nextInt(view_ymax);
        } while (!playable.contains(i, j) || played.contains(i, j));
      } else {
        i = hint_position.get_x();
        j = hint_position.get_y();
      }
      evaluator.play_point(i, j);
      played.add_generator(i, j, true);
    }

    game_control.set_player_kind(HUMAN);
    evaluator.computer_move = false;
    invalidate();

  }

  /**
   * Called when a touch event is dispatched to a view. This allows listeners to
   * get a chance to respond before the target view.
   *
   * @param v     The view the touch event has been dispatched to.
   * @param event The MotionEvent object containing full information about
   *              the event.
   * @return True if the listener has consumed the event, false otherwise.
   */
  @Override
  public boolean onTouch(View v, MotionEvent event) {
    if (game_control.get_player_kind() == HUMAN) {
      float x = event.getX(), y = event.getY();
      float w = getWidth(), h = getHeight();
      if (x < 0 || y < 0 || x > w || y > h)
        highlighting = false;
      else {
        int i = (int) (x / step_x);
        int j = (int) ((h - y) / step_y);
        switch (event.getAction()) {
          case ACTION_UP:
            highlighting = hinting = false;
            // check for valid position
            if (playable.contains(i, j) && !played.contains(i, j)) {
              // add generator
              played.add_generator(i, j, true);
              evaluator.play_point(i, j);
            }
            if (evaluator.base_count != 0) {
              game_control.set_player_kind(COMPUTER);
              evaluator.choose_computer_move();
            }
            break;
          case ACTION_DOWN:
            highlighting = true;
            highlight_x = i;
            highlight_y = j;
            if (playable.contains(i, j) && !played.contains(i, j)) highlight_color = YELLOW;
            else highlight_color = BLACK;
            break;
          case ACTION_MOVE:
            highlight_x = i;
            highlight_y = j;
            if (playable.contains(i, j) && !played.contains(i, j)) highlight_color = YELLOW;
            else highlight_color = BLACK;
        }
        invalidate();
      }
    }
    return true;
  }

  /**
   * Called when a view has been clicked.
   *
   * @param v The view that was clicked.
   */
  @Override
  public void onClick(View v) {

    if (v == new_game_button) {
      game_control.new_game(this, view_xmax, view_ymax, game_level);
    } else if (v == hint_button) {
      value_text.setText(String.valueOf(evaluator.game_value()));
      hint_position = evaluator.hint_position();
      hinting = true;
      invalidate();
    }

  }

  public void set_buttons_to_listen(
      Button ng_button, TextView vt_view, Button h_button
  ) {

    new_game_button = ng_button;
    new_game_button.setOnClickListener(this);
    value_text = vt_view;
    hint_button = h_button;
    hint_button.setOnClickListener(this);
    if (game_level % 2 == 0) {
      hint_button.setVisibility(INVISIBLE);
      value_text.setVisibility(INVISIBLE);
    } else {
      hint_button.setVisibility(VISIBLE);
      value_text.setVisibility(VISIBLE);
    }

  }

  /**
   * Called when a shared preference is changed, added, or removed. This
   * may be called even if a preference is set to its existing value.
   * <p>
   * <p>This callback will be run on your main thread.
   *
   * @param pref The {@link SharedPreferences} that received
   *                          the change.
   * @param key               The key of the preference that was changed, added, or
   */
  @Override
  public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
    
    Context context = getContext();
    
    if (key.equals(context.getString(R.string.level_pref))) {
      game_level = pref.getInt(context.getString(R.string.level_pref), 1);
      game_level = (game_level < 1) ? 1 : game_level;
      if (game_level % 2 == 0) {
        hint_button.setVisibility(INVISIBLE);
        value_text.setVisibility(INVISIBLE);
      } else {
        hint_button.setVisibility(VISIBLE);
        value_text.setVisibility(VISIBLE);
      }
      game_control.new_game(this, view_xmax, view_ymax, game_level);
    } else if (key.equals(context.getString(R.string.max_pref_key))) {
      int max = pref.getInt(context.getString(R.string.max_pref_key), 7);
      max = (max < 7) ? 7 : max;
      if (max != view_xmax) {
        view_xmax = view_ymax = max;
        step_x = getWidth() / view_xmax;
        step_y = getHeight() / view_ymax;
        evaluator = new Game_Evaluation(context, playable, played, view_xmax, view_ymax, game_level);
        invalidate();
      }
    } else if (key.equals(context.getString(R.string.stupid_pref_key)))
      computer_sometimes_dumb = pref.getBoolean(context.getString(R.string.stupid_pref_key), true);
    else if (key.equals(context.getString(R.string.bg_color_key)))
      background_color = pref.getInt(context.getString(R.string.bg_color_key), background_color);
    else if (key.equals(context.getString(R.string.playable_color_key)))
      playable_color = pref.getInt(context.getString(R.string.playable_color_key), playable_color);
    else if (key.equals(context.getString(R.string.played_color_key)))
      played_color = pref.getInt(context.getString(R.string.played_color_key), played_color);
    else if (key.equals(context.getString(R.string.highlight_color_key)))
      highlight_color = pref.getInt(context.getString(R.string.highlight_color_key), highlight_color);
    else if (key.equals(context.getString(R.string.hint_color_key)))
      hint_color = pref.getInt(context.getString(R.string.hint_color_key), hint_color);
    else if (key.equals(context.getString(R.string.invalid_color_key)))
      invalid_color = pref.getInt(context.getString(R.string.invalid_color_key), invalid_color);
    bg_paint.setShader(
        new LinearGradient(
            0, getHeight(), getWidth(), 0, background_color, disappear_color, Shader.TileMode.CLAMP
        )
    );
    played_paint.setShader(
        new LinearGradient(
            0, getHeight(), getWidth(), 0, played_color, disappear_color, Shader.TileMode.CLAMP
        )
    );
    SharedPreferences.Editor editor = pref.edit();
    editor.putBoolean(context.getString(R.string.stupid_pref_key), computer_sometimes_dumb);
    editor.putInt(context.getString(R.string.level_pref), game_level);
    editor.putInt(context.getString(R.string.max_pref_key), view_xmax);
    editor.putInt(context.getString(R.string.bg_color_key), background_color);
    editor.putInt(context.getString(R.string.playable_color_key), playable_color);
    editor.putInt(context.getString(R.string.played_color_key), played_color);
    editor.putInt(context.getString(R.string.highlight_color_key), highlight_color);
    editor.putInt(context.getString(R.string.hint_color_key), hint_color);
    editor.putInt(context.getString(R.string.invalid_color_key), invalid_color);
    editor.apply();
  }

  protected int view_xmax = 7, view_ymax = 7;
  protected float step_x, step_y;
  protected int highlight_x, highlight_y;
  protected boolean highlighting = false, hinting = false;
  protected int playable_color = BLUE, played_color = Color.rgb(0xff, 0x80, 0x00),
      disappear_color = GRAY, invalid_color = BLACK,
      background_color = RED, highlight_color = YELLOW, hint_color = Color.rgb(0xff, 0x80, 0x00);
  protected Paint highlight_paint = new Paint(), hint_paint = new Paint(),
      played_paint = new Paint(), playable_paint = new Paint(),
      bg_paint = new Paint(), line_paint = new Paint(), invalid_paint = new Paint();
  protected Path playable_path = new Path(), played_path = new Path();

  protected Ideal playable, played;

  protected Position hint_position;

  protected int game_level = 4;
  protected Game_Control game_control;
  protected boolean computer_sometimes_dumb = false;

  protected Game_Evaluation evaluator;

  protected Button new_game_button, hint_button;
  protected TextView value_text;

  final protected static String tag = "Playfield";

}
