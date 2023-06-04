(ns clj-seh-challenge.core-test
  (:require
    [clj-seh-challenge.core :refer :all]
    [clj-seh-challenge.lpg :as lpg]
    [clojure.test :refer :all]
    [loom.io :as loom-io])
  (:import
    (clj_seh_challenge.lpg
      LPG-Timbre-Logger)
    (java.util
      Date)))


(defn lpg:simple-serialize-slurp
  [impl]
  (let [start-ms (-> (Date.) .getTime)
        lpg-impl (lpg/from-json impl (slurp "./test/clj_seh_challenge/test-lpg.json"))]
    (is (= 7
           (count (lpg/get-nodes lpg-impl))))

    (is (let [end-ms (-> (Date.) .getTime)]
          (println "Slurp Serialize Test Took " (type impl) " : " (float (/ (- end-ms start-ms) 1000)) " s")
          true))))


(defn lpg:simple-serialize-round-trip
  [impl]
  (let [start-ms (-> (Date.) .getTime)
        nodes    (->> (range 100)
                      (map str))
        attrs    (->> (range 10)
                      (map str))
        edges    (remove nil? (for [n1 (take 10 (repeatedly #(rand-nth (take 50 nodes))))
                                    n2 (take 10 (repeatedly #(rand-nth (take-last 50 nodes))))]
                                [n1 n2]))
        lpg-impl impl
        lpg-impl (->>
                   nodes
                   (reduce (fn [lpg-impl n]
                             (lpg/add-node lpg-impl n))
                           lpg-impl))
        lpg-impl (->>
                   edges
                   (reduce (fn [lpg-impl e]
                             (lpg/add-attr-to-edge lpg-impl e (rand-nth attrs)))
                           lpg-impl))]

    (is (= 100
           (count (lpg/get-nodes lpg-impl))))


    (is (= lpg-impl
           (let [j      (-> lpg-impl
                            (lpg/to-json))
                 from-j (lpg/from-json lpg-impl j)]
             from-j)))

    (is (let [end-ms (-> (Date.) .getTime)]
          (println "Serialize Test Took " (type impl) " : " (float (/ (- end-ms start-ms) 1000)) " s")
          true))))


(defn lpg:simple-perf
  [impl]
  (let [start-ms (-> (Date.) .getTime)
        nodes    (->> (range 500)
                      (map str))
        attrs    (->> (range 20)
                      (map str))
        edges    (remove nil? (for [n1 (take 40 (repeatedly #(rand-nth (take 250 nodes))))
                                    n2 (take 40 (repeatedly #(rand-nth (take-last 250 nodes))))]
                                [n1 n2]))
        lpg-impl impl
        lpg-impl (->>
                   nodes
                   (reduce (fn [lpg-impl n]
                             (lpg/add-node lpg-impl n))
                           lpg-impl))
        lpg-impl (->>
                   edges
                   (reduce (fn [lpg-impl e]
                             (lpg/add-attr-to-edge lpg-impl e (rand-nth attrs)))
                           lpg-impl))]

    (is (= 500
           (count (lpg/get-nodes lpg-impl))))

    (is (map? (lpg/aggregate-nodes-query lpg-impl
                                         {:group-by (rand-nth attrs)
                                          :map      (rand-nth attrs)
                                          :reduce   count})))

    (is (let [end-ms (-> (Date.) .getTime)]
          (println "Perf Test Took " (type impl) " : " (float (/ (- end-ms start-ms) 1000)) " s")
          true))))


(defn lpg:simple-test
  [impl]
  (let [start-ms    (-> (Date.) .getTime)
        fred        "fred"
        barney      "barney"
        roger       "roger"
        dan         "dan"
        sarah       "sarah"
        jim         "jim"
        ron         "ron"
        bob         "bob"
        david       "david"


        manages     "manages"
        prefers     "prefers"
        allergic-to "allergic-to"

        shellfish   "shellfish"
        dairy       "dairy"
        fun         "fun"

        mexican     "mexican"
        indian      "indian"
        french      "french"
        thai        "thai"
        texmex      "texmex"



        lpg-impl    (-> impl
                        (lpg/add-node fred)
                        (lpg/add-node barney)
                        (lpg/add-node roger)
                        (lpg/add-node dan)
                        (lpg/add-node sarah)
                        (lpg/add-node jim)
                        (lpg/add-node ron)
                        (lpg/add-attr-to-edge [fred barney] manages)
                        (lpg/add-attr-to-edge [fred roger] manages)
                        (lpg/add-attr-to-edge [fred dan] manages)
                        (lpg/add-attr-to-edge [fred bob] manages)
                        (lpg/add-attr-to-edge [sarah jim] manages)
                        (lpg/add-attr-to-edge [sarah ron] manages)
                        (lpg/add-attr-to-edge [sarah david] manages)

                        (lpg/add-attr-to-edge [david shellfish] allergic-to)
                        (lpg/add-attr-to-edge [bob fun] allergic-to)
                        (lpg/add-attr-to-edge [ron dairy] allergic-to)

                        (lpg/add-attr-to-edge [david indian] prefers)
                        (lpg/add-attr-to-edge [bob mexican] prefers)
                        (lpg/add-attr-to-edge [fred mexican] prefers)
                        (lpg/add-attr-to-edge [barney thai] prefers)
                        (lpg/add-attr-to-edge [roger indian] prefers)
                        (lpg/add-attr-to-edge [dan indian] prefers)
                        (lpg/add-attr-to-edge [sarah texmex] prefers)
                        (lpg/add-attr-to-edge [jim french] prefers)
                        (lpg/add-attr-to-edge [ron french] prefers))]


    ;; Example: we have an employee seating chart with preferences, we want to group the prefs by managers
    ;;  and find the most frequent food preferences to make lunch choices by "team"

    ;; who does fred manage?
    (is (= #{roger barney dan bob}
           (set (lpg/get-child-nodes-with-attr lpg-impl fred manages))))

    ;; who prefers french?
    (is (= [jim ron]
           (lpg/get-parent-nodes-with-attr lpg-impl french prefers)))

    ;; who are the managers?
    (is (= #{fred sarah}
           (lpg/get-nodes-query lpg-impl
                                {:by-outgoing-edge-attrs (fn [attrs] (get attrs manages))})))


    ;; for each group of "managed", what is the most preferred cuisine?
    (is (= {"sarah" {"french" 2, "indian" 1},
            "fred"  {"indian" 2, "mexican" 1, "thai" 1}}
           (lpg/aggregate-nodes-query lpg-impl
                                      {:group-by manages
                                       :map      prefers
                                       :reduce   count})))

    ;; for the same groups, what are people allergic to?
    (is (= {"sarah" {"dairy" 1, "shellfish" 1},
            "fred"  {"fun" 1}}
           (lpg/aggregate-nodes-query lpg-impl
                                      {:group-by manages
                                       :map      allergic-to
                                       :reduce   count})))

    ;; what if we want overall preferences?
    (is (= {"french" 2, "indian" 3, "mexican" 2, "thai" 1, "texmex" 1}
           (lpg/aggregate-nodes-query lpg-impl
                                      {:map    prefers
                                       :reduce count})))

    ;; and overall allergies?
    (is (= {"dairy" 1, "fun" 1, "shellfish" 1}
           (lpg/aggregate-nodes-query lpg-impl
                                      {:map    allergic-to
                                       :reduce count})))

    ;; BF traverse starting from the 2 topmost nodes of the test graph
    (is (= #{fred roger bob mexican barney dan indian fun thai}
           (set (lpg/bf-traverse lpg-impl fred))))

    (is (= #{sarah jim ron david texmex french dairy indian shellfish}
           (set (lpg/bf-traverse lpg-impl sarah))))

    (is (let [end-ms (-> (Date.) .getTime)]
          (println "Simple Test Took " (type impl) " : " (float (/ (- end-ms start-ms) 1000)) " s")
          true))


    #_(loom-io/view (:g lpg-impl))))


(deftest lpg:loom
  #_(testing "Loom Impl can pass initial tests"
      (lpg:init-test (lpg/->lpg:loom)))

  (testing "Loom Impl can pass simple scenario"
    (lpg:simple-test (lpg/->lpg:loom)))
  (testing "Loom Impl can perf test"
    (lpg:simple-perf (lpg/->lpg:loom))))


(deftest lpg:scratch

  (testing "clj can pass simple scenario"
    (lpg:simple-test (lpg/->lpg:scratch (LPG-Timbre-Logger.))))
  (testing "clj can perf test"
    (lpg:simple-perf (lpg/->lpg:scratch)))
  (testing "clj can de/serialize"
    (lpg:simple-serialize-round-trip (lpg/->lpg:scratch))
    (lpg:simple-serialize-slurp (lpg/->lpg:scratch))))


(comment
  (run-tests 'clj-seh-challenge.core-test))

