(ns app.interface.view.developments
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [clojure.string :as st]
            [app.interface.view.util :refer [tally-marks]]
            [app.interface.view.unique-id :refer [get-unique-id]]
            [app.interface.view.personnel :refer [personnel-view]]
            [app.interface.developments :refer [resources]]
            [app.interface.development-placement
             :refer
             [make-toggle-development-placement-fn]]
            ["cytoscape" :as cytoscape]))


(defn development-blueprint-view
  [development]
  (let [dev-name     (name (:type development))
        existing-num @(rf/subscribe [:num-developments (:type development)])
        unique-id    (str dev-name "-blueprint")
        currently-placing-this-development
        (= (:development-type @(rf/subscribe [:tile-selection/selection-data]))
           (:type development))]
    [:div
     {:key      unique-id
      :style    {:background  (if (:not-implemented development)
                                "LightGrey"
                                "LightBlue")
                 :text-align  "left"
                 :width       "250px"
                 :height      "300px"
                 :flex        1
                 :padding     "15px"
                 :font-weight (if currently-placing-this-development
                                "bold"
                                "normal")
                 :border      "2px solid black"}
      :on-click (make-toggle-development-placement-fn development)}
     [:div
      [:strong dev-name] " " existing-num "/" (:max development) " "
      [personnel-view (:personnel development)]]
     [:div
      [:small
       "Place in "
       (st/join ", " (sort (map name (:valid-lands development))))]]
     [:div (:description development)]
     (if (:production-chains development)
       [:div
        "Chains: "
        (for [chain (:production-chains development)]
          [:div {:key (str chain unique-id)}
           (str chain)])]
       nil)
     (if (:land-production development)
       [:div
        "Harvests: "
        (for [[land production] (:land-production development)]
          [:div {:key (str land unique-id)}
           (str land " : " production)])]
       nil)]))


; Should take a form like
;   [{:data {:id "a"}}
;    {:data {:id "b"}}
;    {:data {:id "c"}}
;    {:data {:id "d"}}
;    {:data {:id "e"}}
;    {:data {:id "ab" :source "a" :target "b"}}
;    {:data {:id "ad" :source "a" :target "d"}}
;    {:data {:id "be" :source "b" :target "e"}}
;    {:data {:id "cb" :source "c" :target "b"}}
;    {:data {:id "de" :source "d" :target "e"}}))]
(defn make-development-graph
  [developments]
  (concat
    ; Development nodes
    (mapv (fn [{:keys [letter type]}]
            {:data {:id letter :label type :type :development}})
      developments)
    ; Edges
    (reduce concat
      (mapv
        (fn [{:keys [production-chains letter]}]
          (reduce concat
            (for [production-chain production-chains]
              (mapv (fn [[k v]]
                      {:data {:id     (str (name k) letter)
                              :source (if (> v 0) letter (name k))
                              :target (if (> v 0) (name k) letter)
                              :label  (str v)}})
                production-chain))))
        developments))
    ; Resource Nodes
    (mapv (fn [resource]
            {:data {:id    (name resource)
                    :label (name resource)
                    :type  :resource}})
      resources)))
  

(defn cytoscape-resource-flow
  "See inspiration at https://blog.klipse.tech/visualization/2021/02/16/graph-playground-cytoscape.html."
  ; Note that we pass an atom here, not data, so that react will re-render on
  ; any changes.
  [developments-atom]
  (let [graph-element-id "graph"]
    (r/create-class
      {:reagent-render (fn [_]
                         ; dummy deref to trigger re-render on change
                         @developments-atom
                         [:div
                          "Cytoscape view:"
                          [:div {:id    graph-element-id
                                 :style {:height "400px" :width "600px"}}]])
       ; We use this react lifecycle function because our graph-element-id div
       ; must exist before we call the cytoscape functionality that populates
       ; it.
       :component-did-update
       (fn [_]
         (cytoscape
           (clj->js
             {:style     [{:selector "node"
                           :style    {:background-color "#666"
                                      :label "data(label)"}}
                          {:selector "edge"
                           :style    {:width       2
                                      :line-color  "#ccc"
                                      :target-arrow-color "#ccc"
                                      :curve-style "bezier"
                                      :target-arrow-shape "triangle"
                                      :label       "data(label)"}}]
              :layout    {:name       "cose"
                          :nodeDimensionsIncludeLabels true}
              :userZoomingEnabled false
              :userPanningEnabled false
              :boxSelectionEnabled false
              :container (js/document.getElementById graph-element-id)
              :elements  (make-development-graph @developments-atom)})))})))
       
(defn blueprints-view
  []
  ; TODO calculate this height based on the board height instead of hardcoding
  (let [developments (rf/subscribe [:blueprints])]
    [:div {:style {:height "1000px" :width "600px" :overflow "auto"}}
     [:div
      "Take turns placing developments by clicking on the one you want to "
      "place then clicking on a valid (highlighted) location to place it. "
      "You can cancel by clicking on the development again. "
      "Click \"End Turn\" to advance to the next player. "]
     [:br]
     [:div
      "You get 2 points for each adjacent development you have to other "
      "players."]
     [:br]
     [:div
      "The game ends when all developments are placed "
      "(note the limit next to each development name)!"]
     [:br]
     [cytoscape-resource-flow developments]
     [:br]
     (into [:div {:style {:display       "grid"
                          :grid-template-columns "auto auto"
                          :margin-bottom "100%"
                          :grid-gap      "10px"}}]
           (for [development (sort-by (fn [{:keys [not-implemented type]}]
                                        [not-implemented type])
                                      @developments)]
             [development-blueprint-view development]))]))


(defn development-build-button-view
  [development]
  [:button {:style         {:width "200px" :height "30px"}
            :on-mouse-over #(rf/dispatch [:popup-window-at-coords
                                          (development-blueprint-view
                                            development)
                                          {:x (.-clientX %)
                                           :y (.-clientY %)}])
            :on-mouse-out  #(rf/dispatch [:popup-window-at-coords nil nil])
            :on-click      (make-toggle-development-placement-fn development)}
   [:div
    [:strong (name (:type development))]
    " "
    [personnel-view (:personnel development)]]])


; TODO organize these by type
(defn build-buttons-view
  []
  (let [developments @(rf/subscribe [:blueprints])
        groups       (group-by :category developments)]
    (into
      [:div {:style {:display  "grid"
                     :grid-template-columns (st/join " "
                                                     (map (constantly "auto")
                                                       groups))
                     :grid-gap "5px"}}
       (for [[group sub-developments] groups]
         (into [:div {:style {:display  "grid"
                              :grid-template-columns "auto"
                              :grid-gap "5px"}}
                [:pre (name group)]]
               (for [development sub-developments]
                 [development-build-button-view development])))])))
