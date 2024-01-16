(ns app.interface.players
  (:require [re-frame.core :as rf]
            [app.interface.utils :refer [get-only]]
            [app.interface.developments :refer [developments]]))

(def starting-personnel
  {:explorers 5
   :channelers 2})

(defn player-data
  [i player-name]
  {:player-name player-name
   :idx         i
   :color       (get ["blue" "red" "purple" "black"] i)
   :points      0})

#_(defn update-resources
    [db player-idx resource-delta]
    (update-in db
               [:players player-idx :owned-resources]
               #(merge-with + resource-delta %)))

#_(defn update-resources-with-check
    [db player-idx resource-delta]
    (let [updated (update-resources db player-idx resource-delta)]
      (if (seq (filter (fn [[_ amount]] (> 0 amount))
                 (get-in updated [:players player-idx :owned-resources])))
        [false (assoc db :message "Cannot pay the cost!")]
        [true updated])))

(defn get-available-personnel
  [board player-idx]
  (reduce #(merge-with + %1 %2)
    starting-personnel
    (->> (flatten board)
         (filter #(= (:controller-idx %) player-idx))
         (map #(:personnel
                 (get-only developments :type (:development-type %)))))))

(rf/reg-sub
  :personnel
  (fn [{:keys [board]} [_ player-idx]]
    (get-available-personnel board player-idx)))
    

(defn get-current-player
  [{:keys [players current-player-idx] :as db}]
  (if (nil? players)
    nil
    (nth players current-player-idx)))

(rf/reg-sub
  :current-player
  (fn [db _]
    (get-current-player db)))

(defn next-player-idx
  [{:keys [players current-player-idx] :as db}]
  (if (= (+ 1 current-player-idx) (count players))
    0
    (+ 1 current-player-idx)))
