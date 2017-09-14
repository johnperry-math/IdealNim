package name.cantanima.idealnim;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import yuku.ambilwarna.widget.AmbilWarnaPreference;

/**
 * Created by cantanima on 8/11/17.
 */

public class Settings_Fragment extends PreferenceFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener
{

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
  }

  /**
   * Called when the fragment is visible to the user and actively running.
   * This is generally
   * tied to {@link Activity#onResume() Activity.onResume} of the containing
   * Activity's lifecycle.
   */
  @Override
  public void onResume() {
    super.onResume();
    PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
  }

  /**
   * Called when the Fragment is no longer resumed.  This is generally
   * tied to {@link Activity#onPause() Activity.onPause} of the containing
   * Activity's lifecycle.
   */
  @Override
  public void onPause() {
    super.onPause();
    PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
  }

  /**
   * Called when a shared preference is changed, added, or removed. This
   * may be called even if a preference is set to its existing value.
   * <p>
   * <p>This callback will be run on your main thread.
   *
   * @param sharedPreferences The {@link SharedPreferences} that received
   *                          the change.
   * @param key               The key of the preference that was changed, added, or
   */
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (isAdded()) {
      if (key.equals(getString(R.string.bg_color_key))) {
        AmbilWarnaPreference pref = (AmbilWarnaPreference) findPreference(getString(R.string.bg_color_key));
        pref.forceSetValue(sharedPreferences.getInt(getString(R.string.bg_color_key), 0));
      } else if (key.equals(getString(R.string.playable_color_key))) {
        AmbilWarnaPreference pref = (AmbilWarnaPreference) findPreference(getString(R.string.playable_color_key));
        pref.forceSetValue(sharedPreferences.getInt(getString(R.string.playable_color_key), 0));
      } else if (key.equals(getString(R.string.played_color_key))) {
        AmbilWarnaPreference pref = (AmbilWarnaPreference) findPreference(getString(R.string.played_color_key));
        pref.forceSetValue(sharedPreferences.getInt(getString(R.string.played_color_key), 0));
      } else if (key.equals(getString(R.string.last_played_color_key))) {
        AmbilWarnaPreference pref = (AmbilWarnaPreference) findPreference(getString(R.string.last_played_color_key));
        pref.forceSetValue(sharedPreferences.getInt(getString(R.string.last_played_color_key), 0));
      }
    }
  }
}
