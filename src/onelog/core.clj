(ns onelog.core
  "Batteries-included logging for Clojure. You can require this one
file and begin logging, with no further configuration necessary.


BUG - TODO: Fix so that (set-default-logger!) gets called automatically if
the user tries to log without calling it first.

TODO: Add profiling methods (i.e. run a function and log how long it took)
"
  (:require
   [clojure.tools.logging :as log]
   [io.aviso.exception :refer [write-exception format-exception]]
   [clj-logging-config.log4j :as log-config]
   [clansi.core :as ansi])
  (:import [org.apache.log4j DailyRollingFileAppender EnhancedPatternLayout FileAppender]))


(defn color
  "ANSI colorizes the given data and returns it.

   The first argument is a either a color specifier as per clansi.core, or a collection
   of color specifiers. For example, :white or [:bright :white] are valid

  Subsequent arguments are concatenated into a string.

   Examples:

     (log/error (log/color [:bright :red] \"foo\"))
     (log/error (log/color :red \"foo\"))
"
  [colors & data]
  (let [colors (if (coll? colors)
                 colors
                 (vector colors))]
    (apply ansi/style (apply str data) colors)))


(def ^:dynamic *logfile* "log/clojure.log")
(def ^:dynamic *loglevel* :info)

(def ^:dynamic *warn-color* [:bright :yellow])
(def ^:dynamic *error-color* [:bright :red])


;; If set to true, will copy all log messages to STDOUT in addition to logging them
(def ^:dynamic *copy-to-console* false)
(defmacro with-console
  "Executes the given code block with all log messages copied to
  STDOUT in addition to logging them."
  [ & forms]
  `(binding [*copy-to-console* true] ~@forms))


;; The generation of the calling class, line numbers, etc. is
;; extremely slow, and should be used only in development mode or for
;; debugging. Production code should not log that information if
;; performance is an issue. See
;; http://logging.apache.org/log4j/companions/extras/apidocs/org/apache/log4j/EnhancedPatternLayout.html
;; for information on which fields are slow.
;;
;; TODO: Make these functions, somehow, so that it can alter the
;; spacing dynamically; e.g. if an %x (execution context message) is
;; present, log it, otherwise ignore it. Putting a %x in prints "null"
;; to the log if none is set, which we don't want, which is why it's
;; not here now.
;;
(def debugging-log-prefix-format "%d (%t) %l [%p] : (%x) %throwable%m%n")
(def production-log-prefix-format "%d (%t) [%p] : (%x) %throwable%m%n")


;; Some basic logging adapters.
;; Many other logging infrastructures are possible. There are syslog
;; adapters, network socket adapters, etc.
;; For more information, see:
;; - https://github.com/malcolmsparks/clj-logging-config
;; - http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/AppenderSkeleton.html for different log destinations
;; - http://logging.apache.org/log4j/companions/extras/apidocs/org/apache/log4j/EnhancedPatternLayout.html for prefix formatting options
;;
;; TODO: Make a custom layout class that colorizes the log level. Maybe this can be done in a filter.
;;
(defn rotating-logger
  [logfile]
  "Returns a logging adapter that rotates the logfile nightly at about midnight."
  (DailyRollingFileAppender.
   (EnhancedPatternLayout. production-log-prefix-format)
   logfile
   ".yyyy-MM-dd"))

(defn appender-for-file
  "Returns a FileAppender for the given file."
  [logfile]
  (FileAppender.
   (EnhancedPatternLayout. production-log-prefix-format)
   logfile
   true))

;; 1-arity function which makes a log appender for the given filename
(def ^:dynamic *appender-fn* rotating-logger)


(defn set-namespace-logger!
  ([ns log-level log-adapter]
     "Specify a specific logging appender for the given
namespace. This is only necessary if you don't want logs for
that namespace to go to the default general logfile.
"
     (log-config/set-logger! ns
                             :level log-level
                             :out (*appender-fn* *logfile*)))
  ([logfile]
     "Specify a specific logfile for the current namespace. This is
only necessary if you don't want to use the general default logfile
for that namespace."
     (set-namespace-logger! (str *ns*)
                  *loglevel*
                  (*appender-fn* logfile))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Starting the logger
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def initialized (atom false))
(defn start!
  "Sets a default, appwide log adapter. Optional arguments set the
default logfile and loglevel. If no logfile is provided, logs to
log/clojure.log from the current working directory.

If 'initialized' is true, does nothing unless 'force' is true. This is
so that multiple libraries can all call this in the same project and
share the same logfile, while each also specifying a default logfile
for itself when used separately.
"
  ([logfile loglevel force]
     (when (or (not @initialized) force)
       (log-config/set-loggers! :root
                                {:level loglevel
                                 :out (rotating-logger logfile)})
       (swap! initialized (constantly true))))
  ([logfile loglevel] (start! logfile loglevel false))
  ([logfile] (start! logfile *loglevel*))
  ([] (start! *logfile* *loglevel*)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Convert throwables into strings
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn stacktrace
  "Converts a Throwable into a sequence of strings with the stacktrace."
  [^Throwable throwable]
  (clojure.string/join "\n" (doall (map str (.getStackTrace throwable)))))

(defn root-cause
  "Returns the last 'cause' Throwable in a chain of Throwables.
 (From http://clojuredocs.org/clojure_core/clojure.stacktrace/root-cause)"
  [^Throwable tr]
  (if-let [cause (.getCause tr)]
    (recur cause)
    tr))

(defn throwable
  "Renders a single Throwable into string form. Includes the class name,
  message, and the stacktrace.

 If there is a chain of causes present, also logs the root cause."
  [^Throwable tr]
  ;; We used to do our own formatting here, but io.aviso.exception is
  ;; so awesome that we just use that now...
  (format-exception tr))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Adjust the log level
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn set-log-level!
  "Sets the global log level to the given level. Levels are keywords
  - :debug, :info, :warn, etc."
  [level]
  (log-config/set-logger-level! :root level))


(defn set-trace! [] (set-log-level! :trace))
(defn set-debug! [] (set-log-level! :debug))
(defn set-info!  [] (set-log-level! :info))
(defn set-warn!  [] (set-log-level! :warn))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn println-stderr
  [& forms]
  (binding [*out* *err*]
    (apply println forms)))





;; Unfortunately, log/warn, log/error, etc. are all macros, which
;; makes generating higher order functions on them annoying. So we convert them to functions.
(defn trace 
  [& forms]
  (log/trace (apply str forms)))

(defn debug
  [& forms]
  (log/debug (apply str forms)))

(defn info
  [& forms]
  (log/info (apply str forms)))

(defn warn
  [& forms] 
  (log/warn (apply color [:bright :yellow] forms)))

(defn error
  [& forms] 
  (log/error (apply color [:bright :red] forms)))

(defn fatal
  [& forms] 
  (log/fatal (apply color [:bright :red] forms)))

(defmacro plus-logger
  "Defines a new logger function called by the given symbol name suffixed with a plus.
   This function copies forms given to it to STDERR in addition to
  logging them at the specified level.

   For example, (plus-logger info) creates a function called
  info+. Calling (info+ \"foo\") is the same as calling (info \"foo\"),
  except that the message will be written to STDERR in addition to
  logged at the :info level.  "
  [logger-sym]
  (let [fn-name (symbol (str logger-sym "+"))]
    `(def ~fn-name
       (fn
         [& forms#]
         (let [forms# (apply str forms#)]
           (println-stderr (str "[" (.toUpperCase (str '~logger-sym)) "] " forms#))
           (~logger-sym forms#))))))

(plus-logger trace)
(plus-logger info)
(plus-logger debug)
(plus-logger warn)
(plus-logger error)
(plus-logger fatal)


(defmacro spy
  "Evaluates the given forms, logs their result at debug level, and then
  returns their result. You can then take a given form and insert
  a (spy (foo)) around it to trace its operation."
  [& forms]
  `(let [result# (do ~@forms)]
     (debug "[SPY] Evaluated forms: " (color [:bright :white] '~@forms) 
            " and got: " (color [:magenta] result#) 
            " which is a: " (class result#))
     result#))


(defmacro spy+
  "Like spy, but copies messages to STDERR in addition to logging them."
  [& forms]
  `(let [result#  (do ~@forms)
         message# (str "[SPY] Evaluated forms: " (color [:bright :white] '~@forms) 
                       " and got: " (color [:magenta] result#) 
                       " which is a: " (class result#))]
     (println-stderr message#)
     (debug message#)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Deprecated code, for backwards compatibility
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-default-logger!
  "Deprecated old name for start!."
  [ & args]
  (apply start! args)
  (error+ "set-default-logger! is deprecated - change your code to use start! instead."))

