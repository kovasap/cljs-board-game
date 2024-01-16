(ns app.interface.view.players
  (:require [re-frame.core :as rf]
            [app.interface.view.util :refer [tally-marks]]))


(defn player-card-view
  [{:keys [player-name idx color]}]
  (let [current-player-name (:player-name @(rf/subscribe [:current-player]))
        {:keys [explorers channelers]} @(rf/subscribe [:personnel idx])
        points @(rf/subscribe [:score-for-player idx])]
    [:div
     [:div {:style {:color color}}
      player-name
      (if (= player-name current-player-name) "*" "")]
     [:div "Explorers: " (tally-marks explorers "i")]
     [:div "Channelers: " (tally-marks channelers "j")]
     (for [[k v] points]
       [:div {:key (str player-name (name k))} v " pts for " (name k)])]))
