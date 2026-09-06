package com.nox.monoposto;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(new RacingView(this));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    static class RacingView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final SharedPreferences save;
        private final RectF b1 = new RectF();
        private final RectF b2 = new RectF();
        private final RectF b3 = new RectF();
        private final RectF b4 = new RectF();
        private final RectF left = new RectF();
        private final RectF right = new RectF();
        private final RectF gas = new RectF();

        private int screen = 0; // 0 menu, 1 career, 2 garage, 3 race, 4 licenses, 5 result
        private int season;
        private int points;
        private int reputation;
        private int credits;
        private int performance;
        private int lap = 1;
        private float progress = 0f;
        private float speed = 0f;
        private float carOffset = 0f;
        private boolean gasPressed = false;
        private long lastFrame = 0L;
        private String resultText = "";

        RacingView(Context context) {
            super(context);
            setFocusable(true);
            save = context.getSharedPreferences("monoposto_career", Context.MODE_PRIVATE);
            season = save.getInt("season", 1);
            points = save.getInt("points", 0);
            reputation = save.getInt("reputation", 10);
            credits = save.getInt("credits", 2500);
            performance = save.getInt("performance", 1);
        }

        private void persist() {
            save.edit()
                    .putInt("season", season)
                    .putInt("points", points)
                    .putInt("reputation", reputation)
                    .putInt("credits", credits)
                    .putInt("performance", performance)
                    .apply();
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            if (screen == 3) {
                drawRace(c);
                postInvalidateOnAnimation();
                return;
            }
            drawBackground(c);
            if (screen == 0) drawMenu(c);
            else if (screen == 1) drawCareer(c);
            else if (screen == 2) drawGarage(c);
            else if (screen == 4) drawLicenses(c);
            else if (screen == 5) drawResult(c);
        }

        private void drawBackground(Canvas c) {
            int w = getWidth();
            int h = getHeight();
            p.setColor(Color.rgb(8, 10, 14));
            c.drawRect(0, 0, w, h, p);

            p.setColor(Color.rgb(22, 25, 32));
            Path track = new Path();
            track.moveTo(-80, h * 0.82f);
            track.cubicTo(w * 0.28f, h * 0.42f, w * 0.64f, h * 1.02f, w + 120, h * 0.56f);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(h * 0.22f);
            c.drawPath(track, p);
            p.setColor(Color.rgb(80, 82, 88));
            p.setStrokeWidth(4f);
            c.drawPath(track, p);
            p.setStyle(Paint.Style.FILL);

            p.setColor(Color.rgb(0, 220, 110));
            c.drawRect(0, 0, 10, h, p);
        }

        private void title(Canvas c, String text, float y) {
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setFakeBoldText(true);
            p.setTextSize(Math.max(34, getHeight() * 0.095f));
            c.drawText(text, getWidth() / 2f, y, p);
            p.setFakeBoldText(false);
        }

        private void subtitle(Canvas c, String text, float y) {
            p.setColor(Color.rgb(170, 178, 190));
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(Math.max(16, getHeight() * 0.036f));
            c.drawText(text, getWidth() / 2f, y, p);
        }

        private void button(Canvas c, RectF r, String text, boolean primary) {
            p.setColor(primary ? Color.rgb(0, 220, 110) : Color.rgb(33, 38, 48));
            c.drawRoundRect(r, 18, 18, p);
            p.setColor(primary ? Color.rgb(5, 10, 8) : Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setFakeBoldText(true);
            p.setTextSize(Math.max(16, getHeight() * 0.034f));
            c.drawText(text, r.centerX(), r.centerY() - (p.ascent() + p.descent()) / 2f, p);
            p.setFakeBoldText(false);
        }

        private void setMenuButtons() {
            float w = getWidth();
            float h = getHeight();
            float bw = Math.min(w * 0.46f, 560f);
            float bh = Math.max(54f, h * 0.105f);
            float x = (w - bw) / 2f;
            float y = h * 0.34f;
            float gap = h * 0.025f;
            b1.set(x, y, x + bw, y + bh);
            b2.set(x, y + (bh + gap), x + bw, y + (bh + gap) + bh);
            b3.set(x, y + 2 * (bh + gap), x + bw, y + 2 * (bh + gap) + bh);
            b4.set(x, y + 3 * (bh + gap), x + bw, y + 3 * (bh + gap) + bh);
        }

        private void drawMenu(Canvas c) {
            setMenuButtons();
            title(c, "MONOPOSTO", getHeight() * 0.18f);
            subtitle(c, "Carreira • Evolução • Corrida", getHeight() * 0.245f);
            button(c, b1, "MODO CARREIRA", true);
            button(c, b2, "CORRIDA RÁPIDA", false);
            button(c, b3, "GARAGEM", false);
            button(c, b4, "LICENCIAMENTO", false);
        }

        private void drawCareer(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            title(c, "MODO CARREIRA", h * 0.15f);
            p.setTextAlign(Paint.Align.LEFT);
            p.setColor(Color.WHITE);
            p.setTextSize(Math.max(18, h * 0.04f));
            float x = w * 0.12f;
            float y = h * 0.29f;
            float line = h * 0.075f;
            c.drawText("Piloto: NOX", x, y, p);
            c.drawText("Equipe: Fúria Racing Academy", x, y + line, p);
            c.drawText("Temporada: " + season, x, y + line * 2, p);
            c.drawText("Pontos: " + points + "   Reputação: " + reputation, x, y + line * 3, p);
            c.drawText("Créditos: " + credits + "   Carro: Nível " + performance, x, y + line * 4, p);

            float bw = w * 0.28f;
            float bh = h * 0.11f;
            b1.set(w * 0.58f, h * 0.30f, w * 0.58f + bw, h * 0.30f + bh);
            b2.set(w * 0.58f, h * 0.46f, w * 0.58f + bw, h * 0.46f + bh);
            b3.set(w * 0.58f, h * 0.62f, w * 0.58f + bw, h * 0.62f + bh);
            button(c, b1, "INICIAR ETAPA", true);
            button(c, b2, "EVOLUIR CARRO", false);
            button(c, b3, "VOLTAR", false);
        }

        private void drawGarage(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            title(c, "GARAGEM", h * 0.15f);
            drawCar(c, w * 0.50f, h * 0.46f, w * 0.30f, h * 0.20f, false);
            subtitle(c, "Fúria MR-01 • Performance " + performance + "/10", h * 0.67f);
            subtitle(c, "Upgrade: 500 créditos", h * 0.73f);
            b1.set(w * 0.31f, h * 0.79f, w * 0.49f, h * 0.90f);
            b2.set(w * 0.51f, h * 0.79f, w * 0.69f, h * 0.90f);
            button(c, b1, "MELHORAR", true);
            button(c, b2, "VOLTAR", false);
        }

        private void drawLicenses(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            title(c, "LICENCIAMENTO", h * 0.17f);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(Math.max(17, h * 0.038f));
            p.setColor(Color.WHITE);
            c.drawText("O jogo está preparado para receber conteúdo oficial licenciado.", w / 2f, h * 0.38f, p);
            p.setColor(Color.rgb(180, 188, 198));
            c.drawText("Equipes, pilotos, logos e marcas reais só podem entrar com autorização.", w / 2f, h * 0.47f, p);
            c.drawText("Nesta versão usamos nomes e carros fictícios.", w / 2f, h * 0.56f, p);
            b1.set(w * 0.39f, h * 0.72f, w * 0.61f, h * 0.84f);
            button(c, b1, "VOLTAR", true);
        }

        private void startRace() {
            screen = 3;
            lap = 1;
            progress = 0f;
            speed = 0f;
            carOffset = 0f;
            gasPressed = false;
            lastFrame = System.nanoTime();
            invalidate();
        }

        private void drawRace(Canvas c) {
            int w = getWidth();
            int h = getHeight();
            p.setColor(Color.rgb(70, 130, 70));
            c.drawRect(0, 0, w, h, p);
            p.setColor(Color.rgb(45, 47, 52));
            float roadLeft = w * 0.19f;
            float roadRight = w * 0.81f;
            c.drawRect(roadLeft, 0, roadRight, h, p);

            p.setColor(Color.WHITE);
            float center = w / 2f;
            for (int i = 0; i < 9; i++) {
                float y = (i * h / 8f + (progress * 8f) % (h / 8f));
                c.drawRect(center - 4, y, center + 4, y + h * 0.06f, p);
            }

            long now = System.nanoTime();
            float dt = Math.min(0.05f, (now - lastFrame) / 1_000_000_000f);
            lastFrame = now;
            float maxSpeed = 255f + performance * 12f;
            if (gasPressed) speed = Math.min(maxSpeed, speed + 125f * dt);
            else speed = Math.max(0f, speed - 85f * dt);
            progress += (speed / Math.max(1f, maxSpeed)) * 22f * dt;
            if (progress >= 100f) {
                progress -= 100f;
                lap++;
                if (lap > 3) finishRace();
            }

            float carX = center + carOffset;
            float carY = h * 0.68f;
            drawCar(c, carX, carY, w * 0.14f, h * 0.21f, true);

            p.setColor(Color.argb(205, 0, 0, 0));
            c.drawRoundRect(new RectF(w * 0.02f, h * 0.03f, w * 0.30f, h * 0.20f), 15, 15, p);
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(Math.max(16, h * 0.034f));
            c.drawText("VOLTA " + Math.min(lap, 3) + "/3", w * 0.045f, h * 0.09f, p);
            c.drawText((int) speed + " km/h", w * 0.045f, h * 0.15f, p);

            left.set(w * 0.03f, h * 0.76f, w * 0.15f, h * 0.94f);
            right.set(w * 0.17f, h * 0.76f, w * 0.29f, h * 0.94f);
            gas.set(w * 0.79f, h * 0.73f, w * 0.96f, h * 0.94f);
            button(c, left, "◀", false);
            button(c, right, "▶", false);
            button(c, gas, "ACELERAR", true);
        }

        private void finishRace() {
            screen = 5;
            gasPressed = false;
            points += 25;
            reputation += 3;
            credits += 750;
            if (points >= season * 100) season++;
            persist();
            resultText = "Pódio! +25 pontos • +750 créditos • +3 reputação";
            invalidate();
        }

        private void drawResult(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            title(c, "CORRIDA CONCLUÍDA", h * 0.24f);
            subtitle(c, resultText, h * 0.42f);
            b1.set(w * 0.36f, h * 0.60f, w * 0.64f, h * 0.74f);
            button(c, b1, "VOLTAR À CARREIRA", true);
        }

        private void drawCar(Canvas c, float cx, float cy, float cw, float ch, boolean topDown) {
            p.setColor(Color.rgb(225, 25, 50));
            RectF body = new RectF(cx - cw * 0.20f, cy - ch * 0.48f, cx + cw * 0.20f, cy + ch * 0.48f);
            c.drawRoundRect(body, 18, 18, p);
            p.setColor(Color.rgb(15, 15, 18));
            c.drawRect(cx - cw * 0.48f, cy - ch * 0.35f, cx + cw * 0.48f, cy - ch * 0.24f, p);
            c.drawRect(cx - cw * 0.43f, cy + ch * 0.28f, cx + cw * 0.43f, cy + ch * 0.39f, p);
            c.drawRect(cx - cw * 0.39f, cy - ch * 0.16f, cx - cw * 0.20f, cy + ch * 0.02f, p);
            c.drawRect(cx + cw * 0.20f, cy - ch * 0.16f, cx + cw * 0.39f, cy + ch * 0.02f, p);
            c.drawRect(cx - cw * 0.39f, cy + ch * 0.09f, cx - cw * 0.20f, cy + ch * 0.27f, p);
            c.drawRect(cx + cw * 0.20f, cy + ch * 0.09f, cx + cw * 0.39f, cy + ch * 0.27f, p);
            p.setColor(Color.rgb(0, 220, 110));
            c.drawCircle(cx, cy - ch * 0.08f, cw * 0.07f, p);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();

            if (screen == 3) {
                if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
                    if (gas.contains(x, y)) gasPressed = true;
                    if (left.contains(x, y)) carOffset = Math.max(-getWidth() * 0.22f, carOffset - getWidth() * 0.035f);
                    if (right.contains(x, y)) carOffset = Math.min(getWidth() * 0.22f, carOffset + getWidth() * 0.035f);
                }
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    gasPressed = false;
                }
                return true;
            }

            if (e.getAction() != MotionEvent.ACTION_UP) return true;

            if (screen == 0) {
                if (b1.contains(x, y)) screen = 1;
                else if (b2.contains(x, y)) startRace();
                else if (b3.contains(x, y)) screen = 2;
                else if (b4.contains(x, y)) screen = 4;
            } else if (screen == 1) {
                if (b1.contains(x, y)) startRace();
                else if (b2.contains(x, y)) screen = 2;
                else if (b3.contains(x, y)) screen = 0;
            } else if (screen == 2) {
                if (b1.contains(x, y)) {
                    if (credits >= 500 && performance < 10) {
                        credits -= 500;
                        performance++;
                        persist();
                    }
                } else if (b2.contains(x, y)) screen = 1;
            } else if (screen == 4) {
                if (b1.contains(x, y)) screen = 0;
            } else if (screen == 5) {
                if (b1.contains(x, y)) screen = 1;
            }
            invalidate();
            return true;
        }
    }
}
