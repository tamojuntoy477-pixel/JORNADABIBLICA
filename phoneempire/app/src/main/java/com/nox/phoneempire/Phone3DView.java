package com.nox.phoneempire;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Phone3DView extends GLSurfaceView {
    private final PhoneRenderer renderer;
    private float lastX, lastY;

    public Phone3DView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new PhoneRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public void setBodyColor(float r,float g,float b) { renderer.setBodyColor(r,g,b); }
    public void setAutoRotate(boolean value) { renderer.autoRotate=value; }
    public boolean isAutoRotate(){ return renderer.autoRotate; }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX(), y=e.getY();
        if(e.getAction()==MotionEvent.ACTION_MOVE){
            float dx=x-lastX, dy=y-lastY;
            renderer.angleY += dx*0.45f;
            renderer.angleX += dy*0.35f;
            renderer.angleX=Math.max(-70f,Math.min(70f,renderer.angleX));
        }
        lastX=x; lastY=y;
        return true;
    }

    static class PhoneRenderer implements GLSurfaceView.Renderer {
        private final float[] projection=new float[16];
        private final float[] view=new float[16];
        private final float[] model=new float[16];
        private final float[] mv=new float[16];
        private final float[] mvp=new float[16];
        private int program, posLoc, matrixLoc, colorLoc;
        volatile float angleX=-12f, angleY=-24f;
        volatile boolean autoRotate=true;
        private float bodyR=0.18f, bodyG=0.18f, bodyB=0.22f;
        private final FloatBuffer cube;

        private final float[] cubeVertices={
            -0.5f,-0.5f, 0.5f,   0.5f,-0.5f, 0.5f,   0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f, 0.5f,   0.5f, 0.5f, 0.5f,  -0.5f, 0.5f, 0.5f,
             0.5f,-0.5f,-0.5f,  -0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
             0.5f,-0.5f,-0.5f,  -0.5f, 0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
            -0.5f,-0.5f,-0.5f,  -0.5f,-0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f,  -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,-0.5f,
             0.5f,-0.5f, 0.5f,   0.5f,-0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
             0.5f,-0.5f, 0.5f,   0.5f, 0.5f,-0.5f,  0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f,   0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f,
            -0.5f, 0.5f, 0.5f,   0.5f, 0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
            -0.5f,-0.5f,-0.5f,   0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f,   0.5f,-0.5f, 0.5f, -0.5f,-0.5f, 0.5f
        };

        PhoneRenderer(){
            ByteBuffer bb=ByteBuffer.allocateDirect(cubeVertices.length*4).order(ByteOrder.nativeOrder());
            cube=bb.asFloatBuffer(); cube.put(cubeVertices); cube.position(0);
        }

        void setBodyColor(float r,float g,float b){bodyR=r;bodyG=g;bodyB=b;}

        @Override public void onSurfaceCreated(javax.microedition.khronos.egl.EGLConfig config){
            GLES20.glClearColor(0.027f,0.039f,0.063f,1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            String vs="uniform mat4 uMVP; attribute vec4 aPos; void main(){ gl_Position=uMVP*aPos; }";
            String fs="precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }";
            program=link(vs,fs);
            posLoc=GLES20.glGetAttribLocation(program,"aPos");
            matrixLoc=GLES20.glGetUniformLocation(program,"uMVP");
            colorLoc=GLES20.glGetUniformLocation(program,"uColor");
        }

        @Override public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl,int w,int h){
            GLES20.glViewport(0,0,w,h);
            float ratio=(float)w/(float)Math.max(1,h);
            Matrix.perspectiveM(projection,0,42f,ratio,0.1f,100f);
            Matrix.setLookAtM(view,0,0f,0f,5.4f,0f,0f,0f,0f,1f,0f);
        }

        @Override public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl){
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            if(autoRotate) angleY+=0.22f;
            GLES20.glUseProgram(program);
            cube.position(0);
            GLES20.glEnableVertexAttribArray(posLoc);
            GLES20.glVertexAttribPointer(posLoc,3,GLES20.GL_FLOAT,false,0,cube);

            // phone body
            drawBox(0,0,0,1.62f,3.15f,0.24f,bodyR,bodyG,bodyB,1f);

            // glossy front screen
            drawBox(0,0,0.135f,1.48f,2.88f,0.035f,0.03f,0.05f,0.09f,1f);
            drawBox(0,1.31f,0.158f,0.32f,0.045f,0.02f,0.10f,0.11f,0.15f,1f);

            // back camera island
            drawBox(-0.46f,0.93f,-0.165f,0.56f,0.78f,0.08f,0.10f,0.10f,0.13f,1f);
            drawBox(-0.57f,1.13f,-0.225f,0.19f,0.19f,0.055f,0.01f,0.01f,0.015f,1f);
            drawBox(-0.35f,1.13f,-0.225f,0.19f,0.19f,0.055f,0.01f,0.01f,0.015f,1f);
            drawBox(-0.57f,0.88f,-0.225f,0.19f,0.19f,0.055f,0.01f,0.01f,0.015f,1f);
            drawBox(-0.35f,0.88f,-0.225f,0.10f,0.10f,0.04f,0.72f,0.74f,0.78f,1f);

            // side buttons
            drawBox(0.835f,0.43f,0,0.045f,0.52f,0.11f,0.35f,0.35f,0.39f,1f);
            drawBox(0.835f,-0.23f,0,0.045f,0.26f,0.11f,0.35f,0.35f,0.39f,1f);

            GLES20.glDisableVertexAttribArray(posLoc);
        }

        private void drawBox(float tx,float ty,float tz,float sx,float sy,float sz,float r,float g,float b,float a){
            Matrix.setIdentityM(model,0);
            Matrix.rotateM(model,0,angleX,1f,0f,0f);
            Matrix.rotateM(model,0,angleY,0f,1f,0f);
            Matrix.translateM(model,0,tx,ty,tz);
            Matrix.scaleM(model,0,sx,sy,sz);
            Matrix.multiplyMM(mv,0,view,0,model,0);
            Matrix.multiplyMM(mvp,0,projection,0,mv,0);
            GLES20.glUniformMatrix4fv(matrixLoc,1,false,mvp,0);
            GLES20.glUniform4f(colorLoc,r,g,b,a);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,36);
        }

        private int shader(int type,String src){
            int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,src); GLES20.glCompileShader(s); return s;
        }
        private int link(String vs,String fs){
            int p=GLES20.glCreateProgram();
            GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,vs));
            GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,fs));
            GLES20.glLinkProgram(p); return p;
        }
    }
}
