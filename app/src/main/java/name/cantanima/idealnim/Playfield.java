package name.cantanima.idealnim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Iterator;
import java.util.LinkedList;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.GRAY;
import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.graphics.Color.WHITE;
import static android.graphics.Paint.Style.FILL;
import static android.graphics.Paint.Style.STROKE;

/**
 * Created by cantanima on 8/8/17.
 */

public class Playfield extends View implements View.OnTouchListener {

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
  }

  public void set_view_xmax(int x) { view_xmax = x; }

  public void set_view_ymax(int y) { view_ymax = y; }

  public void set_background_color(int color) { background_color = color; }

  public void set_ideal_color(int color) { ideal_color = color; }

  public void set_coidealColor(int color) { coideal_color = color; }

  /**
   * Implement this to do your drawing.
   *
   * @param canvas the canvas on which the background will be drawn
   */
  @Override
  protected void onDraw(Canvas canvas) {

    super.onDraw(canvas);

    Paint bg_paint = new Paint(), line_paint = new Paint(),
        ideal_paint = new Paint(), coideal_paint = new Paint();
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

    float step_x = w / view_xmax, step_y = h / view_ymax;

    if (playable.T.size() != 0) {
      Path coideal_path = new Path();
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
      Path ideal_path = new Path();
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
    return true;
  }

  protected int view_xmax = 10, view_ymax = 10;
  protected int background_color = GREEN, ideal_color = GRAY, coideal_color = RED;
  protected Ideal playable, gone;

  protected String tag = "Playfield";

}
