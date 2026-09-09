extends Node3D

# futREAL 5.0 - primeira base Godot 3D
# Foco: modo carreira + partida mobile jogavel.

var match_running := false
var match_clock := 0.0
var home_score := 0
var away_score := 0
var season_games := 0
var season_goals := 0
var season_wins := 0
var fans := 1250
var ovr := 70
var xp := 0

var player: Node3D
var ball: MeshInstance3D
var camera: Camera3D
var teammates: Array[Node3D] = []
var opponents: Array[Node3D] = []
var ball_velocity := Vector3.ZERO
var user_has_ball := true
var opponent_has_ball := false
var opponent_owner := -1
var stamina := 100.0

var move_left := false
var move_right := false
var move_up := false
var move_down := false
var sprint_down := false

var ui_layer: CanvasLayer
var career_panel: Control
var hud: Control
var score_label: Label
var clock_label: Label
var stamina_bar: ProgressBar
var career_stats_label: Label
var career_ovr_label: Label
var message_label: Label

const FIELD_X := 33.0
const FIELD_Z := 51.0
const GOAL_HALF := 7.2

func _ready() -> void:
    _build_world()
    _build_match_objects()
    _build_ui()
    _show_career()

func _physics_process(delta: float) -> void:
    if not match_running:
        return
    match_clock += delta * 1.6
    _update_player(delta)
    _update_ball(delta)
    _update_teammates(delta)
    _update_opponents(delta)
    _check_goal()
    _update_camera(delta)
    _update_hud()
    if match_clock >= 90.0:
        _finish_match()

func _build_world() -> void:
    var env_node := WorldEnvironment.new()
    var env := Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = Color(0.20, 0.43, 0.68)
    env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.ambient_light_color = Color(0.72, 0.78, 0.85)
    env.ambient_light_energy = 0.65
    env_node.environment = env
    add_child(env_node)

    var sun := DirectionalLight3D.new()
    sun.rotation_degrees = Vector3(-55.0, -28.0, 0.0)
    sun.light_energy = 1.25
    sun.shadow_enabled = true
    add_child(sun)

    _add_box(self, Vector3(68.0, 0.18, 105.0), Vector3(0, -0.12, 0), Color(0.035, 0.34, 0.10))
    for i in range(10):
        if i % 2 == 0:
            _add_box(self, Vector3(68.0, 0.02, 10.5), Vector3(0, 0.0, -47.25 + i * 10.5), Color(0.05, 0.40, 0.12))

    var white := Color(0.94, 0.96, 0.94)
    _add_box(self, Vector3(68.0, 0.035, 0.12), Vector3(0, 0.03, -52.5), white)
    _add_box(self, Vector3(68.0, 0.035, 0.12), Vector3(0, 0.03, 52.5), white)
    _add_box(self, Vector3(0.12, 0.035, 105.0), Vector3(-34.0, 0.03, 0), white)
    _add_box(self, Vector3(0.12, 0.035, 105.0), Vector3(34.0, 0.03, 0), white)
    _add_box(self, Vector3(68.0, 0.035, 0.12), Vector3(0, 0.03, 0), white)

    # Grande circulo central em pequenos segmentos.
    for i in range(40):
        var a := TAU * float(i) / 40.0
        var marker := _add_box(self, Vector3(1.5, 0.035, 0.10), Vector3(cos(a) * 9.15, 0.035, sin(a) * 9.15), white)
        marker.rotation.y = -a

    _build_goal(-53.0)
    _build_goal(53.0)
    _build_stadium()

