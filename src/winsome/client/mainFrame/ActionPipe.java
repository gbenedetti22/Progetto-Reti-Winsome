package winsome.client.mainFrame;

public class ActionPipe {
    // Oggetto usato per l attesa
    // è possibile usare anche le condition variables
    private static final Object key = new Object();
    private static Object attach; // oggetto usato per far comunicare chi chiama la performAction()
    // e chi è in attesa sulla waitForAction()
    private static ACTIONS currentAction; // Azione corrente da compiere. Viene modificata con la performAction()
    private static boolean closed = false;

    public static ACTIONS waitForAction() {
        if (closed) return ACTIONS.CLOSE_ACTION;

        currentAction = ACTIONS.NONE;

        synchronized (key) {
            while (currentAction == ACTIONS.NONE) {
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
        return attach;
    }

    public static void performAction(ACTIONS action, Object param) {
        synchronized (key) {
            currentAction = action;
            key.notify();
        }

        attach = param;
    }

    public static void performAction(ACTIONS action) {
        performAction(action, null);
    }

    public static void closeActionPipe() {
        synchronized (key) {
            currentAction = ACTIONS.CLOSE_ACTION;
            closed = true;
            key.notify();
        }
    }

}
