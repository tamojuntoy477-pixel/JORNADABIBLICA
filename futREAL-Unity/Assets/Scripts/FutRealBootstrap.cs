using System;
using System.Collections.Generic;
using UnityEngine;

public class FutRealBootstrap : MonoBehaviour
{
    class Bot
    {
        public Transform t;
        public Vector3 home;
        public bool blue;
        public bool keeper;
        public float speed;
    }

    readonly List<Bot> bots = new List<Bot>();
    readonly List<Transform> blueTeammates = new List<Transform>();

    Transform player;
    CharacterController controller;
    Rigidbody ball;
    Camera cam;

    bool inMenu = true;
    bool matchRunning;
    bool sprinting;
    float minute;
    int blueScore;
    int redScore;
    int careerMatches;
    int careerGoals;

    int joystickFinger = -1;
    Vector2 joystickStart;
    Vector2 joystickVector;
    Vector3 facing = Vector3.back;

    Material grassA, grassB, white, blue, red, dark, crowdBlue, crowdRed, skin, ballMat;

    const float FieldHalfX = 34f;
    const float FieldHalfZ = 52.5f;

    void Start()
    {
        Application.targetFrameRate = 60;
        Screen.sleepTimeout = SleepTimeout.NeverSleep;
        Screen.orientation = ScreenOrientation.LandscapeLeft;
        careerMatches = PlayerPrefs.GetInt("career_matches", 0);
        careerGoals = PlayerPrefs.GetInt("career_goals", 0);

        CreateMaterials();
        BuildWorld();
        BuildTeams();
        ResetMatch();
    }

    void CreateMaterials()
    {
        grassA = Mat(new Color(0.025f, 0.38f, 0.09f), 0.8f);
        grassB = Mat(new Color(0.035f, 0.46f, 0.12f), 0.78f);
        white = Mat(new Color(0.94f, 0.97f, 0.95f), 0.55f);
        blue = Mat(new Color(0.08f, 0.42f, 0.96f), 0.45f);
        red = Mat(new Color(0.92f, 0.10f, 0.14f), 0.45f);
        dark = Mat(new Color(0.025f, 0.03f, 0.045f), 0.7f);
        crowdBlue = Mat(new Color(0.03f, 0.20f, 0.55f), 0.7f);
        crowdRed = Mat(new Color(0.52f, 0.025f, 0.06f), 0.7f);
        skin = Mat(new Color(0.62f, 0.40f, 0.25f), 0.7f);
        ballMat = Mat(new Color(0.95f, 0.96f, 0.94f), 0.35f);
    }

    Material Mat(Color c, float roughness)
    {
        Shader s = Shader.Find("Standard");
        var m = new Material(s);
        m.color = c;
        m.SetFloat("_Glossiness", 1f - roughness);
        return m;
    }

    void BuildWorld()
    {
        RenderSettings.ambientLight = new Color(0.42f, 0.45f, 0.52f);

        var sunObj = new GameObject("Sun");
        var sun = sunObj.AddComponent<Light>();
        sun.type = LightType.Directional;
        sun.intensity = 1.15f;
        sun.color = new Color(1f, 0.95f, 0.86f);
        sun.shadows = LightShadows.Soft;
        sunObj.transform.rotation = Quaternion.Euler(48f, -28f, 0f);

        CreateBox("StadiumBase", new Vector3(0, -1.25f, 0), new Vector3(100, 2, 140), dark, true);

        for (int i = 0; i < 10; i++)
        {
            float z = -47.25f + i * 10.5f;
            CreateBox("GrassStrip", new Vector3(0, -0.08f, z), new Vector3(68, 0.16f, 10.5f), i % 2 == 0 ? grassA : grassB, true);
        }

        CreateBox("LineMid", new Vector3(0, 0.015f, 0), new Vector3(68, 0.035f, 0.13f), white, false);
        CreateBox("LineL", new Vector3(-34, 0.015f, 0), new Vector3(0.13f, 0.035f, 105), white, false);
        CreateBox("LineR", new Vector3(34, 0.015f, 0), new Vector3(0.13f, 0.035f, 105), white, false);
        CreateBox("LineTop", new Vector3(0, 0.015f, -52.5f), new Vector3(68, 0.035f, 0.13f), white, false);
        CreateBox("LineBottom", new Vector3(0, 0.015f, 52.5f), new Vector3(68, 0.035f, 0.13f), white, false);
        CreateCircleLine(9.15f, 72);

        BuildGoal(-53.15f);
        BuildGoal(53.15f);
        BuildStands();

        cam = new GameObject("BroadcastCamera").AddComponent<Camera>();
        cam.fieldOfView = 46f;
        cam.nearClipPlane = 0.2f;
        cam.farClipPlane = 220f;
        cam.transform.position = new Vector3(0, 18, 35);
        cam.transform.LookAt(Vector3.zero);
        cam.tag = "MainCamera";
    }

