package com.denshlk.collapsefiles

import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitVisible
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.driver.execute
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.waitForDumbMode
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.JTreeUiComponent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class CollapseFilesIntegrationTest {

    init {
        // Configure CI server to fail tests on IDE exceptions
        di = DI {
            extend(di)
            bindSingleton<CIServer>(overrides = true) {
                object : CIServer by NoCIServer {
                    override fun reportTestFailure(
                        testName: String,
                        message: String,
                        details: String,
                        linkToLogs: String?
                    ) {
                        fail { "$testName fails: $message. \n$details" }
                    }
                }
            }
        }
    }

    companion object {
        private lateinit var testProjectPath: Path
        
        @BeforeAll
        @JvmStatic
        fun setupTestProject() {
            // Get the path to our comprehensive test project
            testProjectPath = Paths.get("src/integrationTest/testData/test-project").toAbsolutePath()
            println("Test project path: $testProjectPath")
        }
    }

    private fun withTestProjectTree(testBlock: (JTreeUiComponent) -> Unit) {
        // Create a test context that opens the IDE with our test project
        Starter.newContext(
            "pluginInstallationTest",
            TestCase(IdeProductProvider.IC, LocalProjectInfo(testProjectPath)).withVersion("2025.1")
        ).apply {
            // Install the plugin from the built distribution
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            if (pathToPlugin != null) {
                println("Installing plugin from: $pathToPlugin")
                PluginConfigurator(this).installPluginFromFolder(File(pathToPlugin))
            } else {
                println("WARNING: path.to.build.plugin system property not set")
            }

            // we don't need indexing for tests, disable to save performance
            this.disableOrdinaryIndexes()
            this.setSharedIndexesDownload(false)
            this.skipGitLogIndexing()
            this.allowSkippingFullScanning()
            this.skipIndicesInitialization()

            this.executeRightAfterIdeOpened()
        }.runIdeWithDriver().useDriverAndCloseIde {
            println("IDE started successfully with test project loaded")

            // Wait for the IDE to initialize
            execute(CommandChain().waitForDumbMode(10))
            println("Dumb mode engaged")

            ideFrame {
                val tree = this.projectView().projectViewTree
                tree.waitVisible()
                tree.waitContainsText("test-project")
                tree.waitNoTexts("loading...")
                testBlock(tree)
            }
        }
    }

    private fun isPathVisible(tree: JTreeUiComponent, path: Array<String>): Boolean {
        return tree.collectExpandedPaths().find {  path.toList() == it.path.drop(1) } != null
    }

    private fun assertCollapsed(tree: JTreeUiComponent, parent: String, nodeTextPrefix: String) {
        assertTrue(tree.hasSubtext(nodeTextPrefix)) {
            "Collapsed node starting with '$nodeTextPrefix' not found under '$parent'"
        }
    }

    private fun assertNotCollapsed(tree: JTreeUiComponent, parent: String, items: List<String>) {
        items.forEach { item ->
            assertTrue(tree.hasText(item)) { "Item '$item' should be visible under '$parent'" }
        }
    }

    private fun uncollapse(tree: JTreeUiComponent, nameContains: String) {
        val idx = tree.collectExpandedPaths().indexOfFirst { row ->
            row.path.any { it.contains(nameContains) }
        }
        tree.doubleClickRow(idx)
    }

    private fun expandAndCheck(
        tree: JTreeUiComponent,
        parent: String,
        check: () -> Unit
    ) {
        tree.collapsePath(parent)

        tree.expandPath(parent)
        check()
        tree.collapsePath(parent)
    }

    @Test
    fun testBelowThreshold() {
        val items = (1..9).map { "item${it.toString().padStart(2, '0')}" }
        
        withTestProjectTree { tree ->
            expandAndCheck(tree, "below-threshold") {
                assertNotCollapsed(tree, "below-threshold", items)
            }
        }
    }

    @Test
    fun testManyFoldersCollapse() {
        withTestProjectTree { tree ->
            expandAndCheck(tree, "many-folders") {
                assertCollapsed(tree, "many-folders", "folder01|folder02")
                val folderNames = (1..15).map { "folder${it.toString().padStart(2, '0')}" }
                folderNames.forEach {
                    assertFalse(tree.hasText(it)) { "Folder $it should be collapsed" }
                }
            }
        }
    }

    @Test
    fun testManyFilesCollapse() {
        withTestProjectTree { tree ->
            expandAndCheck(tree, "many-files") {
                assertCollapsed(tree, "many-files", "file01.txt|file02.txt")
                val fileNames = (1..15).map { "file${it.toString().padStart(2, '0')}.txt" }
                fileNames.forEach {
                    assertFalse(tree.hasText(it)) { "File $it should be collapsed" }
                }
            }
        }
    }

    @Test
    fun testMixedScenarioCollapse() {
        withTestProjectTree { tree ->
            expandAndCheck(tree, "mixed-scenario") {
                assertCollapsed(tree, "mixed-scenario", "folder01|folder02")
                assertCollapsed(tree, "mixed-scenario", "file01.txt|file02.txt")
            }
        }
    }

    @Test
    fun testCollapsedNodeExpansion() {
        withTestProjectTree { tree ->
            expandAndCheck(tree, "many-folders") {
                assertCollapsed(tree, "many-folders", "folder01|folder02")
                uncollapse(tree, "folder01|folder02")
                assertFalse(tree.hasSubtext("folder01|folder02")) { "Collapsed node should be gone" }
                val folderNames = (1..15).map { "folder${it.toString().padStart(2, '0')}" }
                assertNotCollapsed(tree, "many-folders", folderNames)
            }
        }
    }

    @Test
    fun testOpenFilePreventsCollapse() {
        withTestProjectTree { tree ->
            // expand to open file
            tree.expandPath("many-folders")
            uncollapse(tree, "folder01|folder02")
            tree.expandPath("many-folders", "folder05")
            tree.doubleClickPath("many-folders", "folder05", "dummy.txt")

            expandAndCheck(tree, "many-folders") {
                // folder05 and its parents are protected from collapse
                assertTrue(tree.hasText("folder05")) { "folder05 should be visible" }

                // Folders 1-4 are below threshold and should not collapse
                val firstFolders = (1..4).map { "folder${it.toString().padStart(2, '0')}" }
                assertNotCollapsed(tree, "many-folders", firstFolders)

                // Folders 6-15 are 10 items and should collapse
                assertCollapsed(tree, "many-folders", "folder06|folder07")
            }
        }
    }
} 