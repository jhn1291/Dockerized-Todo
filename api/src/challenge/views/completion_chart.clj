(ns challenge.views.completion-chart
  (:require [hiccup.core :as h]
            [hiccup.form :refer :all]))

(defn completion-chart
  [grouped-todos]
  (let [count-complete (count (get grouped-todos :todo.state/complete))
        count-incomplete (count (get grouped-todos :todo.state/incomplete))]
    (h/html [:div
             [:a {:href "/todo"} "Return"]
             [:h1 (str "Total Complete: " count-complete)]
             [:ul
              (map (fn [td] [:li (:todo/content td)])
                   (sort-by #(get-in % [:todo/meta :todometa/retired-on]) (get grouped-todos :todo.state/complete)))]]
            [:div [:h1 (str "Total Incomplete: " count-incomplete)]
             [:ul
              (map (fn [td] [:li (:todo/content td)])
                   (sort-by #(get-in % [:todo/meta :todometa/created-on]) (get grouped-todos :todo.state/incomplete)))]]
            )))
