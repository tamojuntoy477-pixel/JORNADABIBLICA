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
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * futREAL 12 - MATCH REBUILD.
 *
 * The screenshot from v11 showed the exact problems this class attacks:
 * tiny players, a diagonal prototype camera, giant mobile buttons, neon grass
 * and a shirtless base character. v12 uses a CC0 rigged footballer with real
 * Shirt/Pants/Socks/Shoes material slots, embedded skeletal clips, a closer
 * sideline broadcast camera, a cleaner HUD and a calmer grass treatment.
 *
 * All kits/branding are original futREAL colors. No EA/FC assets, UI, club
 * crests, player likenesses or licensed football content are used.
 */
public class FutRealV12Game extends FutRealV10Game {
    private static final String PLAYER_ASSET = "characters/football-player.glb";
    private static final float FIELD_W = 68f;
    private static final float FIELD_L = 105f;

    private static class HDPlayer {
        Object sim;
        Scene scene;
        String idleClip;
        String walkClip;
        String runClip;
        String activeClip = "";
        float sizeVariation = 1f;
    }

    private final Array<HDPlayer> hdPlayers = new Array<>();
    private SceneAsset footballerAsset;
    private SceneManager sceneManager;
    private PerspectiveCamera camera;
    private DirectionalLightEx keyLight;
    private boolean hdReady;

    private ModelBuilder presentationBuilder;
    private Texture pitchTexture;

    private ShapeRenderer uiShapes;
    private SpriteBatch uiBatch;
    private BitmapFont uiFont;
    private BitmapFont uiSmall;
    private final Matrix4 ui = new Matrix4();
    private int sw = 1;
    private int sh = 1;

    private Field screenField;
    private Field blueField;
    private Field redField;
    private Field stadiumField;
    private Field ownedModelsField;
    private Field cameraField;
    private Field ballPosField;
    private Field blueScoreField;
    private Field redScoreField;
    private Field matchRealSecondsField;
    private Field controlledField;
    private Field moveField;
    private Field fullTimeField;
    private Field bannerTimerField;
    private Field bannerField;

    private Field posField;
    private Field facingField;
    private Field runBlendField;
    private Field keeperField;
    private Field playerBlueField;
    private Field playerIndexField;

    private Method render3DMethod;

    private float modelScale = 1f;
    private float modelFloorOffset = 0f;

