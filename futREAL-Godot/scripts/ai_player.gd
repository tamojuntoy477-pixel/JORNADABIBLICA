extends CharacterBody3D
class_name FutAIPlayer

var ball: RigidBody3D
var home := Vector3.ZERO
var attack_dir := 1.0
var shirt_color := Color(0.9, 0.15, 0.15)
var anim_time := 0.0
var left_leg: MeshInstance3D
var right_leg: MeshInstance3D
var left_arm: MeshInstance3D
var right_arm: MeshInstance3D

func setup(p_ball: RigidBody3D, p_home: Vector3, p_attack_dir: float, p_color: Color) -> void:
	ball = p_ball
	home = p_home
	attack_dir = p_attack_dir
	shirt_color = p_color
	position = home

func _ready() -> void:
	var cs := CollisionShape3D.new()
	var cap := CapsuleShape3D.new()
	cap.radius = 0.47
	cap.height = 1.85
	cs.shape = cap
	cs.position.y = 0.93
	add_child(cs)
	_build_body()

func _physics_process(delta: float) -> void:
	if not is_instance_valid(ball):
		return
	var d := global_position.distance_to(ball.global_position)
	var target := home
	if d < 20.0:
		target = Vector3(ball.global_position.x, 0, ball.global_position.z)
	else:
		target.z += clamp(ball.global_position.z * 0.16, -8.0, 8.0)
		target.x += clamp(ball.global_position.x * 0.12, -5.0, 5.0)
	var dir := target - global_position
	dir.y = 0
	var moving := dir.length() > 0.35
	if moving:
		dir = dir.normalized()
		velocity.x = dir.x * 6.4
		velocity.z = dir.z * 6.4
		rotation.y = atan2(-dir.x, -dir.z)
		anim_time += delta * 7.0
	else:
		velocity.x = move_toward(velocity.x, 0.0, 15.0 * delta)
		velocity.z = move_toward(velocity.z, 0.0, 15.0 * delta)
	if not is_on_floor():
		velocity.y -= 14.0 * delta
	else:
		velocity.y = 0
	move_and_slide()
	global_position.x = clamp(global_position.x, -32.0, 32.0)
	global_position.z = clamp(global_position.z, -50.0, 50.0)
	_animate(moving)

	if d < 1.75 and abs(ball.global_position.y) < 1.25 and ball.has_method("kick"):
		var goal := Vector3(0, 0, 54.0 * attack_dir)
		var kick_dir := (goal - global_position).normalized()
		ball.kick(kick_dir, 17.0, 1.3)

func _mat(c: Color) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = c
	m.roughness = 0.58
	return m

func _box(size: Vector3, c: Color) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = _mat(c)
	return mi

func _build_body() -> void:
	var skin := Color(0.76, 0.54, 0.34)
	var shorts := Color(0.05,0.06,0.08)
	var torso := _box(Vector3(1.0,1.32,.62),shirt_color)
	torso.position.y=1.60
	add_child(torso)
	var head := MeshInstance3D.new()
	var sm := SphereMesh.new()
	sm.radius=.35
	sm.height=.7
	sm.radial_segments=14
	sm.rings=7
	head.mesh=sm
	head.material_override=_mat(skin)
	head.position.y=2.52
	add_child(head)
	left_arm=_box(Vector3(.23,1.0,.23),shirt_color)
	right_arm=_box(Vector3(.23,1.0,.23),shirt_color)
	left_arm.position=Vector3(-.64,1.56,0)
	right_arm.position=Vector3(.64,1.56,0)
	add_child(left_arm)
	add_child(right_arm)
	left_leg=_box(Vector3(.29,1.05,.33),shorts)
	right_leg=_box(Vector3(.29,1.05,.33),shorts)
	left_leg.position=Vector3(-.27,.53,0)
	right_leg.position=Vector3(.27,.53,0)
	add_child(left_leg)
	add_child(right_leg)

func _animate(moving: bool) -> void:
	var swing := sin(anim_time) * (0.62 if moving else 0.05)
	left_leg.rotation.x=swing
	right_leg.rotation.x=-swing
	left_arm.rotation.x=-swing*.75
	right_arm.rotation.x=swing*.75
