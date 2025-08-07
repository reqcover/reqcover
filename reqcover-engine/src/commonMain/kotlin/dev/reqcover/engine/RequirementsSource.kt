package dev.reqcover.engine

interface RequirementsSource {
    /**
     * Loads requirements from the specified URI.
     *
     * @param uri The URI from which to load requirements.
     * @return A set of requirement IDs loaded from the URI.
     * @throws UnsupportedFormat If the format of the requirements is not supported.
     * @throws NotFound If the requirements file cannot be found at the specified URI.
     */
    fun load(uri: String): Set<String>
}
