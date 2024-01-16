(ns app.interface.affinity
  (:require
   [re-frame.core :as rf]
   [app.interface.board :refer [sum-tiles]]))

(defn get-affinity
  [{:keys [board]} player-idx]
  (reduce +
    (->> (flatten board)
         (filter #(= (:controller-idx %) player-idx))
         (map #(get (:production %) :points 0)))))

(rf/reg-sub
  :affinity-for-player
  (fn [db [_ player-idx]]))
    
    
