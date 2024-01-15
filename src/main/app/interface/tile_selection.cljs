(ns app.interface.tile-selection
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.undo :as undo :refer [undoable]] 
   [app.interface.board :refer [update-tiles]]))


; Tile selection works by first calling :tile-selection/begin, which takes:
; 1. a validator function that takes a tile and returns true if that tile can
;    be selected, false otherwise.
; 2. a callback function that takes the app db and the selected tile and
;    returns a new db.  This function is called when a tile is selected
;    (signalled via :tile-selection/end).
; 3. a selection-data map containing information specific to the current type
;    of selection 


(rf/reg-event-db
  :tile-selection/start
  (undoable "Starting tile selection") 
  (fn [db [_ validator callback selection-data]]
    (-> db
        (assoc :tile-selection/currently-selecting true)
        (assoc :tile-selection/callback callback)
        (assoc :tile-selection/selection-data selection-data)
        (assoc :board (update-tiles (:board db)
                                    (fn [tile]
                                      (assoc tile :is-valid-selection
                                                  (validator tile))))))))
 
(rf/reg-event-db
  :tile-selection/end
  (undoable "Choosing a tile") 
  (fn [db [_ tile]]
    (-> db
        (assoc :tile-selection/currently-selecting false)
        (dissoc :tile-selection/callback)
        (dissoc :tile-selection/selection-data)
        ((:tile-selection/callback db) tile))))
  
(doseq [kw [:tile-selection/callback
            :tile-selection/selection-data
            :tile-selection/currently-selecting]]
  (rf/reg-sub kw
              (fn [db _] (kw db))))
