package com.nox.futreal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * futREAL - 10 day rebuild / DAY 3.
 * Focus: football-specific body language layered over the rigged GLB players.
 * Adds a procedural pass/kick follow-through plus dribble/sprint body lean while
 * keeping the existing idle/walk/run clips and Day 1/Day 2 gameplay intact.
 */
public class FutRealV16Day3Game extends FutRealV15Day2Game {
    private static final int ACTION_NONE = 0;
    private static final int ACTION_PASS = 1;
    private static final int ACTION_SHOT = 2;

    private Field screenField;
    private Field controlledField;
    private Field ballPosField;
    private Field ballVelField;
    private Field actionCooldownField;
    private Field moveField;
    private Field sprintField;

    private Field playerPosField;

    private Field hdPlayersField;
    private Field sceneManagerField;
    private Field hdSimField;
    private Field hdSceneField;

    private Method render3DMethod;
    private Method updateHdTransformsMethod;
    private Method drawDay1HudMethod;

    private ShapeRenderer badgeShapes;
    private SpriteBatch badgeBatch;
    private BitmapFont badgeFont;
    private final Matrix4 badgeUi = new Matrix4();

    private boolean day3Ready;
    private float lastActionCooldown;
    private float footballActionTimer;
    private float footballActionDuration;
    private int footballAction = ACTION_NONE;
    private Object footballActionOwner;

    private final Quaternion poseRotation = new Quaternion();

