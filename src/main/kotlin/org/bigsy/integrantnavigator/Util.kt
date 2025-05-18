package org.bigsy.integrantnavigator

import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import cursive.gotoclass.ClojureGoToSymbolContributor
import cursive.parser.ClojureElementTypes
import cursive.psi.api.ClKeyword
import cursive.psi.api.symbols.ClSymbol

object Util {
    fun findImplementations(project: Project, scope: SearchScope): Iterable<ClKeyword> {
        val indicator = EmptyProgressIndicator()
        
        return ProgressManager.getInstance().runProcess<Iterable<ClKeyword>>({
            ClojureGoToSymbolContributor()
                .getItemsByName("init-key", "init-key", project, true)
                .filterIsInstance(ClSymbol::class.java)
                .filter { it.namespace == "integrant.core" }
                .flatMap { symbol ->
                    ReferencesSearch.search(symbol, scope)
                        .map { UsageInfo(it) }
                        .mapNotNull { info ->
                            info.element?.let { element ->
                                PsiTreeUtil.findSiblingForward(element, ClojureElementTypes.KEYWORD, null)?.let {
                                    it as? ClKeyword
                                }
                            }
                        }
                }
        }, indicator)
    }
}
