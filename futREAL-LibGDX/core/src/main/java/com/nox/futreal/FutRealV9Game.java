package com.nox.futreal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.lang.reflect.Field;

/**
 * futREAL 9 PRO
 * Original high-detail mobile football layer built on the stable v8/v7 core.
 * No EA/FC assets, club crests or player likenesses are used.
 */
public class FutRealV9Game extends FutRealV8Game {
    private ModelBuilder proBuilder;
    private ShapeRenderer proShapes;
    private SpriteBatch proBatch;
    private BitmapFont proFont;
    private final Matrix4 proUi = new Matrix4();
    private int sw = 1, sh = 1;
    private float throughCooldown;
    private float skillCooldown;

    private Field screenField;
    private Field stadiumField;
    private Field ownedModelsField;
    private Field environmentField;
    private Field cameraField;
    private Field blueTeamField;
    private Field redTeamField;
    private Field controlledField;
    private Field ballPosField;
    private Field ballVelField;

    private Field playerPosField;
    private Field playerFacingField;
    private Field playerKeeperField;
    private Field playerBlueField;
    private Field playerIndexField;
    private Field torsoField;
    private Field shortsField;
    private Field headField;
    private Field armLField;
    private Field armRField;
    private Field legLField;
    private Field legRField;
    private Field bootLField;
    private Field bootRField;

    private Array<?> blueTeam;
    private Array<?> redTeam;

