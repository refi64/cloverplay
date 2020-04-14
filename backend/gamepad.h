#pragma once

#include <array>

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

class StadiaGamepad : public Gamepad {
 public:
  static StatusOr<StadiaGamepad> Create(UinputConnection* connection);
  StadiaGamepad(StadiaGamepad&& other) = default;
  ~StadiaGamepad() {}

  absl::Status QueueButtonState(Button button, bool pressed) override;
  absl::Status QueueJoystickState(Joystick joystick, JoystickAxis axis, double position) override;
  absl::Status QueueTriggerState(Trigger trigger, double position) override;
  absl::Status QueueDpadState(DpadDirection direction, bool pressed) override;

 protected:
  StadiaGamepad(UinputUsbController controller) : Gamepad(std::move(controller)) {}
};
