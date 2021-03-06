
package com.clockwork.app.state;
 
import com.clockwork.app.Application;
import com.clockwork.renderer.RenderManager;
import com.clockwork.util.SafeArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The AppStateManager holds a list of AppState}s which
 * it will update and render.
 * When an AppState} is attached or detached, the
 * AppState#stateAttached(com.clockwork.app.state.AppStateManager) } and
 * AppState#stateDetached(com.clockwork.app.state.AppStateManager) } methods
 * will be called respectively.
 *
 * The lifecycle for an attached AppState is as follows:
 * 
 * stateAttached() : called when the state is attached on the thread on which
 *                       the state was attached.
 * initialize() : called ONCE on the render thread at the beginning of the next
 *                    AppStateManager.update().
 * stateDetached() : called when the state is detached on the thread on which
 *                       the state was detached.  This is not necessarily on the
 *                       render thread and it is not necessarily safe to modify
 *                       the scene graph, etc..
 * cleanup() : called ONCE on the render thread at the beginning of the next update
 *                 after the state has been detached or when the application is 
 *                 terminating.  
 *  
 *
 * 
 */
public class AppStateManager {

    /**
     *  List holding the attached app states that are pending
     *  initialization.  Once initialized they will be added to
     *  the running app states.  
     */
    private final SafeArrayList<AppState> initializing = new SafeArrayList<AppState>(AppState.class);
    
    /**
     *  Holds the active states once they are initialized.  
     */
    private final SafeArrayList<AppState> states = new SafeArrayList<AppState>(AppState.class);
    
    /**
     *  List holding the detached app states that are pending
     *  cleanup.  
     */
    private final SafeArrayList<AppState> terminating = new SafeArrayList<AppState>(AppState.class);
 
    // All of the above lists need to be thread safe but access will be
    // synchronized separately.... but always on the states list.  This
    // is to avoid deadlocking that may occur and the most common use case
    // is that they are all modified from the same thread anyway.
    
    private final Application app;
    private AppState[] stateArray;

    public AppStateManager(Application app){
        this.app = app;
    }

    /**
     *  Returns the Application to which this AppStateManager belongs.
     */
    public Application getApplication() {
        return app;
    }

    protected AppState[] getInitializing() { 
        synchronized (states){
            return initializing.getArray();
        }
    } 

    protected AppState[] getTerminating() { 
        synchronized (states){
            return terminating.getArray();
        }
    } 

    protected AppState[] getStates(){
        synchronized (states){
            return states.getArray();
        }
    }

    /**
     * Attach a state to the AppStateManager, the same state cannot be attached
     * twice.
     *
     * @param state The state to attach
     * @return True if the state was successfully attached, false if the state
     * was already attached.
     */
    public boolean attach(AppState state){
        synchronized (states){
            if (!states.contains(state) && !initializing.contains(state)){
                state.stateAttached(this);
                initializing.add(state);
                return true;
            }else{
                return false;
            }
        }
    }

    /**
     * Attaches many state to the AppStateManager in a way that is guaranteed
     * that they will all get initialized before any of their updates are run.
     * The same state cannot be attached twice and will be ignored.
     *
     * @param states The states to attach
     */
    public void attachAll(AppState... states){
        attachAll(Arrays.asList(states));
    }

    /**
     * Attaches many state to the AppStateManager in a way that is guaranteed
     * that they will all get initialized before any of their updates are run.
     * The same state cannot be attached twice and will be ignored.
     *
     * @param states The states to attach
     */
    public void attachAll(Iterable<AppState> states){
        synchronized (this.states){
            for( AppState state : states ) {
                attach(state);
            }
        }
    }

    /**
     * Detaches the state from the AppStateManager. 
     *
     * @param state The state to detach
     * @return True if the state was detached successfully, false
     * if the state was not attached in the first place.
     */
    public boolean detach(AppState state){
        synchronized (states){
            if (states.contains(state)){
                state.stateDetached(this);
                states.remove(state);
                terminating.add(state);
                return true;
            } else if(initializing.contains(state)){
                state.stateDetached(this);
                initializing.remove(state);
                return true;
            }else{
                return false;
            }
        }
    }

    /**
     * Check if a state is attached or not.
     *
     * @param state The state to check
     * @return True if the state is currently attached to this AppStateManager.
     * 
     * see AppStateManager#attach(com.clockwork.app.state.AppState)
     */
    public boolean hasState(AppState state){
        synchronized (states){
            return states.contains(state) || initializing.contains(state);
        }
    }

    /**
     * Returns the first state that is an instance of subclass of the specified class.
     * @param <T>
     * @param stateClass
     * @return First attached state that is an instance of stateClass
     */
    public <T extends AppState> T getState(Class<T> stateClass){
        synchronized (states){
            AppState[] array = getStates();
            for (AppState state : array) {
                if (stateClass.isAssignableFrom(state.getClass())){
                    return (T) state;
                }
            }
            
            // This may be more trouble than its worth but I think
            // it's necessary for proper decoupling of states and provides
            // similar behavior to before where a state could be looked
            // up even if it wasn't initialized. -pspeed
            array = getInitializing();
            for (AppState state : array) {
                if (stateClass.isAssignableFrom(state.getClass())){
                    return (T) state;
                }
            }
        }
        return null;
    }

    protected void initializePending(){
        AppState[] array = getInitializing();
        if (array.length == 0)
            return;
            
        synchronized( states ) {
            // Move the states that will be initialized
            // into the active array.  In all but one case the
            // order doesn't matter but if we do this here then
            // a state can detach itself in initialize().  If we
            // did it after then it couldn't.
            List<AppState> transfer = Arrays.asList(array);         
            states.addAll(transfer);
            initializing.removeAll(transfer);
        }        
        for (AppState state : array) {
            state.initialize(this, app);
        }
    }
    
    protected void terminatePending(){
        AppState[] array = getTerminating();
        if (array.length == 0)
            return;
            
        for (AppState state : array) {
            state.cleanup();
        }        
        synchronized( states ) {
            // Remove just the states that were terminated...
            // which might now be a subset of the total terminating
            // list.
            terminating.removeAll(Arrays.asList(array));         
        }
    }    

    /**
     * Calls update for attached states, do not call directly.
     * @param tpf Time per frame.
     */
    public void update(float tpf){
    
        // Cleanup any states pending
        terminatePending();

        // Initialize any states pending
        initializePending();

        // Update enabled states    
        AppState[] array = getStates();
        for (AppState state : array){
            if (state.isEnabled()) {
                state.update(tpf);
            }
        }
    }

    /**
     * Calls render for all attached and initialized states, do not call directly.
     * @param rm The RenderManager
     */
    public void render(RenderManager rm){
        AppState[] array = getStates();
        for (AppState state : array){
            if (state.isEnabled()) {
                state.render(rm);
            }
        }
    }

    /**
     * Calls render for all attached and initialized states, do not call directly.
     */
    public void postRender(){
        AppState[] array = getStates();
        for (AppState state : array){
            if (state.isEnabled()) {
                state.postRender();
            }
        }
    }

    /**
     * Calls cleanup on attached states, do not call directly.
     */
    public void cleanup(){
        AppState[] array = getStates();
        for (AppState state : array){
            state.cleanup();
        }
    }    
}
