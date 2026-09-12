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
import com.badlogic.gdx.math.Vector3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * futREAL - DAY 30 upgrade.
 * Focus: tighter broadcast framing, smoother camera tracking and a cleaner
 * premium presentation while preserving the proven Day 10 gameplay stack.
 */
public class FutRealV30Game extends FutRealV23Day10FinalGame {
    private Field screenField;
    private Field ballPosField;
    private Field ballVelField;
    private Field controlledField;
    private Field playerPosField;
    private PerspectiveCamera camera;
    private Method renderDay3Presentation;

    private ShapeRenderer badgeShapes;
    private SpriteBatch badgeBatch;
    private BitmapFont badgeFont;
    private final Matrix4 ui = new Matrix4();
    private final Vector3 focus = new Vector3();
    private final Vector3 desired = new Vector3();
    private boolean day30Ready;

    @Override
    public void create() {
        super.create();
        badgeShapes = new ShapeRenderer();
        badgeBatch = new SpriteBatch();
        badgeFont = new BitmapFont();
        badgeFont.getData().setScale(0.74f);

        try {
            bindDay30();
            day30Ready = true;
        } catch (Throwable t) {
            day30Ready = false;
            Gdx.app.error("futREAL-DAY30", "Day 30 setup failed; Day 10 remains playable", t);
        }
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

    private void bindDay30() throws Exception {
        Class<?> base = FutRealV7Game.class;
        screenField = field(base, "screen");
        ballPosField = field(base, "ballPos");
        ballVelField = field(base, "ballVel");
        controlledField = field(base, "controlled");
        camera = (PerspectiveCamera) field(base, "camera").get(this);

        Object controlled = controlledField.get(this);
        if (controlled == null) throw new IllegalStateException("No controlled player");
        playerPosField = field(controlled.getClass(), "pos");

        renderDay3Presentation = method(FutRealV16Day3Game.class, "renderDay3Presentation", float.class);
    }

    private boolean isMatch() {
        try {
            Object s = screenField.get(this);
            return s != null && "MATCH".equals(s.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void render() {
        super.render();
        if (!day30Ready || !isMatch()) return;

        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);
        try {
            updateDay30Camera(dt);
            // Final redraw uses the Day 30 camera while keeping the rigged players,
            // football poses and compact HUD from the proven animation stack.
            renderDay3Presentation.invoke(this, dt);
            drawDay30Badge();
        } catch (Throwable t) {
            Gdx.app.error("futREAL-DAY30", "Day 30 frame failed", t);
        }
    }

    private void updateDay30Camera(float dt) throws Exception {
        Vector3 ball = (Vector3) ballPosField.get(this);
        Vector3 vel = (Vector3) ballVelField.get(this);
        Object controlled = controlledField.get(this);
        Vector3 cp = controlled == null ? ball : (Vector3) playerPosField.get(controlled);

        focus.set(
            ball.x * 0.48f + cp.x * 0.52f,
            0.95f,
            ball.z * 0.58f + cp.z * 0.42f
        );

        float horizontalSpeed = vel == null ? 0f : (float)Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        float targetFov = 38.5f + MathUtils.clamp(horizontalSpeed * 0.10f, 0f, 4.8f);
        camera.fieldOfView = MathUtils.lerp(camera.fieldOfView, targetFov,
            MathUtils.clamp(dt * 5.4f, 0f, 1f));

        // Closer/lower television angle than Day 1: players read larger while
        // enough teammates stay visible for passing decisions.
        desired.set(
            32.4f,
            11.8f,
            MathUtils.clamp(focus.z + 3.4f, -44f, 44f)
        );
        float smooth = 1f - (float)Math.pow(0.018f, dt);
        camera.position.lerp(desired, smooth);
        camera.up.set(Vector3.Y);
        camera.lookAt(MathUtils.clamp(focus.x, -20f, 20f), 0.88f, focus.z - 1.8f);
        camera.near = 0.06f;
        camera.far = 340f;
        camera.update();
    }

    private void drawDay30Badge() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        ui.setToOrtho2D(0f, 0f, w, h);
        badgeShapes.setProjectionMatrix(ui);
        badgeBatch.setProjectionMatrix(ui);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        badgeShapes.begin(ShapeRenderer.ShapeType.Filled);
        // Cover the inherited dev badge with the final Day 30 identity.
        badgeShapes.setColor(0.002f, 0.008f, 0.014f, 0.96f);
        badgeShapes.rect(8f, h - 48f, 170f, 38f);
        badgeShapes.setColor(0.58f, 0.97f, 0.12f, 1f);
        badgeShapes.rect(8f, h - 14f, 170f, 4f);
        badgeShapes.end();

        badgeBatch.begin();
        badgeFont.setColor(Color.WHITE);
        badgeFont.draw(badgeBatch, "futREAL  DAY 30", 20f, h - 25f);
        badgeBatch.end();
    }

    @Override
    public void dispose() {
        if (badgeShapes != null) badgeShapes.dispose();
        if (badgeBatch != null) badgeBatch.dispose();
        if (badgeFont != null) badgeFont.dispose();
        super.dispose();
    }
}
