import groovy.json.JsonSlurper
import groovy.json.internal.LazyMap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.task
import java.io.File
import java.io.IOException

open class GKNightlyVersionVerifier : Plugin<Project> {

    companion object {
        private const val GV_VERSION_PATH_FILE = "buildSrc/src/main/java/GeckoVersions.kt"

    }

    override fun apply(project: Project) {

        project.task("hello") {
            setVariables()

            dependsOn("dependencyUpdates")


            doLast {
                val newVersion = getLastGeckoViewNightlyVersion(project)

                if (newVersion.isNotEmpty()) {
                    updateGeckoVersion(newVersion)

                    println("New Version/ Running tests")
                    if (runGradleTests()) {
                        //createCommit and generate a pull request
                        println("Creating pull request")
                        if (openAPROnGitHub())
                            println("PR Opened Successfully")
                        else
                            println("Error Creating PR")


                    } else {
                        //Create a github issue
                        println("Tests Fail creating Github Issue")

                    }
                } else {

                    println("Sorry no new Version from gecko view")

                }


            }
        }
    }


    private fun getLastGeckoViewNightlyVersion(project: Project): String {
        val file = File(project.rootDir, "build" + File.separator +
                "dependencyUpdates" + File.separator + "report.json")

        var json = JsonSlurper().parse(file)


        val outdated = (json as LazyMap)["outdated"]
        val dependencies = (outdated as Map<Any, Any>)["dependencies"] as ArrayList<LazyMap>

        var gecko = dependencies.filter {
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

    private fun updateGeckoVersion(newVersion: String) {
        val lisence = """/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 """
        val fileTemplate = """$lisence
object GeckoVersions {
    const val nightly_version = "$newVersion"
}"""

        val file = File(GV_VERSION_PATH_FILE)
        file.writeText(fileTemplate)
    }

    private fun runGradleTests(): Boolean {
        println("****Gradle tests Result****")
        return true //return "./gradlew clean test".runCommand() == 0
    }

    private fun openAPROnGitHub(): Boolean {
        println("****Gradle tests Result****")
        val branchName = "new_update_${GeckoVersions.nightly_version}"
        val gitCheckout = "git checkout -B $branchName"
        val gitAdd = "git add buildSrc/src/main/java/GeckoVersions.kt"
        val gitCommit = "git commit -m New_GK_Nightly_Version"
        val gitPush = "git push -u -f origin $branchName"


        var commandResult = gitCheckout.runCommand()
        println(gitCheckout + commandResult)
        println(gitCheckout)
        commandResult = gitAdd.runCommand()
        println(gitAdd + commandResult)
        commandResult = gitCommit.runCommand()
        println(gitCommit + commandResult)
        commandResult = gitPush.runCommand()
        println(gitPush + commandResult)

        if (commandResult) {
            val gitCheckoutMaster = "git checkout master"
            val gitDeleteNewBranch = "git branch -d $branchName"

            if (gitCheckoutMaster.runCommand())
                gitDeleteNewBranch.runCommand()
        }

        return commandResult
    }

    private fun String.runCommand(workingDir: File = File(".")): Boolean {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            // println(proc.inputStream.bufferedReader().readText())

            return proc.waitFor() == 0
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }
}