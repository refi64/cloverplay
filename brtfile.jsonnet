local android_sdk_components = import 'android_sdk_components.json';

local bazel_ver = '3.0.0';
local fmt_ver = '6.2.0';
local magic_enum_ver = '0.6.5';
local proguard_specs_ver = 'android-10.0.0_r33';
local rapidjson_ver = '8f4c021fa2f1e001d2376095928fc0532adf2ae6';

local android_rules_commit = '9ab1134546364c6de84fc6c80b4202fdbebbbb35';
local cc_rules_commit = '7c3170fe93e13fbd4835bfa4f64ff93cf2c9b6c8';
local jvm_rules_commit = 'bad9e2501279aea5268b1b8a5463ccc1be8ddf65';
local kotlin_rules_ver = 'legacy-1.4.0-rc2';
local pkg_rules_commit = '79eafadca7b4fdb675b1cfa40b2ac20f23139271';

local AndroidSdkComponentUrl(name) =
  std.format('https://dl.google.com/android/repository/%s', android_sdk_components[name].filename);

local BazelRulesSnapshot(user, repo, revision, release_file='') = {
  url: if std.length(release_file) == 0
        then std.format('https://github.com/%s/%s/archive/%s.tar.gz', [user, repo, revision])
        else std.format('https://github.com/%s/%s/releases/download/%s/%s', [user, repo, revision, release_file],),
  dest: std.format('third_party/%s', repo),
  arc: { prefix: std.format('%s-%s', [repo, revision]) },
};

local OfficialBazelRulesSnapshot(repo, revision, release_file='') =
  BazelRulesSnapshot('bazelbuild', repo, revision, release_file);

{
  files: [
    {
      url: AndroidSdkComponentUrl('platform'),
      dest: 'third_party/android-sdk/platforms',
      arc: {
        post: std.format('mv $BRT_EXTRACTED/{android-*,android-%s}', android_sdk_components.platform.version),
      },
    },
    {
      url: AndroidSdkComponentUrl('ndk'),
      dest: 'third_party/android-sdk/ndk',
      arc: { prefix: std.format('android-ndk-r%s', android_sdk_components.ndk.version) },
    },
    {
      url: AndroidSdkComponentUrl('platform_tool'),
      dest: 'third_party/android-sdk/platform-tools',
      arc: { prefix: 'platform-tools' },
    },
    {
      url: AndroidSdkComponentUrl('build_tool'),
      dest: 'third_party/android-sdk/build-tools',
      arc: {
        post: std.format('mv $BRT_EXTRACTED/{android-*,%s}', android_sdk_components.build_tool.version),
      },
    },
    {
      url: AndroidSdkComponentUrl('tool'),
      dest: 'third_party/android-sdk/tools',
      arc: { prefix: 'tools' },
    },

    {
      url: std.format('https://github.com/bazelbuild/bazel/releases/download/%s/bazel-%s-linux-x86_64', [bazel_ver, bazel_ver]),
      dest: 'third_party/bazel/bazel',
    },
    {
      url: std.format('https://github.com/fmtlib/fmt/releases/download/%s/fmt-%s.zip', [fmt_ver, fmt_ver]),
      dest: 'third_party/fmt',
      arc: { prefix: std.format('fmt-%s', fmt_ver) },
    },
    {
      url: std.format('https://github.com/Neargye/magic_enum/releases/download/v%s/magic_enum.hpp', magic_enum_ver),
      dest: 'third_party/magic_enum/',
    },
    {
      url: std.format('https://github.com/Tencent/rapidjson/archive/%s.tar.gz', rapidjson_ver),
      dest: 'third_party/rapidjson',
      arc: { prefix: std.format('rapidjson-%s', rapidjson_ver) },
    },
    {
      url: std.format('https://android.googlesource.com/platform/sdk/+/%s/files/proguard-android-optimize.txt?format=TEXT', proguard_specs_ver),
      dest: 'third_party/proguard_specs/raw.txt',
      post: 'mkdir -p third_party/proguard_specs; base64 -d $BRT_DOWNLOAD > third_party/proguard_specs/proguard-android-optimize.txt',
    },

    OfficialBazelRulesSnapshot('rules_android', android_rules_commit),
    OfficialBazelRulesSnapshot('rules_cc', cc_rules_commit),
    OfficialBazelRulesSnapshot('rules_jvm_external', jvm_rules_commit),
    OfficialBazelRulesSnapshot('rules_kotlin', kotlin_rules_ver, release_file='rules_kotlin_release.tgz') {
      arc: {
        // XXX
        post: |||
          sed -i 's/\(deps = ctx.attr.deps\) + ctx.attr.plugins/\1/' "$BRT_EXTRACTED"/kotlin/internal/jvm/compile.bzl
        |||,
      },
    },
    OfficialBazelRulesSnapshot('rules_pkg', pkg_rules_commit) {
      arc+: { prefix: super.prefix + '/pkg' },
    },
  ],
}
