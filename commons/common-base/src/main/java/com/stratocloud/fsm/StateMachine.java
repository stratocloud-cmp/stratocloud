package com.stratocloud.fsm;

import com.stratocloud.exceptions.StratoException;

import java.util.*;


public class StateMachine<S, E> {
    private final HashMap<S, StateEntry> _states;
    private final StateEntry _initialStateEntry;

    public StateMachine() {
        _initialStateEntry = new StateEntry(null);

        _states = new HashMap<>();
    }

    public void addTransition(S currentState, E event, S toState) {
      addTransition(new Transition<>(currentState, event, toState));
    }

    public void addTransition(S currentState, E event) {
        addTransition(currentState, event, null);
    }


    private void addTransition(Transition<S, E> transition) {
        S currentState = transition.currentState();
        E event = transition.event();
        S toState = transition.toState();
        StateEntry entry;
        if (currentState == null) {
        entry = _initialStateEntry;
        } else {
        entry = _states.get(currentState);
        if (entry == null) {
          entry = new StateEntry(currentState);
          _states.put(currentState, entry);
        }
        }

        entry.addTransition(event, toState, transition);

        entry = _states.get(toState);
        if (entry == null) {
        entry = new StateEntry(toState);
        _states.put(toState, entry);
        }
        entry.addFromTransition(event, currentState);
    }

    public Set<E> getPossibleEvents(S s) {
        StateEntry entry = _states.get(s);
        return entry.nextStates.keySet();
    }

    public S getNextState(S s, E e) {
        return getTransition(s, e).toState();
    }

    private Transition<S, E> getTransition(S s, E e) {
        StateEntry entry;
        if (s == null) {
        entry = _initialStateEntry;
        } else {
        entry = _states.get(s);
        assert entry != null : "Cannot retrieve transitions for state " + s;
        }

        Transition<S, E> transition = entry.nextStates.get(e);
        if (transition == null) {
          throw new StratoException("Unable to transition to a new state from " + s + " via " + e);
        }
        return transition;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(1024);
        _initialStateEntry.buildString(str);
        for (StateEntry entry : _states.values()) {
            entry.buildString(str);
        }
        return str.toString();
    }

    private record Transition<S, E>(S currentState, E event, S toState) {
    }

    private class StateEntry {
        public S state;
        public HashMap<E, Transition<S, E>> nextStates;
        public HashMap<E, List<S>> prevStates;

        public StateEntry(S state) {
            this.state = state;
            prevStates = new HashMap<>();
            nextStates = new HashMap<>();
        }

        public void addTransition(E e, S s, Transition<S, E> transition) {
            assert !nextStates.containsKey(e) : "State " + getStateStr() + " already contains a transition to state " + nextStates.get(e).toString() + " via event " +
                e.toString() + ".  Please revisit the rule you're adding to state " + s.toString();
            nextStates.put(e, transition);
        }

        public void addFromTransition(E e, S s) {
            List<S> l = prevStates.computeIfAbsent(e, k -> new ArrayList<>());

            assert !l.contains(s) : "Already contains the from transition " + e.toString() + " from state " + s.toString() + " to " + getStateStr();
            l.add(s);
        }

        protected String getStateStr() {
            return state == null ? "Initial" : state.toString();
        }

        public void buildString(StringBuilder str) {
            str.append("State: ").append(getStateStr()).append("\n");
            for (Map.Entry<E, Transition<S, E>> nextState : nextStates.entrySet()) {
                str.append("  --> Event: ");
                Formatter format = new Formatter();
                str.append(format.format("%-30s", nextState.getKey().toString()));
                str.append("----> State: ");
                str.append(nextState.getValue().toString());
                str.append("\n");
            }
        }
    }
}
