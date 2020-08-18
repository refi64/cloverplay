"""
TODO
"""

SHORT_CPU_MAP = {
    "arm": "armeabi-v7a",
    "arm64": "arm64-v8a",
}

def _android_native_cpu_transition_impl(ctx):
    providers = []
    dep = ctx.attr.dep[0]

    if CcInfo in dep:
        providers.append(dep[CcInfo])

    if ApkInfo in dep:
        providers.append(dep[ApkInfo])

    providers.append(DefaultInfo(files = dep.files))
    return providers

def _transition_impl(settings, attr):
    cpu = SHORT_CPU_MAP.get(attr.cpu, attr.cpu)

    transition = {
        "//command_line_option:android_cpu": cpu,
        "//command_line_option:cpu": cpu,
        "//command_line_option:fat_apk_cpu": cpu,
    }

    if cpu.startswith("arm"):
        transition["//command_line_option:crosstool_top"] = "//external:android/crosstool"

    return transition

def _android_binary_cpu_transition_impl(ctx):
    apk_info = ctx.attr.dep[0][ApkInfo]

    inputs = [apk_info.signed_apk, apk_info.unsigned_apk]
    outputs = []

    for input in inputs:
        output = ctx.actions.declare_file(input.basename)
        ctx.actions.run(
            inputs = [input],
            outputs = [output],
            executable = "cp",
            arguments = [input.path, output.path],
        )

    return [DefaultInfo(files = depset(outputs))]

_transition = transition(
    implementation = _transition_impl,
    inputs = [],
    outputs = [
        "//command_line_option:android_cpu",
        "//command_line_option:cpu",
        "//command_line_option:crosstool_top",
        "//command_line_option:fat_apk_cpu",
    ],
)

android_native_cpu_transition = rule(
    implementation = _android_native_cpu_transition_impl,
    attrs = {
        "cpu": attr.string(),
        "dep": attr.label(cfg = _transition),
        "_whitelist_function_transition": attr.label(
            default = "@bazel_tools//tools/whitelists/function_transition_whitelist",
        ),
    },
)

android_binary_cpu_transition = rule(
    implementation = _android_binary_cpu_transition_impl,
    attrs = {
        "cpu": attr.string(),
        "dep": attr.label(cfg = _transition, providers = [ApkInfo]),
        "_whitelist_function_transition": attr.label(
            default = "@bazel_tools//tools/whitelists/function_transition_whitelist",
        ),
    },
)
