#include "service.h"

#include "google/protobuf/util/json_util.h"
#include "service/service.pb.h"
#include <memory>
#include <string>
#include <string_view>

namespace {

absl::Status ProtoStatusToAbseil(std::string_view prefix,
                                 google::protobuf::util::Status proto_status) {
  if (proto_status.ok()) {
    return absl::OkStatus();
  }

  std::string error(prefix);
  proto_status.message().AppendToString(&error);
  return absl::Status(static_cast<absl::StatusCode>(proto_status.code()), error);
}

}  // namespace

Service::~Service() { delete stadia_.connection(); }

// static
StatusOr<Service> Service::Create() {
  auto stadia_uinput =
      std::make_unique<UinputConnection>(TRY_STATUS_OR(UinputConnection::Connect()));
  auto xbox_uinput = std::make_unique<UinputConnection>(TRY_STATUS_OR(UinputConnection::Connect()));

  StadiaGamepad stadia = TRY_STATUS_OR(StadiaGamepad::Create(stadia_uinput.get()));
  stadia_uinput.release();

  Xbox360Gamepad xbox = TRY_STATUS_OR(Xbox360Gamepad::Create(xbox_uinput.get()));
  xbox_uinput.release();

  return Service(std::move(stadia), std::move(xbox));
}

absl::Status Service::Run() {
  for (;;) {
    std::string req_json;
    if (!std::getline(std::cin, req_json)) {
      return absl::Status(absl::StatusCode::kResourceExhausted, "EOF on stdin");
    }

    if (req_json == "q") {
      std::cerr << "Quitting..." << std::endl;
      return absl::OkStatus();
    }

    proto::Reply reply;
    if (absl::Status status = HandleRequest(req_json); !status.ok()) {
      reply.set_error(status.ToString());
    }

    std::string reply_json;
    TRY_STATUS(
        ProtoStatusToAbseil("Failed to encode JSON reply",
                            google::protobuf::util::MessageToJsonString(reply, &reply_json)));

    std::cout << reply_json << std::endl;
  }
}

absl::Status Service::HandleRequest(std::string_view req_json) {
  proto::Request req;
  TRY_STATUS(
      ProtoStatusToAbseil("Failed to parse JSON message",
                          google::protobuf::util::JsonStringToMessage(req_json.data(), &req)));

  Gamepad* gamepad = nullptr;
  switch (req.controller()) {
  case proto::Controller::STADIA:
    gamepad = &stadia_;
    break;
  case proto::Controller::XBOX360:
    gamepad = &xbox_;
    break;
  default:
    return absl::Status(absl::StatusCode::kInvalidArgument, "Invalid controller value");
  }

  for (const proto::Event& event : req.events()) {
    switch (event.event_case()) {
    case proto::Event::EventCase::kButton:
      TRY_STATUS(HandleButtonEvent(gamepad, event.button()));
      break;
    case proto::Event::EventCase::kJoystick:
      TRY_STATUS(HandleJoystickEvent(gamepad, event.joystick()));
      break;
    case proto::Event::EventCase::kTrigger:
      TRY_STATUS(HandleTriggerEvent(gamepad, event.trigger()));
      break;
    case proto::Event::EventCase::kDpad:
      TRY_STATUS(HandleDpadEvent(gamepad, event.dpad()));
      break;
    default:
      return absl::Status(absl::StatusCode::kInvalidArgument, "Invalid event");
    }
  }

  return gamepad->ReportEvents();
}

absl::Status Service::HandleButtonEvent(Gamepad* gamepad, const proto::ButtonEvent& event) {
  return gamepad->QueueButtonState(static_cast<Gamepad::Button>(event.button()), event.pressed());
}

absl::Status Service::HandleJoystickEvent(Gamepad* gamepad, const proto::JoystickEvent& event) {
  return gamepad->QueueJoystickState(static_cast<Gamepad::Joystick>(event.joystick()),
                                     static_cast<Gamepad::JoystickAxis>(event.axis()),
                                     event.position());
}

absl::Status Service::HandleTriggerEvent(Gamepad* gamepad, const proto::TriggerEvent& event) {
  return gamepad->QueueTriggerState(static_cast<Gamepad::Trigger>(event.trigger()),
                                    event.position());
}

absl::Status Service::HandleDpadEvent(Gamepad* gamepad, const proto::DpadEvent& event) {
  return gamepad->QueueDpadState(static_cast<Gamepad::DpadDirection>(event.direction()),
                                 event.pressed());
}
