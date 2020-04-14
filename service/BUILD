load("@rules_cc//cc:defs.bzl", "cc_binary", "cc_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

proto_library(
    name = "proto",
    srcs = ["service.proto"],
    visibility = ["//visibility:public"],
)

cc_proto_library(
    name = "cc_proto",
    deps = [":proto"],
)

# Define a service binary for Android and one for host testing.
[cc_binary(
    name = name,
    srcs = [
        "main.cc",
        "service.cc",
        "service.h",
    ],
    linkopts = select({
        "//:android": [
            "-llog",
            "-ldl",
            "-lm",
        ],
        "//conditions:default": [],
    }),
    visibility = ["//visibility:public"],
    deps = [
        ":cc_proto",
        "//backend",
        "@absl//absl/status",
        "@fmt",
    ],
) for name in [
    "clover_service",
    "clover_service_host",
]]
