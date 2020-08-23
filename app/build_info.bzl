"""
TODO
"""

BUILD_CPU_INFO = {
    "arm": struct(
        cpus = ["arm"],
        version_suffix = "32",
    ),
    "arm64": struct(
        cpus = ["arm64"],
        version_suffix = "64",
    ),
    "fat": struct(
        cpus = [
            "arm",
            "arm64",
        ],
        version_suffix = "00",
    ),
}

BUILD_MODES = ["trial", "paid"]

BUILD_CONFIGS = [
    struct(
        cpu = "fat",
        mode = "trial",
    ),
    struct(
        cpu = "fat",
        mode = "paid",
    ),
    struct(
        cpu = "arm",
        mode = "paid",
    ),
    struct(
        cpu = "arm64",
        mode = "paid",
    ),
]
