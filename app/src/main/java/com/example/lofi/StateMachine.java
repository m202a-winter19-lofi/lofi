package com.example.lofi;

public class StateMachine {

    private final static int NUM_STATES = 3;
    private int[] states = new int[NUM_STATES]; // This is effectively a queue
    public int state = 0; // 0: non-active, 1: active

    public StateMachine () {

        for (int i = 0; i < NUM_STATES; i++) {
            states[i] = 0;
        }

    }

    public void update(int level) {

        int sum = 0;

        for (int i = NUM_STATES - 1; i > 0; i--) {
            states[i] = states[i-1];
            sum += states[i];
        }

        states[0] = level;
        sum += states[0];

        if (sum == 0) state = 0;          // if last 3 states were = 0
        if (sum == NUM_STATES) state = 1; // if last 3 states were = 1

    }


}
