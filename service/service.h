#pragma once

#include "backend/gamepad.h"
#include "service/service.pb.h"

namespace proto = cloverplay::proto;

class Service {
 public:
  Service(Service&& other) = default;
  ~Service();

  static StatusOr<Service> Create();

  absl::Status Run();

 private:
  Service(StadiaGamepad stadia, Xbox360Gamepad xbox)
      : stadia_(std::move(stadia)), xbox_(std::move(xbox)) {}

  absl::Status HandleRequest(std::string_view req_json);

  absl::Status HandleButtonEvent(Gamepad* gamepad, const proto::ButtonEvent& event);
  absl::Status HandleJoystickEvent(Gamepad* gamepad, const proto::JoystickEvent& event);
  absl::Status HandleTriggerEvent(Gamepad* gamepad, const proto::TriggerEvent& event);
  absl::Status HandleDpadEvent(Gamepad* gamepad, const proto::DpadEvent& event);

  StadiaGamepad stadia_;
  Xbox360Gamepad xbox_;
};
