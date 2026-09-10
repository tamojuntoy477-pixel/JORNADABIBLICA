package com.nox.futreal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodeAnimation;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
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

/**
 * futREAL 11 REBUILD.
 *
 * First non-procedural player pipeline: a real skinned CC0 GLB character,
 * external skeletal animation library, PBR rendering and instanced 11v11.
 * Existing gameplay/career code is kept while the visual layer is rebuilt.
 * No EA/FC assets, logos, UI, club crests or player likenesses are used.
 */
public class FutRealV11Game extends FutRealV10Game {
    private static final String PLAYER_ASSET = "characters/night-striker.glb";
    private static final String ANIM_ASSET = "characters/universal-animation-library.glb";

    private static class HDPlayer {
        Object simulationPlayer;
        Scene scene;
        String activeClip = "";
        float scale = 1f;
    }

    private final Array<HDPlayer> hdPlayers = new Array<>();
    private SceneAsset characterAsset;
    private SceneAsset animationAsset;
    private SceneManager sceneManager;
    private DirectionalLightEx keyLight;
    private PerspectiveCamera gameCamera;
    private boolean hdReady;
    private String hdStatus = "CARREGANDO PBR";

    private Field screenField;
    private Field blueField;
    private Field redField;
    private Field cameraField;
    private Field posField;
    private Field facingField;
    private Field runBlendField;
    private Field keeperField;
    private Field playerBlueField;
    private Field playerIndexField;

    private Method drawMatchHudMethod;
    private Method drawEliteOverlayMethod;
    private Method drawProControlsMethod;
    private Method drawUltraOverlayMethod;

    private ShapeRenderer rebuildShapes;
    private SpriteBatch rebuildBatch;
    private BitmapFont rebuildFont;
    private final Matrix4 rebuildUi = new Matrix4();
    private int sw = 1;
    private int sh = 1;

