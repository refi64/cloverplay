load("@rules_cc//cc:defs.bzl", "cc_binary")

# Define a service binary for Android and one for host testing.
[cc_binary(
    name = name,
    srcs = [
        "json.cc",
        "json.h",
        "main.cc",
        "service.cc",
        "service.h",
    ],
    includes = ["."],
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
        "//backend",
        "@absl//absl/status",
        "@fmt",
        "@rapidjson",
    ],
) for name in [
    "clover_service",
    "clover_service_host",
]]
