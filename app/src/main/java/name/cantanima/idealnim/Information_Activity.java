package name.cantanima.idealnim;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class Information_Activity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_information_);
    WebView help_view = (WebView) findViewById(R.id.help_webview);
    help_view.loadUrl("file:///android_asset/Help.html");
  }
}
