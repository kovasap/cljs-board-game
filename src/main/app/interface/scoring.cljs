(ns app.interface.scoring
  (:require
   [re-frame.core :as rf]
   [app.interface.board :refer [get-adjacent-tiles]]))

(defn get-opponent-adjacency-points
  [{:keys [board]} player-idx]
  (reduce +
    (->> (flatten board)
         (filter #(= (:controller-idx %) player-idx))
         (map (fn [tile]
                (reduce +
                  (->> (get-adjacent-tiles board tile)
                       (filter #(and (not (nil? (:controller-idx %)))
                                     (not (= (:controller-idx %) player-idx))))
                       (map (constantly 2)))))))))
             
(defn get-produced-points
  [{:keys [board]} player-idx]
  (reduce +
    (->> (flatten board)
         (filter #(= (:controller-idx %) player-idx))
         (map #(get (:production %) :points 0)))))

(defn get-largest-area-points
  [{:keys [board]} player-idx]
  0)


(rf/reg-sub
  :score-for-player
  (fn [db [_ player-idx]]
    {:adjacency (get-opponent-adjacency-points db player-idx)
     :production (get-produced-points db player-idx)
     :largest-area (get-largest-area-points db player-idx)}))
    
