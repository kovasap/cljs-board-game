(ns app.interface.developments
  (:require [re-frame.core :as rf]
            [app.interface.players :refer [get-current-player]]
            [app.interface.board :refer [update-tiles get-adjacent-tiles]]
            [app.interface.map-generation :refer [lands]]
            [app.interface.utils :refer [get-only]]))

(def all-land-types
  (into #{} (for [land lands]
              (:type land))))

(defn update-resources
  [db player-idx resource-delta]
  (let [updated (update-in db
                           [:players player-idx :resources]
                           #(merge-with + resource-delta %))]
    (if (seq (filter (fn [[_ amount]] (> 0 amount))
               (get-in updated [:players player-idx :resources])))
      [false (assoc db :message "Cannot pay the cost!")]
      [true updated])))

(defn is-legal-land-placement
  [tile legal-land-placements]
  (contains? legal-land-placements (:type (:land tile))))

(defn adjacent-to-owned-developments?
  [board tile player]
  (some #(= (:player-name (:controller %)) (:player-name player))
        (get-adjacent-tiles board tile)))
  
  
(def developments
  [{:type        :settlement
    :description "Produces resources"
    :is-legal-placement? (fn [db tile]
                           (and (nil? (:development tile))
                                (adjacent-to-owned-developments?
                                  (:board db) tile (get-current-player db))))
    :use         (fn [db instance]
                   (second (update-resources db
                                             (:current-player-idx db)
                                             (:production instance))))
    :place       (fn [db instance])
    :max         6
    :cost        {:wood -1}
    :tax         {:food -1}
    :production  {}}
   {:type        :capitol
    :description "Can build off of"
    :is-legal-placement? (fn [db tile]
                           (nil? (:development tile)))
    :use         (fn [db instance]
                   (second (update-resources db
                                             (:current-player-idx db)
                                             (:production instance))))
    :place       (fn [db instance])
    :max         3
    :cost        {}
    :tax         {}
    :production  {}}
   {:type        :library
    :description "Draw 3 cards, discard 2"
    :is-legal-placement? (fn [db tile]
                           (and (is-legal-land-placement tile #{"mountain"})
                                (nil? (:development tile))))
    :use         (fn [db instance] (assoc db :message "No cards in game yet"))
    :max         2
    :cost        {:wood -1}
    :tax         {:sand -1}
    :production  {}}
   {:type        :crossroads
    :description "Use all adjacent tiles that have not been used yet."
    :is-legal-placement? (fn [db tile]
                           (and (is-legal-land-placement tile #{"plains"})
                                (nil? (:development tile))))
    :use         (fn [db instance]
                   (assoc db :message "Terraformer not implemented yet"))
    :max         3
    :cost        {:wood -1}
    :tax         {:stone -1}
    :production  {}}
   {:type        :terraformer
    :description "Change the land type of a tile"
    :is-legal-placement? (fn [db tile]
                           (and (is-legal-land-placement tile #{"sand"})
                                (nil? (:development tile))))
    :use         (fn [db instance]
                   (assoc db :message "Terraformer not implemented yet"))
    :max         3
    :cost        {:wood -1}
    :tax         {:stone -1}
    :production  {}}])

 
(rf/reg-event-db
  :development/use
  (fn [db [_ development {:keys [row-idx col-idx worker-owner]}]]
    (let [current-player-name (:player-name @(rf/subscribe [:current-player]))
          tax (if (= current-player-name (:owner development))
                {}
                (:tax development))
          [cost-payable updated-db] (update-resources
                                      db (:current-player-idx db) tax)]
      (cond
        worker-owner (assoc db
                       :message (str "Worker from "
                                     worker-owner
                                     " already here!"))
        cost-payable (-> updated-db
                         (update-in [:players
                                     (:current-player-idx db)
                                     :workers]
                                    dec)
                         (assoc-in [:board row-idx col-idx :worker-owner]
                                   current-player-name)
                         ((:use development) development))
        :else        updated-db))))

(defn stop-placing
  ([db]
   (-> db
       (assoc :board (update-tiles (:board db)
                                   (fn [tile]
                                     (assoc tile :legal-placement? false))))
       (assoc :placing false)))
  ([db _] (stop-placing db)))

(rf/reg-event-db
  :development/start-placing
  (fn [db [_ development-type placer]]
    (let [development (get-only developments :type development-type)]
      (-> db
          (stop-placing)
          (assoc :board (update-tiles (:board db)
                                      (fn [tile]
                                        (assoc tile
                                          :legal-placement?
                                            ((:is-legal-placement? development)
                                             db
                                             tile)))))
          (assoc :placing development
                 :placer  placer)))))

(rf/reg-event-db :development/stop-placing stop-placing)

(rf/reg-event-db
  :development/place
  (fn [db [_ {:keys [row-idx col-idx legal-placement?]}]]
    (let [existing-num (count @(rf/subscribe [:developments
                                              (:type (:placing db))]))
          current-player-name (:player-name @(rf/subscribe [:current-player]))
          [cost-payable updated-db] (update-resources db
                                                      (:current-player-idx db)
                                                      (:cost (:placing db)))]
      (cond (not legal-placement?)
            (assoc db :message "Invalid location!")
            (>= existing-num (:max (:placing db)))
            (assoc db :message "Max number already placed!")
            (= 0 (get-in db [:players (:current-player-idx db) :workers]))
            (assoc db :message "No more workers!")
            (not cost-payable) updated-db
            :else
            (->
              updated-db
              (update-in
                [:board row-idx col-idx]
                (fn [tile]
                  (assoc tile
                    :development  (assoc (:placing db)
                                    :owner      (:placer db)
                                    :production (get-in tile
                                                        [:land :production]))
                    :controller (get-in db [:players (:current-player-idx db)])
                    :worker-owner current-player-name)))
              (update-in [:players (:current-player-idx db) :workers] dec)
              (stop-placing))))))
        

(rf/reg-sub
  :placing
  (fn [db _]
    (:placing db)))


(rf/reg-sub
  :developments
  (fn [db [_ development-type]]
    (filter #(= (:type %) development-type)
      (reduce concat
        (for [column (:board db)]
          (for [tile column] (:development tile)))))))