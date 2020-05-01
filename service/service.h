#pragma once

#include "backend/gamepad.h"
#include "service/json.h"

class Service {
 public:
  Service(Service&& other) = default;

  static StatusOr<Service> Create();

  absl::Status Run();

 private:
  Service(std::vector<UinputConnection> uinput_connections, StadiaGamepad stadia,
          Xbox360Gamepad xbox)
      : uinput_connections_(std::move(uinput_connections)), stadia_(std::move(stadia)),
        xbox_(std::move(xbox)) {}

  absl::Status HandleRequest(std::string_view req_json);

  absl::Status HandleButtonEvent(Gamepad* gamepad, const Event::Button& event);
  absl::Status HandleJoystickEvent(Gamepad* gamepad, const Event::Joystick& event);
  absl::Status HandleTriggerEvent(Gamepad* gamepad, const Event::Trigger& event);
  absl::Status HandleDpadEvent(Gamepad* gamepad, const Event::Dpad& event);

  std::vector<UinputConnection> uinput_connections_;

  StadiaGamepad stadia_;
  Xbox360Gamepad xbox_;
};
