#include "uinput.h"

#include <cstdint>
#include <cstring>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <sys/ioctl.h>
#include <unistd.h>

UinputConnection::UinputConnection(UinputConnection&& other) : fd_(other.fd_) {
  other.fd_ = kUnsetFd;
}

UinputConnection::~UinputConnection() {
  if (fd_ != kUnsetFd && close(fd_) == -1) {
    CheckStatus(STATUS_FROM_ERRNO("Failed to close uinput device"));
  }
}

// static
StatusOr<UinputConnection> UinputConnection::Connect() {
  int fd = open("/dev/uinput", O_WRONLY);
  if (fd == -1) {
    return STATUS_FROM_ERRNO("Failed to open uinput device");
  }

  return UinputConnection(fd);
}

absl::Status UinputConnection::EnableType(std::uint64_t category, std::uint16_t code) {
  if (ioctl(fd_, category, code) == -1) {
    return STATUS_FROM_ERRNO("Failed to enable type {}:{}", category, code);
  }

  return absl::OkStatus();
}

absl::Status UinputConnection::SetAxisBounds(std::uint16_t axis, const AxisProperties& properties) {
  uinput_abs_setup setup;
  setup.code = axis;

  setup.absinfo.value = properties[kAxisProp_Value];
  setup.absinfo.minimum = properties[kAxisProp_Min];
  setup.absinfo.maximum = properties[kAxisProp_Max];
  setup.absinfo.fuzz = properties[kAxisProp_Fuzz];
  setup.absinfo.flat = properties[kAxisProp_Flat];
  setup.absinfo.resolution = properties[kAxisProp_Res];

  if (ioctl(fd_, UI_ABS_SETUP, &setup) == -1) {
    return STATUS_FROM_ERRNO("Failed to set up axis {}", axis);
  }

  return absl::OkStatus();
}

absl::Status UinputConnection::CreateUsbDevice(UsbDeviceProperties properties) {
  uinput_setup setup;
  std::memset(&setup, 0, sizeof(setup));
  setup.id.bustype = BUS_USB;
  setup.id.vendor = properties.vendor;
  setup.id.product = properties.product;
  setup.id.version = properties.version;
  std::memcpy(setup.name, properties.name.data(), properties.name.size());
  setup.name[properties.name.size()] = '\0';

  if (ioctl(fd_, UI_DEV_SETUP, &setup) == -1 || ioctl(fd_, UI_DEV_CREATE) == -1) {
    return STATUS_FROM_ERRNO("Failed to setup USB device {:x}:{:x} ({})", properties.vendor,
                             properties.product, properties.name);
  }

  return absl::OkStatus();
}

absl::Status UinputConnection::QueueEvent(std::uint16_t type, std::uint16_t code,
                                          std::int32_t value) {
  input_event event;
  event.type = type;
  event.code = code;
  event.value = value;

  event.time.tv_sec = event.time.tv_usec = 0;

  if (write(fd_, &event, sizeof(event)) == -1) {
    return STATUS_FROM_ERRNO("Failed to emit event {:x}:{:x}", type, code);
  }

  return absl::OkStatus();
}

absl::Status UinputConnection::ReportEvents() { return QueueEvent(EV_SYN, SYN_REPORT, 0); }

absl::Status UinputConnection::DestroyUsbDevice() {
  if (ioctl(fd_, UI_DEV_DESTROY) == -1) {
    return STATUS_FROM_ERRNO("Failed to destroy USB device");
  }

  return absl::OkStatus();
}

// static
StatusOr<UinputUsbController>
UinputUsbController::Create(UinputConnection* connection,
                            UinputConnection::UsbDeviceProperties properties,
                            absl::btree_map<std::uint16_t, UinputConnection::AxisProperties> axes,
                            absl::btree_set<std::uint16_t> buttons) {
  if (!axes.empty()) {
    TRY_STATUS(connection->EnableType(UI_SET_EVBIT, EV_ABS));
    for (const auto& pair : axes) {
      TRY_STATUS(connection->EnableType(UI_SET_ABSBIT, pair.first));
      TRY_STATUS(connection->SetAxisBounds(pair.first, pair.second));
    }
  }

  if (!buttons.empty()) {
    TRY_STATUS(connection->EnableType(UI_SET_EVBIT, EV_KEY));
    for (std::uint16_t button : buttons) {
      TRY_STATUS(connection->EnableType(UI_SET_KEYBIT, button));
    }
  }

  TRY_STATUS(connection->CreateUsbDevice(properties));

  return UinputUsbController(connection);
}

absl::Status UinputUsbController::QueueButtonEvent(std::uint16_t button, bool pressed) {
  return connection_->QueueEvent(EV_KEY, button, pressed ? 1 : 0);
}

absl::Status UinputUsbController::QueueAxisEvent(std::uint16_t axis, std::int32_t value) {
  return connection_->QueueEvent(EV_ABS, axis, value);
}

absl::Status UinputUsbController::ReportEvents() { return connection_->ReportEvents(); }
