package dev.reqcover.engine

fun parseRequirementsUris(requirementsUris: String): List<String> =
    requirementsUris.split(",").map { it.trim() }.filter { it.isNotEmpty() }
