(ns challenge.datomic
  (:require [datomic.api :as d]))

(def db (atom nil))
(def conn (atom nil))

(defn pull-ent
  [eid]
  (d/pull (d/db @conn) [:*] eid))

(defn pull-todo-state
  [eid]
  (d/pull (d/db @conn) [{:todo/state [:db/ident]}] eid))

(defn pull-user
  [eid]
  (d/pull (d/db @conn) [:* {:user/todos [:db/id
                                        :todo/content
                                        {:todo/state [:db/ident]}
                                        {:todo/meta [:*]}]}] eid))

(defn pull-todo-metadata
  [eid]
  (d/pull (d/db @conn) [:*] eid))

(defn query-todo-metadata
  [eid]
  (ffirst (d/q '[:find ?e
                 :in $ ?eid
                 :where [?eid :todo/meta ?e]]
               (d/db @conn) eid)))

(defn query-user
  [email]
  (ffirst (d/q '[:find ?e
                 :in $ ?email
                 :where [?e :user/email ?email]]
               (d/db @conn) email)))

(def schema
  {:user [{:db/ident :user/email
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity
            :db/doc "The user's email address"}
           {:db/ident :user/todos
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many
            :db/isComponent true ;; A user owns their todos
            :db/doc "The user's todos"}]
   :state-enums [{:db/ident :todo.state/incomplete}
                 {:db/ident :todo.state/complete}
                 {:db/ident :todo.state/removed}]
   :meta [{:db/ident :todometa/created-on
           :db/valueType :db.type/instant
           :db/cardinality :db.cardinality/one
           :db/doc "Date created"}
          {:db/ident :todometa/completed-on
           :db/valueType :db.type/instant
           :db/cardinality :db.cardinality/one
           :db/doc "Date completed"}
          {:db/ident :todometa/removed-on
           :db/valueType :db.type/instant
          :db/cardinality :db.cardinality/one
          :db/doc "Date removed"}]
   :todos [{:db/ident :todo/content
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one
            :db/doc "Content of a todo"}
           {:db/ident :todo/state
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one
            :db/doc "State of a todo"}
           {:db/ident :todo/meta
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one
            :db/doc "Todo metadata"}]})

(defn import-all-schemas
  []
  (def uri "datomic:dev://datomicdb:4334/todo")
  (reset! db (d/create-database uri))
  (reset! conn (d/connect uri))
  (mapv #(d/transact @conn (val %)) schema))

(defn transact
  [txdata]
  @(d/transact @conn txdata))

(defn make-user
  [email]
  (-> (transact [{:user/email email}])
      :tempids
      first
      val))
