package com.martin.intellij.plugin.mockbuilder.action

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.lang.java.JavaImportOptimizer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.martin.intellij.plugin.mockbuilder.component.MockBuilderGeneratorProjectComponent

class MockBuilderGeneratorActionHandler : CodeInsightActionHandler
{
    override fun invoke(project: Project, editor: Editor, originalFile: PsiFile)
    {
        if (originalFile !is PsiJavaFile) return
        if (originalFile.classes.isEmpty()) return

        val mockBuilderComponent = project.getComponent(
                MockBuilderGeneratorProjectComponent::class.java)

        val mockBuilderClass = mockBuilderComponent.execute(originalFile)

        CodeStyleManager.getInstance(project).reformat(mockBuilderClass)

        JavaImportOptimizer().processFile(mockBuilderClass.containingFile).run()

        mockBuilderClass.navigate(true)
    }
}

//        val createTypeFromText = elementFactory.createTypeFromText(CommonClassNames.JAVA_LANG_STRING, null)

//var testDirectoryName = containingDirectory.name.replace("main", "test")
//
//val module = ModuleUtil.findModuleForFile(psiJavaFile.virtualFile, project)!!


//Messages.showMessageDialog("done", "My Message Box", Messages.getInformationIcon())




//        val mockClassEditor = PsiUtilBase.findEditor(mockBuilderClass)
//
//        val mockClassContext = SimpleDataContext.getSimpleContext(CommonDataKeys.EDITOR.name, mockClassEditor,
//                dataContext)
//
////        psiJavaFile.virtualFile.move()
//
//        OptimizeImportsAction.actionPerformedImpl(mockClassContext)
