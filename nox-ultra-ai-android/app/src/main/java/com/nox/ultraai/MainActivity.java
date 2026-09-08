package com.nox.ultraai;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String BACKEND_CONFIG_URL = "https://raw.githubusercontent.com/tamojuntoy477-pixel/JORNADABIBLICA/nox-ultra-ai-app/nox-backend-url.txt";
    private WebView webView;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new NoxBridge(), "NOX");

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("pt", "BR"));
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

    public class NoxBridge {
        @JavascriptInterface
        public void ask(String mode, String prompt) {
            if (prompt == null || prompt.trim().isEmpty()) return;
            new Thread(() -> {
                try {
                    String backendUrl = resolveBackendUrl();
                    String result = callBackend(backendUrl, mode, prompt.trim());
                    sendResult(result);
                } catch (Exception e) {
                    sendError(e.getMessage() == null ? "Falha ao consultar a NOX." : e.getMessage());
                }
            }).start();
        }

        @JavascriptInterface
        public void speak(String text) {
            runOnUiThread(() -> {
                if (tts != null && text != null) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nox-read");
                }
            });
        }
    }

    private String resolveBackendUrl() throws Exception {
        URL url = new URL(BACKEND_CONFIG_URL + "?t=" + System.currentTimeMillis());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setUseCaches(false);
        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) throw new Exception("Não foi possível localizar o servidor da NOX.");
        String value = readAll(conn.getInputStream()).trim();
        if (!value.startsWith("https://")) throw new Exception("Servidor seguro da NOX ainda não foi ativado.");
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String callBackend(String backendUrl, String mode, String prompt) throws Exception {
        URL url = new URL(backendUrl + "/api/chat");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");

        JSONObject body = new JSONObject();
        body.put("mode", mode == null ? "auto" : mode);
        body.put("prompt", prompt);
        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }

        int status = conn.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        String raw = readAll(stream);
        JSONObject json = new JSONObject(raw.isEmpty() ? "{}" : raw);
        if (status < 200 || status >= 300) {
            throw new Exception(json.optString("error", "Erro do servidor (" + status + ")"));
        }
        String text = json.optString("text", "").trim();
        if (text.isEmpty()) throw new Exception("A NOX respondeu sem texto.");
        return text;
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append('\n');
        reader.close();
        return sb.toString();
    }

    private void sendResult(String text) {
        runOnUiThread(() -> webView.evaluateJavascript("window.NOX_RESULT(" + JSONObject.quote(text) + ")", null));
    }

    private void sendError(String text) {
        runOnUiThread(() -> webView.evaluateJavascript("window.NOX_ERROR(" + JSONObject.quote(text) + ")", null));
    }
}
