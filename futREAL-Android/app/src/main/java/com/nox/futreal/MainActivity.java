package com.nox.futreal;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity {
    private GameState state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideBars();

        state = new GameState();
        FrameLayout root = new FrameLayout(this);
        GLGameView game = new GLGameView(this, state);
        HudView hud = new HudView(this, state);
        root.addView(game, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(hud, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideBars();
    }

    static class GLGameView extends GLSurfaceView {
        GLGameView(Context context, GameState state) {
            super(context);
            setEGLContextClientVersion(2);
            setPreserveEGLContextOnPause(true);
            setRenderer(new GameRenderer(state));
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }
    }

    static class GameState {
        volatile float joyX, joyY;
        volatile boolean sprint;
        volatile boolean shootRequest;
        volatile boolean passRequest;

        float playerX = 0f, playerZ = 30f;
        float facingX = 0f, facingZ = -1f;
        float runPhase;
        float moveAmount;

        float ballX = 0f, ballY = .45f, ballZ = 27.8f;
        float ballVX, ballVY, ballVZ;
        float ballSpin;
        boolean userHasBall = true;
        boolean opponentHasBall = false;
        int opponentOwner = -1;

        int homeScore, awayScore;
        float minute;
        float stamina = 100f;
        int shots, passes;
        float shotPulse;

        final float[][] mates = {
                {-18f, 22f}, {18f, 20f}, {-11f, 2f}, {13f, -4f}, {0f, -24f}
        };
        final float[][] opponents = {
                {-20f, -18f}, {-8f, -9f}, {9f, -12f}, {20f, -22f}, {0f, -34f}
        };
        final Random rng = new Random(19);

        void update(float dt) {
            minute += dt * 1.75f;
            shotPulse = Math.max(0f, shotPulse - dt * 2.6f);
            if (minute >= 90f) {
                minute = 0f;
                homeScore = 0;
                awayScore = 0;
                shots = 0;
                passes = 0;
                resetKickoff();
            }

            float jx = joyX, jz = joyY;
            float mag = (float) Math.sqrt(jx * jx + jz * jz);
            if (mag > 1f) { jx /= mag; jz /= mag; mag = 1f; }
            moveAmount = mag;
            if (mag > .08f) {
                facingX = jx;
                facingZ = jz;
                runPhase += dt * (sprint ? 13f : 8.2f) * mag;
            }

            float speed = sprint && stamina > 1f ? 13.6f : 8.8f;
            if (sprint && mag > .12f) stamina = Math.max(0f, stamina - dt * 14f);
            else stamina = Math.min(100f, stamina + dt * 6.8f);

            playerX = clamp(playerX + jx * speed * dt, -31f, 31f);
            playerZ = clamp(playerZ + jz * speed * dt, -49f, 49f);

            if (userHasBall) {
                float fx = mag > .08f ? facingX : 0f;
                float fz = mag > .08f ? facingZ : -1f;
                ballX = playerX + fx * .72f;
                ballZ = playerZ + fz * .72f;
                ballY = .45f + Math.abs((float)Math.sin(runPhase * 1.8f)) * .035f;
                ballVX = ballVY = ballVZ = 0f;
                ballSpin += dt * speed * mag * 2.7f;
            }

            if (shootRequest) {
                shootRequest = false;
                if (userHasBall) {
                    userHasBall = false;
                    shots++;
                    shotPulse = 1f;
                    float aimX = Math.abs(jx) > .12f ? jx : -ballX / 34f;
                    float targetX = clamp(aimX * 5.6f, -5.6f, 5.6f);
                    float dx = targetX - ballX;
                    float dz = -53f - ballZ;
                    float d = Math.max(.1f, (float)Math.sqrt(dx * dx + dz * dz));
                    ballVX = dx / d * 30f;
                    ballVZ = dz / d * 30f;
                    ballVY = 6.7f;
                }
            }

            if (passRequest) {
                passRequest = false;
                if (userHasBall) {
                    userHasBall = false;
                    passes++;
                    int best = 0;
                    float bestScore = 9999f;
                    for (int i = 0; i < mates.length; i++) {
                        float dz = mates[i][1] - playerZ;
                        float score = Math.abs(mates[i][0] - playerX)
                                + Math.abs(dz) * .42f + (dz > 5 ? 18 : 0);
                        if (score < bestScore) { bestScore = score; best = i; }
                    }
                    float tx = mates[best][0], tz = mates[best][1];
                    float dx = tx - ballX, dz = tz - ballZ;
                    float d = Math.max(.1f, (float)Math.sqrt(dx * dx + dz * dz));
                    ballVX = dx / d * 21f;
                    ballVZ = dz / d * 21f;
                    ballVY = 1.35f;
                }
            }

            if (!userHasBall && !opponentHasBall) {
                ballX += ballVX * dt;
                ballY += ballVY * dt;
                ballZ += ballVZ * dt;
                ballVY -= 15.5f * dt;
                float rollingSpeed = (float)Math.sqrt(ballVX * ballVX + ballVZ * ballVZ);
                ballSpin += rollingSpeed * dt * 1.8f;
                ballVX *= (float)Math.pow(.985, dt * 60f);
                ballVZ *= (float)Math.pow(.985, dt * 60f);
                if (ballY < .45f) {
                    ballY = .45f;
                    if (Math.abs(ballVY) > 1f) ballVY = -ballVY * .38f;
                    else ballVY = 0f;
                }
                if (dist(playerX, playerZ, ballX, ballZ) < 1.52f && ballY < 1.2f) {
                    userHasBall = true;
                }
            }

            moveTeammates(dt);
            moveOpponents(dt);

            if (opponentHasBall && opponentOwner >= 0) {
                float[] o = opponents[opponentOwner];
                ballX = o[0]; ballZ = o[1] + 1.0f; ballY = .45f;
                ballSpin += dt * 10f;
                if (dist(playerX, playerZ, o[0], o[1]) < 1.45f) {
                    opponentHasBall = false;
                    opponentOwner = -1;
                    userHasBall = true;
                } else if (o[1] > 39f) {
                    opponentHasBall = false;
                    opponentOwner = -1;
                    float dx = -ballX * .22f;
                    float dz = 53f - ballZ;
                    float d = Math.max(.1f, (float)Math.sqrt(dx * dx + dz * dz));
                    ballVX = dx / d * 25f;
                    ballVZ = dz / d * 25f;
                    ballVY = 4.2f;
                }
            }

            keeperBlocks();
            checkGoals();
        }

        private void moveTeammates(float dt) {
            float[][] targets = {
                    {-20f, 20f}, {20f, 18f}, {-12f, -2f}, {14f, -8f}, {0f, -28f}
            };
            for (int i = 0; i < mates.length; i++) {
                float tx = targets[i][0] + playerX * .20f;
                float tz = targets[i][1] + (playerZ - 30f) * .35f;
                chase(mates[i], tx, tz, 5.0f, dt);
                if (!userHasBall && !opponentHasBall
                        && dist(mates[i][0], mates[i][1], ballX, ballZ) < 1.15f && ballY < 1.0f) {
                    float dx = playerX - mates[i][0];
                    float dz = playerZ - mates[i][1];
                    float d = Math.max(.1f, (float)Math.sqrt(dx * dx + dz * dz));
                    ballX = mates[i][0]; ballZ = mates[i][1];
                    ballVX = dx / d * 16f; ballVZ = dz / d * 16f; ballVY = .8f;
                }
            }
        }

        private void moveOpponents(float dt) {
            int nearest = 0;
            float nearestD = 999f;
            for (int i = 0; i < opponents.length; i++) {
                float d = dist(opponents[i][0], opponents[i][1], ballX, ballZ);
                if (d < nearestD) { nearestD = d; nearest = i; }
            }
            for (int i = 0; i < opponents.length; i++) {
                float tx, tz;
                if (opponentHasBall && i == opponentOwner) {
                    tx = clamp(opponents[i][0] * .96f, -12f, 12f);
                    tz = 49f;
                    chase(opponents[i], tx, tz, 7.8f, dt);
                } else if (i == nearest || (userHasBall && i < 2)) {
                    tx = userHasBall ? playerX : ballX;
                    tz = userHasBall ? playerZ : ballZ;
                    chase(opponents[i], tx, tz, 7.2f, dt);
                } else {
                    float baseX = (i - 2) * 10f;
                    tx = clamp(baseX + playerX * .12f, -25f, 25f);
                    tz = -18f + i * 3f + (playerZ - 30f) * .26f;
                    chase(opponents[i], tx, tz, 4.2f, dt);
                }

                if (userHasBall && dist(opponents[i][0], opponents[i][1], playerX, playerZ) < 1.35f) {
                    if (rng.nextFloat() < dt * 2.0f) {
                        userHasBall = false;
                        opponentHasBall = true;
                        opponentOwner = i;
                    }
                } else if (!userHasBall && !opponentHasBall
                        && dist(opponents[i][0], opponents[i][1], ballX, ballZ) < 1.2f && ballY < 1.0f) {
                    opponentHasBall = true;
                    opponentOwner = i;
                }
            }
        }

        private void keeperBlocks() {
            if (!userHasBall && !opponentHasBall && ballY < 2.6f) {
                if (ballZ < -49f && Math.abs(ballX) < 7.2f && rng.nextFloat() < .35f) {
                    ballVZ = Math.abs(ballVZ) * .55f;
                    ballVX += (ballX >= 0 ? 7f : -7f);
                    ballVY = 4f;
                }
                if (ballZ > 49f && Math.abs(ballX) < 7.2f && rng.nextFloat() < .35f) {
                    ballVZ = -Math.abs(ballVZ) * .55f;
                    ballVX += (ballX >= 0 ? 7f : -7f);
                    ballVY = 4f;
                }
            }
        }

        private void checkGoals() {
            if (ballZ < -52f && Math.abs(ballX) < 7.3f && ballY < 3.0f) {
                homeScore++;
                resetKickoff();
            } else if (ballZ > 52f && Math.abs(ballX) < 7.3f && ballY < 3.0f) {
                awayScore++;
                resetKickoff();
            } else if (Math.abs(ballX) > 36f || Math.abs(ballZ) > 58f) {
                resetKickoff();
            }
        }

        private void resetKickoff() {
            playerX = 0f; playerZ = 30f;
            facingX = 0f; facingZ = -1f;
            ballX = 0f; ballY = .45f; ballZ = 27.8f;
            ballVX = ballVY = ballVZ = 0f;
            userHasBall = true;
            opponentHasBall = false;
            opponentOwner = -1;
            float[][] m = {{-18,22},{18,20},{-11,2},{13,-4},{0,-24}};
            float[][] o = {{-20,-18},{-8,-9},{9,-12},{20,-22},{0,-34}};
            for (int i=0;i<mates.length;i++){mates[i][0]=m[i][0]; mates[i][1]=m[i][1];}
            for (int i=0;i<opponents.length;i++){opponents[i][0]=o[i][0]; opponents[i][1]=o[i][1];}
        }

        private static void chase(float[] p, float tx, float tz, float speed, float dt) {
            float dx = tx - p[0], dz = tz - p[1];
            float d = (float)Math.sqrt(dx * dx + dz * dz);
            if (d > .05f) {
                float step = Math.min(d, speed * dt);
                p[0] += dx / d * step;
                p[1] += dz / d * step;
            }
            p[0] = clamp(p[0], -31f, 31f);
            p[1] = clamp(p[1], -49f, 49f);
        }

        static float dist(float ax, float az, float bx, float bz) {
            float dx = ax - bx, dz = az - bz;
            return (float)Math.sqrt(dx * dx + dz * dz);
        }

        static float clamp(float v, float lo, float hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }

    static class GameRenderer implements GLSurfaceView.Renderer {
        private final GameState s;
        private int program, posLoc, normalLoc, mvpLoc, modelLoc, colorLoc, lightLoc, ambientLoc;
        private final float[] proj = new float[16];
        private final float[] view = new float[16];
        private final float[] vp = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];
        private Mesh cube, sphere;
        private long lastMs;
        private float sceneTime;
        private float camX, camZ = 53f, camH = 15f;

        GameRenderer(GameState state) { s = state; }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(.055f, .12f, .20f, 1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            String vs =
                    "uniform mat4 uMVP; uniform mat4 uModel; attribute vec3 aPos; attribute vec3 aNormal; " +
                    "varying vec3 vNormal; varying vec3 vWorld; " +
                    "void main(){ vec4 w=uModel*vec4(aPos,1.0); vWorld=w.xyz; " +
                    "vNormal=normalize(mat3(uModel)*aNormal); gl_Position=uMVP*vec4(aPos,1.0); }";
            String fs =
                    "precision mediump float; uniform vec4 uColor; uniform vec3 uLightDir; uniform float uAmbient; " +
                    "varying vec3 vNormal; varying vec3 vWorld; " +
                    "void main(){ float d=max(dot(normalize(vNormal),normalize(-uLightDir)),0.0); " +
                    "float rim=pow(1.0-max(abs(vNormal.y),0.0),3.0)*0.08; " +
                    "vec3 col=uColor.rgb*(uAmbient+d*0.72+rim); gl_FragColor=vec4(col,uColor.a); }";
            program = link(vs, fs);
            posLoc = GLES20.glGetAttribLocation(program, "aPos");
            normalLoc = GLES20.glGetAttribLocation(program, "aNormal");
            mvpLoc = GLES20.glGetUniformLocation(program, "uMVP");
            modelLoc = GLES20.glGetUniformLocation(program, "uModel");
            colorLoc = GLES20.glGetUniformLocation(program, "uColor");
            lightLoc = GLES20.glGetUniformLocation(program, "uLightDir");
            ambientLoc = GLES20.glGetUniformLocation(program, "uAmbient");
            cube = Mesh.cube();
            sphere = Mesh.sphere(12, 18);
            lastMs = SystemClock.uptimeMillis();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float aspect = width / (float)Math.max(1, height);
            Matrix.perspectiveM(proj, 0, 49f, aspect, .1f, 260f);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            long now = SystemClock.uptimeMillis();
            float dt = Math.min(.033f, Math.max(.001f, (now - lastMs) / 1000f));
            lastMs = now;
            sceneTime += dt;
            s.update(dt);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            float targetCamX = s.playerX * .18f + s.ballX * .08f;
            float targetCamZ = s.playerZ + 24f;
            float targetH = 15f + Math.min(2.2f, s.ballY * .18f);
            float blend = Math.min(1f, dt * 3.0f);
            camX += (targetCamX - camX) * blend;
            camZ += (targetCamZ - camZ) * blend;
            camH += (targetH - camH) * blend;
            float shake = s.shotPulse > 0f ? (float)Math.sin(sceneTime * 48f) * .10f * s.shotPulse : 0f;

            Matrix.setLookAtM(view, 0,
                    camX + shake, camH, camZ,
                    s.playerX * .10f, .65f, s.playerZ - 16f,
                    0f, 1f, 0f);
            Matrix.multiplyMM(vp, 0, proj, 0, view, 0);
            GLES20.glUseProgram(program);
            GLES20.glUniform3f(lightLoc, -.35f, -1f, .25f);
            GLES20.glUniform1f(ambientLoc, .34f);

            drawStadium();
            drawPitch();
            drawGoals();
            drawPlayers();
            drawBall();
        }

        private void drawStadium() {
            float[] concrete = {.16f,.18f,.21f,1};
            float[] dark = {.08f,.095f,.115f,1};
            float[] blue = {.08f,.32f,.52f,1};
            float[] red = {.55f,.10f,.14f,1};
            float[] lime = {.62f,.92f,.16f,1};

            drawCube(0,-1.35f,0,88,2.2f,132,dark);

            for (int tier=0;tier<3;tier++) {
                float x = 41.5f + tier * 3.2f;
                float y = 2.0f + tier * 3.2f;
                float h = 3.3f;
                drawCube(-x,y,0,5.6f,h,118,concrete);
                drawCube(x,y,0,5.6f,h,118,concrete);
            }
            for (int tier=0;tier<3;tier++) {
                float z = 63f + tier * 3.2f;
                float y = 2.0f + tier * 3.2f;
                drawCube(0,y,-z,76,3.3f,5.6f,concrete);
                drawCube(0,y,z,76,3.3f,5.6f,concrete);
            }

            for (int side=-1; side<=1; side+=2) {
                for (int z=-50; z<=50; z+=5) {
                    for (int row=0; row<3; row++) {
                        float x = side * (39.4f + row * 3.2f);
                        float y = 2.5f + row * 3.2f;
                        float[] c = ((z/5 + row) & 1)==0 ? blue : red;
                        drawCube(x,y,z,.55f,1.2f,3.0f,c);
                        drawCube(x,y+1.0f,z+1.25f,.45f,.65f,.55f,((z/5)&1)==0?lime:blue);
                    }
                }
            }
            for (int end=-1; end<=1; end+=2) {
                for (int x=-30; x<=30; x+=5) {
                    for (int row=0; row<2; row++) {
                        float z = end * (60.5f + row * 3.2f);
                        float y = 2.6f + row * 3.2f;
                        drawCube(x,y,z,3.0f,1.2f,.55f,((x/5+row)&1)==0?red:blue);
                    }
                }
            }

            drawFloodlight(-39f,-50f);
            drawFloodlight(39f,-50f);
            drawFloodlight(-39f,50f);
            drawFloodlight(39f,50f);

            float[] board = {.03f,.16f,.18f,1};
            float[] ad = {.68f,.98f,.16f,1};
            drawCube(-35.2f,.65f,0,.35f,1.25f,106,board);
            drawCube(35.2f,.65f,0,.35f,1.25f,106,board);
            for (int z=-45;z<=45;z+=15) {
                drawCube(-35.4f,.72f,z,.08f,.55f,9,ad);
                drawCube(35.4f,.72f,z,.08f,.55f,9,ad);
            }
        }

        private void drawFloodlight(float x,float z) {
            float[] pole={.20f,.22f,.24f,1};
            float[] lamp={1f,.98f,.78f,1};
            drawCube(x,9f,z,.30f,18f,.30f,pole);
            drawCube(x,18.2f,z,5.2f,.35f,1.3f,pole);
            for(int i=-2;i<=2;i++) drawCube(x+i*1.0f,18.15f,z-.55f,.65f,.45f,.18f,lamp);
        }

        private void drawPitch() {
            float[] g1 = {.028f,.42f,.105f,1};
            float[] g2 = {.022f,.34f,.078f,1};
            for (int i=0;i<12;i++) {
                float z = -48.1f + i*8.75f;
                drawCube(0,-.06f,z,68,.12f,8.75f,(i%2==0)?g1:g2);
            }
            float[] white = {.92f,.95f,.92f,1};
            drawCube(0,.025f,-52.5f,68,.05f,.12f,white);
            drawCube(0,.025f,52.5f,68,.05f,.12f,white);
            drawCube(-34,.025f,0,.12f,.05f,105,white);
            drawCube(34,.025f,0,.12f,.05f,105,white);
            drawCube(0,.025f,0,68,.05f,.12f,white);
            drawCube(-10,.025f,-43,.12f,.05f,19,white);
            drawCube(10,.025f,-43,.12f,.05f,19,white);
            drawCube(0,.025f,-33.5f,20,.05f,.12f,white);
            drawCube(-10,.025f,43,.12f,.05f,19,white);
            drawCube(10,.025f,43,.12f,.05f,19,white);
            drawCube(0,.025f,33.5f,20,.05f,.12f,white);
            for (int i=0;i<40;i++) {
                double a = i * Math.PI * 2 / 40.0;
                float x = (float)Math.cos(a) * 9.15f;
                float z = (float)Math.sin(a) * 9.15f;
                drawCubeRotY(x,.03f,z,1.48f,.05f,.09f,-i*9f,white);
            }
        }

        private void drawGoals() {
            drawGoal(-53.2f, -1f);
            drawGoal(53.2f, 1f);
        }

        private void drawGoal(float z, float dir) {
            float[] w={.96f,.96f,.96f,1};
            float[] net={.72f,.78f,.78f,.55f};
            drawCube(-7.3f,1.25f,z,.16f,2.5f,.16f,w);
            drawCube(7.3f,1.25f,z,.16f,2.5f,.16f,w);
            drawCube(0,2.5f,z,14.7f,.16f,.16f,w);
            float backZ=z+dir*2.4f;
            drawCube(-7.3f,1.1f,backZ,.10f,2.2f,.10f,net);
            drawCube(7.3f,1.1f,backZ,.10f,2.2f,.10f,net);
            drawCube(0,2.2f,backZ,14.6f,.10f,.10f,net);
            for(int i=-6;i<=6;i+=2) drawCube(i,1.1f,backZ,.035f,2.1f,.035f,net);
            for(int y=0;y<=2;y++) drawCube(0,.45f+y*.7f,backZ,14.4f,.035f,.035f,net);
        }

        private void drawPlayers() {
            float[] user={.62f,.92f,.13f,1};
            float[] mate={.08f,.56f,.94f,1};
            float[] opp={.92f,.12f,.16f,1};
            float[] skin={.82f,.58f,.38f,1};
            float[] keeper={.98f,.68f,.08f,1};

            float userAngle = angleFromDir(s.facingX, s.facingZ);
            drawPerson(s.playerX,s.playerZ,user,skin,true,s.runPhase,s.moveAmount,userAngle);
            for(int i=0;i<s.mates.length;i++) {
                float[] q=s.mates[i];
                float ang=angleFromDir(s.ballX-q[0],s.ballZ-q[1]);
                drawPerson(q[0],q[1],mate,skin,false,sceneTime*5f+i*.9f,.65f,ang);
            }
            for(int i=0;i<s.opponents.length;i++) {
                float[] q=s.opponents[i];
                float ang=angleFromDir(s.ballX-q[0],s.ballZ-q[1]);
                drawPerson(q[0],q[1],opp,skin,false,sceneTime*5.6f+i*.7f,.72f,ang);
            }
            drawPerson(0,-49.8f,keeper,skin,false,sceneTime*2f,.2f,0f);
            drawPerson(0,49.8f,keeper,skin,false,sceneTime*2f,.2f,180f);
        }

        private void drawPerson(float x,float z,float[] kit,float[] skin,boolean selected,
                                float phase,float movement,float rotY) {
            drawShadow(x,z,1.05f);
            float swing=(float)Math.sin(phase)*28f*movement;
            float bob=Math.abs((float)Math.sin(phase))*0.055f*movement;
            float[] shorts={kit[0]*.42f,kit[1]*.42f,kit[2]*.42f,1};
            float[] socks={Math.min(1f,kit[0]*1.08f),Math.min(1f,kit[1]*1.08f),Math.min(1f,kit[2]*1.08f),1};
            float[] boots={.035f,.045f,.055f,1};
            float[] hair={.055f,.04f,.03f,1};
            float[] accent={.92f,.95f,.92f,1};

            drawPart(x,z,0,1.58f+bob,0,1.08f,1.35f,.66f,rotY,0,kit,cube);
            drawPart(x,z,0,.94f+bob,0,.92f,.52f,.70f,rotY,0,shorts,cube);
            drawPart(x,z,0,1.62f+bob,-.355f,.72f,.16f,.08f,rotY,0,accent,cube);

            drawPart(x,z,-.72f,1.58f+bob,0,.20f,1.02f,.22f,rotY,-swing,skin,cube);
            drawPart(x,z,.72f,1.58f+bob,0,.20f,1.02f,.22f,rotY,swing,skin,cube);

            drawPart(x,z,-.28f,.42f+bob,.03f,.27f,.88f,.29f,rotY,swing,socks,cube);
            drawPart(x,z,.28f,.42f+bob,.03f,.27f,.88f,.29f,rotY,-swing,socks,cube);
            drawPart(x,z,-.28f,.08f+bob,-.12f,.34f,.16f,.55f,rotY,swing*.25f,boots,cube);
            drawPart(x,z,.28f,.08f+bob,-.12f,.34f,.16f,.55f,rotY,-swing*.25f,boots,cube);

            drawPart(x,z,0,2.58f+bob,0,.47f,.47f,.47f,rotY,0,skin,sphere);
            drawPart(x,z,0,2.86f+bob,.015f,.46f,.20f,.43f,rotY,0,hair,sphere);

            if(selected) {
                float pulse=1.55f+(float)Math.sin(sceneTime*5f)*.08f;
                drawFlatSphere(x,.038f,z,pulse,.035f,pulse,new float[]{.66f,1f,.12f,.78f});
            }
        }

        private void drawShadow(float x,float z,float size) {
            drawFlatSphere(x,.04f,z,size,.035f,size*.72f,new float[]{.01f,.015f,.018f,.32f});
        }

        private void drawBall() {
            drawFlatSphere(s.ballX,.035f,s.ballZ,.54f,.025f,.42f,new float[]{.01f,.015f,.018f,.30f});
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,s.ballX,s.ballY,s.ballZ);
            Matrix.rotateM(model,0,s.ballSpin*57.2958f,1,.35f,.25f);
            Matrix.scaleM(model,0,.46f,.46f,.46f);
            drawCurrent(sphere,new float[]{.98f,.98f,.95f,1});

            float[][] patches={{0,.39f,.18f},{.32f,.08f,.25f},{-.30f,.12f,.27f},{.12f,-.28f,.30f}};
            for(float[] q:patches) {
                Matrix.setIdentityM(model,0);
                Matrix.translateM(model,0,s.ballX,s.ballY,s.ballZ);
                Matrix.rotateM(model,0,s.ballSpin*57.2958f,1,.35f,.25f);
                Matrix.translateM(model,0,q[0],q[1],q[2]);
                Matrix.scaleM(model,0,.095f,.095f,.095f);
                drawCurrent(sphere,new float[]{.035f,.04f,.045f,1});
            }
        }

        private void drawFlatSphere(float x,float y,float z,float sx,float sy,float sz,float[] c) {
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,x,y,z);
            Matrix.scaleM(model,0,sx,sy,sz);
            drawCurrent(sphere,c);
        }

        private void drawPart(float wx,float wz,float lx,float y,float lz,
                              float sx,float sy,float sz,float rotY,float rotX,float[] c,Mesh mesh) {
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,wx,0,wz);
            Matrix.rotateM(model,0,rotY,0,1,0);
            Matrix.translateM(model,0,lx,y,lz);
            Matrix.rotateM(model,0,rotX,1,0,0);
            Matrix.scaleM(model,0,sx,sy,sz);
            drawCurrent(mesh,c);
        }

        private void drawCube(float x,float y,float z,float sx,float sy,float sz,float[] c) {
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,x,y,z);
            Matrix.scaleM(model,0,sx,sy,sz);
            drawCurrent(cube,c);
        }

        private void drawCubeRotY(float x,float y,float z,float sx,float sy,float sz,float deg,float[] c) {
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,x,y,z);
            Matrix.rotateM(model,0,deg,0,1,0);
            Matrix.scaleM(model,0,sx,sy,sz);
            drawCurrent(cube,c);
        }

        private void drawCurrent(Mesh mesh,float[] color) {
            Matrix.multiplyMM(mvp,0,vp,0,model,0);
            GLES20.glUniformMatrix4fv(mvpLoc,1,false,mvp,0);
            GLES20.glUniformMatrix4fv(modelLoc,1,false,model,0);
            GLES20.glUniform4fv(colorLoc,1,color,0);
            mesh.draw(posLoc,normalLoc);
        }

        private static float angleFromDir(float dx,float dz) {
            if(Math.abs(dx)+Math.abs(dz)<.001f) return 0f;
            return (float)Math.toDegrees(Math.atan2(dx,dz));
        }

        private static int link(String vs,String fs) {
            int v=compile(GLES20.GL_VERTEX_SHADER,vs);
            int f=compile(GLES20.GL_FRAGMENT_SHADER,fs);
            int p=GLES20.glCreateProgram();
            GLES20.glAttachShader(p,v);
            GLES20.glAttachShader(p,f);
            GLES20.glLinkProgram(p);
            GLES20.glDeleteShader(v);
            GLES20.glDeleteShader(f);
            return p;
        }

        private static int compile(int type,String src) {
            int sh=GLES20.glCreateShader(type);
            GLES20.glShaderSource(sh,src);
            GLES20.glCompileShader(sh);
            return sh;
        }
    }

    static class Mesh {
        final FloatBuffer vertices;
        final FloatBuffer normals;
        final int count;

        Mesh(float[] v,float[] n) {
            count=v.length/3;
            vertices=ByteBuffer.allocateDirect(v.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            normals=ByteBuffer.allocateDirect(n.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            vertices.put(v).position(0);
            normals.put(n).position(0);
        }

        void draw(int pos,int normal) {
            vertices.position(0);
            normals.position(0);
            GLES20.glEnableVertexAttribArray(pos);
            GLES20.glEnableVertexAttribArray(normal);
            GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,vertices);
            GLES20.glVertexAttribPointer(normal,3,GLES20.GL_FLOAT,false,0,normals);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
            GLES20.glDisableVertexAttribArray(pos);
            GLES20.glDisableVertexAttribArray(normal);
        }

        static Mesh cube() {
            float[] v={
                    -.5f,-.5f,.5f, .5f,-.5f,.5f, .5f,.5f,.5f, -.5f,-.5f,.5f, .5f,.5f,.5f, -.5f,.5f,.5f,
                    .5f,-.5f,-.5f, -.5f,-.5f,-.5f, -.5f,.5f,-.5f, .5f,-.5f,-.5f, -.5f,.5f,-.5f, .5f,.5f,-.5f,
                    -.5f,-.5f,-.5f, -.5f,-.5f,.5f, -.5f,.5f,.5f, -.5f,-.5f,-.5f, -.5f,.5f,.5f, -.5f,.5f,-.5f,
                    .5f,-.5f,.5f, .5f,-.5f,-.5f, .5f,.5f,-.5f, .5f,-.5f,.5f, .5f,.5f,-.5f, .5f,.5f,.5f,
                    -.5f,.5f,.5f, .5f,.5f,.5f, .5f,.5f,-.5f, -.5f,.5f,.5f, .5f,.5f,-.5f, -.5f,.5f,-.5f,
                    -.5f,-.5f,-.5f, .5f,-.5f,-.5f, .5f,-.5f,.5f, -.5f,-.5f,-.5f, .5f,-.5f,.5f, -.5f,-.5f,.5f
            };
            float[] n=new float[36*3];
            int k=0;
            float[][] face={{0,0,1},{0,0,-1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
            for(float[] f:face) for(int i=0;i<6;i++){n[k++]=f[0];n[k++]=f[1];n[k++]=f[2];}
            return new Mesh(v,n);
        }

        static Mesh sphere(int lat,int lon) {
            float[] out=new float[lat*lon*6*3];
            float[] norm=new float[out.length];
            int k=0;
            for(int i=0;i<lat;i++) {
                float a1=(float)(-Math.PI/2 + Math.PI*i/lat);
                float a2=(float)(-Math.PI/2 + Math.PI*(i+1)/lat);
                for(int j=0;j<lon;j++) {
                    float b1=(float)(2*Math.PI*j/lon);
                    float b2=(float)(2*Math.PI*(j+1)/lon);
                    float[] p1={cos(a1)*cos(b1),sin(a1),cos(a1)*sin(b1)};
                    float[] p2={cos(a2)*cos(b1),sin(a2),cos(a2)*sin(b1)};
                    float[] p3={cos(a2)*cos(b2),sin(a2),cos(a2)*sin(b2)};
                    float[] p4={cos(a1)*cos(b2),sin(a1),cos(a1)*sin(b2)};
                    float[][] tri={p1,p2,p3,p1,p3,p4};
                    for(float[] p:tri) {
                        out[k]=p[0]; norm[k++]=p[0];
                        out[k]=p[1]; norm[k++]=p[1];
                        out[k]=p[2]; norm[k++]=p[2];
                    }
                }
            }
            return new Mesh(out,norm);
        }

        static float sin(float a){return (float)Math.sin(a);}
        static float cos(float a){return (float)Math.cos(a);}
    }

    static class HudView extends View {
        final GameState s;
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        float joyCx,joyCy,joyR;
        int joyPointer=-1;
        float knobX,knobY;

        HudView(Context c,GameState state) {
            super(c);
            s=state;
            setBackgroundColor(Color.TRANSPARENT);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(),h=getHeight();
            joyCx=118; joyCy=h-118; joyR=80;
            if(joyPointer<0){knobX=joyCx;knobY=joyCy;}

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(205,4,10,16));
            c.drawRoundRect(w*.345f,16,w*.655f,86,20,20,p);
            textCenter(c,"AUR  "+s.homeScore+"   -   "+s.awayScore+"  VIL",w*.50f,54,23,Color.WHITE);
            text(c,String.format("%02d'",(int)s.minute),w*.36f,56,17,Color.rgb(205,255,85));

            p.setColor(Color.argb(190,4,10,16));
            c.drawRoundRect(16,15,190,72,16,16,p);
            text(c,"futREAL v4",29,42,20,Color.WHITE);
            text(c,"NEXT LEAP 3D",29,62,11,Color.rgb(205,255,85));

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            p.setColor(Color.argb(90,255,255,255));
            c.drawCircle(joyCx,joyCy,joyR,p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(92,255,255,255));
            c.drawCircle(joyCx,joyCy,joyR-5,p);
            p.setColor(Color.argb(205,205,255,85));
            c.drawCircle(knobX,knobY,32,p);

            float shootX=w-105,shootY=h-158,shootR=64;
            float passX=w-245,passY=h-100,passR=53;
            float sprintX=w-278,sprintY=h-226,sprintR=47;
            button(c,shootX,shootY,shootR,"CHUTE",Color.argb(220,205,255,85),Color.rgb(5,20,12));
            button(c,passX,passY,passR,"PASSE",Color.argb(205,245,248,252),Color.rgb(8,22,30));
            button(c,sprintX,sprintY,sprintR,"SPRINT",Color.argb(s.sprint?225:135,55,165,255),Color.WHITE);

            float barW=180,x=w-215,y=25;
            p.setColor(Color.argb(180,5,10,15));
            c.drawRoundRect(x,y,x+barW,y+19,10,10,p);
            p.setColor(s.stamina>25?Color.rgb(205,255,85):Color.rgb(255,150,60));
            c.drawRoundRect(x,y,x+barW*(s.stamina/100f),y+19,10,10,p);
            text(c,"STAMINA",x-67,y+15,11,Color.WHITE);

            p.setColor(Color.argb(165,4,10,16));
            c.drawRoundRect(18,82,148,132,14,14,p);
            text(c,"CHUTES  "+s.shots,30,104,12,Color.WHITE);
            text(c,"PASSES  "+s.passes,30,123,12,Color.LTGRAY);

            drawMiniMap(c,w*.50f,h-54,154,38);
            postInvalidateOnAnimation();
        }

        private void drawMiniMap(Canvas c,float cx,float cy,float mw,float mh) {
            p.setColor(Color.argb(175,3,25,12));
            c.drawRoundRect(cx-mw/2,cy-mh/2,cx+mw/2,cy+mh/2,8,8,p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.4f);
            p.setColor(Color.argb(185,255,255,255));
            c.drawRect(cx-mw/2+3,cy-mh/2+3,cx+mw/2-3,cy+mh/2-3,p);
            p.setStyle(Paint.Style.FILL);
            dot(c,cx+s.playerX/68f*mw,cy+s.playerZ/105f*mh,4.2f,Color.rgb(205,255,85));
            for(float[] q:s.mates) dot(c,cx+q[0]/68f*mw,cy+q[1]/105f*mh,3,Color.CYAN);
            for(float[] q:s.opponents) dot(c,cx+q[0]/68f*mw,cy+q[1]/105f*mh,3,Color.RED);
            dot(c,cx+s.ballX/68f*mw,cy+s.ballZ/105f*mh,2.3f,Color.WHITE);
        }

        private void dot(Canvas c,float x,float y,float r,int col){p.setColor(col);c.drawCircle(x,y,r,p);}
        private void button(Canvas c,float x,float y,float r,String label,int bg,int fg){p.setColor(bg);c.drawCircle(x,y,r,p);textCenter(c,label,x,y+5,14,fg);}
        private void text(Canvas c,String t,float x,float y,float size,int col){p.setColor(col);p.setTextSize(size);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.LEFT);c.drawText(t,x,y,p);}
        private void textCenter(Canvas c,String t,float x,float y,float size,int col){p.setColor(col);p.setTextSize(size);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);c.drawText(t,x,y,p);p.setTextAlign(Paint.Align.LEFT);}

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            int action=e.getActionMasked();
            int idx=e.getActionIndex();
            if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
                float x=e.getX(idx),y=e.getY(idx);
                int id=e.getPointerId(idx);
                if(x<getWidth()*.43f && y>getHeight()*.40f && joyPointer<0) {
                    joyPointer=id;
                    updateJoy(x,y);
                } else handleButtonDown(x,y);
            } else if(action==MotionEvent.ACTION_MOVE) {
                boolean sprintHeld=false;
                for(int i=0;i<e.getPointerCount();i++) {
                    int id=e.getPointerId(i);
                    float x=e.getX(i),y=e.getY(i);
                    if(id==joyPointer) updateJoy(x,y);
                    if(inCircle(x,y,getWidth()-278,getHeight()-226,61)) sprintHeld=true;
                }
                s.sprint=sprintHeld;
            } else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP || action==MotionEvent.ACTION_CANCEL) {
                int id=e.getPointerId(idx);
                if(id==joyPointer) {
                    joyPointer=-1;
                    s.joyX=s.joyY=0;
                    knobX=joyCx;knobY=joyCy;
                }
                if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) s.sprint=false;
            }
            invalidate();
            return true;
        }

        private void handleButtonDown(float x,float y) {
            float w=getWidth(),h=getHeight();
            if(inCircle(x,y,w-105,h-158,80)) s.shootRequest=true;
            else if(inCircle(x,y,w-245,h-100,69)) s.passRequest=true;
            else if(inCircle(x,y,w-278,h-226,63)) s.sprint=true;
        }

        private void updateJoy(float x,float y) {
            float dx=x-joyCx,dy=y-joyCy;
            float d=(float)Math.sqrt(dx*dx+dy*dy);
            if(d>joyR){dx=dx/d*joyR;dy=dy/d*joyR;}
            knobX=joyCx+dx;
            knobY=joyCy+dy;
            s.joyX=dx/joyR;
            s.joyY=dy/joyR;
        }

        private static boolean inCircle(float x,float y,float cx,float cy,float r) {
            float dx=x-cx,dy=y-cy;
            return dx*dx+dy*dy<=r*r;
        }
    }
}
