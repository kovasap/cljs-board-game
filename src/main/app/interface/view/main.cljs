(ns app.interface.view.main
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [clojure.string :as st]
            ; [ring.middleware.anti-forgery]
            [app.interface.sente :refer [chsk-state login]]
            [app.interface.view.map :refer [board-view]]
            [app.interface.view.players :refer [player-card-view]]
            [app.interface.developments :refer [developments]]
            [app.interface.view.developments
             :refer
             [blueprints-view build-buttons-view]]
            [cljs.pprint]))

(defn undo-button
  []
  ; only enable the button when there's undos
  (let [undos? (rf/subscribe [:undos?])]
    (fn []
      [:button.btn.btn-outline-primary
       {:disabled (not @undos?)
        :on-click #(rf/dispatch [:undo])
        :style {:margin-right "auto"}}
       "Undo"])))

(defn redo-button
  []
  ; only enable the button when there's redos
  (let [redos? (rf/subscribe [:redos?])]
    (fn []
      [:button.btn.btn-outline-primary
       {:disabled (not @redos?)
        :on-click #(rf/dispatch [:redo])
        :style {:margin-right "auto"}}
       "Redo"])))

; Not currently necessary/used
(defn login-field
  []
  [:span
   [:input#input-login {:type :text :placeholder "User-id"}]
   [:button.btn.btn-outline-primary
    {:on-click (fn []
                 (let [user-id (.-value (.getElementById js/document
                                                         "input-login"))]
                   (login user-id)))}
    "Secure login!"]])

(rf/reg-event-db
  :popup-window-at-coords
  (fn [db [_ window {:keys [x y]}]]
    (if (nil? window)
      (dissoc db :popup-window-at-coords)
      (assoc db :popup-window-at-coords
                [:div.popup-window-at-coords
                  {:style {:left x :top y :z-index 100 :position "absolute"}}
                  window]))))

(rf/reg-sub
  :popup-window-at-coords
  (fn [db _]
    (:popup-window-at-coords db)))

(defn main
  "Main view for the application."
  []
  (let [players @(rf/subscribe [:players])
        db      @(rf/subscribe [:db])
        popup-window-at-coords @(rf/subscribe [:popup-window-at-coords])]
    [:div
     popup-window-at-coords
     [:div @chsk-state]
     [:div.container
      #_(let [csrf-token (force
                           ring.middleware.anti-forgery/*anti-forgery-token*)]
          [:div#sente-csrf-token {:data-csrf-token csrf-token}])
      [:h1 "Welcome to Shifting Worlds!"]
      ; [login-field]
      [:div {:style {:display "flex"}}
       [:button.btn.btn-outline-primary {:on-click #(rf/dispatch
                                                      [:game/setup])}
        "Reset Game"]
       [undo-button]
       [redo-button]
       (into [:div {:style {:display         "flex"
                            :width           "100%"
                            :justify-content "space-evenly"}}]
             (for [player players]
               (player-card-view player)))
       [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:end-turn])
                                         :style    {:margin-left "auto"}}
        [:b "End Turn "]
        "and Send to Server"]]
      [:br]
      [:div [:p "Orders: " (:orders db)] [:p "Points given to first achiever"]]
      [build-buttons-view]
      [:div {:style {:display  "grid"
                     :grid-template-columns "auto auto"
                     :grid-gap "15px"}}
       [:div
        [board-view]
        [:br]
        [:div @(rf/subscribe [:message])]
        [:br]]
       [blueprints-view]]
      [:div
       "TODO add diff of game state to show what just happened\n"
       [:pre (with-out-str (cljs.pprint/pprint db))]]]]))
