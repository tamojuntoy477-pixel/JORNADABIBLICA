package com.nox.futreal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * futREAL 10 ULTRA
 * Original procedural high-detail Android football layer.
 * No EA/FC art, logos, player likenesses or licensed club assets are used.
 */
public class FutRealV10Game extends FutRealV9Game {
    private ModelBuilder ultraBuilder;
    private Texture grassTexture;
    private ShapeRenderer ultraShapes;
    private SpriteBatch ultraBatch;
    private BitmapFont ultraFont;
    private final Matrix4 ultraUi = new Matrix4();
    private int sw = 1;
    private int sh = 1;

    private Field stadiumField;
    private Field ownedModelsField;
    private Field environmentField;
    private Field cameraField;
    private Field blueTeamField;
    private Field redTeamField;
    private Field ballPosField;
    private Field ballVelField;
    private Field blueScoreField;
    private Field redScoreField;

    private Field playerPosField;
    private Field playerKeeperField;
    private Field playerBlueField;
    private Field playerIndexField;

    private final Array<PlayerDetail> playerDetails = new Array<>();
    private final Array<AnimatedLight> animatedLights = new Array<>();
    private PerspectiveCamera camera;
    private Vector3 ballPos;
    private Vector3 ballVel;
    private int lastBlueScore;
    private int lastRedScore;
    private float goalPulse;

    private static class PlayerDetail {
        Object player;
        ModelInstance hair;
        ModelInstance neck;
        ModelInstance shoulderL;
        ModelInstance shoulderR;
        ModelInstance sockL;
        ModelInstance sockR;
        ModelInstance chestAccent;
    }

    private static class AnimatedLight {
        ModelInstance instance;
        float x, y, z;
        float phase;
    }

