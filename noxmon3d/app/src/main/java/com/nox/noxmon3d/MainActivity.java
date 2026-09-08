package com.nox.noxmon3d;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;

public class MainActivity extends Activity implements GameRenderer.Listener {
    private GameView gameView;
    private TextView hud;
    private TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        FrameLayout root = new FrameLayout(this);
        gameView = new GameView(this);
        root.addView(gameView, new FrameLayout.LayoutParams(-1, -1));

        hud = new TextView(this);
        hud.setText("NOXMON 3D   •   Capturados: 0/5");
        hud.setTextColor(Color.WHITE);
        hud.setTextSize(18);
        hud.setShadowLayer(5, 0, 2, Color.BLACK);
        hud.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable hudBg = new GradientDrawable();
        hudBg.setColor(0x99000000);
        hudBg.setCornerRadius(dp(14));
        hud.setBackground(hudBg);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2);
        hp.gravity = Gravity.TOP | Gravity.LEFT;
        hp.setMargins(dp(14), dp(12), 0, 0);
        root.addView(hud, hp);

        message = new TextView(this);
        message.setText("Explore o mapa e aproxime-se de uma criatura.");
        message.setTextColor(Color.WHITE);
        message.setTextSize(15);
        message.setGravity(Gravity.CENTER);
        message.setShadowLayer(4, 0, 2, Color.BLACK);
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(dp(420), -2);
        mp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        mp.setMargins(0, dp(14), 0, 0);
        root.addView(message, mp);

        addMoveButton(root, "▲", dp(82), dp(126), 0, -1);
        addMoveButton(root, "▼", dp(82), dp(42), 0, 1);
        addMoveButton(root, "◀", dp(18), dp(42), -1, 0);
        addMoveButton(root, "▶", dp(146), dp(42), 1, 0);

        Button capture = makeButton("CAPTURAR");
        capture.setTextSize(17);
        capture.setOnClickListener(v -> gameView.renderer.tryCapture());
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(150), dp(66));
        cp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        cp.setMargins(0, 0, dp(24), dp(26));
        root.addView(capture, cp);

        TextView hint = new TextView(this);
        hint.setText("Use as setas para andar");
        hint.setTextColor(0xDDFFFFFF);
        hint.setTextSize(12);
        FrameLayout.LayoutParams hintp = new FrameLayout.LayoutParams(-2, -2);
        hintp.gravity = Gravity.LEFT | Gravity.BOTTOM;
        hintp.setMargins(dp(24), 0, 0, dp(10));
        root.addView(hint, hintp);

        setContentView(root);
    }

    private void addMoveButton(FrameLayout root, String text, int left, int bottom, float x, float z) {
        Button b = makeButton(text);
        b.setTextSize(25);
        b.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                gameView.renderer.setMove(x, z);
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                gameView.renderer.setMove(0, 0);
                return true;
            }
            return true;
        });
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(58), dp(58));
        p.gravity = Gravity.LEFT | Gravity.BOTTOM;
        p.setMargins(left, 0, 0, bottom);
        root.addView(b, p);
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC302E63);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(2), 0xAA8D8BFF);
        b.setBackground(bg);
        return b;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCapture(String species, int total) {
        runOnUiThread(() -> {
            hud.setText(String.format(Locale.US, "NOXMON 3D   •   Capturados: %d/5", total));
            message.setText(species + " capturado! ✨");
        });
    }

    @Override
    public void onMessage(String text) {
        runOnUiThread(() -> message.setText(text));
    }

    @Override protected void onPause() { super.onPause(); gameView.onPause(); }
    @Override protected void onResume() { super.onResume(); gameView.onResume(); }
}

class GameView extends GLSurfaceView {
    final GameRenderer renderer;
    GameView(MainActivity activity) {
        super(activity);
        setEGLContextClientVersion(2);
        renderer = new GameRenderer(activity);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}

class GameRenderer implements GLSurfaceView.Renderer {
    interface Listener { void onCapture(String species, int total); void onMessage(String text); }
    private final Listener listener;
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] mv = new float[16];
    private final float[] mvp = new float[16];
    private int program;
    private int aPos, uMvp, uColor;
    private FloatBuffer cubeBuffer;
    private FloatBuffer planeBuffer;
    private volatile float moveX = 0f, moveZ = 0f;
    private volatile float playerX = 0f, playerZ = 6f;
    private long lastNanos;
    private String lastHint = "";

