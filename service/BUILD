load("@rules_cc//cc:defs.bzl", "cc_binary", "cc_library")

cc_library(
    name = "core",
    srcs = [
        "json.cc",
        "json.h",
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
)

cc_binary(
    name = "demo",
    srcs = ["demo.cc"],
    deps = [":core"],
)

cc_library(
    name = "service",
    srcs = ["java.cc"],
    visibility = ["//visibility:public"],
    deps = [":core"],
    alwayslink = True,
)
