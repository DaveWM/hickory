(ns hickory.test.core
  (:use clojure.test
        hickory.core)
  (:require [clojure.zip :as zip]))

;; This document tests: doctypes, comments, white space text nodes, attributes,
;; and cdata nodes.
(deftest basic-documents
  (is (= ["<!DOCTYPE html>"
          "<!--comment-->"
          [:html {}
           [:head {}]
           [:body {}
            [:a {:href "foo"} "foo"] " "
            [:a {:id "so", :href "bar"} "bar"]
            [:script {:src "blah.js"} "alert(\"hi\");"]]]]
         (as-hiccup (parse "<!DOCTYPE html><!--comment--><a href=\"foo\">foo</a> <a id=\"so\" href=\"bar\">bar</a><script src=\"blah.js\">alert(\"hi\");</script>"))))
  (is (= {:type :document,
          :children [{:type :document-type,
                      :attrs {:name "html", :publicid "", :systemid ""}}
                     {:type :comment
                      :children ["comment"]}
                     {:type :element,
                      :attrs {},
                      :tag :html,
                      :children [{:type :element,
                                  :attrs {},
                                  :tag :head,
                                  :children []}
                                 {:type :element,
                                  :attrs {},
                                  :tag :body,
                                  :children [{:type :element,
                                              :attrs {:href "foo"},
                                              :tag :a,
                                              :children ["foo"]}
                                             " "
                                             {:type :element,
                                              :attrs {:id "so", :href "bar"},
                                              :tag :a,
                                              :children ["bar"]}
                                             {:type :element,
                                              :attrs {:src "blah.js"},
                                              :tag :script,
                                              :children ["alert(\"hi\");"]}]}]}]}
         (as-dom-map (parse "<!DOCTYPE html><!--comment--><a href=\"foo\">foo</a> <a id=\"so\" href=\"bar\">bar</a><script src=\"blah.js\">alert(\"hi\");</script>"))))
  )

;; Want to test a document fragment that has multiple nodes with no parent,
;; as well as a text node between nodes.
(deftest basic-document-fragment
  (is (= [[:a {:href "foo"} "foo"] " "
          [:a {:href "bar"} "bar"]]
         (map as-hiccup
              (parse-fragment "<a href=\"foo\">foo</a> <a href=\"bar\">bar</a>"))))
  (is (= [{:type :element,
           :attrs {:href "foo"},
           :tag :a,
           :children ["foo"]}
          " "
          {:type :element,
           :attrs {:href "bar"},
           :tag :a,
           :children ["bar"]}]
         (map as-dom-map
              (parse-fragment "<a href=\"foo\">foo</a> <a href=\"bar\">bar</a>")))))

(deftest html-zipper
  (is (= {:type :document,
          :children [{:type :element,
                      :attrs {},
                      :tag :html,
                      :children [{:type :element,
                                  :attrs {},
                                  :tag :head,
                                  :children []}
                                 {:type :element,
                                  :attrs {},
                                  :tag :body,
                                  :children [{:type :element,
                                              :attrs {},
                                              :tag :a,
                                              :children []}]}]}]}
         (zip/node (html-zip (as-dom-map (parse "<a>"))))))
  (is (= {:type :element,
          :attrs {},
          :tag :html,
          :children [{:type :element,
                      :attrs {},
                      :tag :head,
                      :children []} {:type :element,
                                     :attrs {},
                                     :tag :body,
                                     :children [{:type :element,
                                                 :attrs {},
                                                 :tag :a,
                                                 :children []}]}]}
       (-> (html-zip (as-dom-map (parse "<a>")))
           zip/next zip/node)))
  (is (= {:type :element, :attrs {}, :tag :head, :children []}
       (-> (html-zip (as-dom-map (parse "<a>")))
           zip/next zip/next zip/node)))
  (is (= {:type :element,
          :attrs {},
          :tag :body,
          :children [{:type :element,
                      :attrs {},
                      :tag :a,
                      :children []}]}
         (-> (html-zip (as-dom-map (parse "<a>")))
             zip/next zip/next zip/next zip/node)))
  (is (= {:type :element,
          :attrs {},
          :tag :html,
          :children [{:type :element,
                      :attrs {},
                      :tag :head,
                      :children []}
                     {:type :element,
                      :attrs {},
                      :tag :body,
                      :children [{:type :element,
                                  :attrs {},
                                  :tag :a,
                                  :children []}]}]}
         (-> (html-zip (as-dom-map (parse "<a>")))
             zip/next zip/next zip/next zip/up zip/node))))

(deftest html-output
  (is (= "<!DOCTYPE html><html><head></head><body><p><!--hi--><a href=\"foo\" id=\"bar\">hi</a></p></body></html>"
         (dom-to-html (as-dom-map (parse "<!DOCTYPE html><P><!--hi--><a href=foo id=\"bar\">hi"))))))