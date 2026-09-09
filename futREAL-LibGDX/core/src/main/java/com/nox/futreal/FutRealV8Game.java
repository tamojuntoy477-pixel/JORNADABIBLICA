package com.nox.futreal;

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
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.lang.reflect.Field;

/**
 * futREAL 8 ELITE
 *
 * Visual/performance upgrade layered over the stable v7 gameplay core.
 * All art remains original/procedural so the project does not depend on
 * licensed club/player assets.
 */
public class FutRealV8Game extends FutRealV7Game {
    private static final float FIELD_W = 68f;
    private static final float FIELD_L = 105f;

    private ModelBuilder v8Builder;
    private ShapeRenderer overlayShapes;
    private SpriteBatch overlayBatch;
    private BitmapFont overlayFont;
    private BitmapFont overlaySmall;
    private final Matrix4 overlayMatrix = new Matrix4();
    private int sw = 1;
    private int sh = 1;

    private Field screenField;
    private Field posField;
    private Field indexField;
    private Field keeperField;
    private Field blueField;
    private Field torsoField;
    private Field shortsField;
    private Field headField;
    private Field armLField;
    private Field armRField;
    private Field legLField;
    private Field legRField;
    private Field bootLField;
    private Field bootRField;
    private Field shadowField;

    private Array<?> blueTeam;
    private Array<?> redTeam;

    private final Color[] skinTones = new Color[] {
        new Color(0.84f, 0.64f, 0.47f, 1f),
        new Color(0.69f, 0.46f, 0.31f, 1f),
        new Color(0.48f, 0.29f, 0.18f, 1f),
        new Color(0.30f, 0.18f, 0.12f, 1f)
    };

