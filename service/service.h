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
  Service(StadiaGamepad stadia) : stadia_(std::move(stadia)) {}

  absl::Status HandleRequest(std::string_view req_json);

  absl::Status HandleButtonEvent(const proto::ButtonEvent& event);
  absl::Status HandleJoystickEvent(const proto::JoystickEvent& event);
  absl::Status HandleTriggerEvent(const proto::TriggerEvent& event);
  absl::Status HandleDpadEvent(const proto::DpadEvent& event);

  StadiaGamepad stadia_;
};