    @Override
    public void create() {
        super.create();
        ultraBuilder = new ModelBuilder();
        ultraShapes = new ShapeRenderer();
        ultraBatch = new SpriteBatch();
        ultraFont = new BitmapFont();
        ultraFont.getData().setScale(0.82f);

        try {
            Class<?> base = FutRealV7Game.class;
            stadiumField = field(base, "stadium");
            ownedModelsField = field(base, "ownedModels");
            environmentField = field(base, "environment");
            cameraField = field(base, "camera");
            blueTeamField = field(base, "blue");
            redTeamField = field(base, "red");
            ballPosField = field(base, "ballPos");
            ballVelField = field(base, "ballVel");
            blueScoreField = field(base, "blueScore");
            redScoreField = field(base, "redScore");

            @SuppressWarnings("unchecked")
            Array<ModelInstance> stadium = (Array<ModelInstance>) stadiumField.get(this);
            @SuppressWarnings("unchecked")
            Array<Model> owned = (Array<Model>) ownedModelsField.get(this);
            @SuppressWarnings("unchecked")
            Array<Object> blue = (Array<Object>) blueTeamField.get(this);
            @SuppressWarnings("unchecked")
            Array<Object> red = (Array<Object>) redTeamField.get(this);

            camera = (PerspectiveCamera) cameraField.get(this);
            ballPos = (Vector3) ballPosField.get(this);
            ballVel = (Vector3) ballVelField.get(this);

            if (blue.size > 0) {
                Class<?> pc = blue.get(0).getClass();
                playerPosField = field(pc, "pos");
                playerKeeperField = field(pc, "keeper");
                playerBlueField = field(pc, "blue");
                playerIndexField = field(pc, "index");
            }

            buildUltraGrass(stadium, owned);
            buildUltraArena(stadium, owned);
            buildPlayerDetails(blue, stadium, owned);
            buildPlayerDetails(red, stadium, owned);
            tuneUltraLighting((Environment) environmentField.get(this));

            lastBlueScore = blueScoreField.getInt(this);
            lastRedScore = redScoreField.getInt(this);
            camera.fieldOfView = 37.5f;
            camera.near = 0.07f;
            camera.far = 620f;
            camera.update();
        } catch (Throwable t) {
            Gdx.app.error("futREAL-V10", "ULTRA layer initialization failed", t);
        }

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Field field(Class<?> c, String name) throws NoSuchFieldException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private Material colorMat(Color diffuse, float specular) {
        return new Material(
            ColorAttribute.createDiffuse(diffuse),
            ColorAttribute.createSpecular(new Color(specular, specular, specular, 1f))
        );
    }

    private Model own(Array<Model> owned, Model model) {
        owned.add(model);
        return model;
    }

    private ModelInstance add(Array<ModelInstance> list, Model model, float x, float y, float z) {
        ModelInstance i = new ModelInstance(model);
        i.transform.setToTranslation(x, y, z);
        list.add(i);
        return i;
    }

    private void buildUltraGrass(Array<ModelInstance> stadium, Array<Model> owned) {
        Pixmap pixmap = new Pixmap(384, 384, Pixmap.Format.RGBA8888);
        Random random = new Random(1919L);
        for (int y = 0; y < 384; y++) {
            boolean lightBand = ((y / 24) & 1) == 0;
            for (int x = 0; x < 384; x++) {
                float noise = (random.nextFloat() - 0.5f) * 0.045f;
                float r = 0.022f + noise * 0.20f;
                float g = (lightBand ? 0.355f : 0.305f) + noise;
                float b = 0.068f + noise * 0.30f;
                pixmap.setColor(MathUtils.clamp(r, 0f, 1f), MathUtils.clamp(g, 0f, 1f), MathUtils.clamp(b, 0f, 1f), 1f);
                pixmap.drawPixel(x, y);
            }
        }
        // Fine mowing direction and worn centre areas.
        pixmap.setColor(0.07f, 0.43f, 0.10f, 0.28f);
        for (int x = 0; x < 384; x += 12) pixmap.drawLine(x, 0, x, 383);
        pixmap.setColor(0.13f, 0.34f, 0.08f, 0.18f);
        for (int y = 174; y < 210; y += 4) pixmap.drawLine(130, y, 254, y);

        grassTexture = new Texture(pixmap);
        grassTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        grassTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        pixmap.dispose();

        Material grassMat = new Material(
            TextureAttribute.createDiffuse(grassTexture),
            ColorAttribute.createSpecular(new Color(0.055f, 0.065f, 0.055f, 1f))
        );
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;
        Model grass = own(owned, ultraBuilder.createBox(68f, 0.022f, 105f, grassMat, attrs));
        add(stadium, grass, 0f, 0.013f, 0f);

        // Subtle cut lines keep the pitch readable from the broadcast camera.
        Model cut = own(owned, ultraBuilder.createBox(67.8f, 0.008f, 0.022f,
            colorMat(new Color(0.18f, 0.54f, 0.17f, 1f), 0.04f),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal));
        for (float z = -49f; z <= 49f; z += 7f) add(stadium, cut, 0f, 0.027f, z);
    }

    private void buildUltraArena(Array<ModelInstance> stadium, Array<Model> owned) {
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        Color steel = new Color(0.055f, 0.065f, 0.085f, 1f);
        Color glass = new Color(0.07f, 0.16f, 0.23f, 1f);
        Color white = new Color(0.94f, 0.98f, 1f, 1f);
        Color cyan = new Color(0.02f, 0.62f, 1f, 1f);
        Color lime = new Color(0.64f, 1f, 0.12f, 1f);

        // Premium press/VIP boxes above the near stand.
        Model press = own(owned, ultraBuilder.createBox(22f, 3.2f, 5.2f, colorMat(glass, 0.95f), attrs));
        Model pressRoof = own(owned, ultraBuilder.createBox(24f, 0.35f, 6f, colorMat(steel, 0.65f), attrs));
        add(stadium, press, -49.6f, 14.3f, 0f);
        add(stadium, pressRoof, -49.6f, 16.0f, 0f);

        // Continuous stadium light arrays.
        Model lamp = own(owned, ultraBuilder.createSphere(0.42f, 0.28f, 0.24f, 10, 6, colorMat(white, 1f), attrs));
        for (int side : new int[]{-1, 1}) {
            for (int z = -54; z <= 54; z += 4) {
                AnimatedLight al = new AnimatedLight();
                al.x = side * 44.8f;
                al.y = 16.1f;
                al.z = z;
                al.phase = (z + 60f) * 0.13f + side;
                al.instance = add(stadium, lamp, al.x, al.y, al.z);
                animatedLights.add(al);
            }
        }
        for (int end : new int[]{-1, 1}) {
            for (int x = -32; x <= 32; x += 4) {
                AnimatedLight al = new AnimatedLight();
                al.x = x;
                al.y = 15.6f;
                al.z = end * 63.3f;
                al.phase = (x + 35f) * 0.12f + end * 2f;
                al.instance = add(stadium, lamp, al.x, al.y, al.z);
                animatedLights.add(al);
            }
        }

        // Original futREAL LED ribbon on all four sides.
        Model sideLedCyan = own(owned, ultraBuilder.createBox(0.16f, 0.82f, 7.8f, colorMat(cyan, 1f), attrs));
        Model sideLedLime = own(owned, ultraBuilder.createBox(0.16f, 0.82f, 7.8f, colorMat(lime, 1f), attrs));
        for (int side : new int[]{-1, 1}) {
            for (int z = -48, n = 0; z <= 48; z += 8, n++) {
                add(stadium, (n & 1) == 0 ? sideLedCyan : sideLedLime, side * 35.35f, 0.62f, z);
            }
        }
        Model endLedCyan = own(owned, ultraBuilder.createBox(7.2f, 0.82f, 0.16f, colorMat(cyan, 1f), attrs));
        Model endLedLime = own(owned, ultraBuilder.createBox(7.2f, 0.82f, 0.16f, colorMat(lime, 1f), attrs));
        for (int end : new int[]{-1, 1}) {
            for (int x = -28, n = 0; x <= 28; x += 8, n++) {
                add(stadium, (n & 1) == 0 ? endLedLime : endLedCyan, x, 0.62f, end * 54.2f);
            }
        }

        // Roof braces add depth when the camera pans.
        Model brace = own(owned, ultraBuilder.createBox(0.26f, 0.26f, 18f, colorMat(steel, 0.62f), attrs));
        for (int side : new int[]{-1, 1}) {
            for (int z = -45; z <= 45; z += 18) {
                ModelInstance b = add(stadium, brace, side * 46.8f, 15.2f, z);
                b.transform.rotate(Vector3.X, 18f * side);
            }
        }

        // Team benches and technical seats.
        Model benchShell = own(owned, ultraBuilder.createBox(8.5f, 2.15f, 2.3f, colorMat(new Color(0.035f, 0.11f, 0.16f, 1f), 0.82f), attrs));
        Model seat = own(owned, ultraBuilder.createBox(0.56f, 0.62f, 0.56f, colorMat(new Color(0.08f, 0.22f, 0.34f, 1f), 0.42f), attrs));
        for (int z : new int[]{-17, 17}) {
            add(stadium, benchShell, -37.2f, 1.08f, z);
            for (int s = -3; s <= 3; s++) add(stadium, seat, -36.55f, 0.57f, z + s * 0.82f);
        }

        // Stewards around the pitch create human scale without heavy models.
        Model stewardBody = own(owned, ultraBuilder.createCapsule(0.28f, 1.35f, 12, colorMat(new Color(0.92f, 0.68f, 0.06f, 1f), 0.22f), attrs));
        Model stewardHead = own(owned, ultraBuilder.createSphere(0.34f, 0.38f, 0.34f, 10, 7, colorMat(new Color(0.66f, 0.43f, 0.29f, 1f), 0.18f), attrs));
        for (int z = -45; z <= 45; z += 15) {
            add(stadium, stewardBody, 36.4f, 0.8f, z);
            add(stadium, stewardHead, 36.4f, 1.63f, z);
        }

        // Behind-goal digital boards.
        Model megaFrame = own(owned, ultraBuilder.createBox(19f, 5.3f, 0.55f, colorMat(new Color(0.015f, 0.018f, 0.025f, 1f), 0.7f), attrs));
        Model megaScreen = own(owned, ultraBuilder.createBox(17.7f, 4.15f, 0.16f, colorMat(new Color(0.02f, 0.40f, 0.68f, 1f), 1f), attrs));
        add(stadium, megaFrame, 0f, 10.8f, 71.1f);
        add(stadium, megaScreen, 0f, 10.8f, 70.8f);
    }

    private void buildPlayerDetails(Array<Object> team, Array<ModelInstance> stadium, Array<Model> owned) throws IllegalAccessException {
        if (team == null || team.size == 0 || playerPosField == null) return;
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        Model hairDark = own(owned, ultraBuilder.createSphere(0.56f, 0.27f, 0.54f, 18, 10,
            colorMat(new Color(0.025f, 0.020f, 0.018f, 1f), 0.16f), attrs));
        Model hairBrown = own(owned, ultraBuilder.createSphere(0.56f, 0.28f, 0.54f, 18, 10,
            colorMat(new Color(0.12f, 0.065f, 0.035f, 1f), 0.18f), attrs));
        Model neck = own(owned, ultraBuilder.createCylinder(0.25f, 0.34f, 0.25f, 14,
            colorMat(new Color(0.61f, 0.39f, 0.27f, 1f), 0.24f), attrs));
        Model shoulderBlue = own(owned, ultraBuilder.createSphere(0.36f, 0.28f, 0.42f, 12, 8,
            colorMat(new Color(0.035f, 0.52f, 0.98f, 1f), 0.58f), attrs));
        Model shoulderRed = own(owned, ultraBuilder.createSphere(0.36f, 0.28f, 0.42f, 12, 8,
            colorMat(new Color(0.90f, 0.06f, 0.13f, 1f), 0.58f), attrs));
        Model shoulderKeeper = own(owned, ultraBuilder.createSphere(0.36f, 0.28f, 0.42f, 12, 8,
            colorMat(new Color(0.96f, 0.72f, 0.06f, 1f), 0.64f), attrs));
        Model sockWhite = own(owned, ultraBuilder.createCylinder(0.19f, 0.46f, 0.19f, 12,
            colorMat(new Color(0.92f, 0.94f, 0.96f, 1f), 0.30f), attrs));
        Model sockDark = own(owned, ultraBuilder.createCylinder(0.19f, 0.46f, 0.19f, 12,
            colorMat(new Color(0.06f, 0.08f, 0.12f, 1f), 0.30f), attrs));
        Model accentBlue = own(owned, ultraBuilder.createBox(0.62f, 0.11f, 0.47f,
            colorMat(new Color(0.65f, 1f, 0.12f, 1f), 0.7f), attrs));
        Model accentRed = own(owned, ultraBuilder.createBox(0.62f, 0.11f, 0.47f,
            colorMat(new Color(1f, 0.86f, 0.14f, 1f), 0.7f), attrs));

        for (Object p : team) {
            boolean isBlue = playerBlueField.getBoolean(p);
            boolean keeper = playerKeeperField.getBoolean(p);
            int index = playerIndexField.getInt(p);
            PlayerDetail d = new PlayerDetail();
            d.player = p;
            d.hair = add(stadium, (index & 1) == 0 ? hairDark : hairBrown, 0f, 0f, 0f);
            d.neck = add(stadium, neck, 0f, 0f, 0f);
            Model shoulder = keeper ? shoulderKeeper : (isBlue ? shoulderBlue : shoulderRed);
            d.shoulderL = add(stadium, shoulder, 0f, 0f, 0f);
            d.shoulderR = add(stadium, shoulder, 0f, 0f, 0f);
            Model sock = isBlue ? sockWhite : sockDark;
            d.sockL = add(stadium, sock, 0f, 0f, 0f);
            d.sockR = add(stadium, sock, 0f, 0f, 0f);
            d.chestAccent = add(stadium, isBlue ? accentBlue : accentRed, 0f, 0f, 0f);
            playerDetails.add(d);
        }
    }

    private void tuneUltraLighting(Environment env) {
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.40f, 0.43f, 0.50f, 1f));
        env.add(new DirectionalLight().set(1.00f, 0.96f, 0.86f, -0.30f, -0.96f, -0.20f));
        env.add(new DirectionalLight().set(0.42f, 0.54f, 0.82f, 0.76f, -0.52f, 0.34f));
        env.add(new DirectionalLight().set(0.30f, 0.42f, 0.68f, -0.72f, -0.40f, 0.62f));
        env.add(new DirectionalLight().set(0.22f, 0.28f, 0.36f, 0.05f, -0.65f, -0.95f));
    }

    private void updatePlayerDetails() {
        if (playerPosField == null) return;
        try {
            for (PlayerDetail d : playerDetails) {
                Vector3 p = (Vector3) playerPosField.get(d.player);
                float x = p.x;
                float z = p.z;
                d.hair.transform.setToTranslation(x, 2.73f, z);
                d.neck.transform.setToTranslation(x, 2.16f, z);
                d.shoulderL.transform.setToTranslation(x - 0.39f, 1.92f, z);
                d.shoulderR.transform.setToTranslation(x + 0.39f, 1.92f, z);
                d.sockL.transform.setToTranslation(x - 0.19f, 0.55f, z);
                d.sockR.transform.setToTranslation(x + 0.19f, 0.55f, z);
                d.chestAccent.transform.setToTranslation(x, 1.70f, z + 0.245f);
            }
        } catch (Throwable ignored) {
        }
    }

    private void updateBroadcastLook(float dt) {
        if (camera == null || ballVel == null) return;
        float speed = ballVel.len();
        float targetFov = 36.5f + MathUtils.clamp(speed * 0.14f, 0f, 5.5f);
        if (goalPulse > 0f) {
            goalPulse -= dt;
            targetFov = 32.5f + (float)Math.sin(goalPulse * 8f) * 0.8f;
        }
        camera.fieldOfView = MathUtils.lerp(camera.fieldOfView, targetFov, MathUtils.clamp(dt * 3.4f, 0f, 1f));
        camera.update();

        float time = com.badlogic.gdx.utils.TimeUtils.millis() * 0.001f;
        for (AnimatedLight al : animatedLights) {
            float bob = (float)Math.sin(time * 1.45f + al.phase) * 0.035f;
            al.instance.transform.setToTranslation(al.x, al.y + bob, al.z);
        }
    }

    private void detectGoalPulse() {
        if (blueScoreField == null || redScoreField == null) return;
        try {
            int b = blueScoreField.getInt(this);
            int r = redScoreField.getInt(this);
            if (b != lastBlueScore || r != lastRedScore) {
                goalPulse = 2.4f;
                lastBlueScore = b;
                lastRedScore = r;
                if (Gdx.input != null) Gdx.input.vibrate(95);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);
        updatePlayerDetails();
        updateBroadcastLook(dt);
        super.render();
        detectGoalPulse();
        drawUltraOverlay();
    }

    private void drawUltraOverlay() {
        if (ultraShapes == null || ultraBatch == null || ultraFont == null) return;
        ultraUi.setToOrtho2D(0f, 0f, sw, sh);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ultraShapes.setProjectionMatrix(ultraUi);
        ultraShapes.begin(ShapeRenderer.ShapeType.Filled);
        ultraShapes.setColor(0.008f, 0.012f, 0.020f, 0.62f);
        ultraShapes.rect(12f, sh - 42f, 172f, 28f);
        ultraShapes.setColor(0.62f, 1f, 0.12f, 0.92f);
        ultraShapes.rect(12f, sh - 17f, 172f, 3f);
        if (goalPulse > 0f) {
            float a = 0.10f + 0.08f * Math.abs((float)Math.sin(goalPulse * 9f));
            ultraShapes.setColor(0.65f, 1f, 0.15f, a);
            ultraShapes.rect(0f, 0f, sw, sh);
        }
        ultraShapes.end();

        ultraBatch.setProjectionMatrix(ultraUi);
        ultraBatch.begin();
        ultraFont.setColor(Color.WHITE);
        ultraFont.draw(ultraBatch, "futREAL 10  •  ULTRA", 22f, sh - 22f);
        ultraBatch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        sw = Math.max(1, width);
        sh = Math.max(1, height);
        ultraUi.setToOrtho2D(0f, 0f, sw, sh);
    }

    @Override
    public void dispose() {
        if (ultraShapes != null) ultraShapes.dispose();
        if (ultraBatch != null) ultraBatch.dispose();
        if (ultraFont != null) ultraFont.dispose();
        if (grassTexture != null) grassTexture.dispose();
        super.dispose();
    }
}
