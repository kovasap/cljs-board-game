(ns app.interface.lands)

; TODO maybe do something with these
(def temperature-scale
  [:frozen :cold :moderate :hot :scorching])
(def order-scale
  [:chaotic :disordered :aligned :crystallized])

; In order of cycle shifting.
(def lands
  [{:letter     "F"
    :type       :forest
    :perlin-cutoff 0.35
    :style      {:background-color "forestgreen"}}
   {:letter     "P"
    :type       :plains
    :perlin-cutoff 0.3
    :style      {:background-color "orange"}}
   {:letter     "S"
    :type       :sand
    :perlin-cutoff 0.2
    :style      {:background-color "yellow"}}
   {:letter     "W"
    :type       :water
    :perlin-cutoff 0.0
    :style      {:background-color "MediumTurquoise"}}
   {:letter     "M"
    :type       :mountain
    :perlin-cutoff 0.75
    :style      {:background-color "grey"}}
   #_{:letter     "V"
      :type       :void
      :perlin-cutoff 10.0
      :style      {:background-color "black"}}])