    @Override
    public void create() {
        super.create();
        badgeShapes = new ShapeRenderer();
        badgeBatch = new SpriteBatch();
        badgeFont = new BitmapFont();
        badgeFont.getData().setScale(0.72f);

        try {
            bindDay3();
            day3Ready = true;
        } catch (Throwable t) {
            day3Ready = false;
            Gdx.app.error("futREAL-DAY3", "Day 3 animation setup failed; Day 2 remains playable", t);
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

    @SuppressWarnings("unchecked")
    private void bindDay3() throws Exception {
        Class<?> base = FutRealV7Game.class;
        screenField = field(base, "screen");
        controlledField = field(base, "controlled");
        ballPosField = field(base, "ballPos");
        ballVelField = field(base, "ballVel");
        actionCooldownField = field(base, "actionCooldown");
        moveField = field(base, "move");
        sprintField = field(base, "sprint");
        render3DMethod = method(base, "render3D");

        Object controlled = controlledField.get(this);
        if (controlled == null) throw new IllegalStateException("No controlled player for Day 3");
        playerPosField = field(controlled.getClass(), "pos");

        hdPlayersField = field(FutRealV12Game.class, "hdPlayers");
        sceneManagerField = field(FutRealV12Game.class, "sceneManager");
        updateHdTransformsMethod = method(FutRealV12Game.class, "updateHdTransforms", float.class);
        drawDay1HudMethod = method(FutRealV14Day1Game.class, "drawDay1Hud");

        Array<Object> hdPlayers = (Array<Object>) hdPlayersField.get(this);
        if (hdPlayers == null || hdPlayers.size == 0) throw new IllegalStateException("HD player scenes unavailable");
        Object hd = hdPlayers.first();
        hdSimField = field(hd.getClass(), "sim");
        hdSceneField = field(hd.getClass(), "scene");
    }

    private boolean isMatch() {
        try {
            Object screen = screenField.get(this);
            return screen != null && "MATCH".equals(screen.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void render() {
        super.render();
        if (!day3Ready || !isMatch()) return;

        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);
        try {
            detectFootballAction();
            footballActionTimer = Math.max(0f, footballActionTimer - dt);
            if (footballActionTimer <= 0f) {
                footballAction = ACTION_NONE;
                footballActionOwner = null;
            }
            renderDay3Presentation(dt);
        } catch (Throwable t) {
            Gdx.app.error("futREAL-DAY3", "Day 3 frame failed", t);
        }
    }

    private void detectFootballAction() throws Exception {
        float cooldown = actionCooldownField.getFloat(this);
        Vector3 ballVel = (Vector3) ballVelField.get(this);
        Vector3 ballPos = (Vector3) ballPosField.get(this);
        Object controlled = controlledField.get(this);

        if (controlled != null && cooldown > lastActionCooldown + 0.075f) {
            Vector3 playerPos = (Vector3) playerPosField.get(controlled);
            float horizontalSpeed = (float)Math.sqrt(ballVel.x * ballVel.x + ballVel.z * ballVel.z);
            float distance = playerPos.dst(ballPos.x, 0f, ballPos.z);
            if (distance < 4.0f && horizontalSpeed > 10.5f) {
                footballAction = horizontalSpeed > 22.5f ? ACTION_SHOT : ACTION_PASS;
                footballActionDuration = footballAction == ACTION_SHOT ? 0.46f : 0.34f;
                footballActionTimer = footballActionDuration;
                footballActionOwner = controlled;
            }
        }
        lastActionCooldown = cooldown;
    }

    @SuppressWarnings("unchecked")
    private void renderDay3Presentation(float dt) throws Exception {
        // Clear the inherited HUD/world picture once and redraw the final frame with
        // animation poses applied after the GLB animation controller has updated.
        render3DMethod.invoke(this);
        updateHdTransformsMethod.invoke(this, dt);

        Array<Object> hdPlayers = (Array<Object>) hdPlayersField.get(this);
        Object controlled = controlledField.get(this);
        Vector2 move = (Vector2) moveField.get(this);
        boolean sprint = sprintField.getBoolean(this);

        for (Object hd : hdPlayers) {
            Object sim = hdSimField.get(hd);
            Scene scene = (Scene) hdSceneField.get(hd);
            if (scene == null || scene.modelInstance == null) continue;

            if (footballAction != ACTION_NONE && sim == footballActionOwner) {
                applyKickPose(scene, footballAction == ACTION_SHOT,
                    1f - MathUtils.clamp(footballActionTimer / Math.max(0.001f, footballActionDuration), 0f, 1f));
            } else if (sim == controlled) {
                applyControlPose(scene, move, sprint);
            }
        }

        SceneManager manager = (SceneManager) sceneManagerField.get(this);
        if (manager != null) {
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthMask(true);
            manager.render();
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        }

        drawDay1HudMethod.invoke(this);
        drawDay3Badge();
    }

    private void applyKickPose(Scene scene, boolean shot, float progress) {
        float upper;
        float lower;
        float torso;
        float plant;

        if (progress < 0.30f) {
            float t = progress / 0.30f;
            upper = MathUtils.lerp(0f, shot ? -31f : -20f, t);
            lower = MathUtils.lerp(0f, shot ? 43f : 29f, t);
            torso = MathUtils.lerp(0f, shot ? -7f : -4f, t);
            plant = MathUtils.lerp(0f, 7f, t);
        } else if (progress < 0.66f) {
            float t = (progress - 0.30f) / 0.36f;
            upper = MathUtils.lerp(shot ? -31f : -20f, shot ? 58f : 39f, t);
            lower = MathUtils.lerp(shot ? 43f : 29f, shot ? -12f : -5f, t);
            torso = MathUtils.lerp(shot ? -7f : -4f, shot ? 10f : 6f, t);
            plant = MathUtils.lerp(7f, 11f, t);
        } else {
            float t = (progress - 0.66f) / 0.34f;
            upper = MathUtils.lerp(shot ? 58f : 39f, shot ? 20f : 12f, t);
            lower = MathUtils.lerp(shot ? -12f : -5f, 8f, t);
            torso = MathUtils.lerp(shot ? 10f : 6f, 1f, t);
            plant = MathUtils.lerp(11f, 2f, t);
        }

        rotate(scene, "UpperLeg.R", Vector3.X, upper);
        rotate(scene, "LowerLeg.R", Vector3.X, lower);
        rotate(scene, "UpperLeg.L", Vector3.X, -plant);
        rotate(scene, "LowerLeg.L", Vector3.X, plant * 0.72f);
        rotate(scene, "Abdomen", Vector3.X, torso * 0.55f);
        rotate(scene, "Torso", Vector3.X, torso);

        float arm = shot ? 18f : 12f;
        float armPulse = MathUtils.sin(MathUtils.PI * MathUtils.clamp(progress, 0f, 1f));
        rotate(scene, "UpperArm.L", Vector3.X, arm * armPulse);
        rotate(scene, "UpperArm.R", Vector3.X, -arm * armPulse);
        rotate(scene, "Shoulder.L", Vector3.Z, -7f * armPulse);
        rotate(scene, "Shoulder.R", Vector3.Z, 7f * armPulse);

        scene.modelInstance.calculateTransforms();
    }

    private void applyControlPose(Scene scene, Vector2 move, boolean sprint) {
        if (move == null || move.len2() < 0.015f) return;
        float amount = MathUtils.clamp(move.len(), 0f, 1f);
        float lateral = MathUtils.clamp(move.x, -1f, 1f);
        float forwardLean = sprint ? -7.5f * amount : -2.8f * amount;
        float sideLean = -5.0f * lateral * amount;

        rotate(scene, "Abdomen", Vector3.X, forwardLean * 0.45f);
        rotate(scene, "Torso", Vector3.X, forwardLean);
        rotate(scene, "Torso", Vector3.Z, sideLean);
        rotate(scene, "Neck", Vector3.Z, -sideLean * 0.35f);

        if (!sprint) {
            // Small side-foot body cue while carrying the ball at close-control speed.
            rotate(scene, "UpperLeg.R", Vector3.Z, lateral * 4.5f * amount);
            rotate(scene, "UpperLeg.L", Vector3.Z, lateral * 2.5f * amount);
        }
        scene.modelInstance.calculateTransforms();
    }

    private void rotate(Scene scene, String nodeId, Vector3 axis, float degrees) {
        Node node = findNode(scene.modelInstance.nodes, nodeId);
        if (node == null || Math.abs(degrees) < 0.01f) return;
        poseRotation.setFromAxis(axis, degrees);
        node.rotation.mul(poseRotation);
    }

    private Node findNode(Array<Node> roots, String id) {
        for (Node node : roots) {
            Node found = findNode(node, id);
            if (found != null) return found;
        }
        return null;
    }

    private Node findNode(Node node, String id) {
        if (id.equals(node.id)) return node;
        for (Node child : node.getChildren()) {
            Node found = findNode(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private void drawDay3Badge() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        badgeUi.setToOrtho2D(0f, 0f, w, h);
        badgeShapes.setProjectionMatrix(badgeUi);
        badgeBatch.setProjectionMatrix(badgeUi);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        badgeShapes.begin(ShapeRenderer.ShapeType.Filled);
        badgeShapes.setColor(0.003f, 0.009f, 0.014f, 0.94f);
        badgeShapes.rect(10f, h - 44f, 148f, 32f);
        badgeShapes.setColor(0.62f, 1f, 0.10f, 1f);
        badgeShapes.rect(10f, h - 15f, 148f, 3f);
        badgeShapes.end();

        badgeBatch.begin();
        badgeFont.setColor(Color.WHITE);
        badgeFont.draw(badgeBatch, "futREAL  DAY 3", 19f, h - 23f);
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
