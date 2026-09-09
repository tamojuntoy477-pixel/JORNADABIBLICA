package com.nox.futreal;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class FutRealGameNext extends ApplicationAdapter {
    private static final float W = 68f, L = 105f, HW = 34f, HL = 52.5f;

    private final Array<Model> models = new Array<>();
    private final Array<ModelInstance> scene = new Array<>();
    private final Array<P> blue = new Array<>();
    private final Array<P> red = new Array<>();

    private ModelBatch modelBatch;
    private ModelBuilder mb;
    private Environment env;
    private PerspectiveCamera cam;
    private ModelInstance ballModel;
    private final Vector3 ball = new Vector3(0, .48f, 0);
    private final Vector3 ballV = new Vector3();
    private P hero;

    private SpriteBatch uiBatch;
    private BitmapFont font;
    private ShapeRenderer shapes;
    private final Matrix4 ui = new Matrix4();
    private int sw, sh;
    private final Vector2 move = new Vector2();
    private boolean sprint;
    private float cooldown;
    private int scoreA, scoreB;
    private float time;

    @Override public void create() {
        modelBatch = new ModelBatch();
        mb = new ModelBuilder();
        uiBatch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.35f);
        shapes = new ShapeRenderer();

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, .55f, .58f, .62f, 1));
        env.add(new DirectionalLight().set(1f, .96f, .9f, -.4f, -1f, -.2f));

        cam = new PerspectiveCamera(56f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = .1f;
        cam.far = 220f;

        stadium();
        teams();
        makeBall();
        kickoff();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Material mat(Color c) { return new Material(ColorAttribute.createDiffuse(c)); }
    private Model own(Model m) { models.add(m); return m; }

    private void box(float x,float y,float z,float w,float h,float d,Color c) {
        Model m = own(mb.createBox(w,h,d,mat(c), VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal));
        ModelInstance i = new ModelInstance(m);
        i.transform.setToTranslation(x,y,z);
        scene.add(i);
    }

    private void stadium() {
        box(0,-.2f,0,W,.4f,L,new Color(.025f,.30f,.07f,1));
        for(int i=0;i<10;i++) {
            float z=-47.25f+i*10.5f;
            Color g=(i%2==0)?new Color(.035f,.41f,.10f,1):new Color(.025f,.34f,.075f,1);
            box(0,.01f,z,W,.03f,10.5f,g);
        }
        Color white=new Color(.95f,.96f,.95f,1);
        box(0,.04f,0,W,.04f,.14f,white);
        box(-HW,.04f,0,.14f,.04f,L,white);
        box(HW,.04f,0,.14f,.04f,L,white);
        box(0,.04f,-HL,W,.04f,.14f,white);
        box(0,.04f,HL,W,.04f,.14f,white);

        Color dark=new Color(.04f,.05f,.07f,1);
        box(-43,5,0,15,10,124,dark); box(43,5,0,15,10,124,dark);
        box(0,5,-62,76,10,17,dark); box(0,5,62,76,10,17,dark);

        for(float z:new float[]{-53.1f,53.1f}) {
            box(-7.3f,1.25f,z,.18f,2.5f,.18f,Color.WHITE);
            box(7.3f,1.25f,z,.18f,2.5f,.18f,Color.WHITE);
            box(0,2.5f,z,14.8f,.18f,.18f,Color.WHITE);
        }
    }

    private void teams() {
        Model bm=own(mb.createCapsule(.48f,2.15f,12,mat(new Color(.10f,.62f,1f,1)),VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal));
        Model rm=own(mb.createCapsule(.48f,2.15f,12,mat(new Color(.94f,.10f,.16f,1)),VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal));
        float[][] bf={{0,43},{-22,30},{-8,32},{8,32},{22,30},{-20,13},{0,18},{20,13},{-16,-5},{0,-10},{16,-5}};
        float[][] rf={{0,-43},{-22,-30},{-8,-32},{8,-32},{22,-30},{-20,-13},{0,-18},{20,-13},{-16,5},{0,10},{16,5}};
        for(int i=0;i<11;i++){ P p=new P(new ModelInstance(bm),true,bf[i][0],bf[i][1]); blue.add(p); scene.add(p.m); }
        for(int i=0;i<11;i++){ P p=new P(new ModelInstance(rm),false,rf[i][0],rf[i][1]); red.add(p); scene.add(p.m); }
        hero=blue.get(10);
    }

    private void makeBall() {
        Model m=own(mb.createSphere(.72f,.72f,.72f,18,12,mat(Color.WHITE),VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal));
        ballModel=new ModelInstance(m);
        scene.add(ballModel);
    }

    private void kickoff() {
        for(P p:blue)p.reset(); for(P p:red)p.reset();
        ball.set(0,.48f,0); ballV.setZero(); sync();
    }

    private void sync() {
        for(P p:blue)p.m.transform.setToTranslation(p.pos.x,1.05f,p.pos.z);
        for(P p:red)p.m.transform.setToTranslation(p.pos.x,1.05f,p.pos.z);
        ballModel.transform.setToTranslation(ball);
    }

    @Override public void render() {
        float dt=Math.min(Gdx.graphics.getDeltaTime(),.05f);
        controls();
        update(dt);
        camera(dt);

        Gdx.gl.glViewport(0,0,sw,sh);
        Gdx.gl.glClearColor(.012f,.022f,.035f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(cam);
        for(ModelInstance i:scene)modelBatch.render(i,env);
        modelBatch.end();
        hud();
    }

    private void controls() {
        move.setZero(); sprint=false;
        float cx=Math.max(110f,sw*.11f), cy=Math.max(105f,sh*.18f), r=Math.min(90f,sh*.15f);
        boolean shoot=false, pass=false;
        for(int n=0;n<5;n++) if(Gdx.input.isTouched(n)) {
            float x=Gdx.input.getX(n), y=sh-Gdx.input.getY(n);
            if(x<sw*.43f) {
                float dx=x-cx,dy=y-cy,len=(float)Math.sqrt(dx*dx+dy*dy);
                if(len>7){float s=Math.min(1f,len/r)/len;move.set(dx*s,dy*s);}
            } else if(x>sw*.82f&&y<sh*.42f) shoot=true;
            else if(x>sw*.64f&&x<sw*.83f&&y<sh*.28f) pass=true;
            else if(x>sw*.68f&&x<sw*.84f&&y>sh*.30f&&y<sh*.54f) sprint=true;
        }
        if(shoot)kick(27f,4.2f); else if(pass)kick(15f,1.2f);
    }

    private void kick(float power,float lift) {
        if(cooldown>0||hero.pos.dst(ball)>2.7f)return;
        Vector3 d=hero.face.cpy(); if(d.len2()<.01f)d.set(0,0,-1); d.nor();
        ballV.set(d.x*power,lift,d.z*power); cooldown=.28f;
    }

    private void update(float dt) {
        time+=dt; cooldown=Math.max(0,cooldown-dt);
        Vector3 d=new Vector3(move.x,0,-move.y);
        if(d.len2()>.01f){d.nor();hero.face.lerp(d,.28f).nor();hero.pos.mulAdd(d,(sprint?12.2f:8.3f)*dt);}
        clamp(hero);
        ai(blue,true,dt); ai(red,false,dt);

        ballV.y-=9.8f*dt; ball.mulAdd(ballV,dt);
        if(ball.y<.48f){ball.y=.48f;if(ballV.y<0)ballV.y*=-.25f;float f=(float)Math.pow(.985f,dt*60f);ballV.x*=f;ballV.z*=f;}
        if(Math.abs(ball.x)>HW){ball.x=MathUtils.clamp(ball.x,-HW,HW);ballV.x*=-.5f;}

        if(ball.z<-HL-.6f&&Math.abs(ball.x)<7.3f){scoreA++;kickoff();}
        else if(ball.z>HL+.6f&&Math.abs(ball.x)<7.3f){scoreB++;kickoff();}
        else if(Math.abs(ball.z)>HL+2f){ball.z=MathUtils.clamp(ball.z,-HL,HL);ballV.z*=-.45f;}
        sync();
    }

    private void ai(Array<P> team,boolean isBlue,float dt) {
        for(P p:team){
            if(p==hero)continue;
            float db=p.pos.dst(ball); Vector3 target=p.home.cpy();
            if(db<17f||(isBlue&&ball.z>0)||(!isBlue&&ball.z<0))target.lerp(new Vector3(ball.x,0,ball.z),.40f);
            Vector3 d=target.sub(p.pos);
            if(d.len2()>.25f){d.nor();p.face.lerp(d,.18f).nor();p.pos.mulAdd(d,(db<8?7f:5.5f)*dt);clamp(p);}
            if(db<1.6f&&cooldown<=0){float gz=isBlue?-HL:HL;Vector3 k=new Vector3(-ball.x*.04f,0,gz-ball.z).nor();ballV.set(k.x*18,1.3f,k.z*18);cooldown=.18f;}
        }
    }

    private void clamp(P p){p.pos.x=MathUtils.clamp(p.pos.x,-HW+1,HW-1);p.pos.z=MathUtils.clamp(p.pos.z,-HL+1,HL-1);}

    private void camera(float dt) {
        Vector3 focus=hero.pos.cpy().lerp(new Vector3(ball.x,0,ball.z),.2f);
        Vector3 desired=new Vector3(focus.x*.35f,18.5f,focus.z+25f);
        float a=1f-(float)Math.pow(.02f,dt);
        cam.position.lerp(desired,a); cam.up.set(Vector3.Y); cam.lookAt(focus.x,.8f,focus.z-7f); cam.update();
    }

    private void hud() {
        float cx=Math.max(110f,sw*.11f),cy=Math.max(105f,sh*.18f);
        shapes.setProjectionMatrix(ui); shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.02f,.04f,.06f,.86f); shapes.rect(sw*.35f,sh-70,sw*.30f,56);
        shapes.setColor(.04f,.05f,.07f,.80f); shapes.circle(cx,cy,Math.min(90f,sh*.15f),32);
        shapes.setColor(.72f,1f,.20f,.95f); shapes.circle(cx+move.x*60,cy+move.y*60,32,24);
        shapes.setColor(.04f,.05f,.07f,.9f); shapes.circle(sw*.90f,sh*.18f,58,32); shapes.rect(sw*.68f,sh*.08f,120,64); shapes.rect(sw*.70f,sh*.34f,112,58);
        shapes.end();

        uiBatch.setProjectionMatrix(ui); uiBatch.begin();
        font.setColor(Color.WHITE); font.draw(uiBatch,"AUR  "+scoreA+"  -  "+scoreB+"  VIL",sw*.41f,sh-35);
        font.setColor(new Color(.72f,1f,.20f,1)); font.draw(uiBatch,String.format("%02d'",Math.min(90,(int)(time/6f))),sw*.36f,sh-35);
        font.setColor(Color.WHITE); font.draw(uiBatch,"CHUTE",sw*.865f,sh*.19f);font.draw(uiBatch,"PASSE",sw*.695f,sh*.14f);font.draw(uiBatch,"SPRINT",sw*.71f,sh*.40f);
        font.setColor(new Color(.72f,1f,.20f,1));font.draw(uiBatch,"futREAL • LIBGDX 3D",18,sh-22);
        uiBatch.end();
    }

    @Override public void resize(int width,int height){sw=Math.max(1,width);sh=Math.max(1,height);ui.setToOrtho2D(0,0,sw,sh);cam.viewportWidth=sw;cam.viewportHeight=sh;cam.update();}
    @Override public void dispose(){modelBatch.dispose();uiBatch.dispose();font.dispose();shapes.dispose();for(Model m:models)m.dispose();}

    private static class P {
        final ModelInstance m; final Vector3 home=new Vector3(); final Vector3 pos=new Vector3(); final Vector3 face=new Vector3(0,0,-1);
        P(ModelInstance m,boolean blue,float x,float z){this.m=m;home.set(x,0,z);reset();if(!blue)face.set(0,0,1);}
        void reset(){pos.set(home);}
    }
}
