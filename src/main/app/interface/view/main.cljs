(ns app.interface.view.main
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [clojure.string :as st]
            [app.interface.view.map :refer [board-view]]
            [app.interface.view.players :refer [player-card-view]]
            [app.interface.developments :refer [developments]]
            [cljs.pprint]))

(defn- main
  "Main view for the application."
  []
  (let [experiments @(rf/subscribe [:experiments])
        players     @(rf/subscribe [:players])
        placing     @(rf/subscribe [:placing])
        db  @(rf/subscribe [:db-no-board])
        current-player-name (:player-name @(rf/subscribe [:current-player]))]
    [:div.container
     [:h1 "Welcome to Terraforming Catan!"]
     [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:game/setup])}
      "Setup Game"]
     [:br]
     [:br]
     [:div {:style {:display  "grid"
                    :grid-template-columns "auto auto auto"
                    :grid-gap "15px"}}
      (into [:div] (for [player players] (player-card-view player)))
      [:div
       (board-view)
       [:br]
       [:div @(rf/subscribe [:message])]
       [:br]
       (doall
         (for [development developments
               :let        [n        (name (:type development))
                            existing @(rf/subscribe [:developments
                                                     (:type development)])
                            placing-current (= (:type placing)
                                               (:type development))]]
           [:button.btn.btn-outline-primary
            {:key      n ; Required by react (otherwise we get a warning).
             :style    {:font-weight (if placing-current
                                       "bold"
                                       "normal")}
             :on-click #(if placing-current
                          (rf/dispatch [:development/stop-placing])
                          (rf/dispatch [:development/start-placing
                                        (:type development)
                                        current-player-name]))}
            [:div "Place " n " " (count existing) "/" (:max development)]
            [:div "(cost " (:cost development) ")"]]))
       [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:end-turn])}
        "End Turn"]
       [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:end-round])}
        "End Round"]]
      [:div "TODO add diff of game state to show what just happened\n"
       [:pre (with-out-str (cljs.pprint/pprint db))]]]]))


