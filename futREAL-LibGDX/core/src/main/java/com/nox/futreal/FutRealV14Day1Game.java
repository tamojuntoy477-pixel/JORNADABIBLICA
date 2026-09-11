package com.nox.futreal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * futREAL - 10 day rebuild / DAY 1.
 * Focus: camera, player scale and match HUD only.
 * Gameplay remains inherited from the proven v10-v13 stack.
 */
public class FutRealV14Day1Game extends FutRealV13Game {
    private static final float FIELD_W = 68f;
    private static final float FIELD_L = 105f;

    private PerspectiveCamera camera;
    private Method render3D;
    private Method renderHdPlayers;

    private Field screenField;
    private Field ballPosField;
    private Field ballVelField;
    private Field controlledField;
    private Field blueField;
    private Field redField;
    private Field blueScoreField;
    private Field redScoreField;
    private Field matchSecondsField;
    private Field moveField;
    private Field fullTimeField;
    private Field bannerTimerField;
    private Field bannerField;

    private Field posField;
    private Field indexField;

    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont scoreFont;
    private BitmapFont labelFont;
    private BitmapFont tinyFont;
    private final Matrix4 ui = new Matrix4();
    private int sw = 1;
    private int sh = 1;
    private boolean day1Ready;

    private final Vector3 camFocus = new Vector3();
    private final Vector3 camDesired = new Vector3();

