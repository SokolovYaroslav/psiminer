import kotlinx.serialization.Serializable

@Serializable data class Config(
    /*
    Pipeline parameters
     */
    val format: String,
    val problem: String,

    /*
    Parser parameters
     */
    val resolveTypes: Boolean = true,
    val normalizeTokens: Boolean = true,
    val normalizeLiterals: Boolean = true,
    val splitNames: Boolean = false,
    val batchSize: Int = 1000,
    val removeKeyword: Boolean = false,
    val compressOperators: Boolean = false,
    val removeWhiteSpaces: Boolean = true,
    val removeComments: Boolean = true,
    val removeJavaDoc: Boolean = true,
    val removeAsterisks: Boolean = true,
    val removePrintableSymbols: Boolean = true,
    val removeEmptyLists: Boolean = true,
    val compressTree: Boolean = false,

    /*
    Code2Seq storage parameters
     */
    val maxPathWidth: Int? = 4,
    val maxPathLength: Int? = 9,
    val maxPathsInTrain: Int? = null,
    val maxPathsInTest: Int? = null,
    val resolvedTypesFirst: Boolean = false,
    val nodesToNumbers: Boolean = false,

    /*
    Filters and them parameters
     */
    val filters: List<String> = listOf(),
    val minTreeSize: Int = 0,
    val maxTreeSize: Int? = null,
    val minCodeLength: Int = 0,
    val maxCodeLength: Int? = null,

    /*
    Debugging
     */
    val printTrees: Boolean = false
)
