package com.nox.futreal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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
import com.badlogic.gdx.utils.Array;

import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.SceneManager;

import java.lang.reflect.Field;

/**
 * futREAL - 10 day rebuild / DAY 10 FINAL.
 *
 * Final pass keeps the working Day 1-3 gameplay/animation stack and focuses on
 * presentation that is cheap enough for Android: a fuller stadium shell, upper
 * tiers, roof/light bars, pitch-side LED boards and brighter balanced lighting.
 * All geometry/materials are original procedural futREAL assets.
 */
public class FutRealV23Day10FinalGame extends FutRealV16Day3Game {
    private ModelBuilder finalBuilder;
    private boolean finalReady;
    private ShapeRenderer finalShapes;
    private SpriteBatch finalBatch;
    private BitmapFont finalFont;
    private final Matrix4 finalUi = new Matrix4();

    @Override
    public void create() {
        super.create();
        finalBuilder = new ModelBuilder();
        finalShapes = new ShapeRenderer();
        finalBatch = new SpriteBatch();
        finalFont = new BitmapFont();
        finalFont.getData().setScale(0.72f);
        try {
            buildFinalStadium();
            tuneLighting();
            tunePlayerScale();
            finalReady = true;
        } catch (Throwable t) {
            finalReady = false;
            Gdx.app.error("futREAL-DAY10", "Final polish failed; Day 3 remains playable", t);
        }
    }

    private Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    @SuppressWarnings("unchecked")
    private void buildFinalStadium() throws Exception {
        Field stadiumField = field(FutRealV7Game.class, "stadium");
        Field ownedField = field(FutRealV7Game.class, "ownedModels");
        Array<ModelInstance> stadium = (Array<ModelInstance>) stadiumField.get(this);
        Array<Model> owned = (Array<Model>) ownedField.get(this);
        if (stadium == null || owned == null) return;

        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        Color shell = new Color(0.020f, 0.038f, 0.060f, 1f);
        Color upper = new Color(0.040f, 0.070f, 0.105f, 1f);
        Color crowdA = new Color(0.075f, 0.135f, 0.185f, 1f);
        Color crowdB = new Color(0.125f, 0.105f, 0.155f, 1f);
        Color crowdC = new Color(0.075f, 0.175f, 0.190f, 1f);
        Color cyan = new Color(0.02f, 0.62f, 0.92f, 1f);
        Color lime = new Color(0.56f, 0.96f, 0.12f, 1f);
        Color warm = new Color(0.96f, 0.93f, 0.78f, 1f);

        // Far-side stadium shell removes the empty black void behind play.
        addBox(owned, stadium, -52.5f, 8.0f, 0f, 6.0f, 16.0f, 128f, shell, attrs);
        addBox(owned, stadium, -48.8f, 14.1f, 0f, 13.5f, 0.65f, 128f, upper, attrs);

        // Layered upper seating gives depth from the broadcast camera.
        Color[] crowd = {crowdA, crowdB, crowdC, crowdA};
        for (int row = 0; row < 7; row++) {
            float x = -42.6f - row * 1.20f;
            float y = 5.0f + row * 1.18f;
            addBox(owned, stadium, x, y, 0f, 1.95f, 0.72f, 119f,
                crowd[row % crowd.length], attrs);
        }

        // End-corner structures close the view when the camera tracks toward goals.
        addBox(owned, stadium, -45.5f, 7.0f, -61.8f, 16f, 14f, 6f, shell, attrs);
        addBox(owned, stadium, -45.5f, 7.0f,  61.8f, 16f, 14f, 6f, shell, attrs);

        // Roof lip and light bars.
        addBox(owned, stadium, -45.8f, 15.0f, 0f, 15.5f, 0.52f, 124f, new Color(0.025f, 0.032f, 0.045f, 1f), attrs);
        for (int z = -50; z <= 50; z += 20) {
            addBox(owned, stadium, -39.0f, 13.2f, z, 0.30f, 0.32f, 12.5f, warm, attrs);
        }

        // Original futREAL LED perimeter boards: brighter but still thin enough
        // not to obstruct the field.
        for (int z = -48; z <= 48; z += 12) {
            addBox(owned, stadium, -35.25f, 0.88f, z, 0.14f, 0.34f, 10.8f,
                ((z / 12) & 1) == 0 ? cyan : lime, attrs);
        }

        // Small tunnel/bench mass on the far sideline for less empty space.
        addBox(owned, stadium, -37.5f, 1.15f, 19f, 3.2f, 2.3f, 15f, upper, attrs);
        addBox(owned, stadium, -37.5f, 1.15f, -19f, 3.2f, 2.3f, 15f, upper, attrs);
    }

