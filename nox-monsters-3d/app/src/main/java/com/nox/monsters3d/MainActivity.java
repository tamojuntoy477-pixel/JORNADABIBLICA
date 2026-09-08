package com.nox.monsters3d;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private GLSurfaceView glView;
    private GameRenderer renderer;
    private float moveX = 0f;
    private float moveZ = 0f;
    private float joyCenterX;
    private float joyCenterY;
    private TextView hud;
    private TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        hideSystemUi();

        FrameLayout root = new FrameLayout(this);
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        renderer = new GameRenderer(this, this::updateHud);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(glView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        hud = new TextView(this);
        hud.setTextColor(Color.WHITE);
        hud.setTextSize(18f);
        hud.setShadowLayer(4f, 2f, 2f, Color.BLACK);
        hud.setText("NOX Monsters 3D   •   Capturados: 0/8");
        FrameLayout.LayoutParams hudParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        hudParams.gravity = Gravity.TOP | Gravity.LEFT;
        hudParams.leftMargin = 24;
        hudParams.topMargin = 18;
        root.addView(hud, hudParams);

        message = new TextView(this);
        message.setTextColor(Color.WHITE);
        message.setTextSize(16f);
        message.setGravity(Gravity.CENTER);
        message.setPadding(22, 10, 22, 10);
        GradientDrawable msgBg = new GradientDrawable();
        msgBg.setColor(0xAA07131D);
        msgBg.setCornerRadius(24f);
        message.setBackground(msgBg);
        message.setText("Explore o campo e chegue perto de uma criatura!");
        FrameLayout.LayoutParams msgParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        msgParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        msgParams.topMargin = 58;
        root.addView(message, msgParams);

        View joystick = new View(this);
        GradientDrawable joyBg = new GradientDrawable();
        joyBg.setShape(GradientDrawable.OVAL);
        joyBg.setColor(0x553E5568);
        joyBg.setStroke(4, 0xAAFFFFFF);
        joystick.setBackground(joyBg);
        FrameLayout.LayoutParams joyParams = new FrameLayout.LayoutParams(190, 190);
        joyParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
        joyParams.leftMargin = 30;
        joyParams.bottomMargin = 26;
        root.addView(joystick, joyParams);

        joystick.setOnTouchListener((v, event) -> {
            float w = v.getWidth();
            float h = v.getHeight();
            if (joyCenterX == 0f && joyCenterY == 0f) {
                joyCenterX = w / 2f;
                joyCenterY = h / 2f;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                moveX = 0f;
                moveZ = 0f;
                if (renderer != null) renderer.setMove(0f, 0f);
                return true;
            }
            float dx = (event.getX() - joyCenterX) / (w / 2f);
            float dy = (event.getY() - joyCenterY) / (h / 2f);
            float len = (float)Math.sqrt(dx * dx + dy * dy);
            if (len > 1f) {
                dx /= len;
                dy /= len;
            }
            moveX = dx;
            moveZ = dy;
            if (renderer != null) renderer.setMove(moveX, moveZ);
            return true;
        });

        Button capture = new Button(this);
        capture.setText("CAPTURAR");
        capture.setTextColor(Color.WHITE);
        capture.setTextSize(17f);
        capture.setAllCaps(false);
        GradientDrawable capBg = new GradientDrawable();
        capBg.setShape(GradientDrawable.OVAL);
        capBg.setColor(0xDDFF5A3C);
        capBg.setStroke(4, 0xFFFFFFFF);
        capture.setBackground(capBg);
        FrameLayout.LayoutParams capParams = new FrameLayout.LayoutParams(190, 190);
        capParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        capParams.rightMargin = 34;
        capParams.bottomMargin = 28;
        root.addView(capture, capParams);
        capture.setOnClickListener(v -> {
            if (renderer != null) renderer.tryCapture();
        });

        TextView hint = new TextView(this);
        hint.setText("Analógico: mover");
        hint.setTextColor(0xDDFFFFFF);
        hint.setTextSize(13f);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        hintParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
        hintParams.leftMargin = 55;
        hintParams.bottomMargin = 8;
        root.addView(hint, hintParams);

        setContentView(root);
    }

    private void updateHud(int captured, int total, String text) {
        runOnUiThread(() -> {
            if (hud != null) {
                hud.setText("NOX Monsters 3D   •   Capturados: " + captured + "/" + total);
            }
            if (message != null && text != null && !text.isEmpty()) {
                message.setText(text);
            }
        });
    }

    private void hideSystemUi() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (glView != null) glView.onResume();
    }

    @Override
    protected void onPause() {
        if (glView != null) glView.onPause();
        super.onPause();
    }
}
