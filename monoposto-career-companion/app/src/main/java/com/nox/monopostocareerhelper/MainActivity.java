package com.nox.monopostocareerhelper;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS = "career";
    private static final String MONOPOSTO_PACKAGE = "com.gabama.monopostolite";

    private final String[] rounds = {
            "GP 1 — Austrália", "GP 2 — Japão", "GP 3 — Itália", "GP 4 — Mônaco",
            "GP 5 — Inglaterra", "GP 6 — Bélgica", "GP 7 — Brasil", "GP 8 — México",
            "GP 9 — Estados Unidos", "GP 10 — Singapura", "GP 11 — Abu Dhabi", "GP 12 — Final"
    };
    private final int[] pointsTable = {25,18,15,12,10,8,6,4,2,1};

    private SharedPreferences prefs;
    private TextView seasonText, roundText, pointsText, winsText, moneyText, nextRaceText, recordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(buildUi());
        refresh();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(12, 12, 16));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("MODO CARREIRA", 30, true);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        TextView subtitle = text("Carreira para usar junto com o Monoposto instalado", 15, false);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setPadding(0, dp(6), 0, dp(20));
        root.addView(subtitle);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackgroundColor(Color.rgb(28, 28, 34));
        root.addView(card, lpTop(0));

        seasonText = stat(card);
        roundText = stat(card);
        pointsText = stat(card);
        winsText = stat(card);
        moneyText = stat(card);
        recordText = stat(card);

        nextRaceText = text("", 22, true);
        nextRaceText.setTextColor(Color.WHITE);
        nextRaceText.setPadding(0, dp(22), 0, dp(12));
        root.addView(nextRaceText);

        Button race = button("CORRER NO MONOPOSTO");
        race.setOnClickListener(v -> launchMonoposto());
        root.addView(race, lpTop(8));

        Button result = button("REGISTRAR RESULTADO");
        result.setOnClickListener(v -> chooseResult());
        root.addView(result, lpTop(10));

        Button reset = button("NOVA CARREIRA");
        reset.setOnClickListener(v -> confirmReset());
        root.addView(reset, lpTop(10));

        TextView note = text("Depois da corrida, volte aqui e registre sua posição. O progresso fica salvo no celular.", 14, false);
        note.setTextColor(Color.GRAY);
        note.setPadding(0, dp(18), 0, 0);
        root.addView(note);

        return scroll;
    }

    private TextView stat(LinearLayout parent) {
        TextView tv = text("", 17, true);
        tv.setTextColor(Color.WHITE);
        tv.setPadding(0, dp(5), 0, dp(5));
        parent.addView(tv);
        return tv;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.rgb(184, 0, 24));
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(14), dp(10), dp(14));
        return b;
    }

    private TextView text(String s, int size, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(size);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private LinearLayout.LayoutParams lpTop(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(top);
        return lp;
    }

    private void launchMonoposto() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(MONOPOSTO_PACKAGE);
        if (intent == null) {
            Toast.makeText(this, "Monoposto normal não encontrado no celular.", Toast.LENGTH_LONG).show();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void chooseResult() {
        String[] positions = {"P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10", "P11 ou pior / abandono"};
        new AlertDialog.Builder(this)
                .setTitle("Resultado da corrida")
                .setItems(positions, (dialog, which) -> recordResult(which + 1))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void recordResult(int position) {
        int season = prefs.getInt("season", 1);
        int round = prefs.getInt("round", 0);
        int points = prefs.getInt("points", 0);
        int wins = prefs.getInt("wins", 0);
        int money = prefs.getInt("money", 0);
        int best = prefs.getInt("best", 99);

        if (position <= 10) points += pointsTable[position - 1];
        if (position == 1) wins++;
        best = Math.min(best, position);
        money += Math.max(5000, 30000 - (position - 1) * 2200);

        round++;
        if (round >= rounds.length) {
            season++;
            round = 0;
            points = 0;
        }

        prefs.edit()
                .putInt("season", season)
                .putInt("round", round)
                .putInt("points", points)
                .putInt("wins", wins)
                .putInt("money", money)
                .putInt("best", best)
                .apply();

        refresh();
        Toast.makeText(this, "Resultado salvo!", Toast.LENGTH_SHORT).show();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Nova carreira")
                .setMessage("Apagar o progresso atual e começar de novo?")
                .setPositiveButton("Sim", (d, w) -> {
                    prefs.edit().clear().apply();
                    refresh();
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void refresh() {
        int season = prefs.getInt("season", 1);
        int round = prefs.getInt("round", 0);
        int points = prefs.getInt("points", 0);
        int wins = prefs.getInt("wins", 0);
        int money = prefs.getInt("money", 0);
        int best = prefs.getInt("best", 99);

        seasonText.setText("Temporada: " + season);
        roundText.setText("Etapa: " + (round + 1) + "/" + rounds.length);
        pointsText.setText("Pontos: " + points);
        winsText.setText("Vitórias: " + wins);
        moneyText.setText("Créditos: " + money);
        recordText.setText("Melhor resultado: " + (best == 99 ? "—" : "P" + best));
        nextRaceText.setText("PRÓXIMA CORRIDA\n" + rounds[round]);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
