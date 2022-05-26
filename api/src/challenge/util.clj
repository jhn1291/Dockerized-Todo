(ns challenge.util
  (:require [clojure.java.io :as io]
            [datomic.api :as d]
            [challenge.datomic :as cd]
            [clojure.edn :as edn]))

(defmacro if-let*
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

(defn list-dir
  "Given path, returns a set of directory contents"
  [path]
  (set (.list (io/file path))))

(defn read-user-file
  [email]
  (edn/read-string (slurp (str "db/" email))))

(defn keywordize-params
  "My middleware isn't quite working for the
   hiccup forms submitting to POST routes."
  [params]
  (into {} (map (fn [[k v]] [(keyword k) v]) params)))

(defn str-dbid->long-dbid
  [entity]
  (update entity :db/id #(Long/parseLong %)))

(defn now
  []
  (new java.util.Date))

(defn shape-todo-new
  [params]
  {:todo/content (:content params)
   :todo/state :todo.state/incomplete
   :todo/meta {:db/id (d/tempid :db.part/user) :todometa/created-on (now)}})

(def todo-state-string->todo-state-ident
  {"incomplete" :todo.state/incomplete
   "complete" :todo.state/complete
   "removed" :todo.state/removed})

(defn lookup-state-ident
  [state-string]
  (get todo-state-string->todo-state-ident state-string))

(defn lookup-state-string
  [state-ident]
  (get (clojure.set/map-invert todo-state-string->todo-state-ident) state-ident))

(defn set-todo-metadata
  [eid metadata]
  (let [meta-id (cd/query-todo-metadata eid)
        meta (cd/pull-todo-metadata meta-id)]
    (when meta-id
      (cd/transact [{:db/id meta-id (first metadata) (second metadata)}]))))

(defn retract-if-exists
  [e retract-key]
  (when (retract-key e)
    (cd/transact [[:db/retract (:db/id e) retract-key (retract-key e)]])))

(defn retract-todo-metadata
  [eid retract-keys]
  (let [meta-id (cd/query-todo-metadata eid)
        meta (cd/pull-todo-metadata meta-id)]
    (mapv #(retract-if-exists meta %) retract-keys)))

(def todo-fsm
  "A way to assert extra facts into the server upon state change.
   keys are a state-change tuple, values are either nil or a side-effecting fn.

   When todo already had a state, functions should expect one arg that is the :db/id
   of the todo.

   When we are creating a state from scractch, so any [:new *] state transistion,
   functions should expect one arg that is a todo-entity and return a modified todo-entity. "
  {[:new :todo.state/incomplete] nil
   [:todo.state/incomplete :todo.state/complete] #(set-todo-metadata % [:todometa/completed-on (now)])
   [:todo.state/complete :todo.state/incomplete] #(retract-todo-metadata % [:todometa/completed-on])
   [:todo.state/removed :todo.state/complete] #(set-todo-metadata % [:todometa/completed-on (now)])
   [:todo.state/removed :todo.state/incomplete] #(retract-todo-metadata % [:todometa/completed-on
                                                                       :todometa/removed-on])
   [:todo.state/complete :todo.state/removed] #(set-todo-metadata % [:todometa/removed-on (now)])
   [:todo.state/incomplete :todo.state/removed] #(set-todo-metadata % [:todometa/removed-on (now)])})

(defn shape-todo-update
  [params]
  (let [todo-id (:db/id params)
        current-state (get-in (cd/pull-todo-state todo-id) [:todo/state :db/ident])
        next-state (lookup-state-ident (:todo/state params))
        state-change-hook (get todo-fsm [current-state next-state])]
    (when state-change-hook
      (state-change-hook todo-id))
  (assoc params :todo/state next-state)))

(defn shape-todo-new
  [params next-state]
  (let [current-state :new
        state-change-hook (get todo-fsm [current-state next-state])
        new-todo-shell {:todo/content (:content params)
                        :todo/state :todo.state/incomplete
                        :todo/meta {:db/id (d/tempid :db.part/user) :todometa/created-on (now)}}]
    (if state-change-hook
      (state-change-hook new-todo-shell)
      new-todo-shell)))

