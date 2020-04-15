workspace(name = "cloverplay")

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

android_sdk_repository(
    name = "androidsdk",
    path = "third_party/android-sdk",
)

android_ndk_repository(
    name = "androidndk",
    path = "third_party/android-sdk/ndk",
)

git_repository(
    name = "absl",
    commit = "1112609635037a32435de7aa70a9188dcb591458",
    remote = "https://github.com/abseil/abseil-cpp",
    shallow_since = "1586310615 -0400",
)

new_local_repository(
    name = "fmt",
    build_file = "build/BUILD.fmt",
    path = "third_party/fmt",
)

new_local_repository(
    name = "magic_enum",
    build_file = "build/BUILD.magic_enum",
    path = "third_party/magic_enum",
)

local_repository(
    name = "rules_android",
    path = "third_party/rules_android",
)

local_repository(
    name = "rules_cc",
    path = "third_party/rules_cc",
)

local_repository(
    name = "rules_jvm_external",
    path = "third_party/rules_jvm_external",
)

local_repository(
    name = "io_bazel_rules_kotlin",
    path = "third_party/rules_kotlin",
)

local_repository(
    name = "rules_pkg",
    path = "third_party/rules_pkg",
)

local_repository(
    name = "rules_proto",
    path = "third_party/rules_proto",
)

local_repository(
    name = "build_stack_rules_proto",
    path = "third_party/stackb_rules_proto",
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "androidx.appcompat:appcompat:1.0.2",
        "androidx.constraintlayout:constraintlayout:1.1.3",
        "androidx.core:core-ktx:1.0.2",
        "androidx.multidex:multidex:2.0.1",
        "androidx.preference:preference:1.1.0",
        "com.github.topjohnwu.libsu:core:2.5.1",
        "com.google.android.material:material:1.1.0",
        "com.google.code.gson:gson:2.8.6",
        "de.psdev.licensesdialog:licensesdialog:2.1.0",
    ],
    generate_compat_repositories = True,
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
        "https://jitpack.io",
    ],
)

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")

kotlin_repositories()

kt_register_toolchains()

load("@rules_pkg//:deps.bzl", "rules_pkg_dependencies")

rules_pkg_dependencies()

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

rules_proto_dependencies()

rules_proto_toolchains()

load("@build_stack_rules_proto//java:deps.bzl", "com_google_errorprone_error_prone_annotations")
load("@build_stack_rules_proto//android:deps.bzl", "android_proto_compile", "com_google_guava_guava_android")

android_proto_compile()

com_google_errorprone_error_prone_annotations()

com_google_guava_guava_android()

bind(
    name = "gson",
    actual = "@maven//:com_google_code_gson_gson",
)

bind(
    name = "guava",
    actual = "@com_google_guava_guava_android",
)
