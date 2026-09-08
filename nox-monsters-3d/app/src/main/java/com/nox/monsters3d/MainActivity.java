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
import java.util.Random;

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
        v.setPadding(18, 11, 18, 11);
        return v;
    }

    private Button makeButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(16f);
        b.setAllCaps(false);
        b.setBackground(rounded(Color.rgb(22, 67, 108), Color.rgb(102, 205, 255), 32f));
        return b;
    }

    private void showMenu() {
        if (glView != null) {
            glView.onPause();
            glView = null;
            renderer = null;
        }
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(4, 13, 24));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(48, 38, 48, 38);
        card.setBackground(rounded(Color.rgb(9, 31, 56), Color.rgb(64, 185, 244), 46f));

        TextView logo = text("N✦", 62f, Color.rgb(255, 219, 70));
        TextView title = text("NOX MONSTERS 3D", 32f, Color.WHITE);
        TextView sub = text("Edição ULTRA • mapa maior • 12 criaturas • mundo 3D", 15f,
                Color.rgb(193, 224, 243));
        Button play = makeButton("JOGAR");
        play.setOnClickListener(v -> startGame());

        card.addView(logo, new LinearLayout.LayoutParams(-1, -2));
        card.addView(title, new LinearLayout.LayoutParams(-1, -2));
        card.addView(sub, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, 24, 0, 0);
        card.addView(play, bp);

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().widthPixels * .70f), -2);
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

        hud = text("Capturados: 0/12", 15f, Color.WHITE);
        hud.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        hud.setBackground(rounded(0xB9081727, Color.rgb(88, 181, 236), 24f));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2);
        hp.gravity = Gravity.TOP | Gravity.LEFT;
        hp.setMargins(20, 14, 0, 0);
        root.addView(hud, hp);

        message = text("Explore a vila e encontre as criaturas!", 14f, Color.WHITE);
        message.setBackground(rounded(0xB9081727, Color.rgb(88, 181, 236), 24f));
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(-2, -2);
        mp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        mp.setMargins(0, 14, 0, 0);
        root.addView(message, mp);

        Button menu = makeButton("← MENU");
        menu.setTextSize(13f);
        menu.setOnClickListener(v -> showMenu());
        FrameLayout.LayoutParams menup = new FrameLayout.LayoutParams(-2, 60);
        menup.gravity = Gravity.TOP | Gravity.RIGHT;
        menup.setMargins(0, 12, 18, 0);
        root.addView(menu, menup);

        JoystickView joystick = new JoystickView();
        FrameLayout.LayoutParams jp = new FrameLayout.LayoutParams(220, 220);
        jp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        jp.setMargins(26, 0, 0, 26);
        root.addView(joystick, jp);

        Button capture = makeButton("CAPTURAR");
        GradientDrawable cap = new GradientDrawable();
        cap.setShape(GradientDrawable.OVAL);
        cap.setColor(0xE6E3443B);
        cap.setStroke(5, 0xFFFFFFFF);
        capture.setBackground(cap);
        capture.setOnClickListener(v -> {
            if (glView != null && renderer != null) glView.queueEvent(() -> renderer.capture());
        });
        FrameLayout.LayoutParams capP = new FrameLayout.LayoutParams(178, 178);
        capP.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        capP.setMargins(0, 0, 34, 38);
        root.addView(capture, capP);

        TextView hint = text("Analógico: mover  •  chegue perto e toque CAPTURAR", 12f, 0xEEFFFFFF);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-2, -2);
        ip.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        ip.setMargins(0, 0, 0, 8);
        root.addView(hint, ip);

        setContentView(root);
        glView.onResume();
    }

    private void updateHud(int caught, String msg) {
        runOnUiThread(() -> {
            if (hud != null) hud.setText("Capturados: " + caught + "/12");
            if (message != null && msg != null) message.setText(msg);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        immersive();
        if (glView != null) glView.onResume();
    }

    @Override protected void onPause() {
        if (glView != null) glView.onPause();
        super.onPause();
    }

    @Override public void onBackPressed() {
        if (glView != null) showMenu(); else super.onBackPressed();
    }

    private final class JoystickView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float knobX = 110f, knobY = 110f;
        JoystickView() { super(MainActivity.this); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth()/2f, cy = getHeight()/2f;
            float r = Math.min(cx, cy)-8f;
            p.setColor(0x66325E7C); c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5f); p.setColor(0xE5FFFFFF);
            c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.FILL); p.setColor(0xE8FFFFFF);
            c.drawCircle(knobX, knobY, r*.34f, p);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float cx=getWidth()/2f, cy=getHeight()/2f, r=Math.min(cx,cy)-12f;
            if (e.getActionMasked()==MotionEvent.ACTION_UP || e.getActionMasked()==MotionEvent.ACTION_CANCEL) {
                knobX=cx; knobY=cy; if(renderer!=null) renderer.setMove(0,0); invalidate(); return true;
            }
            float dx=e.getX()-cx, dy=e.getY()-cy;
            float len=(float)Math.sqrt(dx*dx+dy*dy);
            if(len>r){ dx=dx/len*r; dy=dy/len*r; }
            knobX=cx+dx; knobY=cy+dy;
            if(renderer!=null) renderer.setMove(dx/r,dy/r);
            invalidate(); return true;
        }
    }

    private final class GameRenderer implements GLSurfaceView.Renderer {
        private final float[] projection=new float[16], view=new float[16], model=new float[16], mv=new float[16], mvp=new float[16];
        private final List<Creature> creatures=new ArrayList<>();
        private final List<Tree> trees=new ArrayList<>();
        private final List<Rock> rocks=new ArrayList<>();
        private final List<House> houses=new ArrayList<>();
        private final List<Grass> grasses=new ArrayList<>();
        private FloatBuffer cubeBuffer;
        private int program,aPosition,aNormal,uMvp,uModel,uColor,uLight;
        private volatile float moveX,moveZ;
        private float playerX=0,playerZ=3,playerYaw=180,time,compX=-1.1f,compZ=4f,capturePulse;
        private long lastNs;
        private int caught;
        private final Random rnd=new Random(77);

        private final float[] cube={
                -0.5f,-0.5f,0.5f,0,0,1, 0.5f,-0.5f,0.5f,0,0,1, 0.5f,0.5f,0.5f,0,0,1,
                -0.5f,-0.5f,0.5f,0,0,1, 0.5f,0.5f,0.5f,0,0,1, -0.5f,0.5f,0.5f,0,0,1,
                0.5f,-0.5f,-0.5f,0,0,-1, -0.5f,-0.5f,-0.5f,0,0,-1, -0.5f,0.5f,-0.5f,0,0,-1,
                0.5f,-0.5f,-0.5f,0,0,-1, -0.5f,0.5f,-0.5f,0,0,-1, 0.5f,0.5f,-0.5f,0,0,-1,
                -0.5f,-0.5f,-0.5f,-1,0,0, -0.5f,-0.5f,0.5f,-1,0,0, -0.5f,0.5f,0.5f,-1,0,0,
                -0.5f,-0.5f,-0.5f,-1,0,0, -0.5f,0.5f,0.5f,-1,0,0, -0.5f,0.5f,-0.5f,-1,0,0,
                0.5f,-0.5f,0.5f,1,0,0, 0.5f,-0.5f,-0.5f,1,0,0, 0.5f,0.5f,-0.5f,1,0,0,
                0.5f,-0.5f,0.5f,1,0,0, 0.5f,0.5f,-0.5f,1,0,0, 0.5f,0.5f,0.5f,1,0,0,
                -0.5f,0.5f,0.5f,0,1,0, 0.5f,0.5f,0.5f,0,1,0, 0.5f,0.5f,-0.5f,0,1,0,
                -0.5f,0.5f,0.5f,0,1,0, 0.5f,0.5f,-0.5f,0,1,0, -0.5f,0.5f,-0.5f,0,1,0,
                -0.5f,-0.5f,-0.5f,0,-1,0, 0.5f,-0.5f,-0.5f,0,-1,0, 0.5f,-0.5f,0.5f,0,-1,0,
                -0.5f,-0.5f,-0.5f,0,-1,0, 0.5f,-0.5f,0.5f,0,-1,0, -0.5f,-0.5f,0.5f,0,-1,0
        };

        GameRenderer(){ seedWorld(); }
        void setMove(float x,float z){ moveX=x; moveZ=z; }

        private void seedWorld(){
            creatures.add(new Creature("Voltik",-8,-7,rgb(1f,.78f,.14f),0));
            creatures.add(new Creature("Mossy",8,-8,rgb(.16f,.80f,.30f),1));
            creatures.add(new Creature("Bubloo",-14,9,rgb(.12f,.52f,1f),2));
            creatures.add(new Creature("Emberu",13,10,rgb(1f,.25f,.10f),3));
            creatures.add(new Creature("Pebbit",-5,-16,rgb(.60f,.57f,.50f),4));
            creatures.add(new Creature("Lumii",6,17,rgb(.86f,.34f,1f),5));
            creatures.add(new Creature("Spriggo",18,-13,rgb(.18f,.92f,.55f),6));
            creatures.add(new Creature("Nimbu",-18,-12,rgb(.82f,.92f,1f),7));
            creatures.add(new Creature("Cindro",20,7,rgb(.94f,.39f,.13f),8));
            creatures.add(new Creature("Aqualo",-20,4,rgb(.08f,.70f,.96f),9));
            creatures.add(new Creature("Glimmi",11,21,rgb(1f,.55f,.82f),10));
            creatures.add(new Creature("Thornix",-12,21,rgb(.30f,.72f,.18f),11));

            for(int i=0;i<50;i++){
                float x=rnd.nextFloat()*58f-29f, z=rnd.nextFloat()*58f-29f;
                if(Math.abs(x)<5.5f || (x>-15&&x<-6&&z>5&&z<15)) { i--; continue; }
                trees.add(new Tree(x,z,.75f+rnd.nextFloat()*.65f));
            }
            for(int i=0;i<34;i++){
                float x=rnd.nextFloat()*54f-27f, z=rnd.nextFloat()*54f-27f;
                if(Math.abs(x)<4f) { i--; continue; }
                rocks.add(new Rock(x,z,.45f+rnd.nextFloat()*.75f));
            }
            for(int i=0;i<90;i++){
                float x=rnd.nextFloat()*58f-29f, z=rnd.nextFloat()*58f-29f;
                if(Math.abs(x)<3.8f) { i--; continue; }
                grasses.add(new Grass(x,z,.25f+rnd.nextFloat()*.35f));
            }
            houses.add(new House(-10,8,rgb(.95f,.67f,.36f),rgb(.72f,.18f,.14f)));
            houses.add(new House(-10,15,rgb(.84f,.90f,.98f),rgb(.20f,.42f,.70f)));
            houses.add(new House(10,8,rgb(.95f,.84f,.55f),rgb(.66f,.24f,.12f)));
            houses.add(new House(10,15,rgb(.78f,.93f,.79f),rgb(.18f,.50f,.27f)));
            houses.add(new House(-18,-1,rgb(.89f,.77f,.95f),rgb(.49f,.20f,.62f)));
            houses.add(new House(18,-1,rgb(.96f,.79f,.73f),rgb(.72f,.25f,.19f)));
        }

        @Override public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config){
            GLES20.glClearColor(.20f,.61f,.92f,1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);
            cubeBuffer=ByteBuffer.allocateDirect(cube.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            cubeBuffer.put(cube).position(0);
            String vs="uniform mat4 uMVP; uniform mat4 uModel; attribute vec3 aPosition; attribute vec3 aNormal; varying vec3 vNormal; void main(){ gl_Position=uMVP*vec4(aPosition,1.0); vNormal=normalize(mat3(uModel)*aNormal); }";
            String fs="precision mediump float; uniform vec4 uColor; uniform vec3 uLight; varying vec3 vNormal; void main(){ float d=max(dot(normalize(vNormal),normalize(uLight)),0.0); float l=.38+d*.62; gl_FragColor=vec4(uColor.rgb*l,uColor.a); }";
            program=link(vs,fs);
            aPosition=GLES20.glGetAttribLocation(program,"aPosition");
            aNormal=GLES20.glGetAttribLocation(program,"aNormal");
            uMvp=GLES20.glGetUniformLocation(program,"uMVP");
            uModel=GLES20.glGetUniformLocation(program,"uModel");
            uColor=GLES20.glGetUniformLocation(program,"uColor");
            uLight=GLES20.glGetUniformLocation(program,"uLight");
            lastNs=System.nanoTime();
        }

        @Override public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl,int width,int height){
            GLES20.glViewport(0,0,width,height);
            Matrix.perspectiveM(projection,0,55f,width/(float)Math.max(1,height),.35f,120f);
        }

        @Override public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl){
            long now=System.nanoTime(); float dt=Math.min(.035f,(now-lastNs)/1_000_000_000f); lastNs=now; time+=dt;
            float mx=moveX,mz=moveZ;
            if(Math.abs(mx)+Math.abs(mz)>.03f){
                playerX=clamp(playerX+mx*7.4f*dt,-28f,28f);
                playerZ=clamp(playerZ+mz*7.4f*dt,-28f,28f);
                playerYaw=(float)Math.toDegrees(Math.atan2(mx,mz));
            }
            compX += ((playerX-1.15f)-compX)*Math.min(1f,dt*5.2f);
            compZ += ((playerZ+1.05f)-compZ)*Math.min(1f,dt*5.2f);
            capturePulse=Math.max(0,capturePulse-dt);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glUseProgram(program);
            Matrix.setLookAtM(view,0,playerX+10.8f,8.8f,playerZ+12.8f,playerX,1.15f,playerZ,0,1,0);

            drawBox(0,-.42f,0,64,.8f,64,0,rgb(.18f,.58f,.22f));
            drawBox(0,-.01f,0,5.2f,.07f,62,0,rgb(.72f,.61f,.38f));
            drawBox(0,.00f,12,27,.08f,4.2f,0,rgb(.72f,.61f,.38f));
            drawBox(-22,.02f,-19,11,.11f,8,0,rgb(.08f,.52f,.78f));
            drawBox(-22,.10f,-19,8.5f,.05f,2.0f,0,rgb(.55f,.38f,.20f));

            drawMountains();
            for(House h:houses) drawHouse(h);
            for(Tree t:trees) drawTree(t);
            for(Rock r:rocks) drawRock(r);
            for(Grass g:grasses) drawGrass(g);
            drawLamp(-2.6f,4); drawLamp(2.6f,4); drawLamp(-2.6f,12); drawLamp(2.6f,12); drawLamp(-2.6f,20); drawLamp(2.6f,20);
            synchronized(creatures){ for(Creature c:creatures) drawCreature(c); }
            drawCompanion();
            drawPlayer();
        }

        private void drawMountains(){
            float[] c1=rgb(.23f,.38f,.29f), c2=rgb(.31f,.44f,.34f);
            for(int i=-30;i<=30;i+=8){
                drawBox(i,3.0f,-31f,7.5f,6f,4.2f,12f,c1);
                drawBox(i+3f,4.5f,-33f,6f,9f,4f,-8f,c2);
            }
        }

        private void drawHouse(House h){
            shadow(h.x,h.z,5.2f,4.8f);
            drawBox(h.x,1.6f,h.z,4.6f,3.2f,4.2f,0,h.wall);
            drawBox(h.x,3.45f,h.z,5.1f,.65f,4.8f,0,h.roof);
            drawBox(h.x,3.85f,h.z,4.1f,.65f,3.8f,0,lighten(h.roof,.08f));
            drawBox(h.x,1.1f,h.z-2.14f,1.0f,2.2f,.20f,0,rgb(.34f,.20f,.11f));
            drawBox(h.x-1.3f,1.8f,h.z-2.15f,.85f,1.05f,.16f,0,rgb(.42f,.78f,.95f));
            drawBox(h.x+1.3f,1.8f,h.z-2.15f,.85f,1.05f,.16f,0,rgb(.42f,.78f,.95f));
        }

        private void drawTree(Tree t){
            shadow(t.x,t.z,2.4f,2.2f);
            drawBox(t.x,1.35f*t.s,t.z,.62f*t.s,2.7f*t.s,.62f*t.s,0,rgb(.33f,.19f,.08f));
            drawBox(t.x,3.0f*t.s,t.z,2.35f*t.s,1.85f*t.s,2.35f*t.s,0,rgb(.06f,.40f,.13f));
            drawBox(t.x+.65f*t.s,3.75f*t.s,t.z-.35f*t.s,1.35f*t.s,1.20f*t.s,1.35f*t.s,18f,rgb(.09f,.55f,.18f));
            drawBox(t.x-.65f*t.s,3.60f*t.s,t.z+.28f*t.s,1.45f*t.s,1.15f*t.s,1.45f*t.s,-12f,rgb(.08f,.50f,.16f));
        }

        private void drawRock(Rock r){
            shadow(r.x,r.z,1.3f*r.s,1.0f*r.s);
            drawBox(r.x,.35f*r.s,r.z,1.2f*r.s,.70f*r.s,.95f*r.s,18f,rgb(.45f,.47f,.47f));
            drawBox(r.x-.25f*r.s,.72f*r.s,r.z+.10f*r.s,.65f*r.s,.55f*r.s,.60f*r.s,-20f,rgb(.55f,.56f,.55f));
        }

        private void drawGrass(Grass g){
            float[] c=rgb(.10f,.72f,.18f);
            drawBox(g.x,.22f,g.z,.10f,.44f,.10f,-18f,c);
            drawBox(g.x+.13f,.20f,g.z+.04f,.09f,.40f,.09f,16f,c);
            drawBox(g.x-.12f,.18f,g.z-.03f,.08f,.36f,.08f,28f,c);
        }

        private void drawLamp(float x,float z){
            drawBox(x,1.4f,z,.16f,2.8f,.16f,0,rgb(.17f,.19f,.22f));
            drawBox(x,2.9f,z,.55f,.42f,.55f,0,rgb(1f,.84f,.28f));
        }

        private void drawPlayer(){
            boolean walking=Math.abs(moveX)+Math.abs(moveZ)>.06f;
            float swing=walking?(float)Math.sin(time*10f)*22f:0f;
            shadow(playerX,playerZ,1.25f,.75f);
            float[] shirt=rgb(.06f,.16f,.24f),skin=rgb(.92f,.70f,.50f),pants=rgb(.07f,.28f,.62f),shoe=rgb(.10f,.10f,.12f);
            drawBox(playerX,1.34f,playerZ,.92f,1.12f,.55f,playerYaw,shirt);
            drawBox(playerX,2.25f,playerZ,.72f,.72f,.68f,playerYaw,skin);
            drawBox(playerX,2.67f,playerZ,.82f,.20f,.75f,playerYaw,rgb(.12f,.12f,.15f));
            drawBox(playerX-.27f,.60f,playerZ,.28f,.84f,.36f,playerYaw+swing,pants);
            drawBox(playerX+.27f,.60f,playerZ,.28f,.84f,.36f,playerYaw-swing,pants);
            drawBox(playerX-.28f,.15f,playerZ-.10f,.34f,.20f,.55f,playerYaw,shoe);
            drawBox(playerX+.28f,.15f,playerZ-.10f,.34f,.20f,.55f,playerYaw,shoe);
            drawBox(playerX-.59f,1.35f,playerZ,.24f,.85f,.28f,playerYaw-swing,skin);
            drawBox(playerX+.59f,1.35f,playerZ,.24f,.85f,.28f,playerYaw+swing,skin);
        }

        private void drawCompanion(){
            float bob=(float)Math.sin(time*4f)*.08f;
            shadow(compX,compZ,1.0f,.75f);
            float[] body=rgb(.98f,.70f,.12f),light=rgb(1f,.88f,.35f),dark=rgb(.45f,.24f,.06f);
            drawBox(compX,.72f+bob,compZ,1.0f,.80f,.88f,0,body);
            drawBox(compX,.12f+bob,compZ,.72f,.34f,.72f,0,dark);
            drawBox(compX,.98f+bob,compZ-.25f,.78f,.65f,.60f,0,light);
            drawBox(compX-.27f,1.48f+bob,compZ-.16f,.22f,.55f,.22f,-18f,body);
            drawBox(compX+.27f,1.48f+bob,compZ-.16f,.22f,.55f,.22f,18f,body);
            drawBox(compX-.20f,1.02f+bob,compZ-.57f,.12f,.12f,.08f,0,rgb(.03f,.04f,.05f));
            drawBox(compX+.20f,1.02f+bob,compZ-.57f,.12f,.12f,.08f,0,rgb(.03f,.04f,.05f));
        }

        private void drawCreature(Creature c){
            float bob=(float)Math.sin(time*2.5f+c.phase)*.12f;
            float y=.66f+bob;
            shadow(c.x,c.z,1.5f,1.2f);
            drawBox(c.x,y+.48f,c.z,1.35f,1.02f,1.15f,0,c.color);
            drawBox(c.x,y+1.34f,c.z-.18f,.98f,.86f,.88f,0,lighten(c.color,.14f));
            drawBox(c.x-.34f,y+1.92f,c.z-.10f,.27f,.62f,.25f,-18f,c.color);
            drawBox(c.x+.34f,y+1.92f,c.z-.10f,.27f,.62f,.25f,18f,c.color);
            drawBox(c.x-.22f,y+1.38f,c.z-.63f,.13f,.13f,.08f,0,rgb(.03f,.04f,.05f));
            drawBox(c.x+.22f,y+1.38f,c.z-.63f,.13f,.13f,.08f,0,rgb(.03f,.04f,.05f));
            drawBox(c.x-.38f,y+.03f,c.z+.12f,.34f,.28f,.48f,0,darken(c.color,.12f));
            drawBox(c.x+.38f,y+.03f,c.z+.12f,.34f,.28f,.48f,0,darken(c.color,.12f));
            drawBox(c.x+.82f,y+.65f,c.z+.12f,.55f,.22f,.22f,30f,c.color);
            if(capturePulse>0){
                float s=2.0f+(1f-capturePulse)*2f;
                drawBox(c.x,.08f,c.z,s,.06f,s,0,rgb(1f,.85f,.18f));
            }
        }

        void capture(){
            Creature best=null; float bestD=999f;
            synchronized(creatures){
                for(Creature c:creatures){ float dx=c.x-playerX,dz=c.z-playerZ,d=(float)Math.sqrt(dx*dx+dz*dz); if(d<bestD){bestD=d;best=c;} }
                if(best==null){ updateHud(caught,"Coleção completa! 🏆"); return; }
                if(bestD<=3.6f){ creatures.remove(best); caught++; capturePulse=.9f; updateHud(caught,best.name+" capturado! ✨"); if(caught==12) updateHud(caught,"Você capturou todas as 12 criaturas! 🏆"); }
                else updateHud(caught,"Chegue mais perto da criatura");
            }
        }

        private void shadow(float x,float z,float sx,float sz){ drawBox(x,.015f,z,sx,.03f,sz,0,rgb(.06f,.15f,.06f)); }

        private void drawBox(float x,float y,float z,float sx,float sy,float sz,float yaw,float[] color){
            Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.rotateM(model,0,yaw,0,1,0); Matrix.scaleM(model,0,sx,sy,sz);
            Matrix.multiplyMM(mv,0,view,0,model,0); Matrix.multiplyMM(mvp,0,projection,0,mv,0);
            GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0); GLES20.glUniformMatrix4fv(uModel,1,false,model,0);
            GLES20.glUniform4f(uColor,color[0],color[1],color[2],1f); GLES20.glUniform3f(uLight,-.35f,.90f,.50f);
            cubeBuffer.position(0); GLES20.glEnableVertexAttribArray(aPosition); GLES20.glVertexAttribPointer(aPosition,3,GLES20.GL_FLOAT,false,24,cubeBuffer);
            cubeBuffer.position(3); GLES20.glEnableVertexAttribArray(aNormal); GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,24,cubeBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,36);
        }

        private int link(String vs,String fs){
            int v=compile(GLES20.GL_VERTEX_SHADER,vs), f=compile(GLES20.GL_FRAGMENT_SHADER,fs);
            int p=GLES20.glCreateProgram(); GLES20.glAttachShader(p,v); GLES20.glAttachShader(p,f); GLES20.glLinkProgram(p); return p;
        }
        private int compile(int type,String src){ int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,src); GLES20.glCompileShader(s); return s; }
    }

    private static final class Creature { final String name; final float x,z,phase; final float[] color; Creature(String n,float x,float z,float[] c,float p){name=n;this.x=x;this.z=z;color=c;phase=p;} }
    private static final class Tree { final float x,z,s; Tree(float x,float z,float s){this.x=x;this.z=z;this.s=s;} }
    private static final class Rock { final float x,z,s; Rock(float x,float z,float s){this.x=x;this.z=z;this.s=s;} }
    private static final class Grass { final float x,z,s; Grass(float x,float z,float s){this.x=x;this.z=z;this.s=s;} }
    private static final class House { final float x,z; final float[] wall,roof; House(float x,float z,float[] w,float[] r){this.x=x;this.z=z;wall=w;roof=r;} }

    private static float[] rgb(float r,float g,float b){ return new float[]{r,g,b}; }
    private static float[] lighten(float[] c,float v){ return rgb(clamp(c[0]+v,0,1),clamp(c[1]+v,0,1),clamp(c[2]+v,0,1)); }
    private static float[] darken(float[] c,float v){ return rgb(clamp(c[0]-v,0,1),clamp(c[1]-v,0,1),clamp(c[2]-v,0,1)); }
    private static float clamp(float v,float a,float b){ return Math.max(a,Math.min(b,v)); }
}