func _build_stadium() -> void:
    var stand_color := Color(0.10, 0.12, 0.16)
    var seat_blue := Color(0.05, 0.30, 0.60)
    var seat_red := Color(0.62, 0.08, 0.13)
    _add_box(self, Vector3(14, 10, 122), Vector3(-43, 5, 0), stand_color)
    _add_box(self, Vector3(14, 10, 122), Vector3(43, 5, 0), stand_color)
    _add_box(self, Vector3(74, 10, 16), Vector3(0, 5, -66), stand_color)
    _add_box(self, Vector3(74, 10, 16), Vector3(0, 5, 66), stand_color)
    for z in range(-50, 51, 8):
        _add_box(self, Vector3(1.3, 4.5, 5.5), Vector3(-39.2, 4.6, z), seat_blue if (z / 8) as int % 2 == 0 else seat_red)
        _add_box(self, Vector3(1.3, 4.5, 5.5), Vector3(39.2, 4.6, z), seat_red if (z / 8) as int % 2 == 0 else seat_blue)
    for x in [-27.0, 27.0]:
        for z in [-55.0, 55.0]:
            _add_box(self, Vector3(0.45, 19.0, 0.45), Vector3(x, 9.5, z), Color(0.28, 0.31, 0.35))
            var lamp := OmniLight3D.new()
            lamp.position = Vector3(x, 18.0, z)
            lamp.light_energy = 2.0
            lamp.omni_range = 35.0
            add_child(lamp)

func _build_goal(z: float) -> void:
    var white := Color(0.96, 0.96, 0.96)
    _add_box(self, Vector3(0.16, 2.6, 0.16), Vector3(-GOAL_HALF, 1.3, z), white)
    _add_box(self, Vector3(0.16, 2.6, 0.16), Vector3(GOAL_HALF, 1.3, z), white)
    _add_box(self, Vector3(GOAL_HALF * 2.0, 0.16, 0.16), Vector3(0, 2.6, z), white)

func _build_match_objects() -> void:
    player = _create_player("Nox Jr.", Color(0.72, 1.0, 0.18), Vector3(0, 0, 26), true)
    var mate_positions := [Vector3(-18,0,20), Vector3(18,0,18), Vector3(-11,0,0), Vector3(13,0,-5)]
    for i in range(mate_positions.size()):
        teammates.append(_create_player("AUR_%d" % i, Color(0.08, 0.62, 0.95), mate_positions[i], false))
    var opp_positions := [Vector3(-19,0,-17), Vector3(-7,0,-8), Vector3(8,0,-11), Vector3(19,0,-20), Vector3(0,0,-33)]
    for i in range(opp_positions.size()):
        opponents.append(_create_player("VIL_%d" % i, Color(0.92, 0.12, 0.13), opp_positions[i], false))
    _create_player("GK_AUR", Color(1.0, 0.72, 0.08), Vector3(0,0,49), false)
    _create_player("GK_VIL", Color(1.0, 0.72, 0.08), Vector3(0,0,-49), false)

    ball = MeshInstance3D.new()
    var sphere := SphereMesh.new()
    sphere.radius = 0.43
    sphere.height = 0.86
    sphere.radial_segments = 16
    sphere.rings = 8
    ball.mesh = sphere
    ball.material_override = _mat(Color(0.96, 0.96, 0.95))
    ball.position = Vector3(0, 0.43, 24.6)
    add_child(ball)

    camera = Camera3D.new()
    camera.fov = 55.0
    camera.position = Vector3(0, 15.0, 44.0)
    add_child(camera)
    camera.current = true

