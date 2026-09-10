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
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.scene.Scene;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * futREAL 13 - PREMIUM MATCH PASS.
 *
 * This build takes the user's v12 gameplay screenshot as the target for a
 * real in-game visual rebuild: cleaner broadcast framing, believable grass,
 * packed crowd panels, calmer shadows, cleaner player selection, stronger
 * team kits and a premium mobile HUD. It keeps the proven v10 gameplay and
 * v12 rigged CC0 footballer pipeline.
 */
public class FutRealV13Game extends FutRealV12Game {
    private static final float FIELD_W = 68f;
    private static final float FIELD_L = 105f;

    private PerspectiveCamera camera13;
    private Method render3DMethod13;
    private Method renderHdPlayersMethod13;
    private Field hdReadyField13;
    private boolean assetsReady13;

    private Field screenField13;
    private Field stadiumField13;
    private Field ownedModelsField13;
    private Field ballPosField13;
    private Field controlledField13;
    private Field blueField13;
    private Field redField13;
    private Field blueScoreField13;
    private Field redScoreField13;
    private Field matchSecondsField13;
    private Field moveField13;
    private Field fullTimeField13;
    private Field bannerTimerField13;
    private Field bannerField13;
    private Field selectRingField13;
    private Field ballShadowField13;

    private Field posField13;
    private Field bluePlayerField13;
    private Field keeperField13;
    private Field indexField13;
    private Field shadowField13;

    private final Matrix4 ui13 = new Matrix4();
    private ShapeRenderer shapes13;
    private SpriteBatch batch13;
    private BitmapFont scoreFont13;
    private BitmapFont textFont13;
    private BitmapFont tinyFont13;
    private int sw13 = 1;
    private int sh13 = 1;

    private ModelBuilder builder13;
    private Texture grass13;
    private Texture crowd13;

