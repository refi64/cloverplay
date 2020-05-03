"""
TODO
"""

SHORT_CPU_MAP = {
    "arm": "armeabi-v7a",
    "arm64": "arm64-v8a",
}

_TransitionInfo = provider(fields = ["deps"])

def _impl(ctx):
    providers = []
    dep = ctx.attr.dep[0]

    if CcInfo in dep:
        providers.append(dep[CcInfo])
    if JavaInfo in dep:
        providers.append(dep[JavaInfo])

    providers.append(DefaultInfo(files = dep.files))
    providers.append(_TransitionInfo(deps = dep.files))
    return providers

def _transition_impl(settings, attr):
    cpu = SHORT_CPU_MAP.get(attr.cpu, attr.cpu)

    transition = {
        "//command_line_option:android_cpu": cpu,
        "//command_line_option:cpu": cpu,
    }

    if cpu.startswith("arm"):
        transition["//command_line_option:crosstool_top"] = "//external:android/crosstool"

    return transition

_transition = transition(
    implementation = _transition_impl,
    inputs = [],
    outputs = [
        "//command_line_option:android_cpu",
        "//command_line_option:cpu",
        "//command_line_option:crosstool_top",
    ],
)

cpu_transition = rule(
    implementation = _impl,
    attrs = {
        "cpu": attr.string(),
        "dep": attr.label(cfg = _transition),
        "_whitelist_function_transition": attr.label(
            default = "@bazel_tools//tools/whitelists/function_transition_whitelist",
        ),
    },
)
