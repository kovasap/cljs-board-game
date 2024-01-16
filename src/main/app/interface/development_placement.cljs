(ns app.interface.development-placement
  (:require [app.interface.lands :refer [lands]]
            [app.interface.board
             :refer
             [update-tiles
              adjacent-to-controlled-developments?
              control-any-tiles?
              get-num-developments]]
            [app.interface.resource-flow
             :refer
             [unmet-resources apply-production-chain]]
            [app.interface.utils :refer [get-only]]))


(defn update-personnel
  "Creates a new personnel map with personnel gained/lost by creating the given
  development."
  [development current-player-personnel]
  (merge-with + current-player-personnel (:personnel development)))

(defn insufficient-personnel?
  "Returns false if the player has enough personnel to create the development,
  true otherwise."
  [development current-player]
  (some neg?
        (vals (update-personnel development (:personnel current-player)))))


(defn make-development-placement-validator
  "Creates a tile validator function to feed to :tile-selection/begin."
  [development board current-player]
  (let [valid-lands         (get development
                                 :valid-lands
                                 (set (map :type lands)))]
    (fn [tile]
      (let [chains-to-unmet-resources
            (into {}
                  (for [chain (:production-chains development)]
                    [chain (unmet-resources chain board tile)]))]
        (cond
          (insufficient-personnel? development current-player)
          "Not enough explorers or channelers!"
          (not (nil? (:development-type tile)))
          "Tile is already occupied!"
          (and (seq chains-to-unmet-resources)
               (every? seq (vals chains-to-unmet-resources)))
          (str "All possible production chains invalid! "
               chains-to-unmet-resources)
          (not (contains? valid-lands (:type (:land tile))))
          (str "Invalid land type, must be one of " valid-lands)
          (and (control-any-tiles? board (:idx current-player))
               (not
                 (adjacent-to-controlled-developments? board
                                                       tile
                                                       (:idx current-player))))
          "Must adjacent to developments you own, unless you own no tiles!"
          (>= (get-num-developments board (:type development))
              (:max development))
          "Max number already placed!"
          :else
          nil)))))


(defn make-development-placement-callback
  "Creates a callback function to feed to :tile-selection/begin."
  [development current-player-idx]
  (fn [db {:keys [row-idx col-idx land] :as tile}]
    (-> db
        ; Add the development to the board.
        (update-in
          [:board row-idx col-idx]
          #(assoc %
             :development-type (:type development)
             :production       ((:type land) (:land-production development))
             :controller-idx   current-player-idx))
        ; Update all production chains.
        (update :board
                (apply comp
                  (for [chain (:production-chains development)]
                    #(apply-production-chain chain % tile))))
        ; Update player personnel counts
        (update-in [:players current-player-idx :personnel]
                   #(update-personnel development %))
        ; Do any special, development specific on placement actions.
        ((:on-placement development)))))

(rf/reg-event-db
  :development/destroy
  (fn [db [_ tile]]
    (-> db
        TODO implement)))
        
  
