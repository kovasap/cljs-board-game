(ns app.interface.view.players
  (:require [re-frame.core :as rf]
            [clojure.string :as st]))

(defn tally-marks
  [number character]
  [:pre {:style {:display "inline"}}
   (st/join (take number (repeat character)))])

(defn player-card-view
  [{:keys [player-name idx color] {:keys [explorers channelers]} :personnel}]
  (let [current-player-name (:player-name @(rf/subscribe [:current-player]))
        points @(rf/subscribe [:score-for-player idx])]
    [:div
     [:div {:style {:color color}}
      player-name
      (if (= player-name current-player-name) "*" "")]
     [:div "Explorers: " (tally-marks explorers "i")]
     [:div "Channelers: " (tally-marks channelers "j")]
     (for [[k v] points]
       [:div {:key (str player-name (name k))} v " pts for " (name k)])]))
