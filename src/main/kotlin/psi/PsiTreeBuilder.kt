package psi

import Config
import astminer.common.normalizeToken
import astminer.common.splitToSubtokens
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.ElementType
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.elementType
import psi.PsiNode.Companion.EMPTY_TOKEN

class PsiTreeBuilder(private val config: Config) {
    private val typeResolver = PsiTypeResolver(config)

    fun buildPsiTree(root: PsiElement): PsiNode {
        val tree = convertPsiElement(root, null)
        return if (config.compressTree) compressSingleChildBranches(tree)
        else tree
    }

    private fun convertPsiElement(node: PsiElement, parent: PsiNode?): PsiNode {
        val resolvedType = typeResolver.resolveType(node)
        val printableType = getPrintableType(node)
        val currentNode = PsiNode(node, parent, resolvedType, printableType)

        // Iterate over the children
        val children = node.children
            .filter { validatePsiElement(it) }
            .map { kid -> convertPsiElement(kid, currentNode) }
        currentNode.setChildren(children)

        // Set token if leaf
        if (children.isEmpty()) {
            val token = node.text
                .replace("\\", "\\\\")
                .replace("\b", "\\b")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\"", "\\\"")
                .replace("\t", "    ")
            val splitToken = splitToSubtokens(node.text).joinToString("|")

            currentNode.setNormalizedToken(
                if (config.normalizeLiterals && numberLiterals.contains(node.elementType)) {
                    if (numberWhiteList.contains(node.text)) node.text else NUMBER_LITERAL
                } else if (config.splitNames && splitToken.isNotEmpty()) {
                    splitToken
                } else if (config.normalizeTokens) {
                    normalizeToken(node.text, EMPTY_TOKEN)
                } else {
                    token
                }
            )
        }

        return currentNode
    }

    private fun validatePsiElement(node: PsiElement): Boolean {
        val isJavaPrintableSymbol = config.removePrintableSymbols && skipElementTypes.any { node.elementType == it }
        val isEmptyList = config.removeEmptyLists && (node.children.isEmpty() || node.text == "()") && listTypes.any { it.isInstance(node) }
        val isSkipWhiteSpace = config.removeWhiteSpaces && node is PsiWhiteSpace
        val isSkipAsterisks = config.removeAsterisks && node.toString() == "PsiDocToken:DOC_COMMENT_LEADING_ASTERISKS"
        val isSkipKeyword = config.removeKeyword && node is PsiKeyword
        val isSkipOperator = config.compressOperators && ElementType.OPERATION_BIT_SET.contains(node.elementType)
        val isSkipComment = config.removeComments && node is PsiComment && node !is PsiDocComment
        val isSkipJavaDoc = config.removeJavaDoc && node is PsiDocComment
        return !(
                    isJavaPrintableSymbol ||
                    isEmptyList ||
                    isSkipAsterisks ||
                    isSkipWhiteSpace ||
                    isSkipKeyword ||
                    isSkipOperator ||
                    isSkipComment ||
                    isSkipJavaDoc
                )
    }

    private fun getPrintableType(node: PsiElement): String? {
        if (!config.compressOperators) return null
        return when (node) {
            is PsiBinaryExpression -> "${node.elementType}:${node.operationSign.elementType}"
            is PsiPrefixExpression -> "${node.elementType}:${node.operationSign.elementType}"
            is PsiPostfixExpression -> "${node.elementType}:${node.operationSign.elementType}"
            is PsiAssignmentExpression -> "${node.elementType}:${node.operationSign.elementType}"
            else -> null
        }
    }

    private fun compressSingleChildBranches(node: PsiNode): PsiNode {
        val compressedChildren = node.getChildren().map { compressSingleChildBranches(it) }
        return if (compressedChildren.size == 1) {
            val child = compressedChildren.first()
            val compressedNode = PsiNode(
                child.wrappedNode,
                node.getParent(),
                child.resolvedTokenType,
                "${node.getTypeLabel()}|${child.getTypeLabel()}"
            )
            compressedNode.setNormalizedToken(child.getNormalizedToken())
            compressedNode.setChildren(child.getChildren())
            compressedNode
        } else {
            node.setChildren(compressedChildren)
            node
        }
    }

    companion object {
        private const val NUMBER_LITERAL = "<NUM>"

        private val numberLiterals = TokenSet.orSet(ElementType.INTEGER_LITERALS, ElementType.REAL_LITERALS)
        private val numberWhiteList = listOf("0", "1", "32", "64")
        private val skipElementTypes = listOf(
            ElementType.LBRACE,
            ElementType.RBRACE,
            ElementType.LBRACKET,
            ElementType.RBRACKET,
            ElementType.LPARENTH,
            ElementType.RPARENTH,
            ElementType.SEMICOLON,
            ElementType.COMMA,
            ElementType.DOT,
            ElementType.ELLIPSIS,
            ElementType.AT
        )
        val listTypes = listOf(
            PsiReferenceParameterList::class, PsiReferenceParameterList::class, PsiModifierList::class,
            PsiReferenceList::class, PsiTypeParameterList::class, PsiExpressionList::class,
            PsiParameterList::class, PsiExpressionListStatement::class, PsiAnnotationParameterList::class
        )
    }
}
