(ns xyz.jmullin.prospector.bot.JasonBot
  (:gen-class :implements [xyz.jmullin.prospector.game.ProspectorBot])
  (:require [clojure.pprint :as pp]))

;; Counter to keep track of the number of probes we've done. Prevents an 
;; infinite loop.
(def num-probes (atom 0))

;; The distance from a point to place new probe points.
(def delta-size 10)

;; A sequence of distance pairs used to calculate new probe points.
(def deltas (filter (fn [[dx dy]] (or (= dx 0) (= dy 0))) (drop 1 (for [x [0 delta-size (- delta-size)] y [0 (- delta-size) delta-size]] [x y]))))

;; The sequence of initial points to probe before any sorting or logic is applied.
(def initial-probe-points (for [x (range 50 500 100) y (range 50 500 100)] [x y]))

(defn coord 
  "Shortcut for creating a coordinate. Less useful than originally thought."
  [x y] 
  (new xyz.jmullin.prospector.game.Coord x y))

(defn probe-fn
  "Returns a function that probes a given x,y coordinate and increases the 
  number of probes."
  [probe]
  (fn [x y]
    (swap! num-probes inc)
    (.query probe (coord x y))))

(defn coord-compass-points
  "Returns a sequence of the north, east, south, west points from the given x,y 
  coordinate using the `deltas`."
  [[x y]]
  (map (fn [[dx dy]] [(+ x dx) (+ y dy)]) deltas))

(defn calc-new-probe-points
  "Given a sequence of previous probe points calculate new probe points in the 
  four cardinal directions."
  [prev-points]
  (mapcat (fn [[coord value]] (coord-compass-points coord)) prev-points))

(defn -getName
  "Return the name of your bot."
  [this]
  "JasonBot")

(defn -prospect
  "Prospect the plot by calling .query(coord) on the provided probe instance with the desired
   coordinate to query. Coordinates are in the range of 0 <= x < 512, 0 <= y < 512.
   Each query will return the value of the plot at the given coordinate. A maximum of 100 queries
   are allowed per plot; queries after this will return a value of 0.
   After returning from this method, your score for the plot will be the value of the largest
   query you made from the plot."
  [this probe]
  (let [probe (probe-fn probe)]
    (reset! num-probes 0)
    (loop [points-to-probe initial-probe-points]
      (if (> 100 @num-probes)
        (recur (->> points-to-probe
                    (map (fn [[x y]] [[x y] (probe x y)])) ; Creates a seq of [[x y] <probe value>] vectors.
                    (sort-by second)
                    reverse
                    (take 2)                               ; Take the 2 highest valued coordinates.
                    calc-new-probe-points))))))
