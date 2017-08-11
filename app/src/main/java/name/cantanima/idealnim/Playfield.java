package name.cantanima.idealnim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
import static android.graphics.Color.GRAY;
import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.graphics.Color.YELLOW;
import static android.graphics.Paint.Style.FILL;
import static android.graphics.Paint.Style.STROKE;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Created by cantanima on 8/8/17.
 */

public class Playfield extends View implements OnTouchListener, OnClickListener {

  public Playfield(Context context, AttributeSet attrs) {

    super(context, attrs);
    playable = new Ideal();
    gone = new Ideal();
    if (isInEditMode()) {
      gone.add_generator_fast(3, 8);
      gone.add_generator_fast(8, 2);
      gone.add_generator_fast(2, 9);
      gone.sort_ideal();
      playable.add_generator_fast(0, 5);
      playable.add_generator_fast(3, 2);
      playable.add_generator_fast(5, 1);
      playable.sort_ideal();
    }

    game_control = new Game_Control();
    game_control.new_game(this);
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

  }

  /**
   * Sets the playable ideal to {@code F} and clears the played ideal.
   * The Ideal {@code F} is copied.
   * @param F ideal of playable positions
   */
  public void set_to(Ideal F) {

    playable = new Ideal(F);
    gone = new Ideal();
    hinting = false;
    if (value_text != null)
      value_text.setText(getContext().getString(R.string.unknown_game_value));

    evaluator = null;
    playfield = new boolean[view_xmax][view_ymax];
    playfield_count = 0;
    for (int i = 0; i < view_xmax; ++i)
      for (int j = 0; j < view_ymax; ++j)
        playfield[i][j] = false;
    for (Position t: playable.T) {
      for (int i = t.get_x(); i < view_xmax; ++i)
        for (int j = t.get_y(); j < view_ymax; ++j)
          if (!playfield[i][j]) {
            playfield[i][j] = true;
            ++playfield_count;
          }
    }
    playfield_max_x = view_xmax;
    playfield_max_y = view_ymax;

  }

  public void set_view_xmax(int x) { view_xmax = x; }

  public void set_view_ymax(int y) { view_ymax = y; }

  public void set_background_color(int color) { background_color = color; }

  public void set_ideal_color(int color) { ideal_color = color; }

  public void set_coidealColor(int color) { coideal_color = color; }

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
    ideal_paint.setColor(ideal_color);
    ideal_paint.setStyle(FILL);
    coideal_paint.setColor(coideal_color);
    coideal_paint.setStyle(FILL);

    float h = getHeight(), w = getWidth();
    canvas.drawRect(0, 0, w, h, bg_paint);

    if (playable.T.size() != 0) {
      coideal_path.rewind();
      coideal_path.moveTo(0, h);
      Iterator<Position> Ti = playable.iterator();
      Position P = Ti.next();
      if (P.get_x() == 0)
        coideal_path.lineTo(0, h - P.get_y() * step_y);
      else {
        coideal_path.lineTo(0, 0);
        coideal_path.lineTo(P.get_x() * step_x, 0);
        coideal_path.lineTo(P.get_x() * step_x, h - P.get_y() * step_y);
      }
      while (Ti.hasNext()) {
        Position Q = Ti.next();
        coideal_path.lineTo(Q.get_x() * step_x, h - P.get_y() * step_y);
        coideal_path.lineTo(Q.get_x() * step_x, h - Q.get_y() * step_y);
        P = Q;
      }
      if (P.get_y() != 0) {
        coideal_path.lineTo(w, h - P.get_y() * step_y);
        coideal_path.lineTo(w, h);
      }
      coideal_path.close();
      canvas.drawPath(coideal_path, coideal_paint);
    }

    if (gone.T.size() != 0) {
      ideal_path.rewind();
      ideal_path.moveTo(w, 0);
      Iterator<Position> Ti = gone.iterator();
      Position P = Ti.next();
      ideal_path.lineTo(P.get_x() * step_x, 0);
      if (P.get_y() < view_ymax)
        ideal_path.lineTo(P.get_x() * step_x, h - P.get_y() * step_y);
      while (Ti.hasNext()) {
        Position Q = Ti.next();
        if (Q.get_y() < view_ymax) {
          ideal_path.lineTo(Q.get_x() * step_x, h - P.get_y() * step_y);
          ideal_path.lineTo(Q.get_x() * step_x, h - Q.get_y() * step_y);
        }
        P = Q;
      }
      if (P.get_x() < view_xmax)
        ideal_path.lineTo(w, h - P.get_y() * step_y);
      ideal_path.close();
      canvas.drawPath(ideal_path, ideal_paint);
    }

    if (highlighting) {
      highlight_paint.setColor(highlight_color);
      highlight_paint.setStyle(FILL);
      canvas.drawRect(
          highlight_x * step_x, h - (highlight_y + 1)* step_y,
          (highlight_x + 1) * step_x, h - highlight_y * step_y,
          highlight_paint
      );
    }

    if (hinting) {
      hint_paint.setColor(hint_color);
      hint_paint.setStyle(FILL);
      int x = hint_position.get_x(), y = hint_position.get_y();
      canvas.drawRect(
          x * step_x, h - (y + 1)* step_y,
          (x + 1) * step_x, h - y * step_y,
          hint_paint
      );
    }

