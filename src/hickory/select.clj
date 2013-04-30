(ns hickory.select
  "Functions to query hickory-format HTML data.

   See clojure.zip for more information on zippers, locs, nodes, next, etc."
  (:require [clojure.zip :as zip]
            [clojure.string :as string]
            [hickory.zip :as hzip]))


;;
;; Select
;;

(defn select-next-loc
  "Given a selector function and a loc inside a hickory zip data structure,
   returns the next zipper loc that satisfies the selection function. This can
   be the loc that is passed in, so be sure to move to the next loc if you
   want to use this function to exhaustively search through a tree manually.
   Note that if there is no next node that satisfies the selection function, nil
   is returned."
  [selector-fn hzip-loc]
  (loop [loc hzip-loc]
    (if (zip/end? loc)
      nil
      (if (selector-fn loc)
        loc
        (recur (zip/next loc))))))

(defn select-locs
  "Given a selector function and a hickory data structure, returns a vector
   containing all of the zipper locs selected by the selector function."
  [selector-fn hickory-tree]
  (loop [loc (select-next-loc selector-fn
                              (hzip/hickory-zip hickory-tree))
         selected-nodes (transient [])]
    (if (nil? loc)
      (persistent! selected-nodes)
      (recur (select-next-loc selector-fn (zip/next loc))
             (conj! selected-nodes loc)))))

(defn select
  "Given a selector function and a hickory data structure, returns a vector
   containing all of the hickory nodes selected by the selector function."
  [selector-fn hickory-tree]
  (mapv zip/node (select-locs selector-fn hickory-tree)))

;;
;; Selectors
;;

(defn has-class?
  "Returns a function that takes a zip-loc argument and returns the
  zip-loc passed in iff it has the given class. The class name comparison
  is done case-insensitively."
  [class-name]
  (letfn [(parse-classes [class-str]
            (into #{} (mapv string/lower-case
                            (string/split class-str #" "))))]
    (fn [hzip-loc]
      (let [node (zip/node hzip-loc)
            class-str (-> node :attrs :class)
            ;; Check first, since not all nodes will have :attrs key
            classes (if class-str
                      (parse-classes class-str))]
        (if (contains? classes (string/lower-case class-name))
          hzip-loc)))))
