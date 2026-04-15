"use strict";Object.defineProperty(exports, "__esModule", {value: true});

var _chunkFMLWYFD2cjs = require('./chunk-FMLWYFD2.cjs');
require('./chunk-6BSQ6ZKC.cjs');

// src/astro.ts
function astro_default(options) {
  return {
    name: "unplugin-auto-import",
    hooks: {
      "astro:config:setup": async (astro) => {
        var _a;
        (_a = astro.config.vite).plugins || (_a.plugins = []);
        astro.config.vite.plugins.push(_chunkFMLWYFD2cjs.unplugin_default.vite(options));
      }
    }
  };
}


exports.default = astro_default;
