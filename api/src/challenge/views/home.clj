(ns challenge.views.home
  (:require [hiccup.core :as h]
            [hiccup.form :refer :all]))

(defn home
  [req]
  (h/html [:div
           [:h1 "To-Do"]
           (form-to [:post "/login"]
                    (text-field "email")
                    (submit-button "Login"))]))
