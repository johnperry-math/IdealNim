package name.cantanima.idealnim;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

/**
 * Created by cantanima on 8/11/17.
 */

public class Settings_Fragment extends PreferenceFragment {

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
  }

}
