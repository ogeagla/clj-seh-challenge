(ns clj-seh-challenge.lpg
  (:require
    [cheshire.core :as json]
    [loom.attr :as loom-attr]
    [loom.derived :as loom-derived]
    [loom.graph :as loom-graph]))


(defprotocol LPG
  "Labelled Property Graph.  Stores nodes, directed edges, and attributes (labels).
  Support various queries and serialization to/from JSON."

  ;; TODO: Here's what I think might be missing. A lot of these can be done outside lib using existing methods,
  ;; but a better version of this library would probably include these:
  ;; - a traverse method (BF/DF)
  ;; - labels for nodes.  it was a design decision to omit node labels in this impl, a better version might include this
  ;; - subgraph method
  ;; - path method
  ;; - method to return all nodes reachable from a provided node
  ;; - a hook to visualize (loom has a method that uses graphviz, my impl would need to use that or something from scratch)
  ;; - delete nodes/edges

  (add-node
    [this node]
    "Adds a node to the graph.")

  (add-attr-to-edge
    [this edge attr-key]
    "Adds an attribute (label) to an edge.  Also adds the edge to the graph.")

  (get-nodes
    [this]
    "Get all nodes in graph.")

  (to-json
    [this]
    "This graph to JSON")

  (from-json
    [this json-string]
    "Returns this, as parsed from a JSON file")

  (get-nodes-query
    [this query]
    "Query for nodes in graph.  Currently, supports filtering nodes by outgoing edge labels.")

  (aggregate-nodes-query
    [this query]
    "Allows for a simple aggregation of nodes via a simplified group-by / map-reduce pipeline for a single attr.")

  (get-attrs-for-edge
    [this edge]
    "Get attributes (labels) for an edge.")

  (get-attr-for-edge
    [this edge attr-key]
    "Get an attribute (label) for an edge.")

  (get-node-edges-incoming
    [this node]
    "Get all incoming edges into a node.")

  (get-node-edges-outgoing
    [this node]
    "Get all outgoing edges from a node.")

  (get-parent-nodes-with-attr
    [this node attr-key]
    "Get nodes which have an edge to provided node via the attribute (label).")

  (get-child-nodes-with-attr
    [this node attr-key]
    "Gets nodes which have an edge from provided node via the attribute (label)."))


(defn reduce-grouped-values
  [g-reduce grouped-vals]
  (->> grouped-vals
       (mapcat identity)
       (group-by identity)
       (map (fn [[k vs]] [k (g-reduce vs)]))
       (into {})))


(defrecord LPG:Loom
  [g]

  LPG

  (to-json [this])


  (from-json [this json-string])


  (get-nodes
    [this]
    (loom-graph/nodes g))


  (get-nodes-query
    [this {by-outgoing-edge-attrs :by-outgoing-edge-attrs
           :as                    query}]
    (->>
      (loom-derived/edges-filtered-by (fn [[n1 n2]]
                                        (by-outgoing-edge-attrs (get-attrs-for-edge this [n1 n2]))) g)
      (loom-graph/edges)
      (map first)
      set))


  (aggregate-nodes-query
    [this {g-group-by :group-by
           g-map      :map
           g-reduce   :reduce
           :as        query}]
    (if g-group-by
      (->> g
           (loom-derived/edges-filtered-by
             (fn [[n1 n2]]
               (contains? (set (keys (loom-attr/attrs g n1 n2)))
                          g-group-by)))
           (loom-graph/edges)
           (map first)
           set
           (map (fn [n]
                  [n
                   (->> (get-child-nodes-with-attr this n g-group-by)
                        (map (fn [node] (get-child-nodes-with-attr this node g-map)))
                        (reduce-grouped-values g-reduce))]))
           (into {}))


      (->> g
           (loom-graph/edges)
           (map first)
           set
           (map (fn [node] (get-child-nodes-with-attr this node g-map)))
           (reduce-grouped-values g-reduce))))


  (add-node
    [this node]
    (assoc this :g (loom-graph/add-nodes g node)))


  (add-attr-to-edge
    [this edge attr-key]
    (assoc this :g (loom-attr/add-attr-to-edges
                     (loom-graph/add-edges g edge)
                     attr-key
                     true
                     [edge])))


  (get-attrs-for-edge
    [this edge]
    (loom-attr/attrs g (first edge) (second edge)))


  (get-attr-for-edge
    [this edge attr-key]
    (loom-attr/attr g (first edge) (second edge) attr-key))


  (get-node-edges-incoming
    [this node]
    (->>
      g
      (loom-derived/edges-filtered-by (fn [[n1 n2]] (= node n2)))
      (loom-graph/edges)))


  (get-node-edges-outgoing
    [this node]
    (->>
      g
      (loom-derived/edges-filtered-by
        (fn [[n1 n2]] (= node n1)))
      (loom-graph/edges)))


  (get-child-nodes-with-attr
    [this node attr-key]
    (->> (get-node-edges-outgoing this node)
         (map (fn [[n1 n2]]
                (when (get-attr-for-edge this [n1 n2] attr-key)
                  n2)))
         (remove nil?)
         vec))


  (get-parent-nodes-with-attr
    [this node attr-key]
    (->> (get-node-edges-incoming this node)
         (map (fn [[n1 n2]]
                (when (get-attr-for-edge this [n1 n2] attr-key)
                  n1)))
         (remove nil?)
         vec)))


