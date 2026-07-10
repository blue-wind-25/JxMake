/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's RobotTcpSession.kt:
// enforceCallLineBreaking's Option 2 (renderCallPreserveGroups) groups a multi-line call's
// arguments by *original source line*, not by argument. When one of several sibling arguments
// is itself a multi-line brace body (a trailing/leading lambda), every line inside that body
// became its own row, and since Kotlin has no `;` to separate statements (unlike C/C++/Java,
// which this pass was originally written for), collapsing those rows onto fewer output lines
// silently merged what were separate statements onto one line with nothing between them --
// producing invalid Kotlin that wouldn't compile.
class Repro {
    fun startReaderThread(s: Socket) {
        val t = Thread({
            try {
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                while (true) {
                    val line = reader.readLine() ?: break
                    replyQueue.offer(line)
                }
                replyQueue.offer(DISCONNECTED)
            } catch (e: IOException) {
                replyQueue.offer(DISCONNECTED)
            }
        }, "tcp-reader")
        t.isDaemon = true
        t.start()
    }
}
