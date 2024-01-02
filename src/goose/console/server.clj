(ns goose.console.server
  (:require [ring.adapter.jetty :as jetty]))

(defonce server (atom nil))

(defn handler [request]
      (let [html "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Goose Dashboard</title>\n<link rel=\"stylesheet\" href=\"styles.css\">\n<style>\n    body {\n    font-family: Arial, sans-serif;\n    margin: 0;\n    padding: 0;\n    background-color: #f4f4f4;\n}\n\nheader nav {\n    background-color: #ffffff;\n    display: flex;\n    justify-content: space-between;\n    padding: 1rem;\n    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);\n}\nmain {\n    height: 50vh;\n    display: flex;\n    align-items: center;\n}\n\n.nav-start {\n    display: flex;\n    justify-content: center;\n    align-items: center;\n}\n#logo {\n    font-size: 1.5rem;\n    color: #333;\n}\n\n#menu a {\n    text-decoration: none;\n    color: #333;\n    margin-left: 2rem;\n}\n\n.statistics {\n    display: flex;\n    justify-content: space-around;\n    align-items: center;\n    background: white;\n    margin: 2rem;\n    padding: 1rem;\n    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);\n    border-radius: 8px;\n    width: -webkit-fill-available;\n}\n\n.stat {\n    text-align: center;\n    display: grid;\n}\n\n.stat .number {\n    color: #C87021; /* or any other color you prefer */\n    font-size: 36px;\n    font-weight: bold;\n    margin-bottom: 5px;\n}\n\n.stat .label {\n    color: #333;\n    font-size: 18px;\n}\n\n/* Toggle switch */\n.toggle-switch {\n  position: relative;\n  display: inline-block;\n  width: 60px;\n  height: 34px;\n}\n\n.toggle-switch input {\n  opacity: 0;\n  width: 0;\n  height: 0;\n}\n\n.switch-slider {\n  position: absolute;\n  cursor: pointer;\n  top: 0;\n  left: 0;\n  right: 0;\n  bottom: 0;\n  background-color: #ccc;\n  transition: .4s;\n  border-radius: 34px;\n}\n\n.switch-slider:before {\n  position: absolute;\n  content: \"\";\n  height: 26px;\n  width: 26px;\n  left: 4px;\n  bottom: 4px;\n  background-color: white;\n  transition: .4s;\n  border-radius: 50%;\n}\n\ninput:checked + .switch-slider {\n  background-color: #2196F3;\n}\n\ninput:focus + .switch-slider {\n  box-shadow: 0 0 1px #2196F3;\n}\n\ninput:checked + .switch-slider:before {\n  transform: translateX(26px);\n}\n\n</style>\n</head>\n<body>\n<header>\n    <nav>\n        <div class=\"nav-start\">\n            <div id=\"logo\">AppName</div>\n            <div id=\"menu\">\n                <a href=\"/enqueued\">Enqueued</a>\n                <a href=\"/scheduled\">Scheduled</a>\n                <a href=\"/periodic\">Periodic</a>\n                <a href=\"/batch\">Batch</a>\n                <a href=\"/dead\">Dead</a>\n            </div>\n        </div>\n        <div class=\"nav-end\">\n            <label class=\"toggle-switch\">\n                <input type=\"checkbox\" />\n                <span class=\"switch-slider\"></span>\n              </label>\n        </div>\n    </nav>\n</header>\n\n<main>\n    <section class=\"statistics\">\n        <div class=\"stat\" id=\"enqueued\">\n            <span class=\"number\">234</span>\n            <span class=\"label\">Enqueued</span>\n        </div>\n        <div class=\"stat\" id=\"scheduled\">\n            <span class=\"number\">5</span>\n            <span class=\"label\">Scheduled</span>\n        </div>\n        <div class=\"stat\" id=\"periodic\">\n            <span class=\"number\">3</span>\n            <span class=\"label\">Periodic</span>\n        </div>\n        <div class=\"stat\" id=\"dead\">\n            <span class=\"number\">43</span>\n            <span class=\"label\">Dead</span>\n        </div>\n    </section>\n</main>\n</body>\n</html>\n"]

           {:status  200
            :headers {"Content-Type" "text/html"}
            :body    html}))

(defn start-server []
  (reset! server (jetty/run-jetty handler {:port  3001
                                           :join? false})))

(defn stop-server []
  (when-let [s @server]
    (.stop s)
    (reset! server nil)
    (println "Server stopped")))

(comment (start-server))
(comment (stop-server))

