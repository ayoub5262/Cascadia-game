package entity

/**
 * Enum um die einzelnen Habitate aufzuzählen
 */
enum class Habitat {
    MOUNTAINS,
    RIVERS,
    FORESTS,
    WETLANDS,
    PRAIRIES,
    ;

    /**
     * überschreibt die enum zu strings
     */
    override fun toString() = when(this) {
        MOUNTAINS -> "Mountains"
        RIVERS -> "Rivers"
        FORESTS -> "Forest"
        WETLANDS -> "Wetlands"
        PRAIRIES -> "Prairies"
    }


}