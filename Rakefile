require 'io/console'
require 'json'
require 'net/http'
require 'rexml/document'
require 'rubygems/version'
require 'set'
require 'shellwords'

ANDROID_SDK_REPO = 'https://dl.google.com/android/repository/repository-12.xml'
ANDROID_SDK_COMPONENTS = Set['tool', 'platform-tool', 'build-tool', 'platform', 'ndk']

BUILD_TOOLS_VERSION_UPPER_BOUND = Gem::Version.new '30.0.0'

task :update_android_sdk_components do
  xml = Net::HTTP.get_response(URI.parse ANDROID_SDK_REPO).body
  doc = REXML::Document.new xml

  components = {}

  ANDROID_SDK_COMPONENTS.each do |component|
    versions = []

    doc.elements.each "sdk:sdk-repository/sdk:#{component}" do |el|
      if component == 'platform'
        version = el.elements['sdk:api-level'].text
        archive = el.elements['sdk:archives'].elements['sdk:archive']
      else
        if component == 'ndk'
          version = el.elements['sdk:revision'].text
        else
          version = el.elements['sdk:revision'].elements.collect(&:text).join('.')
        end

        archive = el.elements['sdk:archives'].to_a.find{|a|
          a.kind_of?(REXML::Element) && a.elements['sdk:url'] && a.elements['sdk:host-os']&.text == 'linux'
        }
      end

      next if component == 'build-tool' && Gem::Version.new(version) >= BUILD_TOOLS_VERSION_UPPER_BOUND

      next if !archive
      filename = archive.elements['sdk:url'].text

      versions << {version: version, filename: filename}
    end

    components[component.gsub '-', '_'] = versions.sort_by{|v| Gem::Version.new(v[:version])}.last
  end

  File.write 'android_sdk_components.json', JSON.pretty_generate(components)
end

task :compile_flags do
  flags = [
    '-xc++',
    '-std=c++17',

    # XXX: This is a huge mess, making clangd use the NDK is FUN(tm)
    '-nostdinc',
    '-nostdinc++',
    '-stdlib=libc++',
    "--sysroot=#{__dir__}/third_party/android-sdk/ndk/sysroot",
    "-I#{__dir__}/third_party/android-sdk/ndk/sources/cxx-stl/llvm-libc++/include",
    "-I#{__dir__}/third_party/android-sdk/ndk/sources/cxx-stl/llvm-libc++abi/include",
    '-iwithsysroot/usr/include',
    '-iwithsysroot/usr/include/aarch64-linux-android',
    "-I#{__dir__}/third_party/android-sdk/ndk/toolchains/llvm/prebuilt/linux-x86_64/lib64/clang/9.0.8/include",

    "-iquote#{__dir__}",
    # TODO: use brt for Abseil
    "-iquote#{__dir__}/external/absl",
    "-iquote#{__dir__}/third_party/fmt/include",
    "-iquote#{__dir__}/third_party/magic_enum",
    "-iquote#{__dir__}/third_party/rapidjson/include",
  ]

  File.open 'compile_flags.txt', 'w' do |file|
    file.puts flags
  end
end

task :sign do
  bin = 'bazel-bin/app'
  password_env = 'SIGN_PASSWORD'

  print 'Key password: '
  ENV[password_env] = STDIN.noecho(&:gets).chomp

  [
    {:mode => 'trial', :cpu => 'fat'},
    {:mode => 'paid', :cpu => 'fat'},
    {:mode => 'paid', :cpu => 'arm'},
    {:mode => 'paid', :cpu => 'arm64'},
  ].each do |config|
    cpu = config[:cpu]
    mode = config[:mode]

    unsigned = "#{bin}/cloverplay_#{mode}_#{cpu}_unsigned.apk"
    unsigned_sentry = "#{bin}/cloverplay_#{mode}_#{cpu}_unsigned_sentry.apk"
    aligned = "#{bin}/cloverplay_#{mode}_#{cpu}_unsigned_aligned.apk"
    release = "#{bin}/cloverplay_#{mode}_#{cpu}_release.apk"
    map = "#{bin}/cloverplay_#{mode}_#{cpu}_proguard.map"
    manifest = "#{bin}/_merged/cloverplay_#{mode}_#{cpu}/AndroidManifest.xml"
    sentry_props = "assets/sentry-debug-meta.properties"
    ks = 'cloverplay.keystore'

    next if !File.exist?(unsigned)

    build_tools = Dir.glob('third_party/android-sdk/build-tools/*').first

    rm [unsigned_sentry, aligned, release], :force => true

    sh "sentry-cli upload-proguard --android-manifest #{manifest} --write-properties #{sentry_props} #{map}"
    cp unsigned, unsigned_sentry
    chmod 0644, unsigned_sentry
    sh "zip #{unsigned_sentry} #{sentry_props}"
    sh "#{build_tools}/zipalign -v -p 4 #{unsigned_sentry} #{aligned}"
    sh "#{build_tools}/apksigner sign --ks #{ks} --ks-pass env:#{password_env} --out #{release} #{aligned}"

    rm_rf "assets"
  end
end
