package com.nox.futreal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.lang.reflect.Field;

/**
 * futREAL - 10 day rebuild / DAY 2.
 * Focus: close control, first touch, sprint dribbling and turning response.
 * Presentation remains inherited from Day 1.
 */
public class FutRealV15Day2Game extends FutRealV14Day1Game {
    private Field screenField;
    private Field ballPosField;
    private Field ballVelField;
    private Field controlledField;
    private Field moveField;
    private Field sprintField;
    private Field actionCooldownField;

    private Field posField;
    private Field facingField;

    private boolean day2Ready;
    private float firstTouchCooldown;
    private float sprintTouchCooldown;

    private final Vector3 inputDir = new Vector3();
    private final Vector3 targetBall = new Vector3();
    private final Vector3 flatBallVelocity = new Vector3();

    @Override
    public void create() {
        super.create();
        try {
            bindDay2();
            day2Ready = true;
        } catch (Throwable t) {
            day2Ready = false;
            Gdx.app.error("futREAL-DAY2", "Day 2 setup failed; Day 1 remains playable", t);
        }
    }

    private Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private void bindDay2() throws Exception {
        Class<?> base = FutRealV7Game.class;
        screenField = field(base, "screen");
        ballPosField = field(base, "ballPos");
        ballVelField = field(base, "ballVel");
        controlledField = field(base, "controlled");
        moveField = field(base, "move");
        sprintField = field(base, "sprint");
        actionCooldownField = field(base, "actionCooldown");

        Object controlled = controlledField.get(this);
        if (controlled == null) throw new IllegalStateException("No controlled player");
        Class<?> pc = controlled.getClass();
        posField = field(pc, "pos");
        facingField = field(pc, "facing");
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
        if (!day2Ready || !isMatch()) return;

        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);
        firstTouchCooldown = Math.max(0f, firstTouchCooldown - dt);
        sprintTouchCooldown = Math.max(0f, sprintTouchCooldown - dt);

        try {
            improveBallFeel(dt);
        } catch (Throwable t) {
            Gdx.app.error("futREAL-DAY2", "Day 2 gameplay assist failed", t);
        }
    }

    private void improveBallFeel(float dt) throws Exception {
        Object controlled = controlledField.get(this);
        if (controlled == null) return;

        Vector3 playerPos = (Vector3) posField.get(controlled);
        Vector3 facing = (Vector3) facingField.get(controlled);
        Vector3 ballPos = (Vector3) ballPosField.get(this);
        Vector3 ballVel = (Vector3) ballVelField.get(this);
        Vector2 move = (Vector2) moveField.get(this);
        boolean sprint = sprintField.getBoolean(this);
        float actionCooldown = actionCooldownField.getFloat(this);

        float dx = ballPos.x - playerPos.x;
        float dz = ballPos.z - playerPos.z;
        float ballDistance = (float)Math.sqrt(dx * dx + dz * dz);

        inputDir.set(move.x, 0f, -move.y);
        float inputAmount = inputDir.len();
        if (inputAmount > 0.08f) {
            inputDir.scl(1f / inputAmount);

            // Faster and more natural turning than the old prototype movement.
            float turn = sprint ? 0.26f : 0.42f;
            facing.lerp(inputDir, turn).nor();
        } else if (facing.len2() < 0.01f) {
            facing.set(0f, 0f, -1f);
        }

        flatBallVelocity.set(ballVel.x, 0f, ballVel.z);
        float ballSpeed = flatBallVelocity.len();

        // First-touch trap: tame a ground pass once as it reaches the player.
        // It only activates on medium-speed incoming balls and never after a shot/pass.
        if (firstTouchCooldown <= 0f
            && actionCooldown <= 0.06f
            && ballPos.y < 0.92f
            && ballDistance < 1.62f
            && ballSpeed > 7.2f
            && ballSpeed < 18.5f) {

            Vector3 receiveDir = inputAmount > 0.12f ? inputDir : facing;
            ballVel.x = ballVel.x * 0.42f + receiveDir.x * 2.1f;
            ballVel.z = ballVel.z * 0.42f + receiveDir.z * 2.1f;
            ballVel.y *= 0.35f;

            targetBall.set(playerPos).mulAdd(receiveDir, 1.02f);
            ballPos.x = MathUtils.lerp(ballPos.x, targetBall.x, 0.18f);
            ballPos.z = MathUtils.lerp(ballPos.z, targetBall.z, 0.18f);
            firstTouchCooldown = 0.58f;
            return;
        }

        // Close-control dribbling. The ball stays readable near the feet without
        // becoming magnetized, and the assist disengages immediately for shots/passes.
        if (actionCooldown <= 0.06f
            && ballPos.y < 0.82f
            && inputAmount > 0.12f
            && ballDistance < (sprint ? 2.75f : 2.22f)
            && ballSpeed < (sprint ? 15.0f : 11.0f)) {

            float lead = sprint ? 1.52f : 1.08f;
            targetBall.set(playerPos).mulAdd(inputDir, lead);
            float correction = MathUtils.clamp(dt * (sprint ? 3.9f : 5.3f), 0f, sprint ? 0.17f : 0.23f);
            ballPos.x = MathUtils.lerp(ballPos.x, targetBall.x, correction);
            ballPos.z = MathUtils.lerp(ballPos.z, targetBall.z, correction);

            float wantedSpeed = sprint ? 8.6f : 5.6f;
            ballVel.x = MathUtils.lerp(ballVel.x, inputDir.x * wantedSpeed, sprint ? 0.10f : 0.16f);
            ballVel.z = MathUtils.lerp(ballVel.z, inputDir.z * wantedSpeed, sprint ? 0.10f : 0.16f);
            if (ballPos.y <= 0.55f) {
                ballPos.y = 0.42f;
                ballVel.y *= 0.30f;
            }

            // Sprint gets separated touches instead of permanently glued control.
            if (sprint && inputAmount > 0.72f && ballDistance < 1.78f && sprintTouchCooldown <= 0f) {
                ballVel.x += inputDir.x * 2.35f;
                ballVel.z += inputDir.z * 2.35f;
                sprintTouchCooldown = 0.31f;
            }
            return;
        }

        // Standing close control: stop the old low-speed jitter when the player
        // is almost stationary with the ball already under control.
        if (actionCooldown <= 0.04f
            && inputAmount <= 0.12f
            && ballPos.y < 0.65f
            && ballDistance < 1.30f
            && ballSpeed < 4.8f) {

            targetBall.set(playerPos).mulAdd(facing, 0.88f);
            float settle = MathUtils.clamp(dt * 2.3f, 0f, 0.10f);
            ballPos.x = MathUtils.lerp(ballPos.x, targetBall.x, settle);
            ballPos.z = MathUtils.lerp(ballPos.z, targetBall.z, settle);
            ballVel.x *= 0.91f;
            ballVel.z *= 0.91f;
        }
    }
}
