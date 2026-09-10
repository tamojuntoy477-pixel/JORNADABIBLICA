package com.nox.formularivals;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.*;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(new RaceView(this));
    }

    static class Rival {
        float lane, z, speed;
        int color;
        Rival(float l,float zz,float s,int c){lane=l;z=zz;speed=s;color=c;}
    }

    static class RaceView extends View {
        Paint p=new Paint(3); Path path=new Path(); Random rnd=new Random();
        int W,H; long last=System.nanoTime();
        int screen=0; // 0 menu, 1 race, 2 garage, 3 results
        float steer=0, speed=0, distance=0, nitro=1f;
        int lap=1, laps=3, position=8; float raceTime=0; int stage=1;
        boolean nitroOn=false; ArrayList<Rival> rivals=new ArrayList<>();
        int playerColor=Color.rgb(225,6,0);
        final float TRACK=4200f, VISIBLE=950f;

        RaceView(Context c){ super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }

        void resetRace(){
            speed=195; distance=0; nitro=1; lap=1; raceTime=0; position=8; steer=0; rivals.clear();
            int[] cs={0xff00a6ff,0xffffd000,0xff8b5cf6,0xff00c853,0xffff6d00,0xffef476f,0xffffffff};
            for(int i=0;i<7;i++) rivals.add(new Rival(-.72f+(i%4)*.48f,180+i*120,260+rnd.nextInt(34),cs[i]));
        }

        @Override protected void onSizeChanged(int w,int h,int ow,int oh){W=w;H=h;}

        @Override protected void onDraw(Canvas c){
            super.onDraw(c); long now=System.nanoTime(); float dt=Math.min(.04f,(now-last)/1_000_000_000f); last=now;
            if(screen==1) update(dt);
            if(screen==0) drawMenu(c); else if(screen==1) drawRace(c); else if(screen==2) drawGarage(c); else drawResults(c);
            invalidate();
        }

        void update(float dt){
            raceTime+=dt;
            float target=nitroOn&&nitro>0?342:305;
            speed += (target-speed)*dt*1.7f;
            if(nitroOn&&nitro>0){nitro=Math.max(0,nitro-dt*.19f);} else nitro=Math.min(1,nitro+dt*.035f);
            distance += speed*dt*.72f;
            for(Rival r:rivals){
                r.z += (r.speed-speed)*dt*.72f;
                if(r.z < -80) r.z += TRACK;
                if(r.z > TRACK-80) r.z -= TRACK;
            }
            if(distance>=TRACK){ distance-=TRACK; lap++; for(Rival r:rivals) r.z += TRACK; if(lap>laps){screen=3; nitroOn=false; return;} }
            int ahead=0; float prog=(lap-1)*TRACK+distance;
            for(int i=0;i<rivals.size();i++){
                Rival r=rivals.get(i);
                float rp=prog+r.z;
                if(rp>prog) ahead++;
                if(r.z>0&&r.z<65&&Math.abs(r.lane-steer)<.23f){ speed=Math.max(155,speed-120*dt); }
            }
            position=Math.min(8,1+ahead);
        }

        float curve(float d){ return (float)(Math.sin((d+distance)*.00165)*.52 + Math.sin((d+distance)*.00063)*.22); }
        float roadHalf(float y){ float h=H*.24f; float t=Math.max(0,Math.min(1,(y-h)/(H-h))); return W*(.085f+t*.39f); }
        float centerAt(float y,float d){ float h=H*.24f; float t=Math.max(0,Math.min(1,(y-h)/(H-h))); return W*.5f + curve(d)*W*.18f*t*t; }

        void drawRace(Canvas c){
            p.setStyle(Paint.Style.FILL); p.setColor(0xff74b9e8); c.drawRect(0,0,W,H*.24f,p);
            p.setColor(0xff1e5f2f); c.drawRect(0,H*.24f,W,H,p);
            p.setColor(0xffeef6ff); c.drawCircle(W*.78f,H*.09f,H*.045f,p);
            drawRoad(c);
            ArrayList<Rival> draw=new ArrayList<>(rivals);
            Collections.sort(draw,(a,b)->Float.compare(b.z,a.z));
            for(Rival r:draw) if(r.z>0&&r.z<VISIBLE) drawRival(c,r);
            drawPlayer(c);
            drawHud(c);
        }

        void drawRoad(Canvas c){
            int n=52; float horizon=H*.24f;
            for(int i=0;i<n;i++){
                float t1=i/(float)n, t2=(i+1)/(float)n;
                float y1=horizon+(float)Math.pow(t1,1.75)*(H-horizon);
                float y2=horizon+(float)Math.pow(t2,1.75)*(H-horizon);
                float d1=(1-t1)*VISIBLE, d2=(1-t2)*VISIBLE;
                float c1=centerAt(y1,d1), c2=centerAt(y2,d2), rw1=roadHalf(y1), rw2=roadHalf(y2);
                path.reset(); path.moveTo(c1-rw1,y1); path.lineTo(c1+rw1,y1); path.lineTo(c2+rw2,y2); path.lineTo(c2-rw2,y2); path.close();
                p.setColor(((i/2)%2==0)?0xff34373b:0xff303337); c.drawPath(path,p);
                float k1=rw1*.12f,k2=rw2*.12f;
                p.setColor(((i/2)%2==0)?0xffffffff:0xffe10600);
                path.reset(); path.moveTo(c1-rw1-k1,y1); path.lineTo(c1-rw1,y1); path.lineTo(c2-rw2,y2); path.lineTo(c2-rw2-k2,y2); path.close(); c.drawPath(path,p);
                path.reset(); path.moveTo(c1+rw1,y1); path.lineTo(c1+rw1+k1,y1); path.lineTo(c2+rw2+k2,y2); path.lineTo(c2+rw2,y2); path.close(); c.drawPath(path,p);
            }
            p.setColor(0xaaeeeeee); p.setStrokeWidth(Math.max(2,W*.004f)); p.setStyle(Paint.Style.STROKE);
            for(int lane=-1;lane<=1;lane++){
                path.reset(); for(int i=0;i<24;i++){ float t=i/23f; float y=H*.24f+(float)Math.pow(t,1.75)*(H-H*.24f); float d=(1-t)*VISIBLE; float x=centerAt(y,d)+lane*roadHalf(y)*.33f; if(i==0)path.moveTo(x,y);else path.lineTo(x,y);} c.drawPath(path,p);
            } p.setStyle(Paint.Style.FILL);
        }

        void drawRival(Canvas c,Rival r){
            float t=1-r.z/VISIBLE; float y=H*.24f+(float)Math.pow(t,1.75)*(H-H*.24f)*.9f;
            float s=.22f+t*.72f; float x=centerAt(y,r.z)+r.lane*roadHalf(y)*.66f;
            drawCar(c,x,y,58*s,104*s,r.color,false);
        }

        void drawPlayer(Canvas c){
            float x=W*.5f+steer*W*.28f; float y=H*.83f;
            drawCar(c,x,y,86,150,playerColor,true);
        }

        void drawCar(Canvas c,float cx,float cy,float w,float h,int color,boolean player){
            p.setColor(0xaa000000); c.drawOval(cx-w*.62f,cy+h*.34f,cx+w*.62f,cy+h*.55f,p);
            p.setColor(0xff090a0c); c.drawRoundRect(cx-w*.70f,cy-h*.28f,cx-w*.43f,cy+h*.25f,w*.08f,w*.08f,p);
            c.drawRoundRect(cx+w*.43f,cy-h*.28f,cx+w*.70f,cy+h*.25f,w*.08f,w*.08f,p);
            p.setColor(color);
            path.reset(); path.moveTo(cx,cy-h*.52f); path.lineTo(cx+w*.27f,cy-h*.18f); path.lineTo(cx+w*.34f,cy+h*.35f); path.lineTo(cx-w*.34f,cy+h*.35f); path.lineTo(cx-w*.27f,cy-h*.18f); path.close(); c.drawPath(path,p);
            c.drawRoundRect(cx-w*.62f,cy+h*.26f,cx+w*.62f,cy+h*.38f,w*.04f,w*.04f,p);
            c.drawRoundRect(cx-w*.50f,cy-h*.43f,cx+w*.50f,cy-h*.32f,w*.04f,w*.04f,p);
            p.setColor(0xff111820); c.drawOval(cx-w*.19f,cy-h*.21f,cx+w*.19f,cy+h*.10f,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(3,w*.055f)); p.setColor(0xffc5ccd5); c.drawArc(cx-w*.23f,cy-h*.22f,cx+w*.23f,cy+h*.12f,205,130,false,p); p.setStyle(Paint.Style.FILL);
            p.setColor(0xffffffff); c.drawRect(cx-w*.08f,cy+h*.12f,cx+w*.08f,cy+h*.30f,p);
            if(player){p.setColor(0xff00e5ff); c.drawRect(cx-w*.07f,cy-h*.47f,cx+w*.07f,cy-h*.40f,p);}
        }

        void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align){p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(size);p.setTextAlign(align);p.setColor(color);c.drawText(s,x,y,p);}
        void panel(Canvas c,float l,float t,float r,float b){p.setColor(0xcc0b0e13);c.drawRoundRect(l,t,r,b,18,18,p);}

        void drawHud(Canvas c){
            panel(c,18,18,W*.25f,H*.19f); text(c,position+"/8",36,H*.075f,H*.055f,Color.WHITE,Paint.Align.LEFT); text(c,"POS",36,H*.125f,H*.025f,0xff9aa4b2,Paint.Align.LEFT);
            text(c,"VOLTA "+Math.min(lap,laps)+"/"+laps,W*.5f,H*.065f,H*.038f,Color.WHITE,Paint.Align.CENTER);
            text(c,String.format(Locale.US,"%d km/h",(int)speed),W*.5f,H*.92f,H*.052f,Color.WHITE,Paint.Align.CENTER);
            int gear=Math.max(1,Math.min(8,(int)(speed/42))); text(c,"MARCHA "+gear,W*.5f,H*.965f,H*.025f,0xffb8c1cc,Paint.Align.CENTER);
            panel(c,W*.77f,18,W-18,H*.19f); text(c,"ERS / NITRO",W*.79f,H*.07f,H*.024f,0xffd3d8de,Paint.Align.LEFT);
            p.setColor(0xff252a31); c.drawRoundRect(W*.79f,H*.10f,W*.965f,H*.14f,12,12,p); p.setColor(nitro>.2?0xff00e5ff:0xffff8a00); c.drawRoundRect(W*.79f,H*.10f,W*.79f+(W*.175f)*nitro,H*.14f,12,12,p);
            text(c,"TOQUE NO CANTO DIREITO = NITRO",W-20,H*.965f,H*.018f,0x99ffffff,Paint.Align.RIGHT);
        }

        void drawMenu(Canvas c){
            p.setColor(0xff080b10); c.drawRect(0,0,W,H,p); p.setColor(0xff161b24); c.drawRect(W*.54f,0,W,H,p);
            drawCar(c,W*.73f,H*.55f,H*.22f,H*.39f,0xffe10600,true);
            text(c,"FORMULA",W*.07f,H*.25f,H*.10f,Color.WHITE,Paint.Align.LEFT); text(c,"RIVALS",W*.07f,H*.35f,H*.10f,0xffe10600,Paint.Align.LEFT);
            text(c,"Corrida de monopostos para celular",W*.075f,H*.41f,H*.027f,0xffaab3bf,Paint.Align.LEFT);
            button(c,W*.07f,H*.50f,W*.44f,H*.61f,"CORRIDA RÁPIDA",0xffe10600);
            button(c,W*.07f,H*.65f,W*.44f,H*.76f,"CAMPEONATO",0xff242a33);
            button(c,W*.07f,H*.80f,W*.44f,H*.91f,"GARAGEM",0xff242a33);
        }

        void button(Canvas c,float l,float t,float r,float b,String s,int col){p.setColor(col);c.drawRoundRect(l,t,r,b,22,22,p);text(c,s,(l+r)/2,(t+b)/2+H*.013f,H*.035f,Color.WHITE,Paint.Align.CENTER);}

        void drawGarage(Canvas c){
            p.setColor(0xff090c12);c.drawRect(0,0,W,H,p);text(c,"GARAGEM",W*.08f,H*.15f,H*.07f,Color.WHITE,Paint.Align.LEFT);text(c,"Escolha uma pintura",W*.08f,H*.21f,H*.027f,0xff9fa9b5,Paint.Align.LEFT);
            drawCar(c,W*.5f,H*.53f,H*.25f,H*.44f,playerColor,true);
            int[] cs={0xffe10600,0xff00a6ff,0xffffd000,0xff8b5cf6,0xff00c853}; for(int i=0;i<5;i++){float x=W*.20f+i*W*.15f;p.setColor(cs[i]);c.drawCircle(x,H*.83f,H*.045f,p);}
            text(c,"TOQUE NUMA COR  •  VOLTAR NO CANTO ESQUERDO",W*.5f,H*.94f,H*.023f,0xffbac2cc,Paint.Align.CENTER);
        }

        void drawResults(Canvas c){
            p.setColor(0xff080b10); c.drawRect(0,0,W,H,p); text(c,"CORRIDA CONCLUÍDA",W*.5f,H*.24f,H*.07f,Color.WHITE,Paint.Align.CENTER);
            text(c,"P"+position,W*.5f,H*.45f,H*.17f,0xffe10600,Paint.Align.CENTER); text(c,String.format(Locale.US,"TEMPO  %.1fs",raceTime),W*.5f,H*.56f,H*.034f,0xffc3cad4,Paint.Align.CENTER);
            button(c,W*.30f,H*.68f,W*.70f,H*.80f,"CORRER NOVAMENTE",0xffe10600); text(c,"MENU",W*.5f,H*.90f,H*.03f,0xffaeb7c2,Paint.Align.CENTER);
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            float x=e.getX(),y=e.getY();
            if(e.getAction()==MotionEvent.ACTION_UP){nitroOn=false;if(screen==1){steer*=.35f;return true;}}
            if(e.getAction()!=MotionEvent.ACTION_DOWN&&e.getAction()!=MotionEvent.ACTION_MOVE)return true;
            if(screen==0&&e.getAction()==MotionEvent.ACTION_DOWN){
                if(x>W*.07f&&x<W*.44f&&y>H*.50f&&y<H*.61f){resetRace();screen=1;}
                else if(x>W*.07f&&x<W*.44f&&y>H*.65f&&y<H*.76f){stage=1;resetRace();screen=1;}
                else if(x>W*.07f&&x<W*.44f&&y>H*.80f&&y<H*.91f)screen=2;
            } else if(screen==1){
                if(x>W*.73f&&y>H*.58f){nitroOn=true;} else steer=Math.max(-1,Math.min(1,(x-W*.5f)/(W*.34f)));
            } else if(screen==2&&e.getAction()==MotionEvent.ACTION_DOWN){
                if(y>H*.76f&&y<H*.90f){int i=Math.max(0,Math.min(4,(int)((x-W*.125f)/(W*.15f))));int[] cs={0xffe10600,0xff00a6ff,0xffffd000,0xff8b5cf6,0xff00c853};playerColor=cs[i];}
                else if(x<W*.18f&&y>H*.86f)screen=0;
            } else if(screen==3&&e.getAction()==MotionEvent.ACTION_DOWN){
                if(y>H*.65f&&y<H*.83f){resetRace();screen=1;} else if(y>H*.84f)screen=0;
            }
            return true;
        }
    }
}