    @Override
    public void create() {
        super.create();

        v8Builder = new ModelBuilder();
        overlayShapes = new ShapeRenderer();
        overlayBatch = new SpriteBatch();
        overlayFont = new BitmapFont();
        overlayFont.getData().setScale(1.18f);
        overlaySmall = new BitmapFont();
        overlaySmall.getData().setScale(0.82f);

        try {
            Class<?> base = FutRealV7Game.class;
            screenField = field(base, "screen");
            Field stadiumField = field(base, "stadium");
            Field ownedModelsField = field(base, "ownedModels");
            Field environmentField = field(base, "environment");
            Field cameraField = field(base, "camera");
            Field blueTeamField = field(base, "blue");
            Field redTeamField = field(base, "red");

            @SuppressWarnings("unchecked")
            Array<ModelInstance> stadium = (Array<ModelInstance>) stadiumField.get(this);
            @SuppressWarnings("unchecked")
            Array<Model> ownedModels = (Array<Model>) ownedModelsField.get(this);
            Environment environment = (Environment) environmentField.get(this);
            PerspectiveCamera camera = (PerspectiveCamera) cameraField.get(this);
            blueTeam = (Array<?>) blueTeamField.get(this);
            redTeam = (Array<?>) redTeamField.get(this);

            if (blueTeam.size > 0) {
                Class<?> playerClass = blueTeam.get(0).getClass();
                posField = field(playerClass, "pos");
                indexField = field(playerClass, "index");
                keeperField = field(playerClass, "keeper");
                blueField = field(playerClass, "blue");
                torsoField = field(playerClass, "torso");
                shortsField = field(playerClass, "shorts");
                headField = field(playerClass, "head");
                armLField = field(playerClass, "armL");
                armRField = field(playerClass, "armR");
                legLField = field(playerClass, "legL");
                legRField = field(playerClass, "legR");
                bootLField = field(playerClass, "bootL");
                bootRField = field(playerClass, "bootR");
                shadowField = field(playerClass, "shadow");
            }

            upgradeLighting(environment);
            buildEliteStadium(stadium, ownedModels);
            upgradePlayers(blueTeam, ownedModels);
            upgradePlayers(redTeam, ownedModels);
            upgradeBallAndSelector(base, ownedModels);

            camera.fieldOfView = 43f;
            camera.far = 420f;
            camera.update();
        } catch (Throwable t) {
            Gdx.app.error("futREAL-V8", "Elite visual layer could not fully initialize", t);
        }

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Field field(Class<?> c, String name) throws NoSuchFieldException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private Material mat(Color diffuse, float specular) {
        return new Material(
            ColorAttribute.createDiffuse(diffuse),
            ColorAttribute.createSpecular(new Color(specular, specular, specular, 1f))
        );
    }

    private Model own(Array<Model> owned, Model model) {
        owned.add(model);
        return model;
    }

    private void upgradeLighting(Environment env) {
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.42f, 0.46f, 0.54f, 1f));
        env.add(new DirectionalLight().set(0.62f, 0.70f, 0.88f, 0.35f, -0.82f, 0.26f));
        env.add(new DirectionalLight().set(0.42f, 0.48f, 0.62f, -0.58f, -0.60f, -0.40f));
        env.add(new DirectionalLight().set(0.32f, 0.36f, 0.48f, 0.10f, -0.72f, 0.80f));
    }

    private void buildEliteStadium(Array<ModelInstance> stadium, Array<Model> owned) {
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        Model crowdBlue = own(owned, v8Builder.createBox(0.62f, 0.76f, 0.54f,
            mat(new Color(0.05f, 0.35f, 0.78f, 1f), 0.08f), attrs));
        Model crowdLime = own(owned, v8Builder.createBox(0.62f, 0.76f, 0.54f,
            mat(new Color(0.52f, 0.82f, 0.09f, 1f), 0.08f), attrs));
        Model crowdDark = own(owned, v8Builder.createBox(0.62f, 0.76f, 0.54f,
            mat(new Color(0.17f, 0.20f, 0.27f, 1f), 0.08f), attrs));
        Model crowdWhite = own(owned, v8Builder.createBox(0.62f, 0.76f, 0.54f,
            mat(new Color(0.76f, 0.80f, 0.82f, 1f), 0.08f), attrs));
        Model[] crowd = new Model[] { crowdBlue, crowdLime, crowdDark, crowdWhite };

        // Four dense-looking spectator strips built from shared low-poly models.
        int ci = 0;
        for (int side : new int[] {-1, 1}) {
            for (int row = 0; row < 4; row++) {
                for (int z = -54; z <= 54; z += 4) {
                    ModelInstance fan = new ModelInstance(crowd[(ci++ + row) % crowd.length]);
                    fan.transform.setToTranslation(side * (38.5f + row * 1.45f), 2.6f + row * 1.25f,
                        z + ((row & 1) == 0 ? 0f : 1.8f));
                    stadium.add(fan);
                }
            }
        }

        // End-stand crowd strips.
        for (int end : new int[] {-1, 1}) {
            for (int row = 0; row < 3; row++) {
                for (int x = -30; x <= 30; x += 4) {
                    ModelInstance fan = new ModelInstance(crowd[(ci++ + row) % crowd.length]);
                    fan.transform.setToTranslation(x, 2.7f + row * 1.2f,
                        end * (58.2f + row * 1.35f));
                    stadium.add(fan);
                }
            }
        }

        Color steel = new Color(0.075f, 0.085f, 0.11f, 1f);
        Color roof = new Color(0.035f, 0.045f, 0.065f, 1f);
        Model sideRoof = own(owned, v8Builder.createBox(12f, 0.65f, 124f, mat(roof, 0.42f), attrs));
        Model endRoof = own(owned, v8Builder.createBox(78f, 0.65f, 12f, mat(roof, 0.42f), attrs));
        for (int side : new int[] {-1, 1}) {
            ModelInstance r = new ModelInstance(sideRoof);
            r.transform.setToTranslation(side * 43.8f, 13.0f, 0f);
            stadium.add(r);
        }
        for (int end : new int[] {-1, 1}) {
            ModelInstance r = new ModelInstance(endRoof);
            r.transform.setToTranslation(0f, 12.5f, end * 63f);
            stadium.add(r);
        }

        Model beam = own(owned, v8Builder.createBox(0.40f, 13f, 0.40f, mat(steel, 0.55f), attrs));
        for (int side : new int[] {-1, 1}) {
            for (int z = -54; z <= 54; z += 18) {
                ModelInstance b = new ModelInstance(beam);
                b.transform.setToTranslation(side * 43f, 6.5f, z);
                stadium.add(b);
            }
        }

        // LED ribbon around the pitch.
        Model ledBlue = own(owned, v8Builder.createBox(0.18f, 0.72f, 9.5f,
            mat(new Color(0.02f, 0.52f, 0.98f, 1f), 0.85f), attrs));
        Model ledLime = own(owned, v8Builder.createBox(0.18f, 0.72f, 9.5f,
            mat(new Color(0.62f, 1.0f, 0.10f, 1f), 0.85f), attrs));
        for (int side : new int[] {-1, 1}) {
            for (int z = -48, k = 0; z <= 48; z += 10, k++) {
                ModelInstance led = new ModelInstance((k & 1) == 0 ? ledBlue : ledLime);
                led.transform.setToTranslation(side * 35.5f, 0.52f, z);
                stadium.add(led);
            }
        }

        // Proper-looking goal nets using thin shared strands.
        Model netVertical = own(owned, v8Builder.createBox(0.035f, 2.35f, 0.035f,
            mat(new Color(0.74f, 0.78f, 0.80f, 1f), 0.25f), attrs));
        Model netHorizontal = own(owned, v8Builder.createBox(7.25f, 0.035f, 0.035f,
            mat(new Color(0.74f, 0.78f, 0.80f, 1f), 0.25f), attrs));
        Model netDepth = own(owned, v8Builder.createBox(0.035f, 0.035f, 2.9f,
            mat(new Color(0.74f, 0.78f, 0.80f, 1f), 0.25f), attrs));
        for (int end : new int[] {-1, 1}) {
            float goalLine = end * 53.05f;
            float back = end * 55.8f;
            for (float x = -3.55f; x <= 3.56f; x += 0.72f) {
                ModelInstance v = new ModelInstance(netVertical);
                v.transform.setToTranslation(x, 1.18f, back);
                stadium.add(v);
            }
            for (float y = 0.24f; y <= 2.35f; y += 0.42f) {
                ModelInstance h = new ModelInstance(netHorizontal);
                h.transform.setToTranslation(0f, y, back);
                stadium.add(h);
            }
            for (float x = -3.55f; x <= 3.56f; x += 0.72f) {
                ModelInstance d = new ModelInstance(netDepth);
                d.transform.setToTranslation(x, 2.36f, (goalLine + back) * 0.5f);
                stadium.add(d);
            }
        }

        // Player tunnel / technical-area silhouettes.
        Model tunnel = own(owned, v8Builder.createBox(5.5f, 3.2f, 4.8f,
            mat(new Color(0.025f, 0.03f, 0.045f, 1f), 0.25f), attrs));
        ModelInstance tunnelI = new ModelInstance(tunnel);
        tunnelI.transform.setToTranslation(-39.8f, 1.6f, 4f);
        stadium.add(tunnelI);

        Model bench = own(owned, v8Builder.createBox(6.8f, 1.5f, 1.8f,
            mat(new Color(0.07f, 0.16f, 0.20f, 1f), 0.65f), attrs));
        for (int z : new int[] {-15, 15}) {
            ModelInstance bi = new ModelInstance(bench);
            bi.transform.setToTranslation(-36.8f, 0.75f, z);
            stadium.add(bi);
        }
    }

    private void upgradePlayers(Array<?> team, Array<Model> owned) throws IllegalAccessException {
        if (team == null || team.size == 0) return;
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        Model blueTorso = own(owned, v8Builder.createCapsule(0.50f, 1.22f, 18,
            mat(new Color(0.035f, 0.50f, 0.98f, 1f), 0.62f), attrs));
        Model redTorso = own(owned, v8Builder.createCapsule(0.50f, 1.22f, 18,
            mat(new Color(0.90f, 0.055f, 0.12f, 1f), 0.62f), attrs));
        Model keeperBlue = own(owned, v8Builder.createCapsule(0.50f, 1.22f, 18,
            mat(new Color(0.96f, 0.72f, 0.06f, 1f), 0.68f), attrs));
        Model keeperRed = own(owned, v8Builder.createCapsule(0.50f, 1.22f, 18,
            mat(new Color(0.12f, 0.80f, 0.42f, 1f), 0.68f), attrs));

        Model blueShort = own(owned, v8Builder.createCylinder(0.82f, 0.38f, 0.64f, 14,
            mat(new Color(0.015f, 0.08f, 0.20f, 1f), 0.35f), attrs));
        Model redShort = own(owned, v8Builder.createCylinder(0.82f, 0.38f, 0.64f, 14,
            mat(new Color(0.19f, 0.015f, 0.035f, 1f), 0.35f), attrs));
        Model blueLeg = own(owned, v8Builder.createCapsule(0.135f, 0.94f, 12,
            mat(new Color(0.08f, 0.45f, 0.92f, 1f), 0.28f), attrs));
        Model redLeg = own(owned, v8Builder.createCapsule(0.135f, 0.94f, 12,
            mat(new Color(0.94f, 0.94f, 0.95f, 1f), 0.28f), attrs));
        Model boot = own(owned, v8Builder.createBox(0.26f, 0.15f, 0.47f,
            mat(new Color(0.018f, 0.022f, 0.028f, 1f), 0.78f), attrs));
        Model shadow = own(owned, v8Builder.createCylinder(0.92f, 0.018f, 0.62f, 20,
            mat(new Color(0.012f, 0.015f, 0.018f, 1f), 0.02f), attrs));

        Model[] heads = new Model[skinTones.length];
        Model[] arms = new Model[skinTones.length];
        for (int s = 0; s < skinTones.length; s++) {
            heads[s] = own(owned, v8Builder.createSphere(0.56f, 0.64f, 0.56f, 20, 14,
                mat(skinTones[s], 0.20f), attrs));
            arms[s] = own(owned, v8Builder.createCapsule(0.13f, 0.92f, 12,
                mat(skinTones[s], 0.18f), attrs));
        }

        for (int i = 0; i < team.size; i++) {
            Object p = team.get(i);
            boolean blue = blueField.getBoolean(p);
            boolean keeper = keeperField.getBoolean(p);
            int idx = indexField.getInt(p);
            int skin = Math.abs(idx * 3 + (blue ? 1 : 2)) % skinTones.length;

            Model torso = keeper ? (blue ? keeperBlue : keeperRed) : (blue ? blueTorso : redTorso);
            torsoField.set(p, new ModelInstance(torso));
            shortsField.set(p, new ModelInstance(blue ? blueShort : redShort));
            headField.set(p, new ModelInstance(heads[skin]));
            armLField.set(p, new ModelInstance(arms[skin]));
            armRField.set(p, new ModelInstance(arms[skin]));
            legLField.set(p, new ModelInstance(blue ? blueLeg : redLeg));
            legRField.set(p, new ModelInstance(blue ? blueLeg : redLeg));
            bootLField.set(p, new ModelInstance(boot));
            bootRField.set(p, new ModelInstance(boot));
            shadowField.set(p, new ModelInstance(shadow));
        }
    }

    private void upgradeBallAndSelector(Class<?> base, Array<Model> owned) throws Exception {
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Model newBallModel = own(owned, v8Builder.createSphere(0.70f, 0.70f, 0.70f, 28, 20,
            mat(new Color(0.965f, 0.97f, 0.94f, 1f), 0.90f), attrs));
        Model newBallShadowModel = own(owned, v8Builder.createCylinder(0.80f, 0.018f, 0.56f, 24,
            mat(new Color(0.010f, 0.012f, 0.015f, 1f), 0.02f), attrs));
        Model newSelectModel = own(owned, v8Builder.createCylinder(1.30f, 0.025f, 1.30f, 32,
            mat(new Color(0.62f, 1.0f, 0.08f, 1f), 0.72f), attrs));

        Field ballModelField = field(base, "ballModel");
        Field ballShadowModelField = field(base, "ballShadowModel");
        Field selectRingModelField = field(base, "selectRingModel");
        Field ballField = field(base, "ball");
        Field ballShadowField = field(base, "ballShadow");
        Field selectRingField = field(base, "selectRing");

        ballModelField.set(this, newBallModel);
        ballShadowModelField.set(this, newBallShadowModel);
        selectRingModelField.set(this, newSelectModel);
        ballField.set(this, new ModelInstance(newBallModel));
        ballShadowField.set(this, new ModelInstance(newBallShadowModel));
        selectRingField.set(this, new ModelInstance(newSelectModel));
    }

    @Override
    public void render() {
        super.render();
        drawEliteOverlay();
    }

    private void drawEliteOverlay() {
        if (overlayShapes == null || overlayBatch == null) return;
        String screenName = "";
        try {
            Object s = screenField != null ? screenField.get(this) : null;
            screenName = s == null ? "" : s.toString();
        } catch (Throwable ignored) { }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        overlayShapes.setProjectionMatrix(overlayMatrix);
        overlayBatch.setProjectionMatrix(overlayMatrix);

        if ("MATCH".equals(screenName)) {
            drawVignette();
            drawRadar();
        }

        overlayShapes.begin(ShapeRenderer.ShapeType.Filled);
        overlayShapes.setColor(0.025f, 0.035f, 0.05f, 0.86f);
        overlayShapes.rect(sw * 0.825f, sh * 0.935f, sw * 0.155f, sh * 0.045f);
        overlayShapes.setColor(0.62f, 1f, 0.10f, 1f);
        overlayShapes.rect(sw * 0.825f, sh * 0.935f, 4f, sh * 0.045f);
        overlayShapes.end();

        overlayBatch.begin();
        overlaySmall.setColor(Color.WHITE);
        overlaySmall.draw(overlayBatch, "futREAL 8  ELITE", sw * 0.838f, sh * 0.968f);
        overlayBatch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawVignette() {
        overlayShapes.begin(ShapeRenderer.ShapeType.Filled);
        overlayShapes.setColor(0.0f, 0.0f, 0.0f, 0.17f);
        overlayShapes.rect(0, 0, sw, sh * 0.025f);
        overlayShapes.rect(0, sh * 0.975f, sw, sh * 0.025f);
        overlayShapes.setColor(0.0f, 0.0f, 0.0f, 0.10f);
        overlayShapes.rect(0, 0, sw * 0.018f, sh);
        overlayShapes.rect(sw * 0.982f, 0, sw * 0.018f, sh);
        overlayShapes.end();
    }

    private void drawRadar() {
        float rw = sw * 0.145f;
        float rh = sh * 0.145f;
        float rx = (sw - rw) * 0.5f;
        float ry = sh * 0.028f;

        overlayShapes.begin(ShapeRenderer.ShapeType.Filled);
        overlayShapes.setColor(0.006f, 0.014f, 0.018f, 0.76f);
        overlayShapes.rect(rx - 8f, ry - 6f, rw + 16f, rh + 12f);
        overlayShapes.setColor(0.05f, 0.28f, 0.12f, 0.80f);
        overlayShapes.rect(rx, ry, rw, rh);
        overlayShapes.setColor(0.86f, 0.91f, 0.88f, 0.78f);
        overlayShapes.rect(rx + rw * 0.5f - 1f, ry, 2f, rh);
        overlayShapes.end();

        drawTeamRadar(blueTeam, rx, ry, rw, rh, new Color(0.12f, 0.65f, 1f, 1f));
        drawTeamRadar(redTeam, rx, ry, rw, rh, new Color(1f, 0.18f, 0.18f, 1f));
    }

    private void drawTeamRadar(Array<?> team, float rx, float ry, float rw, float rh, Color c) {
        if (team == null || posField == null) return;
        overlayShapes.begin(ShapeRenderer.ShapeType.Filled);
        overlayShapes.setColor(c);
        for (int i = 0; i < team.size; i++) {
            try {
                Vector3 p = (Vector3) posField.get(team.get(i));
                float px = rx + ((p.x / FIELD_W) + 0.5f) * rw;
                float py = ry + ((p.z / FIELD_L) + 0.5f) * rh;
                overlayShapes.circle(px, py, Math.max(2.0f, sh * 0.005f), 10);
            } catch (Throwable ignored) { }
        }
        overlayShapes.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        sw = Math.max(1, width);
        sh = Math.max(1, height);
        overlayMatrix.setToOrtho2D(0, 0, sw, sh);
    }

    @Override
    public void dispose() {
        if (overlayShapes != null) overlayShapes.dispose();
        if (overlayBatch != null) overlayBatch.dispose();
        if (overlayFont != null) overlayFont.dispose();
        if (overlaySmall != null) overlaySmall.dispose();
        super.dispose();
    }
}