    void BuildStands()
    {
        CreateBox("StandLeft", new Vector3(-44, 5.5f, 0), new Vector3(15, 11, 128), dark, false);
        CreateBox("StandRight", new Vector3(44, 5.5f, 0), new Vector3(15, 11, 128), dark, false);
        CreateBox("StandNorth", new Vector3(0, 5.5f, -64), new Vector3(78, 11, 18), dark, false);
        CreateBox("StandSouth", new Vector3(0, 5.5f, 64), new Vector3(78, 11, 18), dark, false);

        var rng = new System.Random(19);
        for (int i = 0; i < 180; i++)
        {
            bool left = i % 2 == 0;
            float x = left ? -39f - (float)rng.NextDouble() * 7f : 39f + (float)rng.NextDouble() * 7f;
            float z = -55f + (float)rng.NextDouble() * 110f;
            float y = 2.0f + (float)rng.NextDouble() * 7.5f;
            CreateBox("Crowd", new Vector3(x, y, z), new Vector3(0.55f, 0.8f, 0.55f), i % 3 == 0 ? crowdRed : crowdBlue, false);
        }

        foreach (float x in new[] { -39f, 39f })
        foreach (float z in new[] { -48f, 48f })
        {
            CreateBox("FloodPole", new Vector3(x, 11f, z), new Vector3(0.3f, 22f, 0.3f), white, false);
            var lampObj = new GameObject("FloodLight");
            lampObj.transform.position = new Vector3(x, 21f, z);
            lampObj.transform.LookAt(Vector3.zero);
            var lamp = lampObj.AddComponent<Light>();
            lamp.type = LightType.Spot;
            lamp.range = 90;
            lamp.spotAngle = 62;
            lamp.intensity = 4.2f;
            lamp.color = new Color(0.88f, 0.93f, 1f);
            lamp.shadows = LightShadows.Soft;
        }
    }

    void BuildGoal(float z)
    {
        CreateBox("GoalPost", new Vector3(-7.3f, 1.25f, z), new Vector3(0.16f, 2.5f, 0.16f), white, false);
        CreateBox("GoalPost", new Vector3(7.3f, 1.25f, z), new Vector3(0.16f, 2.5f, 0.16f), white, false);
        CreateBox("GoalBar", new Vector3(0, 2.5f, z), new Vector3(14.75f, 0.16f, 0.16f), white, false);
    }

    void CreateCircleLine(float radius, int segments)
    {
        var go = new GameObject("CenterCircle");
        var lr = go.AddComponent<LineRenderer>();
        lr.loop = true;
        lr.positionCount = segments;
        lr.widthMultiplier = 0.12f;
        lr.material = white;
        lr.useWorldSpace = true;
        for (int i = 0; i < segments; i++)
        {
            float a = Mathf.PI * 2f * i / segments;
            lr.SetPosition(i, new Vector3(Mathf.Cos(a) * radius, 0.04f, Mathf.Sin(a) * radius));
        }
    }

