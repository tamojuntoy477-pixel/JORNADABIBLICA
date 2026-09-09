extends Control
class_name VirtualJoystick

signal changed(value: Vector2)

var active_pointer := -1
var value := Vector2.ZERO
var knob := Vector2.ZERO

func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_STOP
	queue_redraw()

func _gui_input(event: InputEvent) -> void:
	var center: Vector2 = size * 0.5
	var radius: float = float(min(size.x, size.y)) * 0.36
	if event is InputEventScreenTouch:
		if event.pressed and active_pointer == -1:
			active_pointer = event.index
			_update_value(event.position, center, radius)
		elif not event.pressed and event.index == active_pointer:
			active_pointer = -1
			value = Vector2.ZERO
			knob = Vector2.ZERO
			changed.emit(value)
			queue_redraw()
	elif event is InputEventScreenDrag and event.index == active_pointer:
		_update_value(event.position, center, radius)
	elif event is InputEventMouseButton:
		if event.button_index == MOUSE_BUTTON_LEFT:
			if event.pressed:
				active_pointer = 999
				_update_value(event.position, center, radius)
			else:
				active_pointer = -1
				value = Vector2.ZERO
				knob = Vector2.ZERO
				changed.emit(value)
				queue_redraw()
	elif event is InputEventMouseMotion and active_pointer == 999:
		_update_value(event.position, center, radius)

func _update_value(pos: Vector2, center: Vector2, radius: float) -> void:
	var delta: Vector2 = pos - center
	if delta.length() > radius:
		delta = delta.normalized() * radius
	knob = delta
	value = delta / max(radius, 1.0)
	changed.emit(value)
	queue_redraw()

func _draw() -> void:
	var center: Vector2 = size * 0.5
	var radius: float = float(min(size.x, size.y)) * 0.36
	draw_circle(center, radius, Color(1,1,1,0.12))
	draw_circle(center, radius * 0.64, Color(0.04,0.10,0.07,0.32))
	draw_circle(center + knob, radius * 0.32, Color(0.78,1.0,0.30,0.88))
	draw_arc(center, radius, 0, TAU, 48, Color(1,1,1,0.28), 3.0)
