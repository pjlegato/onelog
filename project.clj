(defproject onelog "0.5.1-SNAPSHOT"
  :description "Batteries-included logging for Clojure"
  :url "https://github.com.com/pjlegato/onelog"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/tools.logging "0.2.6"]
                 [clj-logging-config "1.9.10" :exclusions [log4j]]
                 [org.clojars.pjlegato/clansi "1.3.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.7"]
                 [io.aviso/pretty "0.1.30"] ;; Pretty printer / exception formatter
                 ])

