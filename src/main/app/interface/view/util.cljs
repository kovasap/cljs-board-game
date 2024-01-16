(ns app.interface.view.util
  (:require [clojure.string :as st]))

(defn tally-marks
  [number character]
  [:pre {:style {:display "inline"}}
   (st/join (take number (repeat character)))])
