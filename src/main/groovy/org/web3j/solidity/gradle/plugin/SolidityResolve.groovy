/*
 * Copyright 2020 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.solidity.gradle.plugin

import com.github.gradle.node.NodeExtension
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*

@CacheableTask
class SolidityResolve extends DefaultTask {

    private static Set<String> PROVIDERS = ["@openzeppelin/contracts", "@uniswap/lib", "@baseline-protocol/contracts", "@yearn/interfaces"]

    private FileTree sources

    @Input
    @Optional
    private Set<String> allowPaths

    private File packageJson


    @TaskAction
    void resolveSolidity() {
        Set<String> libraries = []
        def pathRemappings = project.solidity.pathRemappings
        def nodeProjectDir = project.node.nodeProjectDir.asFile.get()
        for (def contract in sources) {
            contract.readLines().forEach { line ->
                PROVIDERS.forEach { it ->
                    if (line.contains(it)) {
                        libraries.add(it)
                        pathRemappings.put(it, "$nodeProjectDir.path/node_modules/$it")
                        allowPaths.add("$nodeProjectDir.path/node_modules/$it")
                    }
                }
            }
        }


        def jsonMap = [
                "name"        : project.name,
                "description" : project.description == null ? " " : project.description,
                "repository"  : " ",
                "license"     : "UNLICENSED",
                "dependencies": libraries.collectEntries {
                    [(it): "latest"]
                }
        ]

        if (!packageJson.exists()) {
            packageJson.parentFile.mkdirs()
            packageJson.createNewFile()
        } else {
            packageJson.bytes = []
        }
        def jsonBuilder = new JsonBuilder()
        jsonBuilder jsonMap
        packageJson.append(JsonOutput.prettyPrint(jsonBuilder.toString()) + "\n")
    }

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.ABSOLUTE)
    FileTree getSources() {
        return sources
    }

    void setSources(FileTree sources) {
        this.sources = sources
    }


    @OutputFile
    File getPackageJson() {
        return packageJson
    }

    void setPackageJson(File packageJson) {
        this.packageJson = packageJson
    }

    Set<String> getAllowPaths() {
        return allowPaths
    }

    void setAllowPaths(Set<String> allowPaths) {
        this.allowPaths = allowPaths
    }


}
