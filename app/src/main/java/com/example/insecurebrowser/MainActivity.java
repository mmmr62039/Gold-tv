package com.example.insecurebrowser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.GeckoResult;

public class MainActivity extends Activity {
    private GeckoView geckoView;
    private GeckoSession session;
    private GeckoRuntime runtime;
    private static final String HOME_URL = "http://remote-pishgaman.runflare.com:32555/";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String currentUrl = HOME_URL;
    private int retryDelayMs = 5000;
    private boolean pageLoaded = false;

    private final Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!pageLoaded && hasRealInternet()) {
                session.loadUri(currentUrl);
                retryDelayMs = Math.min(retryDelayMs + 5000, 30000);
            }
            if (!pageLoaded) {
                handler.postDelayed(this, retryDelayMs);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();

        geckoView = new GeckoView(this);
        geckoView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(geckoView);

        GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .consoleOutput(true)
                .build();
        runtime = GeckoRuntime.create(this, settings);

        session = new GeckoSession();
        session.open(runtime);
        geckoView.setSession(session);

        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession session, LoadRequest request) {
                return GeckoResult.fromValue(AllowOrDeny.ALLOW);
            }

            @Override
            public void onLocationChange(GeckoSession session, String url, java.util.List<GeckoSession.PermissionDelegate.ContentPermission> perms, Boolean hasUserGesture) {
                if (url != null) currentUrl = url;
            }
        });

        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStart(GeckoSession session, String url) {
                pageLoaded = false;
                if (url != null) currentUrl = url;
            }

            @Override
            public void onPageStop(GeckoSession session, boolean success) {
                pageLoaded = success;
                if (success) {
                    retryDelayMs = 5000;
                    handler.removeCallbacks(reconnectRunnable);
                    session.purgeHistory();
                } else {
                    startLowPowerReconnectLoop();
                }
            }
        });

        loadFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadFromIntent(intent);
    }

    private void loadFromIntent(Intent intent) {
        Uri data = intent != null ? intent.getData() : null;
        String url = data != null ? data.toString() : HOME_URL;
        currentUrl = url;
        pageLoaded = false;
        session.loadUri(url);
    }

    private void hideSystemUi() {
        View decorView = getWindow().getDecorView();
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    private void startLowPowerReconnectLoop() {
        handler.removeCallbacks(reconnectRunnable);
        if (hasRealInternet()) {
            handler.postDelayed(reconnectRunnable, retryDelayMs);
        } else {
            handler.postDelayed(reconnectRunnable, 30000);
        }
    }

    private boolean hasRealInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(reconnectRunnable);
        if (session != null) session.close();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }
}
