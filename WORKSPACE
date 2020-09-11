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

new_local_repository(
    name = "rapidjson",
    build_file = "build/BUILD.rapidjson",
    path = "third_party/rapidjson",
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

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "androidx.appcompat:appcompat:1.2.0",
        "androidx.constraintlayout:constraintlayout:1.1.3",
        "androidx.core:core-ktx:1.3.1",
        "androidx.preference:preference:1.1.1",
        "androidx.lifecycle:lifecycle-common:2.2.0",
        "com.github.topjohnwu.libsu:core:3.0.2",
        "com.github.topjohnwu.libsu:service:3.0.2",
        "com.google.android.material:material:1.2.0",
        "com.squareup.okhttp3:okhttp:4.8.1",
        "de.psdev.licensesdialog:licensesdialog:2.1.0",
        "io.sentry:sentry-android:2.0.0",
        "io.sentry:sentry-core:2.0.0",
        "org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0",
    ],
    generate_compat_repositories = True,
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
        "https://jcenter.bintray.com/",
        "https://jitpack.io",
    ],
)

load("@io_bazel_rules_kotlin//kotlin:dependencies.bzl", "kt_download_local_dev_dependencies")

kt_download_local_dev_dependencies()

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")

kotlin_repositories()

kt_register_toolchains()

load("@rules_pkg//:deps.bzl", "rules_pkg_dependencies")

rules_pkg_dependencies()
