#pragma once

#include <cstdint>
#include <linux/uinput.h>
#include <optional>
#include <string_view>

#include "absl/container/btree_map.h"
#include "absl/container/btree_set.h"
#include "magic_enum.hpp"

#include "errors.h"

class UinputConnection {
 public:
  struct UsbDeviceProperties {
    std::uint16_t vendor;
    std::uint16_t product;
    std::uint16_t version;
    std::string_view name;
  };

  enum AxisPropertyKeys {
    kAxisProp_Value,
    kAxisProp_Min,
    kAxisProp_Max,
    kAxisProp_Fuzz,
    kAxisProp_Flat,
    kAxisProp_Res,
  };

  static constexpr std::size_t kAxisPropCount = magic_enum::enum_count<AxisPropertyKeys>();
  using AxisProperties = std::array<std::int32_t, kAxisPropCount>;

  UinputConnection(const UinputConnection& other) = delete;
  UinputConnection(UinputConnection&& other);
  ~UinputConnection();

  static StatusOr<UinputConnection> Connect();

  absl::Status EnableType(std::uint64_t bit, std::uint16_t code);
  absl::Status SetAxisBounds(std::uint16_t axis, const AxisProperties& properties);

  absl::Status CreateUsbDevice(UsbDeviceProperties properties);
  absl::Status DestroyUsbDevice();

  absl::Status QueueEvent(std::uint16_t type, std::uint16_t code, std::int32_t value);
  absl::Status ReportEvents();

 private:
  UinputConnection(int fd) : fd_(fd) {}

  static constexpr int kUnsetFd = -1;
  int fd_;
};

class UinputUsbController {
 public:
  UinputUsbController(UinputUsbController&& other) : connection_(nullptr) {
    std::swap(connection_, other.connection_);
  }

  ~UinputUsbController() {
    if (connection_ != nullptr) {
      CheckStatus(connection_->DestroyUsbDevice());
    }
  }

  using AxisMap = absl::btree_map<std::uint16_t, UinputConnection::AxisProperties>;
  using ButtonSet = absl::btree_set<std::uint16_t>;

  static StatusOr<UinputUsbController> Create(UinputConnection* connection,
                                              UinputConnection::UsbDeviceProperties properties,
                                              AxisMap axes, ButtonSet buttons);

  absl::Status QueueButtonEvent(std::uint16_t button, bool pressed);
  absl::Status QueueAxisEvent(std::uint16_t axis, std::int32_t value);

  absl::Status ReportEvents();

  UinputConnection* connection() { return connection_; }

 private:
  UinputUsbController(UinputConnection* connection) : connection_(connection) {}

  UinputConnection* connection_;
};
