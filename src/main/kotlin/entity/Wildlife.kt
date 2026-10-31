package entity

/**
 * Enum um die einzelnen Wildlifes aufzuzählen
 */
enum class Wildlife {
    BEAR,
    FOX,
    HAWK,
    SALMON,
    ELK,
    ;

    /**
     * überschreibt die enum zu strings
     */
    override fun toString() = when(this) {
        BEAR -> "Bear"
        FOX -> "Fox"
        HAWK -> "Hawk"
        SALMON -> "Salmon"
        ELK -> "Elk"
    }


}