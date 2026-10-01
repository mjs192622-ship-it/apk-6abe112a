package com.n.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.View;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.widget.ProgressBar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.PermissionRequest;
import android.webkit.GeolocationPermissions;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;
    private String fcmTokenForWebView = "";
    private static final String WEBSITE_URL = "https://002-1gl.pages.dev/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Immersive sticky mode - hide navigation bar
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        
        setupWebView();
        
        
        // Request runtime permissions
        java.util.List<String> permissionsNeeded = new java.util.ArrayList<>();
        String[] requiredPerms = new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR};
        for (String perm : requiredPerms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(perm);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 2001);
        }
        
        // Add JavaScript Bridge
        webView.addJavascriptInterface(new NativeBridge(), "AndroidBridge");
        
        // Handle deep link intent
        handleIntent(getIntent());
        
        // Load directly; ConnectivityManager can be unreliable on some devices/VPNs.
        // WebView will show its own error page if the connection is actually unavailable.
        webView.loadUrl(WEBSITE_URL);
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleWebViewUrl(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null && request.getUrl() != null) {
                    return handleWebViewUrl(view, request.getUrl().toString());
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) progressBar.setProgress(newProgress);
            }
            
        });

        
    }


    private boolean handleWebViewUrl(WebView view, String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String lower = url.toLowerCase();
        if (lower.startsWith("tel:") || lower.startsWith("mailto:") || lower.startsWith("sms:") || lower.startsWith("smsto:") || lower.startsWith("whatsapp:") || lower.startsWith("market:") || lower.startsWith("intent:")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                startActivity(intent);
                return true;
            } catch (Exception ignored) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
                    return true;
                } catch (Exception ignoredAgain) {
                    return true;
                }
            }
        }
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return true;
        }
        view.loadUrl(url);
        return true;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            String deepUrl = intent.getData().toString();
            // Custom URL scheme: convert myapp://path to website URL
            if (deepUrl.startsWith("https0021glpagesdev://")) {
                String path = deepUrl.replace("https0021glpagesdev://", "");
                String baseUrl = WEBSITE_URL.endsWith("/") ? WEBSITE_URL : WEBSITE_URL + "/";
                deepUrl = baseUrl + path;
            }
            if (deepUrl.startsWith("http")) {
                webView.loadUrl(deepUrl);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    

    // JavaScript Bridge - access via window.AndroidBridge in your web JS
    private class NativeBridge {
        @android.webkit.JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }

        @android.webkit.JavascriptInterface
        public void shareText(String text) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(shareIntent, "Share"));
        }

        @android.webkit.JavascriptInterface
        public void openInBrowser(String url) {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        }

        @android.webkit.JavascriptInterface
        public void copyToClipboard(String text) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("text", text));
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Copied!", Toast.LENGTH_SHORT).show());
        }

        @android.webkit.JavascriptInterface
        public void vibrate(int ms) {
            android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(ms);
        }

        @android.webkit.JavascriptInterface
        public String getAppVersion() {
            try {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) { return "unknown"; }
        }

        @android.webkit.JavascriptInterface
        public void dial(String phone) {
            startActivity(new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + phone)));
        }

        @android.webkit.JavascriptInterface
        public void sendEmail(String to, String subject, String body) {
            Intent intent = new Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:" + to));
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, body);
            startActivity(intent);
        }

        @android.webkit.JavascriptInterface
        public void closeApp() {
            finish();
        }

        @android.webkit.JavascriptInterface
        public String getDeviceInfo() {
            return "{\"brand\":\"" + Build.BRAND + "\",\"model\":\"" + Build.MODEL + "\",\"sdk\":" + Build.VERSION.SDK_INT + ",\"android\":\"" + Build.VERSION.RELEASE + "\"}";
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}