"""
TODO
"""

SHORT_CPU_MAP = {
    "arm": "armeabi-v7a",
    "arm64": "arm64-v8a",
}

def _android_binary_transition_impl(settings, attr):
    cpu = SHORT_CPU_MAP.get(attr.cpu, attr.cpu)

    transition = {
        "//command_line_option:android_cpu": cpu,
        "//command_line_option:cpu": cpu,
        "//command_line_option:fat_apk_cpu": cpu,
    }

    if cpu.startswith("arm"):
        transition["//command_line_option:crosstool_top"] = "//external:android/crosstool"

    return transition

def _android_binary_export_impl(ctx):
    if type(ctx.attr.apk) == type([]):
        apk_target = ctx.attr.apk[0]
    else:
        apk_target = ctx.attr.apk

    inputs = [
        apk_target[ApkInfo].signed_apk,
        apk_target[ApkInfo].unsigned_apk,
        apk_target[AndroidManifestInfo].manifest,
    ]

    if ProguardMappingInfo in apk_target:
        inputs.append(apk_target[ProguardMappingInfo].proguard_mapping)

    outputs = []

    prefix = apk_target.label.name

    for input in inputs:
        if input.basename == "AndroidManifest.xml":
            output_name = "%s_manifest.xml" % ctx.label.name
        else:
            if not input.basename.startswith(prefix):
                fail("Expected %s basename to start with %s" % (input, prefix))
            output_name = ctx.label.name + input.basename[len(prefix):]

        output = ctx.actions.declare_file(output_name)
        outputs.append(output)

        ctx.actions.run(
            inputs = [input],
            outputs = [output],
            executable = "cp",
            arguments = [input.path, output.path],
        )

    return [
        DefaultInfo(files = depset(outputs, transitive = [depset(inputs)])),
    ]

_android_binary_transition = transition(
    implementation = _android_binary_transition_impl,
    inputs = [],
    outputs = [
        "//command_line_option:android_cpu",
        "//command_line_option:cpu",
        "//command_line_option:crosstool_top",
        "//command_line_option:fat_apk_cpu",
    ],
)

android_binary_export = rule(
    implementation = _android_binary_export_impl,
    attrs = {
        "apk": attr.label(providers = [ApkInfo]),
    },
)

android_binary_transition_export = rule(
    implementation = _android_binary_export_impl,
    attrs = {
        "apk": attr.label(cfg = _android_binary_transition, providers = [ApkInfo]),
        "cpu": attr.string(),
        "_whitelist_function_transition": attr.label(
            default = "@bazel_tools//tools/whitelists/function_transition_whitelist",
        ),
    },
)
