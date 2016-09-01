(ns http-deadlock.core
  (:require [org.httpkit.server :refer [run-server
                                        with-channel
                                        websocket?
                                        on-receive
                                        on-close
                                        send!]]
            [org.httpkit.client :as client]
            [clojure.core.async :refer [go chan <!! put!]]
            [gniazdo.core :as ws])
  (:gen-class))

(def clients (atom {}))

(def pending-requests (atom {}))

(defn ws-server [req]
  (with-channel req ws-ch
    (when (websocket? ws-ch)
      (let [id (.toString (java.util.UUID/randomUUID))]
        (swap! clients assoc id ws-ch)
        (swap! pending-requests assoc id {})
        (println "Client " id " connected.")
        (on-receive ws-ch (fn [msg]
                            (let [req-id (.substring msg 0 36)]
                              (if-let [cb (get-in @pending-requests [id req-id])]
                                (cb (.substring msg 37))
                                (println "Client " id " sent a message that is not an answer: " (.substring msg 37))))))
        (on-close ws-ch (fn []
                          (println "Client " id " disconnected.")
                          (swap! clients dissoc id)
                          (swap! pending-requests dissoc id)))))))

(defn web-server [{uri :uri :as req}]
  (let [[id & resource-path] (filter #(< 0 (.length %)) (.split uri "/"))
        client (get @clients id)]
    (if-not client
      {:status 404
       :body "No such client."}
      (let [req-id (.toString (java.util.UUID/randomUUID))
            c (chan)]
        (println "Processing request " req-id " for client " id ".")
        (swap! pending-requests assoc-in [id req-id] (fn [msg] (put! c msg)))
        (send! client (apply str (interpose " " (cons req-id resource-path))))
        {:status 200
         :body (<!! c)}))))

(defn app [{{upgrade "upgrade"} :headers :as req}]
  (if (= upgrade "websocket")
    (ws-server req)
    (web-server req)))

(def ws-clients (atom []))

(defn send-fn [i]
  (partial ws/send-msg (get @ws-clients i)))

(defn start-ws-clients []
  (doseq [i (range 4)]
    (swap! ws-clients conj (ws/connect
                             "ws://localhost:9090/"
                             :on-receive (fn [msg]
                                           (let [[req-id & resource-path] (seq (.split msg " "))]
                                             (Thread/sleep 1000)
                                             ((send-fn i) (apply str req-id " You requested /" (interpose "/" resource-path)))))))))

(defn send-web-requests
  [client-ids]
  (assert client-ids)
  (loop[[client & client-ids] (cycle client-ids)
        [res & resources] (cycle ["/apples" "/oranges" "/chestnuts" "/pineapples" "/mango"])]
    (go
      (println (.toString (java.util.Date.)) (String. (.bytes (:body @(client/get (str "http://localhost:9090/" client res)))))))
    (Thread/sleep 500) ;; with values lower than 390 (on my machine) a deadlock occurs.
    (recur client-ids resources)))

(defn -main
  [& args]
  (run-server app {:port 9090, :ip "127.0.0.1"})
  (start-ws-clients)
  (Thread/sleep 1000)
  (send-web-requests (keys @clients)))
