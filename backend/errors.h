#pragma once

#include "absl/status/status.h"
#include "fmt/format.h"
#include "fmt/ostream.h"
#include <cassert>
#include <functional>
#include <new>

absl::Status StatusFromErrno(int ec, std::string_view message);

#define STATUS_FROM_ERRNO(...)                      \
  ({                                                \
    int _ec = errno;                                \
    StatusFromErrno(_ec, fmt::format(__VA_ARGS__)); \
  })

#define TRY_STATUS(...)                                        \
  do {                                                         \
    if (absl::Status _status = (__VA_ARGS__); !_status.ok()) { \
      return _status;                                          \
    }                                                          \
  } while (0)

#define TRY_STATUS_OR(...)            \
  ({                                  \
    auto _status_or = (__VA_ARGS__);  \
    if (!_status_or.ok()) {           \
      return _status_or.TakeStatus(); \
    }                                 \
    _status_or.TakeValue();           \
  })

void CheckStatus(const absl::Status& status);

template <typename T>
class StatusOr {
 public:
  using ValueType = T;

  StatusOr(T value) : status_(absl::OkStatus()) { new (&value_) T(std::move(value)); }
  StatusOr(absl::Status status) : status_(status) { assert(!status.ok()); }
  StatusOr(const StatusOr& other) = delete;
  StatusOr(StatusOr&& other) {
    std::swap(status_, other.status_);
    if (status_.ok()) {
      new (&value_) T(std::move(other.value_));
    }
  }

  ~StatusOr() {
    if (ok()) {
      std::launder(reinterpret_cast<T*>(&value_))->~T();
    }
  }

  ABSL_MUST_USE_RESULT bool ok() const { return status_.ok(); }

  const T& value() const { return *std::launder(reinterpret_cast<const T*>(&value_)); }

  T TakeValue() { return T(std::move(*std::launder(reinterpret_cast<T*>(&value_)))); }

  const absl::Status& status() const { return status_; }

  absl::Status TakeStatus() {
    // status is used to check if value_ is initialized, so make sure it always has an error.
    if (ok()) {
      return absl::OkStatus();
    }

    absl::Status temp(absl::StatusCode::kUnknown, "");
    std::swap(temp, status_);
    return temp;
  }

  template <typename F>
  StatusOr<typename std::invoke_result_t<F, const T&>::ValueType> Bind(F func) {
    return ok() ? std::invoke(func, value())
                : StatusOr<typename std::invoke_result_t<F, const T&>::ValueType>(status_);
  }

 private:
  typename std::aligned_storage<sizeof(T), alignof(T)>::type value_;
  absl::Status status_;
};
