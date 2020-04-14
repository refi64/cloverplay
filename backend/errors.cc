#include "errors.h"
#include <iostream>

namespace {

absl::StatusCode StatusCodeFromErrno(int ec) {
  switch (ec) {
  case EINTR:
    return absl::StatusCode::kCancelled;
  case EINVAL:
    return absl::StatusCode::kInvalidArgument;
  case ETIMEDOUT:
    return absl::StatusCode::kDeadlineExceeded;
  case ENOENT:
    return absl::StatusCode::kNotFound;
  case EEXIST:
    return absl::StatusCode::kAlreadyExists;
  case EPERM:
    return absl::StatusCode::kPermissionDenied;
  case ERANGE:
    return absl::StatusCode::kOutOfRange;
  case EAGAIN:
    return absl::StatusCode::kUnavailable;
  default:
    return absl::StatusCode::kUnknown;
  }
}

}  // namespace

absl::Status StatusFromErrno(int ec, std::string_view message) {
  return absl::Status(StatusCodeFromErrno(ec),
                      fmt::format("{}: {} [errno {}]", message, strerror(ec), ec));
}

void CheckStatus(const absl::Status& status) {
  if (!status.ok()) {
    fmt::print(std::cerr, "WARNING: {}\n", status.ToString());
  }
}
