package com.nox.monsters3d;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

public class MainActivity extends Activity {
    private GLSurfaceView glView;
    private GameRenderer renderer;
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
        if (glView != null) {
            glView.onPause();
            glView = null;
            renderer = null;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(5, 15, 28));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(44, 34, 44, 34);
        card.setBackground(rounded(Color.rgb(11, 35, 61), Color.rgb(72, 169, 230), 44f));

        TextView icon = text("◆", 66f, Color.rgb(255, 214, 72));
        TextView title = text("NOX MONSTERS 3D", 32f, Color.WHITE);
        TextView sub = text("Mundo 3D real • câmera em perspectiva • 8 criaturas", 15f,
                Color.rgb(195, 220, 239));
        Button play = makeButton("JOGAR EM 3D");
        play.setOnClickListener(v -> startGame());

        card.addView(icon, new LinearLayout.LayoutParams(-1, -2));
        card.addView(title, new LinearLayout.LayoutParams(-1, -2));
        card.addView(sub, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, 24, 0, 0);
        card.addView(play, bp);

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.66f), -2);
        cp.gravity = Gravity.CENTER;
        root.addView(card, cp);
        setContentView(root);
    }

    private void startGame() {
        FrameLayout root = new FrameLayout(this);

        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setPreserveEGLContextOnPause(true);
        renderer = new GameRenderer();
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(glView, new FrameLayout.LayoutParams(-1, -1));

        hud = text("Capturados: 0/8", 15f, Color.WHITE);
        hud.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        hud.setBackground(rounded(0xB20A1A27, Color.rgb(76, 148, 200), 22f));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2);
        hp.gravity = Gravity.TOP | Gravity.LEFT;
        hp.setMargins(20, 14, 0, 0);
        root.addView(hud, hp);

        message = text("Explore o mundo 3D e encontre as criaturas!", 14f, Color.WHITE);
        message.setBackground(rounded(0xB20A1A27, Color.rgb(76, 148, 200), 22f));
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(-2, -2);
        mp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        mp.setMargins(0, 14, 0, 0);
        root.addView(message, mp);

        Button menu = makeButton("← MENU");
        menu.setTextSize(13f);
        menu.setOnClickListener(v -> showMenu());
        FrameLayout.LayoutParams menup = new FrameLayout.LayoutParams(-2, 58);
        menup.gravity = Gravity.TOP | Gravity.RIGHT;
        menup.setMargins(0, 12, 18, 0);
        root.addView(menu, menup);

        JoystickView joystick = new JoystickView();
        FrameLayout.LayoutParams jp = new FrameLayout.LayoutParams(210, 210);
        jp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        jp.setMargins(28, 0, 0, 28);
        root.addView(joystick, jp);

        Button capture = makeButton("CAPTURAR");
        GradientDrawable cap = new GradientDrawable();
        cap.setShape(GradientDrawable.OVAL);
        cap.setColor(0xE5E94D3D);
        cap.setStroke(4, 0xFFFFFFFF);
        capture.setBackground(cap);
        capture.setOnClickListener(v -> {
            if (glView != null && renderer != null) {
                glView.queueEvent(() -> renderer.capture());
            }
        });
        FrameLayout.LayoutParams capP = new FrameLayout.LayoutParams(174, 174);
        capP.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        capP.setMargins(0, 0, 34, 38);
        root.addView(capture, capP);

        TextView hint = text("Analógico: mover • aproxime-se e toque CAPTURAR", 12f, 0xEEFFFFFF);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-2, -2);
        ip.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        ip.setMargins(0, 0, 0, 8);
        root.addView(hint, ip);

        setContentView(root);
        glView.onResume();
    }

    private void updateHud(int caught, String msg) {
        runOnUiThread(() -> {
            if (hud != null) hud.setText("Capturados: " + caught + "/8");
            if (message != null && msg != null) message.setText(msg);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        immersive();
        if (glView != null) glView.onResume();
    }

    @Override
    protected void onPause() {
        if (glView != null) glView.onPause();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (glView != null) showMenu();
        else super.onBackPressed();
    }

    private final class JoystickView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float knobX = 105f, knobY = 105f;

        JoystickView() {
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - 8f;
            p.setColor(0x663A647F);
            c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4f);
            p.setColor(0xDDFFFFFF);
            c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xCCFFFFFF);
            c.drawCircle(knobX, knobY, r * 0.34f, p);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - 12f;
            if (e.getActionMasked() == MotionEvent.ACTION_UP ||
                    e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                knobX = cx;
                knobY = cy;
                if (renderer != null) renderer.setMove(0f, 0f);
                invalidate();
                return true;
            }
            float dx = e.getX() - cx;
            float dy = e.getY() - cy;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > r) {
                dx = dx / len * r;
                dy = dy / len * r;
            }
            knobX = cx + dx;
            knobY = cy + dy;
            if (renderer != null) renderer.setMove(dx / r, dy / r);
            invalidate();
            return true;
        }
    }

    private final class GameRenderer implements GLSurfaceView.Renderer {
        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mv = new float[16];
        private final float[] mvp = new float[16];
        private final List<Creature> creatures = new ArrayList<>();
        private final List<Tree> trees = new ArrayList<>();
        private FloatBuffer cubeBuffer;
        private int program;
        private int aPosition, aNormal, uMvp, uModel, uColor, uLight;
        private volatile float moveX, moveZ;
        private float playerX, playerZ, playerYaw;
        private long lastNs;
        private float time;
        private int caught;

        private final float[] cube = {
                // front
                -0.5f,-0.5f, 0.5f, 0,0,1,   0.5f,-0.5f, 0.5f, 0,0,1,   0.5f, 0.5f, 0.5f, 0,0,1,
                -0.5f,-0.5f, 0.5f, 0,0,1,   0.5f, 0.5f, 0.5f, 0,0,1,  -0.5f, 0.5f, 0.5f, 0,0,1,
                // back
                 0.5f,-0.5f,-0.5f, 0,0,-1, -0.5f,-0.5f,-0.5f, 0,0,-1, -0.5f, 0.5f,-0.5f, 0,0,-1,
                 0.5f,-0.5f,-0.5f, 0,0,-1, -0.5f, 0.5f,-0.5f, 0,0,-1,  0.5f, 0.5f,-0.5f, 0,0,-1,
                // left
                -0.5f,-0.5f,-0.5f,-1,0,0,  -0.5f,-0.5f, 0.5f,-1,0,0,  -0.5f, 0.5f, 0.5f,-1,0,0,
                -0.5f,-0.5f,-0.5f,-1,0,0,  -0.5f, 0.5f, 0.5f,-1,0,0,  -0.5f, 0.5f,-0.5f,-1,0,0,
                // right
                 0.5f,-0.5f, 0.5f,1,0,0,    0.5f,-0.5f,-0.5f,1,0,0,    0.5f, 0.5f,-0.5f,1,0,0,
                 0.5f,-0.5f, 0.5f,1,0,0,    0.5f, 0.5f,-0.5f,1,0,0,    0.5f, 0.5f, 0.5f,1,0,0,
                // top
                -0.5f, 0.5f, 0.5f,0,1,0,    0.5f, 0.5f, 0.5f,0,1,0,    0.5f, 0.5f,-0.5f,0,1,0,
                -0.5f, 0.5f, 0.5f,0,1,0,    0.5f, 0.5f,-0.5f,0,1,0,   -0.5f, 0.5f,-0.5f,0,1,0,
                // bottom
                -0.5f,-0.5f,-0.5f,0,-1,0,   0.5f,-0.5f,-0.5f,0,-1,0,   0.5f,-0.5f, 0.5f,0,-1,0,
                -0.5f,-0.5f,-0.5f,0,-1,0,   0.5f,-0.5f, 0.5f,0,-1,0,  -0.5f,-0.5f, 0.5f,0,-1,0
        };

        GameRenderer() {
            seedWorld();
        }

        void setMove(float x, float z) {
            moveX = x;
            moveZ = z;
        }

        private void seedWorld() {
            creatures.add(new Creature("Voltik", -7, -6, rgb(1.0f, .78f, .18f)));
            creatures.add(new Creature("Mossy", 7, -7, rgb(.18f, .78f, .30f)));
            creatures.add(new Creature("Bubloo", -10, 7, rgb(.16f, .55f, 1f)));
            creatures.add(new Creature("Emberu", 10, 8, rgb(1f, .22f, .12f)));
            creatures.add(new Creature("Pebbit", -4, -13, rgb(.58f, .56f, .51f)));
            creatures.add(new Creature("Lumii", 5, 13, rgb(.85f, .35f, 1f)));
            creatures.add(new Creature("Spriggo", 14, -10, rgb(.20f, .92f, .58f)));
            creatures.add(new Creature("Nimbu", -14, -10, rgb(.82f, .91f, 1f)));
            int[][] t = {
                    {-16,-15},{-11,-15},{-5,-16},{3,-16},{10,-15},{16,-12},{16,-4},
                    {-16,14},{-10,15},{-3,16},{5,15},{12,14},{16,7},{-16,5},{-16,-4},
                    {-12,2},{12,2},{-8,11},{9,11}
            };
            for (int[] q : t) trees.add(new Tree(q[0], q[1]));
        }

        @Override
        public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl,
                                     javax.microedition.khronos.egl.EGLConfig config) {
            GLES20.glClearColor(0.28f, 0.67f, 0.94f, 1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);

            cubeBuffer = ByteBuffer.allocateDirect(cube.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            cubeBuffer.put(cube).position(0);

            String vs =
                    "uniform mat4 uMVP;\n" +
                    "uniform mat4 uModel;\n" +
                    "attribute vec3 aPosition;\n" +
                    "attribute vec3 aNormal;\n" +
                    "varying vec3 vNormal;\n" +
                    "void main(){\n" +
                    "  gl_Position = uMVP * vec4(aPosition,1.0);\n" +
                    "  vNormal = normalize(mat3(uModel) * aNormal);\n" +
                    "}";
            String fs =
                    "precision mediump float;\n" +
                    "uniform vec4 uColor;\n" +
                    "uniform vec3 uLight;\n" +
                    "varying vec3 vNormal;\n" +
                    "void main(){\n" +
                    "  float d = max(dot(normalize(vNormal), normalize(uLight)), 0.0);\n" +
                    "  float light = 0.42 + d * 0.58;\n" +
                    "  gl_FragColor = vec4(uColor.rgb * light, uColor.a);\n" +
                    "}";

            program = link(vs, fs);
            aPosition = GLES20.glGetAttribLocation(program, "aPosition");
            aNormal = GLES20.glGetAttribLocation(program, "aNormal");
            uMvp = GLES20.glGetUniformLocation(program, "uMVP");
            uModel = GLES20.glGetUniformLocation(program, "uModel");
            uColor = GLES20.glGetUniformLocation(program, "uColor");
            uLight = GLES20.glGetUniformLocation(program, "uLight");
            lastNs = System.nanoTime();
        }

        @Override
        public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float ratio = width / (float) Math.max(1, height);
            Matrix.perspectiveM(projection, 0, 58f, ratio, 0.4f, 90f);
        }

        @Override
        public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
            long now = System.nanoTime();
            float dt = Math.min(0.04f, (now - lastNs) / 1_000_000_000f);
            lastNs = now;
            time += dt;

            float mx = moveX;
            float mz = moveZ;
            if (Math.abs(mx) + Math.abs(mz) > 0.03f) {
                playerX = clamp(playerX + mx * 6.2f * dt, -18f, 18f);
                playerZ = clamp(playerZ + mz * 6.2f * dt, -18f, 18f);
                playerYaw = (float) Math.toDegrees(Math.atan2(mx, mz));
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glUseProgram(program);

            Matrix.setLookAtM(view, 0,
                    playerX + 10.5f, 8.2f, playerZ + 12.5f,
                    playerX, 1.2f, playerZ,
                    0f, 1f, 0f);

            drawBox(0f, -0.35f, 0f, 40f, 0.6f, 40f, 0f, rgb(.22f, .62f, .25f));
            drawBox(0f, -0.01f, 0f, 4.0f, 0.08f, 39f, 0f, rgb(.67f, .55f, .33f));
            drawBox(-10f, 0.03f, 8f, 5f, 0.10f, 4f, 0f, rgb(.25f, .58f, .74f));

            for (Tree tree : trees) drawTree(tree);
            synchronized (creatures) {
                for (Creature c : creatures) drawCreature(c);
            }
            drawPlayer();
        }

        private void drawPlayer() {
            float y = 0f;
            float[] shirt = rgb(.08f, .17f, .23f);
            float[] skin = rgb(.91f, .70f, .50f);
            float[] pants = rgb(.08f, .25f, .55f);
            drawBox(playerX, y + 1.25f, playerZ, .85f, 1.10f, .48f, playerYaw, shirt);
            drawBox(playerX, y + 2.18f, playerZ, .70f, .70f, .65f, playerYaw, skin);
            drawBox(playerX - .27f, y + .48f, playerZ, .28f, .75f, .34f, playerYaw, pants);
            drawBox(playerX + .27f, y + .48f, playerZ, .28f, .75f, .34f, playerYaw, pants);
            drawBox(playerX - .58f, y + 1.28f, playerZ, .25f, .85f, .28f, playerYaw, skin);
            drawBox(playerX + .58f, y + 1.28f, playerZ, .25f, .85f, .28f, playerYaw, skin);
        }

        private void drawTree(Tree t) {
            drawBox(t.x, 1.15f, t.z, .75f, 2.3f, .75f, 0f, rgb(.38f, .22f, .10f));
            drawBox(t.x, 3.0f, t.z, 2.5f, 2.4f, 2.5f, 0f, rgb(.08f, .47f, .16f));
            drawBox(t.x + .65f, 3.65f, t.z - .35f, 1.35f, 1.2f, 1.35f, 25f, rgb(.12f, .58f, .20f));
        }

        private void drawCreature(Creature c) {
            float bob = (float) Math.sin(time * 2.3f + c.phase) * .12f;
            float y = .65f + bob;
            drawBox(c.x, y + .55f, c.z, 1.35f, 1.10f, 1.10f, 0f, c.color);
            drawBox(c.x, y + 1.42f, c.z - .05f, .95f, .88f, .88f, 0f, lighten(c.color, .14f));
            drawBox(c.x - .34f, y + 2.00f, c.z, .28f, .62f, .28f, -18f, c.color);
            drawBox(c.x + .34f, y + 2.00f, c.z, .28f, .62f, .28f, 18f, c.color);
            drawBox(c.x - .38f, y + .10f, c.z + .18f, .35f, .32f, .52f, 0f, darken(c.color, .12f));
            drawBox(c.x + .38f, y + .10f, c.z + .18f, .35f, .32f, .52f, 0f, darken(c.color, .12f));
            drawBox(c.x, y + 1.46f, c.z - .48f, .16f, .16f, .08f, 0f, rgb(.06f,.06f,.08f));
        }

        void capture() {
            Creature best = null;
            float bestD = Float.MAX_VALUE;
            synchronized (creatures) {
                for (Creature c : creatures) {
                    float dx = c.x - playerX;
                    float dz = c.z - playerZ;
                    float d = (float) Math.sqrt(dx * dx + dz * dz);
                    if (d < bestD) {
                        bestD = d;
                        best = c;
                    }
                }
                if (best == null) {
                    updateHud(caught, "Coleção completa! ✨");
                    return;
                }
                if (bestD <= 3.5f) {
                    creatures.remove(best);
                    caught++;
                    updateHud(caught, best.name + " capturado! ⭐");
                    if (caught == 8) updateHud(caught, "Você capturou todas as criaturas! 🏆");
                } else {
                    updateHud(caught, "Chegue mais perto para capturar");
                }
            }
        }

        private void drawBox(float x, float y, float z,
                             float sx, float sy, float sz,
                             float rotY, float[] color) {
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, x, y, z);
            if (rotY != 0f) Matrix.rotateM(model, 0, rotY, 0f, 1f, 0f);
            Matrix.scaleM(model, 0, sx, sy, sz);
            Matrix.multiplyMM(mv, 0, view, 0, model, 0);
            Matrix.multiplyMM(mvp, 0, projection, 0, mv, 0);

            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
            GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
            GLES20.glUniform4f(uColor, color[0], color[1], color[2], 1f);
            GLES20.glUniform3f(uLight, -0.35f, 1f, 0.45f);

            cubeBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aPosition);
            GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 24, cubeBuffer);
            cubeBuffer.position(3);
            GLES20.glEnableVertexAttribArray(aNormal);
            GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 24, cubeBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        }

        private int compile(int type, String source) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] ok = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, ok, 0);
            if (ok[0] == 0) {
                String error = GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);
                throw new RuntimeException("Shader: " + error);
            }
            return shader;
        }

        private int link(String vertex, String fragment) {
            int vs = compile(GLES20.GL_VERTEX_SHADER, vertex);
            int fs = compile(GLES20.GL_FRAGMENT_SHADER, fragment);
            int p = GLES20.glCreateProgram();
            GLES20.glAttachShader(p, vs);
            GLES20.glAttachShader(p, fs);
            GLES20.glLinkProgram(p);
            int[] ok = new int[1];
            GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
            GLES20.glDeleteShader(vs);
            GLES20.glDeleteShader(fs);
            if (ok[0] == 0) {
                String error = GLES20.glGetProgramInfoLog(p);
                GLES20.glDeleteProgram(p);
                throw new RuntimeException("Program: " + error);
            }
            return p;
        }
    }

    private static final class Creature {
        final String name;
        final float x, z;
        final float[] color;
        final float phase;

        Creature(String name, float x, float z, float[] color) {
            this.name = name;
            this.x = x;
            this.z = z;
            this.color = color;
            this.phase = (x * 0.37f + z * 0.19f);
        }
    }

    private static final class Tree {
        final float x, z;
        Tree(float x, float z) { this.x = x; this.z = z; }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float[] rgb(float r, float g, float b) {
        return new float[]{r, g, b};
    }

    private static float[] lighten(float[] c, float amount) {
        return new float[]{
                clamp(c[0] + amount, 0f, 1f),
                clamp(c[1] + amount, 0f, 1f),
                clamp(c[2] + amount, 0f, 1f)
        };
    }

    private static float[] darken(float[] c, float amount) {
        return new float[]{
                clamp(c[0] - amount, 0f, 1f),
                clamp(c[1] - amount, 0f, 1f),
                clamp(c[2] - amount, 0f, 1f)
        };
    }
}
