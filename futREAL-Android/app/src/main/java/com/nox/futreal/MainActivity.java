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
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        hideBars();
        setContentView(new FutRealView(this));
    }

    private void hideBars() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideBars();
    }

    static class FutRealView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final SharedPreferences prefs;
        private int screen = 0; // 0 home, 1 career, 2 training, 3 match
        private int games, goals, wins, points, speed, shot, pass;
        private int homeScore, awayScore;
        private float matchTime;
        private long lastNs;
        private float playerX = .50f, playerY = .78f;
        private float ballX = .50f, ballY = .74f, ballVX, ballVY;
        private boolean hasBall = true, sprint;
        private float joyX, joyY;
        private final float[] ox = {.20f,.38f,.60f,.80f,.50f};
        private final float[] oy = {.34f,.31f,.30f,.35f,.18f};
        private final float[] mx = {.24f,.76f,.40f,.63f};
        private final float[] my = {.66f,.63f,.49f,.46f};
        private RectF playCareer = new RectF(), quick = new RectF(), training = new RectF(), profile = new RectF();
        private RectF careerPlay = new RectF(), back = new RectF();
        private RectF trainSpeed = new RectF(), trainShot = new RectF(), trainPass = new RectF();

        FutRealView(Context c) {
            super(c);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            prefs = c.getSharedPreferences("futreal", Context.MODE_PRIVATE);
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
            prefs.edit().putInt("games", games).putInt("goals", goals).putInt("wins", wins)
                    .putInt("points", points).putInt("speed", speed).putInt("shot", shot)
                    .putInt("pass", pass).apply();
        }

        private int ovr() { return Math.round((speed + shot + pass) / 3f); }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            if (screen == 3) {
                updateMatch();
                drawMatch(c);
                postInvalidateOnAnimation();
            } else if (screen == 1) drawCareer(c);
            else if (screen == 2) drawTraining(c);
            else drawHome(c);
        }

        private void bg(Canvas c) {
            c.drawColor(Color.rgb(5, 15, 10));
            p.setColor(Color.rgb(12, 34, 24));
            c.drawRect(0, 0, getWidth(), getHeight(), p);
            p.setColor(Color.argb(70, 202, 255, 85));
            c.drawCircle(getWidth() * .86f, getHeight() * .15f, getHeight() * .32f, p);
        }

        private void text(Canvas c, String s, float x, float y, float size, int color) {
            p.setTextSize(size); p.setColor(color); p.setStyle(Paint.Style.FILL);
            c.drawText(s, x, y, p);
        }

        private void round(Canvas c, RectF r, int color, float rad) {
            p.setStyle(Paint.Style.FILL); p.setColor(color); c.drawRoundRect(r, rad, rad, p);
        }

        private void drawBrand(Canvas c) {
            text(c, "fut", 32, 54, 34, Color.WHITE);
            text(c, "REAL", 85, 54, 34, Color.rgb(202,255,85));
        }

        private void drawHome(Canvas c) {
            bg(c); drawBrand(c);
            float w = getWidth(), h = getHeight();
            text(c, "FUTEBOL MOBILE", 34, h * .24f, 18, Color.rgb(202,255,85));
            text(c, "Sua carreira. Seu futebol.", 34, h * .34f, 38, Color.WHITE);
            text(c, "Jogue, evolua e vire estrela no futREAL.", 34, h * .41f, 18, Color.LTGRAY);

            float x = w * .46f, y = h * .18f, bw = w * .23f, bh = h * .25f, gap = 18;
            playCareer.set(x, y, x+bw, y+bh);
            quick.set(x+bw+gap, y, x+bw*2+gap, y+bh);
            training.set(x, y+bh+gap, x+bw, y+bh*2+gap);
            profile.set(x+bw+gap, y+bh+gap, x+bw*2+gap, y+bh*2+gap);
            drawMenuCard(c, playCareer, "★", "MODO CARREIRA", "Sua temporada", true);
            drawMenuCard(c, quick, "⚽", "PARTIDA RÁPIDA", "Entre em campo", false);
            drawMenuCard(c, training, "+", "TREINO", "Melhore atributos", false);
            drawMenuCard(c, profile, "OVR", "MEU JOGADOR", "Overall " + ovr(), false);
        }

        private void drawMenuCard(Canvas c, RectF r, String icon, String title, String sub, boolean hot) {
            round(c, r, hot ? Color.rgb(24,63,39) : Color.rgb(16,44,31), 24);
            text(c, icon, r.left+22, r.top+42, 24, Color.rgb(202,255,85));
            text(c, title, r.left+22, r.centerY()+4, 20, Color.WHITE);
            text(c, sub, r.left+22, r.centerY()+34, 14, Color.LTGRAY);
        }

        private void drawCareer(Canvas c) {
            bg(c); drawBrand(c); float w=getWidth(), h=getHeight();
            back.set(22,72,92,126); round(c,back,Color.rgb(18,48,34),18); text(c,"‹",44,112,40,Color.WHITE);
            text(c,"MODO CARREIRA",112,112,28,Color.WHITE);
            RectF hero=new RectF(32,150,w*.58f,h*.48f); round(c,hero,Color.rgb(17,52,35),26);
            text(c,"TEMPORADA 1",hero.left+24,hero.top+38,16,Color.rgb(202,255,85));
            text(c,"NOX JR.",hero.left+24,hero.top+88,34,Color.WHITE);
            text(c,"Atlético Aurora • ATA",hero.left+24,hero.top+122,18,Color.LTGRAY);
            text(c,"OVR " + ovr(),hero.right-130,hero.top+76,28,Color.rgb(202,255,85));
            careerPlay.set(hero.left+24,hero.bottom-62,hero.right-24,hero.bottom-18); round(c,careerPlay,Color.rgb(202,255,85),18);
            text(c,"JOGAR PRÓXIMA PARTIDA",careerPlay.left+24,careerPlay.centerY()+7,17,Color.rgb(7,20,13));

            RectF stats=new RectF(w*.61f,150,w-32,h*.48f); round(c,stats,Color.rgb(15,42,30),26);
            text(c,"TEMPORADA",stats.left+22,stats.top+36,17,Color.rgb(202,255,85));
            text(c,"Jogos  " + games,stats.left+22,stats.top+80,20,Color.WHITE);
            text(c,"Gols   " + goals,stats.left+22,stats.top+118,20,Color.WHITE);
            text(c,"Vitórias " + wins,stats.left+22,stats.top+156,20,Color.WHITE);
            int pts=wins*3; text(c,"Pontos  " + pts,stats.left+22,stats.top+194,20,Color.WHITE);

            RectF attrs=new RectF(32,h*.54f,w-32,h-28); round(c,attrs,Color.rgb(10,34,23),24);
            text(c,"ATRIBUTOS",attrs.left+22,attrs.top+34,17,Color.rgb(202,255,85));
            drawAttr(c,"Velocidade",speed,attrs.left+24,attrs.top+72,w*.27f);
            drawAttr(c,"Finalização",shot,w*.36f,attrs.top+72,w*.27f);
            drawAttr(c,"Passe",pass,w*.68f,attrs.top+72,w*.27f);
        }

        private void drawAttr(Canvas c,String n,int v,float x,float y,float width){
            text(c,n,x,y,16,Color.WHITE); text(c,String.valueOf(v),x+width-34,y,16,Color.rgb(202,255,85));
            RectF b=new RectF(x,y+14,x+width,y+25); round(c,b,Color.rgb(32,58,45),8);
            RectF f=new RectF(x,y+14,x+width*(v/99f),y+25); round(c,f,Color.rgb(202,255,85),8);
        }

        private void drawTraining(Canvas c) {
            bg(c); drawBrand(c); float w=getWidth(),h=getHeight();
            back.set(22,72,92,126); round(c,back,Color.rgb(18,48,34),18); text(c,"‹",44,112,40,Color.WHITE);
            text(c,"CENTRO DE TREINO",112,112,28,Color.WHITE);
            text(c,"Pontos disponíveis: " + points,34,166,22,Color.rgb(202,255,85));
            float gap=20, top=205, cardW=(w-68-gap*2)/3f, bottom=h-36;
            trainSpeed.set(34,top,34+cardW,bottom); trainShot.set(34+cardW+gap,top,34+cardW*2+gap,bottom); trainPass.set(34+cardW*2+gap*2,top,w-34,bottom);
            trainCard(c,trainSpeed,"VELOCIDADE",speed,"Arrancada e corrida");
            trainCard(c,trainShot,"FINALIZAÇÃO",shot,"Chutes mais precisos");
            trainCard(c,trainPass,"PASSE",pass,"Distribuição de bola");
        }

        private void trainCard(Canvas c,RectF r,String title,int value,String sub){
            round(c,r,Color.rgb(15,45,31),24); text(c,title,r.left+20,r.top+42,19,Color.WHITE);
            text(c,String.valueOf(value),r.left+20,r.top+96,42,Color.rgb(202,255,85));
            text(c,sub,r.left+20,r.top+126,14,Color.LTGRAY);
            RectF b=new RectF(r.left+20,r.bottom-60,r.right-20,r.bottom-18); round(c,b,points>0?Color.rgb(202,255,85):Color.DKGRAY,16);
            text(c,points>0?"+1 ATRIBUTO":"SEM PONTOS",b.left+18,b.centerY()+6,15,points>0?Color.rgb(7,20,13):Color.LTGRAY);
        }

        private void startMatch() {
            screen=3; homeScore=0; awayScore=0; matchTime=0; lastNs=System.nanoTime();
            playerX=.50f; playerY=.78f; ballX=.50f; ballY=.74f; ballVX=ballVY=0; hasBall=true;
            invalidate();
        }

        private void updateMatch() {
            long now=System.nanoTime(); float dt=Math.min(.033f,(now-lastNs)/1_000_000_000f); lastNs=now;
            matchTime += dt*12f;
            float move=(sprint?.36f:.24f)*(speed/70f);
            playerX=clamp(playerX+joyX*move*dt,.06f,.94f); playerY=clamp(playerY+joyY*move*dt,.08f,.96f);
            if(hasBall){ballX=playerX;ballY=playerY-.035f;} else {
                ballX+=ballVX*dt; ballY+=ballVY*dt; ballVX*=.985f; ballVY*=.985f;
                if(dist(ballX,ballY,playerX,playerY)<.045f) hasBall=true;
            }
            for(int i=0;i<ox.length;i++){
                float tx=hasBall?playerX:ballX, ty=hasBall?playerY:ballY;
                float d=dist(tx,ty,ox[i],oy[i]);
                if(d<.34f){ ox[i]+=(tx-ox[i])*dt*.20f; oy[i]+=(ty-oy[i])*dt*.20f; }
                if(hasBall && d<.038f){ hasBall=false; ballVX=(i%2==0?.14f:-.14f); ballVY=.22f; }
            }
            if(!hasBall && ballY<.035f && ballX>.37f && ballX<.63f){
                homeScore++; goals++; resetKickoff();
            }
            if(matchTime>=90f){
                games++; if(homeScore>awayScore) wins++; points+=2; save(); screen=1; invalidate();
            }
        }

        private void resetKickoff(){ playerX=.50f;playerY=.78f;ballX=.50f;ballY=.74f;ballVX=ballVY=0;hasBall=true; }
        private float dist(float a,float b,float c,float d){float x=a-c,y=b-d;return (float)Math.sqrt(x*x+y*y);} 
        private float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}

        private void drawMatch(Canvas c) {
            float w=getWidth(),h=getHeight(); c.drawColor(Color.rgb(7,31,18));
            for(int i=0;i<10;i++){p.setColor(i%2==0?Color.rgb(25,139,73):Color.rgb(22,126,66)); c.drawRect(0,i*h/10f,w,(i+1)*h/10f,p);} 
            drawPitch(c);
            for(int i=0;i<mx.length;i++) drawPlayer(c,mx[i],my[i],Color.rgb(202,255,85),false);
            for(int i=0;i<ox.length;i++) drawPlayer(c,ox[i],oy[i],Color.rgb(126,184,255),false);
            drawPlayer(c,playerX,playerY,Color.rgb(202,255,85),true); drawBall(c);
            p.setColor(Color.argb(210,4,17,11)); c.drawRoundRect(new RectF(w*.39f,12,w*.61f,62),18,18,p);
            text(c,"AUR  " + homeScore + "  -  " + awayScore + "  VIL",w*.42f,46,20,Color.WHITE);
            text(c,String.format("%02d:00",(int)matchTime),w*.49f,82,15,Color.WHITE);
            drawControls(c);
        }

        private float fx(float x,float y){float w=getWidth();float top=.55f*w,bottom=.96f*w;float ww=top+(bottom-top)*y;return w/2f+(x-.5f)*ww;}
        private float fy(float y){float h=getHeight();return h*.06f+y*h*.94f;}

        private void drawPitch(Canvas c){
            float w=getWidth(),h=getHeight(); p.setStyle(Paint.Style.FILL); p.setColor(Color.argb(65,12,70,34));
            android.graphics.Path path=new android.graphics.Path(); path.moveTo(fx(0,0),fy(0));path.lineTo(fx(1,0),fy(0));path.lineTo(fx(1,1),fy(1));path.lineTo(fx(0,1),fy(1));path.close();c.drawPath(path,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);p.setColor(Color.argb(210,255,255,255));c.drawPath(path,p);
            line(c,0,.5f,1,.5f); box(c,.25f,0,.75f,.17f); box(c,.37f,0,.63f,.07f);
            float cx=fx(.5f,.5f),cy=fy(.5f);c.drawCircle(cx,cy,getWidth()*.075f,p);
            float gl=fx(.39f,.02f),gr=fx(.61f,.02f),gy=fy(.02f);p.setStrokeWidth(4);c.drawLine(gl,gy,gl,gy-22,p);c.drawLine(gl,gy-22,gr,gy-22,p);c.drawLine(gr,gy-22,gr,gy,p);p.setStyle(Paint.Style.FILL);
        }

        private void line(Canvas c,float x1,float y1,float x2,float y2){c.drawLine(fx(x1,y1),fy(y1),fx(x2,y2),fy(y2),p);} 
        private void box(Canvas c,float x1,float y1,float x2,float y2){
            android.graphics.Path q=new android.graphics.Path();q.moveTo(fx(x1,y1),fy(y1));q.lineTo(fx(x2,y1),fy(y1));q.lineTo(fx(x2,y2),fy(y2));q.lineTo(fx(x1,y2),fy(y2));q.close();c.drawPath(q,p);
        }

        private void drawPlayer(Canvas c,float x,float y,int color,boolean selected){
            float px=fx(x,y),py=fy(y),s=.55f+.75f*y,r=11*s;
            p.setColor(Color.argb(80,0,0,0));c.drawOval(new RectF(px-r*1.2f,py+r,px+r*1.2f,py+r*1.7f),p);
            p.setColor(color);c.drawCircle(px,py-r*.75f,r*.55f,p);c.drawRoundRect(new RectF(px-r*.62f,py-r*.25f,px+r*.62f,py+r*1.0f),r*.3f,r*.3f,p);
            if(selected){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.WHITE);c.drawCircle(px,py+r*.25f,r*1.35f,p);p.setStyle(Paint.Style.FILL);} 
        }

        private void drawBall(Canvas c){float px=fx(ballX,ballY),py=fy(ballY),r=5+5*ballY;p.setColor(Color.WHITE);c.drawCircle(px,py,r,p);p.setColor(Color.DKGRAY);c.drawCircle(px,py,r*.32f,p);} 

        private void drawControls(Canvas c){
            float w=getWidth(),h=getHeight();
            p.setColor(Color.argb(95,255,255,255));c.drawCircle(105,h-105,70,p);c.drawCircle(105+joyX*42,h-105+joyY*42,28,p);
            drawBtn(c,w-105,h-112,56,"CHUTE");drawBtn(c,w-220,h-78,48,"PASSE");drawBtn(c,w-205,h-178,42,"SPRINT");
        }

        private void drawBtn(Canvas c,float x,float y,float r,String s){p.setColor(Color.argb(170,8,24,16));c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(202,255,85));c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);text(c,s,x,y+5,13,Color.WHITE);p.setTextAlign(Paint.Align.LEFT);} 

        private void shoot(){
            if(!hasBall)return; float accuracy=shot/99f; float target=.5f+(float)(Math.random()-.5)*.18f*(1f-accuracy); float dx=target-playerX,dy=.01f-playerY,len=(float)Math.sqrt(dx*dx+dy*dy);ballVX=dx/len*.68f;ballVY=dy/len*.68f;hasBall=false;
        }
        private void passBall(){
            if(!hasBall)return; int best=0;for(int i=1;i<my.length;i++)if(my[i]<my[best])best=i;float dx=mx[best]-playerX,dy=my[best]-playerY,len=(float)Math.sqrt(dx*dx+dy*dy);ballVX=dx/len*.44f;ballVY=dy/len*.44f;hasBall=false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x=e.getX(),y=e.getY();
            if(screen==3){ return matchTouch(e,x,y); }
            if(e.getAction()!=MotionEvent.ACTION_UP) return true;
            if(screen==0){
                if(playCareer.contains(x,y))screen=1; else if(quick.contains(x,y))startMatch(); else if(training.contains(x,y))screen=2; else if(profile.contains(x,y))screen=1;
            } else if(screen==1){ if(back.contains(x,y))screen=0; else if(careerPlay.contains(x,y))startMatch(); }
            else if(screen==2){
                if(back.contains(x,y))screen=0; else if(points>0&&trainSpeed.contains(x,y)){speed=Math.min(99,speed+1);points--;save();}
                else if(points>0&&trainShot.contains(x,y)){shot=Math.min(99,shot+1);points--;save();}
                else if(points>0&&trainPass.contains(x,y)){pass=Math.min(99,pass+1);points--;save();}
            }
            invalidate(); return true;
        }

        private boolean matchTouch(MotionEvent e,float x,float y){
            float w=getWidth(),h=getHeight(); int a=e.getActionMasked();
            if(a==MotionEvent.ACTION_DOWN || a==MotionEvent.ACTION_MOVE){
                if(x<w*.34f){float dx=x-105,dy=y-(h-105),m=(float)Math.sqrt(dx*dx+dy*dy);if(m>55){dx=dx/m*55;dy=dy/m*55;}joyX=dx/55f;joyY=dy/55f;}
                if(a==MotionEvent.ACTION_DOWN){
                    if(distPx(x,y,w-105,h-112)<64)shoot();
                    else if(distPx(x,y,w-220,h-78)<58)passBall();
                    else if(distPx(x,y,w-205,h-178)<54)sprint=true;
                }
            }
            if(a==MotionEvent.ACTION_UP || a==MotionEvent.ACTION_CANCEL){joyX=joyY=0;sprint=false;}
            return true;
        }
        private float distPx(float a,float b,float c,float d){float dx=a-c,dy=b-d;return (float)Math.sqrt(dx*dx+dy*dy);} 
    }
}
