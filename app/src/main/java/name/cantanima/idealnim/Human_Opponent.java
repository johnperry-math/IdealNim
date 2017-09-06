package name.cantanima.idealnim;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by cantanima on 9/2/17.
 */

public class Human_Opponent extends Opponent implements BTR_Listener {

  public Human_Opponent(Context context, Ideal I, Ideal J, int level) {

    super(context, I, J, level);
    if (J == null) base_played = new Ideal();

  }

  public void acquired_human_opponent(@NonNull BluetoothSocket socket) { bt_socket = socket; }

  @Override
  public void choose_a_position() {
    BT_Reading_Thread bt_reader = new BT_Reading_Thread(
        overall_context, bt_socket, this, true
    );
    bt_reader.execute();
  }

  @Override
  public void update_with_position(int i, int j) {
    bt_raw_data[0] = (byte) 1;
    bt_raw_data[1] = (byte) i;
    bt_raw_data[2] = (byte) j;
    BT_Writing_Thread bt_writer = new BT_Writing_Thread(overall_context, bt_socket);
    bt_writer.execute(bt_raw_data);
    super.update_with_position(i, j);
  }

  @Override
  public void received_data(int size, byte [] raw_data) {

    Position P = new Position(raw_data[1], raw_data[2]);
    ((Playfield) overall_context.findViewById(R.id.playfield)).get_human_move(P);

  }

  protected BluetoothSocket bt_socket = null;
  protected final Byte [] bt_raw_data = new Byte[3];

  protected static final String tag = "Human_Opponent";

}
