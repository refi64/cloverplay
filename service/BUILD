load("@rules_cc//cc:defs.bzl", "cc_binary")
load("//:build/cpu_transition.bzl", "android_native_cpu_transition")

CPUS = [
    "arm",
    "arm64",
    "host",
]

# Define a service binary for Android and one for host testing.
[cc_binary(
    name = "clover_service_%s" % cpu,
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
) for cpu in CPUS]

[android_native_cpu_transition(
    name = "clover_service_%s_transition" % cpu,
    cpu = cpu,
    dep = ":clover_service_%s" % cpu,
    visibility = ["//visibility:public"],
) for cpu in CPUS if cpu != "host"]
