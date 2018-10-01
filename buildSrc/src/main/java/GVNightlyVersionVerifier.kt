/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import GeckoVersions.nightly_version
import groovy.json.JsonSlurper
import groovy.json.internal.LazyMap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.task
import java.io.File
import java.io.IOException

open class GVNightlyVersionVerifier : Plugin<Project> {

    companion object {
        private const val GV_VERSION_PATH_FILE = "buildSrc/src/main/java/Gecko.kt"
        private const val REPO_OWNER = "mozilla-mobile"
        private const val REPO_NAME = "android-components"
        private const val BASE_BRANCH_NAME = "master"
        private const val HEAD = "amejia481"
    }

    override fun apply(project: Project) {

        project.task("verifyGVNightlyVersion") {
            setVariables()

            dependsOn("dependencyUpdates")

            doLast {
                verifyNewGVNightlyVersion(project)
            }
        }
    }

    private fun verifyNewGVNightlyVersion(project: Project) {

        val newVersion = getLastGeckoViewNightlyVersion(project)

        if (newVersion.isNotEmpty()) {

            updateConfigFileWithNewGeckoVersion(newVersion)

            val token = project.property("token").toString()
            val client = GitHubClient(token)

            if (runGradleTests()) {

                if (openAPROnGitHub(client))
                    println("PR Opened Successfully")
                else
                    println("Error Creating PR")

            } else {
                println("Tests failed creating Github Issue")
                createIssue(client)
            }
        } else {
            println("Sorry no new version of GeckoViewNightly available")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getLastGeckoViewNightlyVersion(project: Project): String {
        val file = File(project.rootDir, "build" + File.separator +
                "dependencyUpdates" + File.separator + "report.json")

        val json = JsonSlurper().parse(file)

        val outdated = (json as LazyMap)["outdated"]
        val dependencies = (outdated as Map<*, *>)["dependencies"] as ArrayList<LazyMap>

        val gecko = dependencies.filter {
            (it["name"] as String).contains("geckoview-nightly")
        }

        return if (gecko.isNotEmpty())
            ((gecko.first()["available"] as LazyMap)["milestone"]) as String
        else ""
    }

    private fun setVariables() {
        System.setProperty("outputFormatter", "json")
        System.setProperty("Drevision", "release")
    }

    private fun updateConfigFileWithNewGeckoVersion(newVersion: String) {
        val file = File(GV_VERSION_PATH_FILE)
        var fileContent = file.readText()
        fileContent = fileContent.replace(Regex("nightly_version.*=.*"),
                "nightly_version = \"$newVersion\"")
        file.writeText(fileContent)
        println("${file.name} file updated")
    }

    private fun runGradleTests(): Boolean {
        println("Running tests")

        val result = "./gradlew clean test".runCommand()

        println("Tests " + passOrFail(result))
        return result
    }

    private fun openAPROnGitHub(client: GitHubClient): Boolean {

        fun commitChanges(): Pair<String, Boolean> {
            println("Creating pull request")
            val branchName = "new_gv_nightly_version_$nightly_version"
            val gitCheckout = "git checkout -B $branchName"
            val gitAdd = "git add buildSrc/src/main/java/GeckoVersions.kt"
            val gitCommit = "git commit -m New_GV_Nightly_Version_Update_$nightly_version"
            val gitPush = "git push -u -f origin $branchName"

            var successful = gitCheckout.runCommand()
            println(gitCheckout + passOrFail(successful))

            successful = gitAdd.runCommand()
            println(gitAdd + passOrFail(successful))

            successful = gitCommit.runCommand()
            println(gitCommit + passOrFail(successful))

            successful = gitPush.runCommand()
            println(gitPush + passOrFail(successful))
            return Pair(branchName, successful)
        }

        val pair = commitChanges()
        val branchName = pair.first
        val commandResult = pair.second

        if (commandResult) {
            createPullRequest(client, branchName)
        }

        return commandResult
    }

    private fun createPullRequest(client: GitHubClient, branchName: String) {
        val bodyJson = ("{\n" +
                "  \"title\": \"[Testing ignore] GV Version Update\",\n" +
                "  \"body\": \"New Version of GV available!\",\n" +
                "  \"head\": \"$HEAD:$branchName\",\n" +
                "  \"base\": \"$BASE_BRANCH_NAME\"\n" +
                "}")

        val result = client.createPullRequest(REPO_OWNER, REPO_NAME, bodyJson)

        val successFul = result.first
        val responseData = result.second

        val stringToPrint = if (successFul) {
            val pullRequestUrl = getUrlFromJSONString(responseData)
            "Pull Request created take a look here $pullRequestUrl"
        } else {
            "Unable to create pull request \n $responseData"
        }
        println(stringToPrint)
    }

    private fun createIssue(client: GitHubClient) {
        val bodyJson = ("{\n" +
                "  \"title\": \"[Testing ignore] New GV Version Failing\",\n" +
                "  \"body\": \"New Version of GV is failing when compile!\"" +
                "}")

        val result = client.createIssue(REPO_OWNER, REPO_NAME, bodyJson)
        val successFul = result.first
        val responseData = result.second

        val stringToPrint = if (successFul) {
            val issueUrl = getUrlFromJSONString(responseData)
            "Issue Create take a look here $issueUrl"
        } else {
            "Unable to create issue \n $responseData"
        }

        println(stringToPrint)
    }

    private fun String.runCommand(workingDir: File = File(".")): Boolean {
        return try {
            val parts = this.split("\\s".toRegex())
            val process = ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()
            process.waitFor() == 0
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun getUrlFromJSONString(json: String): String {
        val jsonResponse = JsonSlurper().parseText(json)
        return (jsonResponse as Map<*, *>)["html_url"].toString()
    }

    private fun passOrFail(result: Boolean) = if (result) "passed" else "failed"
}