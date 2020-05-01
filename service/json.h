#pragma once

#include <cctype>
#include <string_view>
#include <type_traits>

#include "absl/types/span.h"
#include "magic_enum.hpp"
#include "rapidjson/document.h"

#include "backend/gamepad.h"

StatusOr<rapidjson::Document> ParseJsonDocument(std::string_view data);

class JsonPath {
 public:
  static JsonPath Root();

  bool is_root() const { return parent_ == nullptr; }

  JsonPath EnterObjectKey(std::string_view key) const;
  JsonPath EnterArrayItem(std::size_t index) const;

  std::string BuildPath() const;

 private:
  JsonPath(const JsonPath* parent, std::string subpath)
      : parent_(parent), subpath_(std::move(subpath)) {}

  const JsonPath* parent_;
  std::string subpath_;
};

class JsonContext {
 public:
  static std::string_view TypeToString(rapidjson::Type type);

  static JsonContext Root(const rapidjson::Value& value);

  const rapidjson::Value& value() const { return value_; }
  const JsonPath& path() const { return path_; }

  absl::Status Raise(const std::string& error) const;

  absl::Status Require(rapidjson::Type type) const;
  absl::Status RequireOneOf(rapidjson::Type a, rapidjson::Type b) const;

  StatusOr<JsonContext> EnterObjectKey(std::string_view key) const;
  JsonContext EnterArrayItem(std::size_t index) const;

  StatusOr<bool> AsBool() const;
  StatusOr<double> AsDouble() const;
  StatusOr<std::string_view> AsString() const;

  template <typename Enum>
  StatusOr<Enum> AsEnum() const {
    static_assert(std::is_enum_v<Enum>);

    std::string_view repr = TRY_STATUS_OR(AsString());

    std::string name("k");
    bool is_caps = true;
    for (char c : repr) {
      c = std::tolower(c);

      if (is_caps) {
        if (std::isalpha(c)) {
          c = std::toupper(c);
        }

        is_caps = false;
      } else if (c == '_') {
        is_caps = true;
        continue;
      }

      name.push_back(c);
    }

    if (auto opt_value = magic_enum::enum_cast<Enum>(name); opt_value) {
      return *opt_value;
    } else {
      return Raise("Invalid enum value");
    }
  }

 private:
  JsonContext(const rapidjson::Value& value, JsonPath path)
      : value_(value), path_(std::move(path)) {}

  const rapidjson::Value& value_;
  JsonPath path_;
};

class Event {
 public:
  static StatusOr<Event> FromJson(JsonContext context);

  enum class Type { kButton, kJoystick, kTrigger, kDpad };

  struct Button {
    Gamepad::Button button;
    bool pressed;
  };

  struct Joystick {
    Gamepad::Joystick joystick;
    Gamepad::JoystickAxis axis;
    double position;
  };

  struct Trigger {
    Gamepad::Trigger trigger;
    double position;
  };

  struct Dpad {
    Gamepad::DpadDirection direction;
    bool pressed;
  };

  Type type() const { return type_; }

  const Button& button() const;
  const Joystick& joystick() const;
  const Trigger& trigger() const;
  const Dpad& dpad() const;

 private:
  Event(Type type) : type_(type) {}

  void CheckType(Type type) const;

  // XXX: ugh unions
  union {
    Button button;
    Joystick joystick;
    Trigger trigger;
    Dpad dpad;
  } data_;

  Type type_;
};

class Request {
 public:
  static StatusOr<Request> FromJson(JsonContext context);

  enum class Controller { kStadia, kXbox };

  Controller controller() const { return controller_; }
  absl::Span<const Event> events() const { return events_; }

 private:
  Request(Controller controller, std::vector<Event> events)
      : controller_(controller), events_(events) {}

  Controller controller_;
  std::vector<Event> events_;
};
