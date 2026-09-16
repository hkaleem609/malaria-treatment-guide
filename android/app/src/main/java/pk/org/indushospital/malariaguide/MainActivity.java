package pk.org.indushospital.malariaguide;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.app.Activity;

/**
 * A thin WebView around the offline guide.
 *
 * The whole application is one HTML file in assets/. There is no network code
 * here and the app declares no INTERNET permission — everything it needs is
 * bundled, which is the point: it has to work for a CHW with no signal, and a
 * zero-permission health app is far easier to get past Play Protect and past a
 * hospital IT department.
 *
 * Loading over file:///android_asset/ rather than an asset-loader https:// origin
 * is deliberate. The page skips service-worker registration on non-http(s)
 * origins, so on file:// it never attempts a network fetch. Served over a fake
 * https origin it would try, wait for the 3s timeout on every request, and fall
 * back to cache — slower, for no benefit in an app that is already fully local.
 */
public class MainActivity extends Activity {

    private WebView web;

    private static final String START_URL = "file:///android_asset/index.html";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();

        // Required: the guideline engine is JavaScript.
        s.setJavaScriptEnabled(true);

        // Required: the care-setting gate and the language choice are stored in
        // localStorage. Without this the app re-asks "Where are you working?"
        // on every launch and forgets Urdu every time.
        s.setDomStorageEnabled(true);

        // Let the user scale text through Android's display settings; field
        // workers often run large system fonts.
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);

        // No remote content is ever loaded, so keep the door shut.
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        // Keep every navigation inside the WebView; the page has no outbound links.
        web.setWebViewClient(new WebViewClient());

        if (savedInstanceState == null) {
            web.loadUrl(START_URL);
        } else {
            web.restoreState(savedInstanceState);
        }
    }

    /** Preserve the rendered protocol across rotation rather than reloading. */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    /**
     * Back should step through the page's own history — closing collapsed
     * sections, leaving the role gate — before it exits the app. Without this
     * a single Back press quits from anywhere, which reads as a crash.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
