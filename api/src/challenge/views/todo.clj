(ns challenge.views.todo
  (:require [hiccup.core :as h]
            [challenge.util :as util]
            [hiccup.form :refer :all]))

(defn todo
  [user-data]
  (h/html [:div [:h1 (str "Welcome, " (:user/email user-data))]
           (form-to [:post "/logout"]
                    (submit-button "Logout"))
           [:a {:href "/completion-chart"} "Completion Chart"]
           [:br]
           [:a {:href "/burndown"} "Burndown Chart"]]
          [:br] [:br]
          (form-to [:post "/add-todo"]
                   (text-area :todo/content)
                   (submit-button "Add"))
          [:p "todos:"]
          [:ul
           (keep (fn [td]
                   (when (not= (get-in td [:todo/state :db/ident]) :todo.state/removed)
                     [:li [:div (form-to [:post "/update-todo"]
                                         (label "todo/content"
                                                (:todo/content td))
                                         (drop-down "todo/state" ["complete" "incomplete"] (-> util/todo-state-string->todo-state-ident
                                                                                               clojure.set/map-invert
                                                                                               (get (get-in td [:todo/state :db/ident]))))
                                         (hidden-field "db/id" (:db/id td))
                                         (submit-button "Update"))
                           (form-to [:post "/update-todo"]
                                    (hidden-field "db/id" (:db/id td))
                                    (hidden-field "todo/state" "removed")
                                    (submit-button "Remove"))]])) (:user/todos user-data))]))
