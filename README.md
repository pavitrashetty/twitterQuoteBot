#Tweeting Quote Bot (Clojure)

A Clojure library designed to learn from text files, and auto-generate messages that get tweeted to some account.

## Usage

lein repl
> (status-update-on-twitter)

*You would need to provide a profiles.clj file for giving Twitter account credentials.  
Eg:  
{:dev {:env {:app-consumer-key "abc"
             :app-consumer-secret "xyz"
             :user-access-token "pqr"
             :user-access-secret "ijk"}}}

