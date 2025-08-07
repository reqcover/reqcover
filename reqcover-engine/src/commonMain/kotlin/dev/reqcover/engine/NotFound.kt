package dev.reqcover.engine

class NotFound(uri: String, cause: Throwable? = null) :
    Exception("Requirements file not found at URI: $uri", cause)
