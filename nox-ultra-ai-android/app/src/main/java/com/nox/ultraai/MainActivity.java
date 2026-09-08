package com.nox.ultraai;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
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
        public void ask(String apiKey, String model, String mode, String prompt) {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                sendError("Digite sua chave da API para usar a IA.");
                return;
            }
            if (prompt == null || prompt.trim().isEmpty()) return;

            new Thread(() -> {
                try {
                    boolean useWeb = "research".equals(mode);
                    String instructions = systemFor(mode);
                    String first = callResponses(apiKey.trim(), model, instructions, prompt, useWeb, "ultra".equals(mode));
                    String result = first;

                    if ("ultra".equals(mode)) {
                        String improve = "Revise a resposta abaixo. Corrija erros, melhore clareza e precisão, preserve o idioma do usuário e devolva apenas a resposta final melhorada.\n\nRESPOSTA:\n" + first;
                        result = callResponses(apiKey.trim(), model, systemFor("ultra-review"), improve, false, true);
                    }
                    sendResult(result);
                } catch (Exception e) {
                    sendError(e.getMessage() == null ? "Falha ao consultar a IA." : e.getMessage());
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

    private String systemFor(String mode) {
        String base = "Você é NOX ULTRA AI, uma assistente útil, clara, criativa e segura. Responda no idioma do usuário. Não finja capacidades que não possui.";
        switch (mode) {
            case "code": return base + " Priorize código correto, explique bugs de forma prática e use blocos de código quando necessário.";
            case "research": return base + " Priorize fatos verificáveis e pesquisa atual. Diferencie fatos de incertezas.";
            case "creative": return base + " Priorize criatividade, ideias originais e boa apresentação.";
            case "ultra": return base + " Trabalhe com alto cuidado: verifique a própria resposta e priorize precisão.";
            case "ultra-review": return base + " Você é o revisor final. Elimine erros, contradições e conteúdo inútil.";
            default: return base + " Escolha a abordagem mais adequada para a tarefa.";
        }
    }

    private String callResponses(String apiKey, String model, String instructions, String input, boolean useWeb, boolean highReasoning) throws Exception {
        if (model == null || model.trim().isEmpty()) model = "gpt-5.6-sol";

        URL url = new URL("https://api.openai.com/v1/responses");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject body = new JSONObject();
        body.put("model", model.trim());
        body.put("instructions", instructions);
        body.put("input", input);
        if (highReasoning) {
            JSONObject reasoning = new JSONObject();
            reasoning.put("effort", "high");
            body.put("reasoning", reasoning);
        }
        if (useWeb) {
            JSONArray tools = new JSONArray();
            JSONObject web = new JSONObject();
            web.put("type", "web_search");
            tools.put(web);
            body.put("tools", tools);
        }

        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }

        int status = conn.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        String raw = readAll(stream);
        JSONObject json = new JSONObject(raw);

        if (status < 200 || status >= 300) {
            if (json.has("error")) {
                JSONObject err = json.getJSONObject("error");
                throw new Exception(err.optString("message", "Erro da API (" + status + ")"));
            }
            throw new Exception("Erro da API (" + status + ")");
        }

        String text = extractOutputText(json);
        if (text.isEmpty()) throw new Exception("A IA respondeu sem texto.");
        return text;
    }

    private String extractOutputText(JSONObject json) {
        StringBuilder sb = new StringBuilder();
        JSONArray output = json.optJSONArray("output");
        if (output == null) return json.optString("output_text", "");
        for (int i = 0; i < output.length(); i++) {
            JSONObject item = output.optJSONObject(i);
            if (item == null) continue;
            JSONArray content = item.optJSONArray("content");
            if (content == null) continue;
            for (int j = 0; j < content.length(); j++) {
                JSONObject part = content.optJSONObject(j);
                if (part == null) continue;
                if ("output_text".equals(part.optString("type"))) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(part.optString("text", ""));
                }
            }
        }
        return sb.toString().trim();
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "{}";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
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
