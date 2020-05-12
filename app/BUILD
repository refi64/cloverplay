load("@rules_android//android:rules.bzl", "android_binary", "android_library")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_android_library", "kt_compiler_plugin")
load("//:build/cpu_transition.bzl", "cpu_transition")
load("//:build/java_build_config.bzl", "java_build_config")
load("//:config.bzl", "SENTRY_DSN", "TRIAL_KEY")
load(":build_info.bzl", "BUILD_CONFIGS", "BUILD_CPU_INFO")

PACKAGE = "com.refi64.cloverplay"

APP_IDS = {
    "trial": "com.refi64.cloverplay.trial",
    "paid": "com.refi64.cloverplay",
}

java_build_config(
    name = "trial_build_config",
    application_id = APP_IDS["trial"],
    package = PACKAGE,
    trial_key = TRIAL_KEY,
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

[android_library(
    name = "clover_service_lib_%s" % cpu,
    exports = ["//service:clover_service_%s_transition" % cpu],
) for cpu in BUILD_CPU_INFO if cpu != "fat"]

[kt_android_library(
    name = "%s_lib_%s" % (
        config.mode,
        config.cpu,
    ),
    srcs = glob(["java/**"]) + [":%s_build_config" % config.mode],
    assets = [
        "//service:clover_service_%s_transition" % cpu
        for cpu in BUILD_CPU_INFO[config.cpu].cpus
    ],
    assets_dir = ".",
    custom_package = PACKAGE,
    manifest = "AndroidManifest.xml",
    plugins = [":serialization_plugin"],
    resource_files = glob(["res/**"]),
    deps = [
        "@kotlin_rules_maven//:org_jetbrains_kotlinx_kotlinx_serialization_runtime",
        "@maven//:androidx_appcompat_appcompat",
        "@maven//:androidx_constraintlayout_constraintlayout",
        "@maven//:androidx_core_core",
        "@maven//:androidx_core_core_ktx",
        "@maven//:androidx_preference_preference",
        "@maven//:com_github_topjohnwu_libsu_core",
        "@maven//:com_google_android_material_material",
        "@maven//:de_psdev_licensesdialog_licensesdialog",
        "@maven//:io_sentry_sentry_android",
        "@maven//:io_sentry_sentry_core",
        "@maven//:io_trialy_library_trialy",
    ] + [
        ":clover_service_lib_%s" % cpu
        for cpu in BUILD_CPU_INFO[config.cpu].cpus
    ],
) for config in BUILD_CONFIGS]

[android_binary(
    name = "cloverplay_%s_%s" % (
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
    deps = [":%s_lib_%s" % (
        config.mode,
        config.cpu,
    )],
) for config in BUILD_CONFIGS]

[cpu_transition(
    name = "cloverplay_%s_apk_%s" % (
        config.mode,
        config.cpu,
    ),
    cpu = config.cpu,
    dep = ":cloverplay_%s_%s" % (
        config.mode,
        config.cpu,
    ),
    visibility = ["//visibility:public"],
) for config in BUILD_CONFIGS if config.cpu != "fat"]

[alias(
    name = "cloverplay_%s_apk_fat" % config.mode,
    actual = "cloverplay_%s_fat" % config.mode,
    visibility = ["//visibility:public"],
) for config in BUILD_CONFIGS if config.cpu == "fat"]