    private void addBox(Array<Model> owned, Array<ModelInstance> stadium,
                        float x, float y, float z, float w, float h, float d,
                        Color color, long attrs) {
        Model model = finalBuilder.createBox(w, h, d,
            new Material(ColorAttribute.createDiffuse(color)), attrs);
        owned.add(model);
        ModelInstance instance = new ModelInstance(model);
        instance.transform.setToTranslation(x, y, z);
        stadium.add(instance);
    }

    private void tuneLighting() {
        try {
            Field environmentField = field(FutRealV7Game.class, "environment");
            Environment env = (Environment) environmentField.get(this);
            if (env != null) {
                env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.56f, 0.59f, 0.66f, 1f));
                env.add(new DirectionalLight().set(0.28f, 0.33f, 0.46f, 0.50f, -0.78f, 0.28f));
            }
        } catch (Throwable ignored) { }

        try {
            Field managerField = field(FutRealV12Game.class, "sceneManager");
            SceneManager manager = (SceneManager) managerField.get(this);
            if (manager != null) manager.setAmbientLight(0.62f);

            Field keyField = field(FutRealV12Game.class, "keyLight");
            DirectionalLightEx key = (DirectionalLightEx) keyField.get(this);
            if (key != null) {
                key.color.set(1.00f, 0.98f, 0.93f, 1f);
                key.direction.set(-0.34f, -0.92f, -0.18f).nor();
            }
        } catch (Throwable ignored) { }
    }

    private void tunePlayerScale() {
        try {
            Field scaleField = field(FutRealV12Game.class, "modelScale");
            float scale = scaleField.getFloat(this);
            if (scale > 0f) scaleField.setFloat(this, scale * 1.018f);
        } catch (Throwable ignored) { }
    }

    private boolean isMatch() {
        try {
            Field f = field(FutRealV7Game.class, "screen");
            Object screen = f.get(this);
            return screen != null && "MATCH".equals(screen.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void render() {
        super.render();
        if (!finalReady || !isMatch()) return;
        drawFinalBadge();
    }

    private void drawFinalBadge() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        finalUi.setToOrtho2D(0f, 0f, w, h);
        finalShapes.setProjectionMatrix(finalUi);
        finalBatch.setProjectionMatrix(finalUi);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Covers the inherited Day-3 badge with the final build identity.
        finalShapes.begin(ShapeRenderer.ShapeType.Filled);
        finalShapes.setColor(0.003f, 0.009f, 0.014f, 0.98f);
        finalShapes.rect(8f, h - 46f, 174f, 35f);
        finalShapes.setColor(0.62f, 1f, 0.10f, 1f);
        finalShapes.rect(8f, h - 14f, 174f, 3f);
        finalShapes.end();

        finalBatch.begin();
        finalFont.setColor(Color.WHITE);
        finalFont.draw(finalBatch, "futREAL  DAY 10 FINAL", 18f, h - 23f);
        finalBatch.end();
    }

    public boolean isFinalReady() {
        return finalReady;
    }

    @Override
    public void dispose() {
        if (finalShapes != null) finalShapes.dispose();
        if (finalBatch != null) finalBatch.dispose();
        if (finalFont != null) finalFont.dispose();
        super.dispose();
    }
}
