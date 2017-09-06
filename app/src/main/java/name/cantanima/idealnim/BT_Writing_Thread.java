package name.cantanima.idealnim;

import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cantanima on 9/5/17.
 */

public class BT_Writing_Thread extends AsyncTask<Byte [], Integer, Boolean> {

  public BT_Writing_Thread(Context main, BluetoothSocket socket) {
    context = main;
    bt_socket = socket;
  }

  @Override
  protected Boolean doInBackground(Byte[] ...params) {
    try {
      bt_output_stream = bt_socket.getOutputStream();
      int n = params[0][0];
      byte [] info = new byte [2*n + 1];
      info[0] = (byte) n;
      for (int i = 0; i < 2*n; ++i)
        info[i + 1] = params[0][i + 1];
      bt_output_stream.write(info);
      success = true;
      failure_message = "";
    } catch (IOException e) {
      success = false;
      failure_message = e.getMessage();
    }
    return success;
  }

  @Override
  public void onPostExecute(Boolean success) {
    if (!success) {
      String message = context.getString(R.string.bt_failed_to_write) + " " + failure_message;
      new AlertDialog.Builder(context).setTitle(context.getString(R.string.no_bluetooth_title))
          .setMessage(message)
          .setPositiveButton(context.getString(R.string.understood), null)
          .show();
    }
  }

  OutputStream bt_output_stream;
  BluetoothSocket bt_socket;
  Context context;
  boolean success;
  String failure_message;

}

