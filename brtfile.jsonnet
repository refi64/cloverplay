local android_sdk_components = import 'android_sdk_components.json';

local bazel_ver = '3.0.0';
local bazel_compdb_ver = '0.4.3';
local fmt_ver = '6.2.0';
local grpc_ver = '1.28.1';
local magic_enum_ver = '0.6.5';

local android_rules_commit = '9ab1134546364c6de84fc6c80b4202fdbebbbb35';
local cc_rules_commit = '7c3170fe93e13fbd4835bfa4f64ff93cf2c9b6c8';
local jvm_rules_commit = 'bad9e2501279aea5268b1b8a5463ccc1be8ddf65';
local kotlin_rules_commit = '5efac99b66d48992cded75646e0b00778cb8b38d';
local pkg_rules_commit = '79eafadca7b4fdb675b1cfa40b2ac20f23139271';
local proto_rules_commit = '8b81c3ccfdd0e915e46ffa888d3cdb6116db6fa5';
local stackb_proto_rules_commit = '1d6b84118399828511faeecc145d399c1e7bdee2';

local AndroidSdkComponentUrl(name) =
  std.format('https://dl.google.com/android/repository/%s', android_sdk_components[name].filename);

local BazelRulesSnapshot(user, repo, revision, target='') = {
  url: std.format('https://github.com/%s/%s/archive/%s.tar.gz', [user, repo, revision]),
  dest: std.format('third_party/%s', if std.length(target) != 0 then target else repo),
  arc: { prefix: std.format('%s-%s', [repo, revision]) },
};

local OfficialBazelRulesSnapshot(repo, revision) =
  BazelRulesSnapshot('bazelbuild', repo, revision);

{
  files: [
    {
      url: AndroidSdkComponentUrl('platform'),
      dest: 'third_party/android-sdk/platforms',
      arc: {
        post: std.format('mv android-* android-%s', android_sdk_components.platform.version),
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
        post: std.format('mv android-* %s', android_sdk_components.build_tool.version),
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
      url: std.format('https://github.com/grailbio/bazel-compilation-database/archive/%s.tar.gz', bazel_compdb_ver),
      dest: 'third_party/bazel-compdb',
      arc: { prefix: std.format('bazel-compilation-database-%s', bazel_compdb_ver) },
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

    OfficialBazelRulesSnapshot('rules_android', android_rules_commit),
    OfficialBazelRulesSnapshot('rules_cc', cc_rules_commit),
    OfficialBazelRulesSnapshot('rules_jvm_external', jvm_rules_commit),
    OfficialBazelRulesSnapshot('rules_kotlin', kotlin_rules_commit),
    OfficialBazelRulesSnapshot('rules_pkg', pkg_rules_commit) + {
      arc+: { prefix: super.prefix + '/pkg' },
    },
    OfficialBazelRulesSnapshot('rules_proto', proto_rules_commit),
    BazelRulesSnapshot('stackb', 'rules_proto', stackb_proto_rules_commit, 'stackb_rules_proto'),
  ],
}
