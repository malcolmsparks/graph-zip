(ns graph-zip.in-memory
   (:use [graph-zip.core])
   (:require [clojure.zip :as zip]
             [clojure.xml :as xml]
             [clojure.data.zip :as zf]))

(defrecord InMemoryGraph [graph]
    Graph
    (props-map [this node]
      (or (get (:graph this) node)
          {}))
    (prop-values [this node prop]
      (let [props (props-map this node)]
        (or (get props prop)
            []))))

(defn- add-statement-to-map [graph-map {:keys [subject property object]}]
  (assoc graph-map subject
         (let [props (get graph-map subject)]
           (assoc props property
                  (conj (get props property) object)))))

;; statements :: [{:subject :property :object}]
(defn build-in-memory-graph
  ([statements]
     (build-in-memory-graph nil statements))
  ([graph statements]
     (InMemoryGraph. (reduce add-statement-to-map (:graph-map graph) statements))))

;; ----------- TESTS

(def my-map (build-in-memory-graph [{:subject "prod-host" :property :instance :object "prod-host/instance"}
                                    {:subject "prod-host" :property :instance :object "prod-host/instance2"}
                                    {:subject "prod-host/instance" :property :userid :object "my-user"}
                                    {:subject "prod-host/instance" :property "label" :object "1"}
                                    {:subject "prod-host/instance2" :property "label" :object "2"}
                                    {:subject "prod-host/instance" :property "cmdb:jvm" :object "prod-host/instance/jvm"}
                                    {:subject "prod-host/instance/jvm" :property "cmdb:maxMem" :object "1024m"}]))

(def prod-host-zipper (graph-zipper my-map "prod-host"))

(zipper-node (zip1-> prod-host-zipper
                     :instance
                     [(prop= "label" "1")]
                     "cmdb:jvm"
                     "cmdb:maxMem")) ;; -> "1024m"

(zipper-node (zip1-> prod-host-zipper
                     :instance
                     (prop= "label" "2"))) ;; -> "prod-host/instance2"

(map zipper-node (zip-> prod-host-zipper
                        :instance)) ;; -> ("prod-host/instance2" "prod-host/instance")

