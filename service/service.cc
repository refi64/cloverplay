#include "service.h"

#include "rapidjson/ostreamwrapper.h"
#include "rapidjson/writer.h"

#include <memory>
#include <string>
#include <string_view>

// static
StatusOr<Service> Service::Create() {
  std::vector<UinputConnection> connections;

  connections.push_back(TRY_STATUS_OR(UinputConnection::Connect()));
  connections.push_back(TRY_STATUS_OR(UinputConnection::Connect()));

  StadiaGamepad stadia = TRY_STATUS_OR(StadiaGamepad::Create(&connections[0]));
  Xbox360Gamepad xbox = TRY_STATUS_OR(Xbox360Gamepad::Create(&connections[1]));

  return Service(std::move(connections), std::move(stadia), std::move(xbox));
}

absl::Status Service::HandleRequest(std::string_view req_json) {
  rapidjson::Document req_doc = TRY_STATUS_OR(ParseJsonDocument(req_json));
  Request req = TRY_STATUS_OR(Request::FromJson(JsonContext::Root(req_doc)));

  Gamepad* gamepad = nullptr;
  switch (req.controller()) {
  case Request::Controller::kStadia:
    gamepad = &stadia_;
    break;
  case Request::Controller::kXbox:
    gamepad = &xbox_;
    break;
  default:
    return absl::Status(absl::StatusCode::kInvalidArgument, "Invalid controller value");
  }

  for (const Event& event : req.events()) {
    switch (event.type()) {
    case Event::Type::kButton:
      TRY_STATUS(HandleButtonEvent(gamepad, event.button()));
      break;
    case Event::Type::kJoystick:
      TRY_STATUS(HandleJoystickEvent(gamepad, event.joystick()));
      break;
    case Event::Type::kTrigger:
      TRY_STATUS(HandleTriggerEvent(gamepad, event.trigger()));
      break;
    case Event::Type::kDpad:
      TRY_STATUS(HandleDpadEvent(gamepad, event.dpad()));
      break;
    default:
      return absl::Status(absl::StatusCode::kInvalidArgument, "Invalid event");
    }
  }

  return gamepad->ReportEvents();
}

absl::Status Service::HandleButtonEvent(Gamepad* gamepad, const Event::Button& event) {
  return gamepad->QueueButtonState(event.button, event.pressed);
}

absl::Status Service::HandleJoystickEvent(Gamepad* gamepad, const Event::Joystick& event) {
  return gamepad->QueueJoystickState(event.joystick, event.axis, event.position);
}

absl::Status Service::HandleTriggerEvent(Gamepad* gamepad, const Event::Trigger& event) {
  return gamepad->QueueTriggerState(event.trigger, event.position);
}

absl::Status Service::HandleDpadEvent(Gamepad* gamepad, const Event::Dpad& event) {
  return gamepad->QueueDpadState(event.direction, event.pressed);
}