(defn ->lpg:loom
  []
  (let [g (loom-graph/digraph)]
    (LPG:Loom. g)))


(defrecord LPG:Scratch
  [g]

  LPG

  (to-json
    [this]
    (json/generate-string (:g this)))


  (from-json
    [this json-string]
    (let [data (json/parse-string json-string)
          data (-> data
                   (assoc :attrs (get data "attrs"))
                   (assoc :edges (get data "edges"))
                   (assoc :nodes (get data "nodes"))
                   (dissoc "attrs" "edges" "nodes"))]
      (assoc this :g data)))


  (get-nodes [this] (-> g :nodes vec))


  (get-nodes-query
    [this {by-outgoing-edge-attrs :by-outgoing-edge-attrs
           :as                    query}]
    (->>
      (filter (fn [[n1 n2]]
                (let [attrs-map (get-attrs-for-edge this [n1 n2])]
                  (by-outgoing-edge-attrs attrs-map #_(loom-attr/attrs g n1 n2))))
              (:edges g))
      (map first)
      set))


  (aggregate-nodes-query
    [this {g-group-by :group-by
           g-map      :map
           g-reduce   :reduce
           :as        query}]
    (if g-group-by
      (->> (:edges g)
           (filter (fn [[n1 n2]]
                     (contains? (set (keys (get-attrs-for-edge this [n1 n2])))
                                g-group-by)))
           (map first)
           set
           (map (fn [n]
                  [n
                   (->>
                     (get-child-nodes-with-attr this n g-group-by)
                     (map (fn [node] (get-child-nodes-with-attr this node g-map)))
                     (reduce-grouped-values g-reduce))]))
           (into {}))

      (->> (:edges g)
           (map first)
           set
           (map (fn [node] (get-child-nodes-with-attr this node g-map)))
           (reduce-grouped-values g-reduce))))


  (add-node [this node] (assoc this :g (update g :nodes concat [node])))


  (add-attr-to-edge
    [this edge attr-key]
    (assoc this :g (-> g
                       (update :edges concat [edge])
                       (assoc-in [:attrs (first edge) (second edge) attr-key] true))))


  (get-attrs-for-edge
    [this edge]
    (get-in (:attrs g) [(first edge) (second edge)]))


  (get-attr-for-edge
    [this edge attr-key]
    (get-in (:attrs g) [(first edge) (second edge) attr-key]))


  (get-node-edges-incoming
    [this node]
    (filter
      (fn [[n1 n2]] (= node n2))
      (:edges g)))


  (get-node-edges-outgoing
    [this node]
    (filter
      (fn [[n1 n2]] (= node n1))
      (:edges g)))


  (get-parent-nodes-with-attr
    [this node attr-key]
    (->> (get-node-edges-incoming this node)
         (map (fn [[n1 n2]]
                (when (get-attr-for-edge this [n1 n2] attr-key)
                  n1)))
         (remove nil?)
         vec))


  (get-child-nodes-with-attr
    [this node attr-key]
    (->> (get-node-edges-outgoing this node)
         (map (fn [[n1 n2]]
                (when (get-attr-for-edge this [n1 n2] attr-key)
                  n2)))
         (remove nil?)
         vec)))


(defn ->lpg:scratch
  []
  (let [g {}]
    (LPG:Scratch. g)))
