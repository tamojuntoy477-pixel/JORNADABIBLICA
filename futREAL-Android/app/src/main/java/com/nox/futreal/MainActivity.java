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
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

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
        float ballX = 0f, ballY = .45f, ballZ = 27.8f;
        float ballVX, ballVY, ballVZ;
        boolean userHasBall = true;
        boolean opponentHasBall = false;
        int opponentOwner = -1;
        int homeScore, awayScore;
        float minute;
        float stamina = 100f;
        int shots, passes;

        final float[][] mates = {
                {-18f, 22f}, {18f, 20f}, {-11f, 2f}, {13f, -4f}, {0f, -24f}
        };
        final float[][] opponents = {
                {-20f, -18f}, {-8f, -9f}, {9f, -12f}, {20f, -22f}, {0f, -34f}
        };
        final Random rng = new Random(19);

        void update(float dt) {
            minute += dt * 1.75f;
            if (minute >= 90f) {
                minute = 0f;
                homeScore = 0;
                awayScore = 0;
                shots = 0;
                passes = 0;
                resetKickoff();
            }

            float jx = joyX, jy = joyY;
            float mag = (float)Math.sqrt(jx * jx + jy * jy);
            if (mag > 1f) { jx /= mag; jy /= mag; }
            float speed = sprint && stamina > 1f ? 13.5f : 8.8f;
            if (sprint && mag > .12f) stamina = Math.max(0f, stamina - dt * 15f);
            else stamina = Math.min(100f, stamina + dt * 7f);

            playerX = clamp(playerX + jx * speed * dt, -31f, 31f);
            playerZ = clamp(playerZ + jy * speed * dt, -49f, 49f);

            if (userHasBall) {
                ballX = playerX + jx * 1.0f;
                ballZ = playerZ + jy * 1.0f - 1.2f;
                ballY = .45f;
                ballVX = ballVY = ballVZ = 0f;
            }

            if (shootRequest) {
                shootRequest = false;
                if (userHasBall) {
                    userHasBall = false;
                    shots++;
                    float targetX = clamp(jx * 5.5f, -5.5f, 5.5f);
                    float dx = targetX - ballX;
                    float dz = -53f - ballZ;
                    float d = (float)Math.sqrt(dx * dx + dz * dz);
                    ballVX = dx / d * 28f;
                    ballVZ = dz / d * 28f;
                    ballVY = 6.3f;
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
                        float score = Math.abs(mates[i][0] - playerX) + Math.abs(dz) * .4f + (dz > 4 ? 18 : 0);
                        if (score < bestScore) { bestScore = score; best = i; }
                    }
                    float tx = mates[best][0], tz = mates[best][1];
                    float dx = tx - ballX, dz = tz - ballZ;
                    float d = Math.max(.1f, (float)Math.sqrt(dx * dx + dz * dz));
                    ballVX = dx / d * 20f;
                    ballVZ = dz / d * 20f;
                    ballVY = 1.4f;
                }
            }

            if (!userHasBall && !opponentHasBall) {
                ballX += ballVX * dt;
                ballY += ballVY * dt;
                ballZ += ballVZ * dt;
                ballVY -= 15.5f * dt;
                ballVX *= (float)Math.pow(.985, dt * 60f);
                ballVZ *= (float)Math.pow(.985, dt * 60f);
                if (ballY < .45f) {
                    ballY = .45f;
                    if (Math.abs(ballVY) > 1f) ballVY = -ballVY * .38f;
                    else ballVY = 0f;
                }
                if (dist(playerX, playerZ, ballX, ballZ) < 1.55f && ballY < 1.2f) {
                    userHasBall = true;
                }
            }

            moveTeammates(dt);
            moveOpponents(dt);

            if (opponentHasBall && opponentOwner >= 0) {
                float[] o = opponents[opponentOwner];
                ballX = o[0]; ballZ = o[1] + 1.0f; ballY = .45f;
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
                if (!userHasBall && !opponentHasBall && dist(mates[i][0], mates[i][1], ballX, ballZ) < 1.15f && ballY < 1.0f) {
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
                } else if (!userHasBall && !opponentHasBall &&
                        dist(opponents[i][0], opponents[i][1], ballX, ballZ) < 1.2f && ballY < 1.0f) {
                    opponentHasBall = true;
                    opponentOwner = i;
                }
            }
        }

        private void keeperBlocks() {
            if (!userHasBall && !opponentHasBall && ballY < 2.6f) {
                if (ballZ < -49f && Math.abs(ballX) < 7.2f) {
                    if (Math.abs(ballX) < 5.6f && rng.nextFloat() < .35f) {
                        ballVZ = Math.abs(ballVZ) * .55f;
                        ballVX += (ballX >= 0 ? 7f : -7f);
                        ballVY = 4f;
                    }
                }
                if (ballZ > 49f && Math.abs(ballX) < 7.2f) {
                    if (Math.abs(ballX) < 5.6f && rng.nextFloat() < .35f) {
                        ballVZ = -Math.abs(ballVZ) * .55f;
                        ballVX += (ballX >= 0 ? 7f : -7f);
                        ballVY = 4f;
                    }
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
                p[0] += dx / d * step; p[1] += dz / d * step;
            }
            p[0] = clamp(p[0], -31f, 31f); p[1] = clamp(p[1], -49f, 49f);
        }

        private static float dist(float ax, float az, float bx, float bz) {
            float dx = ax - bx, dz = az - bz;
            return (float)Math.sqrt(dx * dx + dz * dz);
        }

        private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    }

    static class GameRenderer implements GLSurfaceView.Renderer {
        private final GameState s;
        private int program, posLoc, mvpLoc, colorLoc;
        private final float[] proj = new float[16];
        private final float[] view = new float[16];
        private final float[] vp = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];
        private Mesh cube, sphere;
        private long lastMs;

        GameRenderer(GameState state) { s = state; }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(.10f, .18f, .28f, 1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);
            String vs = "uniform mat4 uMVP; attribute vec3 aPos; void main(){ gl_Position=uMVP*vec4(aPos,1.0); }";
            String fs = "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }";
            program = link(vs, fs);
            posLoc = GLES20.glGetAttribLocation(program, "aPos");
            mvpLoc = GLES20.glGetUniformLocation(program, "uMVP");
            colorLoc = GLES20.glGetUniformLocation(program, "uColor");
            cube = Mesh.cube();
            sphere = Mesh.sphere(10, 14);
            lastMs = SystemClock.uptimeMillis();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float aspect = width / (float)Math.max(1, height);
            Matrix.perspectiveM(proj, 0, 53f, aspect, .1f, 220f);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            long now = SystemClock.uptimeMillis();
            float dt = Math.min(.033f, Math.max(.001f, (now - lastMs) / 1000f));
            lastMs = now;
            s.update(dt);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            float camX = s.playerX * .20f;
            float camZ = s.playerZ + 23f;
            Matrix.setLookAtM(view, 0,
                    camX, 15.5f, camZ,
                    s.playerX * .12f, 0.5f, s.playerZ - 15f,
                    0f, 1f, 0f);
            Matrix.multiplyMM(vp, 0, proj, 0, view, 0);
            GLES20.glUseProgram(program);

            drawStadium();
            drawPitch();
            drawGoals();
            drawPlayers();
            drawBall();
        }

        private void drawStadium() {
            float[] stand = {.16f,.18f,.22f,1f};
            float[] crowd1 = {.08f,.28f,.45f,1f};
            float[] crowd2 = {.50f,.12f,.20f,1f};
            drawCube(0,-1.1f,0,76,1.8f,118,new float[]{.11f,.12f,.14f,1});
            drawCube(-43,5,0,14,11,118,stand);
            drawCube(43,5,0,14,11,118,stand);
            drawCube(0,5,-65,72,11,16,stand);
            drawCube(0,5,65,72,11,16,stand);
            for (int z=-50; z<=50; z+=10) {
                drawCube(-39,4.5f,z,1.2f,5,7,(z/10)%2==0?crowd1:crowd2);
                drawCube(39,4.5f,z,1.2f,5,7,(z/10)%2==0?crowd2:crowd1);
            }
        }

        private void drawPitch() {
            float[] g1 = {.035f,.39f,.12f,1};
            float[] g2 = {.028f,.32f,.095f,1};
            for (int i=0;i<10;i++) {
                float z = -47.25f + i*10.5f;
                drawCube(0,-.06f,z,68,.12f,10.5f,(i%2==0)?g1:g2);
            }
            float[] white = {.90f,.94f,.90f,1};
            drawCube(0,.03f,-52.5f,68,.06f,.12f,white);
            drawCube(0,.03f,52.5f,68,.06f,.12f,white);
            drawCube(-34,.03f,0,.12f,.06f,105,white);
            drawCube(34,.03f,0,.12f,.06f,105,white);
            drawCube(0,.03f,0,68,.06f,.12f,white);
            drawCube(-10,.03f,-43, .12f,.06f,19,white);
            drawCube(10,.03f,-43, .12f,.06f,19,white);
            drawCube(0,.03f,-33.5f,20,.06f,.12f,white);
            drawCube(-10,.03f,43, .12f,.06f,19,white);
            drawCube(10,.03f,43, .12f,.06f,19,white);
            drawCube(0,.03f,33.5f,20,.06f,.12f,white);
            for (int i=0;i<32;i++) {
                double a = i * Math.PI * 2 / 32.0;
                float x = (float)Math.cos(a) * 9.15f;
                float z = (float)Math.sin(a) * 9.15f;
                drawCubeRotY(x,.035f,z,1.9f,.06f,.10f,-i*11.25f,white);
            }
        }

        private void drawGoals() {
            float[] w={.95f,.95f,.95f,1};
            drawCube(-7.3f,1.25f,-53.2f,.16f,2.5f,.16f,w);
            drawCube(7.3f,1.25f,-53.2f,.16f,2.5f,.16f,w);
            drawCube(0,2.5f,-53.2f,14.7f,.16f,.16f,w);
            drawCube(-7.3f,1.25f,53.2f,.16f,2.5f,.16f,w);
            drawCube(7.3f,1.25f,53.2f,.16f,2.5f,.16f,w);
            drawCube(0,2.5f,53.2f,14.7f,.16f,.16f,w);
        }

        private void drawPlayers() {
            float[] user={.72f,1.0f,.20f,1};
            float[] mate={.12f,.66f,.92f,1};
            float[] opp={.92f,.18f,.18f,1};
            float[] skin={.86f,.65f,.45f,1};
            float[] keeper={.98f,.76f,.12f,1};
            drawPerson(s.playerX,s.playerZ,user,skin,true);
            for(float[] p:s.mates) drawPerson(p[0],p[1],mate,skin,false);
            for(float[] p:s.opponents) drawPerson(p[0],p[1],opp,skin,false);
            drawPerson(0,-49.8f,keeper,skin,false);
            drawPerson(0,49.8f,keeper,skin,false);
        }

        private void drawPerson(float x,float z,float[] kit,float[] skin,boolean selected) {
            drawCube(x,1.55f,z,1.05f,1.55f,.72f,kit);
            drawCube(x,2.75f,z,.62f,.62f,.62f,skin);
            drawCube(x-.26f,.58f,z,.30f,1.05f,.34f,new float[]{.08f,.10f,.12f,1});
            drawCube(x+.26f,.58f,z,.30f,1.05f,.34f,new float[]{.08f,.10f,.12f,1});
            if(selected) drawCube(x,.04f,z,1.8f,.04f,1.8f,new float[]{.74f,1f,.16f,1});
        }

        private void drawBall() {
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,s.ballX,s.ballY,s.ballZ);
            Matrix.scaleM(model,0,.46f,.46f,.46f);
            Matrix.multiplyMM(mvp,0,vp,0,model,0);
            sphere.draw(posLoc,mvpLoc,colorLoc,mvp,new float[]{.96f,.96f,.96f,1});
        }

        private void drawCube(float x,float y,float z,float sx,float sy,float sz,float[] c) {
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,x,y,z);
            Matrix.scaleM(model,0,sx,sy,sz);
            Matrix.multiplyMM(mvp,0,vp,0,model,0);
            cube.draw(posLoc,mvpLoc,colorLoc,mvp,c);
        }

        private void drawCubeRotY(float x,float y,float z,float sx,float sy,float sz,float deg,float[] c) {
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,x,y,z);
            Matrix.rotateM(model,0,deg,0,1,0);
            Matrix.scaleM(model,0,sx,sy,sz);
            Matrix.multiplyMM(mvp,0,vp,0,model,0);
            cube.draw(posLoc,mvpLoc,colorLoc,mvp,c);
        }

        private static int link(String vs, String fs) {
            int v=compile(GLES20.GL_VERTEX_SHADER,vs), f=compile(GLES20.GL_FRAGMENT_SHADER,fs);
            int p=GLES20.glCreateProgram();
            GLES20.glAttachShader(p,v); GLES20.glAttachShader(p,f); GLES20.glLinkProgram(p);
            GLES20.glDeleteShader(v); GLES20.glDeleteShader(f);
            return p;
        }
        private static int compile(int type,String src) {
            int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,src); GLES20.glCompileShader(s); return s;
        }
    }

    static class Mesh {
        final FloatBuffer vertices;
        final int count;
        Mesh(float[] v) {
            count=v.length/3;
            vertices=ByteBuffer.allocateDirect(v.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            vertices.put(v).position(0);
        }
        void draw(int pos,int mvpLoc,int colorLoc,float[] mvp,float[] color) {
            vertices.position(0);
            GLES20.glEnableVertexAttribArray(pos);
            GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,vertices);
            GLES20.glUniformMatrix4fv(mvpLoc,1,false,mvp,0);
            GLES20.glUniform4fv(colorLoc,1,color,0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
            GLES20.glDisableVertexAttribArray(pos);
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
            return new Mesh(v);
        }
        static Mesh sphere(int lat,int lon) {
            float[] out=new float[lat*lon*6*3]; int k=0;
            for(int i=0;i<lat;i++){
                float a1=(float)(-Math.PI/2 + Math.PI*i/lat), a2=(float)(-Math.PI/2 + Math.PI*(i+1)/lat);
                for(int j=0;j<lon;j++){
                    float b1=(float)(2*Math.PI*j/lon), b2=(float)(2*Math.PI*(j+1)/lon);
                    float[] p1={cos(a1)*cos(b1),sin(a1),cos(a1)*sin(b1)};
                    float[] p2={cos(a2)*cos(b1),sin(a2),cos(a2)*sin(b1)};
                    float[] p3={cos(a2)*cos(b2),sin(a2),cos(a2)*sin(b2)};
                    float[] p4={cos(a1)*cos(b2),sin(a1),cos(a1)*sin(b2)};
                    float[][] tri={p1,p2,p3,p1,p3,p4};
                    for(float[] p:tri){out[k++]=p[0];out[k++]=p[1];out[k++]=p[2];}
                }
            }
            return new Mesh(out);
        }
        static float sin(float a){return (float)Math.sin(a);} static float cos(float a){return (float)Math.cos(a);}
    }

    static class HudView extends View {
        final GameState s;
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        float joyCx,joyCy,joyR;
        int joyPointer=-1;
        float knobX,knobY;

        HudView(Context c,GameState state){super(c);s=state;setBackgroundColor(Color.TRANSPARENT);}

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight();
            joyCx=115; joyCy=h-115; joyR=78;
            if(joyPointer<0){knobX=joyCx;knobY=joyCy;}

            p.setStyle(Paint.Style.FILL); p.setColor(Color.argb(185,5,12,18));
            c.drawRoundRect(w*.36f,18,w*.64f,82,18,18,p);
            text(c,"AUR  "+s.homeScore+"  -  "+s.awayScore+"  VIL",w*.43f,55,23,Color.WHITE);
            text(c,String.format("%02d'",(int)s.minute),w*.37f,55,18,Color.rgb(205,255,85));
            text(c,"futREAL 3D",22,42,22,Color.WHITE);
            text(c,"BROADCAST CAM",22,66,12,Color.rgb(205,255,85));

            p.setColor(Color.argb(85,255,255,255)); c.drawCircle(joyCx,joyCy,joyR,p);
            p.setColor(Color.argb(165,205,255,85)); c.drawCircle(knobX,knobY,32,p);

            float shootX=w-105,shootY=h-155,shootR=62;
            float passX=w-240,passY=h-100,passR=52;
            float sprintX=w-270,sprintY=h-220,sprintR=45;
            button(c,shootX,shootY,shootR,"CHUTE",Color.argb(205,205,255,85),Color.rgb(5,20,12));
            button(c,passX,passY,passR,"PASSE",Color.argb(190,255,255,255),Color.rgb(8,22,30));
            button(c,sprintX,sprintY,sprintR,"SPRINT",Color.argb(s.sprint?220:120,60,180,255),Color.WHITE);

            float barW=170, x=w-205, y=26;
            p.setColor(Color.argb(160,5,10,15)); c.drawRoundRect(x,y,x+barW,y+18,9,9,p);
            p.setColor(Color.rgb(205,255,85)); c.drawRoundRect(x,y,x+barW*(s.stamina/100f),y+18,9,9,p);
            text(c,"STAMINA",x-66,y+15,12,Color.WHITE);

            drawMiniMap(c,w*.50f,h-55,145,36);
            postInvalidateOnAnimation();
        }

        private void drawMiniMap(Canvas c,float cx,float cy,float mw,float mh){
            p.setColor(Color.argb(150,3,25,12)); c.drawRoundRect(cx-mw/2,cy-mh/2,cx+mw/2,cy+mh/2,8,8,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f); p.setColor(Color.argb(170,255,255,255));
            c.drawRect(cx-mw/2+3,cy-mh/2+3,cx+mw/2-3,cy+mh/2-3,p); p.setStyle(Paint.Style.FILL);
            dot(c,cx+s.playerX/68f*mw,cy+s.playerZ/105f*mh,4,Color.rgb(205,255,85));
            for(float[] q:s.mates) dot(c,cx+q[0]/68f*mw,cy+q[1]/105f*mh,3,Color.CYAN);
            for(float[] q:s.opponents) dot(c,cx+q[0]/68f*mw,cy+q[1]/105f*mh,3,Color.RED);
        }
        private void dot(Canvas c,float x,float y,float r,int col){p.setColor(col);c.drawCircle(x,y,r,p);}
        private void button(Canvas c,float x,float y,float r,String label,int bg,int fg){p.setColor(bg);c.drawCircle(x,y,r,p);textCenter(c,label,x,y+5,14,fg);}
        private void text(Canvas c,String t,float x,float y,float size,int col){p.setColor(col);p.setTextSize(size);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);c.drawText(t,x,y,p);}
        private void textCenter(Canvas c,String t,float x,float y,float size,int col){p.setColor(col);p.setTextSize(size);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);c.drawText(t,x,y,p);p.setTextAlign(Paint.Align.LEFT);}

        @Override public boolean onTouchEvent(MotionEvent e){
            int action=e.getActionMasked(); int idx=e.getActionIndex();
            if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN){
                float x=e.getX(idx),y=e.getY(idx); int id=e.getPointerId(idx);
                if(x<getWidth()*.43f && y>getHeight()*.42f && joyPointer<0){joyPointer=id;updateJoy(x,y);}
                else handleButtonDown(x,y);
            } else if(action==MotionEvent.ACTION_MOVE){
                boolean sprintHeld=false;
                for(int i=0;i<e.getPointerCount();i++){
                    int id=e.getPointerId(i); float x=e.getX(i),y=e.getY(i);
                    if(id==joyPointer) updateJoy(x,y);
                    if(inCircle(x,y,getWidth()-270,getHeight()-220,58)) sprintHeld=true;
                }
                s.sprint=sprintHeld;
            } else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP || action==MotionEvent.ACTION_CANCEL){
                int id=e.getPointerId(idx);
                if(id==joyPointer){joyPointer=-1;s.joyX=s.joyY=0;knobX=joyCx;knobY=joyCy;}
                if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) s.sprint=false;
            }
            invalidate(); return true;
        }
        private void handleButtonDown(float x,float y){
            float w=getWidth(),h=getHeight();
            if(inCircle(x,y,w-105,h-155,78)) s.shootRequest=true;
            else if(inCircle(x,y,w-240,h-100,68)) s.passRequest=true;
            else if(inCircle(x,y,w-270,h-220,62)) s.sprint=true;
        }
        private void updateJoy(float x,float y){
            float dx=x-joyCx,dy=y-joyCy,d=(float)Math.sqrt(dx*dx+dy*dy);
            if(d>joyR){dx=dx/d*joyR;dy=dy/d*joyR;}
            knobX=joyCx+dx;knobY=joyCy+dy;s.joyX=dx/joyR;s.joyY=dy/joyR;
        }
        private static boolean inCircle(float x,float y,float cx,float cy,float r){float dx=x-cx,dy=y-cy;return dx*dx+dy*dy<=r*r;}
    }
}