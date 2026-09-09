extends Node3D

var player: FutPlayer
var ball: FutBall
var camera: Camera3D
var camera_target := Vector3.ZERO
var home_score := 0
var away_score := 0
var minute := 0.0
var match_running := false
var career_matches := 0
var career_goals := 0
var score_label: Label
var clock_label: Label
var stamina_bar: ProgressBar
var menu_layer: CanvasLayer
var joy: VirtualJoystick

const TEAM_BLUE := Color(0.10, 0.62, 0.96)
const TEAM_RED := Color(0.92, 0.12, 0.15)

func _ready() -> void:
	_ensure_inputs()
	_load_career()
	_build_environment()
	_build_stadium()
	_build_pitch()
	_build_goals()
	_spawn_gameplay()
	_build_hud()
	_show_menu()

func _process(delta: float) -> void:
	if not match_running:
		return
	minute += delta * 0.78
	clock_label.text = "%02d'" % int(minute)
	stamina_bar.value = player.stamina
	_update_camera(delta)
	_check_goal()
	if minute >= 90.0:
		_finish_match()

func _ensure_inputs() -> void:
	var mapping := {
		"move_left": KEY_A,
		"move_right": KEY_D,
		"move_up": KEY_W,
		"move_down": KEY_S,
		"shoot": KEY_SPACE,
		"pass_ball": KEY_E,
		"sprint": KEY_SHIFT
	}
	for action_name in mapping.keys():
		if not InputMap.has_action(action_name):
			InputMap.add_action(action_name)
			var ev := InputEventKey.new()
			ev.physical_keycode = mapping[action_name]
			InputMap.action_add_event(action_name, ev)

func _build_environment() -> void:
	var world := WorldEnvironment.new()
	var env := Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color(0.12, 0.20, 0.32)
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.45, 0.52, 0.62)
	env.ambient_light_energy = 0.82
	env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
	world.environment = env
	add_child(world)

	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-52, -28, 0)
	sun.light_energy = 1.65
	sun.light_color = Color(1.0, 0.94, 0.84)
	sun.shadow_enabled = true
	sun.directional_shadow_max_distance = 125.0
	add_child(sun)

	for x in [-38.0, 38.0]:
		for z in [-45.0, 45.0]:
			var lamp := OmniLight3D.new()
			lamp.position = Vector3(x, 18, z)
			lamp.light_energy = 4.0
			lamp.omni_range = 48.0
			lamp.light_color = Color(0.88, 0.94, 1.0)
			add_child(lamp)

func _material(color: Color, roughness := 0.65, metallic := 0.0) -> StandardMaterial3D:
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	mat.roughness = roughness
	mat.metallic = metallic
	return mat

func _box(parent: Node, pos: Vector3, size: Vector3, color: Color, collision := false) -> Node3D:
	var root: Node3D
	if collision:
		var body := StaticBody3D.new()
		root = body
		var shape := CollisionShape3D.new()
		var bs := BoxShape3D.new()
		bs.size = size
		shape.shape = bs
		body.add_child(shape)
	else:
		root = Node3D.new()
	root.position = pos
	parent.add_child(root)
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = _material(color)
	mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_ON
	root.add_child(mi)
	return root

func _build_stadium() -> void:
	_box(self, Vector3(0, -1.0, 0), Vector3(92, 2, 132), Color(0.035, 0.045, 0.055))
	_box(self, Vector3(-44, 6, 0), Vector3(16, 12, 126), Color(0.11, 0.13, 0.17))
	_box(self, Vector3(44, 6, 0), Vector3(16, 12, 126), Color(0.11, 0.13, 0.17))
	_box(self, Vector3(0, 6, -63), Vector3(74, 12, 18), Color(0.11, 0.13, 0.17))
	_box(self, Vector3(0, 6, 63), Vector3(74, 12, 18), Color(0.11, 0.13, 0.17))
	for side in [-1, 1]:
		var band := 0
		for z in range(-50, 51, 8):
			var c := Color(0.08, 0.34, 0.58) if (band + side) % 2 == 0 else Color(0.52, 0.09, 0.18)
			_box(self, Vector3(39 * side, 5.5, z), Vector3(1.4, 5.5, 6.2), c)
			band += 1