    void BuildTeams()
    {
        player = CreateFootballer("NOX JR 19", new Vector3(0, 1f, 29), blue, true);
        controller = player.gameObject.AddComponent<CharacterController>();
        controller.height = 2f;
        controller.radius = 0.48f;
        controller.center = new Vector3(0, 1f, 0);

        ball = GameObject.CreatePrimitive(PrimitiveType.Sphere).AddComponent<Rigidbody>();
        ball.name = "futREAL Ball";
        ball.transform.position = new Vector3(0, 0.48f, 24.5f);
        ball.transform.localScale = Vector3.one * 0.48f;
        ball.GetComponent<Renderer>().material = ballMat;
        ball.mass = 0.43f;
        ball.drag = 0.18f;
        ball.angularDrag = 0.08f;
        ball.collisionDetectionMode = CollisionDetectionMode.ContinuousDynamic;

        Vector3[] blueHomes = {
            new Vector3(-22,1,28), new Vector3(22,1,28), new Vector3(-12,1,15), new Vector3(12,1,15),
            new Vector3(-24,1,0), new Vector3(0,1,2), new Vector3(24,1,0), new Vector3(-12,1,-14), new Vector3(12,1,-14)
        };
        foreach (var h in blueHomes)
        {
            var t = CreateFootballer("Blue", h, blue, false);
            blueTeammates.Add(t);
            bots.Add(new Bot { t = t, home = h, blue = true, keeper = false, speed = 6.2f });
        }
        var blueGK = CreateFootballer("Blue GK", new Vector3(0,1,48), blue, false);
        bots.Add(new Bot { t = blueGK, home = new Vector3(0,1,48), blue = true, keeper = true, speed = 7f });

        Vector3[] redHomes = {
            new Vector3(-24,1,-28), new Vector3(-8,1,-30), new Vector3(8,1,-30), new Vector3(24,1,-28),
            new Vector3(-20,1,-13), new Vector3(0,1,-15), new Vector3(20,1,-13), new Vector3(-22,1,5), new Vector3(0,1,7), new Vector3(22,1,5)
        };
        foreach (var h in redHomes)
        {
            var t = CreateFootballer("Red", h, red, false);
            bots.Add(new Bot { t = t, home = h, blue = false, keeper = false, speed = 6.45f });
        }
        var redGK = CreateFootballer("Red GK", new Vector3(0,1,-48), red, false);
        bots.Add(new Bot { t = redGK, home = new Vector3(0,1,-48), blue = false, keeper = true, speed = 7f });
    }

    Transform CreateFootballer(string name, Vector3 pos, Material kit, bool selected)
    {
        var root = new GameObject(name);
        root.transform.position = pos;
        var body = GameObject.CreatePrimitive(PrimitiveType.Capsule);
        body.name = "Body";
        body.transform.SetParent(root.transform, false);
        body.transform.localScale = new Vector3(0.82f, 1f, 0.82f);
        body.GetComponent<Renderer>().material = kit;
        Destroy(body.GetComponent<Collider>());
        var head = GameObject.CreatePrimitive(PrimitiveType.Sphere);
        head.name = "Head";
        head.transform.SetParent(root.transform, false);
        head.transform.localPosition = new Vector3(0, 1.12f, 0);
        head.transform.localScale = Vector3.one * 0.48f;
        head.GetComponent<Renderer>().material = skin;
        Destroy(head.GetComponent<Collider>());
        if (selected)
        {
            var ring = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            ring.name = "Selected";
            ring.transform.SetParent(root.transform, false);
            ring.transform.localPosition = new Vector3(0, -0.98f, 0);
            ring.transform.localScale = new Vector3(1.25f, 0.02f, 1.25f);
            ring.GetComponent<Renderer>().material = white;
            Destroy(ring.GetComponent<Collider>());
        }
        return root.transform;
    }

    GameObject CreateBox(string name, Vector3 pos, Vector3 scale, Material mat, bool collider)
    {
        var go = GameObject.CreatePrimitive(PrimitiveType.Cube);
        go.name = name;
        go.transform.position = pos;
        go.transform.localScale = scale;
        go.GetComponent<Renderer>().material = mat;
        if (!collider) Destroy(go.GetComponent<Collider>());
        return go;
    }

    void Update()
    {
        if (!matchRunning) return;
        UpdateTouchJoystick();
        UpdatePlayer();
        UpdateBots();
        UpdateBallAssist();
        UpdateCamera();
        CheckGoal();

        minute += Time.deltaTime * 1.15f;
        if (minute >= 90f) FinishMatch();
    }

    void UpdateTouchJoystick()
    {
        joystickVector = Vector2.zero;
        foreach (Touch t in Input.touches)
        {
            if (t.phase == TouchPhase.Began && t.position.x < Screen.width * 0.42f && joystickFinger < 0)
            {
                joystickFinger = t.fingerId;
                joystickStart = t.position;
            }
            if (t.fingerId != joystickFinger) continue;
            if (t.phase == TouchPhase.Ended || t.phase == TouchPhase.Canceled)
            {
                joystickFinger = -1;
                joystickVector = Vector2.zero;
            }
            else
            {
                joystickVector = Vector2.ClampMagnitude((t.position - joystickStart) / 90f, 1f);
            }
        }
    }