func _create_player(name_text: String, kit: Color, pos: Vector3, selected: bool) -> Node3D:
    var root := Node3D.new()
    root.name = name_text
    root.position = pos
    add_child(root)

    var body := MeshInstance3D.new()
    var capsule := CapsuleMesh.new()
    capsule.radius = 0.48
    capsule.height = 1.45
    body.mesh = capsule
    body.position.y = 1.55
    body.material_override = _mat(kit)
    root.add_child(body)

    var head := MeshInstance3D.new()
    var head_mesh := SphereMesh.new()
    head_mesh.radius = 0.34
    head_mesh.height = 0.68
    head.mesh = head_mesh
    head.position.y = 2.62
    head.material_override = _mat(Color(0.76, 0.53, 0.36))
    root.add_child(head)

    for x in [-0.25, 0.25]:
        var leg := MeshInstance3D.new()
        var leg_mesh := BoxMesh.new()
        leg_mesh.size = Vector3(0.24, 0.90, 0.28)
        leg.mesh = leg_mesh
        leg.position = Vector3(x, 0.53, 0)
        leg.material_override = _mat(Color(0.08, 0.09, 0.11))
        root.add_child(leg)

    if selected:
        var ring := MeshInstance3D.new()
        var cyl := CylinderMesh.new()
        cyl.top_radius = 0.90
        cyl.bottom_radius = 0.90
        cyl.height = 0.025
        cyl.radial_segments = 28
        ring.mesh = cyl
        ring.position.y = 0.02
        ring.material_override = _mat(Color(0.72, 1.0, 0.18))
        root.add_child(ring)
    return root

func _build_ui() -> void:
    ui_layer = CanvasLayer.new()
    add_child(ui_layer)
    career_panel = Control.new()
    career_panel.set_anchors_preset(Control.PRESET_FULL_RECT)
    ui_layer.add_child(career_panel)

    var shade := ColorRect.new()
    shade.set_anchors_preset(Control.PRESET_FULL_RECT)
    shade.color = Color(0.02, 0.07, 0.05, 0.88)
    career_panel.add_child(shade)

    _label(career_panel, "futREAL 5", Vector2(42, 28), Vector2(420, 58), 34, Color.WHITE)
    _label(career_panel, "CAREER FIRST • GODOT 3D", Vector2(42, 82), Vector2(520, 36), 16, Color(0.72,1.0,0.18))
    _label(career_panel, "NOX JR.", Vector2(60, 170), Vector2(420, 60), 38, Color.WHITE)
    _label(career_panel, "Atlético Aurora • ATA • Camisa 19", Vector2(60, 225), Vector2(520, 40), 18, Color(0.78,0.82,0.84))
    career_ovr_label = _label(career_panel, "OVR %d" % ovr, Vector2(60, 288), Vector2(300, 54), 30, Color(0.72,1.0,0.18))
    career_stats_label = _label(career_panel, "", Vector2(60, 350), Vector2(600, 160), 20, Color.WHITE)

    var play := Button.new()
    play.text = "JOGAR PRÓXIMA PARTIDA"
    play.position = Vector2(60, 550)
    play.size = Vector2(430, 72)
    play.add_theme_font_size_override("font_size", 20)
    play.pressed.connect(_start_match)
    career_panel.add_child(play)

    var vision := ColorRect.new()
    vision.position = Vector2(720, 118)
    vision.size = Vector2(500, 490)
    vision.color = Color(0.06, 0.16, 0.11, 0.92)
    career_panel.add_child(vision)
    _label(career_panel, "META DA V5", Vector2(755, 150), Vector2(420, 45), 24, Color(0.72,1.0,0.18))
    _label(career_panel, "• Carreira profunda no celular\n• Partida 3D própria\n• Evolução e fãs\n• IA e física refináveis\n• Base pronta para modelos e animações", Vector2(755, 210), Vector2(420, 250), 19, Color.WHITE)
    message_label = _label(career_panel, "Nova engine. Nova fase.", Vector2(755, 500), Vector2(420, 55), 18, Color(0.80,0.86,0.88))

    hud = Control.new()
    hud.set_anchors_preset(Control.PRESET_FULL_RECT)
    ui_layer.add_child(hud)
    hud.visible = false
    score_label = _label(hud, "AUR 0 - 0 VIL", Vector2(500, 18), Vector2(300, 52), 26, Color.WHITE)
    clock_label = _label(hud, "00'", Vector2(410, 25), Vector2(90, 40), 19, Color(0.72,1.0,0.18))
    _label(hud, "futREAL 5 • GODOT", Vector2(24, 22), Vector2(280, 38), 20, Color.WHITE)

    stamina_bar = ProgressBar.new()
    stamina_bar.position = Vector2(1015, 26)
    stamina_bar.size = Vector2(220, 24)
    stamina_bar.min_value = 0
    stamina_bar.max_value = 100
    stamina_bar.value = 100
    hud.add_child(stamina_bar)

    _hold_button(hud, "◀", Vector2(35, 565), Vector2(82, 82), _left_down, _left_up)
    _hold_button(hud, "▶", Vector2(205, 565), Vector2(82, 82), _right_down, _right_up)
    _hold_button(hud, "▲", Vector2(120, 485), Vector2(82, 82), _up_down, _up_up)
    _hold_button(hud, "▼", Vector2(120, 645), Vector2(82, 68), _down_down, _down_up)

    var shoot := _button(hud, "CHUTE", Vector2(1110, 520), Vector2(130, 92))
    shoot.pressed.connect(_shoot)
    var pass_button := _button(hud, "PASSE", Vector2(960, 595), Vector2(125, 82))
    pass_button.pressed.connect(_pass_ball)
    _hold_button(hud, "SPRINT", Vector2(945, 475), Vector2(125, 82), _sprint_down, _sprint_up)

