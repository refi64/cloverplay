#include <algorithm>
#include <cstdint>
#include <linux/input-event-codes.h>

#include "gamepad.h"

namespace {

constexpr UinputConnection::UsbDeviceProperties kStadiaProperties{0x18d1, 0x9400, 0x111,
                                                                  "Google Inc. Stadia Controller"};

constexpr std::int32_t kStadiaAxisFlat = 15;

constexpr std::int32_t kStadiaJoystickMin = 1;
constexpr std::int32_t kStadiaJoystickMax = 255;

constexpr std::int32_t kStadiaTriggerMin = 0;
constexpr std::int32_t kStadiaTriggerMax = 255;

constexpr std::int32_t kStadiaDpadMin = -1;
constexpr std::int32_t kStadiaDpadMax = 1;

absl::btree_map<Gamepad::Button, std::uint16_t> kStadiaButtons{
    {Gamepad::Button::kA, BTN_A},
    {Gamepad::Button::kB, BTN_B},
    {Gamepad::Button::kX, BTN_X},
    {Gamepad::Button::kY, BTN_Y},

    {Gamepad::Button::kL1, BTN_TL},
    {Gamepad::Button::kR1, BTN_TR},
    {Gamepad::Button::kL3, BTN_THUMBL},
    {Gamepad::Button::kR3, BTN_THUMBR},

    {Gamepad::Button::kStart, BTN_START},
    {Gamepad::Button::kSelect, BTN_SELECT},

    {Gamepad::Button::kHome, BTN_MODE},

    {Gamepad::Button::kStadiaAssistant, BTN_TRIGGER_HAPPY1},
    {Gamepad::Button::kStadiaScreenshot, BTN_TRIGGER_HAPPY2},
};

absl::btree_map<Gamepad::JoystickWithAxis, std::uint16_t> kStadiaJoystickAxes{
    {Gamepad::JoystickWithAxis::kLeftX, ABS_X},
    {Gamepad::JoystickWithAxis::kLeftY, ABS_Y},
    {Gamepad::JoystickWithAxis::kRightX, ABS_Z},
    {Gamepad::JoystickWithAxis::kRightY, ABS_RZ},
};

template <typename I, typename F>
I rounding_cast(F value) {
  return static_cast<I>(std::round(value));
}

}  // namespace

// static
Gamepad::JoystickWithAxis Gamepad::GetJoystickWithAxis(Joystick joystick, JoystickAxis axis) {
  return magic_enum::enum_value<JoystickWithAxis>(magic_enum::enum_integer(joystick) * 2 +
                                                  magic_enum::enum_integer(axis));
}

// static
StatusOr<StadiaGamepad> StadiaGamepad::Create(UinputConnection* connection) {
  UinputUsbController::AxisMap axes;

  std::uint16_t joystick_axes[] = {ABS_X, ABS_Y, ABS_Z, ABS_RZ};
  std::uint16_t trigger_axes[] = {ABS_GAS, ABS_BRAKE};
  std::uint16_t dpad_axes[] = {ABS_HAT0X, ABS_HAT0Y};

  for (std::uint16_t axis : joystick_axes) {
    axes[axis][UinputConnection::kAxisProp_Min] = kStadiaJoystickMin;
    axes[axis][UinputConnection::kAxisProp_Max] = kStadiaJoystickMax;
    axes[axis][UinputConnection::kAxisProp_Flat] = kStadiaAxisFlat;
  }

  for (std::uint16_t axis : trigger_axes) {
    axes[axis][UinputConnection::kAxisProp_Min] = kStadiaTriggerMin;
    axes[axis][UinputConnection::kAxisProp_Max] = kStadiaTriggerMax;
    axes[axis][UinputConnection::kAxisProp_Flat] = kStadiaAxisFlat;
  }

  for (std::uint16_t axis : dpad_axes) {
    axes[axis][UinputConnection::kAxisProp_Min] = kStadiaDpadMin;
    axes[axis][UinputConnection::kAxisProp_Max] = kStadiaDpadMax;
  }

  UinputUsbController::ButtonSet buttons;
  for (const auto& pair : kStadiaButtons) {
    buttons.insert(pair.second);
  }

  UinputUsbController controller = TRY_STATUS_OR(UinputUsbController::Create(
      connection, kStadiaProperties, std::move(axes), std::move(buttons)));
  return StadiaGamepad(std::move(controller));
}

absl::Status StadiaGamepad::QueueButtonState(Button button, bool pressed) /*override*/ {
  return controller()->QueueButtonEvent(kStadiaButtons[button], pressed);
}

absl::Status StadiaGamepad::QueueJoystickState(Joystick joystick, JoystickAxis axis,
                                               double position) /*override*/ {
  std::uint16_t axis_code = kStadiaJoystickAxes[GetJoystickWithAxis(joystick, axis)];

  // Position ranges from [-1,1], so add 1 and divide by 2 to make it [0,1], then a clean
  // multiply by [kStadiaJoystickMin,kStadiaJoystickMax] will scale it.
  std::int32_t scaled_position = rounding_cast<std::int32_t>(
      ((position + 1) / 2) * (kStadiaJoystickMax - kStadiaJoystickMin) + kStadiaJoystickMin);
  // Sanity check.
  scaled_position = std::clamp(scaled_position, kStadiaJoystickMin, kStadiaJoystickMax);

  return controller()->QueueAxisEvent(axis_code, scaled_position);
}

absl::Status StadiaGamepad::QueueTriggerState(Trigger trigger, double position) /*override*/ {
  std::uint16_t axis_code;
  switch (trigger) {
  case Trigger::kLeft:
    axis_code = ABS_BRAKE;
    break;
  case Trigger::kRight:
    axis_code = ABS_GAS;
    break;
  }

  std::int32_t scaled_position = rounding_cast<std::int32_t>(position * kStadiaTriggerMax);

  return controller()->QueueAxisEvent(axis_code, scaled_position);
}

absl::Status StadiaGamepad::QueueDpadState(DpadDirection direction, bool pressed) /*override*/ {
  std::uint16_t axis_code;
  std::int32_t position;

  switch (direction) {
  case DpadDirection::kNorth:
    axis_code = ABS_HAT0Y;
    position = -1;
    break;
  case DpadDirection::kSouth:
    axis_code = ABS_HAT0Y;
    position = 1;
    break;
  case DpadDirection::kEast:
    axis_code = ABS_HAT0X;
    position = 1;
    break;
  case DpadDirection::kWest:
    axis_code = ABS_HAT0X;
    position = -1;
  }

  if (!pressed) {
    position = 0;
  }

  return controller()->QueueAxisEvent(axis_code, position);
}
