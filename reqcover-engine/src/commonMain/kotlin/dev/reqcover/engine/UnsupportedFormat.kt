package dev.reqcover.engine

class UnsupportedFormat(uri: String, cause: Throwable? = null) :
    Exception("Unsupported format for requirements file at URI: $uri", cause)
