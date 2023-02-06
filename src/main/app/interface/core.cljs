(ns app.interface.core
  (:require ["react-dom/client" :refer [createRoot]]
            [ajax.core :as ajax]
            [app.config :as config]
            [day8.re-frame.http-fx]
            [goog.dom :as gdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [app.interface.players :refer [next-player-idx reset-workers]]
            [clojure.string :as st]
            [app.interface.view.main :refer [main]]
            [app.interface.board :refer [update-tiles]]
            [app.interface.resource-flow
             :refer
             [accumulate-land-resources accumulate-production-resources
              update-board-tiles]]
            [app.interface.map-generation :refer [setup-board]]
            [app.interface.developments :refer [make-opening-hand]]
            [app.interface.config :refer [debug]]
            [app.interface.resources :refer [resources]]
            [app.interface.orders :refer [orders]]
            [cljs.pprint]
            [cljs.core.async :as async :refer (<! >! put! chan)]
            [taoensso.sente  :as sente :refer (cb-success?)]
            [taoensso.timbre :as log]))

(rf/reg-sub
  :db-no-board
  (fn [db _]
    (dissoc db :board)))


(def ?csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))


(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
       "/chsk" ; Note the same path as before
       ?csrf-token
       {:type :auto})] ; e/o #{:auto :ajax :ws}
       

  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state))   ; Watchable, read-only atom


;; ----------------------------------------------------------------------------
;; Setup

(defn player-data
  [i player-name]
  {:player-name     player-name
   :index           i
   :color           (get ["blue" "red" "purple" "black"] i)
   :workers         2
   :max-workers     2
   :blueprints      (make-opening-hand)
   :owned-resources (if debug
                      {:wood 2 :stone 5}
                      (assoc (into {}
                                   (for [{:keys [type]} resources] [type 0]))
                        :stone 5))})

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
         :placing false))))

(rf/reg-event-db
  :message
  (fn [db [_ message]]
    (assoc db :message message)))

(rf/reg-sub
  :message
  (fn [db _]
    (:message db)))

;; ----------------------------------------------------------------------------
;; End of Turn

(rf/reg-event-db
  :end-turn
  (fn [db [_]]
    (-> db
      (assoc :current-player-idx (next-player-idx db)))))

(rf/reg-event-db
  :end-round
  (fn [db [_]]
    (-> db
        ; TODO check if orders have been fulfilled and end the game if so.
        ; Give everyone their workers back.
        (update :players #(mapv reset-workers %))
        (update :board update-tiles #(assoc % :worker-owner nil))
        (update :board update-tiles accumulate-land-resources)
        (update :board update-board-tiles accumulate-production-resources))))


;; -----------------------------------------------------------------------------
;; Events and Subscriptions to query the backend and store the result in the
;; app-state.

(rf/reg-event-fx
 :experiments/get
 (fn [_ _]
   {:fx [[:http-xhrio {:method :get
                       :uri (str config/api-location "/experiments")
                       :format (ajax/transit-request-format)
                       :response-format (ajax/transit-response-format)
                       :on-success [:experiments.get/success]
                       :on-failure [:experiments.get/error]}]]}))

(rf/reg-event-db
 :experiments.get/success
 (fn [db [_ response]]
   (assoc db :experiments (:experiments response))))

(rf/reg-event-fx
  :experiments.get/error
  (fn [_ [_ error]]
    {:fx
       [[:log/error
         (str
           "Could not query the experiments. Did you forget to start the api? "
           error)]]}))

(rf/reg-sub
 :experiments
 (fn [db _]
   (:experiments db)))



(rf/reg-fx
 :log/error
 (fn [message]
   (log/error message)))



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