func _build_pitch() -> void:
	_box(self, Vector3(0, -0.14, 0), Vector3(68, 0.20, 105), Color(0.03, 0.36, 0.10), true)
	for i in range(10):
		var z := -47.25 + i * 10.5
		var c := Color(0.035, 0.43, 0.13) if i % 2 == 0 else Color(0.028, 0.35, 0.105)
		_box(self, Vector3(0, -0.02, z), Vector3(68, 0.035, 10.5), c)
	var white := Color(0.94, 0.96, 0.94)
	_box(self, Vector3(0, 0.01, 0), Vector3(68, 0.025, 0.13), white)
	_box(self, Vector3(-34, 0.01, 0), Vector3(0.13, 0.025, 105), white)
	_box(self, Vector3(34, 0.01, 0), Vector3(0.13, 0.025, 105), white)
	_box(self, Vector3(0, 0.01, -52.5), Vector3(68, 0.025, 0.13), white)
	_box(self, Vector3(0, 0.01, 52.5), Vector3(68, 0.025, 0.13), white)
	for z_value in [-43.0, 43.0]:
		var z: float = float(z_value)
		_box(self, Vector3(-10, 0.01, z), Vector3(0.13, 0.025, 19), white)
		_box(self, Vector3(10, 0.01, z), Vector3(0.13, 0.025, 19), white)
		var end_z: float = z + (9.5 if z < 0.0 else -9.5)
		_box(self, Vector3(0, 0.01, end_z), Vector3(20, 0.025, 0.13), white)

func _build_goals() -> void:
	for z in [-53.2, 53.2]:
		_box(self, Vector3(-7.3, 1.25, z), Vector3(0.16, 2.5, 0.16), Color.WHITE)
		_box(self, Vector3(7.3, 1.25, z), Vector3(0.16, 2.5, 0.16), Color.WHITE)
		_box(self, Vector3(0, 2.5, z), Vector3(14.7, 0.16, 0.16), Color.WHITE)

func _spawn_gameplay() -> void:
	ball = FutBall.new()
	add_child(ball)
	ball.global_position = Vector3(0, 0.55, 27)

	player = FutPlayer.new()
	add_child(player)
	player.global_position = Vector3(0, 0, 31)
	player.setup(ball)

	var ai_script := preload("res://scripts/ai_player.gd")
	var homes := [Vector3(-20,0,-18), Vector3(-8,0,-8), Vector3(8,0,-8), Vector3(20,0,-18), Vector3(0,0,-32)]
	for h in homes:
		var ai = ai_script.new()
		ai.setup(ball, h, 1.0, TEAM_RED)
		add_child(ai)
	var mate_homes := [Vector3(-20,0,20), Vector3(20,0,18), Vector3(-11,0,2), Vector3(13,0,-5)]
	for h in mate_homes:
		var mate = ai_script.new()
		mate.setup(ball, h, -1.0, TEAM_BLUE)
		add_child(mate)

	camera = Camera3D.new()
	camera.current = true
	camera.fov = 55
	add_child(camera)
	camera.global_position = Vector3(0, 14, 46)
	camera.look_at(Vector3(0, 0, 12), Vector3.UP)

func _build_hud() -> void:
	var layer := CanvasLayer.new()
	add_child(layer)
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	layer.add_child(root)

	var score_panel := ColorRect.new()
	score_panel.color = Color(0.02, 0.04, 0.06, 0.82)
	score_panel.position = Vector2(458, 18)
	score_panel.size = Vector2(364, 70)
	root.add_child(score_panel)

	score_label = Label.new()
	score_label.text = "AUR  0  -  0  VIL"
	score_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	score_label.add_theme_font_size_override("font_size", 24)
	score_label.position = Vector2(40, 12)
	score_label.size = Vector2(284, 35)
	score_panel.add_child(score_label)

	clock_label = Label.new()
	clock_label.text = "00'"
	clock_label.add_theme_color_override("font_color", Color(0.78, 1, 0.28))
	clock_label.position = Vector2(16, 42)
	clock_label.size = Vector2(80, 24)
	score_panel.add_child(clock_label)

	var title := Label.new()
	title.text = "futREAL 5 • GODOT 3D"
	title.position = Vector2(24, 20)
	title.add_theme_font_size_override("font_size", 23)
	root.add_child(title)

	stamina_bar = ProgressBar.new()
	stamina_bar.position = Vector2(1000, 25)
	stamina_bar.size = Vector2(230, 22)
	stamina_bar.max_value = 100
	stamina_bar.value = 100
	stamina_bar.show_percentage = false
	root.add_child(stamina_bar)

	joy = VirtualJoystick.new()
	joy.position = Vector2(25, 435)
	joy.size = Vector2(260, 260)
	root.add_child(joy)
	joy.changed.connect(_on_joystick)

	var shoot := Button.new()
	shoot.text = "CHUTE"
	shoot.position = Vector2(1080, 500)
	shoot.size = Vector2(155, 115)
	shoot.add_theme_font_size_override("font_size", 20)
	root.add_child(shoot)
	shoot.pressed.connect(_shoot)

	var passb := Button.new()
	passb.text = "PASSE"
	passb.position = Vector2(900, 570)
	passb.size = Vector2(145, 95)
	passb.add_theme_font_size_override("font_size", 18)
	root.add_child(passb)
	passb.pressed.connect(_pass)

	var sprint := Button.new()
	sprint.text = "SPRINT"
	sprint.position = Vector2(925, 455)
	sprint.size = Vector2(130, 85)
	root.add_child(sprint)
	sprint.button_down.connect(func(): player.set_sprint(true))
	sprint.button_up.connect(func(): player.set_sprint(false))

