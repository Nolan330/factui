(ns todomvc.factui.ui
  (:require [clojure.string :as str]
            [factui.api :as f :include-macros true]
            [factui.rum :as fr :refer [*results*] :refer-macros [q]]
            [rum.core :as rum :include-macros true]))

(rum/defc Todo < rum/static
                 {:key-fn (fn [_ todo] todo)}
                 (fr/q [:find [?name ?completed]
                        :in ?t
                        :where
                        [?t :task/name ?name]
                        (maybe [?t :task/completed ?completed])])

  [app-state ?todo]
  (let [[name completed] *results*]
    [:li {:style {:display "inline"}}
     [:div.view
      [:input.toggle {:type "checkbox"
                      :checked completed
                      :on-change (fn [_]
                                   (fr/transact! app-state
                                     [{:db/id ?todo
                                       :task/completed (not completed)}]))}]
      [:label name]
      [:button.destroy]]]))

(rum/defc TodoList < rum/static
                     (fr/q [:find ?mode ?t ?completed
                            :where
                            [?g :global/view-mode ?mode]
                            [?g :global/tasks ?t]
                            (maybe [?t :task/completed ?completed])])
  [app-state]
  (let [todos (->> *results*
                (filter (fn [[mode t completed]]
                          (or (= mode :all)
                              (and (= mode :completed) completed)
                              (and (= mode :active) (not completed)))))
                (map second)
                (sort))]
    [:ul#todo-list (for [todo todos]
                     (Todo app-state todo))]))

(rum/defc PrimaryInput < rum/static
                         (fr/q [:find [?t ?value]
                                :where
                                [_ :global/new-task ?t]
                                [?t :task/name ?value]])
  [app-state]
  (let [[task value] *results*
        clear (fn [evt] (fr/transact! app-state
                          [{:db/id task
                            :task/name ""}]))
        save (fn [evt]
               (when-not (str/blank? value)
                 (fr/transact! app-state [{:db/ident :global
                                           :global/tasks task
                                           :global/new-task {:task/name ""}}])))
        change (fn [evt]
                 (let [v (.-value (.-target evt))]
                   (fr/transact! app-state [{:db/id task
                                             :task/name v}])))]
    [:input#new-todo {:value value
                      :placeholder "What needs to be done?"
                      :on-change change
                      :on-key-down (fn [evt]
                                     (case (.-which evt)
                                       13 (save evt)      ;; enter
                                       27 (clear evt)     ;; escape
                                       nil))}]))

(rum/defc PendingTaskCount < rum/static
                             (fr/q [:find [?t ...]
                                    :where
                                    [?g :global/tasks ?t]
                                    (not [?t :task/completed true])])
  [app-state]
  [:span#todo-count
   [:strong (count *results*)]
   [:span " items left"]])

(rum/defc ModeSelector < rum/static
  [app-state current-mode mode label]
  [:li [:a {:href "#"
            :class (when (= current-mode mode) ["selected"])
            :onClick (fn [evt]
                       (fr/transact! app-state [{:db/ident :global
                                                 :global/view-mode mode}])
                       (.preventDefault evt)
                       false)}
        label]])

(rum/defc Footer < rum/static
                   (fr/q [:find ?mode .
                          :where
                          [?g :global/view-mode ?mode]])
  [app-state]
  [:footer#footer
   (PendingTaskCount app-state)
   [:ul#filters
    (ModeSelector app-state *results* :all "All")
    (ModeSelector app-state *results* :active "Active")
    (ModeSelector app-state *results* :completed "Completed")]])

(rum/defc App < rum/static
  [app-state]
  [:div#app
   [:div
    [:section#todoapp
     [:header#header
      [:h1 "todos"]
      (PrimaryInput app-state)]
     [:div {:style {:display "inline"}}
      [:section#main
       [:span
        [:input#toggle-all {:type "checkbox"}]
        [:label {:for "toggle-all"} "Mark all as complete"]]
       (TodoList app-state)]
      (Footer app-state)]]
    [:footer#info
     [:p "Double click to edit a todo"]]]])