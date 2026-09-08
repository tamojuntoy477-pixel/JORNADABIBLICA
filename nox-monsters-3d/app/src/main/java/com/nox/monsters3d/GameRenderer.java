package com.nox.monsters3d;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameRenderer implements GLSurfaceView.Renderer {
    public interface HudCallback {
        void onHud(int captured, int total, String message);
    }

    private static class Creature {
        float x, z;
        float dirX, dirZ;
        float changeTimer;
        float phase;
        final String name;
        final float[] color;

        Creature(String name, float x, float z, float[] color, float phase) {
            this.name = name;
            this.x = x;
            this.z = z;
            this.color = color;
            this.phase = phase;
        }
    }

    private final Context context;
    private final HudCallback hudCallback;
    private final Random random = new Random(7);
    private final List<Creature> creatures = new ArrayList<>();

    private Cube cube;
    private int program;
    private int aPosition;
    private int uMvp;
    private int uColor;

    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] pv = new float[16];
    private final float[] mvp = new float[16];

    private volatile float moveX;
    private volatile float moveZ;
    private volatile boolean captureRequested;

    private float playerX = 0f;
    private float playerZ = 6f;
    private float playerFacing = 180f;
    private long lastNanos;
    private float worldTime;
    private int captured;
    private int total;
    private float messageTimer;
    private String currentMessage = "Explore o campo e chegue perto de uma criatura!";

    public GameRenderer(Context context, HudCallback callback) {
        this.context = context;
        this.hudCallback = callback;
        seedCreatures();
    }

    private void seedCreatures() {
        creatures.add(new Creature("Voltik", -7f, -4f, new float[]{1.00f, 0.80f, 0.18f, 1f}, 0.2f));
        creatures.add(new Creature("Mossy", 7f, -5f, new float[]{0.36f, 0.82f, 0.42f, 1f}, 1.0f));
        creatures.add(new Creature("Bubloo", -11f, 8f, new float[]{0.30f, 0.72f, 1.00f, 1f}, 2.0f));
        creatures.add(new Creature("Emberu", 11f, 7f, new float[]{1.00f, 0.38f, 0.20f, 1f}, 2.7f));
        creatures.add(new Creature("Pebbit", -3f, -12f, new float[]{0.70f, 0.68f, 0.63f, 1f}, 3.3f));
        creatures.add(new Creature("Lumii", 4f, 13f, new float[]{0.93f, 0.55f, 1.00f, 1f}, 4.0f));
        creatures.add(new Creature("Spriggo", 14f, -11f, new float[]{0.34f, 0.94f, 0.65f, 1f}, 4.8f));
        creatures.add(new Creature("Nimbu", -14f, -10f, new float[]{0.88f, 0.92f, 1.00f, 1f}, 5.7f));
        total = creatures.size();
    }

    public void setMove(float x, float z) {
        this.moveX = x;
        this.moveZ = z;
    }

    public void tryCapture() {
        captureRequested = true;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.42f, 0.75f, 0.96f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        uMvp = GLES20.glGetUniformLocation(program, "uMVP");
        uColor = GLES20.glGetUniformLocation(program, "uColor");
        cube = new Cube();
        lastNanos = System.nanoTime();
        hudCallback.onHud(captured, total, currentMessage);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / Math.max(1, height);
        Matrix.perspectiveM(projection, 0, 55f, ratio, 0.1f, 100f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        worldTime += dt;
        update(dt);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        float camX = playerX;
        float camZ = playerZ + 11f;
        Matrix.setLookAtM(view, 0,
                camX, 8.3f, camZ,
                playerX, 0.8f, playerZ - 2.4f,
                0f, 1f, 0f);
        Matrix.multiplyMM(pv, 0, projection, 0, view, 0);

        drawWorld();
        drawPlayer();
        drawCreatures();
    }

    private void update(float dt) {
        float mx = moveX;
        float mz = moveZ;
        float len = (float)Math.sqrt(mx * mx + mz * mz);
        if (len > 0.08f) {
            float speed = 6.2f;
            playerX += mx * speed * dt;
            playerZ += mz * speed * dt;
            playerX = clamp(playerX, -18f, 18f);
            playerZ = clamp(playerZ, -18f, 18f);
            playerFacing = (float)Math.toDegrees(Math.atan2(mx, mz));
        }

        for (Creature c : creatures) {
            c.changeTimer -= dt;
            if (c.changeTimer <= 0f) {
                float angle = random.nextFloat() * 6.28318f;
                c.dirX = (float)Math.cos(angle);
                c.dirZ = (float)Math.sin(angle);
                c.changeTimer = 1.5f + random.nextFloat() * 2.8f;
            }
            c.x += c.dirX * 0.65f * dt;
            c.z += c.dirZ * 0.65f * dt;
            if (Math.abs(c.x) > 17f) c.dirX *= -1f;
            if (Math.abs(c.z) > 17f) c.dirZ *= -1f;
            c.x = clamp(c.x, -17f, 17f);
            c.z = clamp(c.z, -17f, 17f);
        }

        if (captureRequested) {
            captureRequested = false;
            captureNearest();
        }

        if (messageTimer > 0f) {
            messageTimer -= dt;
            if (messageTimer <= 0f && captured < total) {
                currentMessage = "Explore o campo e chegue perto de uma criatura!";
                hudCallback.onHud(captured, total, currentMessage);
            }
        }
    }

    private void captureNearest() {
        Creature nearest = null;
        float best = Float.MAX_VALUE;
        for (Creature c : creatures) {
            float dx = c.x - playerX;
            float dz = c.z - playerZ;
            float d = (float)Math.sqrt(dx * dx + dz * dz);
            if (d < best) {
                best = d;
                nearest = c;
            }
        }

        if (nearest == null) {
            currentMessage = "Você capturou todas! ✨";
            messageTimer = 999f;
            hudCallback.onHud(captured, total, currentMessage);
            return;
        }

        if (best <= 3.8f) {
            creatures.remove(nearest);
            captured++;
            currentMessage = nearest.name + " capturado!  +1 estrela";
            messageTimer = 2.4f;
            if (captured == total) {
                currentMessage = "Coleção completa! Você capturou as " + total + " criaturas!";
                messageTimer = 999f;
            }
            hudCallback.onHud(captured, total, currentMessage);
        } else {
            currentMessage = "Chegue mais perto para capturar (" + String.format(java.util.Locale.US, "%.1f", best) + "m)";
            messageTimer = 1.8f;
            hudCallback.onHud(captured, total, currentMessage);
        }
    }

    private void drawWorld() {
        drawBox(0f, -0.55f, 0f, 40f, 1f, 40f, 0f,
                new float[]{0.25f, 0.68f, 0.31f, 1f});
        drawBox(0f, -0.02f, -1f, 3.2f, 0.08f, 38f, 0f,
                new float[]{0.74f, 0.68f, 0.52f, 1f});

        float[][] trees = {
                {-16f, -14f}, {-10f, -16f}, {-4f, -15f}, {5f, -16f}, {12f, -15f}, {17f, -11f},
                {-17f, 12f}, {-12f, 16f}, {-5f, 15f}, {6f, 17f}, {13f, 14f}, {17f, 9f},
                {-17f, -2f}, {17f, 0f}
        };
        for (float[] t : trees) {
            drawBox(t[0], 0.7f, t[1], 0.7f, 2.5f, 0.7f, 0f,
                    new float[]{0.42f, 0.25f, 0.12f, 1f});
            drawBox(t[0], 2.6f, t[1], 2.5f, 2.4f, 2.5f, 0f,
                    new float[]{0.12f, 0.50f, 0.22f, 1f});
        }

        for (int i = -15; i <= 15; i += 6) {
            drawBox(-7.5f, 0.12f, i, 1.2f, 0.28f, 1.2f, 0f,
                    new float[]{0.95f, 0.83f, 0.30f, 1f});
            drawBox(8.2f, 0.12f, i + 2f, 1.0f, 0.22f, 1.0f, 0f,
                    new float[]{0.90f, 0.40f, 0.52f, 1f});
        }
    }

    private void drawPlayer() {
        float bob = (Math.abs(moveX) + Math.abs(moveZ) > 0.1f)
                ? (float)Math.sin(worldTime * 10f) * 0.08f : 0f;
        drawBox(playerX, 0.9f + bob, playerZ, 0.9f, 1.3f, 0.7f, playerFacing,
                new float[]{0.12f, 0.24f, 0.44f, 1f});
        drawBox(playerX, 1.85f + bob, playerZ - 0.03f, 0.75f, 0.65f, 0.72f, playerFacing,
                new float[]{0.96f, 0.76f, 0.60f, 1f});
        drawBox(playerX, 2.20f + bob, playerZ - 0.05f, 0.82f, 0.16f, 0.82f, playerFacing,
                new float[]{0.12f, 0.10f, 0.10f, 1f});
    }

    private void drawCreatures() {
        for (Creature c : creatures) {
            float bounce = (float)Math.sin(worldTime * 3.4f + c.phase) * 0.12f;
            float yaw = (float)Math.toDegrees(Math.atan2(c.dirX, c.dirZ));
            drawBox(c.x, 0.55f + bounce, c.z, 1.15f, 0.85f, 1.15f, yaw, c.color);
            drawBox(c.x, 1.20f + bounce, c.z - 0.16f, 0.86f, 0.74f, 0.78f, yaw, c.color);
            drawBox(c.x - 0.34f, 1.70f + bounce, c.z - 0.18f, 0.22f, 0.62f, 0.22f, yaw - 10f, c.color);
            drawBox(c.x + 0.34f, 1.70f + bounce, c.z - 0.18f, 0.22f, 0.62f, 0.22f, yaw + 10f, c.color);
            drawBox(c.x - 0.22f, 1.29f + bounce, c.z - 0.58f, 0.12f, 0.12f, 0.08f, yaw,
                    new float[]{0.05f, 0.06f, 0.08f, 1f});
            drawBox(c.x + 0.22f, 1.29f + bounce, c.z - 0.58f, 0.12f, 0.12f, 0.08f, yaw,
                    new float[]{0.05f, 0.06f, 0.08f, 1f});
        }
    }

    private void drawBox(float x, float y, float z,
                         float sx, float sy, float sz,
                         float yaw, float[] color) {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, z);
        Matrix.rotateM(model, 0, yaw, 0f, 1f, 0f);
        Matrix.scaleM(model, 0, sx, sy, sz);
        Matrix.multiplyMM(mvp, 0, pv, 0, model, 0);
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
        GLES20.glUniform4fv(uColor, 1, color, 0);
        cube.draw(aPosition);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int createProgram(String vertex, String fragment) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertex);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragment);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, vs);
        GLES20.glAttachShader(p, fs);
        GLES20.glLinkProgram(p);
        int[] status = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(p);
            GLES20.glDeleteProgram(p);
            throw new RuntimeException("Program link failed: " + log);
        }
        return p;
    }

    private static int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code);
        GLES20.glCompileShader(s);
        int[] status = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(s);
            GLES20.glDeleteShader(s);
            throw new RuntimeException("Shader compile failed: " + log);
        }
        return s;
    }

    private static class Cube {
        private final FloatBuffer vertices;
        private final int vertexCount;

        Cube() {
            float[] data = {
                    -0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f,
                    -0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
                    0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
                    0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
                    -0.5f,-0.5f,-0.5f, -0.5f,-0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
                    -0.5f,-0.5f,-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,-0.5f,
                    0.5f,-0.5f, 0.5f,  0.5f,-0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
                    0.5f,-0.5f, 0.5f,  0.5f, 0.5f,-0.5f,  0.5f, 0.5f, 0.5f,
                    -0.5f, 0.5f, 0.5f,  0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f,
                    -0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
                    -0.5f,-0.5f,-0.5f,  0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f,
                    -0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f, -0.5f,-0.5f, 0.5f
            };
            vertexCount = data.length / 3;
            vertices = ByteBuffer.allocateDirect(data.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            vertices.put(data).position(0);
        }

        void draw(int positionHandle) {
            vertices.position(0);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertices);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
            GLES20.glDisableVertexAttribArray(positionHandle);
        }
    }

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVP;\n" +
            "attribute vec4 aPosition;\n" +
            "varying float vShade;\n" +
            "void main(){\n" +
            "  gl_Position = uMVP * aPosition;\n" +
            "  vShade = 0.82 + 0.18 * clamp(aPosition.y + 0.5, 0.0, 1.0);\n" +
            "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform vec4 uColor;\n" +
            "varying float vShade;\n" +
            "void main(){\n" +
            "  gl_FragColor = vec4(uColor.rgb * vShade, uColor.a);\n" +
            "}";
}
