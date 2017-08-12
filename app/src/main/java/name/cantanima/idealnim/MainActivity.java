package name.cantanima.idealnim;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

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
    //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    //setSupportActionBar(toolbar);

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
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  AlertDialog waiter;

}
