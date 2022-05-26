(ns challenge.views.burndown-chart
  (:require [hiccup.core :as h]
            [challenge.util :as util]
            [hiccup.form :refer :all]))

(defn todo-relevant-date
  [todo]
  (condp = (:db/ident (:todo/state todo))
    :todo.state/incomplete (:todometa/created-on (:todo/meta todo))
    :todo.state/complete (:todometa/completed-on (:todo/meta todo))
    :todo.state/removed (:todometa/removed-on (:todo/meta todo))))

(defn todo->burndown-collection
  "Given a todo, return a collection of todos with a burndown-date.
   burndown-type is determined by todo-state.

   All todo's will have a creation date in the burndown, but we
   also check for completion/removal burndown information for the todo
   and populate that when applicable."
  [todo])

(defn todos->burndown-seq
  [todos]
  (->> (reduce (fn [ds t]
                 (let [created-date (get-in t [:todo/meta :todometa/created-on])
                       created-todo (assoc t :burndown-date created-date :todo/state {:db/ident :todo.state/incomplete})
                       not-incomplete? (not= (-> t :todo/state :db/ident) :todo.state/incomplete)
                       relevant-date (todo-relevant-date t)
                       current-todo (assoc t :burndown-date relevant-date)
                       todos-to-conj (if not-incomplete?
                                       [created-todo current-todo]
                                       [created-todo])]
                   (apply conj ds todos-to-conj)))
               [] todos)
       (sort-by :burndown-date)))

(def state-ident->string
  {:todo.state/incomplete "Added"
   :todo.state/complete "Completed"
   :todo.state/removed "Removed"})

(defn burndown-score
  [bd-seq]
  (reduce (fn [sum bd]
            (if (= :todo.state/incomplete (-> bd :todo/state :db/ident))
              (inc sum)
              (dec sum)))
            0 bd-seq))

(defn burndown-chart
  [todos]
  (let [burndown-seq (todos->burndown-seq todos)]
    (h/html [:div
             [:a {:href "/todo"} "Return"]
             [:h1 (str "Burndown: " (burndown-score burndown-seq))]
             [:ul (map (fn [td]
                         [:li (str (-> td :todo/state :db/ident state-ident->string)
                                   " " (:burndown-date td) " " (:todo/content td))])
                         burndown-seq)]]
             )))