    @Override
    public void create() {
        super.create();
        proBuilder = new ModelBuilder();
        proShapes = new ShapeRenderer();
        proBatch = new SpriteBatch();
        proFont = new BitmapFont();
        proFont.getData().setScale(0.78f);

        try {
            Class<?> base = FutRealV7Game.class;
            screenField = field(base, "screen");
            stadiumField = field(base, "stadium");
            ownedModelsField = field(base, "ownedModels");
            environmentField = field(base, "environment");
            cameraField = field(base, "camera");
            blueTeamField = field(base, "blue");
            redTeamField = field(base, "red");
            controlledField = field(base, "controlled");
            ballPosField = field(base, "ballPos");
            ballVelField = field(base, "ballVel");

            @SuppressWarnings("unchecked")
            Array<ModelInstance> stadium = (Array<ModelInstance>) stadiumField.get(this);
            @SuppressWarnings("unchecked")
            Array<Model> owned = (Array<Model>) ownedModelsField.get(this);
            Environment env = (Environment) environmentField.get(this);
            PerspectiveCamera cam = (PerspectiveCamera) cameraField.get(this);
            blueTeam = (Array<?>) blueTeamField.get(this);
            redTeam = (Array<?>) redTeamField.get(this);

            if (blueTeam != null && blueTeam.size > 0) {
                Class<?> pc = blueTeam.get(0).getClass();
                playerPosField = field(pc, "pos");
                playerFacingField = field(pc, "facing");
                playerKeeperField = field(pc, "keeper");
                playerBlueField = field(pc, "blue");
                playerIndexField = field(pc, "index");
                torsoField = field(pc, "torso");
                shortsField = field(pc, "shorts");
                headField = field(pc, "head");
                armLField = field(pc, "armL");
                armRField = field(pc, "armR");
                legLField = field(pc, "legL");
                legRField = field(pc, "legR");
                bootLField = field(pc, "bootL");
                bootRField = field(pc, "bootR");
            }

            buildProPitchAndArena(stadium, owned);
            upgradePlayerGeometry(blueTeam, owned);
            upgradePlayerGeometry(redTeam, owned);
            tuneLighting(env);

            cam.fieldOfView = 39f;
            cam.far = 520f;
            cam.near = 0.08f;
            cam.update();
        } catch (Throwable t) {
            Gdx.app.error("futREAL-V9", "PRO layer init failed", t);
        }
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Field field(Class<?> c, String name) throws NoSuchFieldException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private Material mat(Color c, float spec) {
        return new Material(
            ColorAttribute.createDiffuse(c),
            ColorAttribute.createSpecular(new Color(spec, spec, spec, 1f))
        );
    }

    private Model own(Array<Model> owned, Model model) {
        owned.add(model);
        return model;
    }

    private void add(Array<ModelInstance> list, Model model, float x, float y, float z) {
        ModelInstance i = new ModelInstance(model);
        i.transform.setToTranslation(x, y, z);
        list.add(i);
    }

    private void tuneLighting(Environment env) {
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.38f, 0.42f, 0.50f, 1f));
        env.add(new DirectionalLight().set(0.96f, 0.94f, 0.86f, -0.38f, -0.95f, -0.24f));
        env.add(new DirectionalLight().set(0.34f, 0.46f, 0.68f, 0.72f, -0.55f, 0.32f));
        env.add(new DirectionalLight().set(0.22f, 0.30f, 0.48f, -0.66f, -0.44f, 0.70f));
    }

    private void buildProPitchAndArena(Array<ModelInstance> stadium, Array<Model> owned) {
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        // Fine mowing pattern under the existing white field markings.
        Color g1 = new Color(0.018f, 0.315f, 0.075f, 1f);
        Color g2 = new Color(0.022f, 0.375f, 0.090f, 1f);
        Model stripeA = own(owned, proBuilder.createBox(68f, 0.022f, 3.75f, mat(g1, 0.03f), attrs));
        Model stripeB = own(owned, proBuilder.createBox(68f, 0.022f, 3.75f, mat(g2, 0.03f), attrs));
        for (int i = 0; i < 28; i++) {
            float z = -50.625f + i * 3.75f;
            add(stadium, (i & 1) == 0 ? stripeA : stripeB, 0f, -0.004f, z);
        }

        // Corner flags.
        Model pole = own(owned, proBuilder.createCylinder(0.055f, 1.65f, 0.055f, 10,
            mat(new Color(0.92f, 0.92f, 0.90f, 1f), 0.35f), attrs));
        Model flag = own(owned, proBuilder.createBox(0.72f, 0.42f, 0.035f,
            mat(new Color(0.62f, 1f, 0.10f, 1f), 0.55f), attrs));
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                add(stadium, pole, sx * 34.45f, 0.825f, sz * 52.95f);
                add(stadium, flag, sx * 34.10f, 1.43f, sz * 52.95f);
            }
        }

        // Bigger stadium bowl, upper deck and VIP ring.
        Model upperSide = own(owned, proBuilder.createBox(13f, 5.8f, 126f,
            mat(new Color(0.022f, 0.028f, 0.045f, 1f), 0.22f), attrs));
        Model upperEnd = own(owned, proBuilder.createBox(82f, 5.8f, 15f,
            mat(new Color(0.022f, 0.028f, 0.045f, 1f), 0.22f), attrs));
        for (int s : new int[]{-1, 1}) add(stadium, upperSide, s * 50.5f, 10.7f, 0f);
        for (int e : new int[]{-1, 1}) add(stadium, upperEnd, 0f, 10.7f, e * 70f);

        Model vipSide = own(owned, proBuilder.createBox(3.2f, 2.0f, 96f,
            mat(new Color(0.055f, 0.085f, 0.12f, 1f), 0.80f), attrs));
        for (int s : new int[]{-1, 1}) add(stadium, vipSide, s * 43.0f, 9.4f, 0f);

        // Dense crowd dots in the upper tier, shared models for mobile performance.
        Model fanBlue = own(owned, proBuilder.createSphere(0.42f, 0.55f, 0.42f, 8, 6,
            mat(new Color(0.04f, 0.36f, 0.86f, 1f), 0.06f), attrs));
        Model fanLime = own(owned, proBuilder.createSphere(0.42f, 0.55f, 0.42f, 8, 6,
            mat(new Color(0.58f, 0.92f, 0.12f, 1f), 0.06f), attrs));
        Model fanWhite = own(owned, proBuilder.createSphere(0.42f, 0.55f, 0.42f, 8, 6,
            mat(new Color(0.78f, 0.82f, 0.86f, 1f), 0.06f), attrs));
        Model fanDark = own(owned, proBuilder.createSphere(0.42f, 0.55f, 0.42f, 8, 6,
            mat(new Color(0.16f, 0.18f, 0.23f, 1f), 0.06f), attrs));
        Model[] fans = new Model[]{fanBlue, fanLime, fanWhite, fanDark};
        int k = 0;
        for (int side : new int[]{-1, 1}) {
            for (int row = 0; row < 4; row++) {
                for (int z = -56; z <= 56; z += 3) {
                    add(stadium, fans[(k++) & 3], side * (45.5f + row * 1.8f), 8.3f + row * 1.2f,
                        z + ((row & 1) == 0 ? 0f : 1.4f));
                }
            }
        }
        for (int end : new int[]{-1, 1}) {
            for (int row = 0; row < 3; row++) {
                for (int x = -33; x <= 33; x += 3) {
                    add(stadium, fans[(k++) & 3], x, 8.1f + row * 1.25f,
                        end * (63.0f + row * 1.7f));
                }
            }
        }

        // Stadium roof trusses and light bars.
        Model truss = own(owned, proBuilder.createBox(0.32f, 0.32f, 126f,
            mat(new Color(0.13f, 0.15f, 0.19f, 1f), 0.55f), attrs));
        Model lightBar = own(owned, proBuilder.createBox(0.55f, 0.55f, 102f,
            mat(new Color(0.96f, 0.98f, 0.92f, 1f), 1.0f), attrs));
        for (int s : new int[]{-1, 1}) {
            add(stadium, truss, s * 49.0f, 17.0f, 0f);
            add(stadium, lightBar, s * 45.5f, 16.0f, 0f);
        }

        Model endTruss = own(owned, proBuilder.createBox(80f, 0.32f, 0.32f,
            mat(new Color(0.13f, 0.15f, 0.19f, 1f), 0.55f), attrs));
        Model endLight = own(owned, proBuilder.createBox(68f, 0.55f, 0.55f,
            mat(new Color(0.96f, 0.98f, 0.92f, 1f), 1.0f), attrs));
        for (int e : new int[]{-1, 1}) {
            add(stadium, endTruss, 0f, 16.5f, e * 67.0f);
            add(stadium, endLight, 0f, 15.4f, e * 63.8f);
        }

        // Giant original futREAL video board.
        Model boardFrame = own(owned, proBuilder.createBox(17f, 7.5f, 0.65f,
            mat(new Color(0.018f, 0.022f, 0.030f, 1f), 0.70f), attrs));
        Model boardScreen = own(owned, proBuilder.createBox(15.6f, 6.1f, 0.18f,
            mat(new Color(0.035f, 0.28f, 0.43f, 1f), 0.92f), attrs));
        add(stadium, boardFrame, 0f, 13.3f, -71.4f);
        add(stadium, boardScreen, 0f, 13.3f, -71.0f);

        // Broadcast camera platforms and technical zones.
        Model platform = own(owned, proBuilder.createBox(4.8f, 0.35f, 3.2f,
            mat(new Color(0.10f, 0.12f, 0.15f, 1f), 0.55f), attrs));
        Model cameraBody = own(owned, proBuilder.createBox(0.65f, 0.55f, 1.05f,
            mat(new Color(0.025f, 0.028f, 0.034f, 1f), 0.70f), attrs));
        for (int z : new int[]{-34, 0, 34}) {
            add(stadium, platform, -39.2f, 1.0f, z);
            add(stadium, cameraBody, -38.6f, 1.85f, z);
        }

        // Sideline cones/markers add scale and detail.
        Model marker = own(owned, proBuilder.createCylinder(0.18f, 0.28f, 0.18f, 10,
            mat(new Color(1f, 0.42f, 0.04f, 1f), 0.30f), attrs));
        for (int z = -40; z <= 40; z += 10) add(stadium, marker, 36.4f, 0.14f, z);
    }

    private void upgradePlayerGeometry(Array<?> team, Array<Model> owned) throws IllegalAccessException {
        if (team == null || team.size == 0) return;
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        for (Object p : team) {
            boolean blue = playerBlueField.getBoolean(p);
            boolean keeper = playerKeeperField.getBoolean(p);
            int idx = playerIndexField.getInt(p);

            Color shirt;
            Color shorts;
            if (keeper) {
                shirt = blue ? new Color(0.97f, 0.70f, 0.04f, 1f) : new Color(0.14f, 0.92f, 0.36f, 1f);
                shorts = shirt.cpy().mul(0.62f, 0.62f, 0.62f, 1f);
            } else if (blue) {
                shirt = new Color(0.025f, 0.46f + (idx % 3) * 0.025f, 0.98f, 1f);
                shorts = new Color(0.018f, 0.08f, 0.18f, 1f);
            } else {
                shirt = new Color(0.92f, 0.045f, 0.10f + (idx % 3) * 0.018f, 1f);
                shorts = new Color(0.18f, 0.015f, 0.03f, 1f);
            }

            Color skin;
            switch (idx & 3) {
                case 0: skin = new Color(0.86f, 0.66f, 0.49f, 1f); break;
                case 1: skin = new Color(0.70f, 0.48f, 0.32f, 1f); break;
                case 2: skin = new Color(0.50f, 0.31f, 0.20f, 1f); break;
                default: skin = new Color(0.31f, 0.19f, 0.13f, 1f); break;
            }

            Model torso = own(owned, proBuilder.createCapsule(0.48f, 1.24f, 24, mat(shirt, 0.76f), attrs));
            Model shortM = own(owned, proBuilder.createBox(0.78f, 0.40f, 0.46f, mat(shorts, 0.50f), attrs));
            Model head = own(owned, proBuilder.createSphere(0.55f, 0.62f, 0.55f, 22, 16, mat(skin, 0.38f), attrs));
            Model arm = own(owned, proBuilder.createCylinder(0.19f, 0.92f, 0.19f, 12, mat(skin, 0.32f), attrs));
            Model leg = own(owned, proBuilder.createCylinder(0.20f, 0.94f, 0.20f, 12,
                mat(new Color(0.86f, 0.87f, 0.90f, 1f), 0.28f), attrs));
            Model boot = own(owned, proBuilder.createBox(0.26f, 0.17f, 0.47f,
                mat(new Color(0.018f, 0.020f, 0.026f, 1f), 0.82f), attrs));

            torsoField.set(p, new ModelInstance(torso));
            shortsField.set(p, new ModelInstance(shortM));
            headField.set(p, new ModelInstance(head));
            armLField.set(p, new ModelInstance(arm));
            armRField.set(p, new ModelInstance(arm));
            legLField.set(p, new ModelInstance(leg));
            legRField.set(p, new ModelInstance(leg));
            bootLField.set(p, new ModelInstance(boot));
            bootRField.set(p, new ModelInstance(boot));
        }
    }

    @Override
    public void render() {
        throughCooldown = Math.max(0f, throughCooldown - Gdx.graphics.getDeltaTime());
        skillCooldown = Math.max(0f, skillCooldown - Gdx.graphics.getDeltaTime());
        super.render();
        if (isMatch()) {
            try {
                handleProActions();
                drawProControls();
            } catch (Throwable t) {
                Gdx.app.error("futREAL-V9", "PRO action layer error", t);
            }
        }
    }

    private boolean isMatch() {
        try {
            Object s = screenField == null ? null : screenField.get(this);
            return s != null && "MATCH".equals(s.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void handleProActions() throws IllegalAccessException {
        if (controlledField == null || ballPosField == null || ballVelField == null) return;
        Object controlled = controlledField.get(this);
        if (controlled == null) return;
        Vector3 p = (Vector3) playerPosField.get(controlled);
        Vector3 facing = (Vector3) playerFacingField.get(controlled);
        Vector3 ballPos = (Vector3) ballPosField.get(this);
        Vector3 ballVel = (Vector3) ballVelField.get(this);
        if (p == null || facing == null || ballPos == null || ballVel == null) return;

        for (int pointer = 0; pointer < 6; pointer++) {
            if (!Gdx.input.isTouched(pointer)) continue;
            float x = Gdx.input.getX(pointer);
            float y = sh - Gdx.input.getY(pointer);

            // Through pass button: left of the standard action cluster.
            if (x > sw * 0.51f && x < sw * 0.66f && y < sh * 0.24f && throughCooldown <= 0f) {
                if (p.dst(ballPos.x, 0f, ballPos.z) < 2.8f) {
                    Object best = findForwardTeammate(controlled, p, facing);
                    Vector3 dir;
                    if (best != null) {
                        Vector3 target = (Vector3) playerPosField.get(best);
                        dir = target.cpy().sub(ballPos.x, 0f, ballPos.z).nor();
                    } else {
                        dir = facing.cpy().nor();
                    }
                    ballVel.set(dir.x * 22.5f, 1.35f, dir.z * 22.5f);
                    throughCooldown = 0.52f;
                }
            }

            // Skill move button: quick body feint + close ball touch.
            if (x > sw * 0.51f && x < sw * 0.66f && y > sh * 0.30f && y < sh * 0.49f && skillCooldown <= 0f) {
                if (p.dst(ballPos.x, 0f, ballPos.z) < 3.1f) {
                    Vector3 side = new Vector3(-facing.z, 0f, facing.x).nor();
                    float sign = ((System.nanoTime() >> 18) & 1L) == 0L ? -1f : 1f;
                    p.mulAdd(side, 0.92f * sign);
                    ballPos.mulAdd(side, 0.72f * sign);
                    ballVel.scl(0.35f).mulAdd(facing.cpy().nor(), 4.2f);
                    skillCooldown = 0.68f;
                }
            }
        }
    }

    private Object findForwardTeammate(Object controlled, Vector3 from, Vector3 facing) throws IllegalAccessException {
        Array<?> team = blueTeam;
        if (team == null) return null;
        Object best = null;
        float bestScore = -9999f;
        for (Object p : team) {
            if (p == controlled || playerKeeperField.getBoolean(p)) continue;
            Vector3 tp = (Vector3) playerPosField.get(p);
            Vector3 to = tp.cpy().sub(from);
            float d = to.len();
            if (d < 3f || d > 38f) continue;
            float forward = to.nor().dot(facing);
            float score = forward * 15f - d * 0.12f - Math.abs(tp.x - from.x) * 0.04f;
            if (forward > 0.15f && score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    private void drawProControls() {
        proShapes.setProjectionMatrix(proUi);
        proBatch.setProjectionMatrix(proUi);

        float r = Math.min(sh * 0.067f, 54f);
        float x = sw * 0.585f;
        float yPass = sh * 0.15f;
        float ySkill = sh * 0.39f;

        proShapes.begin(ShapeRenderer.ShapeType.Filled);
        proShapes.setColor(0.06f, 0.50f, 0.95f, 0.82f);
        proShapes.circle(x, yPass, r, 28);
        proShapes.setColor(0.70f, 0.96f, 0.12f, 0.82f);
        proShapes.circle(x, ySkill, r, 28);

        // Cinematic top vignette bars make the broadcast HUD feel tighter.
        proShapes.setColor(0.006f, 0.010f, 0.016f, 0.26f);
        proShapes.rect(0f, sh * 0.985f, sw, sh * 0.015f);
        proShapes.end();

        proBatch.begin();
        proFont.setColor(Color.WHITE);
        proFont.draw(proBatch, "ENFIADA", x - r * 0.78f, yPass + 5f);
        proFont.setColor(new Color(0.03f, 0.07f, 0.03f, 1f));
        proFont.draw(proBatch, "DRIBLE", x - r * 0.63f, ySkill + 5f);
        proBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        sw = Math.max(1, width);
        sh = Math.max(1, height);
        proUi.setToOrtho2D(0, 0, sw, sh);
    }

    @Override
    public void dispose() {
        if (proShapes != null) proShapes.dispose();
        if (proBatch != null) proBatch.dispose();
        if (proFont != null) proFont.dispose();
        super.dispose();
    }
}
