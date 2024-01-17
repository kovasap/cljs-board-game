(ns app.interface.view.players
  (:require [re-frame.core :as rf]
            [app.interface.view.personnel :refer [personnel-view]]))


(defn player-card-view
  [{:keys [player-name idx color]}]
  (let [current-player-name (:player-name @(rf/subscribe [:current-player]))
        personnel @(rf/subscribe [:personnel idx])
        points @(rf/subscribe [:score-for-player idx])]
    [:div
     [:div {:style {:color color}}
      player-name
      (if (= player-name current-player-name) "*" "")]
     [:div "Personnel: " [personnel-view personnel]]
     (for [[k v] points]
       [:div {:key (str player-name (name k))} v " pts for " (name k)])]))
