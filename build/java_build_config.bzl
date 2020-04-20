"""
Adds a java_build_config rule to generate BuildConfig.java.
"""

TrialKeyProviderInfo = provider(fields = ["key"])

def _trial_key_provider_impl(ctx):
    return TrialKeyProviderInfo(key = ctx.build_setting_value)

trial_key = rule(
    implementation = _trial_key_provider_impl,
    build_setting = config.string(flag = True),
)

def _java_build_config_impl(ctx):
    output = ctx.actions.declare_file("%s/BuildConfig.java" % ctx.label.name)

    trial_key = ctx.attr.trial_key[TrialKeyProviderInfo].key if ctx.attr.trial_key != None else ""

    lines = [
        "package %s;" % ctx.attr.package,
        "public class BuildConfig {",
        "  public final static boolean DEBUG = %s;" % str(ctx.var["COMPILATION_MODE"] != "opt").lower(),
        '  public final static String APPLICATION_ID = "%s";' % ctx.attr.application_id,
        '  public final static String TRIAL_KEY = "%s";' % trial_key,
        "};",
    ]

    ctx.actions.write(output, "\n".join(lines))

    return [DefaultInfo(files = depset([output])), OutputGroupInfo(all_files = depset([output]))]

java_build_config = rule(
    attrs = {
        "application_id": attr.string(mandatory = True),
        "package": attr.string(mandatory = True),
        "trial_key": attr.label(),
    },
    output_to_genfiles = True,
    implementation = _java_build_config_impl,
)
