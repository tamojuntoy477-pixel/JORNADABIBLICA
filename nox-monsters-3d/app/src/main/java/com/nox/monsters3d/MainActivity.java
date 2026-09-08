package com.nox.monsters3d;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private GameView gameView;
    private TextView hud;
    private TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        immersive();
        showMenu();
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private GradientDrawable rounded(int color, int stroke, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        g.setStroke(2, stroke);
        return g;
    }

    private TextView text(String value, float size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        v.setPadding(16, 10, 16, 10);
        return v;
    }

    private Button makeButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(17f);
        b.setAllCaps(false);
        b.setBackground(rounded(Color.rgb(31, 69, 104), Color.rgb(108, 190, 242), 30f));
        return b;
    }

    private void showMenu() {
        if (gameView != null) {
            gameView.stop();
            gameView = null;
        }
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(6, 18, 31));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(42, 32, 42, 32);
        card.setBackground(rounded(Color.rgb(13, 39, 64), Color.rgb(72, 158, 219), 44f));

        TextView icon = text("◆", 66f, Color.rgb(255, 214, 72));
        TextView title = text("NOX MONSTERS 3D", 32f, Color.WHITE);
        TextView sub = text("Explore o mundo, encontre 8 criaturas e capture todas.", 15f,
                Color.rgb(195, 220, 239));
        Button play = makeButton("JOGAR");
        play.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startGame(); }
        });

        card.addView(icon, new LinearLayout.LayoutParams(-1, -2));
        card.addView(title, new LinearLayout.LayoutParams(-1, -2));
        card.addView(sub, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, 24, 0, 0);
        card.addView(play, bp);

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.62f), -2);
        cp.gravity = Gravity.CENTER;
        root.addView(card, cp);
        setContentView(root);
    }

    private void startGame() {
        FrameLayout root = new FrameLayout(this);
        gameView = new GameView();
        root.addView(gameView, new FrameLayout.LayoutParams(-1, -1));

        hud = text("Capturados: 0/8", 15f, Color.WHITE);
        hud.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        hud.setBackground(rounded(0xB20A1A27, Color.rgb(76, 148, 200), 22f));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2);
        hp.gravity = Gravity.TOP | Gravity.LEFT;
        hp.setMargins(20, 14, 0, 0);
        root.addView(hud, hp);

        message = text("Explore o campo e chegue perto de uma criatura!", 14f, Color.WHITE);
        message.setBackground(rounded(0xB20A1A27, Color.rgb(76, 148, 200), 22f));
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(-2, -2);
        mp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        mp.setMargins(0, 14, 0, 0);
        root.addView(message, mp);

        Button menu = makeButton("← MENU");
        menu.setTextSize(13f);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showMenu(); }
        });
        FrameLayout.LayoutParams menup = new FrameLayout.LayoutParams(-2, 58);
        menup.gravity = Gravity.TOP | Gravity.RIGHT;
        menup.setMargins(0, 12, 18, 0);
        root.addView(menu, menup);

        View joystick = new View(this);
        GradientDrawable joy = new GradientDrawable();
        joy.setShape(GradientDrawable.OVAL);
        joy.setColor(0x55395A70);
        joy.setStroke(4, 0xCCFFFFFF);
        joystick.setBackground(joy);
        FrameLayout.LayoutParams jp = new FrameLayout.LayoutParams(190, 190);
        jp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        jp.setMargins(28, 0, 0, 28);
        root.addView(joystick, jp);

        joystick.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (gameView == null) return true;
                if (event.getActionMasked() == MotionEvent.ACTION_UP ||
                        event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    gameView.setMove(0f, 0f);
                    return true;
                }
                float cx = v.getWidth() / 2f;
                float cy = v.getHeight() / 2f;
                float dx = (event.getX() - cx) / Math.max(1f, cx);
                float dz = (event.getY() - cy) / Math.max(1f, cy);
                float len = (float)Math.sqrt(dx * dx + dz * dz);
                if (len > 1f) {
                    dx /= len;
                    dz /= len;
                }
                gameView.setMove(dx, dz);
                return true;
            }
        });

        Button capture = makeButton("CAPTURAR");
        GradientDrawable cap = new GradientDrawable();
        cap.setShape(GradientDrawable.OVAL);
        cap.setColor(0xE5E94D3D);
        cap.setStroke(4, 0xFFFFFFFF);
        capture.setBackground(cap);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (gameView != null) gameView.capture();
            }
        });
        FrameLayout.LayoutParams capP = new FrameLayout.LayoutParams(170, 170);
        capP.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        capP.setMargins(0, 0, 34, 38);
        root.addView(capture, capP);

        TextView hint = text("Analógico: mover  •  Chegue perto e toque CAPTURAR", 12f,
                0xEEFFFFFF);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-2, -2);
        ip.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        ip.setMargins(0, 0, 0, 8);
        root.addView(hint, ip);

        setContentView(root);
        gameView.start();
    }

    private void updateHud(int caught, String msg) {
        if (hud != null) hud.setText("Capturados: " + caught + "/8");
        if (message != null && msg != null) message.setText(msg);
    }

    @Override protected void onResume() {
        super.onResume();
        immersive();
        if (gameView != null) gameView.start();
    }

    @Override protected void onPause() {
        if (gameView != null) gameView.stop();
        super.onPause();
    }

    private final class GameView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<Creature> creatures = new ArrayList<Creature>();
        private final List<Tree> trees = new ArrayList<Tree>();
        private float playerX = 0f, playerZ = 0f;
        private float moveX, moveZ;
        private int caught;
        private long last;
        private boolean running;
        private float time;

        GameView() {
            super(MainActivity.this);
            setBackgroundColor(Color.rgb(82, 165, 228));
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2f);
            seed();
        }

        private void seed() {
            creatures.add(new Creature("Voltik", -6, -5, 0xFFFFD34E));
            creatures.add(new Creature("Mossy", 7, -6, 0xFF55C66E));
            creatures.add(new Creature("Bubloo", -10, 7, 0xFF58AFFF));
            creatures.add(new Creature("Emberu", 10, 8, 0xFFFF6748));
            creatures.add(new Creature("Pebbit", -3, -12, 0xFFAAA59B));
            creatures.add(new Creature("Lumii", 4, 12, 0xFFE88AF7));
            creatures.add(new Creature("Spriggo", 13, -10, 0xFF5CE79E));
            creatures.add(new Creature("Nimbu", -13, -10, 0xFFE4F0FF));
            int[][] ts = {{-15,-14},{-9,-15},{-3,-14},{5,-15},{12,-14},{15,-8},
                    {-15,13},{-10,15},{-4,14},{5,15},{11,13},{15,8},{-15,0},{15,0}};
            for (int[] t : ts) trees.add(new Tree(t[0], t[1]));
        }

        void setMove(float x, float z) { moveX = x; moveZ = z; }
        void start() {
            if (running) return;
            running = true;
            last = System.nanoTime();
            invalidate();
        }
        void stop() { running = false; }

        void capture() {
            Creature best = null;
            float bestD = 999f;
            for (Creature c : creatures) {
                float dx = c.x - playerX, dz = c.z - playerZ;
                float d = (float)Math.sqrt(dx * dx + dz * dz);
                if (d < bestD) { bestD = d; best = c; }
            }
            if (best == null) {
                updateHud(caught, "Coleção completa! ✨");
                return;
            }
            if (bestD <= 3.8f) {
                creatures.remove(best);
                caught++;
                updateHud(caught, best.name + " capturado! ⭐");
                if (caught == 8) updateHud(caught, "Você capturou todas as 8 criaturas! 🏆");
            } else {
                updateHud(caught, "Chegue mais perto para capturar");
            }
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            long now = System.nanoTime();
            float dt = last == 0 ? 0f : Math.min(0.04f, (now - last) / 1_000_000_000f);
            last = now;
            if (running) {
                playerX = clamp(playerX + moveX * 5.8f * dt, -17f, 17f);
                playerZ = clamp(playerZ + moveZ * 5.8f * dt, -17f, 17f);
                time += dt;
            }

            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;

            p.setShader(new LinearGradient(0, 0, 0, h * .48f,
                    Color.rgb(78, 168, 235), Color.rgb(191, 226, 247), Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h * .48f, p);
            p.setShader(null);

            p.setColor(Color.rgb(77, 174, 83));
            Path ground = new Path();
            ground.moveTo(0, h * .35f);
            ground.lineTo(w, h * .35f);
            ground.lineTo(w, h);
            ground.lineTo(0, h);
            ground.close();
            c.drawPath(ground, p);

            drawGrid(c, w, h);
            drawRoad(c, w, h);

            List<DrawableThing> things = new ArrayList<DrawableThing>();
            for (Tree t : trees) things.add(new DrawableThing(t.x, t.z, 0, null, t));
            for (Creature cr : creatures) things.add(new DrawableThing(cr.x, cr.z, 1, cr, null));
            Collections.sort(things, new Comparator<DrawableThing>() {
                @Override public int compare(DrawableThing a, DrawableThing b) {
                    return Float.compare(a.x + a.z, b.x + b.z);
                }
            });
            for (DrawableThing d : things) {
                if (d.kind == 0) drawTree(c, w, h, d.tree);
                else drawCreature(c, w, h, d.creature);
            }
            drawPlayer(c, w, h);

            if (running) postInvalidateOnAnimation();
        }

        private void drawGrid(Canvas c, int w, int h) {
            stroke.setColor(0x334C8B50);
            stroke.setStrokeWidth(2f);
            for (int i = -18; i <= 18; i += 3) {
                Point a = project(i, -18, w, h);
                Point b = project(i, 18, w, h);
                c.drawLine(a.x, a.y, b.x, b.y, stroke);
                a = project(-18, i, w, h);
                b = project(18, i, w, h);
                c.drawLine(a.x, a.y, b.x, b.y, stroke);
            }
        }

        private void drawRoad(Canvas c, int w, int h) {
            Point a = project(-2f, -18f, w, h);
            Point b = project(2f, -18f, w, h);
            Point d = project(-2f, 18f, w, h);
            Point e = project(2f, 18f, w, h);
            Path road = new Path();
            road.moveTo(a.x, a.y); road.lineTo(b.x, b.y); road.lineTo(e.x, e.y);
            road.lineTo(d.x, d.y); road.close();
            p.setColor(Color.rgb(201, 184, 133));
            c.drawPath(road, p);
        }

        private Point project(float wx, float wz, int w, int h) {
            float rx = wx - playerX;
            float rz = wz - playerZ;
            float tileW = Math.max(23f, w / 34f);
            float tileH = tileW * .48f;
            float sx = w * .50f + (rx - rz) * tileW * .5f;
            float sy = h * .60f + (rx + rz) * tileH * .5f;
            return new Point(sx, sy);
        }

        private float depthScale(float wx, float wz) {
            float d = (wx - playerX + wz - playerZ) * .025f;
            return clamp(1f + d, .55f, 1.35f);
        }

        private void drawTree(Canvas c, int w, int h, Tree t) {
            Point q = project(t.x, t.z, w, h);
            if (q.x < -100 || q.x > w + 100 || q.y < h * .30f - 160 || q.y > h + 140) return;
            float s = depthScale(t.x, t.z);
            p.setColor(Color.rgb(111, 73, 40));
            c.drawRoundRect(new RectF(q.x - 8*s, q.y - 48*s, q.x + 8*s, q.y + 4*s),
                    5*s, 5*s, p);
            p.setColor(Color.rgb(35, 126, 55));
            c.drawCircle(q.x, q.y - 66*s, 32*s, p);
            p.setColor(Color.rgb(53, 155, 69));
            c.drawCircle(q.x - 18*s, q.y - 58*s, 22*s, p);
            c.drawCircle(q.x + 18*s, q.y - 58*s, 22*s, p);
        }

        private void drawCreature(Canvas c, int w, int h, Creature cr) {
            Point q = project(cr.x, cr.z, w, h);
            if (q.x < -120 || q.x > w + 120 || q.y < h * .28f - 180 || q.y > h + 160) return;
            float s = depthScale(cr.x, cr.z);
            float bounce = (float)Math.sin(time * 3f + cr.x) * 4f * s;
            float y = q.y + bounce;
            p.setColor(0x33000000);
            c.drawOval(new RectF(q.x - 30*s, q.y - 4*s, q.x + 30*s, q.y + 10*s), p);
            p.setColor(cr.color);
            c.drawOval(new RectF(q.x - 30*s, y - 52*s, q.x + 30*s, y + 2*s), p);
            c.drawCircle(q.x, y - 64*s, 25*s, p);
            Path ear = new Path();
            ear.moveTo(q.x - 18*s, y - 80*s); ear.lineTo(q.x - 32*s, y - 110*s);
            ear.lineTo(q.x - 7*s, y - 90*s); ear.close(); c.drawPath(ear, p);
            ear.reset(); ear.moveTo(q.x + 18*s, y - 80*s); ear.lineTo(q.x + 32*s, y - 110*s);
            ear.lineTo(q.x + 7*s, y - 90*s); ear.close(); c.drawPath(ear, p);
            p.setColor(Color.rgb(25, 31, 40));
            c.drawCircle(q.x - 8*s, y - 68*s, 3.5f*s, p);
            c.drawCircle(q.x + 8*s, y - 68*s, 3.5f*s, p);
            stroke.setColor(0xAAFFFFFF); stroke.setStrokeWidth(2f*s);
            c.drawCircle(q.x, y - 64*s, 25*s, stroke);
        }

        private void drawPlayer(Canvas c, int w, int h) {
            float x = w * .50f, y = h * .60f;
            float bob = (Math.abs(moveX) + Math.abs(moveZ) > .12f)
                    ? (float)Math.sin(time * 10f) * 3f : 0f;
            p.setColor(0x44000000);
            c.drawOval(new RectF(x - 28, y - 3, x + 28, y + 10), p);
            p.setColor(Color.rgb(38, 84, 150));
            c.drawRoundRect(new RectF(x - 22, y - 67 + bob, x + 22, y - 12 + bob), 12, 12, p);
            p.setColor(Color.rgb(242, 190, 151));
            c.drawCircle(x, y - 82 + bob, 18, p);
            p.setColor(Color.rgb(44, 35, 31));
            c.drawArc(new RectF(x - 20, y - 102 + bob, x + 20, y - 66 + bob), 180, 180, true, p);
            p.setColor(Color.rgb(53, 50, 46));
            c.drawRoundRect(new RectF(x - 17, y - 13 + bob, x - 4, y + 16 + bob), 5, 5, p);
            c.drawRoundRect(new RectF(x + 4, y - 13 + bob, x + 17, y + 16 + bob), 5, 5, p);
        }

        private float clamp(float v, float lo, float hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        private final class Creature {
            final String name; final float x, z; final int color;
            Creature(String n, float x, float z, int c) { name=n; this.x=x; this.z=z; color=c; }
        }
        private final class Tree {
            final float x, z; Tree(float x, float z) { this.x=x; this.z=z; }
        }
        private final class DrawableThing {
            final float x, z; final int kind; final Creature creature; final Tree tree;
            DrawableThing(float x, float z, int kind, Creature c, Tree t) {
                this.x=x; this.z=z; this.kind=kind; creature=c; tree=t;
            }
        }
        private final class Point {
            final float x, y; Point(float x, float y) { this.x=x; this.y=y; }
        }
    }
}