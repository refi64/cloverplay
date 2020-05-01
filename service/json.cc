#include <stack>

#include "rapidjson/error/en.h"

#include "service/json.h"

StatusOr<rapidjson::Document> ParseJsonDocument(std::string_view data) {
  rapidjson::Document doc;
  rapidjson::ParseResult ok = doc.Parse(data.data());
  if (!ok) {
    return absl::Status(absl::StatusCode::kInvalidArgument,
                        fmt::format("Failed to parse JSON: {} ({})",
                                    rapidjson::GetParseError_En(ok.Code()), ok.Offset()));
  }

  return std::move(doc);
}

// static
JsonPath JsonPath::Root() { return JsonPath(nullptr, ""); }

JsonPath JsonPath::EnterObjectKey(std::string_view key) const {
  std::string subpath;
  if (!is_root()) {
    subpath.push_back('.');
  }

  subpath += key.data();
  return JsonPath(this, subpath);
}

JsonPath JsonPath::EnterArrayItem(std::size_t index) const {
  return JsonPath(this, fmt::format("[{}]", index));
}

std::string JsonPath::BuildPath() const {
  std::stack<std::string> parts;
  std::size_t size = 0;

  for (const JsonPath* path = this; path != nullptr; path = path->parent_) {
    parts.push(path->subpath_);
    size += path->subpath_.size();
  }

  std::string result;
  result.reserve(size);

  while (!parts.empty()) {
    result += parts.top();
    parts.pop();
  }

  return result;
}

// static
std::string_view JsonContext::TypeToString(rapidjson::Type type) {
  switch (type) {
  case rapidjson::kNullType:
    return "null";
  case rapidjson::kFalseType:
    return "false";
  case rapidjson::kTrueType:
    return "true";
  case rapidjson::kObjectType:
    return "object";
  case rapidjson::kArrayType:
    return "array";
  case rapidjson::kStringType:
    return "string";
  case rapidjson::kNumberType:
    return "number";
  }
}

// static
JsonContext JsonContext::Root(const rapidjson::Value& value) {
  return JsonContext(value, JsonPath::Root());
}

absl::Status JsonContext::Raise(const std::string& error) const {
  return absl::Status(absl::StatusCode::kInvalidArgument,
                      fmt::format("Error loading key {}: {}", path_.BuildPath(), error));
}

absl::Status JsonContext::Require(rapidjson::Type type) const {
  if (value_.GetType() != type) {
    return Raise(fmt::format("Expected type {}, got {}", TypeToString(type),
                             TypeToString(value_.GetType())));
  }

  return absl::OkStatus();
}

absl::Status JsonContext::RequireOneOf(rapidjson::Type a, rapidjson::Type b) const {
  if (value_.GetType() != a && value_.GetType() != b) {
    return Raise(fmt::format("Expected type {} or {}, got {}", TypeToString(a), TypeToString(b),
                             TypeToString(value_.GetType())));
  }

  return absl::OkStatus();
}

StatusOr<JsonContext> JsonContext::EnterObjectKey(std::string_view key) const {
  rapidjson::Value::ConstObject object = value_.GetObject();
  if (!object.HasMember(key.data())) {
    return Raise(fmt::format("Missing key: {}", key));
  }

  return JsonContext(object[key.data()], path_.EnterObjectKey(key));
}

JsonContext JsonContext::EnterArrayItem(std::size_t index) const {
  return JsonContext(value_.GetArray()[index], path_.EnterArrayItem(index));
}

StatusOr<bool> JsonContext::AsBool() const {
  TRY_STATUS(RequireOneOf(rapidjson::kTrueType, rapidjson::kFalseType));
  return value_.GetBool();
}

StatusOr<double> JsonContext::AsDouble() const {
  TRY_STATUS(Require(rapidjson::kNumberType));
  return value_.GetDouble();
}

StatusOr<std::string_view> JsonContext::AsString() const {
  TRY_STATUS(Require(rapidjson::kStringType));
  return std::string_view(value_.GetString());
}

// static
StatusOr<Event> Event::FromJson(JsonContext context) {
  TRY_STATUS(context.Require(rapidjson::kObjectType));

  Event event(TRY_STATUS_OR(context.EnterObjectKey("type").Bind(&JsonContext::AsEnum<Type>)));

  switch (event.type_) {
  case Type::kButton:
    event.data_.button.button = TRY_STATUS_OR(
        context.EnterObjectKey("button").Bind(&JsonContext::AsEnum<Gamepad::Button>));
    event.data_.button.pressed = TRY_STATUS_OR(
        context.EnterObjectKey("pressed").Bind(&JsonContext::AsBool));
    break;
  case Type::kJoystick:
    event.data_.joystick.joystick = TRY_STATUS_OR(
        context.EnterObjectKey("joystick").Bind(&JsonContext::AsEnum<Gamepad::Joystick>));
    event.data_.joystick.axis = TRY_STATUS_OR(
        context.EnterObjectKey("axis").Bind(&JsonContext::AsEnum<Gamepad::JoystickAxis>));
    event.data_.joystick.position = TRY_STATUS_OR(
        context.EnterObjectKey("position").Bind(&JsonContext::AsDouble));
    break;
  case Type::kTrigger:
    event.data_.trigger.trigger = TRY_STATUS_OR(
        context.EnterObjectKey("trigger").Bind(&JsonContext::AsEnum<Gamepad::Trigger>));
    event.data_.trigger.position = TRY_STATUS_OR(
        context.EnterObjectKey("position").Bind(&JsonContext::AsDouble));
    break;
  case Type::kDpad:
    event.data_.dpad.direction = TRY_STATUS_OR(
        context.EnterObjectKey("direction").Bind(&JsonContext::AsEnum<Gamepad::DpadDirection>));
    event.data_.dpad.pressed = TRY_STATUS_OR(
        context.EnterObjectKey("pressed").Bind(&JsonContext::AsBool));
    break;
  };

  return event;
}

const Event::Button& Event::button() const {
  CheckType(Type::kButton);
  return data_.button;
}

const Event::Joystick& Event::joystick() const {
  CheckType(Type::kJoystick);
  return data_.joystick;
}

const Event::Trigger& Event::trigger() const {
  CheckType(Type::kTrigger);
  return data_.trigger;
}

const Event::Dpad& Event::dpad() const {
  CheckType(Type::kDpad);
  return data_.dpad;
}

void Event::CheckType(Event::Type type) const {
  if (type_ != type) {
    throw std::runtime_error("attempted to get invalid event type");
  }
}

// static
StatusOr<Request> Request::FromJson(JsonContext context) {
  TRY_STATUS(context.Require(rapidjson::kObjectType));

  Controller controller = TRY_STATUS_OR(
      context.EnterObjectKey("controller").Bind(&JsonContext::AsEnum<Controller>));

  JsonContext events_json = TRY_STATUS_OR(context.EnterObjectKey("events"));
  TRY_STATUS(events_json.Require(rapidjson::kArrayType));
  std::vector<Event> events;

  for (std::size_t i = 0; i < events_json.value().GetArray().Size(); i++) {
    events.push_back(TRY_STATUS_OR(Event::FromJson(events_json.EnterArrayItem(i))));
  }

  return Request(controller, std::move(events));
}