    void UpdatePlayer()
    {
        Vector2 input = joystickVector;
        if (input.sqrMagnitude < 0.01f)
            input = new Vector2(Input.GetAxisRaw("Horizontal"), Input.GetAxisRaw("Vertical"));

        Vector3 move = new Vector3(input.x, 0, input.y);
        if (move.sqrMagnitude > 1f) move.Normalize();
        float speed = sprinting ? 10.8f : 7.4f;
        controller.SimpleMove(move * speed);
        player.position = new Vector3(Mathf.Clamp(player.position.x, -32.5f, 32.5f), player.position.y, Mathf.Clamp(player.position.z, -50f, 50f));
        if (move.sqrMagnitude > 0.02f)
        {
            facing = move.normalized;
            player.rotation = Quaternion.Slerp(player.rotation, Quaternion.LookRotation(facing), Time.deltaTime * 10f);
        }

        if (Input.GetKeyDown(KeyCode.Space)) Shoot();
        if (Input.GetKeyDown(KeyCode.E)) Pass();
    }

    void UpdateBots()
    {
        Bot nearestRed = null;
        float nearestRedD = float.MaxValue;
        foreach (var b in bots)
        {
            if (!b.blue && !b.keeper)
            {
                float d = (b.t.position - ball.position).sqrMagnitude;
                if (d < nearestRedD) { nearestRedD = d; nearestRed = b; }
            }
        }

        foreach (var b in bots)
        {
            Vector3 target = b.home;
            if (b.keeper)
            {
                float x = Mathf.Clamp(ball.position.x * 0.65f, -6.2f, 6.2f);
                target = new Vector3(x, 1f, b.blue ? 48f : -48f);
            }
            else if (!b.blue && b == nearestRed)
                target = new Vector3(ball.position.x, 1f, ball.position.z);
            else if (b.blue && ball.position.z > -20f)
                target = Vector3.Lerp(b.home, new Vector3(ball.position.x * 0.45f, 1f, ball.position.z + 8f), 0.28f);

            Vector3 delta = target - b.t.position;
            delta.y = 0;
            if (delta.magnitude > 0.4f)
            {
                Vector3 step = delta.normalized * b.speed * Time.deltaTime;
                if (step.magnitude > delta.magnitude) step = delta;
                b.t.position += step;
                b.t.rotation = Quaternion.Slerp(b.t.rotation, Quaternion.LookRotation(delta.normalized), Time.deltaTime * 7f);
            }

            if (!b.blue && !b.keeper && Vector3.Distance(b.t.position, ball.position) < 1.55f)
            {
                Vector3 attack = Vector3.forward;
                ball.AddForce((attack * 7f + Vector3.up * 0.7f), ForceMode.Acceleration);
            }
        }
    }

    void UpdateBallAssist()
    {
        if (Vector3.Distance(player.position, ball.position) < 1.55f && ball.velocity.magnitude < 9f)
        {
            Vector3 target = player.position + facing * 1.35f + Vector3.up * 0.15f;
            Vector3 correction = target - ball.position;
            ball.AddForce(correction * 18f - ball.velocity * 1.3f, ForceMode.Acceleration);
        }
    }

    void UpdateCamera()
    {
        Vector3 focus = Vector3.Lerp(player.position, ball.position, 0.42f);
        Vector3 desired = focus + new Vector3(0, 15.5f, 21f);
        cam.transform.position = Vector3.Lerp(cam.transform.position, desired, 1f - Mathf.Exp(-4f * Time.deltaTime));
        Vector3 look = focus + Vector3.forward * -4f;
        cam.transform.rotation = Quaternion.Slerp(cam.transform.rotation, Quaternion.LookRotation(look - cam.transform.position), 1f - Mathf.Exp(-6f * Time.deltaTime));
    }

    public void Shoot()
    {
        if (Vector3.Distance(player.position, ball.position) > 2.5f) return;
        Vector3 dir = new Vector3(Mathf.Clamp(-ball.position.x * 0.025f, -0.28f, 0.28f), 0.15f, -1f).normalized;
        ball.velocity *= 0.35f;
        ball.AddForce(dir * 25f, ForceMode.VelocityChange);
    }

