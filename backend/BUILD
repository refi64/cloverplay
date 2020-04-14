load("@rules_cc//cc:defs.bzl", "cc_library")

cc_library(
    name = "backend",
    srcs = [
        "errors.cc",
        "errors.h",
        "gamepad.cc",
        "gamepad.h",
        "uinput.cc",
        "uinput.h",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@absl//absl/container:btree",
        "@absl//absl/status",
        "@fmt",
        "@magic_enum",
    ],
)