    private final String[] species = {"Voltix", "Flamoo", "Aquari", "Leafin", "Cryston"};
    private final float[][] creatures = {
            {-4f, -3f, 0f}, {5f, -6f, 1f}, {0f, -11f, 2f}, {-7f, -14f, 3f}, {7f, -16f, 4f}
    };
    private final boolean[] captured = new boolean[5];

    private static final float[] CUBE = {
        -1,-1, 1,  1,-1, 1,  1, 1, 1,  -1,-1, 1,  1, 1, 1, -1, 1, 1,
         1,-1,-1, -1,-1,-1, -1, 1,-1,   1,-1,-1, -1, 1,-1,  1, 1,-1,
        -1,-1,-1, -1,-1, 1, -1, 1, 1,  -1,-1,-1, -1, 1, 1, -1, 1,-1,
         1,-1, 1,  1,-1,-1,  1, 1,-1,   1,-1, 1,  1, 1,-1,  1, 1, 1,
        -1, 1, 1,  1, 1, 1,  1, 1,-1,  -1, 1, 1,  1, 1,-1, -1, 1,-1,
        -1,-1,-1,  1,-1,-1,  1,-1, 1,  -1,-1,-1,  1,-1, 1, -1,-1, 1
    };
    private static final float[] PLANE = {
        -1,0,-1, 1,0,-1, 1,0,1, -1,0,-1, 1,0,1, -1,0,1
    };

    GameRenderer(Listener listener) { this.listener = listener; }
    void setMove(float x, float z) { moveX = x; moveZ = z; }

    void tryCapture() {
        int nearest = -1;
        float best = 999f;
        for (int i = 0; i < creatures.length; i++) {
            if (captured[i]) continue;
            float dx = playerX - creatures[i][0];
            float dz = playerZ - creatures[i][1];
            float d = (float)Math.sqrt(dx*dx + dz*dz);
            if (d < best) { best = d; nearest = i; }
        }
        if (nearest >= 0 && best < 2.6f) {
            captured[nearest] = true;
            int count = 0;
            for (boolean c : captured) if (c) count++;
            listener.onCapture(species[nearest], count);
            if (count == captured.length) listener.onMessage("Você capturou todas as criaturas! 🏆");
        } else {
            listener.onMessage("Chegue mais perto de uma criatura para capturar.");
        }
    }

    @Override public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
        GLES20.glClearColor(0.38f, 0.72f, 0.95f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        String vs = "attribute vec3 aPosition; uniform mat4 uMVP; void main(){ gl_Position=uMVP*vec4(aPosition,1.0); }";
        String fs = "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }";
        program = createProgram(vs, fs);
        aPos = GLES20.glGetAttribLocation(program, "aPosition");
        uMvp = GLES20.glGetUniformLocation(program, "uMVP");
        uColor = GLES20.glGetUniformLocation(program, "uColor");
        cubeBuffer = makeBuffer(CUBE);
        planeBuffer = makeBuffer(PLANE);
        lastNanos = System.nanoTime();
    }