    @Override
    public void create() {
        super.create();
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        scoreFont = new BitmapFont();
        labelFont = new BitmapFont();
        tinyFont = new BitmapFont();
        scoreFont.getData().setScale(1.18f);
        labelFont.getData().setScale(0.82f);
        tinyFont.getData().setScale(0.66f);

        try {
            bindDay1();
            increasePlayerPresence();
            day1Ready = true;
        } catch (Throwable t) {
            day1Ready = false;
            Gdx.app.error("futREAL-DAY1", "Day 1 setup failed; v13 remains playable", t);
        }
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private Method method(Class<?> type, String name, Class<?>... args) throws Exception {
        Method m = type.getDeclaredMethod(name, args);
        m.setAccessible(true);
        return m;
    }

    @SuppressWarnings("unchecked")
    private void bindDay1() throws Exception {
        Class<?> base = FutRealV7Game.class;
        screenField = field(base, "screen");
        ballPosField = field(base, "ballPos");
        ballVelField = field(base, "ballVel");
        controlledField = field(base, "controlled");
        blueField = field(base, "blue");
        redField = field(base, "red");
        blueScoreField = field(base, "blueScore");
        redScoreField = field(base, "redScore");
        matchSecondsField = field(base, "matchRealSeconds");
        moveField = field(base, "move");
        fullTimeField = field(base, "fullTime");
        bannerTimerField = field(base, "bannerTimer");
        bannerField = field(base, "banner");
        camera = (PerspectiveCamera)field(base, "camera").get(this);
        render3D = method(base, "render3D");
        renderHdPlayers = method(FutRealV12Game.class, "renderHdPlayers", float.class);

        Array<Object> blue = (Array<Object>)blueField.get(this);
        if (blue == null || blue.size == 0) throw new IllegalStateException("No players in match simulation");
        Class<?> pc = blue.first().getClass();
        posField = field(pc, "pos");
        indexField = field(pc, "index");
    }

    private void increasePlayerPresence() throws Exception {
        // V12 normalizes the rig to ~1.82m. A small +6% makes players read much
        // better on a phone without turning them into oversized arcade figures.
        Field scale = field(FutRealV12Game.class, "modelScale");
        float current = scale.getFloat(this);
        if (current > 0f) scale.setFloat(this, current * 1.06f);
    }

    private boolean isMatch() {
        try {
            Object s = screenField.get(this);
            return s != null && "MATCH".equals(s.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void updateBroadcastCamera(float dt) {
        try {
            Vector3 ball = (Vector3)ballPosField.get(this);
            Vector3 vel = (Vector3)ballVelField.get(this);
            Object controlled = controlledField.get(this);
            Vector3 cp = controlled == null ? ball : (Vector3)posField.get(controlled);

            // Keep the ball central but bias toward the controlled footballer.
            camFocus.set(
                ball.x * 0.42f + cp.x * 0.58f,
                0.92f,
                ball.z * 0.54f + cp.z * 0.46f
            );

            float speed = vel == null ? 0f : vel.len();
            float targetFov = 32.8f + MathUtils.clamp(speed * 0.105f, 0f, 4.2f);
            camera.fieldOfView = MathUtils.lerp(camera.fieldOfView, targetFov, MathUtils.clamp(dt * 7.5f, 0f, 1f));

            // Lower + closer than v13. This makes bodies readable while still
            // retaining enough tactical context around the ball.
            camDesired.set(
                36.4f,
                10.7f,
                MathUtils.clamp(camFocus.z + 2.1f, -45.0f, 45.0f)
            );
            camera.position.set(camDesired);
            camera.up.set(Vector3.Y);
            camera.lookAt(MathUtils.clamp(camFocus.x, -18.5f, 18.5f), 0.78f, camFocus.z - 1.2f);
            camera.near = 0.06f;
            camera.far = 330f;
            camera.update();
        } catch (Throwable ignored) { }
    }

    @Override
    public void render() {
        // Let the current game process all gameplay, AI, career state and input.
        super.render();
        if (!day1Ready || !isMatch()) return;

        try {
            float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);
            updateBroadcastCamera(dt);

            // Redraw only the final presentation using the Day-1 camera.
            // The render3D clear removes the old HUD before our own HUD is drawn.
            render3D.invoke(this);
            renderHdPlayers.invoke(this, dt);
            drawDay1Hud();
        } catch (Throwable t) {
            Gdx.app.error("futREAL-DAY1", "Day 1 frame failed", t);
        }
    }

    @SuppressWarnings("unchecked")
    private void drawDay1Hud() {
        shapes.setProjectionMatrix(ui);
        batch.setProjectionMatrix(ui);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        int bs = 0;
        int rs = 0;
        int minute = 0;
        boolean fullTime = false;
        float bannerTimer = 0f;
        String banner = "";
        Vector2 move = new Vector2();
        Object controlled = null;
        try {
            bs = blueScoreField.getInt(this);
            rs = redScoreField.getInt(this);
            minute = Math.min(90, (int)(matchSecondsField.getFloat(this) * 0.60f));
            fullTime = fullTimeField.getBoolean(this);
            bannerTimer = bannerTimerField.getFloat(this);
            banner = String.valueOf(bannerField.get(this));
            move.set((Vector2)moveField.get(this));
            controlled = controlledField.get(this);
        } catch (Throwable ignored) { }

        float scoreW = Math.min(390f, Math.max(300f, sw * 0.30f));
        float scoreH = Math.min(58f, Math.max(48f, sh * 0.072f));
        float scoreX = (sw - scoreW) * 0.5f;
        float scoreY = sh - scoreH - 10f;

        float joyX = Math.max(95f, sw * 0.095f);
        float joyY = Math.max(80f, sh * 0.145f);
        float joyR = Math.min(63f, sh * 0.095f);

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Compact brand. It no longer steals a large chunk of the match view.
        shapes.setColor(0.004f, 0.009f, 0.014f, 0.78f);
        shapes.rect(12f, sh - 42f, 126f, 28f);
        shapes.setColor(0.62f, 1f, 0.10f, 0.96f);
        shapes.rect(12f, sh - 17f, 126f, 3f);

        // Broadcast scoreboard.
        shapes.setColor(0.003f, 0.008f, 0.014f, 0.88f);
        shapes.rect(scoreX, scoreY, scoreW, scoreH);
        shapes.setColor(0.62f, 1f, 0.10f, 0.95f);
        shapes.rect(scoreX, scoreY, 3f, scoreH);
        shapes.rect(scoreX + scoreW - 3f, scoreY, 3f, scoreH);
        shapes.setColor(0.28f, 0.67f, 0.94f, 0.96f);
        shapes.rect(scoreX + 22f, scoreY + 13f, 18f, scoreH - 26f);
        shapes.setColor(0.76f, 0.05f, 0.07f, 0.96f);
        shapes.rect(scoreX + scoreW - 40f, scoreY + 13f, 18f, scoreH - 26f);

        // Glass joystick.
        shapes.setColor(0.005f, 0.012f, 0.018f, 0.36f);
        shapes.circle(joyX, joyY, joyR, 44);
        shapes.setColor(0.60f, 0.66f, 0.70f, 0.38f);
        shapes.circle(joyX + move.x * joyR * 0.47f, joyY + move.y * joyR * 0.47f, joyR * 0.27f, 30);

        // Action buttons line up with the real v7/v9 touch zones.
        button(sw * 0.910f, sh * 0.180f, sh * 0.070f, new Color(0.93f, 0.15f, 0.12f, 0.68f));
        button(sw * 0.755f, sh * 0.135f, sh * 0.052f, new Color(0.10f, 0.50f, 0.96f, 0.66f));
        button(sw * 0.755f, sh * 0.385f, sh * 0.052f, new Color(0.62f, 0.96f, 0.12f, 0.64f));
        button(sw * 0.910f, sh * 0.760f, sh * 0.043f, new Color(0.48f, 0.55f, 0.63f, 0.58f));
        button(sw * 0.585f, sh * 0.150f, sh * 0.045f, new Color(0.08f, 0.46f, 0.92f, 0.58f));
        button(sw * 0.585f, sh * 0.390f, sh * 0.045f, new Color(0.73f, 0.92f, 0.14f, 0.58f));

        // Compact radar.
        float rw = Math.min(168f, sw * 0.125f);
        float rh = rw * 0.60f;
        float rx = (sw - rw) * 0.5f;
        float ry = 10f;
        shapes.setColor(0.002f, 0.009f, 0.012f, 0.68f);
        shapes.rect(rx - 5f, ry - 4f, rw + 10f, rh + 8f);
        shapes.setColor(0.035f, 0.18f, 0.060f, 0.72f);
        shapes.rect(rx, ry, rw, rh);
        shapes.setColor(0.92f, 0.94f, 0.92f, 0.42f);
        shapes.rect(rx + rw * 0.5f - 0.65f, ry, 1.3f, rh);

        // Small player indicator above the controlled footballer.
        if (controlled != null) {
            try {
                Vector3 p = ((Vector3)posField.get(controlled)).cpy();
                p.y = 3.0f;
                camera.project(p);
                float x = p.x;
                float y = p.y + 9f;
                shapes.setColor(0.62f, 1f, 0.10f, 0.98f);
                shapes.triangle(x, y - 8f, x - 7f, y + 3f, x + 7f, y + 3f);
            } catch (Throwable ignored) { }
        }
        shapes.end();

        try {
            drawRadar((Array<Object>)blueField.get(this), rx, ry, rw, rh, new Color(0.18f, 0.63f, 1f, 1f));
            drawRadar((Array<Object>)redField.get(this), rx, ry, rw, rh, new Color(1f, 0.16f, 0.18f, 1f));
        } catch (Throwable ignored) { }

        batch.begin();
        tinyFont.setColor(new Color(0.67f, 1f, 0.14f, 1f));
        tinyFont.draw(batch, "futREAL  DAY 1", 20f, sh - 23f);

        scoreFont.setColor(Color.WHITE);
        scoreFont.draw(batch, "AUR   " + bs + "  -  " + rs + "   VIL", scoreX + 54f, scoreY + scoreH * 0.67f);
        tinyFont.setColor(new Color(0.68f, 1f, 0.14f, 1f));
        tinyFont.draw(batch, String.format("%02d'", minute), scoreX + scoreW - 71f, scoreY + scoreH * 0.66f);

        labelFont.setColor(Color.WHITE);
        label("CHUTE", sw * 0.910f, sh * 0.180f);
        label("PASSE", sw * 0.755f, sh * 0.135f);
        label("SPRINT", sw * 0.755f, sh * 0.385f);
        label("TROCA", sw * 0.910f, sh * 0.760f);
        label("ENFIADA", sw * 0.585f, sh * 0.150f);
        label("DRIBLE", sw * 0.585f, sh * 0.390f);

        if ((bannerTimer > 0f || fullTime) && banner != null && !banner.isEmpty()) {
            scoreFont.setColor(Color.WHITE);
            scoreFont.draw(batch, banner, sw * 0.43f, sh * 0.70f);
        }
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void button(float x, float y, float r, Color c) {
        float rr = Math.max(21f, r);
        shapes.setColor(c);
        shapes.circle(x, y, rr, 36);
        shapes.setColor(0.95f, 0.98f, 1f, 0.15f);
        shapes.circle(x, y, rr * 0.78f, 32);
    }

    private void label(String text, float x, float y) {
        float approx = text.length() * 3.0f;
        labelFont.draw(batch, text, x - approx, y + 4f);
    }

    private void drawRadar(Array<Object> team, float rx, float ry, float rw, float rh, Color c) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(c);
        for (Object player : team) {
            try {
                Vector3 p = (Vector3)posField.get(player);
                float px = rx + ((p.z / FIELD_L) + 0.5f) * rw;
                float py = ry + (0.5f - p.x / FIELD_W) * rh;
                shapes.circle(px, py, Math.max(1.7f, sh * 0.0032f), 10);
            } catch (Throwable ignored) { }
        }
        shapes.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        sw = Math.max(1, width);
        sh = Math.max(1, height);
        ui.setToOrtho2D(0f, 0f, sw, sh);
    }

    @Override
    public void dispose() {
        if (shapes != null) shapes.dispose();
        if (batch != null) batch.dispose();
        if (scoreFont != null) scoreFont.dispose();
        if (labelFont != null) labelFont.dispose();
        if (tinyFont != null) tinyFont.dispose();
        super.dispose();
    }
}
