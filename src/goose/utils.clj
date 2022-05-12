(ns goose.utils)

(defmacro log-on-exceptions
  "Catch any Exception from the body and return nil."
  [& body]
  `(try
     ~@body
     (catch Exception e#
       (println (.toString e#))
       (println
         "Exception occurred at:"
         (-> e#
             (Throwable->map)
             (get :via)
             (first)
             (get :at))))))