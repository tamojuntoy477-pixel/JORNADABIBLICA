package com.nox.futreal;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
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

public class FutRealV7Game extends ApplicationAdapter {
    private static final float FIELD_W = 68f;
    private static final float FIELD_L = 105f;
    private static final float HALF_W = FIELD_W * 0.5f;
    private static final float HALF_L = FIELD_L * 0.5f;
    private static final float GOAL_HALF = 7.32f * 0.5f;

    private enum Screen { HOME, CAREER, MATCH }
    private Screen screen = Screen.HOME;

    private ModelBatch modelBatch;
    private ModelBuilder builder;
    private Environment environment;
    private PerspectiveCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private BitmapFont smallFont;
    private final Matrix4 uiMatrix = new Matrix4();
    private int screenW;
    private int screenH;

    private final Array<Model> ownedModels = new Array<>();
    private final Array<ModelInstance> stadium = new Array<>();
    private final Array<Player> blue = new Array<>();
    private final Array<Player> red = new Array<>();

    private Model blueTorsoModel, redTorsoModel, blueShortModel, redShortModel;
    private Model skinHeadModel, skinArmModel, legModel, bootModel, shadowModel;
    private Model ballModel, ballShadowModel, selectRingModel;
    private ModelInstance ball, ballShadow, selectRing;

    private final Vector3 ballPos = new Vector3();
    private final Vector3 ballVel = new Vector3();
    private Player controlled;
    private Player lastTouch;
    private final Vector2 move = new Vector2();
    private boolean sprint;
    private float actionCooldown;
    private float switchCooldown;
    private float kickoffDelay;
    private float matchRealSeconds;
    private int blueScore;
    private int redScore;
    private boolean halfTimeShown;
    private boolean fullTime;
    private float bannerTimer;
    private String banner = "";
    private boolean careerUpdated;

    private Preferences save;
    private int careerMatches;
    private int careerGoals;
    private int careerAssists;
    private int careerXp;
    private int careerOverall;

    private float menuPulse;

    @Override
    public void create() {
        modelBatch = new ModelBatch();
        builder = new ModelBuilder();
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.65f);
        smallFont = new BitmapFont();
        smallFont.getData().setScale(1.05f);

