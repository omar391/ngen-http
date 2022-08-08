# All in one THREAD_SAFE http-client and tools in Java.

 * Some http libs (i.e.: OkHttp v3) are stateless (doesn't store session cookies) but this Library is built to be stateful to make it readily useable.
 * You could use a single instance of HttpInvoker for all of your tasks and threads, and its thread safe.
 * Use InvokerResponse's quit() method for preventing potential** memory leaks
 * ** = when "stream response types (string, byte[] responses are auto closed)" are consumed in different thread than InvokerResponse's thread.

<br>
<br>

# Future extensions and audits:
-[ ] (check via thread id)check if instance call from multi thread for all the methods are thread safe + if every part (i.e. InvokerRequest ) can be used without data-race (check all class variables)
<br>-[ ] set concurrent thread no for same host
<br>-[ ] download progress listener in InvokerResponse
<br>-[ ] add Lambda in Buffered sink methods
<br>-[ ] remove deprecated method from acceptAllSSLCerts method
<br>-[ ] implement proxy authentication
