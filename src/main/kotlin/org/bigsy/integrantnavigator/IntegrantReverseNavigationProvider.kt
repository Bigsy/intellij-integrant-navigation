@file:Suppress("UnstableApiUsage")

package org.bigsy.integrantnavigator

import com.intellij.navigation.DirectNavigationProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import cursive.ClojureLanguage
import cursive.psi.api.*
import cursive.psi.impl.ClEditorKeyword
import cursive.psi.impl.ClSharp
import cursive.psi.impl.ClTaggedLiteralImpl
import cursive.psi.api.symbols.ClSymbol

class IntegrantReverseNavigationProvider : DirectNavigationProvider {

    private fun isInInitKeyFunction(element: PsiElement): Boolean {
        if (element.containingFile.language != ClojureLanguage.getInstance()) {
            return false
        }
        val defList = PsiTreeUtil.getParentOfType(element, ClList::class.java)
        if (defList == null) {
            return false
        }
        val symbols = PsiTreeUtil.findChildrenOfType(defList, ClSymbol::class.java)
        if (symbols.isEmpty()) {
            return false
        }
        val firstSymbol = symbols.first()
        if (firstSymbol.text != "defmethod") {
            return false
        }
        if (symbols.size < 2) {
            return false
        }
        val secondSymbol = symbols.elementAt(1)
        val isInitKey = secondSymbol.text == "integrant.core/init-key" || secondSymbol.text == "ig/init-key"
        return isInitKey
    }
    
    private fun findKeywordInDefmethod(element: PsiElement): ClKeyword? {
        if (element is ClKeyword) {
            return element
        }
        val defList = PsiTreeUtil.getParentOfType(element, ClList::class.java) ?: return null
        val symbols = PsiTreeUtil.findChildrenOfType(defList, ClSymbol::class.java)
        if (symbols.size < 2) {
            return null
        }
        val keywords = PsiTreeUtil.findChildrenOfType(defList, ClKeyword::class.java)
        if (keywords.isEmpty()) {
            return null
        }
        val keyword = keywords.first()
        return keyword
    }
    
    private fun getNamespaceFromFile(file: PsiFile): String? {
        val lists = PsiTreeUtil.findChildrenOfType(file, ClList::class.java)
        for (list in lists) {
            val symbols = PsiTreeUtil.findChildrenOfType(list, ClSymbol::class.java)
            if (symbols.isNotEmpty() && symbols.first().text == "ns") {
                if (symbols.size > 1) {
                    return symbols.elementAt(1).text
                }
            }
        }
        return null
    }

    private data class ReferenceInfo(
        val element: PsiElement,
        val isInIgRef: Boolean,
        val isFirstChild: Boolean,
        val isFullyQualified: Boolean = false,
        val matchScore: Int = 0
    )

    private fun findConfigReferences(keyword: ClKeyword): List<PsiElement> {
        val keywordText = keyword.text
        val module = ModuleUtilCore.findModuleForPsiElement(keyword)
        if (module == null) {
            return emptyList()
        }
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        val ednFiles = FilenameIndex.getAllFilesByExt(keyword.project, "edn", scope)
        val allReferences = mutableListOf<ReferenceInfo>()
        val keywordName = when {
            keywordText.startsWith("::") -> keywordText.substring(2)
            keywordText.startsWith(":") -> keywordText.substring(1)
            else -> keywordText
        }
        val namespace = getNamespaceFromFile(keyword.containingFile)
        val fullyQualifiedKeyword = if (namespace != null && keywordText.startsWith("::")) {
            ":$namespace/$keywordName"
        } else {
            keywordText
        }
        for (file in ednFiles) {
            val psiFile = file.toPsiFile(keyword.project) ?: continue
            val allKeywords = PsiTreeUtil.findChildrenOfType(psiFile, ClKeyword::class.java)
            for (kw in allKeywords) {
                val kwText = kw.text
                val kwName = when {
                    kwText.startsWith("::") -> kwText.substring(2)
                    kwText.startsWith(":") -> kwText.substring(1)
                    else -> kwText
                }
                val hasNamespace = kwName.contains("/")
                val kwSimpleName = if (hasNamespace) {
                    val parts = kwName.split("/")
                    if (parts.size > 1) parts[1] else kwName
                } else {
                    kwName
                }
                val isMatch = kwText == keywordText || 
                              kwText == fullyQualifiedKeyword ||
                              (hasNamespace && kwSimpleName == keywordName)
                if (isMatch) {
                    val parentTaggedLiteral = PsiTreeUtil.getParentOfType(kw, ClTaggedLiteralImpl::class.java)
                    val isInIgRef = parentTaggedLiteral?.let {
                        val sharp = PsiTreeUtil.findChildOfType(it, ClSharp::class.java)
                        sharp?.containedElement?.text == "ig/ref"
                    } ?: false
                    val parentList = PsiTreeUtil.getParentOfType(kw, ClList::class.java)
                    val isFirstChild = parentList?.let {
                        val firstNonWhitespaceChild = PsiTreeUtil.findChildrenOfType(it, PsiElement::class.java)
                            .firstOrNull { child -> child !is PsiWhiteSpace }
                        firstNonWhitespaceChild == kw
                    } ?: false
                    allReferences.add(ReferenceInfo(
                        element = kw,
                        isInIgRef = isInIgRef,
                        isFirstChild = isFirstChild,
                        isFullyQualified = hasNamespace,
                        matchScore = when {
                            kwText == fullyQualifiedKeyword && !isInIgRef -> 0
                            hasNamespace && !isInIgRef -> 1
                            !isInIgRef -> 2
                            else -> 3
                        }
                    ))
                }
            }
        }
        val sortedReferences = allReferences.sortedWith(compareBy(
            { it.matchScore },
            { !it.isFirstChild }
        ))
        return sortedReferences.map { it.element }
    }

    override fun getNavigationElement(element: PsiElement): PsiElement? {
        if (element.language != ClojureLanguage.getInstance()) {
            return null
        }
        if (isInInitKeyFunction(element)) {
            val keyword = findKeywordInDefmethod(element)
            if (keyword == null) {
                return null
            }
            val references = findConfigReferences(keyword)
            val firstRef = references.firstOrNull()
            return firstRef
        }
        return null
    }
    
    private fun com.intellij.openapi.vfs.VirtualFile.toPsiFile(project: com.intellij.openapi.project.Project): PsiFile? {
        return com.intellij.psi.PsiManager.getInstance(project).findFile(this)
    }
}