        save = Gdx.app.getPreferences("futreal_v7_career");
        loadCareer();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.48f, 0.52f, 0.60f, 1f));
        environment.add(new DirectionalLight().set(1f, 0.97f, 0.88f, -0.45f, -1f, -0.30f));
        environment.add(new DirectionalLight().set(0.22f, 0.30f, 0.48f, 0.65f, -0.42f, 0.55f));

        camera = new PerspectiveCamera(49f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 320f;

        buildSharedModels();
        buildStadium();
        buildTeams();
        buildBall();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        resetMatch();
        screen = Screen.HOME;
    }

    private Material material(Color c) {
        return new Material(ColorAttribute.createDiffuse(c));
    }

    private Model keep(Model m) {
        ownedModels.add(m);
        return m;
    }

    private void buildSharedModels() {
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        blueTorsoModel = keep(builder.createBox(0.86f, 1.02f, 0.45f, material(new Color(0.05f, 0.58f, 1f, 1)), attrs));
        redTorsoModel = keep(builder.createBox(0.86f, 1.02f, 0.45f, material(new Color(0.92f, 0.08f, 0.16f, 1)), attrs));
        blueShortModel = keep(builder.createBox(0.78f, 0.38f, 0.42f, material(new Color(0.02f, 0.12f, 0.24f, 1)), attrs));
        redShortModel = keep(builder.createBox(0.78f, 0.38f, 0.42f, material(new Color(0.20f, 0.02f, 0.04f, 1)), attrs));
        skinHeadModel = keep(builder.createSphere(0.55f, 0.61f, 0.55f, 12, 8, material(new Color(0.70f, 0.46f, 0.31f, 1)), attrs));
        skinArmModel = keep(builder.createCylinder(0.18f, 0.92f, 0.18f, 8, material(new Color(0.70f, 0.46f, 0.31f, 1)), attrs));
        legModel = keep(builder.createCylinder(0.19f, 0.92f, 0.19f, 8, material(new Color(0.83f, 0.84f, 0.87f, 1)), attrs));
        bootModel = keep(builder.createBox(0.24f, 0.16f, 0.45f, material(new Color(0.025f, 0.025f, 0.035f, 1)), attrs));
        shadowModel = keep(builder.createCylinder(0.90f, 0.025f, 0.58f, 16, material(new Color(0.018f, 0.025f, 0.025f, 1)), attrs));
        selectRingModel = keep(builder.createCylinder(1.45f, 0.035f, 1.45f, 28, material(new Color(0.72f, 1f, 0.12f, 1)), attrs));
        selectRing = new ModelInstance(selectRingModel);
    }

    private void addBox(float x, float y, float z, float w, float h, float d, Color c) {
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Model m = keep(builder.createBox(w, h, d, material(c), attrs));
        ModelInstance i = new ModelInstance(m);
        i.transform.setToTranslation(x, y, z);
        stadium.add(i);
    }

    private void addCylinder(float x, float y, float z, float w, float h, float d, Color c) {
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Model m = keep(builder.createCylinder(w, h, d, 20, material(c), attrs));
        ModelInstance i = new ModelInstance(m);
        i.transform.setToTranslation(x, y, z);
        stadium.add(i);
    }

    private void buildStadium() {
        Color grassDark = new Color(0.018f, 0.24f, 0.065f, 1);
        Color grassLight = new Color(0.025f, 0.34f, 0.085f, 1);
        addBox(0, -0.34f, 0, 74f, 0.55f, 111f, grassDark);
        for (int n = 0; n < 14; n++) {
            float z = -48.75f + n * 7.5f;
            addBox(0, -0.035f, z, FIELD_W, 0.05f, 7.5f, (n % 2 == 0) ? grassLight : grassDark);
        }

        Color line = new Color(0.92f, 0.96f, 0.92f, 1);
        addBox(0, 0.03f, 0, FIELD_W, 0.045f, 0.12f, line);
        addBox(-HALF_W, 0.03f, 0, 0.12f, 0.045f, FIELD_L, line);
        addBox(HALF_W, 0.03f, 0, 0.12f, 0.045f, FIELD_L, line);
        addBox(0, 0.03f, -HALF_L, FIELD_W, 0.045f, 0.12f, line);
        addBox(0, 0.03f, HALF_L, FIELD_W, 0.045f, 0.12f, line);

        // Penalty boxes and six-yard boxes.
        for (int s : new int[]{-1, 1}) {
            float goalZ = s * HALF_L;
            float boxZ = s * (HALF_L - 8.25f);
            addBox(-20.16f, 0.035f, boxZ, 0.12f, 0.05f, 16.5f, line);
            addBox(20.16f, 0.035f, boxZ, 0.12f, 0.05f, 16.5f, line);
            addBox(0, 0.035f, s * (HALF_L - 16.5f), 40.32f, 0.05f, 0.12f, line);
            addBox(-9.16f, 0.035f, s * (HALF_L - 2.75f), 0.12f, 0.05f, 5.5f, line);
            addBox(9.16f, 0.035f, s * (HALF_L - 2.75f), 0.12f, 0.05f, 5.5f, line);
            addBox(0, 0.035f, s * (HALF_L - 5.5f), 18.32f, 0.05f, 0.12f, line);

            Color white = Color.WHITE;
            addBox(-GOAL_HALF, 1.22f, goalZ + s * 0.55f, 0.18f, 2.44f, 0.18f, white);
            addBox(GOAL_HALF, 1.22f, goalZ + s * 0.55f, 0.18f, 2.44f, 0.18f, white);
            addBox(0, 2.44f, goalZ + s * 0.55f, GOAL_HALF * 2f + 0.2f, 0.18f, 0.18f, white);
            addBox(-GOAL_HALF, 1.15f, goalZ + s * 2.2f, 0.12f, 2.3f, 3.3f, new Color(0.65f, 0.68f, 0.72f, 1));
            addBox(GOAL_HALF, 1.15f, goalZ + s * 2.2f, 0.12f, 2.3f, 3.3f, new Color(0.65f, 0.68f, 0.72f, 1));
        }

        // Centre circle marker.
        addCylinder(0, 0.035f, 0, 18.3f, 0.035f, 18.3f, new Color(0.80f, 0.84f, 0.80f, 1));
        addCylinder(0, 0.055f, 0, 17.8f, 0.045f, 17.8f, grassDark);
        addCylinder(0, 0.07f, 0, 0.32f, 0.04f, 0.32f, line);

        // Stadium bowl.
        Color standDark = new Color(0.025f, 0.032f, 0.048f, 1);
        Color standMid = new Color(0.055f, 0.065f, 0.085f, 1);
        Color cyan = new Color(0.02f, 0.45f, 0.72f, 1);
        Color lime = new Color(0.53f, 0.86f, 0.08f, 1);
        addBox(-46f, 4.5f, 0, 17f, 9f, 126f, standDark);
        addBox(46f, 4.5f, 0, 17f, 9f, 126f, standDark);
        addBox(0, 4.5f, -65f, 82f, 9f, 19f, standDark);
        addBox(0, 4.5f, 65f, 82f, 9f, 19f, standDark);
        addBox(-43f, 9.2f, 0, 10f, 1.0f, 122f, standMid);
        addBox(43f, 9.2f, 0, 10f, 1.0f, 122f, standMid);

        for (int side : new int[]{-1, 1}) {
            for (int z = -52, idx = 0; z <= 52; z += 8, idx++) {
                addBox(side * 39.5f, 5.0f + (idx % 2) * 0.25f, z, 1.2f, 5.0f, 6.4f, (idx % 3 == 0) ? lime : cyan);
            }
        }

        // Advertising boards.
        Color board = new Color(0.02f, 0.09f, 0.14f, 1);
        Color accent = new Color(0.65f, 1f, 0.10f, 1);
        for (int z = -48; z <= 48; z += 12) {
            addBox(-35.8f, 0.65f, z, 0.22f, 1.15f, 10.5f, board);
            addBox(35.8f, 0.65f, z, 0.22f, 1.15f, 10.5f, accent);
        }

        // Floodlight towers.
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                addBox(sx * 48f, 8f, sz * 58f, 0.65f, 16f, 0.65f, new Color(0.18f, 0.20f, 0.23f, 1));
                addBox(sx * 48f, 15.5f, sz * 58f, 5.4f, 1.2f, 1.1f, new Color(0.95f, 0.95f, 0.82f, 1));
            }
        }
    }

    private Player createPlayer(boolean isBlue, int index, float x, float z, boolean keeper) {
        Model torsoModel = isBlue ? blueTorsoModel : redTorsoModel;
        Model shortsModel = isBlue ? blueShortModel : redShortModel;
        Player p = new Player(isBlue, index, keeper, x, z);
        p.torso = new ModelInstance(torsoModel);
        p.shorts = new ModelInstance(shortsModel);
        p.head = new ModelInstance(skinHeadModel);
        p.armL = new ModelInstance(skinArmModel);
        p.armR = new ModelInstance(skinArmModel);
        p.legL = new ModelInstance(legModel);
        p.legR = new ModelInstance(legModel);
        p.bootL = new ModelInstance(bootModel);
        p.bootR = new ModelInstance(bootModel);
        p.shadow = new ModelInstance(shadowModel);
        return p;
    }

    private void buildTeams() {
        float[][] blueFormation = {
            {0, 47}, {-24, 33}, {-8, 36}, {8, 36}, {24, 33},
            {-17, 17}, {0, 22}, {17, 17}, {-19, -2}, {0, -10}, {19, -2}
        };
        float[][] redFormation = {
            {0, -47}, {-24, -33}, {-8, -36}, {8, -36}, {24, -33},
            {-17, -17}, {0, -22}, {17, -17}, {-19, 2}, {0, 10}, {19, 2}
        };
        for (int i = 0; i < 11; i++) blue.add(createPlayer(true, i, blueFormation[i][0], blueFormation[i][1], i == 0));
        for (int i = 0; i < 11; i++) red.add(createPlayer(false, i, redFormation[i][0], redFormation[i][1], i == 0));
        controlled = blue.get(9);
    }

    private void buildBall() {
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        ballModel = keep(builder.createSphere(0.72f, 0.72f, 0.72f, 20, 14, material(new Color(0.96f, 0.96f, 0.92f, 1)), attrs));
        ballShadowModel = keep(builder.createCylinder(0.82f, 0.02f, 0.55f, 20, material(new Color(0.012f, 0.017f, 0.018f, 1)), attrs));
        ball = new ModelInstance(ballModel);
        ballShadow = new ModelInstance(ballShadowModel);
    }

    private void loadCareer() {
        careerMatches = save.getInteger("matches", 0);
        careerGoals = save.getInteger("goals", 0);
        careerAssists = save.getInteger("assists", 0);
        careerXp = save.getInteger("xp", 0);
        careerOverall = Math.min(99, 67 + careerXp / 300);
    }

    private void saveCareer() {
        careerOverall = Math.min(99, 67 + careerXp / 300);
        save.putInteger("matches", careerMatches);
        save.putInteger("goals", careerGoals);
        save.putInteger("assists", careerAssists);
        save.putInteger("xp", careerXp);
        save.flush();
    }

    private void resetMatch() {
        for (Player p : blue) p.reset();
        for (Player p : red) p.reset();
        ballPos.set(0, 0.42f, 0);
        ballVel.setZero();
        blueScore = 0;
        redScore = 0;
        matchRealSeconds = 0;
        kickoffDelay = 1.2f;
        halfTimeShown = false;
        fullTime = false;
        bannerTimer = 1.3f;
        banner = "KICK OFF";
        careerUpdated = false;
        lastTouch = null;
        controlled = blue.get(9);
        syncTransforms(0f);
    }

    private void startMatch() {
        resetMatch();
        screen = Screen.MATCH;
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 24f);
        menuPulse += dt;
        actionCooldown = Math.max(0f, actionCooldown - dt);
        switchCooldown = Math.max(0f, switchCooldown - dt);
        bannerTimer = Math.max(0f, bannerTimer - dt);

        if (screen == Screen.MATCH) {
            updateMatchControls();
            updateGame(dt);
            updateCamera(dt);
            render3D();
            drawMatchHud();
        } else {
            updateMenuInput();
            drawMenu();
        }
    }

    private void render3D() {
        Gdx.gl.glViewport(0, 0, screenW, screenH);
        Gdx.gl.glClearColor(0.008f, 0.015f, 0.027f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(camera);
        for (ModelInstance i : stadium) modelBatch.render(i, environment);
        for (Player p : blue) renderPlayer(p);
        for (Player p : red) renderPlayer(p);
        modelBatch.render(ballShadow, environment);
        modelBatch.render(ball, environment);
        modelBatch.render(selectRing, environment);
        modelBatch.end();
    }

    private void renderPlayer(Player p) {
        modelBatch.render(p.shadow, environment);
        modelBatch.render(p.torso, environment);
        modelBatch.render(p.shorts, environment);
        modelBatch.render(p.head, environment);
        modelBatch.render(p.armL, environment);
        modelBatch.render(p.armR, environment);
        modelBatch.render(p.legL, environment);
        modelBatch.render(p.legR, environment);
        modelBatch.render(p.bootL, environment);
        modelBatch.render(p.bootR, environment);
    }

    private void updateMenuInput() {
        if (!Gdx.input.justTouched()) return;
        float x = Gdx.input.getX();
        float y = screenH - Gdx.input.getY();
        if (screen == Screen.HOME) {
            if (inside(x, y, screenW * 0.08f, screenH * 0.17f, screenW * 0.37f, screenH * 0.16f)) startMatch();
            else if (inside(x, y, screenW * 0.08f, screenH * 0.36f, screenW * 0.37f, screenH * 0.16f)) screen = Screen.CAREER;
        } else if (screen == Screen.CAREER) {
            if (inside(x, y, screenW * 0.07f, screenH * 0.08f, screenW * 0.20f, screenH * 0.12f)) screen = Screen.HOME;
            else if (inside(x, y, screenW * 0.65f, screenH * 0.10f, screenW * 0.27f, screenH * 0.14f)) startMatch();
        }
    }

    private boolean inside(float x, float y, float rx, float ry, float rw, float rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    private void updateMatchControls() {
        move.setZero();
        sprint = false;
        if (fullTime) {
            if (Gdx.input.justTouched()) screen = Screen.HOME;
            return;
        }

        float joyX = Math.max(125f, screenW * 0.115f);
        float joyY = Math.max(120f, screenH * 0.18f);
        float joyRadius = Math.min(96f, screenH * 0.16f);
        boolean shoot = false;
        boolean pass = false;
        boolean switchPlayer = false;

        for (int pointer = 0; pointer < 6; pointer++) {
            if (!Gdx.input.isTouched(pointer)) continue;
            float x = Gdx.input.getX(pointer);
            float y = screenH - Gdx.input.getY(pointer);
            if (x < screenW * 0.40f) {
                float dx = x - joyX;
                float dy = y - joyY;
                float len = (float)Math.sqrt(dx * dx + dy * dy);
                if (len > 7f) {
                    float s = Math.min(1f, len / joyRadius) / len;
                    move.set(dx * s, dy * s);
                }
            } else if (x > screenW * 0.83f && y < screenH * 0.42f) shoot = true;
            else if (x > screenW * 0.67f && x < screenW * 0.84f && y < screenH * 0.27f) pass = true;
            else if (x > screenW * 0.67f && x < screenW * 0.84f && y > screenH * 0.29f && y < screenH * 0.49f) sprint = true;
            else if (x > screenW * 0.83f && y > screenH * 0.62f) switchPlayer = true;
        }

        if (shoot) tryKick(29f, 4.8f, true);
        else if (pass) passToTeammate();
        if (switchPlayer && switchCooldown <= 0f) switchControlled();
    }

    private void tryKick(float power, float lift, boolean userShot) {
        if (actionCooldown > 0f || controlled.pos.dst(ballPos.x, 0, ballPos.z) > 2.5f) return;
        Vector3 dir = controlled.facing.cpy();
        if (dir.len2() < 0.05f) dir.set(0, 0, -1);
        dir.nor();
        ballVel.set(dir.x * power, lift, dir.z * power);
        lastTouch = controlled;
        actionCooldown = userShot ? 0.36f : 0.25f;
    }

    private void passToTeammate() {
        if (actionCooldown > 0f || controlled.pos.dst(ballPos.x, 0, ballPos.z) > 2.65f) return;
        Player best = null;
        float bestScore = -999f;
        for (Player p : blue) {
            if (p == controlled || p.keeper) continue;
            Vector3 to = p.pos.cpy().sub(controlled.pos);
            float d = to.len();
            if (d < 2f || d > 32f) continue;
            float forward = to.nor().dot(controlled.facing);
            float score = forward * 10f - d * 0.12f;
            if (score > bestScore) { bestScore = score; best = p; }
        }
        Vector3 dir = best != null ? best.pos.cpy().sub(ballPos.x, 0, ballPos.z).nor() : controlled.facing.cpy().nor();
        ballVel.set(dir.x * 17f, 1.0f, dir.z * 17f);
        lastTouch = controlled;
        actionCooldown = 0.28f;
    }

    private void switchControlled() {
        Player best = null;
        float bestD = Float.MAX_VALUE;
        for (Player p : blue) {
            if (p.keeper) continue;
            float d = p.pos.dst(ballPos.x, 0, ballPos.z);
            if (d < bestD) { bestD = d; best = p; }
        }
        if (best != null) controlled = best;
        switchCooldown = 0.45f;
    }

    private void updateGame(float dt) {
        if (fullTime) return;
        if (kickoffDelay > 0f) {
            kickoffDelay -= dt;
            syncTransforms(dt);
            return;
        }

        matchRealSeconds += dt;
        float matchMinute = matchRealSeconds * 0.60f;
        if (!halfTimeShown && matchMinute >= 45f) {
            halfTimeShown = true;
            banner = "INTERVALO";
            bannerTimer = 2.2f;
            kickoffDelay = 1.8f;
            resetPositionsOnly();
        }
        if (matchMinute >= 90f) {
            fullTime = true;
            banner = "FIM DE JOGO";
            bannerTimer = 999f;
            updateCareerAfterMatch();
            return;
        }

        float speed = sprint ? 12.0f : 8.0f;
        Vector3 desired = new Vector3(move.x, 0, -move.y);
        if (desired.len2() > 0.01f) {
            desired.nor();
            controlled.facing.lerp(desired, 0.30f).nor();
            controlled.pos.mulAdd(desired, speed * dt);
            controlled.runBlend = Math.min(1f, controlled.runBlend + dt * 6f);
        } else controlled.runBlend = Math.max(0f, controlled.runBlend - dt * 6f);
        clampPlayer(controlled);

        updateTeamAI(blue, true, dt);
        updateTeamAI(red, false, dt);
        playerBallInteractions();

        ballVel.y -= 10.2f * dt;
        ballPos.mulAdd(ballVel, dt);
        if (ballPos.y < 0.42f) {
            ballPos.y = 0.42f;
            if (ballVel.y < 0f) ballVel.y *= -0.24f;
            float drag = (float)Math.pow(0.983f, dt * 60f);
            ballVel.x *= drag;
            ballVel.z *= drag;
        }

        if (Math.abs(ballPos.x) > HALF_W) {
            ballPos.x = MathUtils.clamp(ballPos.x, -HALF_W, HALF_W);
            ballVel.x *= -0.35f;
        }

        checkGoalOrOut();
        syncTransforms(dt);
    }

    private void playerBallInteractions() {
        Array<Player> allA = blue;
        Array<Player> allB = red;
        for (Player p : allA) touchBall(p);
        for (Player p : allB) touchBall(p);
    }

    private void touchBall(Player p) {
        float d = p.pos.dst(ballPos.x, 0, ballPos.z);
        if (d > 1.05f || ballPos.y > 1.45f) return;
        Vector3 away = new Vector3(ballPos.x - p.pos.x, 0, ballPos.z - p.pos.z);
        if (away.len2() < 0.01f) away.set(p.facing);
        away.nor();
        float controlPush = (p == controlled && move.len2() > 0.02f) ? 5.8f : 3.2f;
        ballVel.x = MathUtils.lerp(ballVel.x, away.x * controlPush + p.facing.x * 2.2f, 0.25f);
        ballVel.z = MathUtils.lerp(ballVel.z, away.z * controlPush + p.facing.z * 2.2f, 0.25f);
        lastTouch = p;
    }

    private void updateTeamAI(Array<Player> team, boolean isBlue, float dt) {
        Player chaser = null;
        float chaseD = Float.MAX_VALUE;
        for (Player p : team) {
            if (p == controlled) continue;
            float d = p.pos.dst(ballPos.x, 0, ballPos.z);
            if (!p.keeper && d < chaseD) { chaseD = d; chaser = p; }
        }

        for (Player p : team) {
            if (p == controlled) continue;
            Vector3 target;
            if (p.keeper) {
                float goalZ = isBlue ? HALF_L - 1.5f : -HALF_L + 1.5f;
                float tx = MathUtils.clamp(ballPos.x * 0.35f, -5.2f, 5.2f);
                target = new Vector3(tx, 0, goalZ);
                if (p.pos.dst(ballPos.x, 0, ballPos.z) < 7.0f) target.set(ballPos.x, 0, ballPos.z);
            } else if (p == chaser) {
                target = new Vector3(ballPos.x, 0, ballPos.z);
            } else {
                float ballInfluence = MathUtils.clamp((ballPos.z - p.home.z) / 34f, -1f, 1f);
                float zShift = ballInfluence * 8f;
                float xShift = MathUtils.clamp(ballPos.x * 0.12f, -5f, 5f);
                target = new Vector3(p.home.x + xShift, 0, p.home.z + zShift);
            }

            Vector3 dir = target.sub(p.pos);
            float dist = dir.len();
            if (dist > 0.25f) {
                dir.scl(1f / dist);
                p.facing.lerp(dir, 0.16f).nor();
                float aiSpeed = p.keeper ? 5.4f : (p == chaser ? 7.5f : 5.2f);
                p.pos.mulAdd(dir, Math.min(dist, aiSpeed * dt));
                p.runBlend = Math.min(1f, p.runBlend + dt * 4f);
                clampPlayer(p);
            } else p.runBlend = Math.max(0f, p.runBlend - dt * 5f);

            float dBall = p.pos.dst(ballPos.x, 0, ballPos.z);
            if (dBall < (p.keeper ? 1.9f : 1.3f) && actionCooldown <= 0f) {
                float attackGoalZ = isBlue ? -HALF_L : HALF_L;
                Vector3 kick = new Vector3(-ballPos.x * 0.035f, 0, attackGoalZ - ballPos.z).nor();
                float power = p.keeper ? 22f : 15.5f;
                ballVel.set(kick.x * power, p.keeper ? 2.6f : 1.1f, kick.z * power);
                lastTouch = p;
                actionCooldown = 0.18f;
            }
        }
    }

    private void clampPlayer(Player p) {
        p.pos.x = MathUtils.clamp(p.pos.x, -HALF_W + 0.45f, HALF_W - 0.45f);
        p.pos.z = MathUtils.clamp(p.pos.z, -HALF_L + 0.45f, HALF_L - 0.45f);
    }

    private void checkGoalOrOut() {
        if (ballPos.z < -HALF_L - 0.45f && Math.abs(ballPos.x) <= GOAL_HALF && ballPos.y < 2.6f) {
            blueScore++;
            if (lastTouch == controlled) { careerGoals++; careerXp += 80; saveCareer(); }
            showGoal(true);
        } else if (ballPos.z > HALF_L + 0.45f && Math.abs(ballPos.x) <= GOAL_HALF && ballPos.y < 2.6f) {
            redScore++;
            showGoal(false);
        } else if (Math.abs(ballPos.z) > HALF_L + 2.5f) {
            ballPos.z = MathUtils.clamp(ballPos.z, -HALF_L, HALF_L);
            ballVel.z *= -0.35f;
        }
    }

    private void showGoal(boolean blueGoal) {
        banner = blueGoal ? "GOOOOL  AURORA!" : "GOL  RIVAL";
        bannerTimer = 2.4f;
        kickoffDelay = 2.0f;
        resetPositionsOnly();
    }

    private void resetPositionsOnly() {
        for (Player p : blue) p.reset();
        for (Player p : red) p.reset();
        ballPos.set(0, 0.42f, 0);
        ballVel.setZero();
        controlled = blue.get(9);
        lastTouch = null;
    }

    private void updateCareerAfterMatch() {
        if (careerUpdated) return;
        careerUpdated = true;
        careerMatches++;
        careerXp += 45 + blueScore * 20;
        saveCareer();
    }

    private void syncTransforms(float dt) {
        float t = matchRealSeconds * 9f;
        for (Player p : blue) syncPlayer(p, t, dt);
        for (Player p : red) syncPlayer(p, t, dt);
        ball.transform.setToTranslation(ballPos).rotate(Vector3.X, matchRealSeconds * 150f);
        ballShadow.transform.setToTranslation(ballPos.x, 0.04f, ballPos.z);
        selectRing.transform.setToTranslation(controlled.pos.x, 0.045f, controlled.pos.z);
    }

    private void syncPlayer(Player p, float t, float dt) {
        float angle = (float)Math.toDegrees(Math.atan2(p.facing.x, p.facing.z));
        float swing = (float)Math.sin(t + p.index * 0.65f) * 24f * p.runBlend;
        float bob = Math.abs((float)Math.sin(t + p.index * 0.65f)) * 0.045f * p.runBlend;
        float x = p.pos.x;
        float z = p.pos.z;

        p.shadow.transform.setToTranslation(x, 0.035f, z);
        p.torso.transform.setToTranslation(x, 1.58f + bob, z).rotate(Vector3.Y, angle);
        p.shorts.transform.setToTranslation(x, 1.03f + bob, z).rotate(Vector3.Y, angle);
        p.head.transform.setToTranslation(x, 2.30f + bob, z).rotate(Vector3.Y, angle);

        p.armL.transform.setToTranslation(x - 0.52f, 1.58f + bob, z).rotate(Vector3.Y, angle).rotate(Vector3.X, -swing * 0.65f);
        p.armR.transform.setToTranslation(x + 0.52f, 1.58f + bob, z).rotate(Vector3.Y, angle).rotate(Vector3.X, swing * 0.65f);
        p.legL.transform.setToTranslation(x - 0.23f, 0.62f, z).rotate(Vector3.Y, angle).rotate(Vector3.X, swing);
        p.legR.transform.setToTranslation(x + 0.23f, 0.62f, z).rotate(Vector3.Y, angle).rotate(Vector3.X, -swing);
        p.bootL.transform.setToTranslation(x - 0.23f, 0.12f, z - 0.10f).rotate(Vector3.Y, angle);
        p.bootR.transform.setToTranslation(x + 0.23f, 0.12f, z - 0.10f).rotate(Vector3.Y, angle);
    }

    private void updateCamera(float dt) {
        Vector3 focus = new Vector3(ballPos.x * 0.55f + controlled.pos.x * 0.45f, 0.8f, ballPos.z * 0.62f + controlled.pos.z * 0.38f);
        Vector3 desired = new Vector3(
            MathUtils.clamp(focus.x * 0.45f + 22f, -6f, 34f),
            25f,
            MathUtils.clamp(focus.z + 24f, -35f, 55f)
        );
        float smooth = 1f - (float)Math.pow(0.025f, dt);
        camera.position.lerp(desired, smooth);
        camera.up.set(Vector3.Y);
        camera.lookAt(focus.x, 0.3f, focus.z - 5f);
        camera.update();
    }

    private void drawMenu() {
        Gdx.gl.glViewport(0, 0, screenW, screenH);
        Gdx.gl.glClearColor(0.006f, 0.015f, 0.025f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        shapes.setProjectionMatrix(uiMatrix);
        batch.setProjectionMatrix(uiMatrix);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.015f, 0.055f, 0.075f, 1f);
        shapes.rect(0, 0, screenW, screenH);
        shapes.setColor(0.03f, 0.12f, 0.12f, 1f);
        shapes.rect(screenW * 0.56f, 0, screenW * 0.44f, screenH);
        for (int i = 0; i < 9; i++) {
            float yy = screenH * (0.08f + i * 0.11f);
            shapes.setColor(0.10f, 0.35f + i * 0.015f, 0.25f, 0.18f);
            shapes.rect(screenW * 0.56f, yy, screenW * 0.44f, 2f);
        }
        shapes.end();

        if (screen == Screen.HOME) drawHome();
        else drawCareer();
    }

    private void drawHome() {
        float bx = screenW * 0.08f;
        float bw = screenW * 0.37f;
        float bh = screenH * 0.16f;
        drawPanel(bx, screenH * 0.17f, bw, bh, true);
        drawPanel(bx, screenH * 0.36f, bw, bh, false);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        float pulse = 0.86f + 0.08f * (float)Math.sin(menuPulse * 2.4f);
        shapes.setColor(0.62f * pulse, 0.96f * pulse, 0.10f * pulse, 1f);
        shapes.circle(screenW * 0.76f, screenH * 0.54f, screenH * 0.18f, 44);
        shapes.setColor(0.015f, 0.045f, 0.05f, 1f);
        shapes.circle(screenW * 0.76f, screenH * 0.54f, screenH * 0.145f, 44);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "futREAL", screenW * 0.075f, screenH * 0.88f);
        smallFont.setColor(new Color(0.67f, 1f, 0.12f, 1));
        smallFont.draw(batch, "FOOTBALL. CARREIRA. SUA HISTORIA.", screenW * 0.078f, screenH * 0.79f);
        font.setColor(Color.WHITE);
        font.draw(batch, "JOGAR PARTIDA", bx + 26f, screenH * 0.17f + bh * 0.63f);
        font.draw(batch, "MODO CARREIRA", bx + 26f, screenH * 0.36f + bh * 0.63f);
        smallFont.setColor(new Color(0.76f, 0.84f, 0.88f, 1));
        smallFont.draw(batch, "11 x 11  |  CAMERA TV  |  OFFLINE", bx, screenH * 0.11f);
        font.setColor(new Color(0.65f, 1f, 0.10f, 1));
        font.draw(batch, "7", screenW * 0.745f, screenH * 0.61f);
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, "NEXT", screenW * 0.735f, screenH * 0.49f);
        batch.end();
    }

    private void drawCareer() {
        float cardX = screenW * 0.07f;
        float cardY = screenH * 0.30f;
        float cardW = screenW * 0.40f;
        float cardH = screenH * 0.52f;
        drawPanel(cardX, cardY, cardW, cardH, true);
        drawPanel(screenW * 0.51f, cardY, screenW * 0.41f, cardH, false);
        drawPanel(screenW * 0.07f, screenH * 0.08f, screenW * 0.20f, screenH * 0.12f, false);
        drawPanel(screenW * 0.65f, screenH * 0.10f, screenW * 0.27f, screenH * 0.14f, true);

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "CARREIRA", screenW * 0.07f, screenH * 0.92f);
        smallFont.setColor(new Color(0.65f, 1f, 0.10f, 1));
        smallFont.draw(batch, "ATLETICO AURORA", cardX + 26f, cardY + cardH - 38f);
        font.setColor(Color.WHITE);
        font.draw(batch, "NOX JR.   #19", cardX + 26f, cardY + cardH - 86f);
        smallFont.setColor(new Color(0.76f, 0.84f, 0.88f, 1));
        smallFont.draw(batch, "ATA / PD", cardX + 26f, cardY + cardH - 125f);
        smallFont.draw(batch, "OVERALL", cardX + 26f, cardY + 92f);
        font.setColor(new Color(0.65f, 1f, 0.10f, 1));
        font.draw(batch, String.valueOf(careerOverall), cardX + 145f, cardY + 103f);

        float sx = screenW * 0.545f;
        font.setColor(Color.WHITE);
        font.draw(batch, "TEMPORADA", sx, cardY + cardH - 48f);
        smallFont.setColor(new Color(0.76f, 0.84f, 0.88f, 1));
        smallFont.draw(batch, "PARTIDAS", sx, cardY + cardH - 105f);
        smallFont.draw(batch, "GOLS", sx, cardY + cardH - 158f);
        smallFont.draw(batch, "ASSISTENCIAS", sx, cardY + cardH - 211f);
        smallFont.draw(batch, "XP", sx, cardY + cardH - 264f);
        font.setColor(Color.WHITE);
        font.draw(batch, String.valueOf(careerMatches), sx + screenW * 0.24f, cardY + cardH - 105f);
        font.draw(batch, String.valueOf(careerGoals), sx + screenW * 0.24f, cardY + cardH - 158f);
        font.draw(batch, String.valueOf(careerAssists), sx + screenW * 0.24f, cardY + cardH - 211f);
        font.draw(batch, String.valueOf(careerXp), sx + screenW * 0.24f, cardY + cardH - 264f);
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, "VOLTAR", screenW * 0.105f, screenH * 0.15f);
        font.draw(batch, "JOGAR", screenW * 0.70f, screenH * 0.19f);
        batch.end();
    }

    private void drawPanel(float x, float y, float w, float h, boolean accent) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(accent ? new Color(0.08f, 0.18f, 0.16f, 1) : new Color(0.035f, 0.075f, 0.09f, 1));
        shapes.rect(x, y, w, h);
        shapes.setColor(accent ? new Color(0.65f, 1f, 0.10f, 1) : new Color(0.14f, 0.24f, 0.26f, 1));
        shapes.rect(x, y + h - 5f, w, 5f);
        shapes.end();
    }

    private void drawMatchHud() {
        shapes.setProjectionMatrix(uiMatrix);
        batch.setProjectionMatrix(uiMatrix);
        float minute = Math.min(90f, matchRealSeconds * 0.60f);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.015f, 0.025f, 0.035f, 0.96f);
        shapes.rect(screenW * 0.36f, screenH * 0.88f, screenW * 0.28f, screenH * 0.095f);
        shapes.setColor(0.65f, 1f, 0.10f, 1f);
        shapes.rect(screenW * 0.36f, screenH * 0.88f, 5f, screenH * 0.095f);

        float joyX = Math.max(125f, screenW * 0.115f);
        float joyY = Math.max(120f, screenH * 0.18f);
        float jr = Math.min(96f, screenH * 0.16f);
        shapes.setColor(0.08f, 0.12f, 0.15f, 0.72f);
        shapes.circle(joyX, joyY, jr, 32);
        shapes.setColor(0.32f, 0.40f, 0.44f, 0.75f);
        shapes.circle(joyX + move.x * jr * 0.55f, joyY + move.y * jr * 0.55f, jr * 0.36f, 24);

        drawButtonCircle(screenW * 0.91f, screenH * 0.18f, screenH * 0.105f, new Color(0.92f, 0.13f, 0.12f, 0.86f));
        drawButtonCircle(screenW * 0.75f, screenH * 0.14f, screenH * 0.082f, new Color(0.12f, 0.55f, 0.96f, 0.86f));
        drawButtonCircle(screenW * 0.76f, screenH * 0.38f, screenH * 0.080f, new Color(0.65f, 1f, 0.10f, 0.82f));
        drawButtonCircle(screenW * 0.92f, screenH * 0.76f, screenH * 0.063f, new Color(0.50f, 0.56f, 0.63f, 0.82f));
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "AUR  " + blueScore + "  -  " + redScore + "  VIL", screenW * 0.39f, screenH * 0.947f);
        smallFont.setColor(new Color(0.67f, 1f, 0.12f, 1));
        smallFont.draw(batch, String.format("%02d'", (int)minute), screenW * 0.59f, screenH * 0.947f);
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, "CHUTE", screenW * 0.885f, screenH * 0.185f);
        smallFont.draw(batch, "PASSE", screenW * 0.725f, screenH * 0.145f);
        smallFont.setColor(new Color(0.04f, 0.08f, 0.05f, 1));
        smallFont.draw(batch, "SPRINT", screenW * 0.725f, screenH * 0.385f);
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, "TROCA", screenW * 0.895f, screenH * 0.765f);
        smallFont.setColor(new Color(0.65f, 1f, 0.10f, 1));
        smallFont.draw(batch, "#" + (controlled.index + 1), 18f, screenH - 18f);

        if (bannerTimer > 0f || fullTime) {
            font.setColor(Color.WHITE);
            font.draw(batch, banner, screenW * 0.40f, screenH * 0.72f);
            if (fullTime) {
                smallFont.setColor(new Color(0.65f, 1f, 0.10f, 1));
                smallFont.draw(batch, "TOQUE PARA VOLTAR AO MENU", screenW * 0.40f, screenH * 0.66f);
            }
        }
        batch.end();
    }

    private void drawButtonCircle(float x, float y, float radius, Color c) {
        shapes.setColor(c);
        shapes.circle(x, y, radius, 28);
    }

    @Override
    public void resize(int width, int height) {
        screenW = Math.max(1, width);
        screenH = Math.max(1, height);
        uiMatrix.setToOrtho2D(0, 0, screenW, screenH);
        camera.viewportWidth = screenW;
        camera.viewportHeight = screenH;
        camera.position.set(22f, 25f, 28f);
        camera.lookAt(0, 0, -8f);
        camera.update();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        batch.dispose();
        shapes.dispose();
        font.dispose();
        smallFont.dispose();
        for (Model m : ownedModels) m.dispose();
    }

    private static class Player {
        final boolean blue;
        final int index;
        final boolean keeper;
        final Vector3 home = new Vector3();
        final Vector3 pos = new Vector3();
        final Vector3 facing = new Vector3(0, 0, -1);
        float runBlend;
        ModelInstance torso, shorts, head, armL, armR, legL, legR, bootL, bootR, shadow;

        Player(boolean blue, int index, boolean keeper, float x, float z) {
            this.blue = blue;
            this.index = index;
            this.keeper = keeper;
            home.set(x, 0, z);
            pos.set(home);
            facing.set(0, 0, blue ? -1f : 1f);
        }

        void reset() {
            pos.set(home);
            facing.set(0, 0, blue ? -1f : 1f);
            runBlend = 0f;
        }
    }
}
