/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.intellectualsites.http;

/*
 Internal abstraction over the HTTP transport layer.
 Implementations are responsible for executing a prepared {@link HttpRequest}
 and returning a populated {@link HttpResponse}. Any transport-level error
 must be surfaced as an exception so that {@link HttpRequest#executeRequest}
 can route it to the caller's exception handler.
 */
interface HttpExecutor {

    /*
     Execute the given request with the supplied timeout value.

     @param request HTTP request to execute
     @param timeout connect/read timeout in milliseconds; {@code <= 0} means use the library default
     @return Populated response; never {@code null}
     @throws Exception on any transport or serialization error
     */
    HttpResponse execute(HttpRequest request, int timeout) throws Exception;

}
