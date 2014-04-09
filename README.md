# onelog

Batteries-included logging for Clojure. Covers the 80% case of all
logging in a process going to one file.

## Features
* One function call sets up logging with sane defaults - just
  `(start!)` and you're ready to go.
* Daily log rotation out of the box.
* `(start! "/foo/bar.log")` to log to a different file.
* Convenience methods for logging stacktraces, throwables, and
  nested throwable root causes.
* Warnings and errors are ANSI-colorized.
* Wraps log4j under the hood. Compatible with any log4j customizations
  you'd care to make.


## Usage

### Basic logging

The default logger logs to a file called `log/clojure.log` with a
timestamp and the log level prepended. Error messages are colored
bright red, warning messages are bright yellow, and everything else is
uncolored.


    user> (require '[onelog.core :as log])
    nil
    user> (log/start!)
    true
    user> (log/error "Hello, world!")
    nil
    user> (slurp "log/clojure.log")
    "2014-04-09 13:12:08,003 [ERROR] : [0mHello, world![0m\n"

    
### Logging to a different file


    user> (log/start! "/tmp/foo.log")
    true
    user> (log/error "A different logfile")
    nil
    user> (slurp "/tmp/foo.log")
    "2014-04-09 13:18:25,845 [ERROR] : [0mA different logfile[0m\n"


### Changing the logfile after startup
`start!` attempts to be idempotent; subsequent calls after the first one have no
effect unless the `force` argument is true. If you want to change the global logfile
after calling `start!`, you have to use the 3 argument version and set `force` to true:



    user> (log/start! "/tmp/bar.log" :info true)
    true
    user> (log/error "Hi")
    nil
    user> (slurp "/tmp/bar.log")
    "2014-04-09 13:19:22,103 [ERROR] : [0mHi[0m\n"


### Copying log messages to STDOUT

Appending a + (plus sign) to the standard logging functions will copy 
the messages to STDOUT in addition to logging them to the log file:


    user> (log/info+ "ABC 123")
    ABC 123
    nil
    user> (log/warn+ "DEF 456")
    DEF 456
    nil


### Logging exceptions and stack traces

A convenience method is provided to transform a throwable into a printable stacktrace.
If the exception has a cause exception embedded in it, it walks the chain of causes
until it finds the root exception and logs that, too.


    user> (log/error (log/throwable (Exception. "A test exception")))
    nil
    user> (println (slurp "/tmp/foo.log"))
    2014-04-09 13:46:49,097 [ERROR] : java.lang.Exception: A test exception
    user$eval9828.invoke(NO_SOURCE_FILE:1)
    clojure.lang.Compiler.eval(Compiler.java:6703)
    clojure.lang.Compiler.eval(Compiler.java:6666)
    clojure.core$eval.invoke(core.clj:2927)
    [...]


## Complex logging situations 

Onelog is designed to cover the 80% use case: log everything that happens in an app to one file.
Onelog is specifically not intended for the other 20% of cases where you have more complex logging
requirements (although most of its functions are compatible with more complex scenarios.)

If you have more complex logging needs, such as multiple logfiles for different parts of 
your app, you are probably better off using [clj-logging-config](https://github.com/malcolmsparks/clj-logging-config)
directly to set up your loggers. It may be helpful to
look at the Onelog source code to get an idea of how to wrangle the
logging configuration. You can still use the Onelog functions
as usual once you set up your logging backend. 

## License

Copyright © 2013-2014 Paul Legato.

Distributed under the Eclipse Public License, the same as Clojure.