    @Override public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = width / (float)Math.max(1, height);
        Matrix.perspectiveM(projection, 0, 58f, ratio, 0.1f, 80f);
    }

    @Override public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        float len = (float)Math.sqrt(moveX*moveX + moveZ*moveZ);
        if (len > 0.01f) {
            playerX += (moveX/len) * 5.2f * dt;
            playerZ += (moveZ/len) * 5.2f * dt;
            playerX = clamp(playerX, -13.5f, 13.5f);
            playerZ = clamp(playerZ, -18f, 8f);
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);
        Matrix.setLookAtM(view, 0, playerX, 6.2f, playerZ + 8.2f, playerX, 0.8f, playerZ - 4.3f, 0, 1, 0);

        drawPlane(0, -0.03f, -6, 18, 1, 22, .24f, .68f, .28f, 1f);
        drawCube(0, 0.02f, -7, 3.0f, 0.04f, 21f, .78f, .68f, .42f, 1f);

        float[][] trees = {{-10,-1},{10,-3},{-11,-9},{11,-10},{-9,-16},{9,-17},{-4,-18},{5,2},{-6,3}};
        for (float[] t : trees) drawTree(t[0], t[1]);

        drawPlayer(playerX, playerZ);
        for (int i = 0; i < creatures.length; i++) if (!captured[i]) drawCreature(i, creatures[i][0], creatures[i][1]);

        int near = nearestCreature();
        String hint = near >= 0 ? "Perto: " + species[near] + " — toque CAPTURAR" : "Explore o mapa e procure criaturas.";
        if (!hint.equals(lastHint)) { lastHint = hint; listener.onMessage(hint); }
    }

    private int nearestCreature() {
        int near = -1; float best = 999f;
        for (int i = 0; i < creatures.length; i++) {
            if (captured[i]) continue;
            float dx = playerX-creatures[i][0], dz = playerZ-creatures[i][1];
            float d = (float)Math.sqrt(dx*dx+dz*dz);
            if (d < best) {best=d; near=i;}
        }
        return best < 3.2f ? near : -1;
    }

    private void drawPlayer(float x, float z) {
        drawCube(x, .9f, z, .55f, .9f, .38f, .12f,.25f,.85f,1);
        drawCube(x, 1.95f, z, .38f, .38f, .38f, .96f,.78f,.62f,1);
        drawCube(x, 2.32f, z, .48f, .12f, .48f, .12f,.12f,.18f,1);
        drawCube(x-.2f, .08f, z, .18f, .28f, .22f, .1f,.1f,.12f,1);
        drawCube(x+.2f, .08f, z, .18f, .28f, .22f, .1f,.1f,.12f,1);
    }

    private void drawTree(float x, float z) {
        drawCube(x, 1.0f, z, .34f, 1.1f, .34f, .42f,.25f,.12f,1);
        drawCube(x, 2.55f, z, 1.05f, .85f, 1.05f, .08f,.55f,.18f,1);
        drawCube(x-.55f, 2.75f, z+.25f, .7f,.55f,.7f, .10f,.68f,.22f,1);
    }

    private void drawCreature(int type, float x, float z) {
        float[][] colors = {{.98f,.88f,.10f},{.96f,.25f,.10f},{.10f,.60f,.96f},{.18f,.80f,.32f},{.63f,.42f,.92f}};
        float[] c = colors[type];
        float bob = (float)Math.sin((System.nanoTime()/1e9) * 2 + type) * .08f;
        drawCube(x, .70f+bob, z, .72f,.58f,.62f, c[0],c[1],c[2],1);
        drawCube(x, 1.45f+bob, z-.05f, .53f,.48f,.50f, c[0],c[1],c[2],1);
        drawCube(x-.35f, 1.98f+bob, z, .16f,.38f,.16f, c[0],c[1],c[2],1);
        drawCube(x+.35f, 1.98f+bob, z, .16f,.38f,.16f, c[0],c[1],c[2],1);
        drawCube(x-.20f, 1.55f+bob, z-.52f, .08f,.08f,.05f, .05f,.05f,.08f,1);
        drawCube(x+.20f, 1.55f+bob, z-.52f, .08f,.08f,.05f, .05f,.05f,.08f,1);
    }

    private void drawPlane(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b,float a) {
        Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.scaleM(model,0,sx,sy,sz); draw(planeBuffer,6,r,g,b,a);
    }
    private void drawCube(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b,float a) {
        Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.scaleM(model,0,sx,sy,sz); draw(cubeBuffer,36,r,g,b,a);
    }
    private void draw(FloatBuffer buffer,int count,float r,float g,float b,float a) {
        Matrix.multiplyMM(mv,0,view,0,model,0); Matrix.multiplyMM(mvp,0,projection,0,mv,0);
        GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0); GLES20.glUniform4f(uColor,r,g,b,a);
        buffer.position(0); GLES20.glEnableVertexAttribArray(aPos); GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,12,buffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count); GLES20.glDisableVertexAttribArray(aPos);
    }
    private FloatBuffer makeBuffer(float[] data) {
        FloatBuffer b = ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer(); b.put(data).position(0); return b;
    }
    private int createProgram(String vs, String fs) {
        int v=compile(GLES20.GL_VERTEX_SHADER,vs), f=compile(GLES20.GL_FRAGMENT_SHADER,fs); int p=GLES20.glCreateProgram(); GLES20.glAttachShader(p,v); GLES20.glAttachShader(p,f); GLES20.glLinkProgram(p); return p;
    }
    private int compile(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s;}
    private float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
}
