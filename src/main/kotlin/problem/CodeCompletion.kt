package problem

import GranularityLevel
import psi.PsiNode

class CodeCompletion : LabelExtractor {

    override val granularityLevel = GranularityLevel.File

    override fun processTree(root: PsiNode): Sample {
        return Sample(root, "")
    }

    companion object {
        const val name: String = "code completion"
    }
}
