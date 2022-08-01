package biz.lermitage.oga

enum class DependencyState {

    /** Dependency officially migrated to new coordinates. */
    MIGRATED,

    /** Dependency is abandoned and has no official successor, but there are some candidates. */
    ABANDONED
}
