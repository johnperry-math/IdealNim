package name.cantanima.idealnim;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.google.android.gms.common.ConnectionResult.SUCCESS;
import static com.google.android.gms.common.GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE;
import static com.google.android.gms.games.GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED;

import com.google.android.gms.games.achievement.Achievement;
import com.google.example.games.basegameutils.BaseGameUtils;

public class MainActivity
    extends AppCompatActivity
    implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, DialogInterface.OnClickListener
{

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Button new_game_button = (Button) findViewById(R.id.new_game_button);
    TextView value_textview = (TextView) findViewById(R.id.value_view);
    Button hint_button = (Button) findViewById(R.id.hint_button);
    Playfield playfield = (Playfield) findViewById(R.id.playfield);
    playfield.set_buttons_to_listen(
        new_game_button, value_textview, hint_button
    );

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
      if (games_client != null && games_client.isConnected())
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
    }

    return super.onOptionsItemSelected(item);
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
      if (!BaseGameUtils.resolveConnectionFailure(
          this, games_client, connectionResult, GAME_SIGN_IN_CODE, R.string.unknown_signin_error
          )) {
        resolving_failure = false;
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
    /* don't really need to do anything here */
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
    sign_in_message_view.setText(p.getDisplayName());
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.d(tag, "connection suspended, trying to reconnect");
    if (games_client != null) games_client.connect();
  }

  public void unlock_achievement(Achievements_to_unlock achievement) {

    if (games_client != null) {

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
        case WON_LEVEL_3:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_won_a_level_3_game));
          break;
        case WON_LEVEL_4:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_won_a_game_at_level_4));
          break;
        case WON_LEVEL_5:
          Games.Achievements.unlock(games_client, getString(R.string.achievement_won_a_game_at_level_5));
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
      }

    }

  }

  public void increment_achievement(Achievements_to_unlock achievement) {

    if (games_client != null) {

      switch (achievement) {
        case JOURNEYMAN:
          Games.Achievements.increment(games_client, getString(R.string.achievement_journeyman), 1);
          break;
        case CRAFTSMAN:
          Games.Achievements.increment(games_client, getString(R.string.achievement_craftsman), 1);
          break;
        case MASTER_CRAFTSMAN:
          Games.Achievements.increment(games_client, getString(R.string.master_craftsman), 1);
          break;
      }

    }

  }

  private GoogleApiClient games_client = null;
  private boolean resolving_failure = false, auto_start_signin = false, sign_in_clicked = false;
  private SignInButton sign_in_button;
  private TextView sign_in_message_view;
  private Button sign_out_button;
  private int PLAY_SERVICES_RESOLUTION_REQUEST = 9200, REQUEST_ACHIEVEMENTS = 9300,
      GAME_SIGN_IN_CODE = 9400;
  public enum Achievements_to_unlock {
    EVERYONE_GETS_A_TROPHY,
    HONORABLE_MENTION,
    ONE_HAND_BEHIND_MY_BACK,
    WON_LEVEL_3,
    WON_LEVEL_4,
    WON_LEVEL_5,
    FAIR_PLAY,
    PATIENCE_A_VIRTUE,
    APPRENTICE,
    JOURNEYMAN,
    CRAFTSMAN,
    MASTER_CRAFTSMAN,
    DOCTOR_OF_IDEAL_NIM
  };

  private String tag = "Main activity";

}
