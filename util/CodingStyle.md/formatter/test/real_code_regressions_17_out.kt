/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's RobotTcpSession.kt:
// EnforceCallLineBreaking's Option 2 (renderCallPreserveGroups) groups a multi-line call's
// Arguments by *original source line*, not by argument. When one of several sibling arguments
// Is itself a multi-line brace body (a trailing/leading lambda), every line inside that body
// Became its own row, and since Kotlin has no `;` to separate statements (unlike C/C++/Java,
// Which this pass was originally written for), collapsing those rows onto fewer output lines
// Silently merged what were separate statements onto one line with nothing between them --
// Producing invalid Kotlin that wouldn't compile.
class Repro {

    fun startReaderThread(s: Socket)
    {
        val t = Thread( {
            try {
                val reader = BufferedReader( InputStreamReader( s.getInputStream() ) )
                while(true) {
                    val line = reader.readLine() ?: break
                    replyQueue.offer(line)
                }
                replyQueue.offer(DISCONNECTED)
            }
            catch(e: IOException) {
                replyQueue.offer(DISCONNECTED)
            }
        }, "tcp-reader" )
        t.isDaemon = true
        t.start()
    }

} // class Repro
