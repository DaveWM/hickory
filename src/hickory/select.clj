(ns hickory.select
  "Functions to query hickory-format HTML data.

   See clojure.zip for more information on zippers, locs, nodes, next, etc."
  (:require [clojure.zip :as zip]
            [clojure.string :as string]
            [hickory.zip :as hzip])
  (:refer-clojure :exclude [class]
                  :rename {and core-and
                           or core-or}))


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

(defn tag
  "Return a function that takes a zip-loc argument and returns the
   zip-loc passed in iff it has the given tag. The tag argument can be
   a String or Named (keyword, symbol). The tag name comparison
   is done case-insensitively."
  [tag]
  (fn [hzip-loc]
    (let [node (zip/node hzip-loc)
          node-tag (-> node :tag)]
      (if (core-and node-tag
               (= (string/lower-case (name node-tag))
                  (string/lower-case (name tag))))
        hzip-loc))))

(defn attr
  "Returns a function that takes a zip-loc argument and returns the
   zip-loc passed in iff it has the given attribute, and that attribute
   optionally satisfies a predicate given as an additional argument. With
   a single argument, the attribute name (a string, keyword, or symbol),
   the function returned will return the zip-loc if that attribute is
   present (and has any value) on the zip-loc's node. The attribute name
   will be compared case-insensitively, but the attribute value (if present),
   will be passed as-is to the predicate.

   If the predicate argument is given, it will only return the zip-loc if
   that predicate is satisfied when given the attribute's value as its only
   argument. Note that the predicate only gets called when the attribute is
   present, so it can assume its argument is not nil."
  ([attr-name]
     ;; Since we want this call to succeed in any case where this attr
     ;; is present, we pass in a function that always returns true.
     (attr attr-name (fn [_] true)))
  ([attr-name predicate]
     ;; Note that attribute names are normalized to lowercase by
     ;; jsoup, as an html5 parser should; see here:
     ;; http://www.whatwg.org/specs/web-apps/current-work/#attribute-name-state
     (fn [hzip-loc]
       (let [node (zip/node hzip-loc)
             attr-key (keyword (string/lower-case (name attr-name)))]
         ;; If the attribute does not exist, we'll definitely return null.
         ;; Otherwise, we'll ask the predicate if we should return hzip-loc.
         (if (core-and (contains? (:attrs node) attr-key)
                  (predicate (get-in node [:attrs attr-key])))
           hzip-loc)))))

(defn id
  "Returns a function that takes a zip-loc argument and returns the
   zip-loc passed in iff it has the given id. The id argument can be
   a String or Named (keyword, symbol). The id name comparison
   is done case-insensitively."
  [id]
  (attr :id #(= (string/lower-case %)
                (string/lower-case (name id)))))

(defn class
  "Returns a function that takes a zip-loc argument and returns the
   zip-loc passed in iff it has the given class. The class argument can
   be a String or Named (keyword, symbol). The class name comparison
   is done case-insensitively."
  [class-name]
  (letfn [(parse-classes [class-str]
                       (into #{} (mapv string/lower-case
                                       (string/split class-str #" "))))]
    (attr :class #(contains? (parse-classes %)
                             (string/lower-case (name class-name))))))

;;
;; Selector combinators
;;

(defn and
  "Takes any number of selectors and returns a selector that is true if
   all of the argument selectors are true."
  [& selectors]
  (fn [zip-loc]
    (if (every? #(% zip-loc) selectors)
      zip-loc)))

(defn or
  "Takes any number of selectors and returns a selector that is true if
   any of the argument selectors are true."
  [& selectors]
  (fn [zip-loc]
    (if (some #(% zip-loc) selectors)
      zip-loc)))