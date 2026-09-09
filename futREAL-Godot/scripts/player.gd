extends CharacterBody3D
class_name FutPlayer

var move_input := Vector2.ZERO
var sprinting := false
var stamina := 100.0
var ball: RigidBody3D
var body_root: Node3D
var left_arm: MeshInstance3D
var right_arm: MeshInstance3D
var left_leg: MeshInstance3D
var right_leg: MeshInstance3D
var anim_time := 0.0
var last_facing := Vector3(0, 0, -1)

const WALK_SPEED := 8.5
const SPRINT_SPEED := 12.8

func _ready() -> void:
	_build_collision()
	_build_body()

func setup(p_ball: RigidBody3D) -> void:
	ball = p_ball

func set_move(v: Vector2) -> void:
	move_input = v.limit_length(1.0)

func set_sprint(v: bool) -> void:
	sprinting = v

func _physics_process(delta: float) -> void:
	var kb := Input.get_vector("move_left", "move_right", "move_up", "move_down")
	var input_vec := move_input if move_input.length() > 0.05 else kb
	var moving := input_vec.length() > 0.05
	var can_sprint := sprinting or Input.is_action_pressed("sprint")
	var speed := WALK_SPEED
	if can_sprint and stamina > 1.0 and moving:
		speed = SPRINT_SPEED
		stamina = max(0.0, stamina - 17.0 * delta)
	else:
		stamina = min(100.0, stamina + 8.0 * delta)

	var dir := Vector3(input_vec.x, 0, input_vec.y)
	velocity.x = dir.x * speed
	velocity.z = dir.z * speed
	if not is_on_floor():
		velocity.y -= 14.0 * delta
	else:
		velocity.y = 0
	move_and_slide()
	global_position.x = clamp(global_position.x, -32.0, 32.0)
	global_position.z = clamp(global_position.z, -50.0, 50.0)

	if moving:
		last_facing = dir.normalized()
		rotation.y = atan2(-last_facing.x, -last_facing.z)
		anim_time += delta * (12.0 if speed > WALK_SPEED else 8.0)
	else:
		anim_time += delta * 2.0
	_animate_body(moving)

	if Input.is_action_just_pressed("shoot"):
		shoot()
	if Input.is_action_just_pressed("pass_ball"):
		pass_ball()

func shoot() -> void:
	if not is_instance_valid(ball): return
	if global_position.distance_to(ball.global_position) > 2.5: return
	var aim := last_facing
	if aim.length() < 0.1: aim = Vector3(0,0,-1)
	if ball.has_method("kick"):
		ball.kick(aim, 27.5, 6.2)

func pass_ball() -> void:
	if not is_instance_valid(ball): return
	if global_position.distance_to(ball.global_position) > 2.5: return
	var aim := last_facing
	if aim.length() < 0.1: aim = Vector3(0,0,-1)
	if ball.has_method("kick"):
		ball.kick(aim, 17.0, 1.5)

func _build_collision() -> void:
	var cs := CollisionShape3D.new()
	var cap := CapsuleShape3D.new()
	cap.radius = 0.48
	cap.height = 1.9
	cs.shape = cap
	cs.position.y = 0.95
	add_child(cs)

func _mat(color: Color, rough := 0.55) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = color
	m.roughness = rough
	return m

func _mesh_box(size: Vector3, color: Color) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = _mat(color)
	return mi

func _mesh_sphere(radius: float, color: Color) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var sm := SphereMesh.new()
	sm.radius = radius
	sm.height = radius * 2.0
	sm.radial_segments = 16
	sm.rings = 8
	mi.mesh = sm
	mi.material_override = _mat(color, 0.62)
	return mi

func _build_body() -> void:
	body_root = Node3D.new()
	add_child(body_root)
	var kit := Color(0.73, 1.0, 0.22)
	var dark := Color(0.04, 0.06, 0.07)
	var skin := Color(0.74, 0.50, 0.31)
	var torso := _mesh_box(Vector3(1.0, 1.35, 0.62), kit)
	torso.position.y = 1.62
	body_root.add_child(torso)
	var head := _mesh_sphere(0.36, skin)
	head.position.y = 2.55
	body_root.add_child(head)
	left_arm = _mesh_box(Vector3(0.24, 1.05, 0.24), kit)
	right_arm = _mesh_box(Vector3(0.24, 1.05, 0.24), kit)
	left_arm.position = Vector3(-0.65, 1.58, 0)
	right_arm.position = Vector3(0.65, 1.58, 0)
	body_root.add_child(left_arm); body_root.add_child(right_arm)
	left_leg = _mesh_box(Vector3(0.30, 1.08, 0.34), dark)
	right_leg = _mesh_box(Vector3(0.30, 1.08, 0.34), dark)
	left_leg.position = Vector3(-0.28, 0.54, 0)
	right_leg.position = Vector3(0.28, 0.54, 0)
	body_root.add_child(left_leg); body_root.add_child(right_leg)

	var shadow := Decal.new()
	shadow.size = Vector3(1.8, 2.0, 1.8)
	shadow.position.y = 0.05
	body_root.add_child(shadow)

func _animate_body(moving: bool) -> void:
	var swing := sin(anim_time) * (0.72 if moving else 0.08)
	left_leg.rotation.x = swing
	right_leg.rotation.x = -swing
	left_arm.rotation.x = -swing * 0.8
	right_arm.rotation.x = swing * 0.8
	body_root.position.y = abs(sin(anim_time * 2.0)) * (0.035 if moving else 0.01)
