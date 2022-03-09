package com.unipi.client.mainFrame;

public class ActionPipe {
    private static final Object key = new Object();
    private static Object obj;
    private static ACTIONS currentAction;
    private static boolean closed = false;

    public static ACTIONS waitForAction() {
        if(closed) return ACTIONS.CLOSE_ACTION;

        currentAction = ACTIONS.NONE;

        synchronized (key){
            while (currentAction == ACTIONS.NONE){
                try {
                    key.wait();
                } catch (InterruptedException e) {
                    return ACTIONS.CLOSE_ACTION;
                }
            }
        }

        return currentAction;
    }

    public static Object getParameter() {
        return obj;
    }

    public static void performAction(ACTIONS action, Object param) {
        synchronized (key){
            currentAction = action;
            key.notify();
        }

        obj = param;
    }

    public static void performAction(ACTIONS action){
        performAction(action, null);
    }

    public static void closeActionPipe(){
        synchronized (key) {
            currentAction = ACTIONS.CLOSE_ACTION;
            closed = true;
            key.notify();
        }
    }

    public static boolean isClosed() {
        return closed;
    }
}
