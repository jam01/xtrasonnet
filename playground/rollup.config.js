/*
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import {nodeResolve} from "@rollup/plugin-node-resolve"
export default {
    input: "./editor.js",
    output: {
        file: "../docs/javascripts/editor.bundle.js",
        format: "iife",
        globals: {
            'rxjs': 'rxjs'
        }
    },
    plugins: [nodeResolve()],
    external: ["rxjs"]
}
