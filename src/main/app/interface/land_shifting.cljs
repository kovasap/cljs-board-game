(ns app.interface.land-shifting
  (:require
    [app.interface.board :refer [update-tiles]]
    [app.interface.utils :refer [only]]
    [app.interface.lands :refer [lands]]))

(defn get-next-land-type
  [land-type]
  (let [land-type-idx
        (only (keep-indexed (fn [idx land] (when (= land-type (:type land))
                                             idx))
                            lands))
        next-idx (if (= land-type-idx (dec (count lands)))
                   0
                   (inc land-type-idx))]
    (:type (get lands next-idx))))

(defn shift-lands
  [board waxing-land-type]
  (update-tiles board
                (fn [tile] (if (= waxing-land-type (:land tile))
                             (-> tile
                                 (dissoc :development-type)
                                 (dissoc :controller))
                             tile))))
