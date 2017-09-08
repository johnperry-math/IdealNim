package name.cantanima.idealnim;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;

import static android.bluetooth.BluetoothAdapter.STATE_ON;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.google.android.gms.common.ConnectionResult.SUCCESS;
import static com.google.android.gms.common.GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE;
import static com.google.android.gms.games.GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED;
import static name.cantanima.idealnim.Game_Control.Player_Kind.COMPUTER;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

public class MainActivity
    extends AppCompatActivity
    implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, DialogInterface.OnClickListener,
        BTR_Listener, DialogInterface.OnCancelListener
{

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Button new_game_button = (Button) findViewById(R.id.new_game_button);
    SeekBar view_seekbar = (SeekBar) findViewById(R.id.view_scale);
    TextView view_label = (TextView) findViewById(R.id.view_label);
    TextView value_textview = (TextView) findViewById(R.id.value_view);
    TextView value_label = (TextView) findViewById(R.id.value_label);
    Button hint_button = (Button) findViewById(R.id.hint_button);
    Playfield playfield = (Playfield) findViewById(R.id.playfield);
    playfield.set_buttons_to_listen(
        new_game_button, value_textview, value_label, hint_button, view_seekbar, view_label
    );
    playfield.start_game(COMPUTER);

    sign_in_button = (SignInButton) findViewById(R.id.sign_in_button);
    sign_in_button.setOnClickListener(this);
    sign_in_message_view = (TextView) findViewById(R.id.sign_in_message);
    sign_out_button = (Button) findViewById(R.id.sign_out_button);
    sign_out_button.setVisibility(INVISIBLE);
    sign_out_button.setOnClickListener(this);

    //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    //setSupportActionBar(toolbar);

    GoogleApiAvailability api_avail = GoogleApiAvailability.getInstance();
    int availability = api_avail.isGooglePlayServicesAvailable(this);
    if (availability == SUCCESS) {
      Log.d(tag, "Play services available, version " + GOOGLE_PLAY_SERVICES_VERSION_CODE);
      games_client = new GoogleApiClient.Builder(this)
          .addConnectionCallbacks(this)
          .addOnConnectionFailedListener(this)
          .addApi(Games.API).addScope(Games.SCOPE_GAMES)
          .build();
    } else {
      Log.d(tag, "Play services NOT available " + api_avail.isGooglePlayServicesAvailable(this));
      if (api_avail.isUserResolvableError(availability))
        api_avail.getErrorDialog(this, availability, PLAY_SERVICES_RESOLUTION_REQUEST).show();
    }

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      Intent i = new Intent(this, SettingsActivity.class);
      startActivity(i);
      return true;
    } else if (id == R.id.action_information) {
      Intent i = new Intent(this, Information_Activity.class);
      startActivity(i);
      return true;
    } else if (id == R.id.action_achievements) {
      if (can_handle_achievements())
        startActivityForResult(
            Games.Achievements.getAchievementsIntent(games_client), REQUEST_ACHIEVEMENTS
        );
      else {
        AlertDialog.Builder build = new AlertDialog.Builder(this);
        build.setTitle(R.string.sign_in);
        build.setMessage(R.string.sign_in_why);
        build.setPositiveButton(R.string.understood, this);
        build.show();
      }
    } else if (id == R.id.action_twoplayer) {
      if (bluetooth_is_available()) {
        if (bt_adapter.isEnabled()) {
          if (bt_thread != null) {
            bt_thread.disconnect();
            bt_thread = null;
          }
          bt_thread = new BT_Setup_Thread(this);
          bt_thread.start();
          bt_thread.host_or_join();
        } else {
          Intent enable_bt_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          startActivityForResult(enable_bt_intent, REQUEST_ENABLE_BT);
        }
      }
    }

    return super.onOptionsItemSelected(item);
  }

  public boolean bluetooth_is_available() {
    bt_adapter = BluetoothAdapter.getDefaultAdapter();
    boolean result = bt_adapter != null;
    if (!result) {
      AlertDialog no_bt_dialog = new AlertDialog.Builder(this)
          .setTitle(R.string.no_bluetooth_title)
          .setMessage(
              getString(R.string.no_bluetooth_message) + " " +
                  getString(R.string.bluetooth_not_available)
          )
          .setPositiveButton(R.string.understood, this)
          .show();
    }
    return result;
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.d(tag, "Connection failed");
    Log.d(tag, connectionResult.toString() + " " + String.valueOf(connectionResult.getErrorCode()));
    if (resolving_failure)
      Log.d(tag, "already resolving");
    else if (sign_in_clicked || auto_start_signin) {
      auto_start_signin = sign_in_clicked = false;
      resolving_failure = true;
      if (connectionResult.hasResolution()) {
        try {
          connectionResult.startResolutionForResult(this, GAME_SIGN_IN_CODE);
        } catch (Exception e) {
          games_client.connect();
          resolving_failure = false;
        }
      } else {
        AlertDialog cancel_dialog = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.conn_fail_title))
            .setMessage(getString(R.string.conn_fail_summ))
            .setPositiveButton(getString(R.string.conn_fail_pos), this)
            .show();
        games_client.disconnect();

      }
    }
  }

  /**
   * Dispatch incoming result to the correct fragment.
   *
   * @param requestCode
   * @param resultCode
   * @param data
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Log.d(tag, "In activity result with code " + String.valueOf(resultCode));
    if (requestCode == GAME_SIGN_IN_CODE || requestCode == PLAY_SERVICES_RESOLUTION_REQUEST) {
      Log.d(tag, "activity result requests sign in");
      sign_in_clicked = false;
      resolving_failure = false;
      if (resultCode == RESULT_OK) {
        Log.d(tag, "activity result OK, trying to connect");
        games_client.connect();
      } else if (resultCode == RESULT_RECONNECT_REQUIRED) {
        Log.d(tag, "reconnect required, retrying");
        games_client.connect();
      } else if (resultCode == RESULT_CANCELED){
        Log.d(tag, "canceled, disconnecting");
        AlertDialog cancel_dialog = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.conn_fail_title))
            .setMessage(getString(R.string.conn_fail_summ))
            .setPositiveButton(getString(R.string.conn_fail_pos), this)
            .show();
        games_client.disconnect();
      } else {
        Log.d(tag, "unsolved resolution");
      }
    } else if (requestCode == REQUEST_ENABLE_BT) {
      Log.d(tag, "bluetooth requests enable");
      if (resultCode == RESULT_OK) {
        Log.d(tag, "bluetooth enable OK");
        if (bt_thread == null)
          bt_thread = new BT_Setup_Thread(this);
        bt_thread.host_or_join();
      } else if (resultCode == RESULT_CANCELED) {
        Log.d(tag, "canceled, aborting");
        new AlertDialog.Builder(this)
            .setTitle(R.string.no_bluetooth_title)
            .setMessage(getString(R.string.no_bluetooth_message) + " " +
                getString(R.string.bluetooth_canceled)
            )
            .setPositiveButton(R.string.understood, this)
            .show();
      }
    } else if (requestCode == REQUEST_HOST) {
      Log.d(tag, "bluetooth requests host");
      if (resultCode == RESULT_OK) {
        Log.d(tag, "bluetooth host ok");
        (new Host_Thread(this)).execute();
      } else {
        Log.d(tag, "canceled hosting");
        new AlertDialog.Builder(this)
            .setTitle(R.string.no_bluetooth_title)
            .setMessage(getString(R.string.no_bluetooth_message) + " "
                + getString(R.string.bt_no_discoverability)
            )
            .setPositiveButton(R.string.understood, this)
            .show();
      }
    }
  }

  /**
   * Called when a view has been clicked.
   *
   * @param v The view that was clicked.
   */
  @Override
  public void onClick(View v) {
    if (v == findViewById(R.id.sign_in_button)) {
      Log.d(tag, "Logging into package" + getPackageName());
      sign_in_clicked = true;
      if (games_client != null) games_client.connect();
      if (games_client.isConnected()) onConnected(null);
    } else if (v == findViewById(R.id.sign_out_button)) {
      Log.d(tag, "Logging out of package");
      sign_in_clicked = false;
      if (games_client != null) Games.signOut(games_client);
      sign_out_button.setVisibility(INVISIBLE);
      sign_in_button.setVisibility(VISIBLE);
      sign_in_message_view.setText(getString(R.string.sign_in_why));
    }
  }

  /**
   * This method will be invoked when a button in the dialog is clicked.
   *
   * @param dialog The dialog that received the click.
   * @param which  The button that was clicked (e.g.
   *               {@link DialogInterface#BUTTON1}) or the position
   */
  @Override
  public void onClick(DialogInterface dialog, int which) {

    if (dialog == bluetooth_dialog) {
      if (which == DialogInterface.BUTTON_NEGATIVE) {
        // abort
      } else {
        // connect to device
        bt_thread.join_game(which);
      }
    } else if (dialog == host_or_join_dialog) {
      two_player_game = true;
      if (which == DialogInterface.BUTTON_POSITIVE) {
        (new Host_Thread(this)).execute();
      } else {
        bt_thread.select_paired_device();
      }
    }

  }

  public void start_human_game() {
    ((Playfield) findViewById(R.id.playfield)).setup_human_game(communication_socket, true);
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (games_client != null) games_client.connect();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (games_client != null) games_client.disconnect();
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    resolving_failure = false;
    Log.d(tag, "connected!");
    sign_out_button.setVisibility(VISIBLE);
    sign_in_button.setVisibility(INVISIBLE);
    Player p = Games.Players.getCurrentPlayer(games_client);
    sign_in_message_view.setText(getString(R.string.signed_in_as) + " " + p.getName());
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.d(tag, "connection suspended, trying to reconnect");
    if (games_client != null) games_client.connect();
  }

  public boolean can_handle_achievements() {
    return games_client != null && games_client.isConnected() &&
        Games.Players.getCurrentPlayer(games_client) != null;
  }

  public void unlock_achievement(Achievements_to_unlock achievement) {

    if (
        games_client != null && games_client.isConnected() &&
        Games.Players.getCurrentPlayer(games_client) != null
    ) {

      switch (achievement) {
        case EVERYONE_GETS_A_TROPHY:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_everyone_gets_a_trophy));
          Games.Achievements.reveal(games_client, getString(R.string.achievement_everyone_gets_a_trophy));
          break;
        case HONORABLE_MENTION:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_honorable_mention));
          break;
        case ONE_HAND_BEHIND_MY_BACK:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_0ne_hand_behind_my_back));
          break;
        case WON_LEVEL_5:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_won_a_game_at_level_5));
          break;
        case WON_LEVEL_7:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_won_a_game_at_level_7));
          break;
        case WON_LEVEL_9:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_won_a_game_at_level_9));
          break;
        case FAIR_PLAY:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_fair_play_award));
          break;
        case PATIENCE_A_VIRTUE:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_patience_is_a_virtue));
          break;
        case APPRENTICE:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_apprentice));
          break;
        case DOCTOR_OF_IDEAL_NIM:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_doctor_of_ideal_nim));
          break;
        case ONLY_HUMAN:
          Games.Achievements.unlock(
              games_client, getString(R.string.achievement_theyre_only_human)
          );
          break;
        case LEGEND_AMONG_KIND:
          Games.Achievements.unlock(
              games_client, getString(R.string.achievement_a_legend_among_your_kind_but_not_mine)
          );
          break;
      }

    }

  }

  public void increment_achievement(Achievements_to_unlock achievement) {

    if (
        games_client != null &&
            games_client.isConnected() &&
            Games.Players.getCurrentPlayer(games_client) != null
    ) {

      switch (achievement) {
        case JOURNEYMAN:
          Games.Achievements.increment(games_client, getString(R.string.achievement_journeyman), 1);
          break;
        case CRAFTSMAN:
          Games.Achievements.increment(games_client, getString(R.string.achievement_craftsman), 1);
          break;
        case MASTER_CRAFTSMAN:
          Games.Achievements.increment(
              games_client, getString(R.string.achievement_master_craftsman), 1
          );
          break;
        case GOOD_SPORT:
          Games.Achievements.increment(games_client, getString(R.string.achievement_good_sport), 1);
          break;
        case PENTAWIN:
          Games.Achievements.increment(games_client, getString(R.string.achievement_pentawin), 1);
          break;
      }

    }

  }

  public boolean i_host_the_game() { return i_am_hosting; }

  @Override
  public void received_data(int size, byte[] data) {
    if (data[0] == 1 && data[1] == 0 && data[2] == 0)
      try {
        communication_socket.close();
      } catch (IOException e) {
        // I don't care at this point
      }
  }

  /**
   * This method will be invoked when the dialog is canceled.
   *
   * @param dialog The dialog that was canceled will be passed into the
   *               method.
   */
  @Override
  public void onCancel(DialogInterface dialog) {
    if (dialog == bt_waiting_to_host_dialog) {
      bt_waiting_to_host_dialog.dismiss();
      if (communication_socket != null) try {
        communication_socket.close();
        communication_socket = null;
      } catch (IOException e) {
        // I really don't understand the point of this
      }
    }
  }

  public class BT_Setup_Thread extends Thread {

    public BT_Setup_Thread(MainActivity my_context) { context = my_context; }

    public void disconnect() {
      try {
        communication_socket.close();
      } catch (Exception e) {
        // I don't care at this point
      }
    }

    public void join_game(int which_device) {

      i_am_hosting = false;
      BluetoothDevice desired_device = available_devices.get(which_device);
      BluetoothSocket socket = null;
      try {
        socket = desired_device.createRfcommSocketToServiceRecord(UUID.fromString(my_uuid));
        communication_socket = socket;
      } catch (IOException e) {
        String message = getString(R.string.bt_unable_to_join) + " " + e.getMessage();
        Log.d(tag, message, e);
        new AlertDialog.Builder(context).setTitle(R.string.no_bluetooth_title)
            .setMessage(message)
            .setPositiveButton(R.string.understood, context)
            .show();
      }

      if (communication_socket != null) {
        try {
          communication_socket.connect();
          if (communication_socket.isConnected())
            ((Playfield) findViewById(R.id.playfield)).setup_human_game(communication_socket, false);
          else {
            new AlertDialog.Builder(context).setTitle(R.string.no_bluetooth_title)
                .setMessage(R.string.bt_unable_to_open_stream)
                .setPositiveButton(R.string.understood, context)
                .show();
          }
        } catch (IOException e) {
          String message = getString(R.string.bt_unable_to_connect) + " " + e.getMessage();
          Log.d(tag, message, e);
          new AlertDialog.Builder(context).setTitle(R.string.no_bluetooth_title)
              .setMessage(message)
              .setPositiveButton(R.string.understood, context)
              .show();
        }
      }

    }

    public void host_or_join() {

      host_or_join_dialog = new AlertDialog.Builder(context)
          .setTitle(R.string.bt_host_or_join_title)
          .setMessage(R.string.bt_host_or_join_message)
          .setPositiveButton(R.string.host, context)
          .setNegativeButton(R.string.join, context)
          .show();

    }

    public void select_paired_device() {

      i_am_hosting = false;
      Set<BluetoothDevice> known_devices = bt_adapter.getBondedDevices();
      String [] device_names = new String[known_devices.size()];
      available_devices = new Vector<>(known_devices.size());
      int i = 0;
      for (BluetoothDevice device : known_devices) {
        device_names[i] = device.getName();
        available_devices.add(device);
        ++i;
      }
      bluetooth_dialog = new AlertDialog.Builder(context)
          .setTitle(R.string.bt_select_device_title)
          .setNegativeButton(R.string.cancel, context)
          .setItems(device_names, context)
          .show();
      if (available_devices.size() == 0)
        new AlertDialog.Builder(context)
            .setTitle(R.string.bt_no_devices_title)
            .setMessage(R.string.bt_no_devices_message)
            .setPositiveButton(R.string.understood, context)
            .show();

    }

    MainActivity context;

  }

  private class Host_Thread extends AsyncTask<Object, Integer, Boolean> {

    public Host_Thread(MainActivity main) { context = main; }

    /**
     * Runs on the UI thread before {@link #doInBackground}.
     *
     * @see #onPostExecute
     * @see #doInBackground
     */
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      bt_waiting_to_host_dialog = new ProgressDialog(context);
      bt_waiting_to_host_dialog.setTitle(R.string.bt_waiting_to_host_title);
      bt_waiting_to_host_dialog.setMessage(getString(R.string.bt_waiting_to_host_message));
      bt_waiting_to_host_dialog.setIndeterminate(true);
      bt_waiting_to_host_dialog.show();
    }

    /**
     * <p>Runs on the UI thread after {@link #doInBackground}. The
     * specified result is the value returned by {@link #doInBackground}.</p>
     * <p>
     * <p>This method won't be invoked if the task was cancelled.</p>
     *
     * @param aBoolean The result of the operation computed by {@link #doInBackground}.
     * @see #onPreExecute
     * @see #doInBackground
     * @see #onCancelled(Object)
     */
    @Override
    protected void onPostExecute(Boolean aBoolean) {
      super.onPostExecute(aBoolean);
      bt_waiting_to_host_dialog.dismiss();
      try {
        server_socket.close();
      } catch (IOException e) {
        // don't care now
      }
      Log.d(tag, "disconnected server socket");
      if (communication_socket.isConnected())
        context.start_human_game();
    }

    @Override
    protected Boolean doInBackground(Object[] params) {

      i_am_hosting = true;
      host_or_join_dialog.dismiss();
      BluetoothServerSocket server = null;
      try {
        server = bt_adapter.listenUsingInsecureRfcommWithServiceRecord(
            "Ideal Nim", UUID.fromString(my_uuid)
        );
        server_socket = server;
      } catch (IOException e) {
        String message = getString(R.string.bt_unable_to_host) + " " + e.getMessage();
        Log.d(tag, message, e);
        new AlertDialog.Builder(context).setTitle(R.string.no_bluetooth_title)
            .setMessage(message)
            .setPositiveButton(R.string.understood, context)
            .show();
      }

      if (server_socket != null) {
        BluetoothSocket socket = null;
        try {
          socket = server_socket.accept();
          communication_socket = socket;
        } catch (IOException e) {
          String message = getString(R.string.bt_unable_to_host) + " " + e.getMessage();
          Log.d(tag, message, e);
          new AlertDialog.Builder(context).setTitle(R.string.no_bluetooth_title)
              .setMessage(message)
              .setPositiveButton(R.string.understood, context)
              .show();
        }
        if (!communication_socket.isConnected()) {
          new AlertDialog.Builder(context).setTitle(R.string.no_bluetooth_title)
              .setMessage(R.string.bt_unable_to_open_stream)
              .setPositiveButton(R.string.understood, context)
              .show();
        }
      }

      return null;
    }

    private MainActivity context;
  }

  public boolean is_two_player_game() { return two_player_game; }

  public void two_player_game_ended() {
    two_player_game = false;
    if (communication_socket != null && communication_socket.isConnected()) {
      if (i_host_the_game()) {
        BT_Reading_Thread bt_thread = new BT_Reading_Thread(this, communication_socket, this, false);
        bt_thread.execute();
      } else {
        BT_Writing_Thread bt_thread = new BT_Writing_Thread(this, communication_socket);
        Byte[] ack = new Byte[3];
        ack[0] = 1; ack[1] = 0; ack[2] = 0;
        bt_thread.execute(ack);
      }
      Log.d(tag, "closing socket");
    }
  }

  private GoogleApiClient games_client = null;
  private boolean resolving_failure = false, auto_start_signin = false, sign_in_clicked = false;
  private SignInButton sign_in_button;
  private TextView sign_in_message_view;
  private Button sign_out_button;

  // games services
  private int PLAY_SERVICES_RESOLUTION_REQUEST = 9200, REQUEST_ACHIEVEMENTS = 9300,
      GAME_SIGN_IN_CODE = 9400;
  enum Achievements_to_unlock {
    EVERYONE_GETS_A_TROPHY,
    HONORABLE_MENTION,
    ONE_HAND_BEHIND_MY_BACK,
    WON_LEVEL_5,
    WON_LEVEL_7,
    WON_LEVEL_9,
    FAIR_PLAY,
    PATIENCE_A_VIRTUE,
    CHANGED_BOARD_SIZE,
    APPRENTICE,
    JOURNEYMAN,
    CRAFTSMAN,
    MASTER_CRAFTSMAN,
    DOCTOR_OF_IDEAL_NIM,
    GOOD_SPORT,
    ONLY_HUMAN,
    PENTAWIN,
    LEGEND_AMONG_KIND
  };

  // bluetooth services
  private final String my_uuid = "baf23ef1-04db-4b6c-bf4f-09dbcfc0591f";
  private int REQUEST_ENABLE_BT = 32003, REQUEST_HOST = 32004;
  private Vector<BluetoothDevice> available_devices = null;
  private AlertDialog bluetooth_dialog = null, host_or_join_dialog = null;
  private BluetoothServerSocket server_socket = null;
  private BluetoothSocket communication_socket = null;
  private BT_Setup_Thread bt_thread = null;
  protected boolean i_am_hosting, two_player_game = false;
  ProgressDialog bt_waiting_to_host_dialog = null;

  private String tag = "Main activity";

  private BluetoothAdapter bt_adapter = null;

}
