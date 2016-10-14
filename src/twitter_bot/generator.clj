(ns twitter-bot.generator
  (:require [clojure.set :as cset]
            [clojure.string :as cstr]
            [clojure.java.io :refer [resource]]
            [twitter.api.restful :as twit]
            [twitter.oauth :as tauth]
            [environ.core :refer [env]]))


(def my-creds (tauth/make-oauth-creds (env :app-consumer-key) (env :app-consumer-secret) (env :user-access-token) (env :user-access-secret)))
;;;(def my-creds (tauth/make-oauth-creds "7qzyGJqxCyDcePwXP0PlTEnX4" "I6UukgYFcQSGIDN8EuoeKtpK8mG6Ypbio8OuaK6bM5lTDvB13L" "113671604-g0snLRWYTCnyTpn4zS1EGojKhADsJGt59LcToBdl" "WX3vFMH5ZgSD7M1muv6SC2p7bEU26Gri3jdcMgJIMndaG"))

(def files ["training-file.txt" "Lear-poems.txt" "learn-clojure.txt"])
(def prefixes ["On the" "They went" "And all" "We think"
                  "For every" "No other" "To a" "And every"
                  "We, too," "For his" "And the" "But the"
                  "Are the" "The Pobble" "For the" "When we"
                  "In the" "Yet we" "With only" "Are the"
                  "Though the"  "And when"
                  "We sit" "And this" "No other" "With a"
                  "And at" "What a" "Of the"
                  "O please" "So that" "And all" "When they"
                  "But before" "Whoso had" "And nobody" "And it's"
               "For any" "For example," "Also in" "In contrast"])


;;; Generating message from input prefixes


(defn- chain->text
  [chain]
  (cstr/join " " chain))


(defn ^:testable walk-chain
  "Logic to walk the word chain, given the prefix"
  [prefix chain result]
  (if (not (empty? (get chain prefix)))
    (let [suffix (first (shuffle (get chain prefix)))
          new-prefix [(last prefix) suffix]
          total-chars (+ (count (chain->text result)) (count suffix) 1)]
      (if (> total-chars 140)
        result
        (recur new-prefix chain (conj result suffix))))
    result
    )
  )


(defn generate-text
  "Given a prefix and word chain, return sentence with that prefix (limited to 140 characters)"
  [text-prefix chain]
  (let [prefix (cstr/split text-prefix #" ")]
    (chain->text (walk-chain prefix chain prefix))))


;;; Learning from input files


(defn- reducing-fn [result input]
  (merge-with cset/union result (let [[a b c] input] {[a b] (if c #{c} #{})})))


(defn ^:testable word-chain
  "Logic for mapping prefix to suffix"
  [word-transitions]
  (reduce reducing-fn {} word-transitions)
  )


(defn ^:testable text->word-chain
  "Convert given text line to a map of prefix-suffix"
  [input-text]
  (word-chain (partition-all 3 1 (cstr/split input-text #"[\s|\n]")))
  )


(defn ^:testable process-file
  "Process an input file"
  [fname]
  (text->word-chain (slurp (resource fname))))


(def learning-files
  "Read all input files and create a referential word chain"
  (apply merge-with cset/union (map process-file files)))


;;; String manupulation method


(defn ^:testable convert-text-to-message
  "Given a string of words, make a readable message using end punctuations"
  [text]
  (let [text-till-last-punctuation (apply str (re-seq #"[\s\w]+[^.!?,]*[.!?,]" text))
        text-till-last-word (apply str (re-seq #".*[^A-Za-z]+" text))
        truncated-text (if (empty? text-till-last-punctuation)
                         text-till-last-word
                         text-till-last-punctuation)
        clean-text (cstr/replace (cstr/replace truncated-text #"[,| ]$" ".") #"\"" "'")]
    clean-text))


;;; Now lets use above functionalities
()

(defn create-tweet
  "Given a prefix, auto-generate a tweet message"
  []
  (let [plain-text (generate-text (-> prefixes shuffle first) learning-files)]
    (convert-text-to-message plain-text)))


(defn status-update-on-twitter
  "Post above generated tweet message to Twitter account"
  []
  (loop []
    (let [msg (create-tweet)
          mcount (count msg)]
      (println "Message is " msg)
      (if (and (> mcount 100) (< mcount 140))
        (try
          (twit/statuses-update :oauth-creds my-creds
                                :params {:status msg})
          (catch Exception e (println "Failed for " e)))
        (recur)))))