    @Override
    public void create() {
        super.create();
        shapes13 = new ShapeRenderer();
        batch13 = new SpriteBatch();
        scoreFont13 = new BitmapFont();
        textFont13 = new BitmapFont();
        tinyFont13 = new BitmapFont();
        scoreFont13.getData().setScale(1.38f);
        textFont13.getData().setScale(0.92f);
        tinyFont13.getData().setScale(0.70f);
        builder13 = new ModelBuilder();

        try {
            bind13();
            buildPremiumGrass13();
            hideOldCenterDiscs13();
            buildTrueCenterRing13();
            buildCrowdAndBoards13();
            softenContactShadows13();
            hideLegacySelectionDisc13();
            retintRiggedKits13();
            assetsReady13 = hdReadyField13.getBoolean(this);
        } catch (Throwable t) {
            assetsReady13 = false;
            Gdx.app.error("futREAL-V13", "Premium presentation setup failed; v12 remains available", t);
        }
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Field field13(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private Method method13(Class<?> type, String name, Class<?>... args) throws Exception {
        Method m = type.getDeclaredMethod(name, args);
        m.setAccessible(true);
        return m;
    }

    @SuppressWarnings("unchecked")
    private void bind13() throws Exception {
        Class<?> base = FutRealV7Game.class;
        screenField13 = field13(base, "screen");
        stadiumField13 = field13(base, "stadium");
        ownedModelsField13 = field13(base, "ownedModels");
        ballPosField13 = field13(base, "ballPos");
        controlledField13 = field13(base, "controlled");
        blueField13 = field13(base, "blue");
        redField13 = field13(base, "red");
        blueScoreField13 = field13(base, "blueScore");
        redScoreField13 = field13(base, "redScore");
        matchSecondsField13 = field13(base, "matchRealSeconds");
        moveField13 = field13(base, "move");
        fullTimeField13 = field13(base, "fullTime");
        bannerTimerField13 = field13(base, "bannerTimer");
        bannerField13 = field13(base, "banner");
        selectRingField13 = field13(base, "selectRing");
        ballShadowField13 = field13(base, "ballShadow");
        Field cameraField = field13(base, "camera");
        camera13 = (PerspectiveCamera)cameraField.get(this);
        render3DMethod13 = method13(base, "render3D");

        hdReadyField13 = field13(FutRealV12Game.class, "hdReady");
        renderHdPlayersMethod13 = method13(FutRealV12Game.class, "renderHdPlayers", float.class);

        Array<Object> blue = (Array<Object>)blueField13.get(this);
        if (blue == null || blue.size == 0) throw new IllegalStateException("No match players");
        Class<?> playerType = blue.first().getClass();
        posField13 = field13(playerType, "pos");
        bluePlayerField13 = field13(playerType, "blue");
        keeperField13 = field13(playerType, "keeper");
        indexField13 = field13(playerType, "index");
        shadowField13 = field13(playerType, "shadow");
    }

    @SuppressWarnings("unchecked")
    private void buildPremiumGrass13() throws Exception {
        Array<ModelInstance> stadium = (Array<ModelInstance>)stadiumField13.get(this);
        Array<Model> owned = (Array<Model>)ownedModelsField13.get(this);

        Pixmap p = new Pixmap(768, 768, Pixmap.Format.RGBA8888);
        Random rng = new Random(130013L);
        for (int y = 0; y < p.getHeight(); y++) {
            int stripeIndex = y / 84;
            float stripe = (stripeIndex & 1) == 0 ? 0.020f : -0.010f;
            for (int x = 0; x < p.getWidth(); x++) {
                float fine = (rng.nextFloat() - 0.5f) * 0.032f;
                float wave = (float)Math.sin((x + y * 0.24f) * 0.055f) * 0.0045f;
                float r = 0.085f + fine * 0.35f + stripe * 0.25f;
                float g = 0.315f + stripe + fine + wave;
                float b = 0.075f + fine * 0.32f;
                p.setColor(MathUtils.clamp(r, 0f, 1f), MathUtils.clamp(g, 0f, 1f), MathUtils.clamp(b, 0f, 1f), 1f);
                p.drawPixel(x, y);
            }
        }
        // Sparse brighter blades break the old checker-board look without
        // introducing a heavy external texture asset.
        for (int i = 0; i < 8200; i++) {
            int x = rng.nextInt(p.getWidth());
            int y = rng.nextInt(p.getHeight());
            float g = 0.34f + rng.nextFloat() * 0.08f;
            p.setColor(0.09f, g, 0.075f, 0.20f);
            p.drawPixel(x, y);
        }

        grass13 = new Texture(p);
        grass13.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        grass13.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        p.dispose();

        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;
        Material mat = new Material(
            TextureAttribute.createDiffuse(grass13),
            ColorAttribute.createSpecular(new Color(0.055f, 0.065f, 0.050f, 1f))
        );
        Model pitch = builder13.createBox(FIELD_W, 0.006f, FIELD_L, mat, attrs);
        owned.add(pitch);
        ModelInstance inst = new ModelInstance(pitch);
        // Sits above the old procedural colour stripes but below the field lines.
        inst.transform.setToTranslation(0f, 0.027f, 0f);
        stadium.add(inst);
    }

    @SuppressWarnings("unchecked")
    private void hideOldCenterDiscs13() throws Exception {
        Array<ModelInstance> stadium = (Array<ModelInstance>)stadiumField13.get(this);
        BoundingBox b = new BoundingBox();
        Vector3 d = new Vector3();
        for (ModelInstance i : stadium) {
            try {
                b.inf();
                i.calculateBoundingBox(b);
                b.getDimensions(d);
                boolean centerDisc = (Math.abs(d.x - 18.3f) < 0.45f && Math.abs(d.z - 18.3f) < 0.45f)
                    || (Math.abs(d.x - 17.8f) < 0.45f && Math.abs(d.z - 17.8f) < 0.45f);
                if (centerDisc && d.y < 0.30f) makeInvisible13(i);
            } catch (Throwable ignored) { }
        }
    }

    @SuppressWarnings("unchecked")
    private void buildTrueCenterRing13() throws Exception {
        Array<ModelInstance> stadium = (Array<ModelInstance>)stadiumField13.get(this);
        Array<Model> owned = (Array<Model>)ownedModelsField13.get(this);
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Material white = new Material(ColorAttribute.createDiffuse(new Color(0.94f, 0.97f, 0.94f, 1f)));

        builder13.begin();
        MeshPartBuilder part = builder13.part("center-ring", GL20.GL_TRIANGLES, attrs, white);
        int seg = 96;
        float outer = 9.15f;
        float inner = 8.94f;
        float y = 0.061f;
        for (int n = 0; n < seg; n++) {
            float a0 = MathUtils.PI2 * n / seg;
            float a1 = MathUtils.PI2 * (n + 1) / seg;
            Vector3 o0 = new Vector3(MathUtils.cos(a0) * outer, y, MathUtils.sin(a0) * outer);
            Vector3 o1 = new Vector3(MathUtils.cos(a1) * outer, y, MathUtils.sin(a1) * outer);
            Vector3 i1 = new Vector3(MathUtils.cos(a1) * inner, y, MathUtils.sin(a1) * inner);
            Vector3 i0 = new Vector3(MathUtils.cos(a0) * inner, y, MathUtils.sin(a0) * inner);
            part.rect(o0, o1, i1, i0, Vector3.Y);
        }
        Model ring = builder13.end();
        owned.add(ring);
        stadium.add(new ModelInstance(ring));
    }

    private Texture createCrowdTexture13() {
        Pixmap p = new Pixmap(1024, 256, Pixmap.Format.RGBA8888);
        p.setColor(0.025f, 0.038f, 0.060f, 1f);
        p.fill();
        Random r = new Random(1313L);
        Color[] palette = {
            new Color(0.18f, 0.43f, 0.68f, 1f), new Color(0.78f, 0.82f, 0.84f, 1f),
            new Color(0.08f, 0.14f, 0.22f, 1f), new Color(0.54f, 0.08f, 0.10f, 1f),
            new Color(0.12f, 0.25f, 0.39f, 1f), new Color(0.71f, 0.62f, 0.42f, 1f)
        };
        for (int row = 0; row < 18; row++) {
            int y = 18 + row * 12;
            for (int x = 6 + (row % 2) * 5; x < 1018; x += 10) {
                Color c = palette[r.nextInt(palette.length)];
                float dim = 0.62f + r.nextFloat() * 0.38f;
                p.setColor(c.r * dim, c.g * dim, c.b * dim, 1f);
                p.fillRectangle(x, y, 4, 7);
                p.setColor(0.56f * dim, 0.42f * dim, 0.32f * dim, 1f);
                p.fillRectangle(x + 1, y + 7, 2, 2);
            }
        }
        // Railings and concourse breaks.
        p.setColor(0.22f, 0.25f, 0.28f, 1f);
        p.fillRectangle(0, 82, 1024, 3);
        p.fillRectangle(0, 164, 1024, 4);
        p.setColor(0.035f, 0.045f, 0.055f, 1f);
        p.fillRectangle(0, 0, 1024, 14);
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    @SuppressWarnings("unchecked")
    private void buildCrowdAndBoards13() throws Exception {
        Array<ModelInstance> stadium = (Array<ModelInstance>)stadiumField13.get(this);
        Array<Model> owned = (Array<Model>)ownedModelsField13.get(this);
        crowd13 = createCrowdTexture13();
        long texAttrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;
        Material crowdMat = new Material(TextureAttribute.createDiffuse(crowd13));

        Model farCrowd = builder13.createBox(0.16f, 9.0f, 116f, crowdMat, texAttrs);
        owned.add(farCrowd);
        ModelInstance far = new ModelInstance(farCrowd);
        far.transform.setToTranslation(-37.35f, 5.15f, 0f);
        stadium.add(far);

        Model endCrowd = builder13.createBox(72f, 7.4f, 0.16f, crowdMat, texAttrs);
        owned.add(endCrowd);
        ModelInstance endA = new ModelInstance(endCrowd);
        endA.transform.setToTranslation(0f, 4.7f, -55.35f);
        stadium.add(endA);
        ModelInstance endB = new ModelInstance(endCrowd);
        endB.transform.setToTranslation(0f, 4.7f, 55.35f);
        stadium.add(endB);

        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Material boardDark = new Material(ColorAttribute.createDiffuse(new Color(0.010f, 0.018f, 0.024f, 1f)));
        Material lime = new Material(ColorAttribute.createDiffuse(new Color(0.62f, 1.0f, 0.10f, 1f)));
        Material cyan = new Material(ColorAttribute.createDiffuse(new Color(0.03f, 0.66f, 0.88f, 1f)));

        Model board = builder13.createBox(0.14f, 1.10f, 103f, boardDark, attrs);
        owned.add(board);
        ModelInstance bi = new ModelInstance(board);
        bi.transform.setToTranslation(-35.55f, 0.72f, 0f);
        stadium.add(bi);

        for (int z = -45, n = 0; z <= 45; z += 15, n++) {
            Model pulse = builder13.createBox(0.16f, 0.12f, 11.2f, (n & 1) == 0 ? lime : cyan, attrs);
            owned.add(pulse);
            ModelInstance pi = new ModelInstance(pulse);
            pi.transform.setToTranslation(-35.45f, 1.30f, z);
            stadium.add(pi);
        }
    }

    @SuppressWarnings("unchecked")
    private void softenContactShadows13() throws Exception {
        Array<Object> blue = (Array<Object>)blueField13.get(this);
        Array<Object> red = (Array<Object>)redField13.get(this);
        for (Object p : blue) softenShadow13((ModelInstance)shadowField13.get(p));
        for (Object p : red) softenShadow13((ModelInstance)shadowField13.get(p));
        Object ballShadow = ballShadowField13.get(this);
        if (ballShadow instanceof ModelInstance) softenShadow13((ModelInstance)ballShadow);
    }

    private void softenShadow13(ModelInstance instance) {
        if (instance == null) return;
        for (Material m : instance.materials) {
            m.set(ColorAttribute.createDiffuse(new Color(0.015f, 0.020f, 0.018f, 0.17f)));
            m.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.17f));
            m.set(new DepthTestAttribute(GL20.GL_LEQUAL, 0f, 1f, false));
        }
    }

    private void hideLegacySelectionDisc13() throws Exception {
        Object ring = selectRingField13.get(this);
        if (ring instanceof ModelInstance) makeInvisible13((ModelInstance)ring);
    }

    private void makeInvisible13(ModelInstance instance) {
        if (instance == null) return;
        for (Material m : instance.materials) {
            ColorAttribute d = m.get(ColorAttribute.class, ColorAttribute.Diffuse);
            if (d != null) d.color.a = 0f;
            m.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0f));
            m.set(new DepthTestAttribute(GL20.GL_LEQUAL, 0f, 1f, false));
        }
    }

    private void retintRiggedKits13() {
        try {
            Field hdField = field13(FutRealV12Game.class, "hdPlayers");
            Object raw = hdField.get(this);
            if (!(raw instanceof Array)) return;
            for (Object hp : (Array<?>)raw) {
                Field simF = field13(hp.getClass(), "sim");
                Field sceneF = field13(hp.getClass(), "scene");
                Object sim = simF.get(hp);
                Scene scene = (Scene)sceneF.get(hp);
                boolean blue = bluePlayerField13.getBoolean(sim);
                boolean keeper = keeperField13.getBoolean(sim);
                retintNodes13(scene.modelInstance.nodes, blue, keeper);
            }
        } catch (Throwable t) {
            Gdx.app.error("futREAL-V13", "Kit retint skipped", t);
        }
    }

    private void retintNodes13(Array<Node> roots, boolean blue, boolean keeper) {
        for (Node n : roots) retintNode13(n, blue, keeper);
    }

    private void retintNode13(Node node, boolean blue, boolean keeper) {
        for (NodePart part : node.parts) {
            Material copy = new Material(part.material);
            String id = copy.id == null ? "" : copy.id.toLowerCase();
            Color c = null;
            if (id.equals("shirt")) {
                c = keeper ? new Color(0.95f, 0.72f, 0.06f, 1f)
                    : blue ? new Color(0.32f, 0.68f, 0.94f, 1f) : new Color(0.72f, 0.035f, 0.055f, 1f);
            } else if (id.contains("shirt2")) {
                c = keeper ? new Color(0.16f, 0.16f, 0.18f, 1f)
                    : blue ? new Color(0.94f, 0.96f, 0.98f, 1f) : new Color(0.055f, 0.060f, 0.070f, 1f);
            } else if (id.contains("pants")) {
                c = keeper ? new Color(0.09f, 0.10f, 0.12f, 1f)
                    : blue ? new Color(0.93f, 0.95f, 0.98f, 1f) : new Color(0.055f, 0.060f, 0.070f, 1f);
            } else if (id.contains("socks")) {
                c = keeper ? new Color(0.84f, 0.64f, 0.04f, 1f)
                    : blue ? new Color(0.36f, 0.71f, 0.96f, 1f) : new Color(0.10f, 0.105f, 0.12f, 1f);
            } else if (id.contains("shoes")) {
                c = new Color(0.025f, 0.028f, 0.032f, 1f);
            }
            if (c != null) copy.set(PBRColorAttribute.createBaseColorFactor(c));
            part.material = copy;
        }
        for (Node child : node.getChildren()) retintNode13(child, blue, keeper);
    }

    private boolean isMatch13() {
        try {
            Object s = screenField13.get(this);
            return s != null && "MATCH".equals(s.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void premiumCamera13() {
        try {
            Vector3 ball = (Vector3)ballPosField13.get(this);
            Object controlled = controlledField13.get(this);
            Vector3 cp = controlled == null ? ball : (Vector3)posField13.get(controlled);
            float focusX = ball.x * 0.46f + cp.x * 0.54f;
            float focusZ = ball.z * 0.58f + cp.z * 0.42f;

            camera13.fieldOfView = 35.5f;
            camera13.position.set(40.4f, 12.4f, MathUtils.clamp(focusZ + 3.2f, -44.5f, 44.5f));
            camera13.up.set(Vector3.Y);
            camera13.lookAt(MathUtils.clamp(focusX, -19f, 19f), 0.82f, focusZ - 1.6f);
            camera13.near = 0.07f;
            camera13.far = 330f;
            camera13.update();
        } catch (Throwable ignored) { }
    }

    @Override
    public void render() {
        if (!assetsReady13 || hdReadyField13 == null) {
            super.render();
            return;
        }

        // Stop v12 from doing its extra presentation redraw. Its parent still
        // processes all gameplay/input. Then v13 performs the final picture.
        try { hdReadyField13.setBoolean(this, false); } catch (Throwable ignored) { }
        super.render();
        try { hdReadyField13.setBoolean(this, true); } catch (Throwable ignored) { }

        if (!isMatch13()) return;
        try {
            float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);
            premiumCamera13();
            render3DMethod13.invoke(this);
            renderHdPlayersMethod13.invoke(this, dt);
            drawPremiumHud13();
        } catch (Throwable t) {
            Gdx.app.error("futREAL-V13", "Premium frame failed", t);
        }
    }

    @SuppressWarnings("unchecked")
    private void drawPremiumHud13() {
        shapes13.setProjectionMatrix(ui13);
        batch13.setProjectionMatrix(ui13);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        int blueScore = 0, redScore = 0;
        float seconds = 0f;
        boolean fullTime = false;
        float bannerTimer = 0f;
        String banner = "";
        Vector2 move = new Vector2();
        Object controlled = null;
        try {
            blueScore = blueScoreField13.getInt(this);
            redScore = redScoreField13.getInt(this);
            seconds = matchSecondsField13.getFloat(this);
            fullTime = fullTimeField13.getBoolean(this);
            bannerTimer = bannerTimerField13.getFloat(this);
            banner = String.valueOf(bannerField13.get(this));
            move.set((Vector2)moveField13.get(this));
            controlled = controlledField13.get(this);
        } catch (Throwable ignored) { }

        float brandW = Math.min(190f, sw13 * 0.15f);
        float brandH = Math.min(62f, sh13 * 0.09f);
        float topY = sh13 - brandH - 14f;

        float scoreW = Math.min(470f, Math.max(325f, sw13 * 0.34f));
        float scoreH = Math.min(72f, Math.max(54f, sh13 * 0.092f));
        float scoreX = (sw13 - scoreW) * 0.5f;
        float scoreY = sh13 - scoreH - 12f;

        float joyX = Math.max(105f, sw13 * 0.105f);
        float joyY = Math.max(86f, sh13 * 0.165f);
        float joyR = Math.min(76f, sh13 * 0.118f);

        shapes13.begin(ShapeRenderer.ShapeType.Filled);
        // Brand panel.
        shapes13.setColor(0.008f, 0.013f, 0.019f, 0.92f);
        shapes13.rect(14f, topY, brandW, brandH);
        shapes13.setColor(0.62f, 1f, 0.10f, 1f);
        shapes13.rect(14f, topY + brandH - 3f, brandW, 3f);

        // Broadcast scoreboard.
        shapes13.setColor(0.006f, 0.010f, 0.016f, 0.93f);
        shapes13.rect(scoreX, scoreY, scoreW, scoreH);
        shapes13.setColor(0.62f, 1f, 0.10f, 1f);
        shapes13.rect(scoreX, scoreY, 5f, scoreH);
        shapes13.rect(scoreX + scoreW - 5f, scoreY, 5f, scoreH);
        shapes13.setColor(0.20f, 0.63f, 0.92f, 1f);
        shapes13.rect(scoreX + 25f, scoreY + 14f, 22f, scoreH - 28f);
        shapes13.setColor(0.72f, 0.04f, 0.06f, 1f);
        shapes13.rect(scoreX + scoreW - 47f, scoreY + 14f, 22f, scoreH - 28f);

        // Joystick glass.
        shapes13.setColor(0.010f, 0.015f, 0.020f, 0.42f);
        shapes13.circle(joyX, joyY, joyR, 48);
        shapes13.setColor(0.66f, 0.70f, 0.73f, 0.73f);
        shapes13.circle(joyX + move.x * joyR * 0.47f, joyY + move.y * joyR * 0.47f, joyR * 0.31f, 36);

        // Action button dark glass cores. Keep the proven touch coordinates.
        filledButton13(sw13 * 0.915f, sh13 * 0.155f, sh13 * 0.062f);
        filledButton13(sw13 * 0.785f, sh13 * 0.125f, sh13 * 0.050f);
        filledButton13(sw13 * 0.785f, sh13 * 0.315f, sh13 * 0.050f);
        filledButton13(sw13 * 0.615f, sh13 * 0.120f, sh13 * 0.042f);
        filledButton13(sw13 * 0.615f, sh13 * 0.315f, sh13 * 0.042f);
        filledButton13(sw13 * 0.925f, sh13 * 0.735f, sh13 * 0.039f);

        // Radar glass.
        float rw = Math.min(220f, sw13 * 0.145f);
        float rh = rw * 0.61f;
        float rx = (sw13 - rw) * 0.5f;
        float ry = 10f;
        shapes13.setColor(0.005f, 0.012f, 0.014f, 0.78f);
        shapes13.rect(rx - 5f, ry - 5f, rw + 10f, rh + 10f);
        shapes13.setColor(0.035f, 0.16f, 0.075f, 0.72f);
        shapes13.rect(rx, ry, rw, rh);
        shapes13.end();

        // Rings and pitch/radar outlines.
        Gdx.gl.glLineWidth(3.0f);
        shapes13.begin(ShapeRenderer.ShapeType.Line);
        shapes13.setColor(0.74f, 0.78f, 0.80f, 0.62f);
        shapes13.circle(joyX, joyY, joyR, 48);
        ring13(sw13 * 0.915f, sh13 * 0.155f, sh13 * 0.062f, new Color(1.0f, 0.20f, 0.17f, 0.95f));
        ring13(sw13 * 0.785f, sh13 * 0.125f, sh13 * 0.050f, new Color(0.10f, 0.62f, 1.0f, 0.95f));
        ring13(sw13 * 0.785f, sh13 * 0.315f, sh13 * 0.050f, new Color(0.50f, 0.94f, 0.20f, 0.95f));
        ring13(sw13 * 0.615f, sh13 * 0.120f, sh13 * 0.042f, new Color(0.10f, 0.62f, 1.0f, 0.86f));
        ring13(sw13 * 0.615f, sh13 * 0.315f, sh13 * 0.042f, new Color(0.95f, 0.68f, 0.13f, 0.90f));
        ring13(sw13 * 0.925f, sh13 * 0.735f, sh13 * 0.039f, new Color(0.68f, 0.72f, 0.76f, 0.80f));
        shapes13.setColor(0.85f, 0.88f, 0.86f, 0.58f);
        shapes13.rect(rx, ry, rw, rh);
        shapes13.line(rx + rw * 0.5f, ry, rx + rw * 0.5f, ry + rh);
        shapes13.circle(rx + rw * 0.5f, ry + rh * 0.5f, rh * 0.19f, 24);
        shapes13.end();
        Gdx.gl.glLineWidth(1f);

        try {
            drawRadarDots13((Array<Object>)blueField13.get(this), rx, ry, rw, rh, new Color(0.20f, 0.68f, 1f, 1f));
            drawRadarDots13((Array<Object>)redField13.get(this), rx, ry, rw, rh, new Color(1f, 0.20f, 0.22f, 1f));
        } catch (Throwable ignored) { }

        // Small triangle above the controlled footballer replaces the solid
        // yellow disc that looked like a prototype marker.
        if (controlled != null) drawPlayerIndicator13(controlled);

        int minute = Math.min(90, (int)(seconds * 0.60f));
        int clockM = (int)(seconds / 60f);
        int clockS = ((int)seconds) % 60;
        batch13.begin();
        scoreFont13.setColor(Color.WHITE);
        scoreFont13.draw(batch13, "AUR", scoreX + 60f, scoreY + scoreH * 0.66f);
        scoreFont13.draw(batch13, blueScore + "  -  " + redScore, scoreX + scoreW * 0.43f, scoreY + scoreH * 0.66f);
        scoreFont13.draw(batch13, "VIL", scoreX + scoreW - 105f, scoreY + scoreH * 0.66f);

        textFont13.setColor(Color.WHITE);
        textFont13.draw(batch13, "futREAL", 28f, topY + brandH * 0.68f);
        tinyFont13.setColor(new Color(0.62f, 1f, 0.10f, 1f));
        tinyFont13.draw(batch13, "13  PREMIUM", 92f, topY + brandH * 0.68f);
        tinyFont13.setColor(new Color(0.86f, 0.90f, 0.92f, 1f));
        tinyFont13.draw(batch13, String.format("%02d:%02d  |  %02d'", clockM, clockS, minute), 28f, topY + 15f);

        textFont13.setColor(Color.WHITE);
        centered13("CHUTE", sw13 * 0.915f, sh13 * 0.155f);
        centered13("PASSE", sw13 * 0.785f, sh13 * 0.125f);
        centered13("SPRINT", sw13 * 0.785f, sh13 * 0.315f);
        centered13("ENFIADA", sw13 * 0.615f, sh13 * 0.120f);
        centered13("DRIBLE", sw13 * 0.615f, sh13 * 0.315f);
        centered13("TROCA", sw13 * 0.925f, sh13 * 0.735f);

        if ((bannerTimer > 0f || fullTime) && banner != null && !banner.isEmpty()) {
            scoreFont13.setColor(Color.WHITE);
            scoreFont13.draw(batch13, banner, sw13 * 0.43f, sh13 * 0.73f);
        }
        batch13.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void filledButton13(float x, float y, float r) {
        shapes13.setColor(0.010f, 0.017f, 0.023f, 0.69f);
        shapes13.circle(x, y, Math.max(24f, r), 44);
    }

    private void ring13(float x, float y, float r, Color c) {
        shapes13.setColor(c);
        shapes13.circle(x, y, Math.max(24f, r), 44);
    }

    private void centered13(String text, float x, float y) {
        float approx = text.length() * 4.5f;
        textFont13.draw(batch13, text, x - approx, y + 5f);
    }

    private void drawPlayerIndicator13(Object controlled) {
        try {
            Vector3 p = ((Vector3)posField13.get(controlled)).cpy().add(0f, 2.65f, 0f);
            camera13.project(p);
            float r = Math.max(7f, sh13 * 0.010f);
            shapes13.begin(ShapeRenderer.ShapeType.Filled);
            shapes13.setColor(0.62f, 1f, 0.10f, 0.96f);
            shapes13.triangle(p.x, p.y - r, p.x - r * 0.72f, p.y + r * 0.35f, p.x + r * 0.72f, p.y + r * 0.35f);
            shapes13.end();
        } catch (Throwable ignored) { }
    }

    private void drawRadarDots13(Array<Object> team, float rx, float ry, float rw, float rh, Color c) {
        shapes13.begin(ShapeRenderer.ShapeType.Filled);
        shapes13.setColor(c);
        for (Object player : team) {
            try {
                Vector3 p = (Vector3)posField13.get(player);
                float px = rx + ((p.z / FIELD_L) + 0.5f) * rw;
                float py = ry + (0.5f - p.x / FIELD_W) * rh;
                shapes13.circle(px, py, Math.max(2.2f, sh13 * 0.0038f), 12);
            } catch (Throwable ignored) { }
        }
        shapes13.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        sw13 = Math.max(1, width);
        sh13 = Math.max(1, height);
        ui13.setToOrtho2D(0f, 0f, sw13, sh13);
    }

    @Override
    public void dispose() {
        if (shapes13 != null) shapes13.dispose();
        if (batch13 != null) batch13.dispose();
        if (scoreFont13 != null) scoreFont13.dispose();
        if (textFont13 != null) textFont13.dispose();
        if (tinyFont13 != null) tinyFont13.dispose();
        if (grass13 != null) grass13.dispose();
        if (crowd13 != null) crowd13.dispose();
        super.dispose();
    }
}
