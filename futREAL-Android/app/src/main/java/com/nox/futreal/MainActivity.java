package com.nox.futreal;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            setContentView(new FutRealView(this));
        } catch (Throwable e) {
            TextView t = new TextView(this);
            t.setBackgroundColor(Color.rgb(5, 15, 10));
            t.setTextColor(Color.WHITE);
            t.setTextSize(22);
            t.setPadding(32, 32, 32, 32);
            t.setText("futREAL\n\nO jogo abriu, mas o modo grafico encontrou um erro.\nReabra o app.");
            setContentView(t);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    static class FutRealView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final SharedPreferences prefs;
        private int screen = 0; // 0 home, 1 carreira, 2 treino, 3 partida
        private int games, goals, wins, points, speed, shot, pass;
        private int homeScore, awayScore;
        private float matchTime;
        private long lastNs;
        private float px = .50f, py = .76f;
        private float bx = .50f, by = .70f, bvx, bvy;
        private boolean hasBall = true, sprint = false;
        private float joyX = 0, joyY = 0;

        private final RectF career = new RectF();
        private final RectF quick = new RectF();
        private final RectF train = new RectF();
        private final RectF profile = new RectF();
        private final RectF back = new RectF();
        private final RectF nextMatch = new RectF();
        private final RectF trainSpeed = new RectF();
        private final RectF trainShot = new RectF();
        private final RectF trainPass = new RectF();

        FutRealView(Context c) {
            super(c);
            setFocusable(true);
            prefs = c.getSharedPreferences("futreal_save", Context.MODE_PRIVATE);
            games = prefs.getInt("games", 0);
            goals = prefs.getInt("goals", 0);
            wins = prefs.getInt("wins", 0);
            points = prefs.getInt("points", 6);
            speed = prefs.getInt("speed", 70);
            shot = prefs.getInt("shot", 68);
            pass = prefs.getInt("pass", 66);
            p.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD));
        }

        private void save() {
            prefs.edit()
                    .putInt("games", games)
                    .putInt("goals", goals)
                    .putInt("wins", wins)
                    .putInt("points", points)
                    .putInt("speed", speed)
                    .putInt("shot", shot)
                    .putInt("pass", pass)
                    .apply();
        }

        private int ovr() {
            return Math.round((speed + shot + pass) / 3f);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            if (getWidth() <= 0 || getHeight() <= 0) return;
            if (screen == 1) drawCareer(c);
            else if (screen == 2) drawTraining(c);
            else if (screen == 3) {
                updateMatch();
                drawMatch(c);
                postInvalidateOnAnimation();
            } else drawHome(c);
        }

        private void bg(Canvas c) {
            c.drawColor(Color.rgb(5, 15, 10));
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(10, 32, 21));
            c.drawRect(0, 0, getWidth(), getHeight(), p);
            p.setColor(Color.argb(55, 180, 255, 70));
            c.drawCircle(getWidth() * .88f, getHeight() * .12f, getHeight() * .34f, p);
        }

        private void text(Canvas c, String s, float x, float y, float size, int color) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            p.setTextSize(size);
            c.drawText(s, x, y, p);
        }

        private void round(Canvas c, RectF r, int color, float radius) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            c.drawRoundRect(r, radius, radius, p);
        }

        private void brand(Canvas c) {
            text(c, "fut", 28, 52, 32, Color.WHITE);
            text(c, "REAL", 76, 52, 32, Color.rgb(202, 255, 85));
        }

        private void drawHome(Canvas c) {
            bg(c);
            brand(c);
            float w = getWidth(), h = getHeight();
            text(c, "FUTEBOL MOBILE", 32, h * .22f, 18, Color.rgb(202,255,85));
            text(c, "Sua carreira. Seu futebol.", 32, h * .32f, 34, Color.WHITE);
            text(c, "Jogue, evolua e vire estrela no futREAL.", 32, h * .39f, 17, Color.LTGRAY);

            float startX = w * .46f;
            float startY = h * .16f;
            float cardW = (w - startX - 42) / 2f;
            float cardH = (h - startY - 48) / 2f;
            float gap = 14;

            career.set(startX, startY, startX + cardW, startY + cardH);
            quick.set(startX + cardW + gap, startY, w - 22, startY + cardH);
            train.set(startX, startY + cardH + gap, startX + cardW, h - 22);
            profile.set(startX + cardW + gap, startY + cardH + gap, w - 22, h - 22);

            menu(c, career, "MODO CARREIRA", "Temporada e evolucao", true);
            menu(c, quick, "PARTIDA RAPIDA", "Entre em campo", false);
            menu(c, train, "TREINO", "Melhore atributos", false);
            menu(c, profile, "MEU JOGADOR", "OVR " + ovr(), false);
        }

        private void menu(Canvas c, RectF r, String title, String sub, boolean hot) {
            round(c, r, hot ? Color.rgb(24, 63, 39) : Color.rgb(16, 44, 31), 22);
            text(c, title, r.left + 18, r.centerY() - 3, 18, Color.WHITE);
            text(c, sub, r.left + 18, r.centerY() + 27, 14, Color.LTGRAY);
        }

        private void drawBack(Canvas c) {
            back.set(20, 66, 92, 120);
            round(c, back, Color.rgb(18,48,34), 16);
            text(c, "<", 46, 105, 30, Color.WHITE);
        }

        private void drawCareer(Canvas c) {
            bg(c); brand(c); drawBack(c);
            float w = getWidth(), h = getHeight();
            text(c, "MODO CARREIRA", 110, 104, 26, Color.WHITE);

            RectF hero = new RectF(30, 142, w * .60f, h - 28);
            round(c, hero, Color.rgb(16, 49, 34), 24);
            text(c, "TEMPORADA 1", hero.left + 22, hero.top + 36, 16, Color.rgb(202,255,85));
            text(c, "NOX JR.", hero.left + 22, hero.top + 84, 32, Color.WHITE);
            text(c, "Atletico Aurora - ATA", hero.left + 22, hero.top + 116, 17, Color.LTGRAY);
            text(c, "OVR " + ovr(), hero.right - 120, hero.top + 80, 26, Color.rgb(202,255,85));
            text(c, "Jogos: " + games + "   Gols: " + goals + "   Vitorias: " + wins,
                    hero.left + 22, hero.top + 160, 18, Color.WHITE);

            nextMatch.set(hero.left + 22, hero.bottom - 58, hero.right - 22, hero.bottom - 16);
            round(c, nextMatch, Color.rgb(202,255,85), 16);
            text(c, "JOGAR PROXIMA PARTIDA", nextMatch.left + 22, nextMatch.centerY() + 6, 16, Color.rgb(7,20,13));

            RectF attrs = new RectF(w * .63f, 142, w - 28, h - 28);
            round(c, attrs, Color.rgb(13, 40, 28), 24);
            text(c, "ATRIBUTOS", attrs.left + 20, attrs.top + 34, 16, Color.rgb(202,255,85));
            attr(c, "Velocidade", speed, attrs.left + 20, attrs.top + 78, attrs.width() - 40);
            attr(c, "Finalizacao", shot, attrs.left + 20, attrs.top + 136, attrs.width() - 40);
            attr(c, "Passe", pass, attrs.left + 20, attrs.top + 194, attrs.width() - 40);
        }

        private void attr(Canvas c, String name, int value, float x, float y, float width) {
            text(c, name, x, y, 15, Color.WHITE);
            text(c, String.valueOf(value), x + width - 30, y, 15, Color.rgb(202,255,85));
            RectF base = new RectF(x, y + 10, x + width, y + 20);
            round(c, base, Color.rgb(35,60,47), 7);
            RectF fill = new RectF(x, y + 10, x + width * (value / 99f), y + 20);
            round(c, fill, Color.rgb(202,255,85), 7);
        }

        private void drawTraining(Canvas c) {
            bg(c); brand(c); drawBack(c);
            float w = getWidth(), h = getHeight();
            text(c, "CENTRO DE TREINO", 110, 104, 26, Color.WHITE);
            text(c, "Pontos disponiveis: " + points, 30, 156, 20, Color.rgb(202,255,85));

            float gap = 16;
            float top = 190;
            float cardW = (w - 60 - gap * 2) / 3f;
            float bottom = h - 26;
            trainSpeed.set(30, top, 30 + cardW, bottom);
            trainShot.set(30 + cardW + gap, top, 30 + cardW * 2 + gap, bottom);
            trainPass.set(30 + cardW * 2 + gap * 2, top, w - 30, bottom);
            trainCard(c, trainSpeed, "VELOCIDADE", speed);
            trainCard(c, trainShot, "FINALIZACAO", shot);
            trainCard(c, trainPass, "PASSE", pass);
        }

        private void trainCard(Canvas c, RectF r, String name, int value) {
            round(c, r, Color.rgb(15,45,31), 22);
            text(c, name, r.left + 18, r.top + 36, 17, Color.WHITE);
            text(c, String.valueOf(value), r.left + 18, r.top + 90, 38, Color.rgb(202,255,85));
            RectF b = new RectF(r.left + 18, r.bottom - 52, r.right - 18, r.bottom - 14);
            round(c, b, points > 0 ? Color.rgb(202,255,85) : Color.DKGRAY, 14);
            text(c, points > 0 ? "+1 ATRIBUTO" : "SEM PONTOS", b.left + 16, b.centerY() + 5, 14,
                    points > 0 ? Color.rgb(7,20,13) : Color.LTGRAY);
        }

        private void startMatch() {
            screen = 3;
            homeScore = 0;
            awayScore = 0;
            matchTime = 0;
            lastNs = System.nanoTime();
            px = .50f; py = .76f;
            bx = .50f; by = .70f;
            bvx = 0; bvy = 0;
            hasBall = true;
            joyX = joyY = 0;
            invalidate();
        }

        private void updateMatch() {
            long now = System.nanoTime();
            float dt = Math.min(.033f, Math.max(.001f, (now - lastNs) / 1000000000f));
            lastNs = now;
            matchTime += dt * 10f;

            float move = (sprint ? .34f : .22f) * (speed / 70f);
            px = clamp(px + joyX * move * dt, .05f, .95f);
            py = clamp(py + joyY * move * dt, .08f, .94f);

            if (hasBall) {
                bx = px;
                by = py - .035f;
            } else {
                bx += bvx * dt;
                by += bvy * dt;
                bvx *= .985f;
                bvy *= .985f;
                if (dist(px, py, bx, by) < .05f) hasBall = true;
            }

            if (by < .035f && bx > .40f && bx < .60f) {
                homeScore++;
                goals++;
                resetKickoff();
            } else if (by > .98f) {
                resetKickoff();
            }

            if (matchTime >= 90f) finishMatch();
        }

        private void resetKickoff() {
            px = .50f; py = .76f;
            bx = .50f; by = .70f;
            bvx = bvy = 0;
            hasBall = true;
        }

        private void finishMatch() {
            games++;
            if (homeScore > awayScore) {
                wins++;
                points += 2;
            } else points += 1;
            save();
            screen = 1;
            invalidate();
        }

        private void drawMatch(Canvas c) {
            float w = getWidth(), h = getHeight();
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(16, 105, 48));
            c.drawRect(0, 0, w, h, p);

            p.setColor(Color.argb(45, 255,255,255));
            for (int i = 0; i < 10; i += 2) {
                c.drawRect(i * w / 10f, 0, (i + 1) * w / 10f, h, p);
            }

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            p.setColor(Color.WHITE);
            c.drawRect(18, 18, w - 18, h - 18, p);
            c.drawLine(18, h / 2f, w - 18, h / 2f, p);
            c.drawCircle(w / 2f, h / 2f, Math.min(w,h) * .12f, p);
            c.drawRect(w * .38f, 18, w * .62f, h * .14f, p);
            c.drawRect(w * .38f, h * .86f, w * .62f, h - 18, p);
            p.setStyle(Paint.Style.FILL);

            float playerSX = px * w, playerSY = py * h;
            float ballSX = bx * w, ballSY = by * h;
            p.setColor(Color.rgb(202,255,85));
            c.drawCircle(playerSX, playerSY, Math.max(12, h * .028f), p);
            p.setColor(Color.WHITE);
            c.drawCircle(ballSX, ballSY, Math.max(6, h * .014f), p);

            p.setColor(Color.rgb(235,70,70));
            float[][] bots = {{.28f,.30f},{.50f,.25f},{.72f,.31f},{.40f,.47f},{.64f,.48f}};
            for (float[] b : bots) c.drawCircle(b[0]*w,b[1]*h,Math.max(11,h*.025f),p);

            RectF score = new RectF(w*.36f, 12, w*.64f, 62);
            round(c, score, Color.argb(220, 5,15,10), 14);
            text(c, "AUR  " + homeScore + "  x  " + awayScore + "  RIV", score.left+18, score.centerY()+6, 18, Color.WHITE);
            text(c, String.format("%02d:00", Math.min(90, (int)matchTime)), score.right-74, score.centerY()+6, 15, Color.rgb(202,255,85));

            drawControls(c);
        }

        private void drawControls(Canvas c) {
            float w = getWidth(), h = getHeight();
            float jr = Math.min(w,h) * .10f;
            float jx = w * .13f, jy = h * .77f;
            p.setColor(Color.argb(85,255,255,255));
            c.drawCircle(jx,jy,jr,p);
            p.setColor(Color.argb(150,255,255,255));
            c.drawCircle(jx + joyX*jr*.55f, jy + joyY*jr*.55f, jr*.42f, p);

            button(c, w*.88f, h*.72f, Math.min(w,h)*.07f, "CHUTE", Color.rgb(202,255,85));
            button(c, w*.77f, h*.82f, Math.min(w,h)*.065f, "PASSE", Color.WHITE);
            button(c, w*.91f, h*.88f, Math.min(w,h)*.060f, "SPRINT", Color.rgb(120,220,255));
        }

        private void button(Canvas c, float x, float y, float r, String s, int color) {
            p.setColor(Color.argb(190,20,35,26));
            c.drawCircle(x,y,r,p);
            p.setTextAlign(Paint.Align.CENTER);
            text(c, s, x, y+5, 13, color);
            p.setTextAlign(Paint.Align.LEFT);
        }

        private void shoot() {
            if (!hasBall) return;
            hasBall = false;
            float power = .62f + (shot - 60) * .005f;
            bvx = joyX * .18f;
            bvy = -power;
        }

        private void doPass() {
            if (!hasBall) return;
            hasBall = false;
            bvx = joyX * .42f;
            bvy = joyY * .42f - .18f;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            float w = getWidth(), h = getHeight();

            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                if (screen == 0) {
                    if (career.contains(x,y)) { screen = 1; invalidate(); return true; }
                    if (quick.contains(x,y)) { startMatch(); return true; }
                    if (train.contains(x,y)) { screen = 2; invalidate(); return true; }
                    if (profile.contains(x,y)) { screen = 1; invalidate(); return true; }
                } else if (screen == 1) {
                    if (back.contains(x,y)) { screen = 0; invalidate(); return true; }
                    if (nextMatch.contains(x,y)) { startMatch(); return true; }
                } else if (screen == 2) {
                    if (back.contains(x,y)) { screen = 0; invalidate(); return true; }
                    if (points > 0 && trainSpeed.contains(x,y)) { speed = Math.min(99,speed+1); points--; save(); invalidate(); return true; }
                    if (points > 0 && trainShot.contains(x,y)) { shot = Math.min(99,shot+1); points--; save(); invalidate(); return true; }
                    if (points > 0 && trainPass.contains(x,y)) { pass = Math.min(99,pass+1); points--; save(); invalidate(); return true; }
                }
            }

            if (screen == 3) {
                float jx = w * .13f, jy = h * .77f, jr = Math.min(w,h) * .14f;
                float dx = x - jx, dy = y - jy;
                float d = (float)Math.sqrt(dx*dx + dy*dy);

                if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
                    if (x < w*.36f && y > h*.52f) {
                        if (d > 1) { joyX = clamp(dx/jr,-1,1); joyY = clamp(dy/jr,-1,1); }
                        return true;
                    }
                    if (distPx(x,y,w*.88f,h*.72f) < Math.min(w,h)*.10f) { shoot(); return true; }
                    if (distPx(x,y,w*.77f,h*.82f) < Math.min(w,h)*.10f) { doPass(); return true; }
                    if (distPx(x,y,w*.91f,h*.88f) < Math.min(w,h)*.10f) { sprint = true; return true; }
                }

                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    joyX = joyY = 0;
                    sprint = false;
                    return true;
                }
            }
            return true;
        }

        private float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }
        private float dist(float x1,float y1,float x2,float y2) {
            float dx=x1-x2, dy=y1-y2; return (float)Math.sqrt(dx*dx+dy*dy);
        }
        private float distPx(float x1,float y1,float x2,float y2) {
            float dx=x1-x2, dy=y1-y2; return (float)Math.sqrt(dx*dx+dy*dy);
        }
    }
}