    for (int i = 0; i < view_xmax; ++i)
      canvas.drawLine(i * step_x, 0, i * step_x, h, line_paint);
    for (int i = 0; i < view_xmax; ++i)
      canvas.drawLine(0, h - i * step_y, w, h - i * step_y, line_paint);
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
    float x = event.getX(), y = event.getY();
    float w = getWidth(), h = getHeight();
    if (x >= 0 && y >= 0 && x <= w && y <= h) {
      int i = (int) (x / step_x);
      int j = (int) ((h - y) / step_y);
      switch (event.getAction()) {
        case ACTION_UP:
          highlighting = hinting = false;
          // check for valid position
          if (playable.contains(i, j) && !gone.contains(i, j)) {
            // add generator
            gone.add_generator(i, j, true);
            // update playfield
            for (int k = i; k < playfield_max_x; ++k)
              for (int l = j; l < playfield_max_y; ++l)
                if (playfield[k][l]) {
                  playfield[k][l] = false;
                  --playfield_count;
                }
            // adjust playfield_max_y
            boolean checking_y = true;
            int old_playfield_max_y = playfield_max_y;
            while (playfield_max_y > 0 && checking_y) {
              boolean found_position = false;
              for (int k = 0; !found_position && k < playfield_max_x; ++k)
                found_position |= playfield[k][playfield_max_y - 1];
              if (found_position) checking_y = false;
              else --playfield_max_y;
            }
            // adjust playfield_max_x
            if (j == 0) playfield_max_x = i;
            boolean checking_x = true;
            while (playfield_max_x > 0 && checking_x) {
              boolean found_position = false;
              for (int l = 0; !found_position && l < old_playfield_max_y; ++l)
                found_position |= playfield[playfield_max_x - 1][l];
              if (found_position) checking_x = false;
              else --playfield_max_x;
            }
          }
          Log.d(tag,
              "count: " + String.valueOf(playfield_count) +
                  " max_x: " + String.valueOf(playfield_max_x) +
                  " max_y: " + String.valueOf(playfield_max_y)
          );
          break;
        case ACTION_DOWN:
          highlighting = true;
          highlight_x = i;
          highlight_y = j;
          if (playable.contains(i, j) && !gone.contains(i, j)) highlight_color = YELLOW;
          else highlight_color = BLACK;
          break;
        case ACTION_MOVE:
          if (highlighting) {
            highlight_x = i;
            highlight_y = j;
            if (playable.contains(i, j) && !gone.contains(i, j)) highlight_color = YELLOW;
            else highlight_color = BLACK;
          }
      }
      invalidate();
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
      game_control.new_game(this);
    } else if (v == evaluate_game_button) {
      waiter = null;
      if (evaluator == null) {
        /*waiter = new ProgressDialog(getContext());
        waiter.setTitle(getContext().getString(R.string.thinking));
        waiter.setMessage(getContext().getString(R.string.please_wait));
        waiter.setMax(playfield_count);
        waiter.setCancelable(false);
        waiter.show();*/
        evaluator
            = new Game_Evaluation(getContext(), playfield_count, playfield_max_x, playfield_max_y);
      }
      value_text.setText(String.valueOf(
          evaluator.game_value(playfield, playfield_count, playfield_max_x, playfield_max_y)
      ));
      if (waiter != null) waiter.dismiss();

    } else if (v == hint_button) {
      if (evaluator == null)
        evaluator
            = new Game_Evaluation(getContext(), playfield_count, playfield_max_x, playfield_max_y);
      evaluator.game_value(playfield, playfield_count, playfield_max_x, playfield_max_y);
      hint_position = evaluator.hint_position();
      hinting = true;
      invalidate();
    }

  }

  public void set_buttons_to_listen(
      Button ng_button, Button eg_button, TextView vt_view, Button h_button
  ) {

    new_game_button = ng_button;
    new_game_button.setOnClickListener(this);
    evaluate_game_button = eg_button;
    evaluate_game_button.setOnClickListener(this);
    value_text = vt_view;
    hint_button = h_button;
    hint_button.setOnClickListener(this);

  }

  protected int view_xmax = 10, view_ymax = 10;
  protected float step_x, step_y;
  protected int highlight_x, highlight_y;
  protected boolean highlighting = false, hinting = false;
  protected int background_color = GREEN, ideal_color = GRAY,
      coideal_color = RED, highlight_color = YELLOW, hint_color = Color.rgb(0xff, 0x80, 0x00);
  protected Paint highlight_paint = new Paint(), hint_paint = new Paint(),
      ideal_paint = new Paint(), coideal_paint = new Paint(),
      bg_paint = new Paint(), line_paint = new Paint();
  protected Path ideal_path = new Path(), coideal_path = new Path();

  protected Ideal playable, gone;
  protected boolean[][] playfield;
  protected int playfield_count, playfield_max_x, playfield_max_y;

  protected Position hint_position;

  protected Game_Control game_control;

  protected Game_Evaluation evaluator;
  protected ProgressDialog waiter;

  protected Button new_game_button, evaluate_game_button, hint_button;
  protected TextView value_text;

  final protected static String tag = "Playfield";

}
