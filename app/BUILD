load("@rules_android//android:rules.bzl", "android_binary", "android_library")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_android_library", "kt_compiler_plugin")
load("//:build/android_binary_export.bzl", "android_binary_export", "android_binary_transition_export")
load("//:build/java_build_config.bzl", "java_build_config")
load("//:config.bzl", "SENTRY_DSN")
load(":build_info.bzl", "BUILD_CONFIGS", "BUILD_CPU_INFO", "BUILD_MODES")

PACKAGE = "com.refi64.cloverplay"

APP_IDS = {
    "trial": "com.refi64.cloverplay.trial",
    "paid": "com.refi64.cloverplay",
}

java_build_config(
    name = "trial_build_config",
    application_id = APP_IDS["trial"],
    package = PACKAGE,
)

java_build_config(
    name = "paid_build_config",
    application_id = APP_IDS["paid"],
    package = PACKAGE,
)

kt_compiler_plugin(
    name = "serialization_plugin",
    deps = [
        "@com_github_jetbrains_kotlin//:kotlinx-serialization-compiler-plugin",
    ],
)

android_library(
    name = "clover_service_lib",
    custom_package = PACKAGE,
    manifest = "AndroidManifest.xml",
    exports = ["//service"],
)

[android_library(
    name = "%s_build_config_lib" % mode,
    srcs = [":%s_build_config" % mode],
    custom_package = PACKAGE,
    manifest = "AndroidManifest.xml",
) for mode in BUILD_MODES]

[kt_android_library(
    name = "%s_lib" % mode,
    srcs = glob(["java/**"]),
    custom_package = PACKAGE,
    manifest = "AndroidManifest.xml",
    plugins = [":serialization_plugin"],
    resource_files = glob(["res/**"]),
    deps = [
        ":%s_build_config_lib" % mode,
        ":clover_service_lib",
        "@maven//:androidx_appcompat_appcompat",
        "@maven//:androidx_constraintlayout_constraintlayout",
        "@maven//:androidx_core_core",
        "@maven//:androidx_core_core_ktx",
        "@maven//:androidx_lifecycle_lifecycle_common",
        "@maven//:androidx_preference_preference",
        "@maven//:com_github_topjohnwu_libsu_core",
        "@maven//:com_github_topjohnwu_libsu_service",
        "@maven//:com_google_android_material_material",
        "@maven//:com_squareup_okhttp3_okhttp",
        "@maven//:de_psdev_licensesdialog_licensesdialog",
        "@maven//:io_sentry_sentry_android",
        "@maven//:io_sentry_sentry_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_serialization_runtime",
    ],
) for mode in BUILD_MODES]

[android_binary(
    name = "cloverplay_%s_%s_apk" % (
        config.mode,
        config.cpu,
    ),
    custom_package = PACKAGE,
    manifest = "AndroidManifest.trial.xml" if config.mode == "trial" else "AndroidManifest.xml",
    manifest_values = {
        "applicationId": APP_IDS[config.mode],
        "versionCode": "4%s" % BUILD_CPU_INFO[config.cpu].version_suffix,
        "versionName": "0.4",
        "SENTRY_DSN": SENTRY_DSN,
    },
    proguard_generate_mapping = True,
    proguard_specs = select({
        "//:release": [
            "//:third_party/proguard_specs/proguard-android-optimize.txt",
            "proguard/cloverplay-rules.pro",
        ],
        "//conditions:default": [],
    }),
    deps = [
        ":%s_lib" % config.mode,
    ],
) for config in BUILD_CONFIGS]

[android_binary_transition_export(
    name = "cloverplay_%s_%s" % (
        config.mode,
        config.cpu,
    ),
    apk = ":cloverplay_%s_%s_apk" % (
        config.mode,
        config.cpu,
    ),
    cpu = config.cpu,
    visibility = ["//visibility:public"],
) for config in BUILD_CONFIGS if config.cpu != "fat"]

[android_binary_export(
    name = "cloverplay_%s_fat" % config.mode,
    apk = "cloverplay_%s_fat_apk" % config.mode,
    visibility = ["//visibility:public"],
) for config in BUILD_CONFIGS if config.cpu == "fat"]
