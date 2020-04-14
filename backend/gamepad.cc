#include <algorithm>
#include <cstdint>
#include <linux/input-event-codes.h>

#include "gamepad.h"

namespace {

constexpr UinputConnection::UsbDeviceProperties kStadiaProperties{0x18d1, 0x9400, 0x111,
                                                                  "Google Inc. Stadia Controller"};

constexpr UinputConnection::UsbDeviceProperties kXbox360Properties{0x045e, 0x02ea, 0x301,
                                                                   "Microsoft X-Box One S pad"};

constexpr std::int32_t kCommonTriggerMin = 0;

constexpr std::int32_t kCommonDpadMin = -1;
constexpr std::int32_t kCommonDpadMax = +1;

constexpr std::int32_t kStadiaAxisFlat = 15;

constexpr std::int32_t kStadiaJoystickMin = 1;
constexpr std::int32_t kStadiaJoystickMax = 255;

constexpr std::int32_t kStadiaTriggerMax = 255;

constexpr std::int32_t kXbox360JoystickFlat = 128;
constexpr std::int32_t kXbox360JoystickFuzz = 16;
constexpr std::int32_t kXbox360JoystickMin = -32768;
constexpr std::int32_t kXbox360JoystickMax = +32767;

constexpr std::int32_t kXbox360TriggerMax = 1023;

absl::flat_hash_map<Gamepad::Button, std::uint16_t> kCommonButtons{
    {Gamepad::Button::kA, BTN_A},         {Gamepad::Button::kB, BTN_B},
    {Gamepad::Button::kX, BTN_X},         {Gamepad::Button::kY, BTN_Y},

    {Gamepad::Button::kL1, BTN_TL},       {Gamepad::Button::kR1, BTN_TR},
    {Gamepad::Button::kL3, BTN_THUMBL},   {Gamepad::Button::kR3, BTN_THUMBR},

    {Gamepad::Button::kStart, BTN_START}, {Gamepad::Button::kSelect, BTN_SELECT},

    {Gamepad::Button::kHome, BTN_MODE},
};

absl::flat_hash_map<Gamepad::Button, std::uint16_t> kStadiaSpecificButtons{
    {Gamepad::Button::kStadiaAssistant, BTN_TRIGGER_HAPPY1},
    {Gamepad::Button::kStadiaScreenshot, BTN_TRIGGER_HAPPY2},
};

absl::flat_hash_map<Gamepad::JoystickWithAxis, std::uint16_t> kCommonJoystickAxes{
    {Gamepad::JoystickWithAxis::kLeftX, ABS_X},
    {Gamepad::JoystickWithAxis::kLeftY, ABS_Y},
};

absl::flat_hash_map<Gamepad::JoystickWithAxis, std::uint16_t> kStadiaSpecificJoystickAxes{
    {Gamepad::JoystickWithAxis::kRightX, ABS_Z},
    {Gamepad::JoystickWithAxis::kRightY, ABS_RZ},
};

absl::flat_hash_map<Gamepad::JoystickWithAxis, std::uint16_t> kXbox360SpecificJoystickAxes{
    {Gamepad::JoystickWithAxis::kRightX, ABS_RX},
    {Gamepad::JoystickWithAxis::kRightY, ABS_RY},
};

absl::flat_hash_map<Gamepad::Trigger, std::uint16_t> kStadiaSpecificTriggers{
    {Gamepad::Trigger::kLeft, ABS_BRAKE},
    {Gamepad::Trigger::kRight, ABS_GAS},
};

absl::flat_hash_map<Gamepad::Trigger, std::uint16_t> kXbox360SpecificTriggers{
    {Gamepad::Trigger::kLeft, ABS_Z},
    {Gamepad::Trigger::kRight, ABS_RZ},
};

absl::flat_hash_map<Gamepad::DpadDirection, GamepadImplHelper::Configuration::DpadValue>
    kCommonDpad{
        {Gamepad::DpadDirection::kNorth, {ABS_HAT0Y, kCommonDpadMin}},
        {Gamepad::DpadDirection::kSouth, {ABS_HAT0Y, kCommonDpadMax}},
        {Gamepad::DpadDirection::kWest, {ABS_HAT0X, kCommonDpadMin}},
        {Gamepad::DpadDirection::kEast, {ABS_HAT0X, kCommonDpadMax}},
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

StatusOr<UinputUsbController>
GamepadImplHelper::Configuration::CreateController(UinputConnection* connection) {
  UinputUsbController::AxisMap axes;
  UinputUsbController::ButtonSet button_set;

  for (const auto [_, axis] : joysticks.map) {
    axes[axis] = joysticks.properties;
  }

  for (const auto [_, axis] : triggers.map) {
    axes[axis] = triggers.properties;
  }

  for (const auto [_, dpad_button] : dpad.map) {
    axes[dpad_button.axis] = dpad.properties;
  }

  for (const auto [_, button] : buttons.map) {
    button_set.insert(button);
  }

  return UinputUsbController::Create(connection, properties, std::move(axes),
                                     std::move(button_set));
}

absl::Status GamepadImplHelper::QueueButtonState(Button button, bool pressed) /*override*/ {
  return controller()->QueueButtonEvent(configuration_.buttons.map[button], pressed);
}

absl::Status GamepadImplHelper::QueueTriggerState(Trigger trigger, double position) /*override*/ {
  std::uint16_t axis_code = configuration_.triggers.map[trigger];
  std::int32_t scaled_position = rounding_cast<std::int32_t>(
      position * configuration_.triggers.properties[UinputConnection::kAxisProp_Max]);

  return controller()->QueueAxisEvent(axis_code, scaled_position);
}

absl::Status GamepadImplHelper::QueueDpadState(DpadDirection direction, bool pressed) /*override*/ {
  Configuration::DpadValue dpad = configuration_.dpad.map[direction];
  std::uint16_t axis_code = dpad.axis;
  std::int32_t position = pressed ? dpad.value : 0;

  return controller()->QueueAxisEvent(axis_code, position);
}

// static
StatusOr<StadiaGamepad> StadiaGamepad::Create(UinputConnection* connection) {
  UinputUsbController::AxisMap axes;
  Configuration configuration;

  configuration.properties = kStadiaProperties;

  configuration.buttons.map = kCommonButtons;
  configuration.buttons.map.merge(kStadiaSpecificButtons);

  configuration.joysticks.map = kCommonJoystickAxes;
  configuration.joysticks.map.merge(kStadiaSpecificJoystickAxes);

  configuration.joysticks.properties[UinputConnection::kAxisProp_Min] = kStadiaJoystickMin;
  configuration.joysticks.properties[UinputConnection::kAxisProp_Max] = kStadiaJoystickMax;
  configuration.joysticks.properties[UinputConnection::kAxisProp_Flat] = kStadiaAxisFlat;

  configuration.triggers.map = kStadiaSpecificTriggers;
  configuration.triggers.properties[UinputConnection::kAxisProp_Min] = kCommonTriggerMin;
  configuration.triggers.properties[UinputConnection::kAxisProp_Max] = kStadiaTriggerMax;
  configuration.triggers.properties[UinputConnection::kAxisProp_Flat] = kStadiaAxisFlat;

  configuration.dpad.map = kCommonDpad;
  configuration.dpad.properties[UinputConnection::kAxisProp_Min] = kCommonDpadMin;
  configuration.dpad.properties[UinputConnection::kAxisProp_Max] = kCommonDpadMax;

  UinputUsbController controller = TRY_STATUS_OR(configuration.CreateController(connection));
  return StadiaGamepad(std::move(controller), std::move(configuration));
}

absl::Status StadiaGamepad::QueueJoystickState(Joystick joystick, JoystickAxis axis,
                                               double position) /*override*/ {
  std::uint16_t axis_code = configuration_.joysticks.map[GetJoystickWithAxis(joystick, axis)];

  // Position ranges from [-1,1], so add 1 and divide by 2 to make it [0,1], then a clean
  // multiply by [kStadiaJoystickMin,kStadiaJoystickMax] will scale it.
  std::int32_t scaled_position = rounding_cast<std::int32_t>(
      ((position + 1) / 2) * (kStadiaJoystickMax - kStadiaJoystickMin) + kStadiaJoystickMin);
  // Sanity check.
  scaled_position = std::clamp(scaled_position, kStadiaJoystickMin, kStadiaJoystickMax);

  return controller()->QueueAxisEvent(axis_code, scaled_position);
}

// static
StatusOr<Xbox360Gamepad> Xbox360Gamepad::Create(UinputConnection* connection) {
  UinputUsbController::AxisMap axes;
  Configuration configuration;

  configuration.properties = kXbox360Properties;

  configuration.buttons.map = kCommonButtons;

  configuration.joysticks.map = kCommonJoystickAxes;
  configuration.joysticks.map.merge(kXbox360SpecificJoystickAxes);

  configuration.joysticks.properties[UinputConnection::kAxisProp_Min] = kXbox360JoystickMin;
  configuration.joysticks.properties[UinputConnection::kAxisProp_Max] = kXbox360JoystickMax;
  configuration.joysticks.properties[UinputConnection::kAxisProp_Flat] = kXbox360JoystickFlat;
  configuration.joysticks.properties[UinputConnection::kAxisProp_Fuzz] = kXbox360JoystickFuzz;

  configuration.triggers.map = kXbox360SpecificTriggers;
  configuration.triggers.properties[UinputConnection::kAxisProp_Min] = kCommonTriggerMin;
  configuration.triggers.properties[UinputConnection::kAxisProp_Max] = kXbox360TriggerMax;

  configuration.dpad.map = kCommonDpad;
  configuration.dpad.properties[UinputConnection::kAxisProp_Min] = kCommonDpadMin;
  configuration.dpad.properties[UinputConnection::kAxisProp_Max] = kCommonDpadMax;

  UinputUsbController controller = TRY_STATUS_OR(configuration.CreateController(connection));
  return Xbox360Gamepad(std::move(controller), std::move(configuration));
}

absl::Status Xbox360Gamepad::QueueJoystickState(Joystick joystick, JoystickAxis axis,
                                                double position) /*override*/ {
  std::uint16_t axis_code = configuration_.joysticks.map[GetJoystickWithAxis(joystick, axis)];

  // Accomodate the right scale being ever-so-slightly larger than the left.
  std::int32_t scaled_position =
      position * (position >= 0 ? kXbox360JoystickMax : -kXbox360JoystickMin);
  // Sanity check.
  scaled_position = std::clamp(scaled_position, kXbox360JoystickMin, kXbox360JoystickMax);

  return controller()->QueueAxisEvent(axis_code, scaled_position);
}