func _start_match() -> void:
    career_panel.visible = false
    hud.visible = true
    match_running = true
    match_clock = 0.0
    home_score = 0
    away_score = 0
    stamina = 100.0
    message_label.text = "Em campo"
    _reset_kickoff()

func _finish_match() -> void:
    match_running = false
    season_games += 1
    if home_score > away_score:
        season_wins += 1
        xp += 120
        fans += 85
        message_label.text = "Vitória! +120 XP • +85 fãs"
    elif home_score == away_score:
        xp += 60
        fans += 25
        message_label.text = "Empate • +60 XP"
    else:
        xp += 30
        message_label.text = "Derrota • +30 XP"
    while xp >= 300:
        xp -= 300
        ovr = min(99, ovr + 1)
    _show_career()

func _show_career() -> void:
    match_running = false
    hud.visible = false if hud else false
    if career_panel:
        career_panel.visible = true
        career_ovr_label.text = "OVR %d" % ovr
        career_stats_label.text = "TEMPORADA 1\nJogos: %d    Gols: %d    Vitórias: %d\nFãs: %d    XP: %d/300" % [season_games, season_goals, season_wins, fans, xp]

func _update_player(delta: float) -> void:
    var move := Vector3.ZERO
    if move_left or Input.is_key_pressed(KEY_A): move.x -= 1.0
    if move_right or Input.is_key_pressed(KEY_D): move.x += 1.0
    if move_up or Input.is_key_pressed(KEY_W): move.z -= 1.0
    if move_down or Input.is_key_pressed(KEY_S): move.z += 1.0
    if move.length() > 1.0:
        move = move.normalized()
    var sprinting := (sprint_down or Input.is_key_pressed(KEY_SHIFT)) and stamina > 1.0
    var speed := 12.5 if sprinting else 8.2
    if sprinting and move.length() > 0.1:
        stamina = max(0.0, stamina - delta * 15.0)
    else:
        stamina = min(100.0, stamina + delta * 7.0)
    player.position += move * speed * delta
    player.position.x = clamp(player.position.x, -FIELD_X, FIELD_X)
    player.position.z = clamp(player.position.z, -FIELD_Z, FIELD_Z)
    if move.length() > 0.05:
        player.rotation.y = lerp_angle(player.rotation.y, atan2(-move.x, -move.z), delta * 8.0)
    if user_has_ball:
        ball.position = player.position + Vector3(0, 0.43, -1.15)

func _update_ball(delta: float) -> void:
    if user_has_ball or opponent_has_ball:
        return
    ball_velocity.y -= 15.0 * delta
    ball.position += ball_velocity * delta
    ball_velocity.x *= pow(0.986, delta * 60.0)
    ball_velocity.z *= pow(0.986, delta * 60.0)
    if ball.position.y < 0.43:
        ball.position.y = 0.43
        if abs(ball_velocity.y) > 0.8:
            ball_velocity.y = -ball_velocity.y * 0.32
        else:
            ball_velocity.y = 0
    if player.position.distance_to(ball.position) < 1.45 and ball.position.y < 1.2:
        user_has_ball = true
        opponent_has_ball = false
        opponent_owner = -1

