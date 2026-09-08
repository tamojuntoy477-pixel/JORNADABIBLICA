package com.nox.jornadabiblica;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity {
    private GameSurface gameSurface;
    private TextView hud;
    private TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
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

    private GradientDrawable rounded(int color, int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        g.setStroke(2, stroke);
        return g;
    }

    private TextView label(String text, int size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setPadding(18, 12, 18, 12);
        return t;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(18);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackground(rounded(Color.rgb(28, 55, 88), Color.rgb(90, 180, 255), 28));
        return b;
    }

    private void showMenu() {
        if (gameSurface != null) {
            gameSurface.onPause();
            gameSurface = null;
        }
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(7, 18, 32));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(40, 40, 40, 40);
        box.setBackground(rounded(Color.rgb(13, 35, 57), Color.rgb(68, 144, 210), 42));

        TextView icon = label("◈", 72, Color.rgb(255, 220, 82));
        TextView title = label("NOX MONSTERS 3D", 34, Color.WHITE);
        TextView sub = label("Explore um mundo 3D, encontre criaturas e capture todas.", 16, Color.rgb(185, 214, 239));
        Button play = button("JOGAR");
        play.setPadding(24, 16, 24, 16);
        play.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startGame(); }
        });

        box.addView(icon, new LinearLayout.LayoutParams(-1, -2));
        box.addView(title, new LinearLayout.LayoutParams(-1, -2));
        box.addView(sub, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 26, 0, 0);
        box.addView(play, p);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels * 0.72f), -2);
        lp.gravity = Gravity.CENTER;
        root.addView(box, lp);
        setContentView(root);
    }

    private void startGame() {
        FrameLayout root = new FrameLayout(this);
        gameSurface = new GameSurface(this);
        root.addView(gameSurface, new FrameLayout.LayoutParams(-1, -1));

        hud = label("Capturados: 0/8", 15, Color.WHITE);
        hud.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        hud.setBackground(rounded(0xAA071421, Color.rgb(78, 145, 198), 20));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2);
        hp.gravity = Gravity.TOP | Gravity.LEFT;
        hp.setMargins(24, 18, 0, 0);
        root.addView(hud, hp);

        Button exit = button("← MENU");
        exit.setTextSize(13);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showMenu(); }
        });
        FrameLayout.LayoutParams ep = new FrameLayout.LayoutParams(-2, 58);
        ep.gravity = Gravity.TOP | Gravity.RIGHT;
        ep.setMargins(0, 14, 20, 0);
        root.addView(exit, ep);

        message = label("Use as setas para explorar", 14, Color.WHITE);
        message.setBackground(rounded(0xB0102639, Color.rgb(71, 141, 197), 18));
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(-2, -2);
        mp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        mp.setMargins(0, 18, 0, 0);
        root.addView(message, mp);

        LinearLayout dpad = new LinearLayout(this);
        dpad.setOrientation(LinearLayout.VERTICAL);
        dpad.setGravity(Gravity.CENTER);
        Button up = control("▲");
        Button down = control("▼");
        Button left = control("◀");
        Button right = control("▶");
        bindMove(up, 0, -1);
        bindMove(down, 0, 1);
        bindMove(left, -1, 0);
        bindMove(right, 1, 0);
        dpad.addView(up, new LinearLayout.LayoutParams(80, 80));
        LinearLayout mid = new LinearLayout(this);
        mid.setGravity(Gravity.CENTER);
        mid.addView(left, new LinearLayout.LayoutParams(80, 80));
        TextView center = label("●", 20, Color.rgb(116, 174, 214));
        mid.addView(center, new LinearLayout.LayoutParams(68, 80));
        mid.addView(right, new LinearLayout.LayoutParams(80, 80));
        dpad.addView(mid);
        dpad.addView(down, new LinearLayout.LayoutParams(80, 80));
        FrameLayout.LayoutParams dp = new FrameLayout.LayoutParams(-2, -2);
        dp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        dp.setMargins(22, 0, 0, 20);
        root.addView(dpad, dp);

        Button capture = button("CAPTURAR");
        capture.setTextSize(16);
        capture.setBackground(rounded(0xDDC93E3E, Color.rgb(255, 208, 86), 100));
        capture.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (gameSurface != null) {
                    gameSurface.queueEvent(new Runnable() {
                        @Override public void run() { gameSurface.renderer.captureNearest(); }
                    });
                }
            }
        });
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(160, 160);
        cp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        cp.setMargins(0, 0, 34, 46);
        root.addView(capture, cp);

        TextView hint = label("Chegue perto de uma criatura e toque CAPTURAR", 12, Color.rgb(222, 236, 247));
        hint.setBackground(rounded(0x9A0B1A29, Color.rgb(56, 103, 139), 15));
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-2, -2);
        ip.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        ip.setMargins(0, 0, 0, 12);
        root.addView(hint, ip);

        setContentView(root);
        gameSurface.onResume();
    }

    private Button control(String text) {
        Button b = button(text);
        b.setTextSize(24);
        b.setPadding(0, 0, 0, 0);
        b.setBackground(rounded(0xB8234666, Color.rgb(99, 173, 228), 22));
        return b;
    }

    private void bindMove(Button b, final int x, final int z) {
        b.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (gameSurface == null) return false;
                if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
                    gameSurface.renderer.setMove(x, z);
                } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    gameSurface.renderer.setMove(0, 0);
                }
                return true;
            }
        });
    }

    void updateHud(final int caught, final String text) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (hud != null) hud.setText("Capturados: " + caught + "/8");
                if (message != null) message.setText(text);
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        immersive();
        if (gameSurface != null) gameSurface.onResume();
    }

    @Override protected void onPause() {
        if (gameSurface != null) gameSurface.onPause();
        super.onPause();
    }

    class GameSurface extends GLSurfaceView {
        final GameRenderer renderer;
        GameSurface(Context c) {
            super(c);
            setEGLContextClientVersion(2);
            renderer = new GameRenderer();
            setRenderer(renderer);
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }
    }

    class GameRenderer implements GLSurfaceView.Renderer {
        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mv = new float[16];
        private final float[] mvp = new float[16];
        private FloatBuffer cube;
        private int program;
        private int posHandle;
        private int mvpHandle;
        private int colorHandle;
        private long lastTime;
        private volatile float moveX = 0;
        private volatile float moveZ = 0;
        private float playerX = 0;
        private float playerZ = 8;
        private int caught = 0;
        private final List<Creature> creatures = new ArrayList<Creature>();

        final float[] vertices = new float[]{
                -0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f,  0.5f,0.5f,0.5f,
                -0.5f,-0.5f, 0.5f,  0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f,
                 0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f,
                 0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f,  0.5f,0.5f,-0.5f,
                -0.5f,-0.5f,-0.5f, -0.5f,-0.5f,0.5f, -0.5f,0.5f,0.5f,
                -0.5f,-0.5f,-0.5f, -0.5f,0.5f,0.5f, -0.5f,0.5f,-0.5f,
                 0.5f,-0.5f,0.5f,  0.5f,-0.5f,-0.5f,  0.5f,0.5f,-0.5f,
                 0.5f,-0.5f,0.5f,  0.5f,0.5f,-0.5f,  0.5f,0.5f,0.5f,
                -0.5f,0.5f,0.5f,   0.5f,0.5f,0.5f,   0.5f,0.5f,-0.5f,
                -0.5f,0.5f,0.5f,   0.5f,0.5f,-0.5f, -0.5f,0.5f,-0.5f,
                -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f,
                -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f, -0.5f,-0.5f,0.5f
        };

        GameRenderer() {
            creatures.add(new Creature(-7, 2, 1.00f, 0.83f, 0.18f));
            creatures.add(new Creature(6, 0, 0.35f, 0.85f, 0.42f));
            creatures.add(new Creature(-4, -7, 0.96f, 0.35f, 0.20f));
            creatures.add(new Creature(7, -8, 0.24f, 0.66f, 0.96f));
            creatures.add(new Creature(1, -3, 0.72f, 0.44f, 0.92f));
            creatures.add(new Creature(-10, -10, 0.78f, 0.68f, 0.48f));
            creatures.add(new Creature(10, 5, 0.98f, 0.55f, 0.68f));
            creatures.add(new Creature(0, -12, 0.28f, 0.92f, 0.82f));
        }

        void setMove(float x, float z) { moveX = x; moveZ = z; }

        @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0.38f, 0.72f, 0.95f, 1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            cube = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            cube.put(vertices).position(0);
            String vs = "uniform mat4 uMVP; attribute vec4 aPos; void main(){ gl_Position=uMVP*aPos; }";
            String fs = "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }";
            int v = shader(GLES20.GL_VERTEX_SHADER, vs);
            int f = shader(GLES20.GL_FRAGMENT_SHADER, fs);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, v);
            GLES20.glAttachShader(program, f);
            GLES20.glLinkProgram(program);
            posHandle = GLES20.glGetAttribLocation(program, "aPos");
            mvpHandle = GLES20.glGetUniformLocation(program, "uMVP");
            colorHandle = GLES20.glGetUniformLocation(program, "uColor");
            lastTime = System.nanoTime();
        }

        @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float ratio = (float) width / (float) height;
            Matrix.perspectiveM(projection, 0, 58f, ratio, 0.1f, 80f);
        }

        @Override public void onDrawFrame(GL10 gl) {
            long now = System.nanoTime();
            float dt = Math.min(0.04f, (now - lastTime) / 1000000000f);
            lastTime = now;
            float len = (float)Math.sqrt(moveX * moveX + moveZ * moveZ);
            if (len > 0.01f) {
                playerX += (moveX / len) * 5.0f * dt;
                playerZ += (moveZ / len) * 5.0f * dt;
                playerX = clamp(playerX, -13.5f, 13.5f);
                playerZ = clamp(playerZ, -13.5f, 13.5f);
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            Matrix.setLookAtM(view, 0, playerX, 6.5f, playerZ + 9f, playerX, 0.6f, playerZ - 2.5f, 0, 1, 0);
            GLES20.glUseProgram(program);
            cube.position(0);
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, cube);
            GLES20.glEnableVertexAttribArray(posHandle);

            drawCube(0, -0.45f, 0, 30, 0.5f, 30, 0.24f, 0.63f, 0.28f);
            drawCube(0, -0.16f, -3, 4.5f, 0.12f, 26, 0.75f, 0.68f, 0.47f);
            drawCube(-7.5f, -0.12f, 7, 13, 0.14f, 3.2f, 0.67f, 0.62f, 0.43f);

            drawTree(-12, 8); drawTree(-10, 2); drawTree(-12, -5); drawTree(-9, -12);
            drawTree(12, 9); drawTree(11, 3); drawTree(12, -3); drawTree(10, -11);
            drawTree(-5, 11); drawTree(5, 11); drawTree(-6, -13); drawTree(6, -13);

            drawCube(playerX, 0.55f, playerZ, 0.75f, 1.1f, 0.55f, 0.12f, 0.38f, 0.86f);
            drawCube(playerX, 1.32f, playerZ, 0.62f, 0.55f, 0.58f, 0.95f, 0.74f, 0.57f);
            drawCube(playerX, 1.60f, playerZ, 0.66f, 0.16f, 0.62f, 0.18f, 0.10f, 0.06f);

            float t = now / 1000000000f;
            synchronized (creatures) {
                for (Creature c : creatures) if (!c.caught) drawCreature(c, t);
            }
        }

        private void drawTree(float x, float z) {
            drawCube(x, 0.6f, z, 0.55f, 1.8f, 0.55f, 0.40f, 0.23f, 0.10f);
            drawCube(x, 2.0f, z, 2.1f, 1.9f, 2.1f, 0.12f, 0.48f, 0.16f);
            drawCube(x + 0.8f, 1.7f, z + 0.3f, 1.25f, 1.35f, 1.25f, 0.18f, 0.58f, 0.20f);
        }

        private void drawCreature(Creature c, float t) {
            float bob = (float)Math.sin(t * 3f + c.x) * 0.12f;
            drawCube(c.x, 0.62f + bob, c.z, 1.05f, 0.86f, 1.25f, c.r, c.g, c.b);
            drawCube(c.x, 1.26f + bob, c.z - 0.28f, 0.82f, 0.72f, 0.78f, c.r, c.g, c.b);
            drawCube(c.x - 0.26f, 1.88f + bob, c.z - 0.29f, 0.22f, 0.65f, 0.22f, c.r * .85f, c.g * .85f, c.b * .85f);
            drawCube(c.x + 0.26f, 1.88f + bob, c.z - 0.29f, 0.22f, 0.65f, 0.22f, c.r * .85f, c.g * .85f, c.b * .85f);
            drawCube(c.x - 0.18f, 1.34f + bob, c.z - 0.70f, 0.12f, 0.12f, 0.10f, 0.05f, 0.05f, 0.05f);
            drawCube(c.x + 0.18f, 1.34f + bob, c.z - 0.70f, 0.12f, 0.12f, 0.10f, 0.05f, 0.05f, 0.05f);
        }

        void captureNearest() {
            Creature nearest = null;
            float best = 999f;
            synchronized (creatures) {
                for (Creature c : creatures) {
                    if (c.caught) continue;
                    float dx = c.x - playerX;
                    float dz = c.z - playerZ;
                    float d = (float)Math.sqrt(dx * dx + dz * dz);
                    if (d < best) { best = d; nearest = c; }
                }
                if (nearest != null && best <= 2.7f) {
                    nearest.caught = true;
                    caught++;
                    if (caught >= 8) updateHud(caught, "Você capturou todas as criaturas! 🏆");
                    else updateHud(caught, "Criatura capturada! Continue explorando.");
                } else {
                    updateHud(caught, "Chegue mais perto de uma criatura.");
                }
            }
        }

        private int shader(int type, String code) {
            int s = GLES20.glCreateShader(type);
            GLES20.glShaderSource(s, code);
            GLES20.glCompileShader(s);
            return s;
        }

        private float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }

        private void drawCube(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, x, y, z);
            Matrix.scaleM(model, 0, sx, sy, sz);
            Matrix.multiplyMM(mv, 0, view, 0, model, 0);
            Matrix.multiplyMM(mvp, 0, projection, 0, mv, 0);
            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0);
            GLES20.glUniform4f(colorHandle, r, g, b, 1f);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        }
    }

    static class Creature {
        final float x, z, r, g, b;
        boolean caught = false;
        Creature(float x, float z, float r, float g, float b) {
            this.x = x; this.z = z; this.r = r; this.g = g; this.b = b;
        }
    }
}
