package io.github.nguyengiathong.kymonbinhphap;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

/** Shows the bundled Kỳ Môn Binh Pháp page (assets/www) in a full-screen WebView. */
public class MainActivity extends Activity {

    // The assets are served on this https origin instead of file:// so the page gets an ordinary
    // origin for localStorage and same-origin font loads. Android reserves the host for exactly
    // this use; requests to it are answered from the APK and never reach the network.
    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/index.html";

    private WebView web;
    private View statusBarFill;
    private OnBackInvokedCallback backCallback;
    private boolean backCallbackRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        web.setBackgroundColor(getColor(R.color.paper));
        statusBarFill = new View(this);
        statusBarFill.setBackgroundColor(getColor(R.color.surface));
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(statusBarFill, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        setContentView(root);

        // Android 15+ always draws the app behind the system bars, so do it on every version
        // that can and keep the page clear of the bars, display cutouts and the keyboard.
        // The strip behind the status bar takes the colour of the page's header.
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                Insets ime = insets.getInsets(WindowInsets.Type.ime());
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) web.getLayoutParams();
                lp.setMargins(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
                web.setLayoutParams(lp);
                statusBarFill.getLayoutParams().height = bars.top;
                statusBarFill.requestLayout();
                return WindowInsets.CONSUMED;
            });
        }

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (!"https".equals(url.getScheme()) || !HOST.equals(url.getHost())) {
                    return null;
                }
                String path = url.getPath();
                if (path == null || path.endsWith("/")) {
                    path = (path == null ? "/" : path) + "index.html";
                }
                String mime = mimeType(path);
                try {
                    return new WebResourceResponse(mime, mime.startsWith("text/") ? "utf-8" : null,
                            getAssets().open("www" + path));
                } catch (IOException e) {
                    return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", null,
                            new ByteArrayInputStream(new byte[0]));
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (HOST.equals(url.getHost())) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, url));
                } catch (ActivityNotFoundException ignored) {
                    // Nothing on the device can open the link; stay on the page.
                }
                return true;
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                updateBackCallback();
            }
        });

        if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) {
            web.loadUrl(START_URL);
        }
    }

    private static String mimeType(String path) {
        String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        switch (ext) {
            case "html": return "text/html";
            case "css": return "text/css";
            case "js": return "text/javascript";
            case "json": return "application/json";
            case "txt": return "text/plain";
            case "svg": return "image/svg+xml";
            case "png": return "image/png";
            case "jpg": case "jpeg": return "image/jpeg";
            case "webp": return "image/webp";
            case "woff2": return "font/woff2";
            case "woff": return "font/woff";
            case "ttf": return "font/ttf";
            default: return "application/octet-stream";
        }
    }

    // The page's tabs are #hash links, so Back first walks back through the tabs the user
    // opened. On Android 13+ the callback is only registered while there is somewhere to go
    // back to, which leaves the system's predictive back-to-home gesture for the last step.
    private void updateBackCallback() {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        boolean canGoBack = web.canGoBack();
        if (canGoBack == backCallbackRegistered) {
            return;
        }
        if (backCallback == null) {
            backCallback = () -> web.goBack();
        }
        OnBackInvokedDispatcher dispatcher = getOnBackInvokedDispatcher();
        if (canGoBack) {
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        } else {
            dispatcher.unregisterOnBackInvokedCallback(backCallback);
        }
        backCallbackRegistered = canGoBack;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Only reached before Android 13; newer versions use updateBackCallback().
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
