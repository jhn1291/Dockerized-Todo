(ns challenge.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.util.response :refer [response redirect]]
            [compojure.core :as c :refer [GET POST]]
            [compojure.route :as route]
            [challenge.datomic :as cd]
            [challenge.util :as util]
            [challenge.views.home :as home]
            [challenge.views.todo :as todo]
            [challenge.views.completion-chart :as c-chart]
            [challenge.views.burndown-chart :as bd-chart])
  (:gen-class))

(defonce server (atom nil))

(defn get-server-session
  [cookie]
  (get-in @server [:user-sessions cookie]))

(defn add-server-session!
  [cookie id]
  (swap! server assoc-in [:user-sessions cookie] id))

(defn remove-server-session!
  [cookie]
  (swap! server update-in [:user-sessions] dissoc cookie))

(defn make-todo
  [user params]
  (cd/transact [(update user :user/todos #(conj % (util/shape-todo-new params :todo.state/incomplete)))]))

(defn retract-entity
  [id]
  (cd/transact [[:db/retractEntity id]]))

(defn get-client-session
  [req]
  (get-in req [:cookies "ring-session" :value]))

(defn logged-in?
  "When true, returns the server's id for the client's session.
   When false, nil."
  [req]
  (get-server-session (get-client-session req)))


(c/defroutes routes

  (GET "/" [] (fn [req]
                (if (logged-in? req)
                    (redirect "/todo")
                    (home/home req))))

  (POST "/login" req (fn [req]
                       (let [email (:email (:params req))]
                         (if-let [userid (cd/query-user email)]
                           (add-server-session! (get-client-session req) userid)
                           (add-server-session! (get-client-session req) (cd/make-user email)))
                         (redirect "/todo"))))

  (POST "/logout" req (fn [req]
                        (remove-server-session! (get-client-session req))
                        (redirect "/")))

  (POST "/add-todo" req (fn [req]
                          (util/if-let* [user-id (logged-in? req)
                                         user (cd/pull-user user-id)]
                                        (do
                                          (make-todo user (:params req))
                                          (redirect "/todo"))
                              (redirect "/todo"))))

  (POST "/update-todo" req (fn [req]
                             (util/if-let* [user-id (logged-in? req)
                                            user (cd/pull-user user-id)]
                                   (let [todo (-> (util/keywordize-params (:params req))
                                                  util/str-dbid->long-dbid
                                                  util/shape-todo-update)]
                                     (cd/transact [todo])
                                     (redirect "/todo"))
                                   (redirect "/todo"))))

  (GET "/burndown" req (fn [req]
                         (util/if-let* [user-id (logged-in? req)
                                        user (cd/pull-user user-id)]
                               (bd-chart/burndown-chart (:user/todos user))
                               (redirect "/"))))

  (GET "/completion-chart" req (fn [req]
                                 (util/if-let* [user-id (logged-in? req)
                                           user (cd/pull-user user-id)
                                           grouped-todos (group-by #(-> % :todo/state :db/ident) (:user/todos user))]
                                       (c-chart/completion-chart grouped-todos)
                                       (redirect "/"))))

  (GET "/todo" req (fn [req]
                     (if-let [user-id (logged-in? req)]
                       (todo/todo (cd/pull-user user-id))
                       (redirect "/"))))

  (route/not-found {:status 404
                    :body "Not found."
                    :headers {"Content-Type" "text/plain"}}))

(def app (-> (fn [req] (routes req))
             wrap-keyword-params
             wrap-params
             (wrap-cookies {:max-age 3600})
             wrap-session))

(defn start-server []
  (cd/import-all-schemas)
  (swap! server
         assoc
         :jetty
         (jetty/run-jetty (fn [req] (app req))
                          {:port 3001
                           :join? false})
         :conn nil))

(defn stop-server []
  (when-some [s @server]
    (.stop (:jetty s))
    (reset! server nil)))

(defn -main []
  (start-server))
