config.performance = {
    maxAssetSize: 10 * 1024 * 1024,
    maxEntrypointSize: 10 * 1024 * 1024,
};

config.ignoreWarnings = [
    {
        message: /Critical dependency: the request of a dependency is an expression/,
        module: /Kvace-app-webApp\.import-object\.mjs$/,
    },
    {
        message: /Critical dependency: Accessing import\.meta directly is unsupported/,
    },
];
