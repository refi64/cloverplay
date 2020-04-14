#include "backend/gamepad.h"

absl::Status Run() {
  UinputConnection connection = TRY_STATUS_OR(UinputConnection::Connect());
  StadiaGamepad gamepad = TRY_STATUS_OR(StadiaGamepad::Create(&connection));

  fmt::print("Ready\n");
  std::cin.ignore();

  TRY_STATUS(gamepad.QueueButtonState(Gamepad::Button::kA, true));
  TRY_STATUS(gamepad.QueueButtonState(Gamepad::Button::kA, false));

  TRY_STATUS(gamepad.ReportEvents());

  TRY_STATUS(gamepad.QueueDpadState(Gamepad::DpadDirection::kNorth, true));
  TRY_STATUS(gamepad.QueueDpadState(Gamepad::DpadDirection::kNorth, false));

  TRY_STATUS(gamepad.ReportEvents());

  TRY_STATUS(gamepad.QueueJoystickState(Gamepad::Joystick::kLeft, Gamepad::JoystickAxis::kX, 0.5));
  TRY_STATUS(gamepad.QueueJoystickState(Gamepad::Joystick::kRight, Gamepad::JoystickAxis::kY, 1.0));

  TRY_STATUS(gamepad.QueueTriggerState(Gamepad::Trigger::kLeft, 0.6));

  TRY_STATUS(gamepad.ReportEvents());

  return absl::OkStatus();
}

int main() {
  absl::Status status = Run();
  if (!status.ok()) {
    CheckStatus(status);
    return 1;
  }

  return 0;
}