func _show_menu() -> void:
	if is_instance_valid(menu_layer):
		menu_layer.queue_free()
	menu_layer = CanvasLayer.new()
	menu_layer.layer = 20
	add_child(menu_layer)
	var menu := Control.new()
	menu.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	menu_layer.add_child(menu)
	var bg := ColorRect.new()
	bg.color = Color(0.015, 0.04, 0.025, 0.96)
	bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	menu.add_child(bg)
	var title := Label.new()
	title.text = "futREAL"
	title.position = Vector2(80, 110)
	title.add_theme_font_size_override("font_size", 72)
	title.add_theme_color_override("font_color", Color(0.78, 1.0, 0.28))
	menu.add_child(title)
	var sub := Label.new()
	sub.text = "CARREIRA 3D • MOBILE FOOTBALL"
	sub.position = Vector2(86, 205)
	sub.add_theme_font_size_override("font_size", 24)
	menu.add_child(sub)
	var stats := Label.new()
	stats.text = "NOX JR.   |   Jogos %d   |   Gols %d" % [career_matches, career_goals]
	stats.position = Vector2(88, 282)
	stats.add_theme_font_size_override("font_size", 22)
	menu.add_child(stats)
	var play := Button.new()
	play.text = "JOGAR PRÓXIMA PARTIDA"
	play.position = Vector2(88, 370)
	play.size = Vector2(430, 90)
	play.add_theme_font_size_override("font_size", 22)
	menu.add_child(play)
	play.pressed.connect(_start_match)
	var note := Label.new()
	note.text = "Nova base Godot: física 3D, animações, câmera, IA e carreira salva."
	note.position = Vector2(88, 500)
	note.add_theme_font_size_override("font_size", 18)
	menu.add_child(note)

func _start_match() -> void:
	match_running = true
	minute = 0
	home_score = 0
	away_score = 0
	_refresh_score()
	_reset_positions()
	if is_instance_valid(menu_layer):
		menu_layer.queue_free()

func _on_joystick(v: Vector2) -> void:
	player.set_move(v)

func _shoot() -> void:
	player.shoot()

func _pass() -> void:
	player.pass_ball()

func _update_camera(delta: float) -> void:
	var desired := player.global_position + Vector3(0, 12.5, 17.5)
	camera.global_position = camera.global_position.lerp(desired, 1.0 - pow(0.025, delta))
	camera_target = camera_target.lerp(player.global_position + Vector3(0, 1.0, -9.0), 1.0 - pow(0.02, delta))
	camera.look_at(camera_target, Vector3.UP)

func _check_goal() -> void:
	if ball.global_position.z < -53.0 and abs(ball.global_position.x) < 7.2 and ball.global_position.y < 3.0:
		home_score += 1
		career_goals += 1
		_refresh_score()
		_reset_positions()
	elif ball.global_position.z > 53.0 and abs(ball.global_position.x) < 7.2 and ball.global_position.y < 3.0:
		away_score += 1
		_refresh_score()
		_reset_positions()
	elif abs(ball.global_position.x) > 40 or abs(ball.global_position.z) > 62:
		_reset_positions()

func _refresh_score() -> void:
	score_label.text = "AUR  %d  -  %d  VIL" % [home_score, away_score]

func _reset_positions() -> void:
	player.global_position = Vector3(0, 0, 31)
	player.velocity = Vector3.ZERO
	ball.reset_ball(Vector3(0, 0.55, 27))

func _finish_match() -> void:
	match_running = false
	career_matches += 1
	_save_career()
	_show_menu()

func _save_career() -> void:
	var cfg := ConfigFile.new()
	cfg.set_value("career", "matches", career_matches)
	cfg.set_value("career", "goals", career_goals)
	cfg.save("user://career.cfg")

func _load_career() -> void:
	var cfg := ConfigFile.new()
	if cfg.load("user://career.cfg") == OK:
		career_matches = int(cfg.get_value("career", "matches", 0))
		career_goals = int(cfg.get_value("career", "goals", 0))
