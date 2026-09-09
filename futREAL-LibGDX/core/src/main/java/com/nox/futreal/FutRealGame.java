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

public class FutRealGame extends ApplicationAdapter {
    private static final float FIELD_W = 68f;
    private static final float FIELD_L = 105f;
    private static final float HALF_W = FIELD_W / 2f;
    private static final float HALF_L = FIELD_L / 2f;

    private ModelBatch modelBatch;
    private ModelBuilder builder;
    private Environment environment;
    private PerspectiveCamera camera;
    private final Array<Model> ownedModels = new Array<>();
    private final Array<ModelInstance> world = new Array<>();
    private final Array<Player> blue = new Array<>();
    private final Array<Player> red = new Array<>();

    private Model ballModel;
    private ModelInstance ball;
    private final Vector3 ballPos = new Vector3();
    private final Vector3 ballVel = new Vector3();
    private Player controlled;

    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapes;
    private final Matrix4 uiMatrix = new Matrix4();
    private int screenW, screenH;

    private final Vector2 move = new Vector2();
    private boolean sprint;
    private float kickCooldown;
    private float matchSeconds;
    private int blueScore;
    private int redScore;

    @Override
    public void create() {
        modelBatch = new ModelBatch();
        builder = new ModelBuilder();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.4f);
        shapes = new ShapeRenderer();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.55f, 0.58f, 0.62f, 1f));
        environment.add(new DirectionalLight().set(1f, 0.96f, 0.88f, -0.35f, -1f, -0.2f));
        environment.add(new DirectionalLight().set(0.25f, 0.33f, 0.45f, 0.5f, -0.5f, 0.8f));

        camera = new PerspectiveCamera(56f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 240f;

        buildStadium();
        buildTeams();
        buildBall();
        resetKickoff();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Material mat(Color c) {
        return new Material(ColorAttribute.createDiffuse(c));
    }

    private Model keep(Model model) {
        ownedModels.add(model);
        return model;
    }

    private void addBox(float x, float y, float z, float w, float h, float d, Color color) {
        Model m = keep(builder.createBox(w, h, d, mat(color), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal));
        ModelInstance i = new ModelInstance(m);
        i.transform.setToTranslation(x, y, z);
        world.add(i);
    }

    private void buildStadium() {
        addBox(0, -0.25f, 0, FIELD_W, 0.5f, FIELD_L, new Color(0.035f, 0.33f, 0.08f, 1));
        for (int n = 0; n < 10; n++) {
            float z = -47.25f + n * 10.5f;
            Color grass = (n % 2 == 0) ? new Color(0.04f, 0.40f, 0.10f, 1) : new Color(0.025f, 0.31f, 0.07f, 1);
            addBox(0, 0.01f, z, FIELD_W, 0.025f, 10.5f, grass);
        }

        Color line = new Color(0.92f, 0.94f, 0.92f, 1);
        addBox(0, 0.04f, 0, FIELD_W, 0.035f, 0.13f, line);
        addBox(-HALF_W, 0.04f, 0, 0.13f, 0.035f, FIELD_L, line);
        addBox(HALF_W, 0.04f, 0, 0.13f, 0.035f, FIELD_L, line);
        addBox(0, 0.04f, -HALF_L, FIELD_W, 0.035f, 0.13f, line);
        addBox(0, 0.04f, HALF_L, FIELD_W, 0.035f, 0.13f, line);

        Color stand = new Color(0.045f, 0.055f, 0.075f, 1);
        addBox(-43, 5, 0, 15, 10, 124, stand);
        addBox(43, 5, 0, 15, 10, 124, stand);
        addBox(0, 5, -62, 76, 10, 17, stand);
        addBox(0, 5, 62, 76, 10, 17, stand);

        Color seatA = new Color(0.08f, 0.37f, 0.72f, 1);
        Color seatB = new Color(0.70f, 0.08f, 0.15f, 1);
        for (int s : new int[]{-1, 1}) {
            for (int z = -48, idx = 0; z <= 48; z += 8, idx++) {
                addBox(37.5f * s, 5.8f, z, 1.5f, 5f, 6.3f, ((idx & 1) == 0) ? seatA : seatB);
            }
        }

        Color white = Color.WHITE;
        for (float z : new float[]{-53.1f, 53.1f}) {
            addBox(-7.3f, 1.25f, z, 0.18f, 2.5f, 0.18f, white);
            addBox(7.3f, 1.25f, z, 0.18f, 2.5f, 0.18f, white);
            addBox(0, 2.5f, z, 14.8f, 0.18f, 0.18f, white);
        }
    }

    private void buildTeams() {
        Model blueBody = keep(builder.createCapsule(0.48f, 2.15f, 12, mat(new Color(0.12f, 0.62f, 1f, 1)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal));
        Model redBody = keep(builder.createCapsule(0.48f, 2.15f, 12, mat(new Color(0.95f, 0.12f, 0.16f, 1)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal));

        float[][] blueFormation = {
            {0, 43}, {-22, 30}, {-8, 32}, {8, 32}, {22, 30},
            {-20, 13}, {0, 18}, {20, 13}, {-16, -5}, {0, -10}, {16, -5}
        };
        float[][] redFormation = {
            {0, -43}, {-22, -30}, {-8, -32}, {8, -32}, {22, -30},
            {-20, -13}, {0, -18}, {20, -13}, {-16, 5}, {0, 10}, {16, 5}
        };

        for (int i = 0; i < 11; i++) {
            Player p = new Player(new ModelInstance(blueBody), true, blueFormation[i][0], blueFormation[i][1]);
            blue.add(p);
            world.add(p.model);
        }
        for (int i = 0; i < 11; i++) {
            Player p = new Player(new ModelInstance(redBody), false, redFormation[i][0], redFormation[i][1]);
            red.add(p);
            world.add(p.model);
        }
        controlled = blue.get(10);
    }

    private void buildBall() {
        ballModel = keep(builder.createSphere(0.72f, 0.72f, 0.72f, 18, 12, mat(Color.WHITE), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal));
        ball = new ModelInstance(ballModel);
        world.add(ball);
    }

    private void resetKickoff() {
        for (Player p : blue) p.reset();
        for (Player p : red) p.reset();
        ballPos.set(0, 0.48f, 0);
        ballVel.setZero();
        syncTransforms();
    }

    private void syncTransforms() {
        for (Player p : blue) p.model.transform.setToTranslation(p.pos.x, 1.05f, p.pos.z);
        for (Player p : red) p.model.transform.setToTranslation(p.pos.x, 1.05f, p.pos.z);
        ball.transform.setToTranslation(ballPos);
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);
        updateControls();
        updateGame(dt);
        updateCamera(dt);

        Gdx.gl.glViewport(0, 0, screenW, screenH);
        Gdx.gl.glClearColor(0.015f, 0.025f, 0.04f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        for (ModelInstance i : world) modelBatch.render(i, environment);
        modelBatch.end();

        drawHud();
    }

    private void updateControls() {
        move.setZero();
        sprint = false;
        float joyX = Math.max(115f, screenW * 0.11f);
        float joyY = Math.max(115f, screenH * 0.19f);
        float joyRadius = Math.min(92f, screenH * 0.16f);

        boolean shoot = false;
        boolean pass = false;
        for (int p = 0; p < 5; p++) {
            if (!Gdx.input.isTouched(p)) continue;
            float x = Gdx.input.getX(p);
            float y = screenH - Gdx.input.getY(p);
            if (x < screenW * 0.42f) {
                float dx = x - joyX;
                float dy = y - joyY;
                float len = (float)Math.sqrt(dx * dx + dy * dy);
                if (len > 8f) {
                    float scale = Math.min(1f, len / joyRadius) / len;
                    move.set(dx * scale, dy * scale);
                }
            } else if (x > screenW * 0.79f && y < screenH * 0.42f) {
                shoot = true;
            } else if (x > screenW * 0.63f && x < screenW * 0.82f && y < screenH * 0.28f) {
                pass = true;
            } else if (x > screenW * 0.67f && x < screenW * 0.83f && y > screenH * 0.30f && y < screenH * 0.52f) {
                sprint = true;
            }
        }
        if (shoot) tryKick(27f, 4.2f);
        else if (pass) tryKick(15f, 1.2f);
    }

    private void tryKick(float power, float lift) {
        if (kickCooldown > 0f || controlled.pos.dst(ballPos) > 2.7f) return;
        Vector3 dir = controlled.facing.cpy();
        if (dir.len2() < 0.1f) dir.set(0, 0, -1);
        dir.nor();
        ballVel.set(dir.x * power, lift, dir.z * power);
        kickCooldown = 0.28f;
    }

    private void updateGame(float dt) {
        matchSeconds += dt;
        kickCooldown = Math.max(0f, kickCooldown - dt);

        float speed = sprint ? 12.2f : 8.3f;
        Vector3 desired = new Vector3(move.x, 0, -move.y);
        if (desired.len2() > 0.01f) {
            desired.nor();
            controlled.facing.lerp(desired, 0.28f).nor();
            controlled.pos.mulAdd(desired, speed * dt);
        }
        clampPlayer(controlled);

        updateAI(blue, true, dt);
        updateAI(red, false, dt);

        ballVel.y -= 9.8f * dt;
        ballPos.mulAdd(ballVel, dt);
        if (ballPos.y < 0.48f) {
            ballPos.y = 0.48f;
            if (ballVel.y < 0) ballVel.y *= -0.28f;
            ballVel.x *= MathUtils.pow(0.985f, dt * 60f);
            ballVel.z *= MathUtils.pow(0.985f, dt * 60f);
        }
        if (Math.abs(ballPos.x) > HALF_W) {
            ballPos.x = MathUtils.clamp(ballPos.x, -HALF_W, HALF_W);
            ballVel.x *= -0.55f;
        }

        if (ballPos.z < -HALF_L - 0.6f && Math.abs(ballPos.x) < 7.3f) {
            blueScore++;
            resetKickoff();
        } else if (ballPos.z > HALF_L + 0.6f && Math.abs(ballPos.x) < 7.3f) {
            redScore++;
            resetKickoff();
        } else if (Math.abs(ballPos.z) > HALF_L + 2f) {
            ballPos.z = MathUtils.clamp(ballPos.z, -HALF_L, HALF_L);
            ballVel.z *= -0.5f;
        }

        syncTransforms();
    }

    private void updateAI(Array<Player> team, boolean isBlue, float dt) {
        for (Player p : team) {
            if (p == controlled) continue;
            float distBall = p.pos.dst(ballPos);
            Vector3 target = p.home.cpy();
            if (distBall < 17f || (isBlue && ballPos.z > 0) || (!isBlue && ballPos.z < 0)) {
                target.lerp(new Vector3(ballPos.x, 0, ballPos.z), 0.42f);
            }
            Vector3 dir = target.sub(p.pos);
            if (dir.len2() > 0.3f) {
                dir.nor();
                p.facing.lerp(dir, 0.18f).nor();
                p.pos.mulAdd(dir, (distBall < 8f ? 7.1f : 5.6f) * dt);
                clampPlayer(p);
            }
            if (distBall < 1.65f && kickCooldown <= 0f) {
                float goalZ = isBlue ? -HALF_L : HALF_L;
                Vector3 kick = new Vector3(-ballPos.x * 0.04f, 0, goalZ - ballPos.z).nor();
                ballVel.set(kick.x * 18f, 1.4f, kick.z * 18f);
                kickCooldown = 0.18f;
            }
        }
    }

    private void clampPlayer(Player p) {
        p.pos.x = MathUtils.clamp(p.pos.x, -HALF_W + 1f, HALF_W - 1f);
        p.pos.z = MathUtils.clamp(p.pos.z, -HALF_L + 1f, HALF_L - 1f);
    }

    private void updateCamera(float dt) {
        Vector3 focus = controlled.pos.cpy().lerp(new Vector3(ballPos.x, 0, ballPos.z), 0.20f);
        Vector3 desired = new Vector3(focus.x * 0.35f, 18.5f, focus.z + 25f);
        camera.position.lerp(desired, 1f - MathUtils.pow(0.02f, dt));
        camera.up.set(Vector3.Y);
        camera.lookAt(focus.x, 0.8f, focus.z - 7f);
        camera.update();
    }

    private void drawHud() {
        shapes.setProjectionMatrix(uiMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.04f, 0.06f, 0.86f);
        shapes.rect(screenW * 0.35f, screenH - 72f, screenW * 0.30f, 58f);
        shapes.setColor(0.05f, 0.06f, 0.075f, 0.78f);
        float joyX = Math.max(115f, screenW * 0.11f);
        float joyY = Math.max(115f, screenH * 0.19f);
        shapes.circle(joyX, joyY, Math.min(92f, screenH * 0.16f), 32);
        shapes.setColor(0.72f, 1f, 0.20f, 0.9f);
        shapes.circle(joyX + move.x * 62f, joyY + move.y * 62f, 34f, 24);

        shapes.setColor(0.04f, 0.05f, 0.065f, 0.88f);
        shapes.circle(screenW * 0.90f, screenH * 0.18f, 58f, 32);
        shapes.rect(screenW * 0.68f, screenH * 0.08f, 120f, 64f);
        shapes.rect(screenW * 0.70f, screenH * 0.33f, 112f, 58f);
        shapes.end();

        batch.setProjectionMatrix(uiMatrix);
        batch.begin();
        font.setColor(Color.WHITE);
        String score = "AUR  " + blueScore + "  -  " + redScore + "  VIL";
        font.draw(batch, score, screenW * 0.41f, screenH - 35f);
        int minute = Math.min(90, (int)(matchSeconds / 6f));
        font.setColor(new Color(0.72f, 1f, 0.20f, 1));
        font.draw(batch, String.format("%02d'", minute), screenW * 0.36f, screenH - 35f);
        font.setColor(Color.WHITE);
        font.draw(batch, "CHUTE", screenW * 0.865f, screenH * 0.19f);
        font.draw(batch, "PASSE", screenW * 0.695f, screenH * 0.14f);
        font.draw(batch, "SPRINT", screenW * 0.71f, screenH * 0.39f);
        font.setColor(new Color(0.72f, 1f, 0.20f, 1));
        font.draw(batch, "futREAL • LIBGDX 3D", 20f, screenH - 24f);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        screenW = Math.max(1, width);
        screenH = Math.max(1, height);
        uiMatrix.setToOrtho2D(0, 0, screenW, screenH);
        camera.viewportWidth = screenW;
        camera.viewportHeight = screenH;
        camera.update();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        batch.dispose();
        font.dispose();
        shapes.dispose();
        for (Model m : ownedModels) m.dispose();
    }

    private static class Player {
        final ModelInstance model;
        final boolean blue;
        final Vector3 home = new Vector3();
        final Vector3 pos = new Vector3();
        final Vector3 facing = new Vector3(0, 0, -1);

        Player(ModelInstance model, boolean blue, float x, float z) {
            this.model = model;
            this.blue = blue;
            home.set(x, 0, z);
            reset();
            if (!blue) facing.set(0, 0, 1);
        }

        void reset() {
            pos.set(home);
        }
    }
}
