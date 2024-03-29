(ns app.interface.board
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [app.interface.config :refer [debug]]))

(defn update-tiles
  [board update-fn]
  (into []
        (for [column board] (into [] (for [tile column] (update-fn tile))))))

(defn one-away?
  [n1 n2]
  (or (= n1 (dec n2))
      (= n1 (inc n2))))

(defn adjacent?
  "Checks if two tiles are adjacent or not (returns a bool)."
  [{row-idx1 :row-idx col-idx1 :col-idx} {row-idx2 :row-idx col-idx2 :col-idx}]
  (or
    ; Same row
    (and (= row-idx1 row-idx2) (one-away? col-idx1 col-idx2))
    (and (one-away? row-idx1 row-idx2)
         (or (and (even? row-idx1) (or (= col-idx1 col-idx2)
                                       (= col-idx1 (inc col-idx2))))
             (and (odd? row-idx1) (or (= col-idx1 col-idx2)
                                      (= col-idx1 (dec col-idx2))))))))
  

(defn get-adjacent-tiles
  [board tile]
  (reduce concat (for [column board] (filter #(adjacent? % tile) column))))

(rf/reg-sub
  :adjacent-tiles
  (fn [db [_ tile]]
    (get-adjacent-tiles (:board db) tile)))

(defn adjacent-to-controlled-developments?
  [board tile player-idx]
  (not (nil? (some #(= (:controller-idx %) player-idx)
                   (get-adjacent-tiles board tile)))))

(defn update-adjacent-tiles
  [board tile update-fn]
  (update-tiles board (fn [t] (if (adjacent? t tile) (update-fn t) t))))


(defn control-any-tiles?
  "Returns true if the given player-idx controls any tiles in the given board."
  [board player-idx]
  (> (count
      (reduce concat
        (for [column board]
          (for [{:keys [controller-idx]} column
                :when (= controller-idx player-idx)]
            controller-idx))))
     0))

(defn get-num-developments
  [board development-type]
  (count
    (filter #(= % development-type)
      (reduce concat
        (for [column board]
          (for [tile column] (:development-type tile)))))))

(rf/reg-sub
  :num-developments
  (fn [db [_ development-type]]
    (get-num-developments (:board db) development-type)))
