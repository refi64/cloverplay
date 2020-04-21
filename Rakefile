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

task :compdb do
  sh 'third_party/bazel-compdb/generate.sh'
end

task :sign do
  bin = 'bazel-bin/app'
  password_env = 'SIGN_PASSWORD'

  print 'Key password: '
  ENV[password_env] = STDIN.noecho(&:gets).chomp

  ['trial', 'long_trial', 'paid'].each do |flavor|
    unsigned = "#{bin}/cloverplay_#{flavor}_unsigned.apk"
    aligned = "#{bin}/cloverplay_#{flavor}_unsigned_aligned.apk"
    release = "#{bin}/cloverplay_#{flavor}_release.apk"
    ks = 'cloverplay.keystore'

    next if !File.exist?(unsigned)

    build_tools = Dir.glob('third_party/android-sdk/build-tools/*').first

    rm [aligned, release], :force => true

    sh "#{build_tools}/zipalign -v -p 4 #{unsigned} #{aligned}"
    sh "#{build_tools}/apksigner sign --ks #{ks} --ks-pass env:#{password_env} --out #{release} #{aligned}"
  end
end
