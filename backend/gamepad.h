#pragma once

#include <array>
#include <cstdint>

#include "uinput.h"

class Gamepad {
 public:
  // Keep in sync with service.proto

  enum class Button {
    kA,
    kB,
    kX,
    kY,
    kL1,
    kL3,
    kR1,
    kR3,
    kStart,
    kSelect,
    kHome,

    kStadiaScreenshot,
    kStadiaAssistant
  };

  enum class JoystickAxis { kX, kY };
  enum class Joystick { kLeft, kRight };
  enum class Trigger { kLeft, kRight };
  enum class DpadDirection { kNorth, kSouth, kEast, kWest };

  // For simplicity.
  enum class JoystickWithAxis { kLeftX, kLeftY, kRightX, kRightY };
  static JoystickWithAxis GetJoystickWithAxis(Joystick joystick, JoystickAxis axis);

  Gamepad(Gamepad&& other) = default;
  virtual ~Gamepad() {}

  virtual absl::Status QueueButtonState(Button button, bool pressed) = 0;
  virtual absl::Status QueueJoystickState(Joystick joystick, JoystickAxis axis,
                                          double position) = 0;
  virtual absl::Status QueueTriggerState(Trigger trigger, double position) = 0;
  virtual absl::Status QueueDpadState(DpadDirection direction, bool pressed) = 0;

  absl::Status ReportEvents() { return controller_.ReportEvents(); }

  UinputConnection* connection() { return controller_.connection(); }

 protected:
  Gamepad(UinputUsbController controller) : controller_(std::move(controller)) {}
  UinputUsbController* controller() { return &controller_; }

 private:
  UinputUsbController controller_;
};

class GamepadImplHelper : public Gamepad {
 public:
  absl::Status QueueButtonState(Button button, bool pressed) override;
  absl::Status QueueTriggerState(Trigger trigger, double position) override;
  absl::Status QueueDpadState(DpadDirection direction, bool pressed) override;

  struct Configuration {
    template <typename T, typename V = std::uint16_t>
    struct Mapped {
      absl::flat_hash_map<T, V> map;
    };

    template <typename T, typename V = std::uint16_t>
    struct MappedWithAxes : Mapped<T, V> {
      UinputConnection::AxisProperties properties = UinputConnection::ZeroAxisProperties();
    };

    struct DpadValue {
      std::uint16_t axis;
      std::int32_t value;
    };

    UinputConnection::UsbDeviceProperties properties;

    Mapped<Gamepad::Button> buttons;
    MappedWithAxes<Gamepad::JoystickWithAxis> joysticks;
    MappedWithAxes<Gamepad::Trigger> triggers;
    MappedWithAxes<Gamepad::DpadDirection, DpadValue> dpad;

    StatusOr<UinputUsbController> CreateController(UinputConnection* connection);
  };

 protected:
  GamepadImplHelper(UinputUsbController controller, Configuration configuration)
      : Gamepad(std::move(controller)), configuration_(std::move(configuration)) {}

  Configuration configuration_;
};

class StadiaGamepad : public GamepadImplHelper {
 public:
  static StatusOr<StadiaGamepad> Create(UinputConnection* connection);
  StadiaGamepad(StadiaGamepad&& other) = default;
  ~StadiaGamepad() {}

  absl::Status QueueJoystickState(Joystick joystick, JoystickAxis axis, double position) override;

 protected:
  StadiaGamepad(UinputUsbController controller, GamepadImplHelper::Configuration configuration)
      : GamepadImplHelper(std::move(controller), std::move(configuration)) {}
};

class Xbox360Gamepad : public GamepadImplHelper {
 public:
  static StatusOr<Xbox360Gamepad> Create(UinputConnection* connection);
  Xbox360Gamepad(Xbox360Gamepad&& other) = default;
  ~Xbox360Gamepad() {}

  absl::Status QueueJoystickState(Joystick joystick, JoystickAxis axis, double position) override;

 protected:
  Xbox360Gamepad(UinputUsbController controller, GamepadImplHelper::Configuration configuration)
      : GamepadImplHelper(std::move(controller), std::move(configuration)) {}
};
