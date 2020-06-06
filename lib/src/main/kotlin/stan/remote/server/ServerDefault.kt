package stan.remote.server

import stan.remote.entity.Code
import stan.remote.entity.Response
import stan.remote.entity.response

internal val DEFAULT_ERROR_HOOK: (Throwable) -> Response = {
    response(
        code = Code.INTERNAL_SERVER_ERROR,
        body = "error: ${it.javaClass.name} ${it.message}"
    )
}

internal val DEFAULT_CODE_DESCRIPTION_HOOK: (Int) -> String = { code ->
    when (code) {
        Code.SUCCESS_OK -> "Success"
        Code.BAD_REQUEST -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        Code.NOT_FOUND -> "Not Found"
        Code.INTERNAL_SERVER_ERROR -> "Internal Server Error"
        else -> "Unknown"
    }
}
