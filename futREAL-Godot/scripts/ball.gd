extends RigidBody3D
class_name FutBall

func _ready() -> void:
	mass = 0.43
	linear_damp = 0.65
	angular_damp = 0.18
	continuous_cd = true
	contact_monitor = true
	max_contacts_reported = 8

	var shape := CollisionShape3D.new()
	var sphere_shape := SphereShape3D.new()
	sphere_shape.radius = 0.45
	shape.shape = sphere_shape
	add_child(shape)

	var mesh_instance := MeshInstance3D.new()
	var sphere := SphereMesh.new()
	sphere.radius = 0.45
	sphere.height = 0.9
	sphere.radial_segments = 20
	sphere.rings = 10
	mesh_instance.mesh = sphere
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.96, 0.96, 0.96)
	mat.metallic = 0.05
	mat.roughness = 0.42
	mesh_instance.material_override = mat
	add_child(mesh_instance)

func reset_ball(pos: Vector3) -> void:
	freeze = true
	global_position = pos
	linear_velocity = Vector3.ZERO
	angular_velocity = Vector3.ZERO
	freeze = false
	sleeping = false

func kick(direction: Vector3, power: float, lift: float) -> void:
	var dir := direction.normalized()
	linear_velocity = Vector3(dir.x * power, lift, dir.z * power)
	angular_velocity = Vector3(-dir.z, 0.0, dir.x) * power * 0.5
	sleeping = false
