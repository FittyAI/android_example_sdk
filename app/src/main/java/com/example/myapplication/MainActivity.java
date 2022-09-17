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
    private WebView webView;
    private WebSocketClient mWebSocketClient;
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    @SuppressLint("SetJavaScriptEnabled")
    public static void configureWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        final String sessionGUID = java.util.UUID.randomUUID().toString();

        connectWebSocket(sessionGUID);

        EasyPermissions.requestPermissions(
                this,
                "A partir deste ponto a permissão de câmera é necessária.",
                200,
                Manifest.permission.CAMERA);

        if(ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) {
            webView = (WebView) findViewById(R.id.webview);
            webView.loadUrl(String.format("https://docs-example.d6x4yzgo302vn.amplifyapp.com/mirror/%s/fitty-camera/%s", "fittar", sessionGUID));
            configureWebView(webView);
            webView.setWebViewClient(new WebViewClient());
            WebChromeClientCustomPoster chromeClient = new WebChromeClientCustomPoster();
            webView.setWebChromeClient(chromeClient);
            WebView.setWebContentsDebuggingEnabled(true);
            Log.d("myTag", "It's not done mate");
        }
        Log.d("myTag", "It's done mate");

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

    private void connectWebSocket(String sessionGUID) {
        URI uri;
        try {
            uri = new URI("wss://backenddev.fittyai.com:443");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");

                mWebSocketClient.send(String.format("{\n" +
                        "    \"company_identifier\": \"fitty\",\n" +
                        "    \"session_id\": \"%s\",\n" +
                        "    \"event_type\": \"session_start\",\n" +
                        "    \"user_bio_information\": {\n" +
                        "        \"height\": 184,\n" +
                        "        \"weight\": 77,\n" +
                        "        \"gender\": 0\n" +
                        "    },\n" +
                        "    \"timestamp\": \"2022-03-01 12:32:12\"\n" +
                        "}", sessionGUID));
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
        mWebSocketClient.connect();
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