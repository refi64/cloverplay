config_setting(
    name = "android",
    values = {"crosstool_top": "//external:android/crosstool"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "release",
    values = {"compilation_mode": "opt"},
    visibility = ["//visibility:public"],
)

exports_files(["third_party/proguard_specs/proguard-android-optimize.txt"])
