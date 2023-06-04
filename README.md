# clj-seh-challenge

## LPG Implementation

There are 2 implementations:

1. `LPG:Scratch` : my homemade implementation of a labelled property graph
2. `LPG:Loom` : with the same API, but uses Loom (https://github.com/aysylu/loom) as the graph mechanism under the hood.  Used for validation and testing.

They both implement protocol `LPG`.

All of this is in the `lpg.clj` file.

## Run Tests

I wrote a lot of code using test-driven development.  I wrote most of the tests to take either impl of `LPG` to check my implementation against 
a "more correct" version using Loom.

In repl:
``` 
(run-tests 'clj-seh-challenge.core-test)
```
or in terminal:
``` 
lein test 
```

output:
``` 
...[logging]...
lein test clj-seh-challenge.core-test
Simple Test Took  clj_seh_challenge.lpg.LPG:Scratch  :  0.004  s
Perf Test Took  clj_seh_challenge.lpg.LPG:Scratch  :  0.284  s
Serialize Test Took  clj_seh_challenge.lpg.LPG:Scratch  :  0.013  s
Slurp Serialize Test Took  clj_seh_challenge.lpg.LPG:Scratch  :  0.003  s
Simple Test Took  clj_seh_challenge.lpg.LPG:Loom  :  0.014  s
Perf Test Took  clj_seh_challenge.lpg.LPG:Loom  :  0.246  s

Ran 2 tests containing 31 assertions.
0 failures, 0 errors.
```

- `Simple Test` : a simple scenario.  tests both implementations
- `Perf Test`: a test with slightly more data, to compare the impls.  tests both implementations
- `Serialize Test`: tests only `LPG:Scratch`, testing the to and from JSON functionality
- `Slurp Serialize Test`: tests only `LPG:Scratch`, testing from an existing JSON file in this repo, `test-lpg.json`


See the test namespace for more documentation about the tests.


## LPG Usage

```
(let [;; Instantiate
      g                    (lpg/->lpg:scratch)
      
      ;; For a noisier, logging version, provide a logging implementation that emits logs:
      g                    (lpg/->lpg:scratch (lpg/LPG-Timbre-Logger.))

      ;; Add nodes: the data structure is immutable, calling a mutating method (like add-node)
      ;;   returns a new one containing updated internal state
      g                    (->
                             g
                             (lpg/add-node "desk")
                             (lpg/add-node "briefcase")
                             (lpg/add-node "pencil 1")
                             (lpg/add-node "pencil 2")
                             (lpg/add-node "pen")
                             (lpg/add-node "sharpie")
                             (lpg/add-node "eraser"))

      ;; Add edge labels
      g                    (->
                             g
                             (lpg/add-attr-to-edge ["pencil 1" "0.7mm"] "writes")
                             (lpg/add-attr-to-edge ["pencil 2" "0.7mm"] "writes")
                             (lpg/add-attr-to-edge ["pen" "blue ink"] "writes")
                             (lpg/add-attr-to-edge ["sharpie" "yellow"] "writes")
                             (lpg/add-attr-to-edge ["briefcase" "pencil 2"] "contains")
                             (lpg/add-attr-to-edge ["desk" "pencil 1"] "contains")
                             (lpg/add-attr-to-edge ["desk" "sharpie"] "contains")
                             (lpg/add-attr-to-edge ["briefcase" "pen"] "contains")
                             (lpg/add-attr-to-edge ["desk" "eraser"] "contains"))


      ;; What node is the child of provided parent with label?
      desk-contains        (lpg/get-child-nodes-with-attr g "desk" "contains")
      ;; => ["pencil 1" "sharpie" "eraser"]


      ;; What node is the parent of provided child with label?
      pen-contained-by     (lpg/get-parent-nodes-with-attr g "pen" "contains")
      ;; => ["briefcase"]


      ;; A flavor of query function allows us to filter nodes based on their
      ;;  outgoing edge labels.  Here, for every X in [X "writes" Y], we want to
      ;;  get the results Y in aggregate.
      what-can-write       (lpg/get-nodes-query
                             g
                             {:by-outgoing-edge-attrs (fn [attrs] (get attrs "writes"))})
      ;; => #{"pen" "pencil 2" "sharpie" "pencil 1"}

      
      ;; Another flavor of query allows us to do some basic grouping/mapping/reductions,
      ;;  which allows us to do some basic aggregation.
      ;; Query steps:
      ;; For nodes which are parents of a provided label,
      ;;  map the nodes to children of another provided label,
      ;;  and reduce those children with provided function.
      ;; In this case, that means: "containers of writing tools have how many writing styles?"
      containers-can-write (lpg/aggregate-nodes-query
                             g
                             {:group-by "contains"
                              :map      "writes"
                              :reduce   count})
      ;; => {"desk" {"0.7mm"  1, "yellow" 1}, "briefcase" {"0.7mm"  1, "blue ink" 1}}


      ;; Here we omit the :group-by key, which removes the previous first step of finding
      ;;  the parent nodes to group the reduction.  In this case, the rest of the query 
      ;;  applies to the whole graph.
      all-can-write        (lpg/aggregate-nodes-query
                             g
                             {:map    "writes"
                              :reduce count})
      ;; =>  {"blue ink" 1, "yellow" 1, "0.7mm" 2}

      
      ;; Get the JSON string of the graph so we can save it:
      json-string          (lpg/to-json g)
      
      ;; In reverse, get an instance of a graph from JSON string:
      g-from-json-string   (lpg/from-json g json-string)
      ])
```

## TODO
Here's what I think might be missing. A lot of these can be done outside lib using existing methods,
but a better version of this library would probably include these:

- [x] BF traverse method 
- [ ] DF traverse method
- [ ] labels for nodes.  it was a design decision to omit node labels in this impl, a better version might include this
- [ ] subgraph method
- [ ] path method
- [ ] method to return all nodes reachable from a provided node
- [ ] a hook to visualize (loom has a method that uses graphviz, my impl would need to use that or something from scratch)
- [ ] delete nodes/edges

## License

Copyright © 2023 Octavian Geagla

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
