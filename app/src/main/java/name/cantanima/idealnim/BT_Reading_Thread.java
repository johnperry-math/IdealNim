package name.cantanima.idealnim;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by cantanima on 9/5/17.
 */

public class BT_Reading_Thread extends AsyncTask<Object, Integer, Boolean> {

  public BT_Reading_Thread(
      Context main, BluetoothSocket socket, BTR_Listener listener, boolean show_dialog
  ) {
    context = main;
    bt_socket = socket;
    notify = listener;
    show_progress_dialog = show_dialog;
  }

  @Override
  protected Boolean doInBackground(Object ...params) {
    try {
      bt_input_stream = bt_socket.getInputStream();
      size = bt_input_stream.read(info);
      success = true;
      failure_message = "";
    } catch (IOException e) {
      success = false;
      failure_message = e.getMessage();
    }
    return success;
  }

  @Override
  public void onPreExecute() {
    if (show_progress_dialog) {
      progress_dialog = new ProgressDialog(context);
      progress_dialog.setTitle(context.getString(R.string.bt_progress_title));
      progress_dialog.setMessage(context.getString(R.string.bt_progress_message));
      progress_dialog.setIndeterminate(true);
      progress_dialog.setCancelable(false);
      progress_dialog.show();
    }
  }

  @Override
  public void onPostExecute(Boolean success) {
    if (show_progress_dialog)
      progress_dialog.dismiss();
    if (success)
      notify.received_data(size, info);
    else {
      String message = context.getString(R.string.bt_failed_to_write) + " " + failure_message;
      new AlertDialog.Builder(context).setTitle(context.getString(R.string.no_bluetooth_title))
          .setMessage(message)
          .setPositiveButton(context.getString(R.string.understood), null)
          .show();
    }
  }

  ProgressDialog progress_dialog;
  InputStream bt_input_stream;
  BluetoothSocket bt_socket;
  BTR_Listener notify;
  Context context;
  boolean success, show_progress_dialog = false;
  final byte [] info = new byte[21];
  int size;
  String failure_message;

}


