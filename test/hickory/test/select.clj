(ns hickory.test.select
  (:use clojure.test)
  (:require [hickory.core :as hickory]
            [hickory.select :as select]
            [hickory.zip :as hzip]
            [clojure.zip :as zip]))

(def html1
  "<!DOCTYPE html>
<!-- Comment 1 -->
<html>
<body>
<h1>Heading</h1>
<p>Paragraph</p>
<a href=\"http://example.com\">Link</a>
<div class=\"aclass bclass cool\">
<div class=\"subdiv cool\" id=\"deepestdiv\">Div</div>
<!-- Comment 2 -->
<span id=\"anid\" class=\"cool\">Span</span>
</div>
</body>
</html>")

(deftest select-next-loc-test
  (testing "The select-next-loc function."
    (let [htree (hickory/as-hickory (hickory/parse html1))
          find-comment-fn (fn [zip-loc]
                            (= (:type (zip/node zip-loc))
                               :comment))]
      (let [selection (select/select-next-loc find-comment-fn
                                              (hzip/hickory-zip htree))]
        (is (and (= :comment
                    (-> selection zip/node :type))
                 (re-find #"Comment 1" (-> (zip/node selection)
                                           :content first))))
        (let [second-selection (select/select-next-loc find-comment-fn
                                                       (zip/next selection))]
          (is (and (= :comment
                      (-> second-selection zip/node :type))
                   (re-find #"Comment 2" (-> (zip/node second-selection)
                                             :content first))))
          (is (nil? (select/select-next-loc find-comment-fn
                                            (zip/next second-selection)))))))))

(deftest select-test
  (testing "The select function."
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (fn [zip-loc]
                                       (= (:type (zip/node zip-loc))
                                          :document-type))
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :document-type
                    (-> selection first :type)))))
      (let [selection (select/select (fn [zip-loc]
                                       (= (:type (zip/node zip-loc))
                                          :comment))
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :comment (:type %))
                                    selection))))))))

;;
;; Selector tests
;;

(deftest tag-test
  (testing "tag selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/tag "h1")
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :h1 (-> selection first :tag)))))
      ;; Case-insensitivity test
      (let [selection (select/select (select/tag "H1")
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :h1 (-> selection first :tag)))))
      ;; Non-string argument test
      (let [selection (select/select (select/tag :h1)
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :h1 (-> selection first :tag))))))))

(deftest id-test
  (testing "id selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/id "deepestdiv")
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"deepestdiv"
                          (-> selection first :attrs :id)))))
      (let [selection (select/select (select/id "anid")
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"anid"
                          (-> selection first :attrs :id)))))
      ;; Case-insensitivity test
      (let [selection (select/select (select/id "ANID")
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"anid"
                          (-> selection first :attrs :id)))))
      ;; Non-string argument test
      (let [selection (select/select (select/id :anid)
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"anid"
                          (-> selection first :attrs :id))))))))

(deftest class-test
  (testing "class selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/class "aclass")
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"aclass"
                          (-> selection first :attrs :class)))))
      (let [selection (select/select (select/class "cool")
                                     htree)]
        (is (and (= 3 (count selection))
                 (every? #(not (nil? %))
                         (map #(re-find #"cool"
                                        (-> % :attrs :class))
                              selection)))))
      ;; Case-insensitivity test
      (let [selection (select/select (select/class "Aclass")
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"aclass"
                          (-> selection first :attrs :class)))))
      ;; Non-string argument test
      (let [selection (select/select (select/class :aclass)
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"aclass"
                          (-> selection first :attrs :class))))))))

