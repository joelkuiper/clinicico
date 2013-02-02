(ns cliniccio.R.util
  (:use     [cliniccio.util]) 
  (:require [clojure.java.io :as io])
  (:import (org.rosuda.REngine)
           (org.rosuda.REngine.Rserve RConnection)))
           
(defn connect [] 
  (let [conn (try 
               (RConnection.)
               (catch Exception e (throw (Exception. "Could not connect to RServe" e))))]
    (if (.isConnected conn) 
      conn)))

(defn- convert-fn [field] 
  (cond 
    (instance? org.rosuda.REngine.REXPInteger field) #(.asIntegers %)
    (instance? org.rosuda.REngine.REXPFactor field) (comp #(.asStrings %) #(.asFactor %))
    (instance? org.rosuda.REngine.REXPDouble field) #(.asDoubles %)
    :else (throw (Exception. (str "Could not convert field " field)))))

(defn in-list  [data $field]
  (let [members (.at data $field)]
    (if-not (nil? members)
      (seq ((convert-fn members) members))
      nil)))

(defn parse [R cmd] 
  (.parseAndEval R cmd nil true))

(defn plot [R item] 
  (let [img-dev (parse R (str "try(png('"item".png'))"))]
    (if (.inherits img-dev "try-error")
      (throw (Exception. (str "Could not initiate image device: " (.asString img-dev))))
      (.voidEval R (str "plot(" item "); dev.off()")))))

(defn parse-matrix [matrix] 
  (let [dimnames (.getAttribute matrix "dimnames")
        labels  (map (fn [ls] (if-not (.isNull ls) (seq (.asStrings ls)) nil)) (.asList dimnames))
        data (map seq (.asDoubleMatrix matrix))]
    (into {} 
      (mapcat (fn [[k v]] {k (zipmap (or (second labels) (range 1 (inc (count v)))) v)}) 
              (zipmap (or (first labels) (range 1 (inc (count data)))) data)))))

(defn list-to-map [data]
  (let [ks (.keys data)] 
    (zipmap ks 
      (map (fn [k] 
       (let [field (.at data k)]
         (seq ((convert-fn field) field)))) ks))))
