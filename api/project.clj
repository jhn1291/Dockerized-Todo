(defproject challenge "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [ring/ring-core "1.9.1"]
                 [ring/ring-jetty-adapter "1.9.1"]
                 [compojure "1.6.2"]
                 [hiccup "1.0.5"]
                 [com.datomic/datomic-pro "1.0.6397"]]
  :main challenge.core
  :aot [challenge.core]
  :repl-options {:init-ns challenge.core})