    @Override
    public void create() {
        super.create();
        presentationBuilder = new ModelBuilder();
        uiShapes = new ShapeRenderer();
        uiBatch = new SpriteBatch();
        uiFont = new BitmapFont();
        uiSmall = new BitmapFont();
        uiFont.getData().setScale(1.05f);
        uiSmall.getData().setScale(0.76f);

        try {
            bindBase();
            buildNaturalPitchAndFarStand();
            buildFootballPlayers();
            hideLegacyBodies();
            hideUltraAccessories();
            hdReady = true;
        } catch (Throwable t) {
            hdReady = false;
            Gdx.app.error("futREAL-V12", "Football presentation rebuild failed; fallback remains playable", t);
            disposeHd();
        }
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private Method method(Class<?> type, String name) throws Exception {
        Method m = type.getDeclaredMethod(name);
        m.setAccessible(true);
        return m;
    }

    @SuppressWarnings("unchecked")
    private void bindBase() throws Exception {
        Class<?> base = FutRealV7Game.class;
        screenField = field(base, "screen");
        blueField = field(base, "blue");
        redField = field(base, "red");
        stadiumField = field(base, "stadium");
        ownedModelsField = field(base, "ownedModels");
        cameraField = field(base, "camera");
        ballPosField = field(base, "ballPos");
        blueScoreField = field(base, "blueScore");
        redScoreField = field(base, "redScore");
        matchRealSecondsField = field(base, "matchRealSeconds");
        controlledField = field(base, "controlled");
        moveField = field(base, "move");
        fullTimeField = field(base, "fullTime");
        bannerTimerField = field(base, "bannerTimer");
        bannerField = field(base, "banner");
        render3DMethod = method(base, "render3D");
        camera = (PerspectiveCamera) cameraField.get(this);

        Array<Object> blue = (Array<Object>) blueField.get(this);
        if (blue == null || blue.size == 0) throw new IllegalStateException("No football simulation players");
        Class<?> pc = blue.first().getClass();
        posField = field(pc, "pos");
        facingField = field(pc, "facing");
        runBlendField = field(pc, "runBlend");
        keeperField = field(pc, "keeper");
        playerBlueField = field(pc, "blue");
        playerIndexField = field(pc, "index");
    }

    @SuppressWarnings("unchecked")
    private void buildNaturalPitchAndFarStand() throws Exception {
        Array<ModelInstance> stadium = (Array<ModelInstance>) stadiumField.get(this);
        Array<Model> owned = (Array<Model>) ownedModelsField.get(this);

        Pixmap p = new Pixmap(512, 512, Pixmap.Format.RGBA8888);
        Random rng = new Random(121219L);
        for (int y = 0; y < 512; y++) {
            boolean stripe = ((y / 46) & 1) == 0;
            for (int x = 0; x < 512; x++) {
                float n = (rng.nextFloat() - 0.5f) * 0.020f;
                float r = 0.040f + n * 0.45f;
                float g = (stripe ? 0.245f : 0.205f) + n;
                float b = 0.050f + n * 0.30f;
                p.setColor(MathUtils.clamp(r, 0f, 1f), MathUtils.clamp(g, 0f, 1f), MathUtils.clamp(b, 0f, 1f), 1f);
                p.drawPixel(x, y);
            }
        }
        p.setColor(0.07f, 0.30f, 0.07f, 0.30f);
        for (int x = 0; x < 512; x += 32) p.drawLine(x, 0, x, 511);
        pitchTexture = new Texture(p);
        pitchTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pitchTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        p.dispose();

        long texAttrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;
        Material grass = new Material(
            TextureAttribute.createDiffuse(pitchTexture),
            ColorAttribute.createSpecular(new Color(0.035f, 0.045f, 0.035f, 1f))
        );
        Model pitch = presentationBuilder.createBox(68f, 0.004f, 105f, grass, texAttrs);
        owned.add(pitch);
        ModelInstance pitchI = new ModelInstance(pitch);
        pitchI.transform.setToTranslation(0f, 0.025f, 0f);
        stadium.add(pitchI);

        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Color[] crowdBands = {
            new Color(0.075f, 0.105f, 0.145f, 1f),
            new Color(0.11f, 0.15f, 0.19f, 1f),
            new Color(0.10f, 0.18f, 0.24f, 1f),
            new Color(0.16f, 0.13f, 0.18f, 1f)
        };
        for (int row = 0; row < 8; row++) {
            Material m = new Material(ColorAttribute.createDiffuse(crowdBands[row % crowdBands.length]));
            Model tier = presentationBuilder.createBox(2.15f, 0.58f, 116f, m, attrs);
            owned.add(tier);
            ModelInstance t = new ModelInstance(tier);
            t.transform.setToTranslation(-38.2f - row * 1.10f, 1.05f + row * 1.02f, 0f);
            stadium.add(t);
        }
        Material roofMat = new Material(ColorAttribute.createDiffuse(new Color(0.035f, 0.050f, 0.070f, 1f)));
        Model roof = presentationBuilder.createBox(13.5f, 0.38f, 121f, roofMat, attrs);
        owned.add(roof);
        ModelInstance roofI = new ModelInstance(roof);
        roofI.transform.setToTranslation(-45.5f, 10.25f, 0f);
        stadium.add(roofI);
    }

    @SuppressWarnings("unchecked")
    private void buildFootballPlayers() throws Exception {
        footballerAsset = new GLBLoader().load(Gdx.files.internal(PLAYER_ASSET));
        if (footballerAsset == null || footballerAsset.scene == null || footballerAsset.scene.model == null) {
            throw new IllegalStateException("CC0 footballer GLB missing");
        }

        ModelInstance measure = new ModelInstance(footballerAsset.scene.model);
        BoundingBox box = new BoundingBox();
        measure.calculateBoundingBox(box);
        float rawHeight = Math.max(0.0001f, box.getHeight());
        modelScale = 1.82f / rawHeight;
        modelFloorOffset = -box.min.y * modelScale;

        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.numBones = 72;
        config.numDirectionalLights = 1;
        config.numPointLights = 0;
        config.numSpotLights = 0;
        DepthShader.Config depth = PBRShaderProvider.createDefaultDepthConfig();
        depth.numBones = 72;
        sceneManager = new SceneManager(new PBRShaderProvider(config), new PBRDepthShaderProvider(depth));
        sceneManager.setCamera(camera);
        sceneManager.setAmbientLight(0.54f);

        keyLight = new DirectionalLightEx();
        keyLight.direction.set(-0.28f, -0.94f, -0.19f).nor();
        keyLight.color.set(1.00f, 0.97f, 0.91f, 1f);
        sceneManager.environment.add(keyLight);

        Array<Object> blue = (Array<Object>) blueField.get(this);
        Array<Object> red = (Array<Object>) redField.get(this);
        for (int i = 0; i < blue.size; i++) addPlayer(blue.get(i), true, i);
        for (int i = 0; i < red.size; i++) addPlayer(red.get(i), false, i);
    }

    private void addPlayer(Object sim, boolean blue, int index) throws Exception {
        Scene scene = new Scene(footballerAsset.scene);
        recolorKit(scene.modelInstance.nodes, blue, keeperField.getBoolean(sim));

        HDPlayer hp = new HDPlayer();
        hp.sim = sim;
        hp.scene = scene;
        hp.idleClip = findClip(scene.modelInstance.animations, "idle");
        hp.walkClip = findClip(scene.modelInstance.animations, "walk");
        hp.runClip = findClip(scene.modelInstance.animations, "run");
        hp.sizeVariation = 0.97f + (index % 5) * 0.015f;
        hdPlayers.add(hp);
        sceneManager.addScene(scene);

        if (scene.animationController != null && hp.idleClip != null) {
            scene.animationController.animate(hp.idleClip, -1, 1f, null, 0f);
            scene.animationController.update((index + (blue ? 2 : 7)) * 0.037f);
            hp.activeClip = hp.idleClip;
        }
    }

    private String findClip(Array<Animation> animations, String key) {
        String k = key.toLowerCase();
        for (Animation a : animations) {
            if (a == null || a.id == null) continue;
            String id = a.id.toLowerCase();
            if (id.endsWith("man_" + k) || id.endsWith("_" + k) || id.equals(k) || id.contains("man_" + k)) return a.id;
        }
        return animations.size > 0 ? animations.first().id : null;
    }

    private void recolorKit(Array<Node> roots, boolean blue, boolean keeper) {
        for (Node root : roots) recolorNode(root, blue, keeper);
    }

    private void recolorNode(Node node, boolean blue, boolean keeper) {
        for (NodePart part : node.parts) {
            Material copy = new Material(part.material);
            String id = copy.id == null ? "" : copy.id.toLowerCase();
            Color target = null;
            if (id.equals("shirt") || id.contains("shirt2")) {
                target = keeper
                    ? new Color(0.96f, 0.74f, 0.08f, 1f)
                    : blue ? new Color(0.035f, 0.42f, 0.92f, 1f) : new Color(0.82f, 0.055f, 0.10f, 1f);
                if (id.contains("shirt2")) target.mul(0.72f);
            } else if (id.contains("pants")) {
                target = keeper
                    ? new Color(0.12f, 0.12f, 0.14f, 1f)
                    : blue ? new Color(0.94f, 0.95f, 0.97f, 1f) : new Color(0.08f, 0.09f, 0.12f, 1f);
            } else if (id.contains("socks")) {
                target = blue ? new Color(0.08f, 0.38f, 0.82f, 1f) : new Color(0.72f, 0.04f, 0.08f, 1f);
            } else if (id.contains("shoes")) {
                target = new Color(0.025f, 0.028f, 0.035f, 1f);
            }
            if (target != null) copy.set(PBRColorAttribute.createBaseColorFactor(target));
            part.material = copy;
        }
        for (Node child : node.getChildren()) recolorNode(child, blue, keeper);
    }

    @SuppressWarnings("unchecked")
    private void hideLegacyBodies() throws Exception {
        Array<Object> blue = (Array<Object>) blueField.get(this);
        Array<Object> red = (Array<Object>) redField.get(this);
        for (Object p : blue) hideLegacyPlayer(p);
        for (Object p : red) hideLegacyPlayer(p);
    }

    private void hideLegacyPlayer(Object player) throws Exception {
        Class<?> pc = player.getClass();
        String[] names = {"torso", "shorts", "head", "armL", "armR", "legL", "legR", "bootL", "bootR"};
        for (String name : names) {
            Field f = field(pc, name);
            Object value = f.get(player);
            if (value instanceof ModelInstance) makeInvisible((ModelInstance)value);
        }
    }

    private void hideUltraAccessories() {
        try {
            Field detailField = field(FutRealV10Game.class, "playerDetails");
            Object raw = detailField.get(this);
            if (!(raw instanceof Array)) return;
            for (Object detail : (Array<?>)raw) {
                for (Field f : detail.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Object value = f.get(detail);
                    if (value instanceof ModelInstance) makeInvisible((ModelInstance)value);
                }
            }
        } catch (Throwable ignored) { }
    }

    private void makeInvisible(ModelInstance instance) {
        if (instance == null) return;
        for (Material material : instance.materials) {
            ColorAttribute diffuse = material.get(ColorAttribute.class, ColorAttribute.Diffuse);
            if (diffuse != null) diffuse.color.a = 0f;
            material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0f));
            material.set(new DepthTestAttribute(GL20.GL_LEQUAL, 0f, 1f, false));
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

    private void reframeBroadcastCamera() {
        if (camera == null) return;
        try {
            Vector3 ball = (Vector3) ballPosField.get(this);
            Object controlled = controlledField.get(this);
            Vector3 cp = controlled == null ? ball : (Vector3) posField.get(controlled);
            float fx = ball.x * 0.68f + cp.x * 0.32f;
            float fz = ball.z * 0.74f + cp.z * 0.26f;

            camera.fieldOfView = 43.5f;
            camera.position.set(41.5f, 16.8f, MathUtils.clamp(fz + 5.0f, -43f, 43f));
            camera.up.set(Vector3.Y);
            camera.lookAt(MathUtils.clamp(fx, -16f, 16f), 0.72f, fz - 1.5f);
            camera.near = 0.08f;
            camera.far = 340f;
            camera.update();
        } catch (Throwable ignored) { }
    }

    private void updateHdTransforms(float dt) {
        for (HDPlayer hp : hdPlayers) {
            try {
                Vector3 pos = (Vector3) posField.get(hp.sim);
                Vector3 facing = (Vector3) facingField.get(hp.sim);
                float runBlend = runBlendField.getFloat(hp.sim);
                int idx = playerIndexField.getInt(hp.sim);
                float angle = (float)Math.toDegrees(Math.atan2(facing.x, facing.z));
                float s = modelScale * hp.sizeVariation;
                float breathe = (float)Math.sin(com.badlogic.gdx.utils.TimeUtils.millis() * 0.0022f + idx * 0.71f) * 0.008f;

                hp.scene.modelInstance.transform.idt()
                    .translate(pos.x, modelFloorOffset * hp.sizeVariation + breathe, pos.z)
                    .rotate(Vector3.Y, angle)
                    .scale(s, s, s);

                String wanted = runBlend > 0.58f ? hp.runClip : runBlend > 0.08f ? hp.walkClip : hp.idleClip;
                if (wanted != null && !wanted.equals(hp.activeClip) && hp.scene.animationController != null) {
                    float speed = runBlend > 0.58f ? 1.15f : 1.0f;
                    hp.scene.animationController.animate(wanted, -1, speed, null, 0.12f);
                    hp.activeClip = wanted;
                }
            } catch (Throwable ignored) { }
        }
        if (sceneManager != null) sceneManager.update(dt);
    }

    private void renderHdPlayers(float dt) {
        if (!hdReady || sceneManager == null || camera == null) return;
        updateHdTransforms(dt);
        sceneManager.setCamera(camera);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        sceneManager.render();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);

        // Let the proven v10 gameplay stack process controls, AI, ball physics,
        // career state and extra pass/dribble actions. Then replace only the
        // final match picture with the v12 presentation.
        super.render();

        if (hdReady && isMatch()) {
            try {
                reframeBroadcastCamera();
                render3DMethod.invoke(this); // clears old HUD and redraws pitch/ball with v12 camera
                renderHdPlayers(dt);
                drawMatchHudV12();
            } catch (Throwable t) {
                Gdx.app.error("futREAL-V12", "v12 presentation frame failed", t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void drawMatchHudV12() {
        uiShapes.setProjectionMatrix(ui);
        uiBatch.setProjectionMatrix(ui);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        int bs = 0;
        int rs = 0;
        int minute = 0;
        boolean fullTime = false;
        float bannerTimer = 0f;
        String banner = "";
        Vector2 move = new Vector2();
        int controlledNumber = 0;
        try {
            bs = blueScoreField.getInt(this);
            rs = redScoreField.getInt(this);
            minute = Math.min(90, (int)(matchRealSecondsField.getFloat(this) * 0.60f));
            fullTime = fullTimeField.getBoolean(this);
            bannerTimer = bannerTimerField.getFloat(this);
            banner = String.valueOf(bannerField.get(this));
            Vector2 src = (Vector2) moveField.get(this);
            move.set(src);
            Object c = controlledField.get(this);
            if (c != null) controlledNumber = playerIndexField.getInt(c) + 1;
        } catch (Throwable ignored) { }

        float scoreW = Math.min(sw * 0.255f, 360f);
        float scoreH = Math.min(sh * 0.075f, 58f);
        float scoreX = (sw - scoreW) * 0.5f;
        float scoreY = sh - scoreH - 12f;

        float joyX = Math.max(92f, sw * 0.095f);
        float joyY = Math.max(82f, sh * 0.145f);
        float joyR = Math.min(64f, sh * 0.105f);

        uiShapes.begin(ShapeRenderer.ShapeType.Filled);
        uiShapes.setColor(0.006f, 0.012f, 0.020f, 0.88f);
        uiShapes.rect(scoreX, scoreY, scoreW, scoreH);
        uiShapes.setColor(0.55f, 0.96f, 0.12f, 1f);
        uiShapes.rect(scoreX, scoreY, 4f, scoreH);

        uiShapes.setColor(0.015f, 0.025f, 0.035f, 0.58f);
        uiShapes.circle(joyX, joyY, joyR, 32);
        uiShapes.setColor(0.42f, 0.50f, 0.55f, 0.72f);
        uiShapes.circle(joyX + move.x * joyR * 0.50f, joyY + move.y * joyR * 0.50f, joyR * 0.31f, 24);

        drawButton(sw * 0.915f, sh * 0.155f, sh * 0.060f, new Color(0.92f, 0.10f, 0.10f, 0.82f));
        drawButton(sw * 0.785f, sh * 0.125f, sh * 0.047f, new Color(0.08f, 0.42f, 0.95f, 0.82f));
        drawButton(sw * 0.785f, sh * 0.315f, sh * 0.047f, new Color(0.60f, 0.95f, 0.10f, 0.82f));
        drawButton(sw * 0.925f, sh * 0.735f, sh * 0.040f, new Color(0.47f, 0.55f, 0.64f, 0.76f));
        drawButton(sw * 0.615f, sh * 0.120f, sh * 0.040f, new Color(0.08f, 0.42f, 0.95f, 0.76f));
        drawButton(sw * 0.615f, sh * 0.315f, sh * 0.040f, new Color(0.60f, 0.95f, 0.10f, 0.76f));

        // compact radar
        float rw = Math.min(sw * 0.125f, 180f);
        float rh = rw * 0.63f;
        float rx = (sw - rw) * 0.5f;
        float ry = 12f;
        uiShapes.setColor(0.006f, 0.014f, 0.020f, 0.73f);
        uiShapes.rect(rx - 5f, ry - 4f, rw + 10f, rh + 8f);
        uiShapes.setColor(0.035f, 0.20f, 0.08f, 0.82f);
        uiShapes.rect(rx, ry, rw, rh);
        uiShapes.setColor(0.90f, 0.93f, 0.90f, 0.68f);
        uiShapes.rect(rx + rw * 0.5f - 0.7f, ry, 1.4f, rh);
        uiShapes.end();

        try {
            drawRadarTeam((Array<Object>) blueField.get(this), rx, ry, rw, rh, new Color(0.10f, 0.55f, 1f, 1f));
            drawRadarTeam((Array<Object>) redField.get(this), rx, ry, rw, rh, new Color(1f, 0.15f, 0.15f, 1f));
        } catch (Throwable ignored) { }

        uiBatch.begin();
        uiFont.setColor(Color.WHITE);
        uiFont.draw(uiBatch, "AUR  " + bs + " - " + rs + "  VIL", scoreX + 20f, scoreY + scoreH * 0.66f);
        uiSmall.setColor(new Color(0.62f, 1f, 0.12f, 1f));
        uiSmall.draw(uiBatch, String.format("%02d'", minute), scoreX + scoreW - 44f, scoreY + scoreH * 0.66f);

        uiSmall.setColor(Color.WHITE);
        label("CHUTE", sw * 0.915f, sh * 0.155f);
        label("PASSE", sw * 0.785f, sh * 0.125f);
        label("SPRINT", sw * 0.785f, sh * 0.315f);
        label("TROCA", sw * 0.925f, sh * 0.735f);
        label("ENFIADA", sw * 0.615f, sh * 0.120f);
        label("DRIBLE", sw * 0.615f, sh * 0.315f);
        uiSmall.setColor(new Color(0.62f, 1f, 0.12f, 1f));
        uiSmall.draw(uiBatch, "futREAL 12", 14f, sh - 18f);
        uiSmall.setColor(Color.WHITE);
        uiSmall.draw(uiBatch, "#" + controlledNumber, 14f, sh - 36f);

        if ((bannerTimer > 0f || fullTime) && banner != null && !banner.isEmpty()) {
            uiFont.setColor(Color.WHITE);
            uiFont.draw(uiBatch, banner, sw * 0.42f, sh * 0.72f);
        }
        uiBatch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawButton(float x, float y, float r, Color c) {
        uiShapes.setColor(c);
        uiShapes.circle(x, y, Math.max(22f, r), 28);
    }

    private void label(String text, float x, float y) {
        float approx = text.length() * 3.4f;
        uiSmall.draw(uiBatch, text, x - approx, y + 4f);
    }

    private void drawRadarTeam(Array<Object> team, float rx, float ry, float rw, float rh, Color c) {
        uiShapes.begin(ShapeRenderer.ShapeType.Filled);
        uiShapes.setColor(c);
        for (Object player : team) {
            try {
                Vector3 p = (Vector3) posField.get(player);
                float px = rx + ((p.z / FIELD_L) + 0.5f) * rw;
                float py = ry + (0.5f - p.x / FIELD_W) * rh;
                uiShapes.circle(px, py, Math.max(1.8f, sh * 0.0038f), 9);
            } catch (Throwable ignored) { }
        }
        uiShapes.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        sw = Math.max(1, width);
        sh = Math.max(1, height);
        ui.setToOrtho2D(0f, 0f, sw, sh);
        if (sceneManager != null && camera != null) {
            sceneManager.setCamera(camera);
            sceneManager.updateViewport(sw, sh);
        }
    }

    private void disposeHd() {
        try { if (sceneManager != null) sceneManager.dispose(); } catch (Throwable ignored) { }
        sceneManager = null;
        try { if (footballerAsset != null) footballerAsset.dispose(); } catch (Throwable ignored) { }
        footballerAsset = null;
        hdPlayers.clear();
    }

    @Override
    public void dispose() {
        if (uiShapes != null) uiShapes.dispose();
        if (uiBatch != null) uiBatch.dispose();
        if (uiFont != null) uiFont.dispose();
        if (uiSmall != null) uiSmall.dispose();
        if (pitchTexture != null) pitchTexture.dispose();
        disposeHd();
        super.dispose();
    }
}
