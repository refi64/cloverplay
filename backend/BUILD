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
        "@absl//absl/container:flat_hash_map",
        "@absl//absl/container:flat_hash_set",
        "@absl//absl/status",
        "@fmt",
        "@magic_enum",
    ],
)