func _update_teammates(delta: float) -> void:
    var bases := [Vector3(-18,0,18), Vector3(18,0,16), Vector3(-10,0,-3), Vector3(12,0,-7)]
    for i in range(teammates.size()):
        var target := bases[i] + Vector3(player.position.x * 0.15, 0, (player.position.z - 26.0) * 0.30)
        _move_toward_3d(teammates[i], target, 5.0, delta)
        if not user_has_ball and not opponent_has_ball and teammates[i].position.distance_to(ball.position) < 1.3:
            var dir := (player.position - teammates[i].position).normalized()
            ball.position = teammates[i].position + Vector3(0,0.43,0)
            ball_velocity = dir * 16.0 + Vector3(0, 1.0, 0)

func _update_opponents(delta: float) -> void:
    var nearest := 0
    var nearest_d := 9999.0
    for i in range(opponents.size()):
        var d := opponents[i].position.distance_to(ball.position)
        if d < nearest_d:
            nearest_d = d
            nearest = i
    for i in range(opponents.size()):
        var target: Vector3
        if opponent_has_ball and i == opponent_owner:
            target = Vector3(clamp(opponents[i].position.x * 0.96, -10.0, 10.0), 0, 49.0)
            _move_toward_3d(opponents[i], target, 7.6, delta)
            ball.position = opponents[i].position + Vector3(0,0.43,1.0)
        elif i == nearest or (user_has_ball and i < 2):
            target = player.position if user_has_ball else ball.position
            _move_toward_3d(opponents[i], Vector3(target.x,0,target.z), 7.0, delta)
        else:
            target = Vector3((i - 2) * 10.0 + player.position.x * 0.12, 0, -18.0 + i * 3.0 + (player.position.z - 26.0) * 0.20)
            _move_toward_3d(opponents[i], target, 4.0, delta)

        if user_has_ball and opponents[i].position.distance_to(player.position) < 1.25 and randf() < delta * 1.6:
            user_has_ball = false
            opponent_has_ball = true
            opponent_owner = i
        elif not user_has_ball and not opponent_has_ball and opponents[i].position.distance_to(ball.position) < 1.25 and ball.position.y < 1.1:
            opponent_has_ball = true
            opponent_owner = i

    if opponent_has_ball and opponent_owner >= 0 and opponents[opponent_owner].position.z > 40:
        var dir := (Vector3(0, 1.2, 53.0) - ball.position).normalized()
        opponent_has_ball = false
        ball_velocity = dir * 24.0 + Vector3(0, 4.0, 0)
        opponent_owner = -1

func _check_goal() -> void:
    if ball.position.z < -52.8 and abs(ball.position.x) < GOAL_HALF and ball.position.y < 2.7:
        home_score += 1
        season_goals += 1
        _reset_kickoff()
    elif ball.position.z > 52.8 and abs(ball.position.x) < GOAL_HALF and ball.position.y < 2.7:
        away_score += 1
        _reset_kickoff()
    elif abs(ball.position.x) > 38 or abs(ball.position.z) > 58:
        _reset_kickoff()

func _shoot() -> void:
    if not match_running or not user_has_ball:
        return
    user_has_ball = false
    var target := Vector3(clamp(player.position.x * 0.12, -5.5, 5.5), 1.0, -53.0)
    var dir := (target - ball.position).normalized()
    ball_velocity = dir * 29.0 + Vector3(0, 5.0, 0)

