package com.nox.futreal;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Random;

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
            t.setBackgroundColor(Color.rgb(5, 12, 9));
            t.setTextColor(Color.WHITE);
            t.setTextSize(21);
            t.setPadding(32, 32, 32, 32);
            t.setText("futREAL\n\nO app abriu em modo seguro.\nFeche e abra novamente para carregar o jogo.");
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
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final SharedPreferences prefs;
        private final Random random = new Random();

        private int screen = 0;
        private int games, goals, wins, draws, points, speed, shot, pass, fans, seasonPoints;
        private int homeScore, awayScore;
        private float matchTime, stamina = 1f;
        private long lastNs;
        private boolean sprint = false;
        private boolean hasBall = true;
        private boolean passInFlight = false;
        private int passTarget = -1;
        private long lastStealNs = 0;

        private float px = .50f, py = .76f;
        private float bx = .50f, by = .72f, bvx = 0, bvy = 0;
        private float joyX = 0, joyY = 0;

        private final float[] mateX = {.18f,.34f,.66f,.82f,.28f,.50f,.72f,.40f,.60f,.50f};
        private final float[] mateY = {.82f,.72f,.72f,.82f,.56f,.60f,.56f,.38f,.38f,.18f};
        private final float[] oppX  = {.15f,.34f,.50f,.66f,.85f,.25f,.44f,.58f,.76f,.40f,.60f};
        private final float[] oppY  = {.18f,.26f,.22f,.26f,.18f,.43f,.46f,.46f,.43f,.62f,.62f};

        private final RectF career = new RectF();
        private final RectF quick = new RectF();
        private final RectF train = new RectF();
        private final RectF profile = new RectF();
        private final RectF back = new RectF();
        private final RectF nextMatch = new RectF();
        private final RectF trainSpeed = new RectF();
        private final RectF trainShot = new RectF();
        private final RectF trainPass = new RectF();
        private final RectF shootBtn = new RectF();
        private final RectF passBtn = new RectF();
        private final RectF sprintBtn = new RectF();

        private float fieldL, fieldT, fieldR, fieldB;

        FutRealView(Context c) {
            super(c);
            setFocusable(true);
            prefs = c.getSharedPreferences("futreal_save_v2", Context.MODE_PRIVATE);
            games = prefs.getInt("games", 0);
            goals = prefs.getInt("goals", 0);
            wins = prefs.getInt("wins", 0);
            draws = prefs.getInt("draws", 0);
            points = prefs.getInt("points", 8);
            speed = prefs.getInt("speed", 72);
            shot = prefs.getInt("shot", 70);
            pass = prefs.getInt("pass", 69);
            fans = prefs.getInt("fans", 1250);
            seasonPoints = prefs.getInt("seasonPoints", 0);
            p.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD));
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.5f);
            stroke.setColor(Color.argb(220, 255, 255, 255));
        }

        private void save() {
            prefs.edit()
                    .putInt("games", games)
                    .putInt("goals", goals)
                    .putInt("wins", wins)
                    .putInt("draws", draws)
                    .putInt("points", points)
                    .putInt("speed", speed)
                    .putInt("shot", shot)
                    .putInt("pass", pass)
                    .putInt("fans", fans)
                    .putInt("seasonPoints", seasonPoints)
                    .apply();
        }

        private int ovr() { return Math.round(speed * .34f + shot * .36f + pass * .30f); }

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
            } else if (screen == 4) drawProfile(c);
            else drawHome(c);
        }

        private void text(Canvas c, String s, float x, float y, float size, int color) {
            p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size); c.drawText(s, x, y, p);
        }

        private void round(Canvas c, RectF r, int color, float radius) {
            p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color); c.drawRoundRect(r, radius, radius, p);
        }

        private void bg(Canvas c) {
            int w = getWidth(), h = getHeight();
            p.setShader(new LinearGradient(0, 0, w, h, Color.rgb(5, 16, 12), Color.rgb(9, 33, 23), Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(new RadialGradient(w * .88f, h * .12f, h * .52f, Color.argb(100, 180, 255, 70), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            c.drawCircle(w * .88f, h * .12f, h * .52f, p); p.setShader(null);
        }

        private void brand(Canvas c) {
            text(c, "fut", 28, 51, 31, Color.WHITE); text(c, "REAL", 76, 51, 31, Color.rgb(204, 255, 83));
            text(c, "MOBILE FOOTBALL", 30, 72, 10, Color.argb(180,255,255,255));
        }

        private void drawHome(Canvas c) {
            bg(c); brand(c); float w = getWidth(), h = getHeight();
            text(c, "CARREIRA EM JOGO", 32, h * .22f, 15, Color.rgb(204,255,83));
            text(c, "O futebol e seu.", 32, h * .31f, 36, Color.WHITE);
            text(c, "Construa seu jogador, vença partidas e vire uma estrela.", 32, h * .38f, 15, Color.LTGRAY);

            RectF hero = new RectF(28, h * .44f, w * .39f, h - 26);
            p.setShader(new LinearGradient(hero.left, hero.top, hero.right, hero.bottom, Color.rgb(20,63,42), Color.rgb(12,39,28), Shader.TileMode.CLAMP));
            c.drawRoundRect(hero, 24, 24, p); p.setShader(null);
            text(c, "NOX JR.", hero.left+20, hero.top+38, 25, Color.WHITE);
            text(c, "ATA  |  OVR " + ovr(), hero.left+20, hero.top+68, 15, Color.rgb(204,255,83));
            text(c, "Atlético Aurora", hero.left+20, hero.top+96, 14, Color.LTGRAY);
            text(c, games + " jogos   " + goals + " gols", hero.left+20, hero.bottom-24, 14, Color.WHITE);

            float sx = w * .43f, sy = h * .14f, gap = 14f;
            float cw = (w - sx - 34 - gap) / 2f, ch = (h - sy - 36 - gap) / 2f;
            career.set(sx, sy, sx+cw, sy+ch); quick.set(sx+cw+gap, sy, w-20, sy+ch);
            train.set(sx, sy+ch+gap, sx+cw, h-20); profile.set(sx+cw+gap, sy+ch+gap, w-20, h-20);
            menuCard(c, career, "MODO CARREIRA", "Temporada, tabela e evolução", true);
            menuCard(c, quick, "PARTIDA RÁPIDA", "Câmera TV e IA dinâmica", false);
            menuCard(c, train, "CENTRO DE TREINO", "Use pontos para evoluir", false);
            menuCard(c, profile, "MEU JOGADOR", "Atributos e números", false);
        }

        private void menuCard(Canvas c, RectF r, String title, String sub, boolean hot) {
            round(c, r, hot ? Color.rgb(25,68,44) : Color.rgb(15,45,32), 22);
            text(c, hot ? "DESTAQUE" : "futREAL", r.left+18, r.top+30, 11, hot ? Color.rgb(204,255,83) : Color.argb(170,255,255,255));
            text(c, title, r.left+18, r.centerY()+2, 18, Color.WHITE); text(c, sub, r.left+18, r.centerY()+28, 13, Color.LTGRAY);
        }

        private void drawBack(Canvas c) { back.set(20, 68, 90, 120); round(c, back, Color.rgb(18,49,35), 16); text(c, "<", 46, 105, 28, Color.WHITE); }

        private void drawCareer(Canvas c) {
            bg(c); brand(c); drawBack(c); float w=getWidth(), h=getHeight(); text(c, "MODO CARREIRA", 108, 103, 25, Color.WHITE);
            RectF season = new RectF(28, 142, w * .58f, h - 26);
            p.setShader(new LinearGradient(season.left, season.top, season.right, season.bottom, Color.rgb(17,56,38), Color.rgb(10,34,24), Shader.TileMode.CLAMP));
            c.drawRoundRect(season, 24, 24, p); p.setShader(null);
            text(c, "TEMPORADA 1", season.left+22, season.top+34, 14, Color.rgb(204,255,83));
            text(c, "Atlético Aurora", season.left+22, season.top+76, 29, Color.WHITE);
            text(c, "Liga Nacional • Rodada " + (games+1), season.left+22, season.top+104, 14, Color.LTGRAY);
            text(c, "PTS", season.left+22, season.top+150, 12, Color.LTGRAY); text(c, String.valueOf(seasonPoints), season.left+22, season.top+184, 28, Color.WHITE);
            text(c, "J", season.left+100, season.top+150, 12, Color.LTGRAY); text(c, String.valueOf(games), season.left+100, season.top+184, 28, Color.WHITE);
            text(c, "V", season.left+160, season.top+150, 12, Color.LTGRAY); text(c, String.valueOf(wins), season.left+160, season.top+184, 28, Color.WHITE);
            text(c, "GOLS", season.left+220, season.top+150, 12, Color.LTGRAY); text(c, String.valueOf(goals), season.left+220, season.top+184, 28, Color.WHITE);
            nextMatch.set(season.left+22, season.bottom-58, season.right-22, season.bottom-16); round(c, nextMatch, Color.rgb(204,255,83), 16);
            text(c, "JOGAR PRÓXIMA PARTIDA", nextMatch.left+20, nextMatch.centerY()+6, 15, Color.rgb(7,20,13));

            RectF player = new RectF(w*.61f, 142, w-28, h-26); round(c, player, Color.rgb(13,42,30), 24);
            text(c, "SEU JOGADOR", player.left+20, player.top+32, 13, Color.rgb(204,255,83)); text(c, "NOX JR.", player.left+20, player.top+70, 28, Color.WHITE);
            text(c, "OVR " + ovr(), player.right-100, player.top+70, 22, Color.rgb(204,255,83)); text(c, "Fãs  " + fans, player.left+20, player.top+104, 14, Color.LTGRAY);
            attr(c,"Velocidade",speed,player.left+20,player.top+148,player.width()-40); attr(c,"Finalização",shot,player.left+20,player.top+202,player.width()-40); attr(c,"Passe",pass,player.left+20,player.top+256,player.width()-40);
        }

        private void attr(Canvas c, String name, int value, float x, float y, float width) {
            text(c,name,x,y,14,Color.WHITE); text(c,String.valueOf(value),x+width-28,y,14,Color.rgb(204,255,83));
            RectF base = new RectF(x,y+10,x+width,y+19); round(c,base,Color.rgb(35,61,49),7);
            RectF fill = new RectF(x,y+10,x+width*(value/99f),y+19); round(c,fill,Color.rgb(204,255,83),7);
        }

        private void drawTraining(Canvas c) {
            bg(c); brand(c); drawBack(c); float w=getWidth(),h=getHeight(); text(c,"CENTRO DE TREINO",108,103,25,Color.WHITE);
            text(c,"Pontos disponíveis: " + points,30,154,19,Color.rgb(204,255,83));
            float gap=16, top=188, cw=(w-60-gap*2)/3f, bottom=h-26;
            trainSpeed.set(30,top,30+cw,bottom); trainShot.set(30+cw+gap,top,30+cw*2+gap,bottom); trainPass.set(30+cw*2+gap*2,top,w-30,bottom);
            trainingCard(c,trainSpeed,"VELOCIDADE",speed,"Arrancada, pique e recuperação"); trainingCard(c,trainShot,"FINALIZAÇÃO",shot,"Potência e precisão no chute"); trainingCard(c,trainPass,"PASSE",pass,"Passe curto e lançamento");
        }

        private void trainingCard(Canvas c, RectF r, String title, int value, String desc) {
            round(c,r,Color.rgb(15,45,32),22); text(c,title,r.left+18,r.top+34,16,Color.WHITE); text(c,String.valueOf(value),r.left+18,r.top+82,36,Color.rgb(204,255,83));
            text(c,desc,r.left+18,r.top+112,12,Color.LTGRAY); RectF b=new RectF(r.left+18,r.bottom-52,r.right-18,r.bottom-14);
            round(c,b,points>0?Color.rgb(204,255,83):Color.DKGRAY,14); text(c,points>0?"+1 ATRIBUTO":"SEM PONTOS",b.left+14,b.centerY()+5,13,points>0?Color.rgb(7,20,13):Color.LTGRAY);
        }

        private void drawProfile(Canvas c) {
            bg(c); brand(c); drawBack(c); float w=getWidth(),h=getHeight(); text(c,"MEU JOGADOR",108,103,25,Color.WHITE);
            RectF card=new RectF(30,142,w*.40f,h-28); round(c,card,Color.rgb(17,54,37),24);
            text(c,"NOX JR.",card.left+22,card.top+46,30,Color.WHITE); text(c,"ATACANTE • CAMISA 19",card.left+22,card.top+76,14,Color.rgb(204,255,83));
            text(c,"OVR",card.left+22,card.top+128,12,Color.LTGRAY); text(c,String.valueOf(ovr()),card.left+22,card.top+175,42,Color.WHITE);
            text(c,"Fãs",card.left+130,card.top+128,12,Color.LTGRAY); text(c,String.valueOf(fans),card.left+130,card.top+175,29,Color.WHITE);
            RectF numbers=new RectF(w*.43f,142,w-30,h-28); round(c,numbers,Color.rgb(13,41,29),24); text(c,"CARREIRA",numbers.left+20,numbers.top+34,13,Color.rgb(204,255,83));
            stat(c,"Jogos",games,numbers.left+20,numbers.top+82); stat(c,"Gols",goals,numbers.left+150,numbers.top+82); stat(c,"Vitórias",wins,numbers.left+280,numbers.top+82); stat(c,"Empates",draws,numbers.left+410,numbers.top+82);
            attr(c,"Velocidade",speed,numbers.left+20,numbers.top+158,numbers.width()-40); attr(c,"Finalização",shot,numbers.left+20,numbers.top+214,numbers.width()-40); attr(c,"Passe",pass,numbers.left+20,numbers.top+270,numbers.width()-40);
        }

        private void stat(Canvas c,String label,int value,float x,float y){ text(c,label,x,y,12,Color.LTGRAY); text(c,String.valueOf(value),x,y+32,26,Color.WHITE); }

        private void startMatch() {
            screen=3; homeScore=0; awayScore=0; matchTime=0; stamina=1f; px=.50f; py=.78f; bx=.50f; by=.74f; bvx=bvy=0;
            hasBall=true; passInFlight=false; passTarget=-1; sprint=false; joyX=joyY=0; lastNs=System.nanoTime(); lastStealNs=lastNs; invalidate();
        }

        private void updateMatch() {
            long now=System.nanoTime(); float dt=Math.min(.033f,Math.max(.001f,(now-lastNs)/1_000_000_000f)); lastNs=now; matchTime += dt * 8.0f;
            float staminaFactor=.68f + .32f*stamina; float move=(sprint?.36f:.235f)*(speed/72f)*staminaFactor;
            px=clamp(px+joyX*move*dt,.06f,.94f); py=clamp(py+joyY*move*dt,.08f,.93f);
            if(sprint && (Math.abs(joyX)+Math.abs(joyY)>.15f)) stamina=Math.max(0,stamina-dt*.055f); else stamina=Math.min(1f,stamina+dt*.022f);
            animateAI(dt, now);

            if(hasBall){ bx=px; by=py-.032f; }
            else{
                bx+=bvx*dt; by+=bvy*dt; float drag=(float)Math.pow(.36f,dt); bvx*=drag; bvy*=drag;
                if(passInFlight && passTarget>=0){
                    float d=dist(bx,by,mateX[passTarget],mateY[passTarget]);
                    if(d<.045f){
                        passInFlight=false;
                        if(mateY[passTarget]<.34f){ bx=mateX[passTarget]; by=mateY[passTarget]-.02f; bvx=(.50f-bx)*1.45f; bvy=-.72f*(.82f+pass/160f); }
                        else{ float dx=px-bx, dy=py-by; float len=Math.max(.001f,(float)Math.sqrt(dx*dx+dy*dy)); bvx=dx/len*.62f; bvy=dy/len*.62f; }
                        passTarget=-1;
                    }
                }
                if(dist(px,py,bx,by)<.045f && Math.abs(bvx)+Math.abs(bvy)<.62f){ hasBall=true; passInFlight=false; passTarget=-1; bvx=bvy=0; }
            }

            if(hasBall && now-lastStealNs > 650_000_000L){
                for(int i=0;i<oppX.length;i++){
                    if(dist(px,py,oppX[i],oppY[i])<.05f){
                        float protection=.30f + pass/180f;
                        if(random.nextFloat()>protection){ hasBall=false; passInFlight=false; passTarget=-1; bvx=(.5f-px)*.18f + (random.nextFloat()-.5f)*.12f; bvy=.62f; lastStealNs=now; }
                        break;
                    }
                }
            }

            if(by<.045f && bx>.41f && bx<.59f){ homeScore++; goals++; resetKickoff(); }
            else if(by>.955f && bx>.41f && bx<.59f){ awayScore++; resetKickoff(); }
            else if(bx<.025f || bx>.975f || by<.015f || by>.985f){ resetBallNearUser(); }
            if(matchTime>=90f) finishMatch();
        }

        private void animateAI(float dt, long now) {
            float t=matchTime*.045f;
            float[] defenseX={.16f,.34f,.66f,.84f}; float[] midX={.28f,.50f,.72f}; float[] attackX={.39f,.61f};
            for(int i=0;i<mateX.length;i++){
                float targetX,targetY;
                if(i<4){ targetX=defenseX[i]; targetY=.78f-py*.08f; }
                else if(i<7){ targetX=midX[i-4]; targetY=.56f-(1f-py)*.11f; }
                else if(i<9){ targetX=attackX[i-7]+(float)Math.sin(t+i)*.04f; targetY=.36f-(1f-py)*.08f; }
                else{ targetX=.50f+(float)Math.sin(t*.7f)*.10f; targetY=.18f; }
                mateX[i]=lerp(mateX[i],targetX,dt*.7f); mateY[i]=lerp(mateY[i],targetY,dt*.7f);
            }
            int first=-1,second=-1; float d1=9,d2=9; float tx=hasBall?px:bx, ty=hasBall?py:by;
            for(int i=0;i<oppX.length;i++){ float d=dist(oppX[i],oppY[i],tx,ty); if(d<d1){d2=d1;second=first;d1=d;first=i;} else if(d<d2){d2=d;second=i;} }
            float[] baseXs={.15f,.34f,.50f,.66f,.85f,.25f,.44f,.58f,.76f,.40f,.60f}; float[] baseYs={.18f,.26f,.22f,.26f,.18f,.43f,.46f,.46f,.43f,.62f,.62f};
            for(int i=0;i<oppX.length;i++){
                float targetX=baseXs[i], targetY=baseYs[i];
                if(i==first){ targetX=tx; targetY=ty+.012f; }
                else if(i==second && ty<.70f){ targetX=lerp(baseXs[i],tx,.55f); targetY=lerp(baseYs[i],ty,.55f); }
                oppX[i]=lerp(oppX[i],targetX,dt*(i==first?1.4f:.58f)); oppY[i]=lerp(oppY[i],targetY,dt*(i==first?1.4f:.58f));
            }
        }

        private void resetKickoff(){
            px=.50f;py=.78f;bx=.50f;by=.74f;bvx=bvy=0;hasBall=true;passInFlight=false;passTarget=-1;
            float[] xs={.15f,.34f,.50f,.66f,.85f,.25f,.44f,.58f,.76f,.40f,.60f}; float[] ys={.18f,.26f,.22f,.26f,.18f,.43f,.46f,.46f,.43f,.62f,.62f};
            for(int i=0;i<oppX.length;i++){ oppX[i]=xs[i]; oppY[i]=ys[i]; }
        }

        private void resetBallNearUser(){ bx=px;by=py-.035f;bvx=bvy=0;hasBall=true;passInFlight=false;passTarget=-1; }

        private void finishMatch(){
            games++;
            if(homeScore>awayScore){ wins++; seasonPoints+=3; points+=2; fans+=180+homeScore*25; }
            else if(homeScore==awayScore){ draws++; seasonPoints+=1; points+=1; fans+=65; }
            else{ fans=Math.max(0,fans-30); points+=1; }
            save(); screen=1; invalidate();
        }

        private void drawMatch(Canvas c){
            float w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,0,0,h,Color.rgb(7,13,18),Color.rgb(12,28,21),Shader.TileMode.CLAMP)); c.drawRect(0,0,w,h,p); p.setShader(null);
            p.setColor(Color.rgb(24,30,38)); c.drawRect(0,0,w,h*.115f,p); c.drawRect(0,h*.89f,w,h,p); drawCrowd(c,0,h*.02f,w,h*.095f,26); drawCrowd(c,0,h*.905f,w,h*.975f,24);
            fieldL=w*.055f; fieldR=w*.945f; fieldT=h*.12f; fieldB=h*.89f;
            p.setColor(Color.rgb(24,126,61)); c.drawRect(fieldL,fieldT,fieldR,fieldB,p);
            for(int i=0;i<10;i++){ if(i%2==0){ p.setColor(Color.argb(25,255,255,255)); c.drawRect(fieldL+i*(fieldR-fieldL)/10f,fieldT,fieldL+(i+1)*(fieldR-fieldL)/10f,fieldB,p); } }
            stroke.setColor(Color.argb(225,255,255,255)); stroke.setStrokeWidth(2.4f); c.drawRect(fieldL+2,fieldT+2,fieldR-2,fieldB-2,stroke);
            float midY=mapY(.50f); c.drawLine(fieldL,midY,fieldR,midY,stroke); c.drawCircle(mapX(.50f),midY,(fieldR-fieldL)*.095f,stroke); c.drawCircle(mapX(.50f),midY,3.5f,pWhite());
            drawBox(c,.30f,.00f,.70f,.16f); drawBox(c,.30f,.84f,.70f,1.00f); drawGoal(c,true); drawGoal(c,false);

            RectF score=new RectF(w*.35f,12,w*.65f,58); round(c,score,Color.argb(225,8,15,18),15);
            text(c,"AUR",score.left+18,score.centerY()+6,14,Color.WHITE); text(c,homeScore+"  -  "+awayScore,score.centerX()-32,score.centerY()+7,20,Color.WHITE); text(c,"RIV",score.right-47,score.centerY()+6,14,Color.WHITE); text(c,String.format("%02d'",Math.min(90,(int)matchTime)),score.right+12,score.centerY()+6,15,Color.WHITE);

            for(int i=0;i<mateX.length;i++) drawPlayer(c,mateX[i],mateY[i],Color.rgb(225,236,244),Color.rgb(24,64,110),false,i==9);
            for(int i=0;i<oppX.length;i++) drawPlayer(c,oppX[i],oppY[i],Color.rgb(235,62,68),Color.rgb(128,20,30),false,i<1);
            drawKeeper(c,.50f,.055f,Color.rgb(251,197,49)); drawKeeper(c,.50f,.945f,Color.rgb(59,179,246)); drawPlayer(c,px,py,Color.WHITE,Color.rgb(16,87,46),true,false);

            float sx=mapX(bx), sy=mapY(by); p.setColor(Color.argb(80,0,0,0)); c.drawOval(new RectF(sx-8,sy+5,sx+8,sy+10),p); p.setColor(Color.WHITE); c.drawCircle(sx,sy,6.5f,p); p.setColor(Color.rgb(35,35,35)); c.drawCircle(sx+2,sy-1,2.2f,p);
            drawMiniMap(c); drawControls(c); drawStamina(c);
        }

        private Paint pWhite(){ p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);return p; }

        private void drawCrowd(Canvas c,float l,float t,float r,float b,int rows){
            float w=r-l,h=b-t; for(int i=0;i<rows*8;i++){ float x=l+(i%(rows*2))*w/(rows*2f)+(i%3)*2; float y=t+(i/(rows*2))*h/4f; int col=(i%5==0)?Color.rgb(204,255,83):(i%3==0?Color.LTGRAY:Color.rgb(72,82,92)); p.setColor(col); c.drawCircle(x,y,1.6f,p); }
        }

        private void drawBox(Canvas c,float x1,float y1,float x2,float y2){ c.drawRect(mapX(x1),mapY(y1),mapX(x2),mapY(y2),stroke); }

        private void drawGoal(Canvas c,boolean top){
            float x1=mapX(.42f),x2=mapX(.58f); float y=top?fieldT:fieldB; float d=top?-13:13; stroke.setColor(Color.WHITE);stroke.setStrokeWidth(2f); c.drawRect(x1,y+(top?d:0),x2,y+(top?0:d),stroke);
            for(int i=1;i<5;i++){ float x=x1+(x2-x1)*i/5f; c.drawLine(x,y,x,y+d,stroke); }
        }

        private void drawPlayer(Canvas c,float nx,float ny,int shirt,int shorts,boolean selected,boolean captain){
            float x=mapX(nx), y=mapY(ny), scale=.82f + ny*.26f;
            if(selected){ p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);p.setColor(Color.rgb(204,255,83)); c.drawCircle(x,y+2,15*scale,p);p.setStyle(Paint.Style.FILL); Path tri=new Path();tri.moveTo(x,y-28*scale);tri.lineTo(x-6,y-36*scale);tri.lineTo(x+6,y-36*scale);tri.close(); p.setColor(Color.rgb(204,255,83));c.drawPath(tri,p); }
            p.setColor(Color.argb(65,0,0,0));c.drawOval(new RectF(x-9*scale,y+10*scale,x+9*scale,y+15*scale),p); p.setColor(Color.rgb(219,173,128));c.drawCircle(x,y-10*scale,5*scale,p);
            p.setColor(shirt);c.drawRoundRect(new RectF(x-7*scale,y-5*scale,x+7*scale,y+8*scale),4,4,p); p.setColor(shorts);c.drawRect(x-6*scale,y+6*scale,x+6*scale,y+11*scale,p);
            p.setStrokeWidth(2.2f*scale); p.setColor(Color.rgb(225,225,225)); c.drawLine(x-4*scale,y+11*scale,x-5*scale,y+18*scale,p); c.drawLine(x+4*scale,y+11*scale,x+5*scale,y+18*scale,p);
            if(captain){ p.setColor(Color.rgb(255,210,42)); c.drawRect(x+5*scale,y-2*scale,x+8*scale,y+3*scale,p); }
        }

        private void drawKeeper(Canvas c,float nx,float ny,int shirt){ drawPlayer(c,nx,ny,shirt,Color.rgb(24,38,48),false,true); }

        private void drawMiniMap(Canvas c){
            float w=getWidth(),h=getHeight(); RectF m=new RectF(w*.445f,h-72,w*.555f,h-14); round(c,m,Color.argb(170,4,13,9),10);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(Color.argb(160,255,255,255));c.drawRect(m.left+4,m.top+4,m.right-4,m.bottom-4,p); p.setStyle(Paint.Style.FILL);
            for(int i=0;i<mateX.length;i++){p.setColor(Color.WHITE);c.drawCircle(m.left+4+mateX[i]*(m.width()-8),m.top+4+mateY[i]*(m.height()-8),1.7f,p);} for(int i=0;i<oppX.length;i++){p.setColor(Color.rgb(239,68,68));c.drawCircle(m.left+4+oppX[i]*(m.width()-8),m.top+4+oppY[i]*(m.height()-8),1.7f,p);} p.setColor(Color.rgb(204,255,83));c.drawCircle(m.left+4+px*(m.width()-8),m.top+4+py*(m.height()-8),2.4f,p);
        }

        private void drawStamina(Canvas c){
            text(c,"NOX JR.",24,83,12,Color.WHITE); RectF base=new RectF(24,91,164,99);round(c,base,Color.argb(140,0,0,0),5); RectF fill=new RectF(24,91,24+140*stamina,99);round(c,fill,stamina>.3f?Color.rgb(204,255,83):Color.rgb(255,124,65),5);
        }

        private void drawControls(Canvas c){
            float w=getWidth(),h=getHeight(); float jx=w*.12f,jy=h*.75f,jr=Math.min(w,h)*.105f; p.setColor(Color.argb(70,255,255,255));c.drawCircle(jx,jy,jr,p); p.setColor(Color.argb(115,255,255,255));c.drawCircle(jx+joyX*jr*.55f,jy+joyY*jr*.55f,jr*.36f,p);
            float br=Math.min(w,h)*.072f; shootBtn.set(w*.86f-br,h*.64f-br,w*.86f+br,h*.64f+br); passBtn.set(w*.76f-br,h*.77f-br,w*.76f+br,h*.77f+br); sprintBtn.set(w*.91f-br,h*.83f-br,w*.91f+br,h*.83f+br);
            circleButton(c,shootBtn,hasBall?"CHUTE":"PRESS",Color.rgb(204,255,83),Color.rgb(7,20,13)); circleButton(c,passBtn,hasBall?"PASSE":"TROCA",Color.argb(210,255,255,255),Color.rgb(15,28,22)); circleButton(c,sprintBtn,sprint?"PIQUE ON":"PIQUE",Color.argb(185,28,44,38),Color.WHITE);
        }

        private void circleButton(Canvas c,RectF r,String label,int color,int tc){ p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawOval(r,p); p.setTextSize(11);p.setColor(tc); float tw=p.measureText(label); c.drawText(label,r.centerX()-tw/2,r.centerY()+4,p); }

        private void shoot(){ if(!hasBall)return; hasBall=false;passInFlight=false;passTarget=-1; bvx=(.50f-px)*1.25f + (random.nextFloat()-.5f)*(.30f-(shot/500f)); bvy=-(.72f + shot/150f); }

        private void passBall(){
            if(!hasBall)return; int best=-1;float bestScore=99;
            for(int i=0;i<mateX.length;i++){ if(mateY[i]>py+.08f)continue; float d=dist(px,py,mateX[i],mateY[i]); float score=d + mateY[i]*.12f; if(score<bestScore && d>.08f){bestScore=score;best=i;} }
            if(best<0){ for(int i=0;i<mateX.length;i++){ float d=dist(px,py,mateX[i],mateY[i]); if(d<bestScore && d>.08f){bestScore=d;best=i;} } }
            if(best>=0){ hasBall=false;passInFlight=true;passTarget=best; float dx=mateX[best]-bx,dy=mateY[best]-by; float len=Math.max(.001f,(float)Math.sqrt(dx*dx+dy*dy)); float power=.58f + pass/210f; bvx=dx/len*power;bvy=dy/len*power; }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e){
            float x=e.getX(),y=e.getY(),w=getWidth(); int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN){
                if(screen==0){ if(career.contains(x,y))screen=1; else if(quick.contains(x,y))startMatch(); else if(train.contains(x,y))screen=2; else if(profile.contains(x,y))screen=4; invalidate();return true; }
                if(screen==1){ if(back.contains(x,y))screen=0; else if(nextMatch.contains(x,y))startMatch(); invalidate();return true; }
                if(screen==2){ if(back.contains(x,y)){screen=0;invalidate();return true;} if(points>0){ if(trainSpeed.contains(x,y)){speed=Math.min(99,speed+1);points--;save();} else if(trainShot.contains(x,y)){shot=Math.min(99,shot+1);points--;save();} else if(trainPass.contains(x,y)){pass=Math.min(99,pass+1);points--;save();} invalidate();return true; } }
                if(screen==4){ if(back.contains(x,y)){screen=0;invalidate();} return true; }
                if(screen==3){ if(shootBtn.contains(x,y)){shoot();return true;} if(passBtn.contains(x,y)){passBall();return true;} if(sprintBtn.contains(x,y)){sprint=!sprint;return true;} if(x<w*.36f){updateJoy(x,y);return true;} }
            } else if(action==MotionEvent.ACTION_MOVE && screen==3){ if(x<w*.38f){updateJoy(x,y);return true;} }
            else if((action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) && screen==3){ if(x<w*.42f){joyX=joyY=0;} return true; }
            return true;
        }

        private void updateJoy(float x,float y){ float cx=getWidth()*.12f,cy=getHeight()*.75f,r=Math.min(getWidth(),getHeight())*.105f; float dx=x-cx,dy=y-cy,len=(float)Math.sqrt(dx*dx+dy*dy); if(len>r){dx=dx/len*r;dy=dy/len*r;} joyX=clamp(dx/r,-1,1);joyY=clamp(dy/r,-1,1); }
        private float mapX(float nx){return fieldL+nx*(fieldR-fieldL);} private float mapY(float ny){return fieldT+ny*(fieldB-fieldT);} private float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
        private float dist(float x1,float y1,float x2,float y2){float dx=x1-x2,dy=y1-y2;return (float)Math.sqrt(dx*dx+dy*dy);} private float lerp(float a,float b,float t){return a+(b-a)*Math.max(0,Math.min(1,t));}
    }
}
