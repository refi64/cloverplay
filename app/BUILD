load("@rules_android//android:rules.bzl", "android_binary")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_android_library", "kt_compiler_plugin")
load("//:build/java_build_config.bzl", "java_build_config", "trial_key")

PACKAGE = "com.refi64.cloverplay"

MODES = [
    "trial",
    "long_trial",
    "paid",
]

APP_IDS = {
    "trial": "com.refi64.cloverplay.trial",
    "long_trial": "com.refi64.cloverplay.ltrial",
    "paid": "com.refi64.cloverplay",
}

trial_key(
    name = "trial_key",
    build_setting_default = "",
)

trial_key(
    name = "long_trial_key",
    build_setting_default = "",
)

[java_build_config(
    name = "%s_build_config" % mode,
    application_id = APP_IDS[mode],
    package = PACKAGE,
    trial_key = ":%s_key" % mode,
) for mode in MODES if mode.endswith("trial")]

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

[kt_android_library(
    name = "%s_lib" % mode,
    srcs = glob(["java/**"]) + [":%s_build_config" % mode],
    assets = [
        "//service:clover_service",
    ],
    assets_dir = ".",
    custom_package = PACKAGE,
    data = [
        "//service:clover_service",
    ],
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
        "@maven//:io_trialy_library_trialy",
    ],
) for mode in MODES]

[android_binary(
    name = "cloverplay_%s" % mode,
    custom_package = PACKAGE,
    manifest = "AndroidManifest.trial.xml" if mode.endswith("trial") else "AndroidManifest.xml",
    manifest_values = {
        "applicationId": APP_IDS[mode],
        "versionCode": "2",
        "versionName": "0.2",
    },
    proguard_specs = select({
        "//:release": [
            "//:third_party/proguard_specs/proguard-android-optimize.txt",
            "proguard/cloverplay-rules.pro",
        ],
        "//conditions:default": [],
    }),
    visibility = ["//visibility:public"],
    deps = [":%s_lib" % mode],
) for mode in MODES]
