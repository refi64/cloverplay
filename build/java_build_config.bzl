"""
Adds a build_config rule to generate BuildConfig.java.
"""

def _java_build_config_impl(ctx, **kwargs):
    output = ctx.actions.declare_file("BuildConfig.java")

    lines = [
        "package %s;" % ctx.attr.package,
        "public class BuildConfig {",
        "  public final static boolean DEBUG = %s;" % str(ctx.var["COMPILATION_MODE"] != "opt").lower(),
        "};",
    ]

    ctx.actions.write(output, "\n".join(lines))

    return [DefaultInfo(files = depset([output])), OutputGroupInfo(all_files = depset([output]))]

java_build_config = rule(
    attrs = {
        "package": attr.string(mandatory = True),
    },
    output_to_genfiles = True,
    implementation = _java_build_config_impl,
)
