#!/usr/bin/env bb

(require '[babashka.http-client :as httpb])
(require '[clojure.string :as str])
(require '[clojure.edn :as edn])

(def load-config
  (delay
    (let [c (edn/read-string (slurp (str (System/getenv "HOME") "/.config/blabla/config.edn")))]
      (for [k [:open-ai-key :model]
            :when (nil? (get c k))]
        (do
          (println (str "Missing config key: " k))
          (System/exit 1)))
      c)))

(defn config [x]
  (get @load-config x))

(defn on-server-sent-event
  "Given a byte stream (anything that supports .read), parse SSE,
   and trigger the given f for each event.
   WARNING: not fully to spec"
  ;; https://html.spec.whatwg.org/multipage/server-sent-events.html
  ;; https://html.spec.whatwg.org/multipage/server-sent-events.html#event-stream-interpretation
  [in f]
  (with-open [stream in]
    (loop [byte-buffer (transient [])]
      (let [byte-read (.read stream)]
        (when (not= -1 byte-read)
          ;; if end of line
          ;; TODO spec defines multiple end of lines
          (if (= (int \newline)
                 byte-read
                 (get byte-buffer (dec (count byte-buffer))))
            (do
              (let [line (String. (byte-array (persistent! byte-buffer))
                                  (java.nio.charset.Charset/forName "UTF-8"))]
                (cond
                  (str/blank? line)
                  nil ;; TODO dispatch
                  (str/starts-with? line ":")
                  nil ;; do nothing
                  (str/includes? line ":")
                  ;; only a single newline at end, b/c we don't at the newline that we just read
                  (let [[_ k v] (re-matches #"(event|data|id|retry): ([^\n]+)\n" line)]
                    (f {(keyword k) v}))
                  :else
                  (f {(keyword line) ""})))
              (recur (transient [])))
            (recur (conj! byte-buffer byte-read))))))))

#_(with-open [s (io/input-stream (io/file "test.txt"))]
    (on-server-sent-event s
                          (fn [x] (println x))))

(def messages (atom [{:role "system"
                      :content "Give brief responses as if talking to an expert that just needs to be reminded."}]))

(defn stream-chat [messages partial-message-f complete-message-f]
  (let [message (atom "")]
    ;; https://platform.openai.com/docs/api-reference/chat
    (on-server-sent-event
      (:body (httpb/request {:method :post
                             :uri "https://api.openai.com/v1/chat/completions"
                             :headers {"Content-Type" "application/json"
                                       "Authorization" (str "Bearer " (config  :open-ai-key))}
                             :as :stream
                             :body (json/generate-string
                                     {:model (config :model)
                                      :verbosity "low"
                                      :stream true
                                      :messages messages})}))
      (fn [event]
        (when (str/starts-with? (:data event) "{")
          (let [partial-message (:content (:delta (first (:choices (json/parse-string (:data event) true)))))]
            (partial-message-f partial-message)
            (swap! message str partial-message)))))
    (complete-message-f @message)))

#_(stream-chat
    [{:role "user"
      :content "hello, what's your name?"}]
    (fn [text]
      (print text)))

(defn prompt []
  (print "> ")
  (flush)
  (read-line))

(loop [input (prompt)]
  (if (str/blank? input)
    (recur (prompt))
    (do
      (swap! messages conj {:role "user"
                            :content input})
      (stream-chat @messages
                   (fn [text]
                     (some-> text
                             (print))
                     (flush))
                   (fn [message]
                     (swap! messages conj {:role "assistant"
                                           :content message})))
      (println "\n")
      (recur (prompt)))))