    @Override
    public void create() {
        super.create();
        rebuildShapes = new ShapeRenderer();
        rebuildBatch = new SpriteBatch();
        rebuildFont = new BitmapFont();
        rebuildFont.getData().setScale(0.86f);

        try {
            bindBaseState();
            buildHighDefinitionPlayers();
            hideProceduralPlayerBodies();
            hideUltraPlayerAccessories();
            hdReady = true;
            hdStatus = "PBR + RIG + ANIM";
        } catch (Throwable t) {
            hdReady = false;
            hdStatus = "FALLBACK 3D";
            Gdx.app.error("futREAL-V11", "High definition character pipeline unavailable; keeping fallback players", t);
            disposeHdOnly();
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
    private void bindBaseState() throws Exception {
        Class<?> base = FutRealV7Game.class;
        screenField = field(base, "screen");
        blueField = field(base, "blue");
        redField = field(base, "red");
        cameraField = field(base, "camera");
        gameCamera = (PerspectiveCamera) cameraField.get(this);

        Array<Object> blue = (Array<Object>) blueField.get(this);
        if (blue == null || blue.size == 0) throw new IllegalStateException("No simulation players");
        Class<?> pc = blue.first().getClass();
        posField = field(pc, "pos");
        facingField = field(pc, "facing");
        runBlendField = field(pc, "runBlend");
        keeperField = field(pc, "keeper");
        playerBlueField = field(pc, "blue");
        playerIndexField = field(pc, "index");

        drawMatchHudMethod = method(FutRealV7Game.class, "drawMatchHud");
        drawEliteOverlayMethod = method(FutRealV8Game.class, "drawEliteOverlay");
        drawProControlsMethod = method(FutRealV9Game.class, "drawProControls");
        drawUltraOverlayMethod = method(FutRealV10Game.class, "drawUltraOverlay");
    }

    @SuppressWarnings("unchecked")
    private void buildHighDefinitionPlayers() throws Exception {
        GLBLoader loader = new GLBLoader();
        characterAsset = loader.load(Gdx.files.internal(PLAYER_ASSET));
        if (characterAsset == null || characterAsset.scene == null || characterAsset.scene.model == null) {
            throw new IllegalStateException("Character GLB missing scene/model");
        }

        // The CC0 animation library uses the same 65-joint skeleton. Retarget the
        // animation node references onto the character model once, before cloning scenes.
        try {
            animationAsset = loader.load(Gdx.files.internal(ANIM_ASSET));
            if (animationAsset != null && animationAsset.scene != null && animationAsset.scene.model != null) {
                retargetAnimations(characterAsset.scene.model, animationAsset.scene.model);
            }
        } catch (Throwable animationError) {
            Gdx.app.error("futREAL-V11", "Animation library unavailable; using static rig", animationError);
        }

        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.numBones = 65;
        config.numDirectionalLights = 1;
        config.numPointLights = 0;
        config.numSpotLights = 0;
        DepthShader.Config depth = PBRShaderProvider.createDefaultDepthConfig();
        depth.numBones = 65;
        sceneManager = new SceneManager(new PBRShaderProvider(config), new PBRDepthShaderProvider(depth));
        sceneManager.setCamera(gameCamera);
        sceneManager.setAmbientLight(0.60f);

        keyLight = new DirectionalLightEx();
        keyLight.direction.set(-0.36f, -0.92f, -0.28f).nor();
        keyLight.color.set(1.0f, 0.96f, 0.88f, 1f);
        sceneManager.environment.add(keyLight);

        Array<Object> blue = (Array<Object>) blueField.get(this);
        Array<Object> red = (Array<Object>) redField.get(this);
        int n = 0;
        for (Object p : blue) addHdPlayer(p, true, n++);
        n = 0;
        for (Object p : red) addHdPlayer(p, false, n++);
    }

    private void retargetAnimations(Model character, Model library) {
        if (character == null || library == null) return;
        for (Animation source : library.animations) {
            if (source == null || source.id == null || character.getAnimation(source.id) != null) continue;
            Animation targetAnimation = new Animation();
            targetAnimation.id = source.id;
            targetAnimation.duration = source.duration;
            for (NodeAnimation sourceNode : source.nodeAnimations) {
                if (sourceNode == null || sourceNode.node == null || sourceNode.node.id == null) continue;
                Node targetNode = character.getNode(sourceNode.node.id, true);
                if (targetNode == null) continue;
                NodeAnimation targetNodeAnimation = new NodeAnimation();
                targetNodeAnimation.node = targetNode;
                targetNodeAnimation.translation = sourceNode.translation;
                targetNodeAnimation.rotation = sourceNode.rotation;
                targetNodeAnimation.scaling = sourceNode.scaling;
                targetAnimation.nodeAnimations.add(targetNodeAnimation);
            }
            if (targetAnimation.nodeAnimations.size > 0) character.animations.add(targetAnimation);
        }
    }

    private void addHdPlayer(Object simulationPlayer, boolean blue, int index) throws Exception {
        Scene scene = new Scene(characterAsset.scene);
        cloneAndTintMaterials(scene.modelInstance.nodes, blue, keeperField.getBoolean(simulationPlayer), index);

        HDPlayer hp = new HDPlayer();
        hp.simulationPlayer = simulationPlayer;
        hp.scene = scene;
        hp.scale = 0.98f + (index % 4) * 0.012f;
        hdPlayers.add(hp);
        sceneManager.addScene(scene);

        if (scene.animationController != null && scene.modelInstance.getAnimation("Idle_Loop") != null) {
            scene.animationController.animate("Idle_Loop", -1, 1f, null, 0f);
            // Small phase offset prevents all 22 players from moving in perfect lockstep.
            scene.animationController.update(index * 0.043f + (blue ? 0.07f : 0.19f));
            hp.activeClip = "Idle_Loop";
        }
    }

    private void cloneAndTintMaterials(Array<Node> roots, boolean blue, boolean keeper, int index) {
        Color tint;
        if (keeper) tint = new Color(1.00f, 0.86f, 0.36f, 1f);
        else if (blue) tint = new Color(0.66f, 0.84f, 1.00f, 1f);
        else tint = new Color(1.00f, 0.69f, 0.71f, 1f);
        for (Node root : roots) cloneAndTintNode(root, tint, index);
    }

    private void cloneAndTintNode(Node node, Color tint, int index) {
        for (NodePart part : node.parts) {
            Material copy = new Material(part.material);
            String id = copy.id == null ? "" : copy.id.toLowerCase();
            boolean skinLike = id.contains("skin") || id.contains("face") || id.contains("eye") || id.contains("hair");
            if (!skinLike) {
                PBRColorAttribute base = copy.get(PBRColorAttribute.class, PBRColorAttribute.BaseColorFactor);
                Color c = base == null ? Color.WHITE.cpy() : base.color.cpy();
                float mix = 0.42f + (index % 3) * 0.04f;
                c.lerp(tint, mix);
                copy.set(PBRColorAttribute.createBaseColorFactor(c));
            }
            part.material = copy;
        }
        for (Node child : node.getChildren()) cloneAndTintNode(child, tint, index);
    }

    @SuppressWarnings("unchecked")
    private void hideProceduralPlayerBodies() throws Exception {
        Array<Object> blue = (Array<Object>) blueField.get(this);
        Array<Object> red = (Array<Object>) redField.get(this);
        for (Object p : blue) hideLegacyParts(p);
        for (Object p : red) hideLegacyParts(p);
    }

    private void hideLegacyParts(Object player) throws Exception {
        Class<?> pc = player.getClass();
        String[] names = {"torso", "shorts", "head", "armL", "armR", "legL", "legR", "bootL", "bootR"};
        for (String name : names) {
            Field f = field(pc, name);
            Object value = f.get(player);
            if (value instanceof ModelInstance) makeInvisible((ModelInstance)value);
        }
        // Keep the cheap procedural contact shadow and selection ring. They help
        // ground the PBR mesh on lower-end phones without a full shadow pass.
    }

    private void hideUltraPlayerAccessories() {
        try {
            Field detailField = field(FutRealV10Game.class, "playerDetails");
            Object raw = detailField.get(this);
            if (!(raw instanceof Array)) return;
            Array<?> details = (Array<?>)raw;
            for (Object detail : details) {
                for (Field f : detail.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Object value = f.get(detail);
                    if (value instanceof ModelInstance) makeInvisible((ModelInstance)value);
                }
            }
        } catch (Throwable ignored) {
        }
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
            Object screen = screenField == null ? null : screenField.get(this);
            return screen != null && "MATCH".equals(screen.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void updateHdPlayerTransforms() {
        try {
            for (HDPlayer hp : hdPlayers) {
                Vector3 pos = (Vector3)posField.get(hp.simulationPlayer);
                Vector3 facing = (Vector3)facingField.get(hp.simulationPlayer);
                float runBlend = runBlendField.getFloat(hp.simulationPlayer);
                int idx = playerIndexField.getInt(hp.simulationPlayer);

                float angle = (float)Math.toDegrees(Math.atan2(facing.x, facing.z));
                float breathing = (float)Math.sin((com.badlogic.gdx.utils.TimeUtils.millis() * 0.0018f) + idx * 0.63f) * 0.006f;
                hp.scene.modelInstance.transform.idt()
                    .translate(pos.x, breathing, pos.z)
                    .rotate(Vector3.Y, angle)
                    .scale(hp.scale, hp.scale, hp.scale);

                String wanted = runBlend > 0.72f ? "Sprint_Loop" : runBlend > 0.10f ? "Jog_Fwd_Loop" : "Idle_Loop";
                if (!wanted.equals(hp.activeClip) && hp.scene.animationController != null && hp.scene.modelInstance.getAnimation(wanted) != null) {
                    hp.scene.animationController.animate(wanted, -1, 1f, null, 0.14f);
                    hp.activeClip = wanted;
                }
            }
        } catch (Throwable t) {
            Gdx.app.error("futREAL-V11", "PBR player sync error", t);
        }
    }

    private void renderHighDefinitionPlayers(float dt) {
        if (!hdReady || sceneManager == null || gameCamera == null) return;
        updateHdPlayerTransforms();
        sceneManager.setCamera(gameCamera);
        sceneManager.update(dt);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        sceneManager.render();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    private void redrawHudOnTop() {
        try {
            if (drawMatchHudMethod != null) drawMatchHudMethod.invoke(this);
            if (drawEliteOverlayMethod != null) drawEliteOverlayMethod.invoke(this);
            if (drawProControlsMethod != null) drawProControlsMethod.invoke(this);
            if (drawUltraOverlayMethod != null) drawUltraOverlayMethod.invoke(this);
        } catch (Throwable t) {
            Gdx.app.error("futREAL-V11", "HUD redraw error", t);
        }
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);
        super.render();
        if (hdReady && isMatch()) {
            renderHighDefinitionPlayers(dt);
            redrawHudOnTop();
        }
        drawRebuildBadge();
    }

    private void drawRebuildBadge() {
        if (rebuildShapes == null || rebuildBatch == null || rebuildFont == null) return;
        rebuildShapes.setProjectionMatrix(rebuildUi);
        rebuildBatch.setProjectionMatrix(rebuildUi);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float w = Math.min(330f, sw * 0.37f);
        rebuildShapes.begin(ShapeRenderer.ShapeType.Filled);
        rebuildShapes.setColor(0.006f, 0.012f, 0.018f, 0.94f);
        rebuildShapes.rect(10f, sh - 48f, w, 36f);
        rebuildShapes.setColor(0.62f, 1.0f, 0.10f, 1f);
        rebuildShapes.rect(10f, sh - 15f, w, 3f);
        rebuildShapes.end();

        rebuildBatch.begin();
        rebuildFont.setColor(Color.WHITE);
        rebuildFont.draw(rebuildBatch, "futREAL 11  •  REBUILD  •  " + hdStatus, 20f, sh - 25f);
        rebuildBatch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        sw = Math.max(1, width);
        sh = Math.max(1, height);
        rebuildUi.setToOrtho2D(0f, 0f, sw, sh);
        if (sceneManager != null) {
            sceneManager.setCamera(gameCamera);
            sceneManager.updateViewport(sw, sh);
        }
    }

    private void disposeHdOnly() {
        try {
            if (sceneManager != null) sceneManager.dispose();
        } catch (Throwable ignored) { }
        sceneManager = null;
        try {
            if (characterAsset != null) characterAsset.dispose();
        } catch (Throwable ignored) { }
        characterAsset = null;
        try {
            if (animationAsset != null) animationAsset.dispose();
        } catch (Throwable ignored) { }
        animationAsset = null;
        hdPlayers.clear();
    }

    @Override
    public void dispose() {
        if (rebuildShapes != null) rebuildShapes.dispose();
        if (rebuildBatch != null) rebuildBatch.dispose();
        if (rebuildFont != null) rebuildFont.dispose();
        disposeHdOnly();
        super.dispose();
    }
}