func _pass_ball() -> void:
    if not match_running or not user_has_ball:
        return
    var best: Node3D = teammates[0]
    var best_score := 9999.0
    for mate in teammates:
        var score := player.position.distance_to(mate.position)
        if mate.position.z > player.position.z:
            score += 14.0
        if score < best_score:
            best_score = score
            best = mate
    user_has_ball = false
    var dir := (best.position - ball.position).normalized()
    ball_velocity = dir * 20.0 + Vector3(0, 1.2, 0)

func _update_camera(delta: float) -> void:
    var desired := player.position + Vector3(player.position.x * -0.12, 14.5, 22.0)
    camera.position = camera.position.lerp(desired, min(1.0, delta * 3.5))
    var look_target := player.position + Vector3(0, 0.7, -13.0)
    camera.look_at(look_target, Vector3.UP)

func _reset_kickoff() -> void:
    player.position = Vector3(0,0,26)
    ball.position = Vector3(0,0.43,24.6)
    ball_velocity = Vector3.ZERO
    user_has_ball = true
    opponent_has_ball = false
    opponent_owner = -1
    var m := [Vector3(-18,0,20), Vector3(18,0,18), Vector3(-11,0,0), Vector3(13,0,-5)]
    var o := [Vector3(-19,0,-17), Vector3(-7,0,-8), Vector3(8,0,-11), Vector3(19,0,-20), Vector3(0,0,-33)]
    for i in range(teammates.size()): teammates[i].position = m[i]
    for i in range(opponents.size()): opponents[i].position = o[i]

func _update_hud() -> void:
    score_label.text = "AUR %d  -  %d VIL" % [home_score, away_score]
    clock_label.text = "%02d'" % int(match_clock)
    stamina_bar.value = stamina

func _move_toward_3d(node: Node3D, target: Vector3, speed: float, delta: float) -> void:
    var delta_pos := target - node.position
    delta_pos.y = 0
    if delta_pos.length() > 0.08:
        node.position += delta_pos.normalized() * min(delta_pos.length(), speed * delta)
        node.rotation.y = lerp_angle(node.rotation.y, atan2(-delta_pos.x, -delta_pos.z), delta * 6.0)
    node.position.x = clamp(node.position.x, -FIELD_X, FIELD_X)
    node.position.z = clamp(node.position.z, -FIELD_Z, FIELD_Z)

func _add_box(parent: Node, size: Vector3, pos: Vector3, color: Color) -> MeshInstance3D:
    var mesh_instance := MeshInstance3D.new()
    var box := BoxMesh.new()
    box.size = size
    mesh_instance.mesh = box
    mesh_instance.position = pos
    mesh_instance.material_override = _mat(color)
    parent.add_child(mesh_instance)
    return mesh_instance

func _mat(color: Color) -> StandardMaterial3D:
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    material.roughness = 0.76
    return material

func _label(parent: Control, text_value: String, pos: Vector2, size_value: Vector2, font_size: int, color: Color) -> Label:
    var label := Label.new()
    label.text = text_value
    label.position = pos
    label.size = size_value
    label.add_theme_font_size_override("font_size", font_size)
    label.add_theme_color_override("font_color", color)
    parent.add_child(label)
    return label

func _button(parent: Control, text_value: String, pos: Vector2, size_value: Vector2) -> Button:
    var b := Button.new()
    b.text = text_value
    b.position = pos
    b.size = size_value
    b.add_theme_font_size_override("font_size", 18)
    parent.add_child(b)
    return b

func _hold_button(parent: Control, text_value: String, pos: Vector2, size_value: Vector2, down: Callable, up: Callable) -> Button:
    var b := _button(parent, text_value, pos, size_value)
    b.button_down.connect(down)
    b.button_up.connect(up)
    return b

func _left_down() -> void: move_left = true
func _left_up() -> void: move_left = false
func _right_down() -> void: move_right = true
func _right_up() -> void: move_right = false
func _up_down() -> void: move_up = true
func _up_up() -> void: move_up = false
func _down_down() -> void: move_down = true
func _down_up() -> void: move_down = false
func _sprint_down() -> void: sprint_down = true
func _sprint_up() -> void: sprint_down = false
