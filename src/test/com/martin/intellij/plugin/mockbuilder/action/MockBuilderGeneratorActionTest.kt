package com.martin.intellij.plugin.mockbuilder.action

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.martin.intellij.plugin.common.util.PsiUtils
import com.martin.intellij.plugin.mockbuilder.component.MockBuilderGeneratorProjectComponent
import org.junit.After
import org.junit.Before
import org.junit.Test

class MockBuilderGeneratorActionTest : LightCodeInsightFixtureTestCase()
{
    @Before
    override fun setUp()
    {
        super.setUp()

        PsiTestUtil.addSourceRoot(myFixture.module, myFixture.tempDirFixture.findOrCreateDir("main"), false)
        PsiTestUtil.addSourceRoot(myFixture.module, myFixture.tempDirFixture.findOrCreateDir("test"), true)
    }

    @After
    override fun tearDown()
    {
        super.tearDown()
    }

    override fun getProjectDescriptor(): LightProjectDescriptor
    {
        return object : LightProjectDescriptor()
        {
            override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry)
            {
                PsiTestUtil.addLibrary(module, model, "easymock", "lib", "easymock-3.4.jar")

                super.configureModule(module, model, contentEntry)
            }

            override fun getSdk() =
                JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false)
        }
    }

    override fun getTestDataPath(): String
    {
        return "testdata/mockbuilder"
    }

    @Test
    fun `test mock is built correctly`()
    {
        myFixture.configureByFile("$testDataPath/actual/SomeService.java")

        executeMockBuilding()

        myFixture.checkResultByFile(
            "test/com/martin/SomeServiceMockBuilder.java",
            "expected/SomeServiceMockBuilder.java",
            true
        )
    }

    private fun executeMockBuilding()
    {
        val subjectClass = JavaPsiFacade.getInstance(project).findClass("SomeService", GlobalSearchScope.allScope(project))!!

        val subjectFile = subjectClass.containingFile as PsiJavaFile
        val targetDirectory = PsiUtils.createDirectoryByPackageName(subjectFile, project, "com.martin")

        WriteActionWrapper(project, subjectFile) {
            project.getComponent(MockBuilderGeneratorProjectComponent::class.java).execute(subjectClass, targetDirectory)
        }.perform()
    }
}