    public void Pass()
    {
        if (Vector3.Distance(player.position, ball.position) > 2.5f) return;
        Transform best = null;
        float bestScore = float.MaxValue;
        foreach (var mate in blueTeammates)
        {
            Vector3 to = mate.position - player.position;
            if (Vector3.Dot(to.normalized, facing) < 0.15f) continue;
            float score = to.magnitude + Mathf.Abs(to.x) * 0.15f;
            if (score < bestScore) { bestScore = score; best = mate; }
        }
        Vector3 dir = best ? (best.position - ball.position).normalized : facing;
        ball.velocity *= 0.25f;
        ball.AddForce((dir + Vector3.up * 0.05f).normalized * 13f, ForceMode.VelocityChange);
    }

    void CheckGoal()
    {
        if (Mathf.Abs(ball.position.x) > 7.35f) return;
        if (ball.position.z < -52.6f)
        {
            blueScore++;
            careerGoals++;
            ResetKickoff();
        }
        else if (ball.position.z > 52.6f)
        {
            redScore++;
            ResetKickoff();
        }
    }

    void ResetKickoff()
    {
        ball.velocity = Vector3.zero;
        ball.angularVelocity = Vector3.zero;
        ball.position = new Vector3(0, 0.48f, 0);
        player.position = new Vector3(0, 1f, 10f);
    }

    void ResetMatch()
    {
        minute = 0;
        blueScore = 0;
        redScore = 0;
        ResetKickoff();
        matchRunning = false;
        inMenu = true;
    }

    void StartMatch()
    {
        minute = 0;
        blueScore = 0;
        redScore = 0;
        inMenu = false;
        matchRunning = true;
        ResetKickoff();
    }

    void FinishMatch()
    {
        careerMatches++;
        PlayerPrefs.SetInt("career_matches", careerMatches);
        PlayerPrefs.SetInt("career_goals", careerGoals);
        PlayerPrefs.Save();
        matchRunning = false;
        inMenu = true;
    }

    void OnGUI()
    {
        float sx = Screen.width / 1280f;
        float sy = Screen.height / 720f;
        GUI.matrix = Matrix4x4.TRS(Vector3.zero, Quaternion.identity, new Vector3(sx, sy, 1));

        GUI.skin.label.fontSize = 22;
        GUI.skin.button.fontSize = 22;

        if (inMenu)
        {
            GUI.Box(new Rect(0, 0, 1280, 720), "");
            GUIStyle title = new GUIStyle(GUI.skin.label) { fontSize = 76, fontStyle = FontStyle.Bold };
            title.normal.textColor = new Color(0.72f, 1f, 0.18f);
            GUI.Label(new Rect(80, 90, 700, 100), "futREAL", title);
            GUI.Label(new Rect(86, 195, 800, 50), "UNITY • CARREIRA 3D • MOBILE FOOTBALL");
            GUI.Label(new Rect(86, 270, 700, 45), $"NOX JR. 19   Jogos {careerMatches}   Gols {careerGoals}");
            if (GUI.Button(new Rect(86, 360, 430, 86), "JOGAR PRÓXIMA PARTIDA")) StartMatch();
            GUI.Label(new Rect(86, 500, 930, 70), "Nova base Unity: 11x11 simplificado, física, IA, câmera de TV e carreira salva.");
            return;
        }

        GUI.Box(new Rect(450, 18, 380, 62), $"AUR   {blueScore}  -  {redScore}   VIL");
        GUI.Label(new Rect(470, 82, 140, 40), $"{Mathf.Min(90, (int)minute):00}'");
        GUI.Label(new Rect(22, 18, 320, 45), "futREAL • UNITY");

        GUI.Box(new Rect(40, 500, 190, 190), "");
        Vector2 knob = new Vector2(135, 595) + joystickVector * 62f;
        GUI.Box(new Rect(knob.x - 28, knob.y - 28, 56, 56), "");

        if (GUI.Button(new Rect(1060, 505, 165, 120), "CHUTE")) Shoot();
        if (GUI.Button(new Rect(875, 575, 150, 95), "PASSE")) Pass();
        sprinting = GUI.RepeatButton(new Rect(900, 465, 135, 82), "SPRINT");
    }
}
