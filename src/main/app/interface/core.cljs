(ns app.interface.core
  (:require ["react-dom/client" :refer [createRoot]]
            [ajax.core :as ajax]
            [app.config :as config]
            [day8.re-frame.http-fx]
            [day8.re-frame.undo :as undo :refer [undoable]]  
            [goog.dom :as gdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [app.interface.sente :refer [send-game-state-to-server!]]
            [app.interface.players :refer [next-player-idx player-data]]
            [clojure.string :as st]
            [app.interface.view.main :refer [main]]
            [app.interface.board :refer [update-tiles]]
            [app.interface.map-generation :refer [setup-board]]
            [app.interface.developments :refer [developments]]
            [app.interface.development-placement]
            [app.interface.config :refer [debug]]
            [app.interface.resources :refer [resources]]
            [app.interface.scoring]
            [app.interface.tile-selection]
            [app.interface.orders :refer [orders]]
            [app.interface.utils :refer [get-only]]
            [cljs.pprint]
            [taoensso.timbre :as log]))

(rf/reg-sub
  :db
  (fn [db _] db))


;; ----------------------------------------------------------------------------
;; Setup

(def num-available-developments (count developments))

(defn select-developments
  []
  (conj (take num-available-developments
              (filter #(not (= (:type %) :gathering)) (shuffle developments)))
        ; Make sure gatherings are included.
        (get-only developments :type :gathering)))

(rf/reg-event-db
  :game/setup
  (fn [db _]
    (-> db
     (setup-board)
     (assoc
         :message ""
         :orders (take 3 (shuffle orders))
         :players (into [] (map-indexed player-data ["cupid" "zeus" "hades"]))
         :current-player-idx 0
         :blueprints (select-developments)
         :placing false))))

(doseq [kw [:players :blueprints :orders :board :message]]
  (rf/reg-sub
    kw
    (fn [db _] (kw db))))

(rf/reg-event-db
  :message
  (fn [db [_ message]]
    (assoc db :message message)))

;; ----------------------------------------------------------------------------
;; End of Game

(defn is-game-over?
  [db]
  false)

(defn end-game
  [db]
  db)

;; ----------------------------------------------------------------------------
;; End of Turn

(rf/reg-event-db
  :end-turn
  (undoable "Turn End")
  (fn [db [_]]
    (let [new-db (-> db
                   (assoc :current-player-idx (next-player-idx db)))]
      (send-game-state-to-server! new-db)
      (if (is-game-over? new-db)
        (end-game new-db)
        new-db))))


;; -- Entry Point -------------------------------------------------------------

(defonce root (createRoot (gdom/getElement "app")))

(defn init
  []
  (rf/dispatch [:game/setup])
  (.render root (r/as-element [main])))

(defn- ^:dev/after-load re-render
  "The `:dev/after-load` metadata causes this function to be called after
  shadow-cljs hot-reloads code. This function is called implicitly by its
  annotation."
  []
  (rf/clear-subscription-cache!)
  (init))
