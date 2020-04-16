load("@rules_android//android:rules.bzl", "android_binary")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_android_library")
load("//:build/java_build_config.bzl", "java_build_config")
load("@build_stack_rules_proto//java:java_proto_compile.bzl", "java_proto_compile")

PACKAGE = "com.refi64.cloverplay"

java_build_config(
    name = "build_config",
    package = PACKAGE,
)

java_proto_compile(
    name = "java_proto",
    deps = ["//service:proto"],
)

kt_android_library(
    name = "lib",
    srcs = glob(["java/**"]) + [
        ":build_config",
        ":java_proto",
    ],
    assets = [
        "//service:clover_service",
    ],
    assets_dir = ".",
    custom_package = PACKAGE,
    data = [
        "//service:clover_service",
    ],
    manifest = "AndroidManifest.xml",
    resource_files = glob(["res/**"]),
    deps = [
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
        "@maven//:androidx_appcompat_appcompat",
        "@maven//:androidx_constraintlayout_constraintlayout",
        "@maven//:androidx_core_core",
        "@maven//:androidx_core_core_ktx",
        "@maven//:androidx_multidex_multidex",
        "@maven//:androidx_preference_preference",
        "@maven//:com_github_topjohnwu_libsu_core",
        "@maven//:com_google_android_material_material",
        "@maven//:de_psdev_licensesdialog_licensesdialog",
    ],
)

android_binary(
    name = "cloverplay",
    custom_package = PACKAGE,
    manifest = "AndroidManifest.xml",
    manifest_values = {
        "versionCode": "1",
        "versionName": "0.1",
    },
    multidex = "native",
    # proguard_specs = select({
    #     "//:release": ["proguard-rules.pro"],
    #     "//conditions:default": [],
    # }),
    resource_files = glob(["res/**"]),
    visibility = ["//visibility:public"],
    deps = [":lib"],
)
