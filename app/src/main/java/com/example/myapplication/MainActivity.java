package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import pub.devrel.easypermissions.EasyPermissions;
import android.graphics.Bitmap;
import android.webkit.WebChromeClient;
import android.widget.TextView;

import java.net.URI;
import java.net.URISyntaxException;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private WebView fittyCamera;
    private WebSocketClient wsClient;
    private static final int CAMERA_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        final String companyName = "fitty";
        final String sessionGUID = java.util.UUID.randomUUID().toString();

        connectFittyWebsocket(sessionGUID);
        EasyPermissions.requestPermissions(
                this,
                "This application needs camera access.",
                CAMERA_REQUEST_CODE,
                Manifest.permission.CAMERA);

        if(ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) {
            fittyCamera = (WebView) findViewById(R.id.fittyCamera);
            fittyCamera.loadUrl(String.format("https://demost.fittyai.com/mirror/%s/fitty-camera/%s", companyName, sessionGUID));
            configureWebView(fittyCamera);
        }

    }

    public static WebView configureWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient());
        WebChromeClientCustomPoster chromeClient = new WebChromeClientCustomPoster();
        webView.setWebChromeClient(chromeClient);
        webView.setWebContentsDebuggingEnabled(true);
        return webView;
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void connectFittyWebsocket(String sessionGUID) {
        URI uri;
        String sessionInitialisationEvent = "{" +
                "    \"company_identifier\": \"fitty\"," +
                "    \"session_id\": \"%s\"," +
                "    \"event_type\": \"session_start\"," +
                "    \"user_bio_information\": {" +
                "        \"height\": 184," +
                "        \"weight\": 77," +
                "        \"gender\": 0" +
                "    }," +
                "    \"timestamp\": \"2022-03-01 12:32:12\"" +
                "}";
        try {
            uri = new URI("wss://backenddev.fittyai.com:443");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        wsClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                wsClient.send(String.format(sessionInitialisationEvent, sessionGUID));
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView)findViewById(R.id.textview_first);
                        textView.setText(message);
                    }
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        wsClient.connect();
    }
}


class WebChromeClientCustomPoster extends WebChromeClient {
    private Activity activity;

    @Override
    public Bitmap getDefaultVideoPoster() {
        return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    }
    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            request.grant(request.getResources());
        }
    }
}