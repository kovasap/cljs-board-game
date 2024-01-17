(ns app.interface.developments
  (:require [app.interface.lands :refer [lands]]
            [re-frame.core :as rf]
            [day8.re-frame.undo :as undo :refer [undoable]] 
            [clojure.set :refer [difference]]))

(defn assoc-if-nil
  "Add k=v to the m if k is not already present in m."
  [m k v]
  (if (nil? (k m))
    (assoc m k v)
    m))
           

; TODO add malli spec for development

(def generally-valid-lands
  (difference (set (map :type lands)) #{:water :void}))

(def basic-resources
  [:wood :grain :stone :water :fossils])

(def refined-resources
  [:planks :bread :flour :charcoal])

(def special-resources
  [:points])

(def resources
  (concat basic-resources refined-resources special-resources))

; TODO add:
;  - railroad or crossroads that can connect production chain tiles
; These are references to development that are not to be copied, just
; referenced via their :type or :letter.  This is because they may contain
; functions, which cannot be serialized as game state.
(def developments
  (for
    [dev
     [{:type        :gathering
       :personnel   {:explorers -1}
       :category    :basic
       :letter      "S"
       :description "Accumulates resources for future collection/processing."
       :land-production {:forest   {:wood 2}
                         :plains   {:grain 2}
                         :mountain {:stone 2}
                         :desert   {:fossils 1}
                         :water    {:water 2}}
       :max         12}
      ; ----------------- Resource Generators -------------------
      {:type        :mill
       :personnel   {:explorers -1}
       :category    :refinement
       :letter      "M"
       :description "Produces planks from wood AND/OR flour from grain."
       :production-chains [{:wood -1 :planks 1} {:grain -1 :flour 1}]
       :max         6}
      {:type        :oven
       :personnel   {:explorers -1}
       :category    :refinement
       :letter      "O"
       :description "Produces charcoal from wood AND/OR bread from flour"
       :production-chains [{:wood -1 :charcoal 1} {:flour -1 :bread 4}]
       :valid-lands #{:plains}
       :max         6}
      ; ----------------- Point Generators --------------------
      {:type        :monument
       :personnel   {:explorers -1}
       :category    :point-generation
       :letter      "U"
       :description "Worth 5 pts"
       :production-chains [{:stone -6 :points 5}]
       :max         2}
      {:type        :nature-preserve
       :personnel   {:explorers -1}
       :category    :point-generation
       :letter      "N"
       :description "Worth 5 pts"
       :production-chains [{:water -2 :points 5}]
       :max         2}
      {:type        :carpenter
       :personnel   {:explorers -1}
       :category    :point-generation
       :letter      "C"
       :description "Transforms planks into points"
       :production-chains [{:planks -1 :points 3}]
       :max         2}
      {:type        :crossroads
       :personnel   {:explorers -1}
       :category    :point-generation
       :letter      "X"
       :description "Worth 1 points for each adjacent development."
       :not-implemented true
       :max         2}
      {:type        :oasis
       :personnel   {:explorers -1}
       :category    :point-generation
       :letter      "A"
       :description "Makes water and points"
       :production-chains [{:water 1 :points 1}]
       :max         2}
      {:type :throne
       :personnel   {:channelers -1}
       :category    :point-generation
       :letter "E"
       :description
       "Worth 10 pts if you have the most tiles of at least 3 land types"
       :production-chains [{:stone -2}]
       :max 2}
      ; TODO make this an infinite sink?
      {:type        :port
       :personnel   {:explorers -1}
       :category    :point-generation
       :letter      "P"
       :description "Resources to points"
       :production-chains (into []
                                (for [resource refined-resources]
                                  {resource -2 :points 2}))
       :valid-lands #{:water}
       :max         2}
      ; ----------------- Personnel ---------------------------------------
      {:type        :house
       :personnel   {:explorers 1}
       :category    :personel
       :letter      "H"
       :description "Bread to explorers"
       :production-chains [{:bread -1}]
       :max         6}
      {:type        :temple
       :personnel   {:channelers 1}
       :category    :personel
       :letter      "T"
       :description "Water to channelers"
       :production-chains [{:water -1}]
       :max         6}
      ; ----------------- Point Eaters ---------------------------------------
      {:type        :bandit-hideout
       :personnel   {:explorers -1}
       :category    :point-consumption
       :letter      "H"
       :description "Turns points into bread"
       :production-chains [{:points -2 :bread 1}]
       :max         6}
      ; ----------------- Misc ---------------------------------------
      {:type        :road
       :personnel   {:explorers -1}
       :category    :misc
       :letter      "R"
       :description "Does nothing, but extends your buildable area"
       :production-chains [{:wood -1} {:stone -1}]
       :max         12}
      {:type        :marketplace
       :personnel   {:explorers -1}
       :category    :misc
       :letter      "K"
       :description "Moves all resources from adjacent tiles to itself."
       :not-implemented true
       :max         4}
      {:type :trading-post
       :personnel   {:explorers -1}
       :category    :misc
       :description
       "Trade resources 2 to 1 according to what trades are available
                            from a rotating trading wheel of options (not yet implemented)."
       :not-implemented true
       :valid-lands #{:plains}
       :max 6}
      {:type        :library
       :personnel   {:explorers -1}
       :category    :misc
       :description "Can use opponent development without paying them a VP"
       :valid-lands #{:mountain}
       :not-implemented true
       :max         2}
      {:type        :terraformer
       :personnel   {:explorers -1}
       :category    :misc
       :description "Change the land type of any tile"
       :not-implemented true
       :max         3}]]
    (-> dev
        (assoc-if-nil :valid-lands generally-valid-lands)
        (assoc-if-nil :on-placement identity)
        (assoc-if-nil :land-accumulation {}))))


(rf/reg-event-db
  :development/use
  (undoable "Development Use")
  (fn [db [_ development {:keys [row-idx col-idx worker-owner] :as tile}]]
    (assoc db :message "Not Yet Implemented")
    #_(let [current-player-name (:player-name @(rf/subscribe [:current-player]))
            tax (if (= current-player-name (:owner development))
                  {}
                  (:tax development))
            [cost-payable updated-db] (update-resources-with-check
                                        db (:current-player-idx db) tax)
            use-fn (if (:use development) (:use development) identity)]
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
                           (use-fn development tile))
          :else        updated-db))))


